package it.tidal.config.utils;

public abstract class AdvancedTemperatureSensor extends BasicTemperatureSensor implements HumidityQueryable, Co2Queryable, PerceivedQueryable, IllnessQueryable {

    protected Double perceived;
    protected Integer illness;

    public Double getPerceived() {
        return perceived;
    }

    public void setPerceived(Double perceived) {
        this.perceived = perceived;
    }

    public Integer getIllness() {
        return illness;
    }

    public void setIllness(Integer illness) {
        this.illness = illness;
    }
}
