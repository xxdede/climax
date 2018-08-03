package it.tidal.climax.database.mapping;

import java.io.Serializable;

public class EnergyStatus implements Serializable {

    /*

    Database table creation and queries.

DROP TABLE `solar_edge_energy`;
CREATE TABLE `solar_edge_energy` (
	`time_sec` bigint(20) NOT NULL,
        `production` float(5, 2) NULL,
        `consumption` float(5, 2) NULL,
        `self_consumption` float(5, 2) NULL,
        `feed_in` float(5, 2) NULL,
        `purchased` float(5, 2) NULL,
	PRIMARY KEY (`time_sec`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

     */
    long timestamp;
    Double production;
    Double consumption;
    Double selfConsumption;
    Double feedIn;
    Double purchased;

    public EnergyStatus() {
    }

    public EnergyStatus(long timestamp, Double production, Double consumption,
            Double selfConsumption, Double feedIn, Double purchased) {

        this.timestamp = timestamp;
        this.production = production;
        this.consumption = consumption;
        this.selfConsumption = selfConsumption;
        this.feedIn = feedIn;
        this.purchased = purchased;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getProduction() {
        return production;
    }

    public void setProduction(Double production) {
        this.production = production;
    }

    public Double getConsumption() {
        return consumption;
    }

    public void setConsumption(Double consumption) {
        this.consumption = consumption;
    }

    public Double getSelfConsumption() {
        return selfConsumption;
    }

    public void setSelfConsumption(Double selfConsumption) {
        this.selfConsumption = selfConsumption;
    }

    public Double getFeedIn() {
        return feedIn;
    }

    public void setFeedIn(Double feedIn) {
        this.feedIn = feedIn;
    }

    public Double getPurchased() {
        return purchased;
    }

    public void setPurchased(Double purchased) {
        this.purchased = purchased;
    }
}
