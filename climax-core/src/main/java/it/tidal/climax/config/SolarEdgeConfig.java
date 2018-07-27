package it.tidal.climax.config;

import java.io.Serializable;

public class SolarEdgeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private String key;
    private String site;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

}
