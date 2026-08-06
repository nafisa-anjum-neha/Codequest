package com.codequest.views;

import com.codequest.db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.sql.*;

/**
 * Feature 8 — Achievement System.
 * Badges are computed live from current stats (no separate "unlocked" table
 * needed) so they always reflect the true state of the student's progress.
 */
public class AchievementView {

    private final BorderPane root = new BorderPane();
    private final FlowPane badgesPane = new FlowPane(16, 16);

    public AchievementView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Achievement System");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Badges you unlock as you build your Competitive Programming habit");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        badgesPane.setPadding(new Insets(6, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(badgesPane);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        VBox center = new VBox(scroll);
        center.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setCenter(center);

        refresh();
    }

    public Node getRoot() { return root; }

    public void refresh() {
        badgesPane.getChildren().clear();
        int solved = 0, topicsCompleted = 0, contestsRun = 0, categoriesFullyDone = 0, distinctSolveDays = 0;

        try (Statement st = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) c FROM problems WHERE status = 'Solved'");
            rs.next(); solved = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(*) c FROM topics WHERE status = 'Completed'");
            rs.next(); topicsCompleted = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(*) c FROM contest_sessions");
            rs.next(); contestsRun = rs.getInt("c");

            rs = st.executeQuery("""
                SELECT COUNT(*) c FROM (
                    SELECT category FROM topics GROUP BY category
                    HAVING SUM(CASE WHEN status = 'Completed' THEN 0 ELSE 1 END) = 0
                )
            """);
            rs.next(); categoriesFullyDone = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(DISTINCT date_solved) c FROM problems WHERE date_solved IS NOT NULL AND date_solved != ''");
            rs.next(); distinctSolveDays = rs.getInt("c");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }

        addBadge("🩸", "First Blood", "Solve your first problem", solved >= 1);
        addBadge("🔥", "On a Roll", "Solve 10 problems", solved >= 10);
        addBadge("🏆", "Half Century", "Solve 50 problems", solved >= 50);
        addBadge("📘", "Topic Master", "Complete your first topic", topicsCompleted >= 1);
        addBadge("🎓", "Category Conqueror", "Fully complete a whole category", categoriesFullyDone >= 1);
        addBadge("⏱", "Contest Rookie", "Run your first mock contest", contestsRun >= 1);
        addBadge("📅", "Consistency", "Solve problems on 3 different days", distinctSolveDays >= 3);
        addBadge("👑", "Grandmaster in Training", "Solve 100 problems", solved >= 100);
    }

    private void addBadge(String icon, String name, String description, boolean unlocked) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 34px;" + (unlocked ? "" : " -fx-opacity: 0.25;"));

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("chart-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("view-subtitle");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(160);

        Label statusLabel = new Label(unlocked ? "Unlocked" : "Locked");
        statusLabel.getStyleClass().add(unlocked ? "small-button" : "small-button-danger");

        VBox card = new VBox(8, iconLabel, nameLabel, descLabel, statusLabel);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(190);
        if (!unlocked) card.setOpacity(0.6);

        badgesPane.getChildren().add(card);
    }
}
