package it.tidal.config.utils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import it.tidal.logging.Log;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

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
            T object = new Gson().fromJson(reader, type);

            l.trace("Found config:\n{}", Log.json(object, true));

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
        }

        return null;
    }
}
