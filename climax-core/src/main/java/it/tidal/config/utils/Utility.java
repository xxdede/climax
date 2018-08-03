package it.tidal.config.utils;

import com.google.gson.annotations.SerializedName;
import it.tidal.gson.GsonFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Mixed utils. A place to put some different things.
 *
 * @author dede
 */
public class Utility {

    public static final ZoneId systemZone = ZoneId.systemDefault();
    public static DateTimeFormatter basicDateTimeFormatter
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final NumberFormat americanDoubleFormatter
            = new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US));

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

    public static String slugFromAnnotation(Enum e) {

        try {
            return e.getClass()
                    .getField(e.name()).getAnnotation(SerializedName.class)
                    .value();
        } catch (Exception ex) {
            return null;
        }
    }

    public static long normalizedNow(long sec) {

        final long ts = new Date().getTime() / 1000;

        if (sec <= 0) {
            return ts;
        }

        return ts - (ts % sec);
    }

    public static long timestamp(LocalDateTime ldt) {

        return ldt.atZone(systemZone).toEpochSecond();
    }

    public static LocalDateTime localDateTime(long timestamp) {

        return Instant.ofEpochSecond(timestamp)
                .atZone(systemZone)
                .toLocalDateTime();
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

        return GsonFactory.prettyInstance().toJson(o);
    }

    public static String americanDouble(Double d) {
        return americanDoubleFormatter.format(d);
    }
}
