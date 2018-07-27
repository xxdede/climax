package it.tidal.climax.config;

public class CoolAutomationDeviceConfig {

    private static final long serialVersionUID = 1L;

    private String name;
    private String ipAddress;
    private Integer port;
    private String lineId;
    private AirIntakeConfig airIntake;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public AirIntakeConfig getAirIntake() {
        return airIntake;
    }

    public void setAirIntake(AirIntakeConfig airIntake) {
        this.airIntake = airIntake;
    }
}
