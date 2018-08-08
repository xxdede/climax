package it.tidal.climax.database.mapping;

import it.tidal.climax.extensions.data.CoolAutomation;
import java.io.Serializable;
import net.sf.persist.annotations.Column;
import net.sf.persist.annotations.NoColumn;

public class HVACStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /*

    Database table creation and queries.

DROP TABLE `daikin_0`;
CREATE TABLE `daikin_0` (
	`time_sec` bigint(20) NOT NULL,
        `offset` tinyint DEFAULT 0,
        `status` tinyint DEFAULT NULL,
        `mode` tinyint DEFAULT NULL,
        `fan_speed` tinyint DEFAULT NULL,
        `room_temperature` float(4, 1) DEFAULT NULL,
        `set_temperature` float(4, 1) DEFAULT NULL,
	PRIMARY KEY (`time_sec`,`offset`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE `daikin_1`;
CREATE TABLE `daikin_1` (
	`time_sec` bigint(20) NOT NULL,
        `offset` tinyint DEFAULT 0,
        `status` tinyint DEFAULT NULL,
        `mode` tinyint DEFAULT NULL,
        `fan_speed` tinyint DEFAULT NULL,
        `room_temperature` float(4, 1) DEFAULT NULL,
        `set_temperature` float(4, 1) DEFAULT NULL,
	PRIMARY KEY (`time_sec`,`offset`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


     */
    String name;
    long timestamp;
    int offset;
    Integer status;
    Integer mode;
    Integer fanSpeed;
    Double roomTemperature;
    Double setTemperature;

    public HVACStatus() {
    }

    public HVACStatus(String name, long timestamp,
            int offset, CoolAutomation ca) {

        this.name = name;
        this.timestamp = timestamp;
        this.offset = offset;

        if (ca.getStatus() != null) {
            this.status = ca.getStatus().getValue();
        }

        if (ca.getOpMode() != null) {
            this.mode = ca.getOpMode().getValue();
        }

        if (ca.getFanSpeed() != null) {
            this.fanSpeed = ca.getFanSpeed().getValue();
        }

        this.roomTemperature = ca.getRoomTemperature();
        this.setTemperature = ca.getSetTemperature();
    }

    public void setCoolAutomation(CoolAutomation ca) {

        if (ca == null) {
            return;
        }

        if (ca.getStatus() != null) {
            this.status = ca.getStatus().getValue();
        } else {
            this.status = null;
        }

        if (ca.getOpMode() != null) {
            this.mode = ca.getOpMode().getValue();
        } else {
            this.mode = null;
        }

        if (ca.getFanSpeed() != null) {
            this.fanSpeed = ca.getFanSpeed().getValue();
        } else {
            this.fanSpeed = null;
        }

        this.roomTemperature = ca.getRoomTemperature();
        this.setTemperature = ca.getSetTemperature();
    }

    @NoColumn
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "time_sec")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Integer getStatus() {
        return status;
    }

    @NoColumn
    public CoolAutomation.Status getStatusEnum() {

        return CoolAutomation.Status.fromInteger(status);
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getMode() {
        return mode;
    }

    @NoColumn
    public CoolAutomation.OpMode getModeEnum() {

        return CoolAutomation.OpMode.fromInteger(mode);
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public Integer getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(Integer fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    @NoColumn
    public CoolAutomation.FanSpeed getFanSpeedEnum() {

        return CoolAutomation.FanSpeed.fromInteger(fanSpeed);
    }

    public Double getRoomTemperature() {
        return roomTemperature;
    }

    public void setRoomTemperature(Double roomTemperature) {
        this.roomTemperature = roomTemperature;
    }

    public Double getSetTemperature() {
        return setTemperature;
    }

    public void setSetTemperature(Double setTemperature) {
        this.setTemperature = setTemperature;
    }

    public String getSummary() {

        final CoolAutomation.Status st = getStatusEnum();
        final CoolAutomation.OpMode om = getModeEnum();
        final CoolAutomation.FanSpeed fs = getFanSpeedEnum();

        return " measured: " + roomTemperature
                + " - " + (st != null ? st.getName() : "?")
                + ", " + (om != null ? om.getName() : "?")
                + ", " + (fs != null ? fs.getName() : "?")
                + ", set: " + setTemperature;
    }
}
