package it.tidal.climax.extensions.managers;

import it.tidal.climax.config.CoolAutomationDeviceConfig;
import it.tidal.climax.extensions.data.CoolAutomation;
import it.tidal.logging.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.telnet.TelnetClient;

/**
 * CoolAutomation Extensions. Connects to and queries CoolAutomation devices.
 *
 * @author dede
 */
public class CoolAutomationManager {

    private static Log l = Log.prepare(CoolAutomationManager.class.getSimpleName());

    private final String deviceName;
    private final String deviceIpAddress;
    private final Integer devicePort;
    private final String lineId;
    private TelnetClient telnet;

    public CoolAutomationManager(String deviceName, String deviceIpAddress,
            Integer devicePort, String lineId) {

        this.deviceName = deviceName;
        this.deviceIpAddress = deviceIpAddress;
        this.devicePort = devicePort;
        this.lineId = lineId;
        this.telnet = null;
    }

    public static CoolAutomationManager getInstance(CoolAutomationDeviceConfig device) {

        if (device == null) {

            l.error("No device specified for CoolAutomation");
            return null;
        }

        return new CoolAutomationManager(device.getName(),
                device.getIpAddress(),
                device.getPort(),
                device.getLineId());
    }

    public boolean connect() {

        return connect(false);
    }

    public boolean connect(boolean reset) {

        if (reset || telnet == null) {

            try {

                if (telnet != null && telnet.isConnected()) {
                    telnet.disconnect();
                }
            } catch (IOException ex) {
            }

            telnet = new TelnetClient();
        }

        try {

            if (!telnet.isConnected()) {
                telnet.connect(this.deviceIpAddress, this.devicePort);
            }
        } catch (IOException ex) {

            telnet = null;
            return false;
        }

        return telnet.isConnected();
    }

    public boolean disconnect() {

        if (telnet == null) {
            return false;
        }

        if (!telnet.isConnected()) {
            return true;
        }

        try {

            telnet.disconnect();
        } catch (IOException ex) {

            return false;
        }

        return !telnet.isConnected();
    }

    private boolean _writeToStream(String cmd) {

        try {

            OutputStream out = telnet.getOutputStream();

            out.write((cmd + "\r\n").getBytes());
            out.flush();

            //l.debug("Sent '{}'", cmd);
            return true;
        } catch (IOException ex) {

            l.error(ex, "Error while writing to telnet socket.");
            return false;
        }
    }

    private String _readFromStream() {

        try {

            InputStream in = telnet.getInputStream();

            byte[] buff = new byte[1024];
            int read = 0;
            int waitAvailable = 0;

            StringBuilder receivedString = new StringBuilder(64);

            do {

                if (in.available() == 0) {

                    if (waitAvailable >= 3) {
                        break;
                    }

                    final int r = receivedString.length();

                    if (r > 0 && receivedString.charAt(r - 1) == '>') {
                        break;
                    }

                    waitAvailable++;
                    Thread.sleep(250);
                } else {

                    waitAvailable = 0;

                    read = in.read(buff);

                    if (read > 0) {
                        receivedString.append(new String(buff, 0, read));
                    }
                }
            } while (read >= 0);

            if (receivedString.length() > 0) {

                final String ret = receivedString.toString();
                //l.trace("Received '{}'", ret);
                return ret;
            } else {

                l.debug("Received nothing on telnet socket.");
                return "";
            }
        } catch (IOException | InterruptedException ex) {

            l.error(ex, "Error while reading from telnet socket.");
            return null;
        }
    }

    private static boolean isOk(String response) {

        // Checks wheter last character is ">" so the server
        // is waiting for input and commands by us
        if (response == null || response.isEmpty()) {
            return false;
        }

        return response.endsWith(">");
    }

    public boolean writeToStream(String cmd) {

        return _writeToStream(cmd);
    }

    public boolean readFromStream() {

        return isOk(_readFromStream());
    }

    public CoolAutomation getDeviceData() {

        boolean ok = true;

        if (ok) {
            ok = connect();
        }
        if (ok) {
            ok = readFromStream();
        }
        if (ok) {
            ok = writeToStream("ls " + lineId);
        }

        String response = _readFromStream();

        if (ok) {
            ok = isOk(response);
        }

        disconnect();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }

        if (!ok) {
            return null;
        }

        return new CoolAutomation(deviceName, lineId, response);
    }

    public CoolAutomation setAll(CoolAutomation data,
            CoolAutomation.OpMode opMode,
            CoolAutomation.FanSpeed fanSpeed,
            CoolAutomation.Status status,
            int delta) {

        l.debug("Setting all ({}, {}, {}, {})",
                opMode, fanSpeed, status, delta);

        Long prevTargetTemperature = Math.round(data.getSetTemperature());
        Long targetTemperature = Math.round(data.getRoomTemperature()) + delta;
        CoolAutomation newData = new CoolAutomation();

        newData.setName(data.getName());
        newData.setLineId(data.getLineId());
        newData.setStatus(data.getStatus());
        newData.setSetTemperature(data.getSetTemperature());
        newData.setRoomTemperature(data.getRoomTemperature());
        newData.setFanSpeed(data.getFanSpeed());
        newData.setOpMode(data.getOpMode());
        newData.setFailCode(data.getFailCode());

        boolean ok = true;
        boolean somethingDone = false;

        if (ok) {
            ok = connect();
        }
        if (ok) {
            ok = readFromStream();
        }

        if (ok && !data.getOpMode().equals(opMode)) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }

            ok = writeToStream(opMode.toString().toLowerCase() + " " + lineId);
            newData.setOpMode(opMode);
            somethingDone = true;
        }

        if (ok && !data.getFanSpeed().equals(fanSpeed)) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }

            ok = writeToStream("fspeed " + lineId + " "
                    + fanSpeed.toString().toLowerCase().substring(0, 1));
            newData.setFanSpeed(fanSpeed);
            somethingDone = true;
        }

        if (ok && !prevTargetTemperature.equals(targetTemperature)) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }

            ok = writeToStream("temp " + lineId
                    + " " + targetTemperature.intValue());
            newData.setSetTemperature(0.0 + targetTemperature);
            somethingDone = true;
        }

        if (ok && !data.getStatus().equals(status)) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }

            ok = writeToStream(status.toString().toLowerCase() + " " + lineId);
            newData.setStatus(status);
            somethingDone = true;
        }

        disconnect();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }

        if (ok) {

            if (!somethingDone) {
                l.debug("Nothing done, everything was already configured!");
            }
            return newData;
        } else {
            return null;
        }
    }
}
