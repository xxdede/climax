package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class WemoDeviceConfig implements Serializable {

    public enum Type {

        @SerializedName("switch")
        SWITCH(0),
        @SerializedName("insight")
        INSIGHT(1);

        private final int v;

        private Type(int v) {
            this.v = v;
        }
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private String ipAddress;
    private Integer port;
    private Type type;

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
