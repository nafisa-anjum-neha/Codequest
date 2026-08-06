package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.ContestSession;
import com.codequest.util.DateUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.*;

/**
 * Future Enhancement 1 — Contest Simulation Module.
 * Lets the student run a timed, Codeforces-style mock contest: pick a duration
 * and number of problems, start a live countdown, log solves as they go, and
 * end with a final score that is saved to contest history.
 */
public class ContestSimulationView {

    private final BorderPane root = new BorderPane();
    private final TableView<ContestSession> historyTable = new TableView<>();
    private final ObservableList<ContestSession> history = FXCollections.observableArrayList();

    private final Label timerLabel = new Label("00:00");
    private final Label statusLabel = new Label("No contest running");
    private final Spinner<Integer> durationSpinner = new Spinner<>(5, 300, 60, 5);
    private final Spinner<Integer> problemsSpinner = new Spinner<>(1, 10, 5, 1);
    private final Spinner<Integer> solvedSpinner = new Spinner<>(0, 10, 0, 1);
    private final Button startBtn = new Button("▶ Start Contest");
    private final Button endBtn = new Button("■ End Contest");

    private Timeline countdown;
    private int remainingSeconds;
    private int currentDuration, currentProblems;
    private String currentStartedAt;

    public ContestSimulationView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Contest Simulation Module");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Practice under real timed-contest pressure");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        VBox setupCard = buildSetupCard();
        VBox liveCard = buildLiveCard();
        HBox topRow = new HBox(20, setupCard, liveCard);

        buildHistoryTable();
        Label historyTitle = new Label("Contest History");
        historyTitle.getStyleClass().add("chart-title");
        VBox historyBox = new VBox(10, historyTitle, historyTable);
        historyBox.getStyleClass().add("chart-card");
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        VBox center = new VBox(20, topRow, historyBox);
        center.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(historyBox, Priority.ALWAYS);
        root.setCenter(center);

        endBtn.setDisable(true);
        loadHistory();
    }

    public Node getRoot() { return root; }

    private VBox buildSetupCard() {
        durationSpinner.setEditable(true);
        problemsSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);
        problemsSpinner.setPrefWidth(90);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.addRow(0, new Label("Duration (minutes):"), durationSpinner);
        grid.addRow(1, new Label("Number of problems:"), problemsSpinner);

        startBtn.getStyleClass().add("primary-button");
        startBtn.setOnAction(e -> startContest());

        Label heading = new Label("Set Up a Mock Contest");
        heading.getStyleClass().add("chart-title");
        VBox box = new VBox(14, heading, grid, startBtn);
        box.getStyleClass().add("chart-card");
        box.setPrefWidth(340);
        return box;
    }

    private VBox buildLiveCard() {
        timerLabel.getStyleClass().add("contest-timer");
        statusLabel.getStyleClass().add("view-subtitle");

        solvedSpinner.setEditable(true);
        solvedSpinner.setPrefWidth(90);
        HBox solvedRow = new HBox(10, new Label("Problems solved so far:"), solvedSpinner);
        solvedRow.setAlignment(Pos.CENTER_LEFT);

        endBtn.getStyleClass().add("small-button-danger");
        endBtn.setOnAction(e -> endContest(false));

        Label heading = new Label("Live Contest");
        heading.getStyleClass().add("chart-title");
        VBox box = new VBox(14, heading, timerLabel, statusLabel, solvedRow, endBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("chart-card");
        box.setPrefWidth(340);
        return box;
    }

    @SuppressWarnings("unchecked")
    private void buildHistoryTable() {
        historyTable.setItems(history);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ContestSession, String> nameCol = new TableColumn<>("Contest");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<ContestSession, Number> durCol = new TableColumn<>("Duration (min)");
        durCol.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        TableColumn<ContestSession, Number> solvedCol = new TableColumn<>("Solved");
        solvedCol.setCellValueFactory(new PropertyValueFactory<>("problemsSolved"));
        TableColumn<ContestSession, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        TableColumn<ContestSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        historyTable.getColumns().addAll(nameCol, durCol, solvedCol, scoreCol, statusCol);
    }

    private void startContest() {
        currentDuration = durationSpinner.getValue();
        currentProblems = problemsSpinner.getValue();
        remainingSeconds = currentDuration * 60;
        currentStartedAt = DateUtil.now();
        solvedSpinner.getValueFactory().setValue(0);

        statusLabel.setText("Contest running — " + currentProblems + " problems, " + currentDuration + " minutes");
        startBtn.setDisable(true);
        endBtn.setDisable(false);

        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0) {
                endContest(true);
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        int m = Math.max(0, remainingSeconds) / 60;
        int s = Math.max(0, remainingSeconds) % 60;
        timerLabel.setText(String.format("%02d:%02d", m, s));
    }

    private void endContest(boolean timeUp) {
        if (countdown != null) countdown.stop();
        int solved = solvedSpinner.getValue();
        int score = solved * 100 + (timeUp ? 0 : Math.max(0, remainingSeconds / 6));

        saveSession("Mock Contest " + DateUtil.today(), currentDuration, currentProblems, solved, score,
                currentStartedAt, DateUtil.now(), timeUp ? "Time's Up" : "Ended Early");

        statusLabel.setText(timeUp ? "Time's up! Final score: " + score : "Contest ended. Final score: " + score);
        startBtn.setDisable(false);
        endBtn.setDisable(true);
        loadHistory();
    }

    private void saveSession(String name, int duration, int planned, int solved, int score,
                              String startedAt, String endedAt, String status) {
        String sql = "INSERT INTO contest_sessions (name, duration_minutes, problems_planned, problems_solved, score, started_at, ended_at, status) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, duration);
            ps.setInt(3, planned);
            ps.setInt(4, solved);
            ps.setInt(5, score);
            ps.setString(6, startedAt);
            ps.setString(7, endedAt);
            ps.setString(8, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }

    private void loadHistory() {
        history.clear();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM contest_sessions ORDER BY id DESC")) {
            while (rs.next()) {
                history.add(new ContestSession(
                        rs.getInt("id"), rs.getString("name"), rs.getInt("duration_minutes"),
                        rs.getInt("problems_planned"), rs.getInt("problems_solved"), rs.getInt("score"),
                        rs.getString("started_at"), rs.getString("ended_at"), rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }
}
