package it.tidal.config.utils;

import java.io.Serializable;

public class Hour implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int hour;
    private final int minute;

    public Hour(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    @Override
    public String toString() {
        return "" + hour + ":" + (minute < 10 ? "0" : "") + minute;
    }

    public static Hour fromString(String representation) {

        int hour = -1;
        int minute = -1;

        if (representation == null) {
            return null;
        }

        final int firstColon = representation.indexOf(":");

        if (firstColon < 0) {
            return null;
        }

        try {
            hour = Integer.parseInt(representation.substring(0, firstColon));
        } catch (Exception ex) {
            return null;
        }

        if (hour < 0 || hour > 23) {
            return null;
        }

        try {
            minute = Integer.parseInt(representation.substring(firstColon + 1));
        } catch (Exception ex) {
            return null;
        }

        if (minute < 0 || minute > 59) {
            return null;
        }

        return new Hour(hour, minute);
    }
}
