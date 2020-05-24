package it.tidal.config.utils;

import java.time.LocalDateTime;

public class GenericIntakeDetector extends IntakeDetector {

    protected String name;
    protected long timestamp;
    protected double temperature;
    protected Integer humidity;
    protected Integer co2;
    protected boolean open;

    public GenericIntakeDetector(String name) {

        this.name = name;
        this.timestamp = Utility.timestamp(LocalDateTime.now());
        this.temperature = 0.0;
        this.humidity = 0;
        this.co2 = 0;
        this.open = false;
    }

    public void mergeDataFromOpenClosedQueryable(OpenClosedQueryable oc, String optionalName) {

        this.open = oc.isOpen();

        if (optionalName != null)
            this.detectorName = optionalName;
    }

    public void mergeDataFromAdvancedTemperatureSensor(AdvancedTemperatureSensor ats, String optionalName) {

        this.timestamp = ats.getTimestamp();
        this.temperature = ats.getTemperature();
        this.humidity = ats.getHumidity();
        this.co2 = ats.getHumidity();
        this.perceived = ats.getPerceived();
        this.illness = ats.getIllness();

        if (optionalName != null)
            this.sensorName = optionalName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Integer getHumidity() {
        return humidity;
    }

    @Override
    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    @Override
    public Integer getCo2() {
        return co2;
    }

    @Override
    public void setCo2(Integer co2) {
        this.co2 = co2;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
