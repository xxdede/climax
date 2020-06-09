package it.tidal.climax.extensions.managers;

import it.tidal.climax.config.MySQLConfig;
import it.tidal.climax.database.mapping.EnergyStatus;
import it.tidal.climax.database.mapping.HVACStatus;
import it.tidal.climax.database.mapping.IntakeStatus;
import it.tidal.climax.database.mapping.RoomStatus;
import it.tidal.climax.extensions.data.SolarEdge;
import it.tidal.climax.extensions.data.SolarEdgeEnergy;
import it.tidal.config.utils.Utility;
import it.tidal.logging.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import net.sf.persist.Persist;

/**
 * Database Extensions. Connects to MySQL databases.
 *
 * @author dede
 */
public class DatabaseManager {

    private static Log l = Log.prepare(DatabaseManager.class.getSimpleName());

    private final static String SOLAR_EDGE_TABLE = "solar_edge_energy";

    private Persist persist = null;

    public DatabaseManager(String host, int port,
            String database, String username, String password) {

        try {

            final String cs = "jdbc:mysql://" + host + ":" + port
                    + "/" + database
                    + "?useSSL=false"
                    + "&user=" + username
                    + "&password=" + password;

            //Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(cs);

            persist = new Persist(connection);

            //l.debug("Connected to: \"" +cs + "\".");
        } catch (Exception ex) {

            l.error("A problem occurred while while initializing DB.", ex);
        }
    }

    public static DatabaseManager getInstance(MySQLConfig config) {

        if (config == null) {

            l.error("No config specified for Database");
            return null;
        }

        return new DatabaseManager(config.getHost(), config.getPort(),
                config.getDatabase(), config.getUsername(),
                config.getPassword());
    }

    public int countRows(String tableName) {

        return persist.read(Integer.class,
                "SELECT COUNT(*) FROM " + tableName);
    }

    public void insertRoomStatus(RoomStatus status) {

        if (persist == null) {
            return;
        }

        StringBuilder sql = new StringBuilder(64);
        StringBuilder values = new StringBuilder(64);

        sql.append("INSERT INTO `").append(Utility.sluggize(status.getName()))
                .append("` (`time_sec`,`temperature`");

        values.append("(").append(status.getTimestamp()).append(",")
                .append(Utility.americanDoubleFormatter.
                        format(status.getTemperature()));

        if (status.getHumidity() != null) {

            sql.append(",`humidity`");
            values.append(",").append(status.getHumidity());
        }

        if (status.getCo2() != null) {

            sql.append(",`co2`");
            values.append(",").append(status.getCo2());
        }

        if (status.getPerceived() != null) {

            sql.append(",`perceived`");
            values.append(",").append(status.getPerceived());
        }

        if (status.getIllnessEnum() != null) {

            sql.append(",`illness`");
            values.append(",").append(status.getIllnessEnum().getValue());
        }

        sql.append(")");
        values.append(")");
        sql.append(" VALUES ").append(values);

        //l.debug(sql.toString());
        persist.execute(sql.toString());
    }

    public void insertHVACStatus(HVACStatus status) {

        if (persist == null) {
            return;
        }

        StringBuilder sql = new StringBuilder(64);
        StringBuilder values = new StringBuilder(64);

        sql.append("INSERT INTO `").append(Utility.sluggize(status.getName()))
                .append("` (`time_sec`,`offset`");

        values.append("(").append(status.getTimestamp()).append(",")
                .append(status.getOffset());

        if (status.getStatus() != null) {

            sql.append(",`status`");
            values.append(",").append(status.getStatus());
        }

        if (status.getMode() != null) {

            sql.append(",`mode`");
            values.append(",").append(status.getMode());
        }

        if (status.getFanSpeed() != null) {

            sql.append(",`fan_speed`");
            values.append(",").append(status.getFanSpeed());
        }

        if (status.getRoomTemperature() != null) {

            sql.append(",`room_temperature`");
            values.append(",").append(status.getRoomTemperature());
        }

        if (status.getSetTemperature() != null) {

            sql.append(",`set_temperature`");
            values.append(",").append(status.getSetTemperature());
        }

        sql.append(")");
        values.append(")");
        sql.append(" VALUES ").append(values);

        //l.debug(sql.toString());
        persist.execute(sql.toString());
    }

    public void checkAndInsertSolarEdgeEnergy(SolarEdgeEnergy energy) {

        if (persist == null || energy == null) {
            return;
        }

        TreeMap<Long, HashMap<SolarEdge.MeterType, Double>> mmap;
        mmap = energy.getEnergyMultiMap();

        for (Long ts : mmap.keySet()) {

            // Check if value is already stored
            if (persist.read(Long.class, "SELECT COUNT(*) FROM `"
                    + SOLAR_EDGE_TABLE + "` WHERE `time_sec` = ?", ts) != 0) {
                l.info("Database already contain SolarEdge data for timestamp "
                        + ts + ", skipping...");
                continue;
            }

            HashMap<SolarEdge.MeterType, Double> map;
            map = mmap.get(ts);

            Double tempValue;
            StringBuilder sql = new StringBuilder(64);
            StringBuilder values = new StringBuilder(64);

            sql.append("INSERT INTO `" + SOLAR_EDGE_TABLE + "` (`time_sec`");
            values.append("(").append(ts);

            tempValue = map.get(SolarEdge.MeterType.Production);
            if (tempValue != null) {
                sql.append(",`production`");
                values.append(",").append(tempValue);
            }

            tempValue = map.get(SolarEdge.MeterType.Consumption);
            if (tempValue != null) {
                sql.append(",`consumption`");
                values.append(",").append(tempValue);
            }

            tempValue = map.get(SolarEdge.MeterType.SelfConsumption);
            if (tempValue != null) {
                sql.append(",`self_consumption`");
                values.append(",").append(tempValue);
            }

            tempValue = map.get(SolarEdge.MeterType.FeedIn);
            if (tempValue != null) {
                sql.append(",`feed_in`");
                values.append(",").append(tempValue);
            }

            tempValue = map.get(SolarEdge.MeterType.Purchased);
            if (tempValue != null) {
                sql.append(",`purchased`");
                values.append(",").append(tempValue);
            }

            sql.append(")");
            values.append(")");
            sql.append(" VALUES ").append(values);

            //l.debug(sql.toString());
            persist.execute(sql.toString());
        }
    }

    public void insertIntakeStatus(IntakeStatus status) {

        if (persist == null) {
            return;
        }

        StringBuilder sql = new StringBuilder(64);
        StringBuilder values = new StringBuilder(64);

        sql.append("INSERT INTO `").append(Utility.sluggize(status.getName()))
                .append("` (`time_sec`,`offset`,`open`");

        values.append("(").append(status.getTimestamp()).append(",")
                .append(status.getOffset()).append(",")
                .append(status.isOpen() ? "1" : "0");

        sql.append(")");
        values.append(")");
        sql.append(" VALUES ").append(values);

        //l.debug(sql.toString());
        persist.execute(sql.toString());
    }

    public TreeMap<LocalDateTime, EnergyStatus> retrieveLastEnergyStatus(int count) {

        if (persist == null) {
            return null;
        }

        TreeMap<LocalDateTime, EnergyStatus> tm = new TreeMap<>();

        try {

            persist.addSuggestedClassTableName(EnergyStatus.class, SOLAR_EDGE_TABLE);
            List<EnergyStatus> ess = persist.readList(EnergyStatus.class,
                    "SELECT * FROM `" + SOLAR_EDGE_TABLE
                    + "` ORDER BY `time_sec` DESC LIMIT " + count);
            persist.clearSuggestedTableNames();

            if (ess != null) {
                for (EnergyStatus es : ess) {
                    tm.put(Utility.localDateTime(es.getTimestamp()), es);
                }
            }

        } catch (Exception ex) {

            l.error("A problem occurred while retrieving SolarEdge energy!", ex);
        }

        return tm;
    }

    public TreeMap<LocalDateTime, EnergyStatus> splitQuarterOfHourToTenMinutesEnergyStatuses(TreeMap<LocalDateTime, EnergyStatus> quartered) {

        TreeMap<LocalDateTime, EnergyStatus> tenned = new TreeMap<>();

        for (LocalDateTime moment : quartered.keySet()) {

            final LocalDateTime momentAlignedToQuarter = Utility.normalizeToQuarterOfHour(moment);

            final LocalDateTime momentAlignedToTen = Utility.normalizeToTenMinutes(moment);
            final LocalDateTime momentAlignedToTenPlusTen = momentAlignedToTen.plusMinutes(10);

            EnergyStatus es15 = quartered.get(moment);

            EnergyStatus es10 = tenned.get(momentAlignedToTen);
            EnergyStatus es10plus10 = tenned.get(momentAlignedToTenPlusTen);

            if (es10 == null) {

                es10 = new EnergyStatus(Utility.timestamp(momentAlignedToTen), 0.0, 0.0, 0.0, 0.0, 0.0);
                tenned.put(momentAlignedToTen, es10);
            }
            if (es10plus10 == null) {

                es10plus10 = new EnergyStatus(Utility.timestamp(momentAlignedToTenPlusTen), 0.0, 0.0, 0.0, 0.0, 0.0);
                tenned.put(momentAlignedToTenPlusTen, es10plus10);
            }

            switch (momentAlignedToQuarter.getMinute()) {

                case 0:
                    // Here es10 is HH:00
                    es10.importFromAnother(es15, 1.0 * 2 / 3);
                    // And es10plus10 is HH:10
                    es10plus10.importFromAnother(es15, 1.0 * 1 / 3);
                    break;
                case 15:
                    // Here es10 is HH:10
                    es10.importFromAnother(es15, 1.0 * 1 / 3);
                    // And es10plus10 is HH:20
                    es10plus10.importFromAnother(es15, 1.0 * 2 / 3);
                    break;
                case 30:
                    // Here es10 is HH:30
                    es10.importFromAnother(es15, 1.0 * 2 / 3);
                    // And es10plus10 is HH:40
                    es10plus10.importFromAnother(es15, 1.0 * 1 / 3);
                    break;
                case 45:
                    // Here es10 is HH:40
                    es10.importFromAnother(es15, 1.0 * 1 / 3);
                    // And es10plus10 is HH:50
                    es10plus10.importFromAnother(es15, 1.0 * 2 / 3);
                    break;
            }
        }

        return tenned;
    }

    public TreeMap<LocalDateTime, HVACStatus> retrieveLastHVACStatus(String deviceOrTableName, int offset, int count) {

        if (persist == null) {
            return null;
        }

        TreeMap<LocalDateTime, HVACStatus> tm = new TreeMap<>();

        try {

            persist.addSuggestedClassTableName(HVACStatus.class, deviceOrTableName);
            List<HVACStatus> hss = persist.readList(HVACStatus.class,
                    "SELECT * FROM `" + deviceOrTableName + "`"
                    + " WHERE offset = ?"
                    + " ORDER BY `time_sec` DESC LIMIT " + count, offset);
            persist.clearSuggestedTableNames();

            if (hss != null) {
                for (HVACStatus hs : hss) {
                    tm.put(Utility.localDateTime(hs.getTimestamp()), hs);
                }
            }

        } catch (Exception ex) {

            l.error("A problem occurred while retrieving HVAC status '" + deviceOrTableName + "' energy!", ex);
        }

        return tm;
    }

    public TreeMap<LocalDateTime, RoomStatus> retrieveLastRoomStatus(String deviceOrTableName, int count) {

        if (persist == null) {
            return null;
        }

        TreeMap<LocalDateTime, RoomStatus> tm = new TreeMap<>();

        try {

            persist.addSuggestedClassTableName(RoomStatus.class, deviceOrTableName);
            List<RoomStatus> rss = persist.readList(RoomStatus.class,
                    "SELECT * FROM `" + deviceOrTableName + "`"
                    + " ORDER BY `time_sec` DESC LIMIT " + count);
            persist.clearSuggestedTableNames();

            if (rss != null) {
                for (RoomStatus rs : rss) {
                    tm.put(Utility.localDateTime(rs.getTimestamp()), rs);
                }
            }

        } catch (Exception ex) {

            l.error("A problem occurred while retrieving room status '" + deviceOrTableName + "' !", ex);
        }

        return tm;
    }

    public void dispose() {

        if (persist == null) {
            return;
        }

        try {

            persist.getConnection().close();
        } catch (Exception ex) {

            l.error("A problem occurred while while disposing DB.", ex);
        }
    }
}
