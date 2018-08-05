package it.tidal.climax.extensions.data;

import it.tidal.logging.Log;
import java.io.Serializable;

/**
 * CoolAutomation Data Extensions. Contains status about a CoolAutomation
 * device.
 *
 * @author dede
 */
public class CoolAutomation implements Serializable {

    private static Log l = Log.prepare(CoolAutomation.class.getSimpleName());

    private static final long serialVersionUID = 1L;

    public enum Status {

        OFF(0),
        ON(1);

        private final int v;

        private Status(int v) {
            this.v = v;
        }

        public String getName() {

            switch (v) {

                case 0:
                    return "OFF";
                case 1:
                    return "ON";
            }

            return "?";
        }

        public int getValue() {

            return v;
        }

        public static Status fromString(String s) {

            for (Status st : Status.values()) {
                if (st.getName().equalsIgnoreCase(s)) {
                    return st;
                }
            }

            return null;
        }

        public static Status fromInteger(Integer i) {

            if (i != null) {
                for (Status st : Status.values()) {
                    if (st.getValue() == i) {
                        return st;
                    }
                }
            }

            return null;
        }
    }

    public enum FanSpeed {

        LOW(0),
        MEDIUM(1),
        HIGH(2);

        private final int v;

        private FanSpeed(int v) {
            this.v = v;
        }

        public int getValue() {

            return v;
        }

        public String getName() {

            switch (v) {

                case 0:
                    return "Low";
                case 1:
                    return "Med";
                case 2:
                    return "High";
            }

            return "?";
        }

        public static FanSpeed fromString(String s) {

            for (FanSpeed fs : FanSpeed.values()) {
                if (fs.getName().equalsIgnoreCase(s)) {
                    return fs;
                }
            }

            return null;
        }

        public static FanSpeed fromInteger(Integer i) {

            if (i != null) {
                for (FanSpeed fs : FanSpeed.values()) {
                    if (fs.getValue() == i) {
                        return fs;
                    }
                }
            }

            return null;
        }
    }

    public enum OpMode {

        COOL(0),
        HEAT(1),
        AUTO(2),
        DRY(3),
        HAUX(4),
        FAN(5);

        private final int v;

        private OpMode(int v) {
            this.v = v;
        }

        public int getValue() {

            return v;
        }

        public String getName() {

            switch (v) {

                case 0:
                    return "Cool";
                case 1:
                    return "Heat";
                case 2:
                    return "Auto";
                case 3:
                    return "Dry";
                case 4:
                    return "Haux";
                case 5:
                    return "Fan";
            }

            return "?";
        }

        public static OpMode fromString(String s) {

            for (OpMode om : OpMode.values()) {
                if (om.getName().equalsIgnoreCase(s)) {
                    return om;
                }
            }

            return null;
        }

        public static OpMode fromInteger(Integer i) {

            if (i != null) {
                for (OpMode om : OpMode.values()) {
                    if (om.getValue() == i) {
                        return om;
                    }
                }
            }

            return null;
        }
    }

    private String name;
    private String lineId;
    private Status status;
    private double setTemperature;
    private double roomTemperature;
    private FanSpeed fanSpeed;
    private OpMode opMode;
    private String failCode;

    public CoolAutomation() {
    }

    public CoolAutomation(String name, String lineId, String responseLine) {

        this.name = name;
        this.lineId = lineId;

        if (responseLine == null || responseLine.isEmpty()) {
            return;
        }

        String[] tokens = responseLine.trim().split("\\s+");

        if (tokens.length > 0 && lineId == null) {
            this.lineId = tokens[0];
        }

        if (tokens.length > 1) {
            this.status = Status.fromString(tokens[1]);
        }

        if (tokens.length > 2) {
            this.setTemperature = parseCelsius(tokens[2]);
        }

        if (tokens.length > 3) {
            this.roomTemperature = parseCelsius(tokens[3]);
        }

        if (tokens.length > 4) {
            this.fanSpeed = FanSpeed.fromString(tokens[4]);
        }

        if (tokens.length > 5) {
            this.opMode = OpMode.fromString(tokens[5]);
        }

        if (tokens.length > 6) {
            this.failCode = tokens[6];
        }

        l.debug("{}: {}, {}, {}, {}, {}.",
                this.name,
                this.status, this.setTemperature,
                this.roomTemperature, this.fanSpeed,
                this.opMode, this.failCode);
    }

    public static double parseCelsius(String temperature) {

        if (temperature == null || temperature.isEmpty()) {
            return -1;
        }

        if (!temperature.endsWith("C")) {
            return -1;
        }

        temperature = temperature.substring(0, temperature.length() - 1);

        return Double.parseDouble(temperature);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getSetTemperature() {
        return setTemperature;
    }

    public void setSetTemperature(double setTemperature) {
        this.setTemperature = setTemperature;
    }

    public double getRoomTemperature() {
        return roomTemperature;
    }

    public void setRoomTemperature(double roomTemperature) {
        this.roomTemperature = roomTemperature;
    }

    public FanSpeed getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(FanSpeed fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    public OpMode getOpMode() {
        return opMode;
    }

    public void setOpMode(OpMode opMode) {
        this.opMode = opMode;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }
}
