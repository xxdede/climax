package it.tidal.climax.extensions.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * SolarEdge Energy Meter. A collection of SalarEdge values.
 *
 * @author dede
 */
public class SolarEdgeMeter implements Serializable {

    private static final long serialVersionUID = 1L;

    private SolarEdge.MeterType type;
    private ArrayList<SolarEdgeValue> values;

    public SolarEdgeMeter(SolarEdge.MeterType type, ArrayList<SolarEdgeValue> values) {

        this.type = type;
        this.values = values;
    }

    public SolarEdge.MeterType getType() {
        return type;
    }

    public void setType(SolarEdge.MeterType type) {
        this.type = type;
    }

    public ArrayList<SolarEdgeValue> getValues() {
        return values;
    }

    public void setValues(ArrayList<SolarEdgeValue> values) {
        this.values = values;
    }
}
