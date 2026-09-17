package com.codequest.model;

import javafx.beans.property.*;

public class Problem {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty platform = new SimpleStringProperty();
    private final StringProperty topic = new SimpleStringProperty();
    private final StringProperty difficulty = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty url = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final StringProperty dateAdded = new SimpleStringProperty();
    private final StringProperty dateSolved = new SimpleStringProperty();

    public Problem(int id, String title, String platform, String topic, String difficulty,
                   String status, String url, String notes, String dateAdded, String dateSolved) {
        this.id.set(id);
        this.title.set(title);
        this.platform.set(platform);
        this.topic.set(topic);
        this.difficulty.set(difficulty);
        this.status.set(status);
        this.url.set(url == null ? "" : url);
        this.notes.set(notes == null ? "" : notes);
        this.dateAdded.set(dateAdded);
        this.dateSolved.set(dateSolved == null ? "" : dateSolved);
    }

    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getPlatform() { return platform.get(); }
    public String getTopic() { return topic.get(); }
    public String getDifficulty() { return difficulty.get(); }
    public String getStatus() { return status.get(); }
    public String getUrl() { return url.get(); }
    public String getNotes() { return notes.get(); }
    public String getDateAdded() { return dateAdded.get(); }
    public String getDateSolved() { return dateSolved.get(); }

    public StringProperty titleProperty() { return title; }
    public StringProperty platformProperty() { return platform; }
    public StringProperty topicProperty() { return topic; }
    public StringProperty difficultyProperty() { return difficulty; }
    public StringProperty statusProperty() { return status; }
    public StringProperty urlProperty() { return url; }
    public StringProperty notesProperty() { return notes; }
    public StringProperty dateSolvedProperty() { return dateSolved; }
}
