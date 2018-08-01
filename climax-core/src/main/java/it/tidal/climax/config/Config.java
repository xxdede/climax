package it.tidal.climax.config;

import java.io.Serializable;
import java.util.List;

/**
 * Configuration file. This class contains config params and other stuff.
 *
 * @author dede
 */
public class Config implements Serializable {

    private static final long serialVersionUID = 1L;

    static int LAST_MAJ_NUMBER = 0;
    static int LAST_MIN_NUMBER = 3;
    static int LAST_REV_NUMBER = 0;

    private int majNumber;
    private int minNumber;
    private int revNumber;

    private String installationName = "example";

    private List<ProgramConfig> programs;
    private MySQLConfig mySQL;
    private SolarEdgeConfig solarEdge;
    private NetAtmoConfig netAtmo;
    private CoolAutomationConfig coolAutomation;
    private WemoConfig wemo;

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

    public List<ProgramConfig> getPrograms() {
        return programs;
    }

    public void setPrograms(List<ProgramConfig> programs) {
        this.programs = programs;
    }

    public MySQLConfig getMySQL() {
        return mySQL;
    }

    public void setMySQL(MySQLConfig mySQL) {
        this.mySQL = mySQL;
    }

    public SolarEdgeConfig getSolarEdge() {
        return solarEdge;
    }

    public void setSolarEdge(SolarEdgeConfig solarEdge) {
        this.solarEdge = solarEdge;
    }

    public NetAtmoConfig getNetAtmo() {
        return netAtmo;
    }

    public void setNetAtmo(NetAtmoConfig netAtmo) {
        this.netAtmo = netAtmo;
    }

    public CoolAutomationConfig getCoolAutomation() {
        return coolAutomation;
    }

    public void setCoolAutomation(CoolAutomationConfig coolAutomation) {
        this.coolAutomation = coolAutomation;
    }

    public WemoConfig getWemo() {
        return wemo;
    }

    public void setWemo(WemoConfig wemo) {
        this.wemo = wemo;
    }
}
