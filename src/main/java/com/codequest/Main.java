package com.codequest;

import com.codequest.db.DatabaseManager;
import com.codequest.views.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CodeQuest — a Competitive Programming learning companion.
 *
 * Features:
 *  1. Learning Roadmap            -> RoadmapView
 *  2. Topic Progress Tracker      -> ProgressTrackerView
 *  3. Problem Management System   -> ProblemManagementView
 *  4. Performance Analytics       -> AnalyticsView
 *  5. Algorithm Visualization     -> VisualizationView
 *  6. Weak Topic Detection        -> WeakTopicView
 *  7. Goal Setting System         -> GoalSettingView
 *  8. Achievement System          -> AchievementView
 *  9. Contest Simulation Module   -> ContestSimulationView
 * 10. Online Synchronization      -> SyncView
 */
public class Main extends Application {

    private final BorderPane shell = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    private RoadmapView roadmapView;
    private ProgressTrackerView progressTrackerView;
    private ProblemManagementView problemManagementView;
    private AnalyticsView analyticsView;
    private VisualizationView visualizationView;
    private WeakTopicView weakTopicView;
    private GoalSettingView goalSettingView;
    private AchievementView achievementView;
    private ContestSimulationView contestSimulationView;
    private SyncView syncView;

    @Override
    public void start(Stage stage) {
        DatabaseManager.initialize();

        roadmapView = new RoadmapView();
        progressTrackerView = new ProgressTrackerView();
        problemManagementView = new ProblemManagementView();
        analyticsView = new AnalyticsView();
        visualizationView = new VisualizationView();
        weakTopicView = new WeakTopicView();
        goalSettingView = new GoalSettingView();
        achievementView = new AchievementView();
        contestSimulationView = new ContestSimulationView();
        syncView = new SyncView();

        // Keep dependent views in sync with each other
        roadmapView.setOnDataChanged(() -> { progressTrackerView.refresh(); analyticsView.refresh(); weakTopicView.refresh(); });
        progressTrackerView.setOnDataChanged(() -> { roadmapView.refresh(); analyticsView.refresh(); weakTopicView.refresh(); });
        problemManagementView.setOnDataChanged(() -> { analyticsView.refresh(); weakTopicView.refresh(); goalSettingView.refresh(); achievementView.refresh(); });

        shell.getStyleClass().add("root-bg");
        shell.setLeft(buildSidebar());
        shell.setTop(buildTopBar());
        shell.setCenter(contentArea);

        showView("Roadmap");

        Scene scene = new Scene(shell, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("CodeQuest — Competitive Programming Companion");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.show();
    }

    private BorderPane buildSidebar() {
        Label brand = new Label("🧭 CodeQuest");
        brand.getStyleClass().add("brand-title");
        Label tagline = new Label("Your CP learning companion");
        tagline.getStyleClass().add("brand-subtitle");
        VBox brandBox = new VBox(2, brand, tagline);
        brandBox.setPadding(new Insets(0, 20, 20, 20));

        VBox navBox = new VBox(brandBox);
        navBox.getChildren().add(navButton("🗺  Roadmap", "Roadmap"));
        navBox.getChildren().add(navButton("📈  Progress Tracker", "Progress"));
        navBox.getChildren().add(navButton("📚  Problem Bank", "Problems"));
        navBox.getChildren().add(navButton("📊  Analytics", "Analytics"));
        navBox.getChildren().add(navButton("🧩  Algorithm Visualizer", "Visualizer"));
        navBox.getChildren().add(navButton("🎯  Weak Topics", "WeakTopics"));
        navBox.getChildren().add(navButton("🚩  Goals", "Goals"));
        navBox.getChildren().add(navButton("🏅  Achievements", "Achievements"));
        navBox.getChildren().add(navButton("⏱  Contest Simulation", "Contest"));
        navBox.getChildren().add(navButton("☁  Online Sync", "Sync"));

        Button exitBtn = new Button("⏻  Exit");
        exitBtn.getStyleClass().add("nav-button");
        exitBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.setStyle("-fx-text-fill: #ff8fa3;");
        exitBtn.setOnAction(e -> Platform.exit());

        VBox footer = new VBox(exitBtn);
        footer.setPadding(new Insets(10, 0, 10, 0));

        BorderPane sidebar = new BorderPane();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setTop(navBox);
        sidebar.setBottom(footer);
        return sidebar;
    }

    private Button navButton(String label, String key) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showView(key));
        navButtons.put(key, btn);
        return btn;
    }

    private HBox buildTopBar() {
        Label title = new Label("Competitive Programming Learning Companion");
        title.getStyleClass().add("top-bar-title");
        HBox topBar = new HBox(title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        return topBar;
    }

    private void showView(String key) {
        navButtons.forEach((k, b) -> b.getStyleClass().remove("nav-button-active"));
        if (navButtons.containsKey(key)) {
            navButtons.get(key).getStyleClass().add("nav-button-active");
        }

        contentArea.getChildren().clear();
        switch (key) {
            case "Roadmap" -> { roadmapView.refresh(); contentArea.getChildren().add(roadmapView.getRoot()); }
            case "Progress" -> { progressTrackerView.refresh(); contentArea.getChildren().add(progressTrackerView.getRoot()); }
            case "Problems" -> { problemManagementView.loadFromDb(); contentArea.getChildren().add(problemManagementView.getRoot()); }
            case "Analytics" -> { analyticsView.refresh(); contentArea.getChildren().add(analyticsView.getRoot()); }
            case "Visualizer" -> contentArea.getChildren().add(visualizationView.getRoot());
            case "WeakTopics" -> { weakTopicView.refresh(); contentArea.getChildren().add(weakTopicView.getRoot()); }
            case "Goals" -> { goalSettingView.refresh(); contentArea.getChildren().add(goalSettingView.getRoot()); }
            case "Achievements" -> { achievementView.refresh(); contentArea.getChildren().add(achievementView.getRoot()); }
            case "Contest" -> contentArea.getChildren().add(contestSimulationView.getRoot());
            case "Sync" -> contentArea.getChildren().add(syncView.getRoot());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
