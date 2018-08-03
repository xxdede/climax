/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.tidal.climax.database.mapping;

import it.tidal.climax.core.Illness;
import it.tidal.config.utils.Utility;
import java.io.Serializable;

public class RoomStatus implements Serializable {

    /*

    Database table creation and queries.

DROP TABLE `camera`;
CREATE TABLE `camera` (
	`time_sec` bigint(20) NOT NULL,
	`temperature` float(4, 1) NOT NULL,
	`humidity` tinyint DEFAULT NULL,
	`co2` smallint DEFAULT NULL,
	`perceived` float(4, 1) DEFAULT NULL,
	`illness` tinyint DEFAULT NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE `sala`;
CREATE TABLE `sala` (
	`time_sec` bigint(20) NOT NULL,
	`temperature` float(4, 1) NOT NULL,
	`humidity` tinyint DEFAULT NULL,
	`co2` smallint DEFAULT NULL,
	`perceived` float(4, 1) DEFAULT NULL,
	`illness` tinyint DEFAULT NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE `cantina`;
CREATE TABLE `cantina` (
	`time_sec` bigint(20) NOT NULL,
	`temperature` float(4, 1) NOT NULL,
	`humidity` tinyint DEFAULT NULL,
	`co2` smallint DEFAULT NULL,
	`perceived` float(4, 1) DEFAULT NULL,
	`illness` tinyint DEFAULT NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE `studio`;
CREATE TABLE `studio` (
	`time_sec` bigint(20) NOT NULL,
	`temperature` float(4, 1) NOT NULL,
	`humidity` tinyint DEFAULT NULL,
	`co2` smallint DEFAULT NULL,
	`perceived` float(4, 1) DEFAULT NULL,
	`illness` tinyint DEFAULT NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE `giardino`;
CREATE TABLE `giardino` (
	`time_sec` bigint(20) NOT NULL,
	`temperature` float(4, 1) NOT NULL,
	`humidity` tinyint DEFAULT NULL,
	`perceived` float(4, 1) DEFAULT NULL,
	`illness` tinyint DEFAULT NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SELECT * FROM camera; SELECT * FROM studio; SELECT * FROM sala; SELECT * FROM cantina; SELECT * FROM giardino;

     */
    String name;
    long timestamp;
    double temperature;
    Integer humidity;
    Integer co2;
    Double perceived;
    Illness.Level illness;

    public RoomStatus() {
    }

    public RoomStatus(String name,
            long timestamp, double temperature,
            Integer humidity, Integer co2,
            Double perceived, Illness.Level illness) {

        this.name = name;
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co2 = co2;
        this.perceived = perceived;
        this.illness = illness;
    }

    public RoomStatus(long timestamp, double temperature,
            Integer humidity, Integer co2,
            Double perceived, Illness.Level illness) {

        this(null, timestamp, temperature, humidity, co2, perceived, illness);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Integer getCo2() {
        return co2;
    }

    public void setCo2(Integer co2) {
        this.co2 = co2;
    }

    public Double getPerceived() {
        return perceived;
    }

    public void setPerceived(Double perceived) {
        this.perceived = perceived;
    }

    public Illness.Level getIllness() {
        return illness;
    }

    public void setIllness(Illness.Level illness) {
        this.illness = illness;
    }

    public String getDescription() {

        return Utility.pad(" ", 11, name, 1) + " "
                + "temperature: " + Utility.americanDoubleFormatter
                        .format(temperature) + "°"
                + ", humidity: " + humidity + "%"
                + ", co2: " + (co2 == null ? " - " : co2) + "ppm,"
                + " perceived temperature: " + Utility.americanDoubleFormatter
                        .format(perceived)
                + ", illness level: " + illness;
    }
}
