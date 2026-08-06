package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.Goal;
import com.codequest.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 7 — Goal Setting System.
 * Students set a target ("solve 20 problems" / "complete 5 topics") with an
 * optional deadline. Progress is computed live from the problems/topics tables
 * (based on activity since the goal's start date) rather than tracked by hand.
 */
public class GoalSettingView {

    private final BorderPane root = new BorderPane();
    private final VBox goalsBox = new VBox(14);

    public GoalSettingView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Goal Setting System");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Set concrete targets and watch your progress update automatically");
        subtitle.getStyleClass().add("view-subtitle");
        VBox headerText = new VBox(4, title, subtitle);

        Button addBtn = new Button("+ New Goal");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openGoalDialog());

        HBox header = new HBox(headerText, spacer(), addBtn);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        goalsBox.setPadding(new Insets(6, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(goalsBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        VBox center = new VBox(scroll);
        center.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setCenter(center);

        refresh();
    }

    public Node getRoot() { return root; }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private void openGoalDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Goal");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Solve 20 problems this month");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Problems Solved", "Topics Completed"));
        typeBox.setValue("Problems Solved");
        Spinner<Integer> targetSpinner = new Spinner<>(1, 500, 10, 1);
        targetSpinner.setEditable(true);
        TextField deadlineField = new TextField();
        deadlineField.setPromptText("YYYY-MM-DD (optional)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Type:"), typeBox);
        grid.addRow(2, new Label("Target:"), targetSpinner);
        grid.addRow(3, new Label("Deadline:"), deadlineField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK || titleField.getText().isBlank()) return;
            String sql = "INSERT INTO goals (title, goal_type, target_value, start_date, deadline, status) VALUES (?,?,?,?,?,'Active')";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, titleField.getText());
                ps.setString(2, typeBox.getValue());
                ps.setInt(3, targetSpinner.getValue());
                ps.setString(4, DateUtil.today());
                ps.setString(5, deadlineField.getText().isBlank() ? null : deadlineField.getText());
                ps.executeUpdate();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
            }
            refresh();
        });
    }

    public void refresh() {
        goalsBox.getChildren().clear();
        List<Goal> goals = new ArrayList<>();

        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM goals ORDER BY id DESC")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("goal_type");
                String startDate = rs.getString("start_date");
                int current = computeCurrentValue(type, startDate);
                int target = rs.getInt("target_value");
                String status = rs.getString("status");
                if (!status.equals("Completed") && current >= target) {
                    markCompleted(id);
                    status = "Completed";
                }
                goals.add(new Goal(id, rs.getString("title"), type, target, startDate,
                        rs.getString("deadline"), status, current));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }

        if (goals.isEmpty()) {
            Label none = new Label("No goals yet — click \"+ New Goal\" to set your first target.");
            none.getStyleClass().add("view-subtitle");
            goalsBox.getChildren().add(none);
            return;
        }

        for (Goal g : goals) {
            goalsBox.getChildren().add(buildGoalCard(g));
        }
    }

    private int computeCurrentValue(String type, String startDate) throws SQLException {
        String sql = type.equals("Problems Solved")
                ? "SELECT COUNT(*) c FROM problems WHERE status = 'Solved' AND date_solved >= ?"
                : "SELECT COUNT(*) c FROM topics WHERE status = 'Completed' AND last_updated >= ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("c");
        }
    }

    private void markCompleted(int id) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement("UPDATE goals SET status = 'Completed' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }

    private VBox buildGoalCard(Goal g) {
        Label titleLabel = new Label(g.getTitle());
        titleLabel.getStyleClass().add("chart-title");

        Label metaLabel = new Label(g.getGoalType() + "  •  started " + g.getStartDate()
                + (g.getDeadline().isBlank() ? "" : "  •  due " + g.getDeadline())
                + "  •  " + g.getStatus());
        metaLabel.getStyleClass().add("view-subtitle");

        double frac = g.getTargetValue() == 0 ? 0 : Math.min(1.0, g.getCurrentValue() / (double) g.getTargetValue());
        ProgressBar bar = new ProgressBar(frac);
        bar.setPrefWidth(320);
        bar.getStyleClass().add("styled-progress-bar");
        Label pctLabel = new Label(g.getCurrentValue() + " / " + g.getTargetValue());
        pctLabel.getStyleClass().add("progress-row-label");
        HBox barRow = new HBox(10, bar, pctLabel);
        barRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, titleLabel, metaLabel, barRow);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(16));
        return card;
    }
}
