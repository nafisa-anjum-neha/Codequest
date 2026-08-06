package com.codequest.model;

import javafx.beans.property.*;

public class Goal {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty goalType = new SimpleStringProperty();
    private final IntegerProperty targetValue = new SimpleIntegerProperty();
    private final StringProperty startDate = new SimpleStringProperty();
    private final StringProperty deadline = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final IntegerProperty currentValue = new SimpleIntegerProperty();

    public Goal(int id, String title, String goalType, int targetValue, String startDate,
                String deadline, String status, int currentValue) {
        this.id.set(id);
        this.title.set(title);
        this.goalType.set(goalType);
        this.targetValue.set(targetValue);
        this.startDate.set(startDate);
        this.deadline.set(deadline == null ? "" : deadline);
        this.status.set(status);
        this.currentValue.set(currentValue);
    }

    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getGoalType() { return goalType.get(); }
    public int getTargetValue() { return targetValue.get(); }
    public String getStartDate() { return startDate.get(); }
    public String getDeadline() { return deadline.get(); }
    public String getStatus() { return status.get(); }
    public int getCurrentValue() { return currentValue.get(); }

    public StringProperty titleProperty() { return title; }
    public StringProperty goalTypeProperty() { return goalType; }
    public StringProperty statusProperty() { return status; }
    public StringProperty deadlineProperty() { return deadline; }
}
