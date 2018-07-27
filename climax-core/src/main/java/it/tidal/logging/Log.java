package it.tidal.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple log wrapper. To avoid including logging libraries to projects.
 *
 * @author dede
 */
public class Log {

    private final Logger logger;

    private Log(String name) {

        logger = LoggerFactory.getLogger(name);
    }

    public static Log prepare(String name) {

        return new Log(name);
    }

    public static String json(Object object, boolean prettyPrint) {

        Gson gson = null;

        if (prettyPrint) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        } else {
            gson = new Gson();
        }

        return gson.toJson(object);
    }

    public void trace(String msg) {

        logger.trace(msg);
    }

    public void debug(String msg) {

        logger.debug(msg);
    }

    public void info(String msg) {

        logger.info(msg);
    }

    public void warn(String msg) {

        logger.warn(msg);
    }

    public void error(String msg) {

        logger.error(msg);
    }

    public void error(Throwable t, String msg) {

        logger.error(msg);
        logger.error("Trace here:", t);
    }

    public void trace(String msg, Object... args) {

        logger.trace(msg, args);
    }

    public void debug(String msg, Object... args) {

        logger.debug(msg, args);
    }

    public void info(String msg, Object... args) {

        logger.info(msg, args);
    }

    public void warn(String msg, Object... args) {

        logger.warn(msg, args);
    }

    public void error(String msg, Object... args) {

        logger.error(msg, args);
    }

    public void error(Throwable t, String msg, Object... args) {

        logger.error(msg, args);
        logger.error("Trace here:", t);
    }
}
