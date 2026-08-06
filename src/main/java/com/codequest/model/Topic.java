package com.codequest.model;

import javafx.beans.property.*;

public class Topic {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final IntegerProperty orderIndex = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final IntegerProperty completionPercent = new SimpleIntegerProperty();

    public Topic(int id, String name, String category, int orderIndex, String status, int completionPercent) {
        this.id.set(id);
        this.name.set(name);
        this.category.set(category);
        this.orderIndex.set(orderIndex);
        this.status.set(status);
        this.completionPercent.set(completionPercent);
    }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public int getOrderIndex() { return orderIndex.get(); }
    public String getStatus() { return status.get(); }
    public int getCompletionPercent() { return completionPercent.get(); }

    public void setStatus(String s) { status.set(s); }
    public void setCompletionPercent(int p) { completionPercent.set(p); }

    public StringProperty nameProperty() { return name; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty statusProperty() { return status; }
    public IntegerProperty completionPercentProperty() { return completionPercent; }
}
