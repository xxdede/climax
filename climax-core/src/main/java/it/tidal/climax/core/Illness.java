package it.tidal.climax.core;

import java.io.Serializable;

/**
 * Illness level and perceived temperature calculator. This class is used to
 * calculate perceived temperature (humidex) and illness level, to help adjust
 * HVAC and thermostat settings.
 *
 * @author dede
 */
public class Illness implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Level {

        NONE(0),
        LIGHT(1),
        MEDIUM(2),
        HIGH(3),
        GRAVE(4),
        PANIC(5);

        private final int v;

        private Level(int v) {
            this.v = v;
        }

        public int getValue() {

            return v;
        }

        public String getSlug() {

            switch (v) {

                case 0:
                    return "none";
                case 1:
                    return "light";
                case 2:
                    return "medium";
                case 3:
                    return "high";
                case 4:
                    return "grave";
                case 5:
                    return "panic";
            }

            return "?";
        }

        public String getLongDescription() {

            switch (v) {

                case 0:
                    return "Optimal situation";
                case 1:
                    return "Very light harassment";
                case 2:
                    return "Simple malaise, limit heavy physical activity";
                case 3:
                    return "Malaise, danger, avoid physical activity";
                case 4:
                    return "Serious danger, stop all activities";
                case 5:
                    return "Imminent heat stroke, danger of death";
            }

            return "?";
        }

        public static Level fromString(String s) {

            for (Level il : Level.values()) {
                if (il.getSlug().equalsIgnoreCase(s)) {
                    return il;
                }
            }

            return null;
        }

        public static Level fromInteger(Integer i) {

            if (i != null) {
                for (Level il : Level.values()) {
                    if (il.getValue() == i) {
                        return il;
                    }
                }
            }

            return null;
        }
    }

    public static int compare(Illness.Level il1, Illness.Level il2) {

        switch (il1) {

            case NONE: {

                switch (il2) {

                    case NONE:
                        return 0;
                    case LIGHT:
                        return 1;
                    case MEDIUM:
                        return 2;
                    case HIGH:
                        return 3;
                    case GRAVE:
                        return 4;
                    case PANIC:
                        return 5;
                }
            }
            case LIGHT: {

                switch (il2) {

                    case NONE:
                        return -1;
                    case LIGHT:
                        return 0;
                    case MEDIUM:
                        return 1;
                    case HIGH:
                        return 2;
                    case GRAVE:
                        return 3;
                    case PANIC:
                        return 4;
                }
            }
            case MEDIUM: {

                switch (il2) {

                    case NONE:
                        return -2;
                    case LIGHT:
                        return -1;
                    case MEDIUM:
                        return 0;
                    case HIGH:
                        return 1;
                    case GRAVE:
                        return 2;
                    case PANIC:
                        return 3;
                }
            }
            case HIGH: {

                switch (il2) {

                    case NONE:
                        return -3;
                    case LIGHT:
                        return -2;
                    case MEDIUM:
                        return -1;
                    case HIGH:
                        return 0;
                    case GRAVE:
                        return 1;
                    case PANIC:
                        return 2;
                }
            }
            case GRAVE: {

                switch (il2) {

                    case NONE:
                        return -4;
                    case LIGHT:
                        return -3;
                    case MEDIUM:
                        return -2;
                    case HIGH:
                        return -1;
                    case GRAVE:
                        return 0;
                    case PANIC:
                        return 1;
                }
            }
            case PANIC: {

                switch (il2) {

                    case NONE:
                        return -5;
                    case LIGHT:
                        return -4;
                    case MEDIUM:
                        return -3;
                    case HIGH:
                        return -2;
                    case GRAVE:
                        return -1;
                    case PANIC:
                        return 0;
                }
            }
        }

        return 0;
    }

    public static double computeHumidexValue(double temperature, int humidity) {

        final int humidex[][] = {
            // Matrix - rows: temperature in °C from 18 to 42 (step 1)
            //          cols: rel. humidity in % from 25 to 100 (step 5)
            {18, 18, 18, 18, 18, 18, 18, 19, 19, 19, 19, 20, 20, 20, 21, 21},
            {19, 19, 19, 19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 22, 22, 23},
            {20, 20, 20, 20, 20, 21, 21, 21, 21, 21, 22, 22, 22, 23, 23, 24},
            {21, 21, 21, 21, 22, 22, 22, 22, 22, 23, 23, 24, 24, 25, 26, 27},
            {22, 22, 22, 22, 23, 24, 25, 25, 26, 27, 27, 28, 29, 30, 30, 31},
            {23, 23, 23, 24, 25, 25, 26, 27, 28, 28, 29, 30, 31, 32, 32, 33},
            {24, 24, 24, 25, 26, 27, 28, 28, 29, 30, 31, 32, 33, 33, 34, 35},
            {25, 25, 26, 27, 27, 28, 29, 30, 31, 32, 33, 34, 34, 35, 36, 37},
            {26, 26, 27, 28, 29, 30, 31, 32, 33, 34, 34, 35, 36, 37, 38, 39},
            {27, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41},
            {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43},
            {29, 30, 31, 32, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 45, 46},
            {30, 32, 33, 34, 35, 36, 37, 39, 40, 41, 42, 43, 45, 46, 47, 48},
            {32, 33, 34, 35, 37, 38, 39, 40, 42, 43, 44, 45, 47, 48, 49, 50},
            {33, 34, 36, 37, 38, 40, 41, 42, 44, 45, 46, 48, 49, 50, 52, 53},
            {34, 36, 37, 39, 40, 41, 43, 44, 46, 47, 48, 50, 51, 53, 54, 55},
            {36, 37, 39, 40, 42, 43, 45, 46, 48, 49, 51, 52, 54, 55, 57, 58},
            {37, 39, 40, 42, 44, 45, 47, 48, 50, 51, 53, 54, 56, 58, 59, 61},
            {39, 40, 42, 44, 45, 47, 49, 50, 52, 54, 55, 57, 59, 60, 62, 63},
            {40, 42, 44, 45, 47, 49, 51, 52, 54, 56, 58, 59, 61, 63, 65, 66},
            {42, 44, 45, 47, 49, 51, 53, 55, 56, 58, 60, 62, 64, 66, 67, 69},
            {43, 45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 66, 68, 70, 72},
            {45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 67, 69, 71, 73, 75},
            {46, 48, 51, 53, 55, 57, 59, 61, 64, 66, 68, 70, 72, 74, 76, 79},
            {48, 50, 52, 55, 57, 59, 62, 64, 66, 68, 71, 73, 75, 77, 80, 82}
        };

        int t, h;

        if (temperature < 18) {
            return temperature + ((-50.0 + humidity) / 50);
        } else if (temperature > 42) {
            t = 24;
        } else {
            t = (int) Math.floor(temperature) - 18;
        }

        if (humidity <= 25) {
            h = 0;
        } else if (humidity <= 30) {
            h = 1;
        } else if (humidity <= 35) {
            h = 2;
        } else if (humidity <= 40) {
            h = 3;
        } else if (humidity <= 45) {
            h = 4;
        } else if (humidity <= 50) {
            h = 5;
        } else if (humidity <= 55) {
            h = 6;
        } else if (humidity <= 60) {
            h = 7;
        } else if (humidity <= 65) {
            h = 8;
        } else if (humidity <= 70) {
            h = 9;
        } else if (humidity <= 75) {
            h = 10;
        } else if (humidity <= 80) {
            h = 11;
        } else if (humidity <= 85) {
            h = 12;
        } else if (humidity <= 90) {
            h = 13;
        } else if (humidity <= 95) {
            h = 14;
        } else {
            h = 15;
        }

        double humMin = humidex[t][h];
        double humMax = humidex[t + 1][h];
        double q = temperature % 1;

        return (humMin * (1 - q) + humMax * q);
    }

    public static Illness.Level compute(double humidex) {

        if (humidex <= 30) {
            return Illness.Level.NONE;
        } else if (humidex <= 34) {
            return Illness.Level.LIGHT;
        } else if (humidex <= 39) {
            return Illness.Level.MEDIUM;
        } else if (humidex <= 45) {
            return Illness.Level.HIGH;
        } else if (humidex <= 53) {
            return Illness.Level.GRAVE;
        } else {
            return Illness.Level.PANIC;
        }
    }

    public static Illness.Level compute(double temperature, int humidity) {

        return compute(computeHumidexValue(temperature, humidity));
    }
}
