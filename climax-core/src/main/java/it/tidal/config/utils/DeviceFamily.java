package it.tidal.config.utils;

import com.google.gson.annotations.SerializedName;
import it.tidal.climax.config.NetAtmoDeviceConfig;
import it.tidal.climax.config.WemoDeviceConfig;
import java.lang.reflect.Type;

public enum DeviceFamily {

    @SerializedName("wemo")
    WEMO(0),
    @SerializedName("netatmo")
    NETATMO(1),
    @SerializedName("coolautomation")
    COOLAUTOMATION(2);

    private final int v;

    private DeviceFamily(int v) {
        this.v = v;
    }

    public String getSlug() {
        return Utility.slugFromAnnotation(this);
    }

    @Override
    public String toString() {
        return getSlug();
    }

    public Type getConfigClass() {
        switch (v) {
            case 0:
                return WemoDeviceConfig.class;
            case 1:
                return NetAtmoDeviceConfig.class;
            case 2:
                return NetAtmoDeviceConfig.class;
        }
        return null;
    }
}
