package it.tidal.climax.config;

import it.tidal.config.utils.Hour;
import it.tidal.config.utils.WeekDay;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

public class ProgramConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private boolean active;
    private int priority = 0;
    private LocalDateTime start;
    private LocalDateTime end;
    private int temperatureOffset = 0;

    private WeekDay weeklyDefault;
    private Map<WeekDay, Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>>> weeklySchedule;

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

    public int getTemperatureOffset() {
        return temperatureOffset;
    }

    public void setTemperatureOffset(int temperatureOffset) {
        this.temperatureOffset = temperatureOffset;
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
