package it.tidal.config.utils;

public abstract class IntakeDetector extends AdvancedTemperatureSensor implements OpenClosedQueryable {

    protected String sensorName;
    protected String detectorName;

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getDetectorName() {
        return detectorName;
    }

    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }
}
