package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import it.tidal.config.utils.Utility;
import java.io.Serializable;

public class GenericDeviceConfig implements Serializable {

    public enum OperationMode {

        @SerializedName("not-specified")
        NOT_SPECIFIED(0),
        @SerializedName("disabled")
        DISABLED(1),
        @SerializedName("enabled")
        ENABLED(2),
        @SerializedName("operate-auto")
        OPERATE_AUTO(3),
        @SerializedName("operate-if-on")
        OPERATE_IF_ON(4),
        @SerializedName("operate-if-on-or-self-off")
        OPERATE_IF_ON_OR_SELF_OFF(5);

        private final int v;

        private OperationMode(int v) {
            this.v = v;
        }

        public String getSlug() {
            return Utility.slugFromAnnotation(this);
        }
    }

    public enum Role {

        @SerializedName("none")
        NONE(0),
        @SerializedName("outside-temperature-provider")
        OUTSIDE_TEMPERATURE_PROVIDER(1),
        @SerializedName("inside-temperature-provider")
        INSIDE_TEMPERATURE_PROVIDER(2),
        @SerializedName("intake-temperature-provider")
        INTAKE_TEMPERATURE_PROVIDER(3),
        @SerializedName("intake-actuator")
        INTAKE_ACTUATOR(4);

        private final int v;

        private Role(int v) {
            this.v = v;
        }

        public String getSlug() {
            return Utility.slugFromAnnotation(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private Role role;

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
