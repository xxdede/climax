package it.tidal.climax.core;

import it.tidal.climax.config.Config;
import it.tidal.climax.config.CoolAutomationDeviceConfig;
import it.tidal.climax.core.Operation.Program;
import it.tidal.climax.extensions.managers.ConfigManager;
import it.tidal.gson.GsonFactory;
import it.tidal.logging.Log;
import java.io.File;
import java.time.LocalDateTime;

/**
 * Entry point. This class contains the main method and it is used to start
 * everything.
 *
 * @author dede
 */
public class Application {

    private static Log l = Log.prepare(Application.class.getSimpleName());

    public static final String TMP_CONFIG_PATH = "/tmp/climax.conf";
    public static final String APPLICATION_BUILD = "2018-08-01";

    public static final LocalDateTime NOW = LocalDateTime.now();

    public static void main(String[] args) {

        String configFileName = TMP_CONFIG_PATH;

        if (args.length > 0) {
            configFileName = args[0];
        }

        File configFile = new File(configFileName);

        if (!configFile.exists() || !configFile.canRead()) {

            l.error("Cannot read config from: {}.", configFileName);
            return;
        }

        Config cfg = ConfigManager.read(Config.class, configFileName);

        if (cfg == null) {

            l.error("No config found in: {}.", configFileName);
            return;
        }

        if (!cfg.isLatestVersion()) {

            l.error("Config version mismatch found: {}, latest: {}."
                    + cfg.getVersion(), Config.getLatestVersion());
            return;
        }

        if (cfg.getVariant() == Config.Variant.LOG_ONLY) {

            l.info(GsonFactory.prettyInstance().toJson(cfg));

            for (CoolAutomationDeviceConfig dev : cfg.getCoolAutomation().getDevices()) {

                l.info("Scheduled configuration for device \"" + dev.getName()
                        + "\" (" + dev.getDeviceFamily() + ") = "
                        + ConfigManager.suitableOperationMode(cfg, dev.getName(), NOW));
            }
        }

        String program = Program.DEFAULT.getSlug();
        boolean recognizedProgram = false;

        if (args.length > 1) {
            program = args[1];
        }

        for (Program availableProgram : Program.values()) {
            if (program.equals(availableProgram.getSlug())) {
                recognizedProgram = true;
                break;
            }
        }

        if (!recognizedProgram) {

            l.error("Unkwnowm program: {}, bailing out!" + program);
            return;
        }

        l.info("Climax {} started with program '{}'...",
                cfg.getVersion(), program);

    }
}
