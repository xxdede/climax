package it.tidal.climax.core;

import it.tidal.climax.config.Config;
import it.tidal.climax.core.Operation.Program;
import it.tidal.climax.extensions.managers.ConfigManager;
import it.tidal.config.utils.Utility;
import it.tidal.logging.Log;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Entry point. This class contains the main method and it is used to start
 * everything.
 *
 * @author dede
 */
public class Application {

    private static Log l = Log.prepare(Application.class.getSimpleName());

    public static final String TMP_CONFIG_PATH = "/tmp/climax.conf";
    public static final String APPLICATION_BUILD = "2020-06-09";

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

            l.error("Config version mismatch found: {}, latest: {}.",
                    cfg.getVersion(), Config.getLatestVersion());
            return;
        }

        /*
        if (cfg.getVariant() == Config.Variant.LOG_ONLY) {

            l.info(GsonFactory.prettyInstance().toJson(cfg));

            for (CoolAutomationDeviceConfig dev : cfg.getCoolAutomation().getDevices()) {

                l.info("Scheduled configuration for device \"" + dev.getName()
                        + "\" (" + dev.getDeviceFamily() + ") = "
                        + ConfigManager.suitableOperationMode(cfg, dev.getName(), NOW));
            }
        }
         */
        Program prg = Program.DEFAULT;

        if (args.length > 1) {
            prg = Program.fromString(args[1]);
        }

        if (prg == null) {

            l.error("Unknown program... bailing out!");
            return;
        } else {

            if (args.length > 2) {

                String argsAsString = Utility.writeCommaSeparated(Arrays.asList(args));

                l.info("Climax {} ({}) started with args {}...",
                        cfg.getVersion(), APPLICATION_BUILD, argsAsString);
            }
            else {

                l.info("Climax {} ({}) started with program '{}'...",
                        cfg.getVersion(), APPLICATION_BUILD, prg.getSlug());
            }
        }

        Operation.execute(args, prg, cfg, NOW);
    }
}
