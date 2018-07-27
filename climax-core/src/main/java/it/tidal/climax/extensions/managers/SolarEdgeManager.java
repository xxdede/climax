package it.tidal.climax.extensions.managers;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import it.tidal.climax.config.SolarEdgeConfig;
import it.tidal.climax.extensions.data.SolarEdge;
import it.tidal.climax.extensions.data.SolarEdgeEnergy;
import it.tidal.logging.Log;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SolarEdge Extensions. Connects to and queries SolarEdge servers.
 *
 * @author dede
 */
public class SolarEdgeManager {

    private static Log l = Log.prepare(SolarEdgeManager.class.getSimpleName());

    private String url;
    private String siteId;
    private String apiKey;

    public SolarEdgeManager(String url, String siteId, String apiKey) {

        this.url = url;
        this.siteId = siteId;
        this.apiKey = apiKey;
    }

    public static SolarEdgeManager getInstance(SolarEdgeConfig cfg) {

        if (cfg == null) {

            l.error("Unable to get SolarEdge instance without a conf!");
            return null;
        }

        return new SolarEdgeManager(
                cfg.getUrl(),
                cfg.getSite(),
                cfg.getKey());

    }

    public String composeUrl() {

        return "" + url + "/site/" + siteId + "/";
    }

    public int getAlignedMinute(int minute) {

        if (minute < 15) {
            return 0;
        } else if (minute < 30) {
            return 15;
        } else if (minute < 45) {
            return 30;
        } else {
            return 45;
        }
    }

    public SolarEdgeEnergy getEnergyDetails(long endTs) {

        final LocalDateTime end = LocalDateTime.
                ofInstant(Instant.ofEpochSecond(endTs), ZoneId.systemDefault());

        return getEnergyDetails(end);
    }

    public SolarEdgeEnergy getEnergyDetails(long startTs, long endTs) {

        final LocalDateTime end = LocalDateTime.
                ofInstant(Instant.ofEpochSecond(endTs), ZoneId.systemDefault());

        final LocalDateTime start = LocalDateTime.
                ofInstant(Instant.ofEpochSecond(startTs), ZoneId.systemDefault());

        return getEnergyDetails(start, end);
    }

    public SolarEdgeEnergy getEnergyDetails(LocalDateTime end) {

        return getEnergyDetails(null, end);
    }

    public SolarEdgeEnergy getEnergyDetails(LocalDateTime start, LocalDateTime end) {

        final String fullUrl = composeUrl() + "energyDetails";
        final LocalDateTime aStart, aEnd;

        try {

            final DateTimeFormatter dtf = SolarEdge.dateFormatter;

            aEnd = end.withMinute(getAlignedMinute(end.getMinute()))
                    .withSecond(0).withNano(0);

            if (start != null) {
                aStart = start.withMinute(getAlignedMinute(start.getMinute()))
                        .withSecond(0).withNano(0);
            } else {
                aStart = aEnd.minusMinutes(15);
            }

            HttpResponse<JsonNode> jsonResponse = Unirest.get(fullUrl)
                    .header("accept", "application/json")
                    .queryString("api_key", apiKey)
                    .queryString("timeUnit", SolarEdge.TimeUnit.QUARTER_OF_AN_HOUR)
                    .queryString("startTime", dtf.format(aStart))
                    .queryString("endTime", dtf.format(aEnd))
                    .asJson();

            final JSONObject data = jsonResponse.getBody()
                    .getObject()
                    .getJSONObject("energyDetails");

            return new Gson().fromJson(data.toString(), SolarEdgeEnergy.class);

        } catch (UnirestException | JSONException ex) {

            l.error(ex, "Problem while getting SolarEdge energy details!");
            return null;
        }
    }
}
