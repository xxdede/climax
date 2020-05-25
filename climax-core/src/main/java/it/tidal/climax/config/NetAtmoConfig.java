package it.tidal.climax.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NetAtmoConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private String scope;
    private List<NetAtmoDeviceConfig> devices;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<NetAtmoDeviceConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<NetAtmoDeviceConfig> devices) {
        this.devices = devices;
    }

    public NetAtmoDeviceConfig find(String name) {

        if (devices == null || devices.isEmpty() || name == null) {
            return null;
        }

        for (NetAtmoDeviceConfig device : devices) {

            if (name.equals(device.getName())) {
                return device;
            }
        }

        return null;
    }
}
