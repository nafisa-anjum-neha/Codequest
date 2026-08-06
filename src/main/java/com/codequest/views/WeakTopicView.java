package com.codequest.views;

import com.codequest.db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.sql.*;
import java.util.*;

/**
 * Feature 6 — Weak Topic Detection.
 * Cross-references topic completion percentages with problem-solving activity
 * per topic to flag areas that need more focus, and offers an adjustable
 * sensitivity threshold so the student can control how strict the detector is.
 */
public class WeakTopicView {

    private final BorderPane root = new BorderPane();
    private final VBox listBox = new VBox(12);
    private final Slider thresholdSlider = new Slider(10, 80, 40);
    private final Label thresholdLabel = new Label("Flag topics below 40% completion");

    public WeakTopicView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Weak Topic Detection");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Automatically flags topics that need more attention");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        thresholdSlider.setPrefWidth(260);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(10);
        thresholdSlider.valueProperty().addListener((o, ov, nv) -> {
            thresholdLabel.setText("Flag topics below " + nv.intValue() + "% completion");
            refresh();
        });
        HBox thresholdRow = new HBox(14, thresholdLabel, thresholdSlider);
        thresholdRow.setAlignment(Pos.CENTER_LEFT);
        thresholdRow.getStyleClass().add("filter-bar");
        thresholdRow.setPadding(new Insets(12));

        listBox.setPadding(new Insets(6, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        VBox center = new VBox(16, thresholdRow, scroll);
        center.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setCenter(center);

        refresh();
    }

    public Node getRoot() { return root; }

    public void refresh() {
        listBox.getChildren().clear();
        int threshold = (int) thresholdSlider.getValue();
        List<String[]> weakTopics = new ArrayList<>(); // name, category, pct, solvedForTopic, attemptedForTopic

        String sql = """
            SELECT t.name, t.category, t.completion_percent,
                   (SELECT COUNT(*) FROM problems p WHERE p.topic = t.name AND p.status = 'Solved') AS solved,
                   (SELECT COUNT(*) FROM problems p WHERE p.topic = t.name) AS attempted
            FROM topics t
            ORDER BY t.completion_percent ASC
        """;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int pct = rs.getInt("completion_percent");
                if (pct < threshold) {
                    weakTopics.add(new String[]{
                            rs.getString("name"), rs.getString("category"),
                            String.valueOf(pct), String.valueOf(rs.getInt("solved")), String.valueOf(rs.getInt("attempted"))
                    });
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
            return;
        }

        if (weakTopics.isEmpty()) {
            Label none = new Label("No weak topics at this threshold — nice work! Try raising the slider to review more.");
            none.getStyleClass().add("view-subtitle");
            listBox.getChildren().add(none);
            return;
        }

        for (String[] row : weakTopics) {
            listBox.getChildren().add(buildWeakCard(row[0], row[1], Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]), Integer.parseInt(row[4])));
        }
    }

    private HBox buildWeakCard(String name, String category, int pct, int solved, int attempted) {
        Circle dot = new Circle(7, pct < 20 ? Color.web("#ff6b6b") : Color.web("#ffb454"));

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("chart-title");
        Label meta = new Label(category + "  •  " + pct + "% complete  •  " + solved + "/" + attempted + " problems solved on this topic");
        meta.getStyleClass().add("view-subtitle");
        VBox textBox = new VBox(4, nameLabel, meta);

        Label suggestion = new Label(pct < 20 ? "High priority" : "Needs review");
        suggestion.getStyleClass().add(pct < 20 ? "small-button-danger" : "small-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox card = new HBox(14, dot, textBox, spacer, suggestion);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(14));
        return card;
    }
}
