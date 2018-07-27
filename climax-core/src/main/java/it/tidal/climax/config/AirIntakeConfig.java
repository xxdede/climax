package it.tidal.climax.config;

public class AirIntakeConfig {

    public enum Status {

        OPEN,
        CLOSED,
        AUTO
    }

    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
