 package com.codequest.views;

import com.codequest.db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Feature 4 — Performance Analytics.
 * A personalized dashboard: solved/total stat cards, a difficulty breakdown pie chart,
 * per-category topic progress bar chart, and a 14-day solving trend line chart.
 */
public class AnalyticsView {

    private final BorderPane root = new BorderPane();
    private final HBox statCardsBox = new HBox(16);
    private final FlowPane chartsPane = new FlowPane(20, 20);

    public AnalyticsView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Performance Analytics");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Data-driven insight into your Competitive Programming journey");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        statCardsBox.setPadding(new Insets(0, 24, 10, 24));
        chartsPane.setPadding(new Insets(10, 24, 24, 24));

        VBox center = new VBox(16, statCardsBox, chartsPane);
        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        root.setCenter(scroll);

        refresh();
    }

    public Node getRoot() { return root; }

    public void refresh() {
        buildStatCards();
        chartsPane.getChildren().clear();
        chartsPane.getChildren().add(wrapChart("Problems by Difficulty", buildDifficultyPie()));
        chartsPane.getChildren().add(wrapChart("Topic Completion by Category", buildCategoryBar()));
        chartsPane.getChildren().add(wrapChart("Solving Trend (Last 14 Days)", buildTrendLine()));
    }

    private VBox wrapChart(String label, Node chart) {
        Label l = new Label(label);
        l.getStyleClass().add("chart-title");
        VBox box = new VBox(8, l, chart);
        box.getStyleClass().add("chart-card");
        box.setPrefWidth(430);
        return box;
    }

    private void buildStatCards() {
        statCardsBox.getChildren().clear();
        int totalProblems = 0, solved = 0;
        int totalTopics = 0, completedTopics = 0;

        try (Statement st = DatabaseManager.getConnection().createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) c FROM problems");
            rs.next(); totalProblems = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(*) c FROM problems WHERE status = 'Solved'");
            rs.next(); solved = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(*) c FROM topics");
            rs.next(); totalTopics = rs.getInt("c");

            rs = st.executeQuery("SELECT COUNT(*) c FROM topics WHERE status = 'Completed'");
            rs.next(); completedTopics = rs.getInt("c");
        } catch (SQLException e) {
            showError(e);
        }

        statCardsBox.getChildren().addAll(
                statCard("Problems Solved", solved + " / " + totalProblems),
                statCard("Topics Completed", completedTopics + " / " + totalTopics),
                statCard("Solve Rate", totalProblems == 0 ? "0%" : Math.round(100.0 * solved / totalProblems) + "%")
        );
    }

    private VBox statCard(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        VBox box = new VBox(6, v, l);
        box.getStyleClass().add("stat-card");
        box.setPrefWidth(200);
        return box;
    }

    private PieChart buildDifficultyPie() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Easy", 0);
        counts.put("Medium", 0);
        counts.put("Hard", 0);
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT difficulty, COUNT(*) c FROM problems GROUP BY difficulty")) {
            while (rs.next()) {
                counts.put(rs.getString("difficulty"), rs.getInt("c"));
            }
        } catch (SQLException e) {
            showError(e);
        }
        PieChart chart = new PieChart();
        counts.forEach((k, v) -> {
            if (v > 0) chart.getData().add(new PieChart.Data(k + " (" + v + ")", v));
        });
        if (chart.getData().isEmpty()) {
            chart.getData().add(new PieChart.Data("No problems yet", 1));
        }
        chart.setLegendVisible(true);
        chart.setPrefHeight(260);
        return chart;
    }

    private BarChart<String, Number> buildCategoryBar() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);
        chart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, int[]> totals = new LinkedHashMap<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT category, completion_percent FROM topics")) {
            while (rs.next()) {
                totals.computeIfAbsent(rs.getString("category"), k -> new int[2]);
                totals.get(rs.getString("category"))[0] += rs.getInt("completion_percent");
                totals.get(rs.getString("category"))[1]++;
            }
        } catch (SQLException e) {
            showError(e);
        }
        for (var entry : totals.entrySet()) {
            int avg = entry.getValue()[1] == 0 ? 0 : entry.getValue()[0] / entry.getValue()[1];
            series.getData().add(new XYChart.Data<>(entry.getKey(), avg));
        }
        chart.getData().add(series);
        return chart;
    }

    private LineChart<String, Number> buildTrendLine() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("MM-dd");
        Map<String, Integer> daily = new TreeMap<>();
        LocalDate start = LocalDate.now().minusDays(13);
        for (int i = 0; i < 14; i++) {
            daily.put(start.plusDays(i).format(fmt), 0);
        }

        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT date_solved FROM problems WHERE date_solved IS NOT NULL AND date_solved != ''")) {
            while (rs.next()) {
                String d = rs.getString("date_solved");
                if (daily.containsKey(d)) daily.merge(d, 1, Integer::sum);
            }
        } catch (SQLException e) {
            showError(e);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (var entry : daily.entrySet()) {
            String label = LocalDate.parse(entry.getKey(), fmt).format(shortFmt);
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }
        chart.getData().add(series);
        return chart;
    }

    private void showError(Exception e) {
        System.err.println("Analytics error: " + e.getMessage());
    }
}
