package it.tidal.climax.extensions.managers;

import it.tidal.climax.config.WemoDeviceConfig;
import it.tidal.logging.Log;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Wemo Extensions. Connects to and queries Wemo devices.
 *
 * @author dede
 */
public class WemoManager {

    private static Log l = Log.prepare(WemoManager.class.getSimpleName());

    private final String GET_BINARY_STATE_ACTION = "\"urn:Belkin:service:basicevent:1#GetBinaryState\"";
    private final String SET_BINARY_STATE_ACTION = "\"urn:Belkin:service:basicevent:1#SetBinaryState\"";
    private final String GET_BINARY_STATE_DATA = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:GetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"><BinaryState>1</BinaryState></u:GetBinaryState></s:Body></s:Envelope>";
    private final String SET_BINARY_STATE_DATA_ON  = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:SetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"><BinaryState>1</BinaryState></u:SetBinaryState></s:Body></s:Envelope>";
    private final String SET_BINARY_STATE_DATA_OFF = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:SetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"><BinaryState>0</BinaryState></u:SetBinaryState></s:Body></s:Envelope>";

    private final String INSIGHT_PARAMS_ACTION = "\"urn:Belkin:service:insight:1#GetInsightParams\"";
    private final String INSIGHT_PARAMS_DATA = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:GetInsightParams xmlns:u=\"urn:Belkin:service:insight:1\"></u:GetInsightParams></s:Body></s:Envelope>";

    public enum WemoDevice {

        SWITCH,
        INSIGHT
    }

    WemoDevice device;
    String host;
    ArrayList<Integer> ports;
    Integer preferredPort;

    public WemoManager(WemoDevice device, String host, ArrayList<Integer> ports) {

        this.device = device;
        this.host = host;
        this.ports = new ArrayList<>(ports);
        this.preferredPort = null;
    }

    public WemoManager(WemoDeviceConfig config) {

        this(config.getType().getWemoDevice(), config.getIpAddress(), config.getPorts());
    }

    private Integer getPortToTry(HashSet<Integer> triedPorts) {

        if (triedPorts == null)
            triedPorts = new HashSet<>(0);

        if (this.preferredPort != null && !triedPorts.contains(this.preferredPort))
            return this.preferredPort;

        if (this.ports == null)
            return null;

        for (Integer port : this.ports) {
            if (!triedPorts.contains(port)) {
                return port;
            }
        }

        return null;
    }

    /**
     * Return the state of the Wemo device. An integer as defined in Wemo specs.
     *
     * @return 0 means OFF, 1 means ON, 8 means STANDBY in case of errors NULL
     * is returned
     */
    public Integer getBinaryState() {

        CloseableHttpResponse res = null;
        String value = null;

        final HashSet<Integer> triedPorts = new HashSet<>();
        boolean finished = false;
        Integer currentPort = getPortToTry(triedPorts);

        do {

            if (currentPort == null) {

                finished = true;
                break;
            }

            try {

                HttpPost httpPost = new HttpPost("http://"
                        + this.host + ":"
                        + currentPort
                        + "/upnp/control/basicevent1");

                httpPost.setHeader("Content-type", "text/xml; charset=\"utf-8\"");
                httpPost.setHeader("SOAPACTION", GET_BINARY_STATE_ACTION);

                httpPost.setEntity(new ByteArrayEntity(
                        GET_BINARY_STATE_DATA.getBytes("UTF8")));

                res = HttpClients.createDefault().execute(httpPost);

                final HttpEntity entity = res.getEntity();
                value = EntityUtils.toString(entity, "UTF8");
                EntityUtils.consume(entity);

                finished = true;
            }
            catch(ConnectException ex) {

                triedPorts.add(currentPort);
                currentPort = getPortToTry(triedPorts);
            }
            catch (IOException | ParseException exx) {

                l.error("Error while retrieving binary state.", exx);

            }
            finally {

                if (currentPort != null)
                    this.preferredPort = currentPort;

                if (res != null) {

                    try {
                        res.close();
                    } catch (Exception ex) {
                    }
                }
            }

        } while (finished != true);

        if (value != null) {

            Pattern p = Pattern.compile("<BinaryState>(\\d?)</BinaryState>");
            Matcher m = p.matcher(value);

            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        return null;
    }

    /**
     * Set the state of the Wemo device. An integer as defined in Wemo specs.
     *
     * @param state 0 means OFF, 1 means ON
     * @return true if communication was successful
     */
    public boolean setBinaryState(Integer state) {

        CloseableHttpResponse res = null;
        String value = null;

        final HashSet<Integer> triedPorts = new HashSet<>();
        boolean finished = false;
        Integer currentPort = getPortToTry(triedPorts);

        do {

            if (currentPort == null) {

                finished = true;
                break;
            }

            try {

                HttpPost httpPost = new HttpPost("http://"
                        + this.host + ":"
                        + currentPort
                        + "/upnp/control/basicevent1");

                httpPost.setHeader("Content-type", "text/xml; charset=\"utf-8\"");
                httpPost.setHeader("SOAPACTION", SET_BINARY_STATE_ACTION);

                httpPost.setEntity(new ByteArrayEntity(
                        (state == 1 ? SET_BINARY_STATE_DATA_ON : SET_BINARY_STATE_DATA_OFF).getBytes("UTF8")));

                res = HttpClients.createDefault().execute(httpPost);

                final HttpEntity entity = res.getEntity();
                value = EntityUtils.toString(entity, "UTF8");
                EntityUtils.consume(entity);

                finished = true;
            }
            catch(ConnectException ex) {

                triedPorts.add(currentPort);
                currentPort = getPortToTry(triedPorts);
            }
            catch (IOException | ParseException ex) {

                l.error("Error while setting binary state.", ex);
            }
            finally {

                if (currentPort != null)
                    this.preferredPort = currentPort;

                if (res != null) {

                    try {
                        res.close();
                    } catch (Exception ex) {
                    }
                }
            }

        } while (finished != true);

        if (value != null) {

            Pattern p = Pattern.compile("<BinaryState>(\\d?)</BinaryState>");
            Matcher m = p.matcher(value);

            if (m.find()) {
                return (Integer.parseInt(m.group(1)) == state);
            }
        }

        return false;
    }

    /**
     * Return the state of the Wemo Insight.
     *
     * It is a pipe separated value with these meanings:
     *
     * 1. State (0 = off, 1 = on, 8 = stand-by); 2. Unix timestamp of last time
     * it changed state; (if currently on, when it was turned on. if currently
     * off, last time it was on.); 3. Seconds it has been on since getting
     * turned on (0 if off); 4. Seconds it has been on today; 5. Number of
     * seconds it has been on over the past two weeks; 6. A constant, is always
     * 1209600, this is two weeks in seconds equals to the time window for
     * average time on per day and average instantaneous power calculations; 7.
     * Average power (W); 8. Instantaneous power (mW); 9. Energy used today (mW
     * minutes); 10. Energy used over past two weeks (mW-minutes); 11.
     * Reserved/unknown.
     *
     * Example of output:
     * "1|1449655736|9102|38433|875000|1209600|23|22565|13195213|317266024|3000"
     *
     * @return the state string or null if an error occurred
     */
    public String getInsightParams() {

        if (device != WemoDevice.INSIGHT) {
            return null;
        }

        CloseableHttpResponse res = null;
        String value = null;

        final HashSet<Integer> triedPorts = new HashSet<>();
        boolean finished = false;
        Integer currentPort = getPortToTry(triedPorts);

        do {

            if (currentPort == null) {

                finished = true;
                break;
            }

            try {

                HttpPost httpPost = new HttpPost("http://"
                        + this.host + ":"
                        + currentPort
                        + "/upnp/control/insight1");

                httpPost.setHeader("Content-type", "text/xml; charset=\"utf-8\"");
                httpPost.setHeader("SOAPACTION", INSIGHT_PARAMS_ACTION);

                httpPost.setEntity(new ByteArrayEntity(
                        INSIGHT_PARAMS_DATA.getBytes("UTF8")));

                res = HttpClients.createDefault().execute(httpPost);

                final HttpEntity entity = res.getEntity();
                value = EntityUtils.toString(entity, "UTF8");
                EntityUtils.consume(entity);

                finished = true;
            }
            catch(ConnectException ex) {

                triedPorts.add(currentPort);
                currentPort = getPortToTry(triedPorts);
            }
            catch (IOException | ParseException ex) {

                l.error("Error while retrieving insight params.", ex);
            }
            finally {

                if (currentPort != null)
                    this.preferredPort = currentPort;

                if (res != null) {

                    try {
                        res.close();
                    } catch (Exception ex) {
                    }
                }
            }

        } while (finished != true);

        if (value != null) {

            Pattern p = Pattern.compile("<InsightParams>(.*?)</InsightParams>");
            Matcher m = p.matcher(value);

            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }
}
