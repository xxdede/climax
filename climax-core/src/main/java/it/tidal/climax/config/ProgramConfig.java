package it.tidal.climax.config;

import it.tidal.config.utils.Hour;
import it.tidal.config.utils.WeekDay;
import it.tidal.logging.Log;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

public class ProgramConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Log l = Log.prepare(ProgramConfig.class.getSimpleName());

    private String name;
    private boolean active;
    private int priority = 0;
    private LocalDateTime start;
    private LocalDateTime end;
    private double minPerceivedTemperature = 32.5;
    private double maxPerceivedTemperature = 34.0;
    private int minCo2Value = 650;
    private int maxCo2Value = 1000;

    private WeekDay weeklyDefault;
    private Map<WeekDay, Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>>> weeklySchedule;

    public boolean isValid() {

        if (name == null) {

            l.warn("Invalid program config, it has no name...");
            return false;
        }

        if (start == null) {

            l.warn("Error in program config \"{}\", no start date specified...", name);
            return false;
        }

        if (end == null) {

            l.warn("Error in program config \"{}\", no end date specified...", name);
            return false;
        }

        if (start.compareTo(end) > 0) {

            l.warn("Error in program config \"{}\", start date is after end date...", name);
            return false;
        }

        if (minPerceivedTemperature > maxPerceivedTemperature) {

            l.warn("Error in program config \"{}\", min perceived temperature is higher than max...", name);
            return false;
        }

        if (minCo2Value > maxCo2Value) {

            l.warn("Error in program config \"{}\", min co2 value is higher than max...", name);
            return false;
        }

        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public double getMinPerceivedTemperature() {
        return minPerceivedTemperature;
    }

    public void setMinPerceivedTemperature(double minPerceivedTemperature) {
        this.minPerceivedTemperature = minPerceivedTemperature;
    }

    public double getMaxPerceivedTemperature() {
        return maxPerceivedTemperature;
    }

    public void setMaxPerceivedTemperature(double maxPerceivedTemperature) {
        this.maxPerceivedTemperature = maxPerceivedTemperature;
    }

    public int getMinCo2Value() {
        return minCo2Value;
    }

    public void setMinCo2Value(int minCo2Value) {
        this.minCo2Value = minCo2Value;
    }

    public int getMaxCo2Value() {
        return maxCo2Value;
    }

    public void setMaxCo2Value(int maxCo2Value) {
        this.maxCo2Value = maxCo2Value;
    }

    public WeekDay getWeeklyDefault() {
        return weeklyDefault;
    }

    public void setWeeklyDefault(WeekDay weeklyDefault) {
        this.weeklyDefault = weeklyDefault;
    }

    public Map<WeekDay, Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>>> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(Map<WeekDay, Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>>> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }
}
