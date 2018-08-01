package it.tidal.climax.extensions.data;

/**
 * SolarEdge Definitions. Groups SolarEdge constant and definitions.
 *
 * @author dede
 */
public class SolarEdge {

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
