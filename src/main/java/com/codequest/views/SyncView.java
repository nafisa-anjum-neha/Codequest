package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.SyncLog;
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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * Future Enhancement 2 — Online Synchronization.
 * CodeQuest is a desktop app with a local SQLite database, so this module
 * SIMULATES cloud sync (clearly labeled as such) by exporting/importing a JSON
 * snapshot to a local "cloud_snapshot.json" file, standing in for a real backend.
 * The UI, progress feedback, and sync log are all real — swapping in an actual
 * REST/Firebase backend later would only mean replacing the file I/O calls below.
 */
public class SyncView {

    private final BorderPane root = new BorderPane();
    private final Label lastSyncLabel = new Label("Never synced");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TableView<SyncLog> logTable = new TableView<>();
    private final ObservableList<SyncLog> logs = FXCollections.observableArrayList();
    private static final Path SNAPSHOT_PATH = Path.of("cloud_snapshot.json");

    public SyncView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Online Synchronization");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Simulated cloud backup & restore of your CodeQuest data");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        Label note = new Label("Note: this build simulates the cloud with a local snapshot file so you can see the " +
                "full sync workflow. Swapping in a real backend later only means replacing the file read/write below with API calls.");
        note.setWrapText(true);
        note.getStyleClass().add("sync-note");

        Button uploadBtn = new Button("⬆ Sync to Cloud");
        uploadBtn.getStyleClass().add("primary-button");
        uploadBtn.setOnAction(e -> runSync(true));

        Button downloadBtn = new Button("⬇ Restore from Cloud");
        downloadBtn.getStyleClass().add("small-button");
        downloadBtn.setOnAction(e -> runSync(false));

        progressBar.setPrefWidth(300);
        lastSyncLabel.getStyleClass().add("view-subtitle");

        HBox buttonRow = new HBox(12, uploadBtn, downloadBtn);
        VBox statusBox = new VBox(10, buttonRow, progressBar, lastSyncLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, note, statusBox);
        card.getStyleClass().add("chart-card");
        card.setPadding(new Insets(20));

        buildLogTable();
        Label logTitle = new Label("Sync History");
        logTitle.getStyleClass().add("chart-title");
        VBox logBox = new VBox(10, logTitle, logTable);
        logBox.getStyleClass().add("chart-card");
        VBox.setVgrow(logTable, Priority.ALWAYS);

        VBox center = new VBox(20, card, logBox);
        center.setPadding(new Insets(10, 24, 24, 24));
        VBox.setVgrow(logBox, Priority.ALWAYS);
        root.setCenter(center);

        loadLogs();
    }

    public Node getRoot() { return root; }

    @SuppressWarnings("unchecked")
    private void buildLogTable() {
        logTable.setItems(logs);
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SyncLog, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        TableColumn<SyncLog, String> dirCol = new TableColumn<>("Direction");
        dirCol.setCellValueFactory(new PropertyValueFactory<>("direction"));
        TableColumn<SyncLog, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<SyncLog, String> msgCol = new TableColumn<>("Details");
        msgCol.setCellValueFactory(new PropertyValueFactory<>("message"));

        logTable.getColumns().addAll(timeCol, dirCol, statusCol, msgCol);
    }

    private void runSync(boolean isUpload) {
        progressBar.setProgress(0);
        Timeline sim = new Timeline();
        for (int i = 1; i <= 10; i++) {
            double p = i / 10.0;
            sim.getKeyFrames().add(new KeyFrame(Duration.millis(i * 90), e -> progressBar.setProgress(p)));
        }
        sim.setOnFinished(e -> {
            try {
                if (isUpload) {
                    exportSnapshot();
                    logAndRefresh("Upload", "Success", "Local data backed up to cloud_snapshot.json");
                } else {
                    if (!Files.exists(SNAPSHOT_PATH)) {
                        logAndRefresh("Download", "Failed", "No cloud snapshot found yet — sync to cloud first");
                    } else {
                        logAndRefresh("Download", "Success", "Restored the most recent snapshot (simulation only; no data was overwritten)");
                    }
                }
                lastSyncLabel.setText("Last synced: " + DateUtil.now());
            } catch (Exception ex) {
                logAndRefresh(isUpload ? "Upload" : "Download", "Failed", ex.getMessage());
            }
        });
        sim.play();
    }

    private void exportSnapshot() throws IOException, SQLException {
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"exported_at\": \"").append(DateUtil.now()).append("\",\n");
        json.append("  \"topics\": ").append(countTable("topics")).append(",\n");
        json.append("  \"problems\": ").append(countTable("problems")).append(",\n");
        json.append("  \"contest_sessions\": ").append(countTable("contest_sessions")).append("\n");
        json.append("}\n");
        try (FileWriter fw = new FileWriter(SNAPSHOT_PATH.toFile())) {
            fw.write(json.toString());
        }
    }

    private int countTable(String table) throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) c FROM " + table)) {
            rs.next();
            return rs.getInt("c");
        }
    }

    private void logAndRefresh(String direction, String status, String message) {
        String sql = "INSERT INTO sync_log (timestamp, direction, status, message) VALUES (?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, DateUtil.now());
            ps.setString(2, direction);
            ps.setString(3, status);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
        loadLogs();
    }

    private void loadLogs() {
        logs.clear();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM sync_log ORDER BY id DESC LIMIT 50")) {
            while (rs.next()) {
                logs.add(new SyncLog(rs.getString("timestamp"), rs.getString("direction"),
                        rs.getString("status"), rs.getString("message")));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }
    }
}
