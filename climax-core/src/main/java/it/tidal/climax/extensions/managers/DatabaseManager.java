package it.tidal.climax.extensions.managers;

import it.tidal.climax.config.MySQLConfig;
import it.tidal.climax.core.Application;
import it.tidal.climax.database.mapping.HVACStatus;
import it.tidal.climax.database.mapping.RoomStatus;
import it.tidal.climax.extensions.data.SolarEdge;
import it.tidal.climax.extensions.data.SolarEdgeEnergy;
import it.tidal.config.utils.Utility;
import it.tidal.logging.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.TreeMap;
import net.sf.persist.Persist;

/**
 * Database Extensions. Connects to MySQL databases.
 *
 * @author dede
 */
public class DatabaseManager {

    private static Log l = Log.prepare(DatabaseManager.class.getSimpleName());

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
                .append(Application.DOUBLE_FMT.format(status.getTemperature()));

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

        if (status.getIllness() != null) {

            sql.append(",`illness`");
            values.append(",").append(status.getIllness().ordinal());
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
            if (persist.read(Long.class, "SELECT COUNT(*) FROM `solar_edge_energy`"
                    + " WHERE `time_sec` = ?", energy.getFirstTimestamp()) != 0) {
                continue;
            }

            HashMap<SolarEdge.MeterType, Double> map;
            map = mmap.get(ts);

            Double tempValue;
            StringBuilder sql = new StringBuilder(64);
            StringBuilder values = new StringBuilder(64);

            sql.append("INSERT INTO `solar_edge_energy` (`time_sec`");
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
