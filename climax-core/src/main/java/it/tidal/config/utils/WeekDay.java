package it.tidal.config.utils;

import com.google.gson.annotations.SerializedName;

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

    @Override
    public String toString() {

        switch (v) {
            case 0:
                return "monday";
            case 1:
                return "tuesday";
            case 2:
                return "wednesday";
            case 3:
                return "thursday";
            case 4:
                return "friday";
            case 5:
                return "saturday";
            case 6:
                return "sunday";
        }

        return "unknown";
    }

}
