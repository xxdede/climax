package it.tidal.config.utils;

import com.google.gson.annotations.SerializedName;

public enum DeviceFamily {

    @SerializedName("wemo")
    WEMO(0),
    @SerializedName("netatmo")
    NETATMO(1);

    private final int v;

    private DeviceFamily(int v) {
        this.v = v;
    }

    public String getDescription() {
        switch (v) {
            case 0:
                return "Wemo";
            case 1:
                return "NetAtmo";
        }
        return null;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
