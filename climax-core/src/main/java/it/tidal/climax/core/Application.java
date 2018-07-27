package it.tidal.climax.core;

import it.tidal.climax.config.Config;
import it.tidal.climax.extensions.managers.DatabaseManager;
import it.tidal.config.utils.ConfigManager;
import it.tidal.logging.Log;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Entry point. This class contains the main method and it is used to start
 * everything.
 *
 * @author dede
 */
public class Application {

    private static Log l = Log.prepare(Application.class.getSimpleName());

    public static final String TMP_CONFIG_PATH = "/tmp/climax.conf";
    public static final String TMP_STATUS_PATH = "/tmp/climax-status-";

    public static final NumberFormat DOUBLE_FMT = new DecimalFormat("#0.0",
            DecimalFormatSymbols.getInstance(Locale.US));

    public static final String APPLICATION_BUILD = "2018-07-27";

    public static final LocalDateTime NOW = LocalDateTime.now();
    public static final boolean IS_DAY = (NOW.getHour() >= 8
            && NOW.getHour() <= 21);

    public static void main(String[] args) {

        String configFileName = TMP_CONFIG_PATH;
        String program = "test";

        if (args.length > 0) {
            configFileName = args[0];
        }

        File configFile = new File(configFileName);

        if (!configFile.exists() || !configFile.canRead()) {

            l.error("Cannot read config from: {}.", configFileName);
            return;
        }

        Config config = ConfigManager.read(Config.class, configFileName);

        if (config == null) {

            l.error("No config found in: {}.", configFileName);
            return;
        }

        if (!config.isLatestVersion()) {

            l.error("Config version mismatch found: {}, latest: {}."
                    + config.getVersion(), Config.getLatestVersion());
            return;
        }

        if (args.length > 1) {
            program = args[1];
        }

        if ("test".equalsIgnoreCase(program)) {

            l.info("Climax {} ({}) started at {}.",
                    config.getVersion(),
                    APPLICATION_BUILD,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(NOW));

            // Test methods
            Operation.runProgram(config);
        } else if ("check-db".equalsIgnoreCase(program)) {

            l.info("Climax {} ({}) testing DB conneciton at {}.",
                    config.getVersion(),
                    APPLICATION_BUILD,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(NOW));

            DatabaseManager dbm;
            dbm = DatabaseManager.getInstance(config.getMySQL());

            final String testTableName = "test_value";

            l.info("Found {} rows in \"{}\", database connection: OK!",
                    dbm.countRows(testTableName), testTableName);

            dbm.dispose();

        } else if ("disable-all".equalsIgnoreCase(program)) {

            l.info("Climax {} ({}) disabling all at {}.",
                    config.getVersion(),
                    APPLICATION_BUILD,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(NOW));

            // Disable all
            Operation.runShutdown(config);

        } else if ("log-solaredge".equalsIgnoreCase(program)) {

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

            l.info("Climax {} ({}) storing SolarEdgeValues {} {}.",
                    config.getVersion(),
                    APPLICATION_BUILD,
                    (start != null ? "" + start + " - " + end : ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(NOW)
            );

            // Disable all
            Operation.runLogSolarEdge(config, start, end);
        }
    }

}
