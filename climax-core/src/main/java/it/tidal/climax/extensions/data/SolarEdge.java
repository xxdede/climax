package it.tidal.climax.extensions.data;

import java.time.format.DateTimeFormatter;

/**
 * SolarEdge Definitions. Groups SolarEdge constant and definitions.
 *
 * @author dede
 */
public class SolarEdge {

    public static DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum TimeUnit {

        QUARTER_OF_AN_HOUR,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public enum MeterType {

        SelfConsumption,
        Consumption,
        Production,
        Purchased,
        FeedIn
    }

}
