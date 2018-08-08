package it.tidal.climax.extensions.data;

import it.tidal.climax.database.mapping.RoomStatus;
import java.io.Serializable;

/**
 * NetAtmo Data Extensions. Contains status about a NetAtmo module.
 *
 * @author dede
 */
public class NetAtmo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private double temperature;
    private Integer humidity;
    private Integer co2;

    public NetAtmo(String name, double temperature) {

        this.name = name;
        this.temperature = temperature;
        this.humidity = null;
        this.co2 = null;
    }

    public NetAtmo(String name, double temperature, Integer humidity, Integer co2) {

        this.name = name;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co2 = co2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Integer getCo2() {
        return co2;
    }

    public void setCo2(Integer co2) {
        this.co2 = co2;
    }

    public RoomStatus toRoomStatus(long ts) {

        return new RoomStatus(name, ts, temperature, humidity, co2, temperature, null);
    }
}
