package com.codequest.model;

import javafx.beans.property.*;

public class SyncLog {
    private final StringProperty timestamp = new SimpleStringProperty();
    private final StringProperty direction = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();

    public SyncLog(String timestamp, String direction, String status, String message) {
        this.timestamp.set(timestamp);
        this.direction.set(direction);
        this.status.set(status);
        this.message.set(message);
    }

    public String getTimestamp() { return timestamp.get(); }
    public String getDirection() { return direction.get(); }
    public String getStatus() { return status.get(); }
    public String getMessage() { return message.get(); }

    public StringProperty timestampProperty() { return timestamp; }
    public StringProperty directionProperty() { return direction; }
    public StringProperty statusProperty() { return status; }
    public StringProperty messageProperty() { return message; }
}
