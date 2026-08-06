package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.Topic;
import com.codequest.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature 2 — Topic Progress Tracker.
 * A management-focused table view of every topic with inline status/percent editing,
 * plus rolled-up progress bars per category and an overall completion summary.
 */
public class ProgressTrackerView {

    private final BorderPane root = new BorderPane();
    private final TableView<Topic> table = new TableView<>();
    private final ObservableList<Topic> data = FXCollections.observableArrayList();
    private final VBox summaryBox = new VBox(10);
    private Runnable onDataChanged = () -> {};

    public ProgressTrackerView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Topic Progress Tracker");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Track and update your mastery of every topic");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        buildTable();

        summaryBox.setPadding(new Insets(20));
        summaryBox.setPrefWidth(300);
        summaryBox.getStyleClass().add("summary-panel");

        HBox content = new HBox(20, table, summaryBox);
        content.setPadding(new Insets(10, 24, 24, 24));
        HBox.setHgrow(table, Priority.ALWAYS);
        root.setCenter(content);

        refresh();
    }

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public Node getRoot() { return root; }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setEditable(true);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Topic, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Topic, String> nameCol = new TableColumn<>("Topic");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Topic, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn("Not Started", "In Progress", "Completed"));
        statusCol.setOnEditCommit(e -> {
            Topic t = e.getRowValue();
            int newPct = e.getNewValue().equals("Completed") ? 100 : t.getCompletionPercent();
            persist(t.getId(), e.getNewValue(), newPct);
            refresh();
        });

        TableColumn<Topic, Integer> pctCol = new TableColumn<>("Completion %");
        pctCol.setCellValueFactory(new PropertyValueFactory<>("completionPercent"));
        pctCol.setCellFactory(col -> new TableCell<>() {
            private final Slider slider = new Slider(0, 100, 0);
            {
                slider.setPrefWidth(140);
            }
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                slider.setValue(value);
                slider.setOnMouseReleased(e -> {
                    Topic t = getTableView().getItems().get(getIndex());
                    int v = (int) slider.getValue();
                    String status = v == 100 ? "Completed" : (v == 0 ? "Not Started" : "In Progress");
                    persist(t.getId(), status, v);
                    refresh();
                });
                Label lbl = new Label(value + "%");
                lbl.setMinWidth(36);
                HBox box = new HBox(8, slider, lbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        table.getColumns().addAll(categoryCol, nameCol, statusCol, pctCol);
    }

    private void persist(int id, String status, int percent) {
        String sql = "UPDATE topics SET status = ?, completion_percent = ?, last_updated = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, percent);
            ps.setString(3, DateUtil.now());
            ps.setInt(4, id);
            ps.executeUpdate();
            onDataChanged.run();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }

    public void refresh() {
        data.clear();
        Map<String, int[]> categoryTotals = new LinkedHashMap<>(); // [sumPercent, count]
        int overallSum = 0, overallCount = 0;

        String sql = "SELECT * FROM topics ORDER BY category, order_index";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Topic t = new Topic(
                        rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                        rs.getInt("order_index"), rs.getString("status"), rs.getInt("completion_percent")
                );
                data.add(t);
                categoryTotals.computeIfAbsent(t.getCategory(), k -> new int[2]);
                categoryTotals.get(t.getCategory())[0] += t.getCompletionPercent();
                categoryTotals.get(t.getCategory())[1]++;
                overallSum += t.getCompletionPercent();
                overallCount++;
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }

        summaryBox.getChildren().clear();
        Label summaryTitle = new Label("Progress Summary");
        summaryTitle.getStyleClass().add("summary-title");
        summaryBox.getChildren().add(summaryTitle);

        int overallPct = overallCount == 0 ? 0 : overallSum / overallCount;
        summaryBox.getChildren().add(buildProgressRow("Overall", overallPct));

        for (var entry : categoryTotals.entrySet()) {
            int pct = entry.getValue()[1] == 0 ? 0 : entry.getValue()[0] / entry.getValue()[1];
            summaryBox.getChildren().add(buildProgressRow(entry.getKey(), pct));
        }
    }

    private VBox buildProgressRow(String label, int percent) {
        Label l = new Label(label + "  —  " + percent + "%");
        l.getStyleClass().add("progress-row-label");
        ProgressBar bar = new ProgressBar(percent / 100.0);
        bar.setPrefWidth(260);
        bar.getStyleClass().add("styled-progress-bar");
        VBox box = new VBox(4, l, bar);
        box.setPadding(new Insets(6, 0, 6, 0));
        return box;
    }
}
