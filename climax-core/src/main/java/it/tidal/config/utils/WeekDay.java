package it.tidal.config.utils;

import com.google.gson.annotations.SerializedName;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public enum WeekDay {

    @SerializedName("monday")
    MONDAY(0),
    @SerializedName("tuesday")
    TUESDAY(1),
    @SerializedName("wednesday")
    WEDNESDAY(2),
    @SerializedName("thursday")
    THURSDAY(3),
    @SerializedName("friday")
    FRIDAY(4),
    @SerializedName("saturday")
    SATURDAY(5),
    @SerializedName("sunday")
    SUNDAY(6);

    private final int v;

    private WeekDay(int v) {
        this.v = v;
    }

    public String getSlug() {
        return Utility.slugFromAnnotation(this);
    }

    public static WeekDay fromDayOfWeek(DayOfWeek dow) {

        if (dow == null) {
            return null;
        }

        switch (dow) {
            case MONDAY:
                return WeekDay.MONDAY;
            case TUESDAY:
                return WeekDay.TUESDAY;
            case WEDNESDAY:
                return WeekDay.WEDNESDAY;
            case THURSDAY:
                return WeekDay.THURSDAY;
            case FRIDAY:
                return WeekDay.FRIDAY;
            case SATURDAY:
                return WeekDay.SATURDAY;
            case SUNDAY:
                return WeekDay.SUNDAY;
        }

        return null;
    }

    public static WeekDay fromLocalDateTime(LocalDateTime ldt) {

        if (ldt == null) {
            return null;
        }

        return fromDayOfWeek(ldt.getDayOfWeek());
    }

    @Override
    public String toString() {
        return getSlug();
    }

}
