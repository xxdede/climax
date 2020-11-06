package it.tidal.climax.core;

import com.google.gson.annotations.SerializedName;
import it.tidal.climax.config.*;
import it.tidal.climax.config.Config.Variant;
import it.tidal.climax.config.GenericDeviceConfig.OperationMode;
import it.tidal.climax.database.mapping.EnergyStatus;
import it.tidal.climax.database.mapping.HVACStatus;
import it.tidal.climax.database.mapping.IntakeStatus;
import it.tidal.climax.database.mapping.RoomStatus;
import it.tidal.climax.extensions.data.ClimaxPack;
import it.tidal.climax.extensions.data.CoolAutomation;
import it.tidal.climax.extensions.data.CoolAutomation.FanSpeed;
import it.tidal.climax.extensions.data.CoolAutomation.OpMode;
import it.tidal.climax.extensions.data.CoolAutomation.Status;
import it.tidal.climax.extensions.data.NetAtmo;
import it.tidal.climax.extensions.data.SolarEdgeEnergy;
import it.tidal.climax.extensions.managers.*;
import it.tidal.config.utils.*;
import it.tidal.logging.Log;
import org.javatuples.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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

                // This program is obsolete, use "DEFAULT" properly set
                l.warn("Program \"{}\" is obsolete, use \"{}\" properly set...", Program.CO2, Program.DEFAULT);

                Integer lowerBound = null;
                Integer upperBound = null;

                try {

                    lowerBound = Integer.parseInt(args[2]);
                    upperBound = Integer.parseInt(args[3]);
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

        final HashMap<String, RoomStatus> rss = new HashMap<>();
        final HashMap<String, Pair<IntakeStatus, IntakeStatus>> iss = new HashMap<>(); // intake status before, intake status after

        // Energy statuses
        l.debug("Retrieving energy status from db and solaredge...");
        TreeMap<LocalDateTime, EnergyStatus> ess;
        ess = dbm.splitQuarterOfHourToTenMinutesEnergyStatuses(
                dbm.retrieveLastEnergyStatus(4));

        SolarEdgeEnergy latestEs = _retrieveSolarEdgeEnergy(cfg.getSolarEdge(), null, null, now);

        // HVAC statuses (pre: before last exec, post: after last exec)
        l.debug("Retrieving hvac status...");
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPre;
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPost;

        hssPre = new HashMap<>(cfg.getCoolAutomation().getDevices().size());
        hssPost = new HashMap<>(cfg.getCoolAutomation().getDevices().size());

        // NetAtmo manager
        l.debug("Retrieving thermo status...");
        NetAtmoManager nam = NetAtmoManager.getInstance(cfg.getNetAtmo(), true);

        // First round to get all statuses
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
            final ArrayList<DeviceFamiliable> itps = ConfigManager.findAllDevices(cfg, cadc.findAllRelateds(GenericDeviceConfig.Role.INSIDE_TEMPERATURE_PROVIDER));
            DeviceFamiliable itp = null;

            if (itps.size() == 1) {

                itp = itps.iterator().next();

                // Search historical data
                final TreeMap<LocalDateTime, RoomStatus> tempRss;
                tempRss = dbm.retrieveLastRoomStatus(itp.getDbName(), 6);

                // Update climax pack
                cp.updateTemperatureAndHumidityWithRoomStatuses(tempRss);
                cp.setRoomStatus(nam.getData(itp.getName()).toRoomStatus(normalizedNowTs));
            }
            else {

                RoomStatus worstPerceived = null;
                String worstPerceivedDbName = null;

                RoomStatus worstCo2 = null;
                String worstCo2DbName = null;

                // If there's more than one inside temperature provider we'll keep the worst one
                for (DeviceFamiliable tempItp : itps) {

                    final RoomStatus tempRoomStatus = nam.getData(tempItp.getName()).toRoomStatus(normalizedNowTs);

                    if (tempRoomStatus.getPerceived() != null && (worstPerceived == null || tempRoomStatus.getPerceived() > worstPerceived.getPerceived())) {

                        worstPerceived = tempRoomStatus.duplicate();
                        worstPerceivedDbName = tempItp.getDbName();
                    }

                    if (tempRoomStatus.getCo2() != null && (worstCo2 == null || tempRoomStatus.getCo2() > worstCo2.getCo2())) {

                        worstCo2 = tempRoomStatus.duplicate();
                        worstCo2DbName = tempItp.getDbName();
                    }
                }

                // Merge the two (all data from first one only co2 value from the second)
                final RoomStatus roomStatus = worstPerceived.duplicate();
                roomStatus.setCo2(worstCo2.getCo2());

                // Search historical data
                final TreeMap<LocalDateTime, RoomStatus> tempRss;
                tempRss = dbm.retrieveLastRoomStatus(worstPerceivedDbName, 6);

                // Update climax pack
                cp.updateTemperatureAndHumidityWithRoomStatuses(tempRss);
                cp.setRoomStatus(roomStatus);
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
                GenericIntakeDetector gid = cp.getIntakeDetector();

                if (gid == null) {

                    gid = new GenericIntakeDetector(cadc.getName() + "_detector");
                    cp.setIntakeDetector(gid);
                }

                if (ktp.getDeviceFamily() == DeviceFamily.NETATMO)
                    gid.mergeDataFromAdvancedTemperatureSensor(nam.getData(ktp.getName()).toRoomStatus(normalizedNowTs), ktp.getName());
            }

            // Intake detectors & actuators (now checking only actuator)
            final DeviceFamiliable atp = ConfigManager.findDevice(cfg, cadc.findRelated(GenericDeviceConfig.Role.INTAKE_ACTUATOR));

            if (atp != null) {

                // Update climax pack
                GenericIntakeDetector gid = cp.getIntakeDetector();

                if (gid == null) {

                    gid = new GenericIntakeDetector(cadc.getName() + "_detector");
                    cp.setIntakeDetector(gid);
                }

                if (atp instanceof WemoDeviceConfig) {

                    final WemoManager wm = new WemoManager((WemoDeviceConfig) atp);
                    // XXX: I don't use gid.mergeDataFromOpenClosedQueryable() for this simple query
                    gid.setOpen(wm.getBinaryState() == 1);
                    gid.setDetectorName(atp.getName());
                }
            }
        }

        // Selecting program to execute between candidates
        final ProgramConfig programConfig = ConfigManager.suitableProgramConfig(cfg.getPrograms(), now);

        // Map with deviceName -> coolAutomationDevice, currentConfig, desiredConfig, climaxPack, Illness motivation
        final HashMap<String, Sextet<CoolAutomationDeviceConfig, CoolAutomation, CoolAutomation, OperationMode, ClimaxPack, String>> environment = new HashMap<>();

        // Set where I put devices that will drain a lot of energy if and when turned on
        final HashSet<String> eagerDevices = new HashSet<>();

        // Cycling (again) through device to decide what to do
        for (CoolAutomationDeviceConfig cadc
                : cfg.getCoolAutomation().getDevices()) {

            final String devName = cadc.getName();
            final OperationMode desiredOpMode = ConfigManager.suitableOperationMode(programConfig, devName, normalizedNow);

            final ClimaxPack cp = cps.get(devName);
            final CoolAutomation current = cp.getCurrentHVACConfig();

            Pair<CoolAutomation, String> result = null;
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
                    result = _desiredSetup(true, cp, programConfig);
                    desired = result.getValue0();

                    break;

                case OPERATE_IF_ON:

                    if (current.getStatus() == Status.ON) {

                        l.info("Device \"{}\" is {}, managing it (case {})...", devName, current.getStatus(), desiredOpMode);
                        result = _desiredSetup(false, cp, programConfig);
                        desired = result.getValue0();

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

                        result = _desiredSetup(false, cp, programConfig);
                        desired = result.getValue0();
                    }

                    break;

                case OPERATE_AUTO:

                    l.info("Device \"{}\" is {}, managing it (case {})!", devName, current.getStatus(), desiredOpMode);
                    result = _desiredSetup(false, cp, programConfig);
                    desired = result.getValue0();
                    break;

                default:

                    l.debug("Device \"{}\" has an unexpected operation mode (i.e. {}) !", devName, desiredOpMode);
                    break;
            }

            // Since we are managing cooling, we need to check that no heating is set
            if (desired != null && (desired.getOpMode() == OpMode.HEAT || desired.getOpMode() == OpMode.HAUX)) {

                desired.setOpMode(OpMode.FAN);
            }

            // Check if device will be eager
            if (desired != null && desired.getStatus() == Status.ON && desired.getOpMode() != OpMode.FAN) {

                l.debug("Eager device \"{}\"!!!", current.getName());
                eagerDevices.add(devName);
            }

            // Store all results
            final String motivation = (result != null ? result.getValue1() : null);
            environment.put(devName, new Sextet<>(cadc, current, desired, desiredOpMode, cp, motivation));

            if (desired != null)
                l.debug("Desired: " + desired.getDescription() + " (" + (motivation != null ? motivation : "unmotivated") + ").");
            else
                l.debug("Desired is not set: " + current.getDescription() + ".");
        }

        // Checking if there are too eager devices
        final HashSet<String> limitedDevices = new HashSet<>();

        if (eagerDevices.size() > programConfig.getMaxEagerDevices()) {

            int deviceToLimit = eagerDevices.size() - programConfig.getMaxEagerDevices();

            // First of all limit every device that is not explicity set to OPERATE_IF_ON then use random picks or, at last, go sequentially
            for (int phase = 1; phase < 100; phase++) {

                for (String devName : environment.keySet()) {

                    final Sextet<CoolAutomationDeviceConfig, CoolAutomation, CoolAutomation, OperationMode, ClimaxPack, String> tuple = environment.get(devName);
                    final OperationMode desiredOpMode = tuple.getValue3();
                    final CoolAutomation desired = tuple.getValue2();
                    boolean willLimit = false;

                    if (deviceToLimit <= 0)
                        break;
                    else if (desiredOpMode == OperationMode.DISABLED || desired.getStatus() == Status.OFF)
                        continue;
                    else if (phase < 5 && desiredOpMode != OperationMode.OPERATE_AUTO && desiredOpMode != OperationMode.OPERATE_IF_ON)
                        willLimit = true;
                    else if (phase < 10 && desiredOpMode != OperationMode.OPERATE_AUTO)
                        willLimit = true;
                    else if (phase >= 10 && phase < 99 && Math.random() > 0.4)
                        willLimit = true;
                    else if (phase == 99)
                        willLimit = true;

                    if (willLimit) {

                        desired.setOpMode(OpMode.FAN);
                        desired.setFanSpeed(FanSpeed.LOW);

                        l.warn("Limiting device \"{}\" due to eager constraints (phase {}).", devName, phase);

                        eagerDevices.remove(devName);
                        limitedDevices.add(devName);
                        deviceToLimit--;
                    }
                }

                if (deviceToLimit <= 0)
                    break;
            }
        }

        // DEBUG: printing data
        //l.json(environment, true);

        // Action to be done
        boolean operateOnHVAC = (cfg.getVariant() != Variant.LOG_ONLY && cfg.getVariant() != Variant.UPDATE_DB_ONLY);
        boolean updateDB = (cfg.getVariant() != Variant.LOG_ONLY);

        // Operation
        if (!operateOnHVAC) {

            l.info("Not operating on devices because \"{}\" variant is set", cfg.getVariant());
        }
        else {

            // Cycling (again) through device to operate and/or update DB
            for (String devName : environment.keySet()) {

                // A little bit of pause...
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {
                }

                final Sextet<CoolAutomationDeviceConfig, CoolAutomation, CoolAutomation, OperationMode, ClimaxPack, String> tuple = environment.get(devName);

                // Unpacking tuple
                final CoolAutomationDeviceConfig cadc = tuple.getValue0();
                final CoolAutomation current = tuple.getValue1();
                final CoolAutomation desired = tuple.getValue2();
                final OperationMode desiredOpMode = tuple.getValue3();
                final ClimaxPack cp = tuple.getValue4();
                final String result = tuple.getValue5();

                // Executing HVAC update
                if (desired != null && !desired.equals(current)) {

                    final CoolAutomationManager cam = CoolAutomationManager.getInstance(cadc);

                    l.info("Sending commands to device \"{}\"...", devName);
                    cam.setAll(current, desired);
                    cam.disconnect();
                }

                // Check if intake is also actuator (to open/close it)
                if (cp.getIntakeDetector() != null) {

                    final boolean intakeIsOpen = cp.getIntakeDetector().isOpen();
                    Boolean intakeShouldBeOpen = null;

                    if (desired != null && desired.getStatus() == Status.ON &&
                            (Illness.LOWER_CO2_NEEDED.equals(result) || Illness.BORDER_CO2.equals(result))) {

                        // If there's too much co2 open the intake
                        intakeShouldBeOpen = true;
                    }
                    else {

                        final AdvancedTemperatureSensor outside = cp.getOutsideStatus();
                        final AdvancedTemperatureSensor inside = cp.getRoomStatus();

                        if (outside != null && inside != null &&
                            outside.getPerceived() > inside.getPerceived() + 2) {

                            // If perceived outside is more than 2 degree high close the intake
                            intakeShouldBeOpen = false;
                        }
                    }
                    // Otherwise leave the intake as is

                    final DeviceFamiliable atp = ConfigManager.findDevice(cfg, cp.getIntakeDetector().getDetectorName());

                    if (atp instanceof WemoDeviceConfig) {

                        final WemoDeviceConfig wdc = (WemoDeviceConfig) atp;
                        final WemoManager wm = new WemoManager(wdc);

                        if (!intakeIsOpen && Boolean.TRUE.equals(intakeShouldBeOpen)) {

                            final boolean done = wm.setBinaryState(1);
                            l.debug("Trying to open intake \"{}\" on \"{}\"... {}!", atp.getName(), devName, (done ? "done" : "NOT done"));

                        } else if (intakeIsOpen && Boolean.FALSE.equals(intakeShouldBeOpen)) {

                            final boolean done = wm.setBinaryState(0);
                            l.debug("Trying to close intake \"{}\" on \"{}\"... {}!", atp.getName(), devName, (done ? "done" : "NOT done"));
                        }
                        else {

                            l.debug("Not touching intake \"{}\" on \"{}\"... it is {}.", atp.getName(), devName, (intakeIsOpen ? "open" : "closed"));
                        }

                        // XXX: check for duplicates
                        iss.put(wdc.getName(), new Pair<>(new IntakeStatus(wdc.getDbName(), normalizedNowTs, -1, intakeIsOpen), new IntakeStatus(wdc.getDbName(), normalizedNowTs, 0, (intakeShouldBeOpen != null ? intakeShouldBeOpen : intakeIsOpen))));
                    }
                    else {

                        l.debug("Intake configured for \"{}\" is not actuable.", devName);
                    }
                }
                else {

                    l.debug("No intake configured for \"{}\".", devName);
                }
            }
        }

        // Database update
        if (!updateDB) {

            l.info("Not updating DB because \"{}\" variant is set", cfg.getVariant());
        }
        else {

            // All netatmos
            for (Map.Entry<String, NetAtmo> entry : nam.getCache().entrySet()) {

                dbm.insertRoomStatus(entry.getValue().toRoomStatus(normalizedNowTs));
            }

            // All HVACs
            for (String devName : environment.keySet()) {

                final Sextet<CoolAutomationDeviceConfig, CoolAutomation, CoolAutomation, OperationMode, ClimaxPack, String> tuple = environment.get(devName);

                // Unpacking tuple
                final CoolAutomationDeviceConfig cadc = tuple.getValue0();
                final CoolAutomation current = tuple.getValue1();
                final CoolAutomation desired = tuple.getValue2();

                // Creating DB statuses
                final HVACStatus statusPre = new HVACStatus(cadc.getDbName(), normalizedNowTs, -1, current);
                final HVACStatus statusPost = new HVACStatus(cadc.getDbName(), normalizedNowTs, 0, (desired != null ? desired : current));

                dbm.insertHVACStatus(statusPre);
                dbm.insertHVACStatus(statusPost);
            }

            // All Intakes
            for (Map.Entry<String, Pair<IntakeStatus, IntakeStatus>> entry : iss.entrySet()) {

                dbm.insertIntakeStatus(entry.getValue().getValue0());
                dbm.insertIntakeStatus(entry.getValue().getValue1());
            }

            // All SolarEdge
            if (latestEs != null) {

                dbm.checkAndInsertSolarEdgeEnergy(latestEs);
            }

            // Closing DB connection
            dbm.dispose();
        }
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

        final SolarEdgeEnergy energy = _retrieveSolarEdgeEnergy(cfg.getSolarEdge(), start, end, now);

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

            l.info("Leaving everything as is (co2: {}ppm, lo: {}ppm, hi: {}ppm)...", roomStatus.getCo2(), lowerBound, upperBound);
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

            l.info("Change hvac, set it to {} (co2: {}ppm, lo: {}ppm, hi: {}ppm)!", desired.getStatus(), roomStatus.getCo2(), lowerBound, upperBound);
        }
        else {

            l.info("Nothing to do, hvac is already ok as is ({}, co2: {}ppm, lo: {}ppm, hi: {}ppm)!", current.getStatus(), roomStatus.getCo2(), lowerBound, upperBound);
        }
    }

    /* Common subprogram parts */

    private static SolarEdgeEnergy _retrieveSolarEdgeEnergy(SolarEdgeConfig solarEdgeConfig, Long start, Long end, LocalDateTime now) {

        SolarEdgeEnergy energy = null;

        try {

            final SolarEdgeManager sem;
            sem = SolarEdgeManager.getInstance(solarEdgeConfig);

            if (start == null || end == null) {

                energy = sem.getEnergyDetails(now);
                l.debug("Collected SolarEdge values with ts "
                        + energy.getFirstTimestamp() + " from the cloud.");
            } else {

                energy = sem.getEnergyDetails(start, end);

                final int tot = energy.getMeters().size();
                l.debug("Collected " + tot + " SolarEdge value"
                        + (tot != 1 ? "s" : "") + " from the cloud.");
            }

        } catch (Exception ex) {

            l.error("An error occurred with SolarEdge!", ex);
        }

        return energy;
    }

    private static Pair<CoolAutomation, String> _desiredSetup(boolean forceOn, ClimaxPack cp, ProgramConfig programConfig) {

        final CoolAutomation current = cp.getCurrentHVACConfig();

        CoolAutomation desired = null;
        String motivation = null;

        final AdvancedTemperatureSensor rs = cp.getRoomStatus();

        final double perceivedTemperature = rs.getPerceived();
        final double minPerceivedTemperature = programConfig.getMinPerceivedTemperature();
        final double maxPerceivedTemperature = programConfig.getMaxPerceivedTemperature();

        l.debug("\"{}\" perceived temperature {} from \"{}\" (threshold {}/{})",
                cp.getName(),
                Utility.americanDoubleFormatter.format(perceivedTemperature),
                rs.getName(),
                Utility.americanDoubleFormatter.format(minPerceivedTemperature),
                Utility.americanDoubleFormatter.format(maxPerceivedTemperature));

        // Evaluating hysteresis
        if (perceivedTemperature > maxPerceivedTemperature) {

            // For now we are concerned with > 0 deltas (i.e. summer cooling: if delta is < 0 the temperature is fine)
            final double deltaPerceived = perceivedTemperature - minPerceivedTemperature;

            if (deltaPerceived < 1) {
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

            motivation = Illness.LOWER_TEMPERATURE_NEEDED;
        }
        else if (perceivedTemperature < minPerceivedTemperature) {

            // We are in calm waters (turning off)
            desired = current.duplicate()
                    .changeStatus(Status.OFF)
                    .changeDelta(0.0)
                    .changeFanSpeed(FanSpeed.LOW);

            motivation = Illness.ALL_OK;
        }
        else {

            // We are between min and max, we'll keep current status
            desired = current.duplicate();
            motivation = Illness.BORDER_TEMPERATURE;
        }

        // If temperature does not turn on HVAC we have to check if co2 does
        if (desired == null || desired.getStatus() == Status.OFF) {

            final int co2Value = rs.getCo2();
            final int minCo2Value = programConfig.getMinCo2Value();
            final int maxCo2Value = programConfig.getMaxCo2Value();

            l.debug("\"{}\" co2 value {} (threshold {}/{})",
                    cp.getName(),
                    co2Value,
                    minCo2Value,
                    maxCo2Value);

            if (co2Value > maxCo2Value) {

                final int deltaCo2 = co2Value - maxCo2Value;

                if (deltaCo2 > 750) {
                    // Fan, 0, MEDIUM
                    desired = current.duplicate()
                            .changeStatus(Status.ON)
                            .changeOpMode(OpMode.FAN)
                            .changeDelta(0.0)
                            .changeFanSpeed(FanSpeed.MEDIUM);
                } else {
                    // Fan, 0, LOW
                    desired = current.duplicate()
                            .changeStatus(Status.ON)
                            .changeOpMode(OpMode.FAN)
                            .changeDelta(0.0)
                            .changeFanSpeed(FanSpeed.LOW);
                }

                motivation = Illness.LOWER_CO2_NEEDED;

            }
            else if (co2Value < minCo2Value) {

                // We are in calm waters (turning off)
                desired = current.duplicate()
                        .changeStatus(Status.OFF)
                        .changeDelta(0.0)
                        .changeFanSpeed(FanSpeed.LOW);

                motivation = Illness.ALL_OK;
            }
            else {

                // We are between min and max, we'll keep current status
                desired = current.duplicate();
                motivation = Illness.BORDER_CO2;
            }
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

                motivation = Illness.ALL_OK;
            }
        }

        if (motivation == null)
            motivation = Illness.ALL_OK;

        return new Pair<>(desired, motivation);
    }
}
