package it.tidal.config.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * Mixed utils. A place to put some different things.
 *
 * @author dede
 */
public class Utility {

    public static boolean writeFile(String path,
            String content, boolean append) {

        try {

            Files.write(Paths.get(path),
                    content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    (append ? StandardOpenOption.APPEND
                            : StandardOpenOption.TRUNCATE_EXISTING));

            return true;
        } catch (IOException ex) {

            return false;
        }
    }

    public static boolean writeFile(String path, String content) {

        return writeFile(path, content, false);
    }

    public static String readFile(String path) {

        try {

            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException ex) {

            return null;
        }
    }

    public static String sluggize(String s) {

        if (s == null) {
            return null;
        }

        return s.toLowerCase().replace(' ', '_');
    }

    public static long normalizedNow(int sec) {

        final long ts = new Date().getTime() / 1000;

        if (sec <= 0) {
            return ts;
        }

        return ts - (ts % sec);
    }

    public static String pad(String filler, int totalLetters,
            final String source, int beforeAfter) {

        String res;

        if (source != null) {
            res = source;
        } else {
            res = "";
        }

        final int paddingNeeded = totalLetters - res.length();

        if (paddingNeeded <= 0) {
            return res;
        }

        if (beforeAfter < 0) {

            for (int i = 0; i < paddingNeeded; i++) {
                res = filler + res;
            }
        } else {

            for (int i = 0; i < paddingNeeded; i++) {
                res += filler;
            }
        }

        return res;
    }

    public static String prettyJson(Object o) {

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(o);
    }
}
