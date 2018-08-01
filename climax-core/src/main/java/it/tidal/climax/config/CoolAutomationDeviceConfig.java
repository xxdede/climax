package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CoolAutomationDeviceConfig {

    public enum Status {

        @SerializedName("open")
        OPEN(0),
        @SerializedName("closed")
        CLOSED(1),
        @SerializedName("managed")
        MANAGED(2);

        private final int v;

        private Status(int v) {
            this.v = v;
        }
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private String ipAddress;
    private Integer port;
    private String lineId;
    private Status status;
    private List<GenericDeviceConfig> related;

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<GenericDeviceConfig> getRelated() {
        return related;
    }

    public void setRelated(List<GenericDeviceConfig> related) {
        this.related = related;
    }
}
