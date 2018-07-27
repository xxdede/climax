package it.tidal.climax.extensions.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * SolarEdge Energy Extensions. Contains energy details.
 *
 * @author dede
 */
public class SolarEdgeEnergy implements Serializable {

    private static final long serialVersionUID = 1L;

    private SolarEdge.TimeUnit timeUnit;
    private String unit;
    private ArrayList<SolarEdgeMeter> meters;

    public SolarEdgeEnergy(SolarEdge.TimeUnit timeUnit,
            String unit,
            ArrayList<SolarEdgeMeter> meters) {

        this.timeUnit = timeUnit;
        this.unit = unit;
        this.meters = meters;
    }

    public SolarEdge.TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(SolarEdge.TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ArrayList<SolarEdgeMeter> getMeters() {
        return meters;
    }

    public void setMeters(ArrayList<SolarEdgeMeter> meters) {
        this.meters = meters;
    }

    public double getMeter(SolarEdge.MeterType type) {

        if (type == null) {
            return 0;
        }

        for (SolarEdgeMeter m : meters) {
            if (type.equals(m.getType())) {
                if (m.getValues() != null && !m.getValues().isEmpty()) {

                    final Double v = m.getValues().get(0).getValue();
                    return (v != null ? v : 0);
                }
                break;
            }
        }

        return 0;
    }

    public Long getTimestamp() {

        for (SolarEdgeMeter m : meters) {
            if (m.getValues() != null && !m.getValues().isEmpty()) {

                final String d = m.getValues().get(0).getDate();
                final LocalDateTime ldt = LocalDateTime.parse(d, SolarEdge.dateFormatter);
                return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
            }
        }

        return null;
    }
}
