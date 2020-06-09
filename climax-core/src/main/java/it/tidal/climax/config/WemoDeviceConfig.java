package it.tidal.climax.config;

import com.google.gson.annotations.SerializedName;
import it.tidal.climax.extensions.managers.WemoManager;
import it.tidal.config.utils.DeviceFamiliable;
import it.tidal.config.utils.DeviceFamily;
import it.tidal.config.utils.Utility;
import java.io.Serializable;
import java.util.ArrayList;

public class WemoDeviceConfig implements Serializable, DeviceFamiliable {

    public enum Type {

        @SerializedName("switch")
        SWITCH(0),
        @SerializedName("insight")
        INSIGHT(1);

        private final int v;

        private Type(int v) {
            this.v = v;
        }

        public WemoManager.WemoDevice getWemoDevice() {

            switch (v) {

                case 0: return WemoManager.WemoDevice.SWITCH;
                case 1: return WemoManager.WemoDevice.INSIGHT;
                default: throw new RuntimeException("Unknown Wemo Device");
            }
        }

        public String getSlug() {
            return Utility.slugFromAnnotation(this);
        }
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private String ipAddress;
    private ArrayList<Integer> ports;
    private Type type;
    private String dbName;

    @Override
    public DeviceFamily getDeviceFamily() {
        return DeviceFamily.WEMO;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public ArrayList<Integer> getPorts() {
        return ports;
    }

    public void setPorts(ArrayList<Integer> ports) {
        this.ports = ports;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
}
