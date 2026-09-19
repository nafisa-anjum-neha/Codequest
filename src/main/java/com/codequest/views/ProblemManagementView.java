 package com.codequest.views;

import com.codequest.db.DatabaseManager;
import com.codequest.model.Problem;
import com.codequest.util.DateUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        for (ComboBox<String> box : java.util.List.of(platformFilter, difficultyFilter, statusFilter)) {
            box.valueProperty().addListener((o, ov, nv) -> applyFilter());
        }
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());

        HBox bar = new HBox(10, platformFilter, difficultyFilter, statusFilter, searchField);
        bar.getStyleClass().add("filter-bar");
        return bar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Problem> buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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

        TextField urlField = new TextField(existing == null ? "" : existing.getUrl());
        urlField.setPromptText("Paste a problem URL, e.g. https://codeforces.com/problemset/problem/4/A");

        Label fetchStatus = new Label("");
        fetchStatus.setStyle("-fx-text-fill: #9aa0a6; -fx-font-size: 11px;");

        TextField titleField = new TextField(existing == null ? "" : existing.getTitle());
        titleField.setPromptText("Auto-filled from the URL, or type it yourself");

        ComboBox<String> platformBox = new ComboBox<>(FXCollections.observableArrayList("Codeforces", "LeetCode", "AtCoder", "HackerRank", "Other"));
        platformBox.setValue(existing == null ? "Other" : existing.getPlatform());

        TextField topicField = new TextField(existing == null ? "" : existing.getTopic());
        topicField.setPromptText("Optional");

        ComboBox<String> difficultyBox = new ComboBox<>(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        difficultyBox.setValue(existing == null ? "Medium" : existing.getDifficulty());

        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("To Do", "Attempted", "Solved", "Revisit"));
        statusBox.setValue(existing == null ? "To Do" : existing.getStatus());

        TextArea notesArea = new TextArea(existing == null ? "" : existing.getNotes());
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Optional");

        // Only wire up auto-fill behavior when adding a brand-new problem —
        // editing an existing one should never silently overwrite what's already saved.
        if (existing == null) {
            urlField.textProperty().addListener((obs, oldVal, newVal) -> {
                String detected = detectPlatformFromUrl(newVal);
                if (detected != null) {
                    platformBox.setValue(detected);
                }
            });

            urlField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    String url = urlField.getText() == null ? "" : urlField.getText().trim();
                    if (!url.isEmpty() && titleField.getText().isBlank()) {
                        fetchStatus.setStyle("-fx-text-fill: #9aa0a6; -fx-font-size: 11px;");
                        fetchStatus.setText("Fetching problem title from the URL...");
                        fetchTitleAsync(url, result -> Platform.runLater(() -> {
                            if (result.title != null && titleField.getText().isBlank()) {
                                titleField.setText(result.title);
                                fetchStatus.setText("Title auto-filled — feel free to edit it.");
                            } else if (result.title == null) {
                                fetchStatus.setText("Couldn't auto-fetch a title (" + result.failureReason + ") — type one in manually.");
                            }
                        }));
                    }
                }
            });
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("URL:"), urlField);
        grid.addRow(1, new Label(""), fetchStatus);
        grid.addRow(2, new Label("Title:"), titleField);
        grid.addRow(3, new Label("Platform:"), platformBox);
        grid.addRow(4, new Label("Topic:"), topicField);
        grid.addRow(5, new Label("Difficulty:"), difficultyBox);
        grid.addRow(6, new Label("Status:"), statusBox);
        grid.addRow(7, new Label("Notes:"), notesArea);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Validation: a title or a URL is required (one can stand in for the other).
        // Platform / topic / difficulty / status all keep sensible defaults, so they're optional.
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().isBlank()) {
                String fallback = deriveFallbackTitleFromUrl(urlField.getText());
                if (!fallback.isBlank()) {
                    titleField.setText(fallback);
                } else {
                    fetchStatus.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
                    fetchStatus.setText("Enter a title, or a problem URL to derive one from.");
                    event.consume();
                }
            }
        });

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

    /** Guesses the platform from the URL's domain. Returns null if it doesn't match a known platform. */
    private String detectPlatformFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String lower = url.toLowerCase();
        if (lower.contains("codeforces.com")) return "Codeforces";
        if (lower.contains("leetcode.com")) return "LeetCode";
        if (lower.contains("atcoder.jp")) return "AtCoder";
        if (lower.contains("hackerrank.com")) return "HackerRank";
        return null;
    }

    /**
     * Fetches the page at the given URL on a background thread and extracts a usable problem
     * title, then hands the result (or null on any failure) to the callback. Never touches the
     * UI thread directly — callers should hop back with Platform.runLater.
     */
    private static final HttpClient PROBLEM_FETCH_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Small result wrapper so callers can tell a clean extraction apart from a specific failure reason. */
    private static final class FetchResult {
        final String title;
        final String failureReason;
        FetchResult(String title, String failureReason) { this.title = title; this.failureReason = failureReason; }
    }

    private void fetchTitleAsync(String url, Consumer<FetchResult> onDone) {
        Task<FetchResult> task = new Task<>() {
            @Override
            protected FetchResult call() {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .GET()
                            .build();
                    HttpResponse<String> response = PROBLEM_FETCH_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        return new FetchResult(null, "site returned HTTP " + response.statusCode());
                    }
                    String html = response.body();
                    if (html == null || html.isBlank()) {
                        return new FetchResult(null, "page came back empty");
                    }

                    String extracted = null;
                    if ("Codeforces".equals(detectPlatformFromUrl(url))) {
                        // Codeforces reuses the CSS class "title" in several unrelated places on the
                        // page (sidebar panels, problem-letter tabs, etc). Scope the match to the
                        // actual problem header block so we don't grab one of those instead.
                        Matcher cfTitle = Pattern.compile("<div class=\"header\">\\s*<div class=\"title\">(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
                        if (cfTitle.find()) {
                            String candidate = cleanHtml(cfTitle.group(1));
                            // A real problem title is always "<Letter>[number]. <Name>" — reject
                            // anything shorter, in case the page structure isn't what we expect.
                            if (candidate.length() > 3) {
                                extracted = candidate;
                            }
                        }
                    }
                    if (extracted == null || extracted.isBlank()) {
                        Matcher titleTag = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
                        if (titleTag.find()) extracted = cleanHtml(titleTag.group(1));
                    }
                    if (extracted != null) {
                        extracted = extracted
                                .replaceAll("(?i)\\s*[-|]\\s*LeetCode\\s*$", "")
                                .replaceAll("(?i)\\s*[-|]\\s*Codeforces\\s*$", "")
                                .replaceAll("(?i)\\s*[-|]\\s*AtCoder.*$", "")
                                .replaceAll("(?i)\\s*[-|]\\s*HackerRank\\s*$", "")
                                .trim();
                    }
                    // A generic "Problem - 1868F" style fallback isn't worth showing —
                    // it's no more informative than the URL itself, so treat it as a miss.
                    if (extracted != null && extracted.length() <= 3) {
                        extracted = null;
                    }
                    if (extracted == null || extracted.isBlank()) {
                        return new FetchResult(null, "couldn't find a title on the page");
                    }
                    return new FetchResult(extracted, null);
                } catch (java.net.http.HttpTimeoutException e) {
                    return new FetchResult(null, "request timed out");
                } catch (Exception e) {
                    return new FetchResult(null, e.getClass().getSimpleName());
                }
            }
        };
        task.setOnSucceeded(e -> onDone.accept(task.getValue()));
        task.setOnFailed(e -> onDone.accept(new FetchResult(null, "unexpected error")));
        Thread t = new Thread(task, "problem-title-fetch");
        t.setDaemon(true);
        t.start();
    }

    private String cleanHtml(String raw) {
        return raw.replaceAll("<[^>]+>", "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
                .trim();
    }

    /** Last-resort title when fetching fails: turns the URL into a readable, identifiable title. */
    private String deriveFallbackTitleFromUrl(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String platform = detectPlatformFromUrl(url);
            String path = URI.create(url.trim()).getPath();
            if (path == null || path.isBlank()) return "";
            java.util.List<String> segs = new java.util.ArrayList<>();
            for (String s : path.split("/")) {
                if (!s.isBlank()) segs.add(s);
            }
            if (segs.isEmpty()) return "";

            // Codeforces URLs end in a bare problem letter/index (e.g. "F"), which is
            // meaningless on its own — pair it with the contest number instead.
            if ("Codeforces".equals(platform) && segs.size() >= 2) {
                String index = segs.get(segs.size() - 1);
                String contestId = segs.get(segs.size() - 2);
                if (contestId.matches("\\d+") && index.matches("(?i)[a-z][0-9]?")) {
                    return "Codeforces " + contestId + index.toUpperCase();
                }
            }

            String last = URLDecoder.decode(segs.get(segs.size() - 1), StandardCharsets.UTF_8);
            last = last.replace("-", " ").replace("_", " ").trim();
            if (last.isBlank() || last.length() <= 2) return "";
            StringBuilder sb = new StringBuilder();
            for (String w : last.split("\\s+")) {
                if (w.isEmpty()) continue;
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
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
