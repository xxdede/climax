package it.tidal.gson;

import com.google.gson.GsonBuilder;
import it.tidal.config.utils.Hour;
import java.time.LocalDateTime;

public class GsonFactory {

    private static com.google.gson.Gson standardInstance;
    private static com.google.gson.Gson prettyInstance;

    public static com.google.gson.Gson instance() {

        synchronized (GsonFactory.class) {

            if (standardInstance == null) {

                standardInstance = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class,
                                new LocalDateTimeAdapter())
                        .registerTypeAdapter(Hour.class,
                                new HourAdapter())
                        .create();
            }
        }

        return standardInstance;
    }

    public static com.google.gson.Gson prettyInstance() {

        synchronized (GsonFactory.class) {

            if (prettyInstance == null) {

                prettyInstance = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class,
                                new LocalDateTimeAdapter())
                        .registerTypeAdapter(Hour.class,
                                new HourAdapter())
                        .setPrettyPrinting()
                        .create();
            }
        }

        return prettyInstance;
    }

}
