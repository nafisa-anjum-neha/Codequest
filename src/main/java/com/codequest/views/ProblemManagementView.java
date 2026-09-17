package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.Problem;
import com.codequest.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.*;

/**
 * Feature 3 — Problem Management System.
 * Full CRUD over a personal problem bank: add / edit / delete, filter by platform,
 * difficulty and status, and free-text search across title & topic.
 */
public class ProblemManagementView {

    private final BorderPane root = new BorderPane();
    private final TableView<Problem> table = new TableView<>();
    private final ObservableList<Problem> masterData = FXCollections.observableArrayList();
    private FilteredList<Problem> filtered;
    private Runnable onDataChanged = () -> {};

    private final ComboBox<String> platformFilter = new ComboBox<>();
    private final ComboBox<String> difficultyFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final TextField searchField = new TextField();

    public ProblemManagementView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Problem Management System");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Your centralized problem bank across every platform");
        subtitle.getStyleClass().add("view-subtitle");
        VBox headerText = new VBox(4, title, subtitle);

        Button addBtn = new Button("+ Add Problem");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openEditor(null));

        HBox header = new HBox(headerText, spacer(), addBtn);
        header.setPadding(new Insets(24, 24, 8, 24));
        header.setSpacing(10);
        root.setTop(header);

        VBox center = new VBox(14, buildFilterBar(), buildTable());
        center.setPadding(new Insets(10, 24, 24, 24));
        root.setCenter(center);

        loadFromDb();
    }

    public void setOnDataChanged(Runnable r) { this.onDataChanged = r; }

    public Node getRoot() { return root; }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private HBox buildFilterBar() {
        platformFilter.getItems().addAll("All Platforms", "Codeforces", "LeetCode", "AtCoder", "HackerRank", "Other");
        platformFilter.setValue("All Platforms");
        difficultyFilter.getItems().addAll("All Difficulties", "Easy", "Medium", "Hard");
        difficultyFilter.setValue("All Difficulties");
        statusFilter.getItems().addAll("All Statuses", "To Do", "Attempted", "Solved", "Revisit");
        statusFilter.setValue("All Statuses");
        searchField.setPromptText("Search by title or topic...");
        searchField.setPrefWidth(220);

        for (ComboBox<String> box : new ComboBox[]{platformFilter, difficultyFilter, statusFilter}) {
            box.valueProperty().addListener((o, ov, nv) -> applyFilter());
        }
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());

        HBox bar = new HBox(10, platformFilter, difficultyFilter, statusFilter, searchField);
        bar.getStyleClass().add("filter-bar");
        return bar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Problem> buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Problem, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Problem, String> platformCol = new TableColumn<>("Platform");
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));

        TableColumn<Problem, String> urlCol = new TableColumn<>("Problem URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink();
            {
                link.setStyle("-fx-text-fill: #5865f2; -fx-underline: true; -fx-padding: 0;");
                link.setOnAction(e -> {
                    String url = getItem();
                    if (url != null && !url.isBlank()) {
                        try {
                            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(url.trim()));
                            }
                        } catch (Exception ex) {
                            System.err.println("Could not open browser for URL: " + url);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isBlank()) {
                    setGraphic(null);
                    setText("-");
                } else {
                    String display = url.replace("https://", "").replace("http://", "");
                    link.setText(display.length() > 28 ? display.substring(0, 25) + "..." : display);
                    link.setTooltip(new Tooltip(url));
                    setGraphic(link);
                    setText(null);
                }
            }
        });

        TableColumn<Problem, String> topicCol = new TableColumn<>("Topic");
        topicCol.setCellValueFactory(new PropertyValueFactory<>("topic"));

        TableColumn<Problem, String> difficultyCol = new TableColumn<>("Difficulty");
        difficultyCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));

        TableColumn<Problem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Problem, String> dateCol = new TableColumn<>("Solved On");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateSolved"));

        TableColumn<Problem, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("small-button");
                delBtn.getStyleClass().add("small-button-danger");
                editBtn.setOnAction(e -> openEditor(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> deleteProblem(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(titleCol, platformCol, urlCol, topicCol, difficultyCol, statusCol, dateCol, actionsCol);
        return table;
    }

    private void applyFilter() {
        if (filtered == null) return;
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        filtered.setPredicate(p -> {
            boolean platformOk = platformFilter.getValue().equals("All Platforms") || p.getPlatform().equals(platformFilter.getValue());
            boolean difficultyOk = difficultyFilter.getValue().equals("All Difficulties") || p.getDifficulty().equals(difficultyFilter.getValue());
            boolean statusOk = statusFilter.getValue().equals("All Statuses") || p.getStatus().equals(statusFilter.getValue());
            boolean searchOk = query.isEmpty() || p.getTitle().toLowerCase().contains(query) || p.getTopic().toLowerCase().contains(query);
            return platformOk && difficultyOk && statusOk && searchOk;
        });
    }

    private void openEditor(Problem existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Problem" : "Edit Problem");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        TextField titleField = new TextField(existing == null ? "" : existing.getTitle());
        ComboBox<String> platformBox = new ComboBox<>(FXCollections.observableArrayList("Codeforces", "LeetCode", "AtCoder", "HackerRank", "Other"));
        platformBox.setValue(existing == null ? "Codeforces" : existing.getPlatform());
        TextField topicField = new TextField(existing == null ? "" : existing.getTopic());
        ComboBox<String> difficultyBox = new ComboBox<>(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        difficultyBox.setValue(existing == null ? "Medium" : existing.getDifficulty());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("To Do", "Attempted", "Solved", "Revisit"));
        statusBox.setValue(existing == null ? "To Do" : existing.getStatus());
        TextField urlField = new TextField(existing == null ? "" : existing.getUrl());
        urlField.setPromptText("e.g. https://leetcode.com/problems/two-sum");
        TextArea notesArea = new TextArea(existing == null ? "" : existing.getNotes());
        notesArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Platform:"), platformBox);
        grid.addRow(2, new Label("Topic:"), topicField);
        grid.addRow(3, new Label("Difficulty:"), difficultyBox);
        grid.addRow(4, new Label("Status:"), statusBox);
        grid.addRow(5, new Label("URL:"), urlField);
        grid.addRow(6, new Label("Notes:"), notesArea);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            if (titleField.getText().isBlank()) return;

            boolean isSolved = statusBox.getValue().equals("Solved");
            if (existing == null) {
                insertProblem(titleField.getText(), platformBox.getValue(), topicField.getText(),
                        difficultyBox.getValue(), statusBox.getValue(), urlField.getText(), notesArea.getText(),
                        isSolved ? DateUtil.today() : null);
            } else {
                updateProblem(existing.getId(), titleField.getText(), platformBox.getValue(), topicField.getText(),
                        difficultyBox.getValue(), statusBox.getValue(), urlField.getText(), notesArea.getText(),
                        isSolved ? (existing.getDateSolved().isBlank() ? DateUtil.today() : existing.getDateSolved()) : "");
            }
            loadFromDb();
            onDataChanged.run();
        });
    }

    private void insertProblem(String title, String platform, String topic, String difficulty,
                                String status, String url, String notes, String dateSolved) {
        String sql = "INSERT INTO problems (title, platform, topic, difficulty, status, url, notes, date_added, date_solved) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, platform);
            ps.setString(3, topic);
            ps.setString(4, difficulty);
            ps.setString(5, status);
            ps.setString(6, url);
            ps.setString(7, notes);
            ps.setString(8, DateUtil.today());
            ps.setString(9, dateSolved);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateProblem(int id, String title, String platform, String topic, String difficulty,
                                String status, String url, String notes, String dateSolved) {
        String sql = "UPDATE problems SET title=?, platform=?, topic=?, difficulty=?, status=?, url=?, notes=?, date_solved=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, platform);
            ps.setString(3, topic);
            ps.setString(4, difficulty);
            ps.setString(5, status);
            ps.setString(6, url);
            ps.setString(7, notes);
            ps.setString(8, dateSolved);
            ps.setInt(9, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteProblem(Problem p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + p.getTitle() + "\"?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement("DELETE FROM problems WHERE id = ?")) {
                    ps.setInt(1, p.getId());
                    ps.executeUpdate();
                    loadFromDb();
                    onDataChanged.run();
                } catch (SQLException e) {
                    showError(e);
                }
            }
        });
    }

    public void loadFromDb() {
        masterData.clear();
        String sql = "SELECT * FROM problems ORDER BY id DESC";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                masterData.add(new Problem(
                        rs.getInt("id"), rs.getString("title"), rs.getString("platform"),
                        rs.getString("topic"), rs.getString("difficulty"), rs.getString("status"),
                        rs.getString("url"), rs.getString("notes"), rs.getString("date_added"), rs.getString("date_solved")
                ));
            }
        } catch (SQLException e) {
            showError(e);
        }
        filtered = new FilteredList<>(masterData, p -> true);
        table.setItems(filtered);
        applyFilter();
    }

    private void showError(Exception e) {
        new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
    }
}
