package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.Topic;
import com.codequest.util.DateUtil;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 1 — Learning Roadmap.
 * Renders topics grouped by category (Basics -> Intermediate -> Advanced -> Expert)
 * as a connected path of nodes. Clicking a node lets the student jump straight to
 * updating its status, which keeps the roadmap and the Topic Progress Tracker in sync.
 */
public class RoadmapView {

    private final BorderPane root = new BorderPane();
    private final VBox pathContainer = new VBox(0);
    private Runnable onDataChanged = () -> {};

    public RoadmapView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Learning Roadmap");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Your step-by-step path from Basics to Advanced algorithms");
        subtitle.getStyleClass().add("view-subtitle");

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        pathContainer.setPadding(new Insets(10, 40, 40, 40));
        ScrollPane scroll = new ScrollPane(pathContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        root.setCenter(scroll);

        refresh();
    }

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public Node getRoot() { return root; }

    public void refresh() {
        pathContainer.getChildren().clear();
        Map<String, List<Topic>> byCategory = loadTopics();

        String[] order = {"Basics", "Intermediate", "Advanced", "Expert"};
        for (String category : order) {
            List<Topic> topics = byCategory.get(category);
            if (topics == null) continue;

            Label catLabel = new Label(category);
            catLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
            catLabel.getStyleClass().add("roadmap-category-label");
            VBox catBox = new VBox(10, catLabel);
            catBox.setPadding(new Insets(20, 0, 10, 0));
            pathContainer.getChildren().add(catBox);

            FlowPane row = new FlowPane();
            row.setHgap(18);
            row.setVgap(18);
            for (Topic t : topics) {
                row.getChildren().add(buildNode(t));
            }
            pathContainer.getChildren().add(row);

            if (!category.equals(order[order.length - 1])) {
                Line connector = new Line(0, 0, 0, 30);
                connector.setStroke(Color.web("#5865f2"));
                connector.setStrokeWidth(3);
                HBox lineWrap = new HBox(connector);
                lineWrap.setAlignment(Pos.CENTER_LEFT);
                lineWrap.setPadding(new Insets(0, 0, 0, 20));
                pathContainer.getChildren().add(lineWrap);
            }
        }
    }

    private StackPane buildNode(Topic t) {
        Circle dot = new Circle(9);
        dot.setFill(colorFor(t.getStatus()));

        Label name = new Label(t.getName());
        name.setWrapText(true);
        name.setMaxWidth(150);
        name.getStyleClass().add("roadmap-node-label");

        Label pct = new Label(t.getCompletionPercent() + "%");
        pct.getStyleClass().add("roadmap-node-pct");

        HBox topRow = new HBox(8, dot, pct);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, topRow, name);
        card.setPadding(new Insets(14));
        card.setPrefWidth(180);
        card.getStyleClass().addAll("roadmap-node", "status-" + statusClass(t.getStatus()));

        card.setOnMouseClicked(e -> openStatusDialog(t));

        StackPane wrap = new StackPane(card);
        return wrap;
    }

    private void openStatusDialog(Topic t) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Topic");
        dialog.setHeaderText(t.getName() + "  (" + t.getCategory() + ")");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Not Started", "In Progress", "Completed");
        statusBox.setValue(t.getStatus());

        Slider slider = new Slider(0, 100, t.getCompletionPercent());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        Label pctLabel = new Label(t.getCompletionPercent() + "%");
        slider.valueProperty().addListener((obs, o, n) -> pctLabel.setText(n.intValue() + "%"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Status:"), statusBox);
        grid.addRow(1, new Label("Completion:"), new HBox(10, slider, pctLabel));

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                updateTopic(t.getId(), statusBox.getValue(), (int) slider.getValue());
                refresh();
                onDataChanged.run();
            }
        });
    }

    private void updateTopic(int id, String status, int percent) {
        String sql = "UPDATE topics SET status = ?, completion_percent = ?, last_updated = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, percent);
            ps.setString(3, DateUtil.now());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private Map<String, List<Topic>> loadTopics() {
        Map<String, List<Topic>> map = new LinkedHashMap<>();
        String sql = "SELECT * FROM topics ORDER BY category, order_index";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Topic t = new Topic(
                        rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                        rs.getInt("order_index"), rs.getString("status"), rs.getInt("completion_percent")
                );
                map.computeIfAbsent(t.getCategory(), k -> new ArrayList<>()).add(t);
            }
        } catch (SQLException e) {
            showError(e);
        }
        return map;
    }

    private String statusClass(String status) {
        return switch (status) {
            case "Completed" -> "completed";
            case "In Progress" -> "in-progress";
            default -> "not-started";
        };
    }

    private Color colorFor(String status) {
        return switch (status) {
            case "Completed" -> Color.web("#3ddc97");
            case "In Progress" -> Color.web("#ffb454");
            default -> Color.web("#6c7086");
        };
    }

    private void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
        alert.showAndWait();
    }
}
