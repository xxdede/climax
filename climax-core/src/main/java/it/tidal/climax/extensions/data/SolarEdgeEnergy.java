package it.tidal.climax.extensions.data;

import it.tidal.config.utils.Utility;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

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

    public Long getFirstTimestamp() {

        for (SolarEdgeMeter m : meters) {
            if (m.getValues() != null && !m.getValues().isEmpty()) {

                final String d = m.getValues().get(0).getDate();
                final LocalDateTime ldt = LocalDateTime.parse(d, Utility.basicDateTimeFormatter);
                return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
            }
        }

        return null;
    }

    public TreeMap<Long, HashMap<SolarEdge.MeterType, Double>> getEnergyMultiMap() {

        final TreeMap<Long, HashMap<SolarEdge.MeterType, Double>> ret;

        ret = new TreeMap<>();

        // Find all timestamp (i.e. the keys)
        for (SolarEdgeMeter m : meters) {
            if (m.getValues() != null && m.getValues() != null) {
                for (SolarEdgeValue v : m.getValues()) {

                    final long ts = LocalDateTime.parse(v.getDate(), Utility.basicDateTimeFormatter)
                            .atZone(ZoneId.systemDefault()).toEpochSecond();

                    HashMap<SolarEdge.MeterType, Double> subMap = ret.get(ts);

                    if (subMap == null) {

                        subMap = new HashMap<>(5);
                        ret.put(ts, subMap);
                    }

                    subMap.put(m.getType(), v.getValue());
                }
            }
        }

        return ret;
    }
}
