package it.tidal.climax.core;

import com.google.gson.annotations.SerializedName;
import it.tidal.climax.config.Config;
import it.tidal.climax.config.Config.Variant;
import it.tidal.climax.config.CoolAutomationDeviceConfig;
import it.tidal.climax.config.GenericDeviceConfig;
import it.tidal.climax.config.GenericDeviceConfig.OperationMode;
import it.tidal.climax.database.mapping.EnergyStatus;
import it.tidal.climax.database.mapping.HVACStatus;
import it.tidal.climax.database.mapping.RoomStatus;
import it.tidal.climax.extensions.data.ClimaxPack;
import it.tidal.climax.extensions.data.CoolAutomation;
import it.tidal.climax.extensions.data.CoolAutomation.FanSpeed;
import it.tidal.climax.extensions.data.CoolAutomation.OpMode;
import it.tidal.climax.extensions.data.CoolAutomation.Status;
import it.tidal.climax.extensions.data.SolarEdgeEnergy;
import it.tidal.climax.extensions.managers.ConfigManager;
import it.tidal.climax.extensions.managers.CoolAutomationManager;
import it.tidal.climax.extensions.managers.DatabaseManager;
import it.tidal.climax.extensions.managers.NetAtmoManager;
import it.tidal.climax.extensions.managers.SolarEdgeManager;
import it.tidal.config.utils.DeviceFamiliable;
import it.tidal.config.utils.Utility;
import it.tidal.logging.Log;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Operation {

    public enum Program {

        @SerializedName("default")
        DEFAULT(0),
        @SerializedName("shutdown")
        SHUTDOWN(1),
        @SerializedName("only-solaredge")
        ONLY_SOLAREDGE(2),
        @SerializedName("co2")
        CO2(3);

        private final int v;

        private Program(int v) {
            this.v = v;
        }

        public static Program fromString(String s) {

            for (Program p : Program.values()) {
                if (p.getSlug().equals(s)) {
                    return p;
                }
            }

            return null;
        }

        public String getSlug() {
            return Utility.slugFromAnnotation(this);
        }
    }

    private static Log l = Log.prepare(Operation.class.getSimpleName());

    public static void execute(String[] args, Program prg, Config cfg,
            LocalDateTime now) {

        switch (prg) {

            case DEFAULT:
                defaultProgram(cfg, now);
                break;
            case SHUTDOWN:
                shutdownProgram(cfg);
                break;
            case CO2: {

                Integer upperBound = null;
                Integer lowerBound = null;

                try {

                    upperBound = Integer.parseInt(args[2]);
                    lowerBound = Integer.parseInt(args[3]);
                }
                catch(Exception ex) {}

                co2Program(cfg, now, lowerBound, upperBound);
                break;
            }
            case ONLY_SOLAREDGE: {

                Long start = null;
                Long end = null;

                if (args.length > 3) {

                    start = Long.parseLong(args[2]);
                    end = Long.parseLong(args[3]);

                    if (start > end) {
                        throw new IllegalArgumentException("Start ts (" + start
                                + ") must precede end ts (" + end + ").");
                    }
                }

                solarEdgeProgram(cfg, start, end, now);
                break;
            }
        }
    }

    private static void defaultProgram(Config cfg, LocalDateTime now) {

        final LocalDateTime normalizedNow = Utility.normalizeToTenMinutes(now);
        final long normalizedNowTs = Utility.timestamp(normalizedNow);
        final DatabaseManager dbm = DatabaseManager.getInstance(cfg.getMySQL());
        final HashMap<String, ClimaxPack> cps = new HashMap<>();

        // Energy statuses
        l.debug("Retrieving energy status...");
        TreeMap<LocalDateTime, EnergyStatus> ess;
        ess = dbm.splitQuarterOfHourToTenMinutesEnergyStatuses(
                dbm.retrieveLastEnergyStatus(4));

        // HVAC statuses (pre: before last exec, post: after last exec)
        l.debug("Retrieving hvac status...");
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPre;
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPost;

        hssPre = new HashMap<>(cfg.getCoolAutomation().getDevices().size());
        hssPost = new HashMap<>(cfg.getCoolAutomation().getDevices().size());

        // NetAtmo manager
        l.debug("Retrieving thermo status...");
        NetAtmoManager nam = NetAtmoManager.getInstance(cfg.getNetAtmo(), true);

        for (CoolAutomationDeviceConfig cadc
                : cfg.getCoolAutomation().getDevices()) {

            final String devName = cadc.getName();
            final String dbName = cadc.getDbName();

            // Check if climax pack exists
            ClimaxPack cp = cps.get(devName);

            if (cp == null) {

                cp = new ClimaxPack(devName);
                cps.put(devName, cp);
            }

            // Current HVAC status
            final CoolAutomationManager cam = CoolAutomationManager.getInstance(cadc);
            cp.setCurrentHVACConfig(cam.getDeviceData());
            cam.disconnect();

            // Previous HVAC statuses
            final TreeMap<LocalDateTime, HVACStatus> tempHssPre;
            final TreeMap<LocalDateTime, HVACStatus> tempHssPost;

            tempHssPre = dbm.retrieveLastHVACStatus(dbName, -1, 6);
            tempHssPost = dbm.retrieveLastHVACStatus(dbName, 0, 6);

            hssPre.put(devName, tempHssPre);
            hssPost.put(devName, tempHssPost);

            // Update climax pack
            cp.setLastHVACStatus(tempHssPost.lastEntry().getValue());

            // Room statuses (previous and current)
            final DeviceFamiliable itp = ConfigManager.findDevice(cfg, cadc.findRelated(GenericDeviceConfig.Role.INSIDE_TEMPERATURE_PROVIDER));

            if (itp != null) {

                final TreeMap<LocalDateTime, RoomStatus> tempRss;
                tempRss = dbm.retrieveLastRoomStatus(itp.getDbName(), 6);

                // Update climax pack
                cp.updateWithRoomStatuses(tempRss);
                cp.setRoomStatus(nam.getData(itp.getName()).toRoomStatus(normalizedNowTs));
            }

            // Outside status
            final DeviceFamiliable otp = ConfigManager.findDevice(cfg, cadc.findRelated(GenericDeviceConfig.Role.OUTSIDE_TEMPERATURE_PROVIDER));

            if (otp != null) {

                // Update climax pack
                cp.setOutsideStatus(nam.getData(otp.getName()).toRoomStatus(normalizedNowTs));
            }

            // Intake status
            final DeviceFamiliable ktp = ConfigManager.findDevice(cfg, cadc.findRelated(GenericDeviceConfig.Role.INTAKE_TEMPERATURE_PROVIDER));

            if (ktp != null) {

                // Update climax pack
                cp.setIntakeStatus(nam.getData(ktp.getName()).toRoomStatus(normalizedNowTs));
            }
        }

        if (false) {
            // TODO: check consumption and define how many device we can turn on
        }

        // Cycling again through device to decide what to do
        for (CoolAutomationDeviceConfig cadc
                : cfg.getCoolAutomation().getDevices()) {

            final String devName = cadc.getName();
            final CoolAutomationManager cam = CoolAutomationManager.getInstance(cadc);
            final OperationMode desiredOpMode = ConfigManager.suitableOperationMode(cfg, devName, normalizedNow);

            final ClimaxPack cp = cps.get(devName);
            final CoolAutomation current = cp.getCurrentHVACConfig();
            CoolAutomation desired = null;

            switch (desiredOpMode) {

                case NOT_SPECIFIED:

                    l.warn("Device \"{}\" has no suitable operation mode?!", devName);
                    break;

                case DISABLED:

                    if (current.getStatus() != Status.OFF) {

                        desired = current.duplicate();
                        desired.setStatus(Status.OFF);
                    }

                    break;

                case ENABLED:

                    l.info("Device \"{}\" is {}, forcing it (case {})!", devName, current.getStatus(), desiredOpMode);
                    desired = desiredSetup(true, cp);
                    break;

                case OPERATE_IF_ON:

                    if (current.getStatus() == Status.ON) {

                        l.info("Device \"{}\" is {}, managing it (case {})...", devName, current.getStatus(), desiredOpMode);
                        desired = desiredSetup(false, cp);

                    } else {

                        l.info("Device \"{}\" is {} so do not touching it (case {})...", devName, current.getStatus(), desiredOpMode);
                    }

                    break;

                case OPERATE_IF_ON_OR_SELF_OFF:

                    boolean shouldOperate = false;

                    if (current.getStatus() == Status.ON) {

                        l.info("Device \"{}\" is {}, managing it (case {})!", devName, current.getStatus(), desiredOpMode);
                        shouldOperate = true;

                    } else {

                        final Map.Entry<LocalDateTime, HVACStatus> hs = hssPost.get(devName).lastEntry();

                        if (hs == null) {

                            l.warn("Unable to find last status for device \"{}\" and now it is {}, do not touching it (case {})...", devName, current.getStatus(), desiredOpMode);

                        } else {

                            // Check last recorded value date & time
                            final LocalDateTime lowerBound = normalizedNow.minusMinutes(15);

                            if (lowerBound.compareTo(hs.getKey()) > 0) {

                                l.warn("Last status for device \"{}\" is more than 15 minutes old and now it is {}, do not touching it (case {})...", devName, current.getStatus(), desiredOpMode);

                            } else if (hs.getValue().getStatusEnum() == current.getStatus()) {

                                l.info("Last status for device \"{}\" was {} and now it is {}, managing it (case {})!", devName, hs.getValue().getStatusEnum(), current.getStatus(), desiredOpMode);
                                shouldOperate = true;

                            } else {

                                l.info("Last status for device \"{}\" was {} and now it is {}, do not touching it (case {})...", devName, hs.getValue().getStatusEnum(), current.getStatus(), desiredOpMode);
                            }
                        }
                    }

                    if (shouldOperate) {

                        desired = desiredSetup(false, cp);
                    }

                    break;

                case OPERATE_AUTO:

                    l.info("Device \"{}\" is {}, managing it (case {})!", devName, current.getStatus(), desiredOpMode);
                    desired = desiredSetup(false, cp);
                    break;

                default:

                    l.debug("Device \"{}\" has an unexpected operation mode (i.e. {}) !", devName, desiredOpMode);
                    break;
            }

            cam.disconnect();

            // DEBUG: printing dedired
            l.debug("Desired: " + (desired != null ? desired.getDescription() : current.getDescription() + " (no changes)") + ".");
        }

        // TODO: cycle again on devices to check if what we want is actually running
    }

    private static CoolAutomation desiredSetup(boolean forceOn, ClimaxPack cp) {

        final CoolAutomation current = cp.getCurrentHVACConfig();
        CoolAutomation desired = null;

        final RoomStatus rs = cp.getRoomStatus();

        l.debug("Perceived temperature {} for \"{}\"",
                Utility.americanDoubleFormatter.format(rs.getPerceived()),
                cp.getName());

        final double perceived = rs.getPerceived();
        final double targetPerceived = 33.0;
        final double deltaPerceived = perceived - targetPerceived;

        if (deltaPerceived < 0.5) {
            // TODO: Evaluate hysteresis
        } else if (deltaPerceived < 1) {
            // Cool, 0, LOW
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(0.0)
                    .changeFanSpeed(FanSpeed.LOW);
        } else if (deltaPerceived < 1.5) {
            // Cool, -1, LOW
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-1.0)
                    .changeFanSpeed(FanSpeed.LOW);
        } else if (deltaPerceived < 2) {
            // Cool, -1, MEDIUM
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-1.0)
                    .changeFanSpeed(FanSpeed.MEDIUM);
        } else if (deltaPerceived < 2.5) {
            // Cool, -1, HIGH
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-1.0)
                    .changeFanSpeed(FanSpeed.HIGH);
        } else if (deltaPerceived < 3) {
            // Cool, -2, LOW
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-2.0)
                    .changeFanSpeed(FanSpeed.LOW);
        } else if (deltaPerceived < 3.5) {
            // Cool, -2, MEDIUM
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-2.0)
                    .changeFanSpeed(FanSpeed.MEDIUM);
        } else if (deltaPerceived < 4) {
            // Cool, -3, LOW
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-3.0)
                    .changeFanSpeed(FanSpeed.LOW);
        } else if (deltaPerceived < 4.5) {
            // Cool, -3, MEDIUM
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-3.0)
                    .changeFanSpeed(FanSpeed.MEDIUM);
        } else {
            // Cool, -3, HIGH
            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeOpMode(OpMode.COOL)
                    .changeDelta(-3.0)
                    .changeFanSpeed(FanSpeed.HIGH);
        }

        // If is set "forceOn" it means that even if we believe
        // that device should be turned off we keep it going
        if (forceOn) {

            if (desired != null) {
                desired.setStatus(Status.ON);
            } else {
                desired = current.duplicate()
                        .changeStatus(Status.ON)
                        .changeOpMode(OpMode.COOL)
                        .changeDelta(0.0)
                        .changeFanSpeed(FanSpeed.LOW);
            }
        }

        return desired;
    }

    private static void shutdownProgram(Config cfg) {

        for (CoolAutomationDeviceConfig cadc
                : cfg.getCoolAutomation().getDevices()) {

            final String devName = cadc.getName();

            try {

                // Check status of device
                final CoolAutomationManager cam;
                final CoolAutomation previous;

                cam = CoolAutomationManager.getInstance(cadc);
                previous = cam.getDeviceData();

                l.debug("Turning device \"" + devName + "\" off...");

                if (previous.getStatus() == Status.OFF) {

                    l.info("Device \"" + devName + "\" already off...");

                } else if (!Variant.LOG_ONLY.equals(cfg.getVariant())) {

                    CoolAutomation current = cam.setAll(previous,
                            previous.getOpMode(), previous.getFanSpeed(),
                            Status.OFF, 0);

                    if (current != null) {
                        l.info("Device \"" + devName + "\" turned off.");
                    } else {
                        l.error("Something went wrong while turning off"
                                + " device \"{}\"!", devName);
                    }

                    // Shutdown usually is triggered and not scheduled
                    // so we do not add its data to database
                }

                cam.disconnect();
            } catch (Exception ex) {

                l.error("A problem occurred while turning off device \"" + devName + "\"!", ex);
            }
        }
    }

    private static void solarEdgeProgram(Config cfg,
            Long start, Long end, LocalDateTime now) {

        SolarEdgeEnergy energy = null;

        try {

            final SolarEdgeManager sem;
            sem = SolarEdgeManager.getInstance(cfg.getSolarEdge());

            if (start == null || end == null) {

                energy = sem.getEnergyDetails(now);
                l.debug("Collected values with ts "
                        + energy.getFirstTimestamp() + ".");
            } else {

                energy = sem.getEnergyDetails(start, end);

                final int tot = energy.getMeters().size();
                l.debug("Collected " + tot + " value"
                        + (tot != 1 ? "s." : "."));
            }

        } catch (Exception ex) {

            l.error("An error occurred with SolarEdge!", ex);
        }

        try {

            if (Variant.LOG_ONLY.equals(cfg.getVariant())) {
                l.info(Utility.prettyJson(energy));
            } else {

                DatabaseManager dbm;
                dbm = DatabaseManager.getInstance(cfg.getMySQL());
                dbm.checkAndInsertSolarEdgeEnergy(energy);
                dbm.dispose();
            }

        } catch (Exception ex) {

            l.error("An error occurred while saving to DB!", ex);
        }
    }

    private static void co2Program(Config cfg, LocalDateTime now, Integer lowerBound, Integer upperBound) {

        final DatabaseManager dbm = DatabaseManager.getInstance(cfg.getMySQL());

        // Dispositivi coinvolti
        final String sourceDeviceName = "camera";
        final String destinationDeviceName = "Piano 1";

        if (lowerBound == null)
            lowerBound = 950;
        if (upperBound == null)
            upperBound = 1300;

        if (lowerBound >= upperBound)
            lowerBound = upperBound - 150;

        // Controllo nel database l'ultimo valore
        final TreeMap<LocalDateTime, RoomStatus> tempRss;
        tempRss = dbm.retrieveLastRoomStatus(sourceDeviceName, 1);

        if (tempRss == null || tempRss.size() < 1) {

            l.error("Cannot find source device status (name: {})!", sourceDeviceName);
            return;
        }

        final LocalDateTime time = tempRss.firstKey();
        final Duration elapsed = Duration.between(time, now);

        // Se è passata più di mezz'ora non va bene...
        if (elapsed.toMinutes() > 30) {

            l.error("Last room status is {} minutes old, bailing out!", elapsed.toMinutes());
            return;
        }

        final RoomStatus roomStatus = tempRss.get(time);
        boolean shouldBeActivated = false;
        boolean shouldBeDeactivated = false;

        if (roomStatus.getCo2() > upperBound)
            shouldBeActivated = true;
        else if (roomStatus.getCo2() < lowerBound)
            shouldBeDeactivated = true;
        else {

            l.info("Leaving everything as is (co2 is {}ppm)...", roomStatus.getCo2());
            return;
        }


        final CoolAutomationManager cam = CoolAutomationManager.getInstance(cfg.getCoolAutomation().find(destinationDeviceName));

        if (cam == null) {

            l.error("Cannot find destination device {}!", destinationDeviceName);
            return;
        }

        final CoolAutomation current = cam.getDeviceData();

        if (current == null) {

            l.error("Cannot find destination device status {}!", destinationDeviceName);
            return;
        }

        if (current.getStatus() == Status.ON && current.getOpMode() != OpMode.FAN) {

            l.info("HVAC is in {} mode, probably set manually... leaving it as is!", current.getOpMode());
            return;
        }

        CoolAutomation desired = null;

        if (current.getStatus() == Status.ON && shouldBeDeactivated) {

            desired = current.duplicate().changeStatus(Status.OFF);
        }
        else if ((current.getStatus() == Status.OFF && shouldBeActivated) || current.getOpMode() != OpMode.FAN) {

            desired = current.duplicate()
                    .changeStatus(Status.ON)
                    .changeFanSpeed(FanSpeed.LOW)
                    .changeOpMode(OpMode.FAN);
        }

        if (desired != null) {

            try {  Thread.sleep(2000); } catch (Exception ex) {}

            cam.setAll(current,
                    desired.getOpMode(),
                    desired.getFanSpeed(),
                    desired.getStatus(),
                    0);

            l.info("Change hvac, set it to {} (co2: {}ppm)!", desired.getStatus(), roomStatus.getCo2());
        }
        else {

            l.info("Nothing to do, hvac is already ok as is ({}, co2: {}ppm)!", current.getStatus(), roomStatus.getCo2());
        }
    }
}
