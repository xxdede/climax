package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import it.tidal.config.utils.DeviceFamily;

public class GenericDeviceConfig {

    public enum Role {

        @SerializedName("outside-temperature-provider")
        OUTSIDE_TEMPERATURE_PROVIDER(0),
        @SerializedName("inside-temperature-provider")
        INSIDE_TEMPERATURE_PROVIDER(1),
        @SerializedName("intake-temperature-provider")
        INTAKE_TEMPERATURE_PROVIDER(2),
        @SerializedName("intake-actuator")
        INTAKE_ACTUATOR(3);

        private final int v;

        private Role(int v) {
            this.v = v;
        }
    }

    private static final long serialVersionUID = 1L;

    private DeviceFamily family;
    private String name;
    private Role role;

    public DeviceFamily getFamily() {
        return family;
    }

    public void setFamily(DeviceFamily family) {
        this.family = family;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
