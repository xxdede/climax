package it.tidal.climax.extensions.managers;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import it.tidal.climax.config.NetAtmoConfig;
import it.tidal.climax.extensions.data.NetAtmo;
import it.tidal.config.utils.Utility;
import it.tidal.gson.GsonFactory;
import it.tidal.logging.Log;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * NetAtmo Extensions. Connects to and queries NetAtmo servers.
 *
 * @author dede
 */
public class NetAtmoManager {

    private static Log l = Log.prepare(NetAtmoManager.class.getSimpleName());

    public static final String URL_STATION_DATA = "https://api.netatmo.com/api/getstationsdata";
    public static final String URL_THERMO_DATA = "https://api.netatmo.com/api/getthermostatsdata";
    public static final String URL_TOKEN = "https://api.netatmo.com/oauth2/token";
    public static final String TMP_CREDENTIALS_PATH = "/tmp/climax-netatmo-tokens";

    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiration;

    public NetAtmoManager(String accessToken, String refreshToken, int expirationSecs) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiration = LocalDateTime.now().plusSeconds(expirationSecs - 10);
    }

    public NetAtmoManager(Map<String, Object> map) {

        super();

        Object temp = map.get("expiration");
        Long epochSeconds = 0L;

        if (temp instanceof Long) {
            epochSeconds = (Long) temp;
        } else if (temp instanceof Double) {
            epochSeconds = ((Double) temp).longValue();
        }

        LocalDateTime expirationTime = Utility.localDateTime(epochSeconds);

        this.accessToken = (String) map.get("accessToken");
        this.refreshToken = (String) map.get("refreshToken");
        this.expiration = expirationTime;
    }

    public HashMap<String, Object> toMap() {

        HashMap<String, Object> data = new HashMap<>(3);

        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("expiration", Utility.timestamp(expiration));

        return data;
    }

    public static void writeToFile(NetAtmoManager creds) {

        try {

            String output = GsonFactory.instance().toJson(creds.toMap());
            Files.write(Paths.get(TMP_CREDENTIALS_PATH), output.getBytes());
        } catch (IOException ex) {

            l.error("Cannot serialize NetAtmo creds!");
        }
    }

    public static NetAtmoManager readFromFile() {

        try {

            File file = new File(TMP_CREDENTIALS_PATH);

            if (!file.exists() || !file.canRead()) {

                l.debug("No previous NetAtmo creds...");
                return null;
            }

            JsonReader reader = new JsonReader(new FileReader(file));
            Type t = new TypeToken<HashMap<String, Object>>() {
            }.getType();
            HashMap<String, Object> map = GsonFactory.instance().fromJson(reader, t);
            return new NetAtmoManager(map);
        } catch (JsonSyntaxException | IOException ex) {

            l.error("Cannot deserialize NetAtmo creds!", ex);
            return null;
        }
    }

    public static NetAtmoManager getInstance(NetAtmoConfig cfg) {

        if (cfg == null) {

            l.error("Unable to get NetAtmo instance without a conf!");
            return null;
        }

        NetAtmoManager prev = readFromFile();

        if (prev != null) {

            l.debug("Found previous NetAtmo credentials...");
            prev.checkAndRenew(cfg);
            return prev;
        } else {

            l.debug("Asking new credentials to NetAtmo...");

            try {

                HttpResponse<JsonNode> jsonResponse = Unirest.post(URL_TOKEN)
                        .header("accept", "application/json")
                        .field("grant_type", "password")
                        .field("client_id", cfg.getClientId())
                        .field("client_secret", cfg.getClientSecret())
                        .field("username", cfg.getUsername())
                        .field("password", cfg.getPassword())
                        .field("scope", cfg.getScope())
                        .asJson();

                JSONObject res = jsonResponse.getBody().getObject();

                NetAtmoManager nm = new NetAtmoManager(
                        res.getString("access_token"),
                        res.getString("refresh_token"),
                        res.getInt("expires_in"));

                writeToFile(nm);
                return nm;
            } catch (UnirestException | JSONException ex) {

                l.error("Error while asking new credentials to NetAtmo!");
            }
        }

        return null;
    }

    public boolean checkAndRenew(NetAtmoConfig cfg) {

        if (!isExpired()) {

            l.debug("NetAtmo credentials are still valid...");
            return true;
        }

        try {

            l.debug("NetAtmo credentials expired, renewing...");

            HttpResponse<JsonNode> jsonResponse = Unirest.post(URL_TOKEN)
                    .header("accept", "application/json")
                    .field("grant_type", "refresh_token")
                    .field("client_id", cfg.getClientId())
                    .field("client_secret", cfg.getClientSecret())
                    .field("refresh_token", refreshToken)
                    .asJson();

            JSONObject res = jsonResponse.getBody().getObject();

            String newAccessToken = res.getString("access_token");
            String newRefreshToken = res.getString("refresh_token");
            int newExpiresIn = res.getInt("expires_in");

            if (newAccessToken != null && newRefreshToken != null) {

                accessToken = newAccessToken;
                refreshToken = newRefreshToken;
                expiration = LocalDateTime.now().plusSeconds(newExpiresIn - 10);

                writeToFile(this);
                return true;
            } else {

                l.warn("Received new blank credentials from NetAtmo?!?");
                return false;
            }
        } catch (UnirestException | JSONException ex) {

            l.error("Problem while getting new NetAtmo credentials.");
            return false;
        }
    }

    public boolean isExpired() {

        if (expiration == null) {
            return true;
        }

        return (LocalDateTime.now().compareTo(expiration) >= 0);
    }

    public NetAtmo getStationData(String moduleName) {

        if (moduleName == null) {

            l.error("Cannot get NetAtmo station data without module name!");
            return null;
        }

        JSONArray devs = null;

        try {

            HttpResponse<JsonNode> jsonResponse = Unirest.post(URL_STATION_DATA)
                    .header("accept", "application/json")
                    .field("access_token", accessToken)
                    .asJson();

            final JSONObject body = jsonResponse.getBody()
                    .getObject()
                    .getJSONObject("body");

            devs = body.getJSONArray("devices");
        } catch (UnirestException | JSONException ex) {

            l.error(ex, "Problem while getting station data!");
            return null;
        }

        for (int i = 0; i < devs.length(); i++) {

            final JSONObject dev = devs.getJSONObject(i);
            final String baseModuleName = dev.getString("module_name");

            if (moduleName.equals(baseModuleName)) {

                final JSONObject data = dev.getJSONObject("dashboard_data");

                return new NetAtmo(moduleName,
                        data.getDouble("Temperature"),
                        data.getInt("Humidity"),
                        (data.has("CO2") ? data.getInt("CO2") : null));
            }

            final JSONArray modules = dev.getJSONArray("modules");

            for (int j = 0; j < modules.length(); j++) {

                final JSONObject module = modules.getJSONObject(j);
                final String tempModuleName = module.getString("module_name");

                if (!moduleName.equals(tempModuleName)) {
                    continue;
                }

                final JSONObject data = module.getJSONObject("dashboard_data");

                return new NetAtmo(moduleName,
                        data.getDouble("Temperature"),
                        data.getInt("Humidity"),
                        (data.has("CO2") ? data.getInt("CO2") : null));
            }
        }

        return null;
    }

    public HashMap<String, NetAtmo> getStationsData(List<String> moduleNames) {

        if (moduleNames == null || moduleNames.isEmpty()) {

            l.error("Cannot get NetAtmo stations data without module names!");
            return null;
        }

        JSONArray devs = null;
        HashMap<String, NetAtmo> rets = new HashMap<>(moduleNames.size());

        try {

            HttpResponse<JsonNode> jsonResponse = Unirest.post(URL_STATION_DATA)
                    .header("accept", "application/json")
                    .field("access_token", accessToken)
                    .asJson();

            final JSONObject body = jsonResponse.getBody()
                    .getObject()
                    .getJSONObject("body");

            devs = body.getJSONArray("devices");
        } catch (UnirestException | JSONException ex) {

            l.error(ex, "Problem while getting stations data!");
            return null;
        }

        for (int i = 0; i < devs.length(); i++) {

            final JSONObject dev = devs.getJSONObject(i);
            final String baseModuleName = dev.getString("module_name");

            if (moduleNames.contains(baseModuleName)) {

                final JSONObject data = dev.getJSONObject("dashboard_data");

                rets.put(baseModuleName, new NetAtmo(baseModuleName,
                        data.getDouble("Temperature"),
                        data.getInt("Humidity"),
                        (data.has("CO2") ? data.getInt("CO2") : null)));
            }

            final JSONArray modules = dev.getJSONArray("modules");

            for (int j = 0; j < modules.length(); j++) {

                final JSONObject module = modules.getJSONObject(j);
                final String tempModuleName = module.getString("module_name");

                if (!moduleNames.contains(tempModuleName)) {
                    continue;
                }

                final JSONObject data = module.getJSONObject("dashboard_data");

                rets.put(tempModuleName, new NetAtmo(tempModuleName,
                        data.getDouble("Temperature"),
                        data.getInt("Humidity"),
                        (data.has("CO2") ? data.getInt("CO2") : null)));
            }
        }

        return rets;
    }

    public NetAtmo getThermostatData(String moduleName) {

        if (moduleName == null) {

            l.error("Cannot get NetAtmo thermostat data without module name!");
            return null;
        }

        JSONArray devs = null;

        try {

            HttpResponse<JsonNode> jsonResponse = Unirest.post(URL_THERMO_DATA)
                    .header("accept", "application/json")
                    .field("access_token", accessToken)
                    .asJson();

            final JSONObject body = jsonResponse.getBody()
                    .getObject()
                    .getJSONObject("body");

            devs = body.getJSONArray("devices");
        } catch (UnirestException | JSONException ex) {

            l.error(ex, "Problem while getting thermostat data!");
            return null;
        }

        for (int i = 0; i < devs.length(); i++) {

            final JSONObject dev = devs.getJSONObject(i);
            final String baseModuleName = dev.getString("station_name");

            /*
            if (moduleName.equals(baseModuleName)) {

                final JSONObject data = dev.getJSONObject("dashboard_data");

                return new NetAtmo(moduleName,
                        data.getDouble("Temperature"),
                        data.getInt("Humidity"),
                        (data.has("CO2") ? data.getInt("CO2") : null));
            }
             */
            final JSONArray modules = dev.getJSONArray("modules");

            for (int j = 0; j < modules.length(); j++) {

                final JSONObject module = modules.getJSONObject(j);
                final String tempModuleName = module.getString("module_name");

                if (!moduleName.equals(tempModuleName)) {
                    continue;
                }

                final JSONObject data = module.getJSONObject("measured");
                return new NetAtmo(moduleName, data.getDouble("temperature"));
            }
        }

        return null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(LocalDateTime expiration) {
        this.expiration = expiration;
    }
}
