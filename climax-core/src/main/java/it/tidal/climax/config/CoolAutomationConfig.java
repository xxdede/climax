package it.tidal.climax.config;

import java.io.Serializable;
import java.util.List;

public class CoolAutomationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CoolAutomationDeviceConfig> devices;

    public List<CoolAutomationDeviceConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<CoolAutomationDeviceConfig> devices) {
        this.devices = devices;
    }

    public CoolAutomationDeviceConfig find(String name) {

        if (devices == null || devices.isEmpty() || name == null) {
            return null;
        }

        for (CoolAutomationDeviceConfig device : devices) {

            if (name.equals(device.getName())) {
                return device;
            }
        }

        return null;
    }
}
