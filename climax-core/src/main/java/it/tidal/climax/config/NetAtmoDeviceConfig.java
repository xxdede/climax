package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class NetAtmoDeviceConfig implements Serializable {

    public enum Type {

        @SerializedName("thermostat")
        THERMOSTAT(0),
        @SerializedName("weather-station")
        WEATHER_STATION(1),
        @SerializedName("weather-indoor-module")
        WEATHER_INDOOR_MODULE(2),
        @SerializedName("weather-outdoor-module")
        WEATHER_OUTDOOR_MODULE(3);

        private final int v;

        private Type(int v) {
            this.v = v;
        }
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private Type type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
