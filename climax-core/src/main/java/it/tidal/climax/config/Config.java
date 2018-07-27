package it.tidal.climax.config;

import java.io.Serializable;

/**
 * Configuration file. This class contains config params and other stuff.
 *
 * @author dede
 */
public class Config implements Serializable {

    private static final long serialVersionUID = 1L;

    static int LAST_MAJ_NUMBER = 0;
    static int LAST_MIN_NUMBER = 2;
    static int LAST_REV_NUMBER = 1;

    private int majNumber;
    private int minNumber;
    private int revNumber;

    private String installationName = "example";
    private boolean skipActions = true;
    private boolean alwaysConfigure = false;

    private NetAtmoConfig netAtmo = null;
    private CoolAutomationConfig coolAutomation = null;
    private MySQLConfig mySQL = null;
    private SolarEdgeConfig solarEdge = null;

    public Config() {

        majNumber = LAST_MAJ_NUMBER;
        minNumber = LAST_MIN_NUMBER;
        revNumber = LAST_REV_NUMBER;
    }

    public static String getLatestVersion() {

        return LAST_MAJ_NUMBER + "."
                + LAST_MIN_NUMBER + "."
                + LAST_REV_NUMBER;
    }

    public int getMajNumber() {
        return majNumber;
    }

    public void setMajNumber(int majNumber) {
        this.majNumber = majNumber;
    }

    public int getMinNumber() {
        return minNumber;
    }

    public void setMinNumber(int minNumber) {
        this.minNumber = minNumber;
    }

    public int getRevNumber() {
        return revNumber;
    }

    public void setRevNumber(int revNumber) {
        this.revNumber = revNumber;
    }

    public String getVersion() {

        return majNumber + "."
                + minNumber + "."
                + revNumber;
    }

    public boolean isLatestVersion() {

        return Config.getLatestVersion().equals(getVersion());
    }

    public String getInstallationName() {
        return installationName;
    }

    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    public boolean isSkipActions() {
        return skipActions;
    }

    public void setSkipActions(boolean skipActions) {
        this.skipActions = skipActions;
    }

    public NetAtmoConfig getNetAtmo() {
        return netAtmo;
    }

    public void setNetatmo(NetAtmoConfig netAtmo) {
        this.netAtmo = netAtmo;
    }

    public CoolAutomationConfig getCoolAutomation() {
        return coolAutomation;
    }

    public void setCoolAutomation(CoolAutomationConfig coolAutomation) {
        this.coolAutomation = coolAutomation;
    }

    public MySQLConfig getMySQL() {
        return mySQL;
    }

    public void setMySQL(MySQLConfig mySQL) {
        this.mySQL = mySQL;
    }

    public boolean isAlwaysConfigure() {
        return alwaysConfigure;
    }

    public void setAlwaysConfigure(boolean alwaysConfigure) {
        this.alwaysConfigure = alwaysConfigure;
    }

    public SolarEdgeConfig getSolarEdge() {
        return solarEdge;
    }

    public void setSolarEdge(SolarEdgeConfig solarEdge) {
        this.solarEdge = solarEdge;
    }
}
