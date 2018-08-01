package it.tidal.climax.config;

import it.tidal.climax.config.GenericDeviceConfig.OperationMode;
import it.tidal.config.utils.Hour;
import it.tidal.config.utils.WeekDay;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
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

    public OperationMode getDeviceConfig(String deviceName, LocalDateTime now) {
        return getDeviceConfig(deviceName, now, now.minusDays(7));
    }

    public OperationMode getDeviceConfig(String deviceName, LocalDateTime now, LocalDateTime safeGuard) {

        if (deviceName == null || now == null || safeGuard == null) {
            return OperationMode.NOT_SPECIFIED;
        }

        do {

            final WeekDay wd = WeekDay.fromLocalDateTime(now);
            Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>> ws = null;

            if (weeklySchedule != null) {
                ws = weeklySchedule.get(wd);

                if (ws == null && weeklyDefault != null) {
                    ws = weeklySchedule.get(weeklyDefault);
                }
            }

            final Map<Hour, GenericDeviceConfig.OperationMode> hm = (ws != null ? ws.get(deviceName) : null);

            if (hm != null) {

                OperationMode om = hm.get(Hour.fromLocalDateTime(now));

                if (om != null) {
                    return om;
                }
            }

            now = now.minusMinutes(1);

        } while (now.compareTo(safeGuard) > 0);

        return OperationMode.NOT_SPECIFIED;
    }

    public static ProgramConfig findSuitableConfig(List<ProgramConfig> cfgs, LocalDateTime now) {

        if (cfgs == null) {
            return null;
        }

        if (now == null) {
            now = LocalDateTime.now();
        }

        ProgramConfig candidate = null;

        for (ProgramConfig cfg : cfgs) {

            if (!cfg.isActive()) {
                continue;
            }

            if (cfg.start != null && now.compareTo(cfg.start) < 0) {
                continue;
            }

            if (cfg.end != null && now.compareTo(cfg.end) > 0) {
                continue;
            }

            if (candidate == null
                    || (cfg.getPriority() > candidate.getPriority())) {
                candidate = cfg;
            }
        }

        return candidate;
    }
}
