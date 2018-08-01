package it.tidal.climax.config;

import java.io.Serializable;
import java.util.List;

public class WemoConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<WemoDeviceConfig> devices;

    public List<WemoDeviceConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<WemoDeviceConfig> devices) {
        this.devices = devices;
    }

    public WemoDeviceConfig find(String name) {

        if (devices == null || devices.isEmpty() || name == null) {
            return null;
        }

        for (WemoDeviceConfig device : devices) {

            if (name.equals(device.getName())) {
                return device;
            }
        }

        return null;
    }
}
