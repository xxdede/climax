package it.tidal.climax.database.mapping;

import net.sf.persist.annotations.Column;
import net.sf.persist.annotations.NoColumn;

public class IntakeStatus {

    private static final long serialVersionUID = 1L;

       /*

    Database table creation and queries.

    DROP TABLE `bocchetta`;
    CREATE TABLE `bocchetta` (
        `time_sec` bigint(20) NOT NULL,
        `offset` tinyint(4) NOT NULL DEFAULT '0',
        `open` tinyint NOT NULL,
        PRIMARY KEY (`time_sec`,`offset`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    */

    String name;
    long timestamp;
    int offset;
    boolean open;

    public IntakeStatus() {
    }

    public IntakeStatus(String name, long timestamp, int offset, boolean open) {
        this.name = name;
        this.timestamp = timestamp;
        this.offset = offset;
        this.open = open;
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

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
