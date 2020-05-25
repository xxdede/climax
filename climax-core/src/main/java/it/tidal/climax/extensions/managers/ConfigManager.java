package it.tidal.climax.extensions.managers;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import it.tidal.climax.config.Config;
import it.tidal.climax.config.GenericDeviceConfig;
import it.tidal.climax.config.ProgramConfig;
import it.tidal.config.utils.DeviceFamiliable;
import it.tidal.config.utils.Hour;
import it.tidal.config.utils.Utility;
import it.tidal.config.utils.WeekDay;
import it.tidal.gson.GsonFactory;
import it.tidal.logging.Log;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Configuration helper. Manage config file and retrieve parameters.
 *
 * @author dede
 */
public class ConfigManager {

    private static Log l = Log.prepare(ConfigManager.class.getSimpleName());

    public static <T> T read(Class<T> type, String path) {

        try {

            l.debug("Reading configs from: {}", path);

            BufferedReader reader = new BufferedReader(new FileReader(path));
            T object = GsonFactory.instance().fromJson(reader, type);

            return object;
        } catch (JsonIOException
                | JsonSyntaxException
                | FileNotFoundException ex) {

            l.error(ex, "A problem occurred while reading config...");
        }

        try {

            return type.newInstance();
        } catch (IllegalAccessException
                | InstantiationException ex) {
            l.error(ex, "A problem occurred while instantiating config...");
        }

        return null;
    }

    public static GenericDeviceConfig.OperationMode suitableOperationMode(ProgramConfig programConfig, String deviceName, LocalDateTime now) {

        if (programConfig == null) {
            return GenericDeviceConfig.OperationMode.NOT_SPECIFIED;
        }

        return suitableDeviceConfig(programConfig, deviceName, now, now.minusDays(7));
    }

    private static GenericDeviceConfig.OperationMode suitableDeviceConfig(ProgramConfig programConfig, String deviceName, LocalDateTime now, LocalDateTime safeGuard) {

        if (programConfig == null || deviceName == null || now == null || safeGuard == null) {
            return GenericDeviceConfig.OperationMode.NOT_SPECIFIED;
        }

        final WeekDay weeklyDefault = programConfig.getWeeklyDefault();
        final Map<WeekDay, Map<String, TreeMap<Hour, GenericDeviceConfig.OperationMode>>> weeklySchedule = programConfig.getWeeklySchedule();

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

                GenericDeviceConfig.OperationMode om = hm.get(Hour.fromLocalDateTime(now));

                if (om != null) {
                    return om;
                }
            }

            now = now.minusMinutes(1);

        } while (now.compareTo(safeGuard) > 0);

        return GenericDeviceConfig.OperationMode.NOT_SPECIFIED;
    }

    public static ProgramConfig suitableProgramConfig(List<ProgramConfig> cfgs, LocalDateTime now) {

        if (cfgs == null) {
            return null;
        }

        if (now == null) {
            now = LocalDateTime.now();
        }

        ProgramConfig candidate = null;

        for (ProgramConfig cfg : cfgs) {

            if (!cfg.isValid() || !cfg.isActive()) {
                continue;
            }

            if (cfg.getStart() != null && cfg.getEnd() != null
                    && cfg.getStart().compareTo(cfg.getEnd()) > 0) {

                l.error("Program '" + cfg.getName() + "' configuration error: start ("
                        + Utility.basicDateTimeFormatter.format(cfg.getStart()) + ") is after end ("
                        + Utility.basicDateTimeFormatter.format(cfg.getEnd()) + ")!");
            }

            if (cfg.getStart() != null && now.compareTo(cfg.getStart()) < 0) {
                continue;
            }

            if (cfg.getEnd() != null && now.compareTo(cfg.getEnd()) > 0) {
                continue;
            }

            if (candidate == null
                    || (cfg.getPriority() > candidate.getPriority())) {
                candidate = cfg;
            }
        }

        // Found the best candidate (matching day/time and with highest priority)
        return candidate;
    }

    public static DeviceFamiliable findDevice(Config cfg, GenericDeviceConfig deviceConfig) {

        if (cfg == null || deviceConfig == null) {
            return null;
        }

        return findDevice(cfg, deviceConfig.getName());
    }

    public static ArrayList<DeviceFamiliable> findAllDevices(Config cfg, List<GenericDeviceConfig> deviceConfigs) {

        ArrayList<DeviceFamiliable> res = new ArrayList<>();

        if (cfg == null || deviceConfigs == null) {

            return res;
        }

        for (GenericDeviceConfig gdc : deviceConfigs) {

            final DeviceFamiliable df = findDevice(cfg, gdc.getName());

            if (df != null)
                res.add(df);
        }

        return res;
    }

    public static DeviceFamiliable findDevice(Config cfg, String deviceName) {

        if (cfg == null || deviceName == null) {
            return null;
        }

        DeviceFamiliable d = null;

        if (cfg.getNetAtmo() != null) {
            d = cfg.getNetAtmo().find(deviceName);
        }

        if (d == null && cfg.getCoolAutomation() != null) {
            d = cfg.getCoolAutomation().find(deviceName);
        }

        if (d == null && cfg.getWemo() != null) {
            d = cfg.getWemo().find(deviceName);
        }

        return d;
    }
}
