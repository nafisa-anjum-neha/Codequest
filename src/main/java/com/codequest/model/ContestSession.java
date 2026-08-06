package com.codequest.model;

import javafx.beans.property.*;

public class ContestSession {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty durationMinutes = new SimpleIntegerProperty();
    private final IntegerProperty problemsPlanned = new SimpleIntegerProperty();
    private final IntegerProperty problemsSolved = new SimpleIntegerProperty();
    private final IntegerProperty score = new SimpleIntegerProperty();
    private final StringProperty startedAt = new SimpleStringProperty();
    private final StringProperty endedAt = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public ContestSession(int id, String name, int durationMinutes, int problemsPlanned,
                           int problemsSolved, int score, String startedAt, String endedAt, String status) {
        this.id.set(id);
        this.name.set(name);
        this.durationMinutes.set(durationMinutes);
        this.problemsPlanned.set(problemsPlanned);
        this.problemsSolved.set(problemsSolved);
        this.score.set(score);
        this.startedAt.set(startedAt);
        this.endedAt.set(endedAt == null ? "" : endedAt);
        this.status.set(status);
    }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public int getDurationMinutes() { return durationMinutes.get(); }
    public int getProblemsPlanned() { return problemsPlanned.get(); }
    public int getProblemsSolved() { return problemsSolved.get(); }
    public int getScore() { return score.get(); }
    public String getStartedAt() { return startedAt.get(); }
    public String getEndedAt() { return endedAt.get(); }
    public String getStatus() { return status.get(); }

    public StringProperty nameProperty() { return name; }
    public IntegerProperty durationMinutesProperty() { return durationMinutes; }
    public IntegerProperty problemsSolvedProperty() { return problemsSolved; }
    public IntegerProperty scoreProperty() { return score; }
    public StringProperty statusProperty() { return status; }
}
