package it.tidal.climax.extensions.data;

import it.tidal.climax.database.mapping.HVACStatus;
import it.tidal.climax.database.mapping.RoomStatus;
import it.tidal.config.utils.AdvancedTemperatureSensor;
import it.tidal.config.utils.GenericIntakeDetector;
import it.tidal.config.utils.IntakeDetector;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ClimaxPack implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private HVACStatus lastHVACStatus;

    private ArrayList<Double> roomTemperatures;
    private ArrayList<Integer> roomHumidities;

    private AdvancedTemperatureSensor roomStatus;
    private AdvancedTemperatureSensor outsideStatus;
    private GenericIntakeDetector intakeDetector;

    private CoolAutomation currentHVACConfig;
    private CoolAutomation desiredHVACConfig;

    public ClimaxPack() {
    }

    public ClimaxPack(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HVACStatus getLastHVACStatus() {
        return lastHVACStatus;
    }

    public void setLastHVACStatus(HVACStatus lastHVACStatus) {
        this.lastHVACStatus = lastHVACStatus;
    }

    public ArrayList<Double> getRoomTemperatures() {
        return roomTemperatures;
    }

    public void setRoomTemperatures(ArrayList<Double> roomTemperatures) {
        this.roomTemperatures = roomTemperatures;
    }

    public ArrayList<Integer> getRoomHumidities() {
        return roomHumidities;
    }

    public void setRoomHumidities(ArrayList<Integer> roomHumidities) {
        this.roomHumidities = roomHumidities;
    }

    public AdvancedTemperatureSensor getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(AdvancedTemperatureSensor roomStatus) {
        this.roomStatus = roomStatus;
    }

    public GenericIntakeDetector getIntakeDetector() {
        return intakeDetector;
    }

    public void setIntakeDetector(GenericIntakeDetector intakeDetector) {
        this.intakeDetector = intakeDetector;
    }

    public AdvancedTemperatureSensor getOutsideStatus() {
        return outsideStatus;
    }

    public void setOutsideStatus(AdvancedTemperatureSensor outsideStatus) {
        this.outsideStatus = outsideStatus;
    }

    public CoolAutomation getCurrentHVACConfig() {
        return currentHVACConfig;
    }

    public void setCurrentHVACConfig(CoolAutomation currentHVACConfig) {
        this.currentHVACConfig = currentHVACConfig;
    }

    public CoolAutomation getDesiredHVACConfig() {
        return desiredHVACConfig;
    }

    public void setDesiredHVACConfig(CoolAutomation desiredHVACConfig) {
        this.desiredHVACConfig = desiredHVACConfig;
    }

    public void updateTemperatureAndHumidityWithRoomStatuses(TreeMap<LocalDateTime, RoomStatus> rss) {

        this.roomTemperatures = new ArrayList<>(rss.size());
        this.roomHumidities = new ArrayList<>(rss.size());

        for (Map.Entry<LocalDateTime, RoomStatus> rs : rss.entrySet()) {

            this.roomTemperatures.add(rs.getValue().getTemperature());

            if (rs.getValue().getHumidity() != null) {
                this.roomHumidities.add(rs.getValue().getHumidity());
            }
        }
    }
}
