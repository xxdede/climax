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
        ONLY_SOLAREDGE(2);

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
        TreeMap<LocalDateTime, EnergyStatus> ess;
        ess = dbm.splitQuarterOfHourToTenMinutesEnergyStatuses(
                dbm.retrieveLastEnergyStatus(4));

        // HVAC statuses (pre: before last exec, post: after last exec)
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPre;
        HashMap<String, TreeMap<LocalDateTime, HVACStatus>> hssPost;

        hssPre = new HashMap<>(cfg.getCoolAutomation().getDevices().size());
        hssPost = new HashMap<>(cfg.getCoolAutomation().getDevices().size());

        // NetAtmo manager
        NetAtmoManager nam = NetAtmoManager.getInstance(cfg.getNetAtmo(), true);

        for (CoolAutomationDeviceConfig cadc
                : cfg.getCoolAutomation().getDevices()) {

            final String devName = cadc.getName();
            final String dbName = cadc.getDbName();

            // Check if climax pack exists
            ClimaxPack cp = cps.get(devName);

            if (cp == null) {

                cp = new ClimaxPack();
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

            final CoolAutomation current = cps.get(devName).getCurrentHVACConfig();
            final CoolAutomation desired;

            switch (desiredOpMode) {

                case NOT_SPECIFIED:

                    l.warn("Device \"{}\" has no suitable operation mode?!", devName);

                    break;

                case DISABLED:

                    try {

                        if (current.getStatus() != Status.OFF) {

                            l.info("Device \"{}\" already {} (case {})...", devName, Status.OFF, desiredOpMode);

                        } else if (!Variant.LOG_ONLY.equals(cfg.getVariant())) {

                            desired = cam.setAll(current,
                                    current.getOpMode(), current.getFanSpeed(),
                                    Status.OFF, 0);

                            if (desired != null) {
                                l.info("Device \"{}\" turned {} (case {}).", devName, Status.OFF, desiredOpMode);
                            } else {
                                l.error("Something went wrong while turning device \"{}\" {} (case {})!", devName, Status.OFF, desiredOpMode);
                            }
                        }
                    } catch (Exception ex) {

                        l.error("A problem occurred while turning device \"" + devName + "\" " + Status.OFF + " (case " + desiredOpMode + ")!", ex);
                    }

                    break;

                case ENABLED:

                    l.info("Device \"{}\" is {}, forcing it (case {})!", devName, current.getStatus(), desiredOpMode);
                    desiredSetup(true);
                    break;

                case OPERATE_IF_ON:

                    if (current.getStatus() == Status.ON) {

                        l.info("Device \"{}\" is {}, managing it (case {})...", devName, current.getStatus(), desiredOpMode);
                        desiredSetup(false);

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

                        desiredSetup(false);
                    }

                    break;

                case OPERATE_AUTO:

                    l.info("Device \"{}\" is {}, managing it (case {})!", devName, current.getStatus(), desiredOpMode);
                    desiredSetup(false);
                    break;

                default:

                    l.debug("Device \"{}\" has an unexpected operation mode (i.e. {}) !", devName, desiredOpMode);
                    break;
            }

            cam.disconnect();
        }

        // TODO: cycle again on devices to check if what we want is actually running
    }

    private static CoolAutomation desiredSetup(boolean forceOn) {

        // XXX: forceOn = true means that even if we believe that device should be turned off we keep it going
        l.debug("TODO: I'm here");
        return null;
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

    /*
    public static void runProgram(Config config) {

        //////////////////  Definitions  //////////////////
        final String moduleCantina = "Cantina";
        final String moduleCamera = "Camera";
        final String moduleSala = "Sala";
        final String moduleGiardino = "Giardino";

        final String thermoStudio = "Studio";

        ///////////////////  Managers  ///////////////////
        NetAtmoManager nam;
        CoolAutomationManager cam0, cam1;
        CoolAutomationDeviceConfig cadc0 = config.getCoolAutomation()
                .find("Piano terra");
        CoolAutomationDeviceConfig cadc1 = config.getCoolAutomation()
                .find("Piano 1");

        nam = NetAtmoManager.getInstance(config.getNetAtmo());
        cam0 = CoolAutomationManager.getInstance(cadc0);
        cam1 = CoolAutomationManager.getInstance(cadc1);

        ///////////////////  Data  ///////////////////
        NetAtmo cameraData, salaData, studioData, cantinaData, giardinoData;
        final CoolAutomation piano0Data, piano1Data;

        List<String> moduleNames = new ArrayList<>(3);
        moduleNames.add(moduleCamera);
        moduleNames.add(moduleSala);
        moduleNames.add(moduleCantina);
        moduleNames.add(moduleGiardino);

        Map<String, NetAtmo> modules = nam.getStationsData(moduleNames);
        //l.info(Log.json(modules, true));

        cameraData = modules.get(moduleCamera);
        salaData = modules.get(moduleSala);
        cantinaData = modules.get(moduleCantina);
        giardinoData = modules.get(moduleGiardino);

        if (false || cameraData == null) {

            l.error("A problem occurred while retrieving '"
                    + moduleCamera + "' data...");
            return;
        }

        if (false || salaData == null) {

            l.error("A problem occurred while retrieving '"
                    + moduleSala + "' data...");
            return;
        }

        if (false || cantinaData == null) {

            l.error("A problem occurred while retrieving '"
                    + moduleCantina + "' data...");
            return;
        }

        if (false || giardinoData == null) {

            l.error("A problem occurred while retrieving '"
                    + moduleGiardino + "' data...");
            return;
        }

        studioData = nam.getThermostatData(thermoStudio);

        if (studioData == null) {

            l.error("A problem occurred while retrieving '"
                    + thermoStudio + "' data...");
            return;
        }

        // Since studio is a thermostat I merge data from camera
        studioData.setHumidity(cameraData.getHumidity());
        studioData.setCo2(cameraData.getCo2());

        piano0Data = cam0.getDeviceData();
        piano1Data = cam1.getDeviceData();

        if (piano0Data == null) {

            l.error("Problems while retrieving 'Piano terra' data...");
            return;
        }

        if (piano1Data == null) {

            l.error("Problems while retrieving 'Piano 1' data...");
            return;
        }

        //////////////////  Illness  /////////////////
        double cameraPt, salaPt, cantinaPt, giardinoPt, studioPt;
        Illness.Level cameraIl, salaIl, cantinaIl, giardinoIl, studioIl;

        cameraPt = Illness.computeHumidexValue(cameraData.getTemperature(),
                cameraData.getHumidity());
        cameraIl = Illness.compute(cameraPt);

        salaPt = Illness.computeHumidexValue(salaData.getTemperature(),
                salaData.getHumidity());
        salaIl = Illness.compute(salaPt);

        cantinaPt = Illness.computeHumidexValue(cantinaData.getTemperature(),
                cantinaData.getHumidity());
        cantinaIl = Illness.compute(cantinaPt);

        giardinoPt = Illness.computeHumidexValue(giardinoData.getTemperature(),
                giardinoData.getHumidity());
        giardinoIl = Illness.compute(giardinoPt);

        studioPt = Illness.computeHumidexValue(studioData.getTemperature(),
                studioData.getHumidity());
        studioIl = Illness.compute(studioPt);

        ///////////////  Item Statuses  //////////////
        long nnow = Utility.normalizedNow(600);

        RoomStatus cameraStatus = new RoomStatus(moduleCamera, nnow,
                cameraData.getTemperature(),
                cameraData.getHumidity(),
                cameraData.getCo2(),
                cameraPt, cameraIl);

        RoomStatus salaStatus = new RoomStatus(moduleSala, nnow,
                salaData.getTemperature(),
                salaData.getHumidity(),
                salaData.getCo2(),
                salaPt, salaIl);

        RoomStatus cantinaStatus = new RoomStatus(moduleCantina, nnow,
                cantinaData.getTemperature(),
                cantinaData.getHumidity(),
                cantinaData.getCo2(),
                cantinaPt, cantinaIl);

        RoomStatus studioStatus = new RoomStatus(thermoStudio, nnow,
                studioData.getTemperature(),
                studioData.getHumidity(),
                studioData.getCo2(),
                studioPt, studioIl);

        RoomStatus giardinoStatus = new RoomStatus(moduleGiardino, nnow,
                giardinoData.getTemperature(),
                giardinoData.getHumidity(),
                giardinoData.getCo2(),
                giardinoPt, giardinoIl);

        HVACStatus daikin0StatusPre = new HVACStatus("daikin_0",
                nnow, -1, piano0Data);

        HVACStatus daikin0StatusPost = new HVACStatus("daikin_0",
                nnow, 0, piano0Data);

        HVACStatus daikin1StatusPre = new HVACStatus("daikin_1",
                nnow, -1, piano1Data);

        HVACStatus daikin1StatusPost = new HVACStatus("daikin_1",
                nnow, 0, piano1Data);

        //////////////////  Results  /////////////////
        l.debug(cameraStatus.getDescription());
        l.debug(salaStatus.getDescription());
        l.debug(cantinaStatus.getDescription());
        l.debug(studioStatus.getDescription());
        l.debug(giardinoStatus.getDescription());

        l.debug("HVAC '{}' is {} (mode {}), room temperature: {}°,"
                + " set temperature: {}°",
                piano0Data.getName(), piano0Data.getStatus(),
                piano0Data.getOpMode(),
                DOUBLE_FMT.format(piano0Data.getRoomTemperature()),
                DOUBLE_FMT.format(piano0Data.getSetTemperature()));

        l.debug("HVAC '{}'     is {} (mode {}), room temperature: {}°,"
                + " set temperature: {}°",
                piano1Data.getName(), piano1Data.getStatus(),
                piano1Data.getOpMode(),
                DOUBLE_FMT.format(piano1Data.getRoomTemperature()),
                DOUBLE_FMT.format(piano1Data.getSetTemperature()));

        ///////////////////  Actions  //////////////////
        if (IS_DAY) {

            final double firstFloorPt = cameraPt;
            final Illness.Level firstFloorIl = Illness.compute(firstFloorPt);

            if (Illness.compare(firstFloorIl, salaIl) > 1) {

                CoolAutomation tempData;
                CoolAutomation finalData = operateEverything(config, cadc0, cam0,
                        salaPt, giardinoPt,
                        piano0Data);

                daikin0StatusPost.setCoolAutomation(finalData);

                if (finalData == null) {

                    l.info("Managed PT but nothing did happen...");
                    tempData = operateEverything(config, cadc1, cam1,
                            firstFloorPt, studioPt, piano1Data);
                    daikin1StatusPost.setCoolAutomation(tempData);
                    l.info("...managed also P1!");
                } else if (finalData.getStatus() == CoolAutomation.Status.OFF
                        || finalData.getOpMode() == CoolAutomation.OpMode.Fan) {

                    l.info("Managed PT but illness is ok...");
                    tempData = operateEverything(config, cadc1, cam1,
                            firstFloorPt, studioPt, piano1Data);
                    daikin1StatusPost.setCoolAutomation(tempData);
                    l.info("...and managed also P1!");
                } else if (firstFloorIl != Illness.Level.NONE) {

                    l.info("Managed PT...");
                    tempData = operateShutdown(config, cadc1, cam1,
                            piano1Data, false);
                    daikin1StatusPost.setCoolAutomation(tempData);
                    l.info("...and disabled P1.");
                } else {

                    l.info("Managed PT...");
                    tempData = operateShutdown(config, cadc1, cam1,
                            piano1Data, false);
                    daikin1StatusPost.setCoolAutomation(tempData);
                    l.info("...and disabled P1.");
                }
            } else {

                CoolAutomation tempData;
                CoolAutomation finalData = operateEverything(config, cadc1, cam1,
                        firstFloorPt, studioPt,
                        piano1Data);

                daikin1StatusPost.setCoolAutomation(finalData);

                if (finalData == null) {

                    l.info("Managed P1 but nothing did happen...");
                    tempData = operateEverything(config, cadc0, cam0,
                            salaPt, giardinoPt, piano0Data);
                    daikin0StatusPost.setCoolAutomation(tempData);
                    l.info("...managed also PT!");
                } else if (salaIl != Illness.Level.NONE
                        && salaIl != Illness.Level.LIGHT
                        && finalData.getStatus() == CoolAutomation.Status.OFF) {

                    l.info("Managed P1 but illness is under control...");
                    tempData = operateEverything(config, cadc0, cam0,
                            salaPt, giardinoPt, piano0Data);
                    daikin0StatusPost.setCoolAutomation(tempData);
                    l.info("...and managed PT!");
                } else {

                    l.info("Managed P1...");
                    tempData = operateShutdown(config, cadc0, cam0,
                            piano0Data, false);
                    daikin0StatusPost.setCoolAutomation(tempData);
                    l.info("...and disabled PT.");
                }
            }
        } else {

            CoolAutomation tempData;

            // At night check air intake temperature and run only bedroom devs
            tempData = operateEverything(config, cadc1, cam1,
                    cameraPt, studioPt, piano1Data);

            daikin1StatusPost.setCoolAutomation(tempData);

            tempData = operateShutdown(config, cadc0, cam0,
                    piano0Data, false);

            daikin0StatusPost.setCoolAutomation(tempData);

            l.info("Managed P1 only (is night)...");
        }

        ///////////////////  Save on DB  //////////////////
        try {

            DatabaseManager dbm;
            dbm = DatabaseManager.getInstance(config.getMySQL());

            dbm.insertRoomStatus(cameraStatus);
            dbm.insertRoomStatus(salaStatus);
            dbm.insertRoomStatus(cantinaStatus);
            dbm.insertRoomStatus(studioStatus);
            dbm.insertRoomStatus(giardinoStatus);

            dbm.insertHVACStatus(daikin0StatusPre);
            dbm.insertHVACStatus(daikin0StatusPost);
            dbm.insertHVACStatus(daikin1StatusPre);
            dbm.insertHVACStatus(daikin1StatusPost);

            dbm.dispose();
        } catch (Exception ex) {

            l.error("An error occurred while saving to DB!", ex);
        }
    }

    public static void runShutdown(Config config) {

        ///////////////////  Managers  ///////////////////
        CoolAutomationManager cam0, cam1;
        CoolAutomationDeviceConfig cadc0 = config.getCoolAutomation()
                .find("Piano terra");
        CoolAutomationDeviceConfig cadc1 = config.getCoolAutomation()
                .find("Piano 1");

        cam0 = CoolAutomationManager.getInstance(cadc0);
        cam1 = CoolAutomationManager.getInstance(cadc1);

        ///////////////////  Data  ///////////////////
        CoolAutomation piano0Data, piano1Data;

        piano0Data = cam0.getDeviceData();
        piano1Data = cam1.getDeviceData();

        if (piano0Data == null) {

            l.error("Problems while retrieving 'Piano terra' data...");
            return;
        }

        if (piano1Data == null) {

            l.error("Problems while retrieving 'Piano 1' data...");
            return;
        }

        // At night run only bedroom devices
        operateShutdown(config, cadc0, cam0, piano0Data, true);
        operateShutdown(config, cadc1, cam1, piano1Data, true);
    }

    private static CoolAutomation operateShutdown(Config config,
            CoolAutomationDeviceConfig cadc,
            CoolAutomationManager cam,
            CoolAutomation targetData,
            boolean force) {

        l.debug("Operating shutdown on '" + cadc.getName() + "'.");

        CoolAutomation finalData = null;

        String statusPath = TMP_STATUS_PATH + targetData.getName()
                .replace(' ', '-').toLowerCase();

        /////////////////  Overrides  ////////////////
        boolean skipAction = config.isSkipActions();
        CoolAutomation prevData = null;

        if (skipAction && !force) {

            l.debug("Skip action set... nothing to do!");
            return null;
        }

        try {

            String prev = Utility.readFile(statusPath);

            if (prev != null) {
                prevData = GsonFactory.instance().fromJson(prev, CoolAutomation.class);
            }
        } catch (JsonSyntaxException ex) {

            prevData = null;
        }

        if (prevData == null) {

            l.debug("No previous data in '{}' so we go automatic!", statusPath);
        } else if (prevData.getStatus() == CoolAutomation.Status.OFF) {

            l.info("CoolAutomation already off...");
            finalData = prevData;

            // We've just read it, no need to rewrite
            //Utility.writeFile(statusPath, Log.json(finalData, true));
            return finalData;
        }

        //////////////////  Actions  /////////////////
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
        }

        finalData = cam.setAll(targetData, targetData.getOpMode(),
                targetData.getFanSpeed(), CoolAutomation.Status.OFF, 0);

        if (finalData == null) {

            l.error("A problem occurred while setting CoolAutomation...");
            return null;
        }

        l.info("Properly configured CoolAutomation, current values:\n{}",
                Log.json(finalData, true));

        Utility.writeFile(statusPath, Log.json(finalData, true));

        return finalData;
    }

    private static CoolAutomation operateFan(Config config,
            CoolAutomationDeviceConfig cadc,
            CoolAutomationManager cam,
            CoolAutomation targetData) {

        l.debug("Operating only fan on '" + cadc.getName() + "'.");

        CoolAutomation finalData = null;

        String statusPath = TMP_STATUS_PATH + targetData.getName()
                .replace(' ', '-').toLowerCase();

        /////////////////  Overrides  ////////////////
        boolean skipAction = config.isSkipActions();
        CoolAutomation prevData = null;

        if (skipAction) {

            l.debug("Skip action set... nothing to do!");
            return null;
        }

        try {

            String prev = Utility.readFile(statusPath);

            if (prev != null) {
                prevData = GsonFactory.instance().fromJson(prev, CoolAutomation.class);
            }
        } catch (JsonSyntaxException ex) {

            prevData = null;
        }

        if (prevData == null) {

            l.debug("No previous data in '{}' so we go automatic!", statusPath);
        } else if (prevData.getStatus() == CoolAutomation.Status.ON
                && prevData.getOpMode() == CoolAutomation.OpMode.Fan
                && prevData.getFanSpeed() == CoolAutomation.FanSpeed.Low) {

            l.info("CoolAutomation already fan low...");
            finalData = prevData;

            // We've just read it, no need to rewrite
            //Utility.writeFile(statusPath, Log.json(finalData, true));
            return finalData;
        }

        //////////////////  Actions  /////////////////
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
        }

        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);

        if (finalData == null) {

            l.error("A problem occurred while setting CoolAutomation...");
            return null;
        }

        l.info("Properly configured CoolAutomation, current values:\n{}",
                Log.json(finalData, true));

        Utility.writeFile(statusPath, Log.json(finalData, true));

        return finalData;
    }

    private static CoolAutomation operateEverything(Config config,
            CoolAutomationDeviceConfig cadc,
            CoolAutomationManager cam,
            double sourcePt, double outsidePt,
            CoolAutomation targetData) {

        l.debug("Operating on '" + cadc.getName() + "'.");

        CoolAutomation finalData = null;

        String statusPath = TMP_STATUS_PATH + targetData.getName()
                .replace(' ', '-').toLowerCase();

        /////////////////  Overrides  ////////////////
        boolean skipAction = config.isSkipActions();
        boolean manualOverride = false;
        CoolAutomation prevData = null;

        if (skipAction) {

            l.debug("Skip action set... nothing to do!");
            return null;
        }

        try {

            String prev = Utility.readFile(statusPath);

            if (prev != null) {
                prevData = GsonFactory.instance().fromJson(prev, CoolAutomation.class);
            }
        } catch (JsonSyntaxException ex) {

            prevData = null;
        }

        // If someone manually changed one setting between: status,
        // setTemperature or opMode we should not do anything
        // (to start over and go auto simply remove the 'statusPath' file)
        if (prevData == null) {
            l.debug("No previous data in '{}' so we go automatic!", statusPath);
        } else if (!prevData.getName().equals(targetData.getName())
                || !prevData.getLineId().equals(targetData.getLineId())) {
            l.debug("Previous data was for a different device/line...");
        } else {

            if (prevData.getOpMode() != targetData.getOpMode()) {

                l.debug("OpMode differs: {} vs {}",
                        prevData.getOpMode(),
                        targetData.getOpMode());
                manualOverride = true;
            }
            if (prevData.getStatus() != targetData.getStatus()) {

                l.debug("Status differs: {} vs {}",
                        prevData.getStatus(),
                        targetData.getStatus());
                manualOverride = true;
            }
            if (prevData.getSetTemperature() != targetData.getSetTemperature()) {

                l.debug("Set temperature differs: {} vs {}",
                        prevData.getSetTemperature(),
                        targetData.getSetTemperature());
                // If set temperature differs we do not think that someone
                // did a manual override (it happens that Daikin does not
                // accept temperature at first set)
            }
            if (prevData.getFanSpeed() != targetData.getFanSpeed()) {

                l.debug("Fan speed differs: {} vs {}",
                        prevData.getFanSpeed(),
                        targetData.getFanSpeed());
                // Same as previous
            }
        }

        if (manualOverride) {

            if (config.isAlwaysConfigure()) {

                l.debug("Someone changed settings manually, "
                        + "but we go on, no matter!");
            } else {

                l.debug("Someone changed settings manually, "
                        + "we do not interfere!");
                return null;
            }
        }

        //////////////////  Actions  /////////////////
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
        }

        // Check if it's not too warm and air intake is fresh
        // we should use air without cooling
        if (sourcePt < 34 && outsidePt <= sourcePt - 0.5) {

            boolean shouldUseExternalAir = false;

            switch (cadc.getAirIntake().getStatus()) {

                case AUTO:
                    l.debug("TODO: We should open air intake...");
                    //airIntakeOpen = true;
                    break;

                case OPEN:
                    shouldUseExternalAir = true;
                    break;
            }

            if (shouldUseExternalAir) {

                if (IS_DAY) {

                    if (sourcePt <= 31.0) {
                        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                                CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
                    } else if (sourcePt <= 32.0) {
                        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                                CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, 0);
                    } else {
                        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                                CoolAutomation.FanSpeed.High, CoolAutomation.Status.ON, 0);
                    }
                } else {

                    if (sourcePt <= 31.6) {
                        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                                CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
                    } else {
                        finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                                CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, 0);
                    }
                }

                l.info("ExtAir configured CoolAutomation, current values:\n{}",
                        Log.json(finalData, true));

                Utility.writeFile(statusPath, Log.json(finalData, true));
                return finalData;
            }
        }

        // If airIntake is closed or outside temp is too high
        if (IS_DAY) {

            if (sourcePt <= 30.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.OFF, 0);
            } else if (sourcePt <= 31.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
            } else if (sourcePt <= 32.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
            } else if (sourcePt <= 33.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, -1);
            } else if (sourcePt <= 34.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, -2);
            } else if (sourcePt <= 35.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, -2);
            } else if (sourcePt <= 36.5) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, -3);
            } else {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.High, CoolAutomation.Status.ON, -3);
            }
        } else {

            if (sourcePt <= 31.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.OFF, 0);
            } else if (sourcePt <= 32.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Fan,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
            } else if (sourcePt <= 33.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, 0);
            } else if (sourcePt <= 34.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, -1);
            } else if (sourcePt <= 35.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Low, CoolAutomation.Status.ON, -2);
            } else if (sourcePt <= 36.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, -2);
            } else if (sourcePt <= 37.0) {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.Med, CoolAutomation.Status.ON, -3);
            } else {
                finalData = cam.setAll(targetData, CoolAutomation.OpMode.Cool,
                        CoolAutomation.FanSpeed.High, CoolAutomation.Status.ON, -3);
            }
        }

        if (finalData == null) {

            l.error("A problem occurred while setting CoolAutomation...");
            return null;
        }

        l.info("Properly configured CoolAutomation, current values:\n{}",
                Log.json(finalData, true));

        Utility.writeFile(statusPath, Log.json(finalData, true));
        return finalData;
    }
     */
}
