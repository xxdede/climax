package it.tidal.climax.extensions.data;

import java.io.Serializable;

/**
 * SolarEdge Energy Value. A single unit of SalarEdge value.
 *
 * @author dede
 */
public class SolarEdgeValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;
    private Double value;

    public SolarEdgeValue(String date, Double value) {

        this.date = date;
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
