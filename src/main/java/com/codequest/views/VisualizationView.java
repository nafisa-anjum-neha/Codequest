package com.codequest.views;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.*;

/**
 * Feature 5 — Algorithm Visualization Module.
 * Bridges theory and code with step-by-step animated visualizations of:
 * Bubble Sort, Binary Search, Linked List Traversal, Merge Sort (Divide & Conquer),
 * BFS, DFS, Dijkstra's Algorithm, and Dynamic Programming (Fibonacci memoization).
 * Each algorithm records a list of "frames" ahead of time so a Timeline can
 * play, pause, and step through exactly what the algorithm does at every stage.
 */
public class VisualizationView {

    private final BorderPane root = new BorderPane();
    private final Canvas canvas = new Canvas(760, 340);
    private final Label stepLabel = new Label("Press \"Run\" to start the visualization.");
    private final ComboBox<String> algoBox = new ComboBox<>();
    private final Slider speedSlider = new Slider(1, 10, 5);
    private final Button runBtn = new Button("▶ Run");
    private final Button resetBtn = new Button("⟲ Reset");

    private Timeline timeline;
    private List<Frame> frames = new ArrayList<>();
    private int frameIndex = 0;

    // ---------- Graph model shared by BFS / DFS / Dijkstra ----------
    private record GNode(String name, double x, double y) {}
    private record GEdge(String from, String to, int weight) {}
    private final List<GNode> nodes = List.of(
            new GNode("A", 120, 70), new GNode("B", 330, 60), new GNode("C", 560, 70),
            new GNode("D", 120, 230), new GNode("E", 330, 210), new GNode("F", 560, 230),
            new GNode("G", 680, 140)
    );
    private final List<GEdge> edges = List.of(
            new GEdge("A", "B", 4), new GEdge("A", "D", 2), new GEdge("B", "C", 5),
            new GEdge("B", "E", 1), new GEdge("C", "F", 3), new GEdge("D", "E", 6),
            new GEdge("E", "F", 2), new GEdge("E", "G", 7), new GEdge("F", "G", 1)
    );

    public VisualizationView() {
        root.getStyleClass().add("view-root");

        Label title = new Label("Algorithm Visualization Module");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Watch key algorithms execute step by step to bridge theory and code");
        subtitle.getStyleClass().add("view-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(24, 24, 8, 24));
        root.setTop(header);

        algoBox.getItems().addAll(
                "Bubble Sort", "Binary Search", "Linked List Traversal",
                "Merge Sort (Divide & Conquer)", "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)", "Dijkstra's Algorithm",
                "Dynamic Programming (Fibonacci)"
        );
        algoBox.setValue("Bubble Sort");
        algoBox.setOnAction(e -> resetForCurrentAlgorithm());

        speedSlider.setPrefWidth(140);
        speedSlider.setShowTickMarks(true);

        runBtn.getStyleClass().add("primary-button");
        runBtn.setOnAction(e -> playPause());
        resetBtn.getStyleClass().add("small-button");
        resetBtn.setOnAction(e -> resetForCurrentAlgorithm());

        HBox controls = new HBox(14,
                new Label("Algorithm:"), algoBox,
                new Label("Speed:"), speedSlider,
                runBtn, resetBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 24, 14, 24));

        StackPane canvasWrap = new StackPane(canvas);
        canvasWrap.getStyleClass().add("canvas-card");
        canvasWrap.setPadding(new Insets(16));

        stepLabel.getStyleClass().add("viz-step-label");
        stepLabel.setWrapText(true);
        stepLabel.setMaxWidth(740);
        VBox canvasBox = new VBox(12, canvasWrap, stepLabel);
        canvasBox.setPadding(new Insets(0, 24, 24, 24));
        canvasBox.setAlignment(Pos.CENTER);

        VBox center = new VBox(controls, canvasBox);
        root.setCenter(center);

        resetForCurrentAlgorithm();
    }

    public Node getRoot() { return root; }

    private void playPause() {
        if (timeline == null) return;
        if (timeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            timeline.pause();
            runBtn.setText("▶ Resume");
        } else {
            timeline.play();
            runBtn.setText("⏸ Pause");
        }
    }

    private void resetForCurrentAlgorithm() {
        if (timeline != null) timeline.stop();
        runBtn.setText("▶ Run");
        frames.clear();
        frameIndex = 0;

        switch (algoBox.getValue()) {
            case "Bubble Sort" -> setupBubbleSort();
            case "Binary Search" -> setupBinarySearch();
            case "Linked List Traversal" -> setupLinkedList();
            case "Merge Sort (Divide & Conquer)" -> setupMergeSort();
            case "Breadth-First Search (BFS)" -> setupBFS();
            case "Depth-First Search (DFS)" -> setupDFS();
            case "Dijkstra's Algorithm" -> setupDijkstra();
            case "Dynamic Programming (Fibonacci)" -> setupDP();
        }

        buildTimeline();
        renderFrame(0);
    }

    private void buildTimeline() {
        double rate = speedSlider.getValue();
        double millisPerFrame = 950 - (rate * 75);
        timeline = new Timeline(new KeyFrame(Duration.millis(Math.max(150, millisPerFrame)), e -> {
            frameIndex++;
            if (frameIndex >= frames.size()) {
                timeline.stop();
                runBtn.setText("▶ Run Again");
                frameIndex = frames.size() - 1;
                return;
            }
            renderFrame(frameIndex);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    // =========================================================
    // Frame hierarchy
    // =========================================================
    private static abstract class Frame {
        final String description;
        Frame(String description) { this.description = description; }
    }

    private static class ArrayFrame extends Frame {
        int[] array; int a, b;
        ArrayFrame(int[] array, int a, int b, String description) {
            super(description); this.array = array; this.a = a; this.b = b;
        }
    }

    private static class SearchFrame extends ArrayFrame {
        int lo, hi, mid;
        SearchFrame(int[] array, int lo, int hi, int mid, String description) {
            super(array, mid, -1, description);
            this.lo = lo; this.hi = hi; this.mid = mid;
        }
    }

    private static class RangeFrame extends ArrayFrame {
        int lo, hi;
        RangeFrame(int[] array, int lo, int hi, int a, int b, String description) {
            super(array, a, b, description);
            this.lo = lo; this.hi = hi;
        }
    }

    private static class GraphFrame extends Frame {
        Set<String> visited; String current; List<String> frontier; Map<String, Integer> distances;
        GraphFrame(Set<String> visited, String current, List<String> frontier, Map<String, Integer> distances, String description) {
            super(description);
            this.visited = visited; this.current = current; this.frontier = frontier; this.distances = distances;
        }
    }

    private static class DPFrame extends Frame {
        int[] memo; int current; int dep1, dep2;
        DPFrame(int[] memo, int current, int dep1, int dep2, String description) {
            super(description);
            this.memo = memo; this.current = current; this.dep1 = dep1; this.dep2 = dep2;
        }
    }

    // =========================================================
    // Bubble Sort
    // =========================================================
    private void setupBubbleSort() {
        Random rand = new Random();
        int[] a = new int[10];
        for (int i = 0; i < a.length; i++) a[i] = 10 + rand.nextInt(90);

        frames.add(new ArrayFrame(a.clone(), -1, -1, "Starting array. Bubble Sort repeatedly compares adjacent elements."));
        for (int i = 0; i < a.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - i - 1; j++) {
                frames.add(new ArrayFrame(a.clone(), j, j + 1, "Comparing index " + j + " and " + (j + 1)));
                if (a[j] > a[j + 1]) {
                    int tmp = a[j]; a[j] = a[j + 1]; a[j + 1] = tmp;
                    swapped = true;
                    frames.add(new ArrayFrame(a.clone(), j, j + 1, "Swapped — elements were out of order."));
                }
            }
            if (!swapped) break;
        }
        frames.add(new ArrayFrame(a.clone(), -1, -1, "Array is fully sorted."));
    }

    // =========================================================
    // Binary Search
    // =========================================================
    private int[] sortedArray;
    private int searchTarget;

    private void setupBinarySearch() {
        sortedArray = new int[]{4, 9, 13, 21, 28, 35, 42, 50, 63, 77, 85, 91};
        Random rand = new Random();
        searchTarget = sortedArray[rand.nextInt(sortedArray.length)];

        int lo = 0, hi = sortedArray.length - 1;
        frames.add(new ArrayFrame(sortedArray.clone(), -1, -1, "Searching for target = " + searchTarget + " in a sorted array."));
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            frames.add(new SearchFrame(sortedArray.clone(), lo, hi, mid,
                    "lo=" + lo + ", hi=" + hi + ", mid=" + mid + " -> array[mid]=" + sortedArray[mid]));
            if (sortedArray[mid] == searchTarget) {
                frames.add(new SearchFrame(sortedArray.clone(), lo, hi, mid, "Found target " + searchTarget + " at index " + mid + "!"));
                break;
            } else if (sortedArray[mid] < searchTarget) {
                frames.add(new SearchFrame(sortedArray.clone(), lo, hi, mid, "array[mid] < target -> search right half"));
                lo = mid + 1;
            } else {
                frames.add(new SearchFrame(sortedArray.clone(), lo, hi, mid, "array[mid] > target -> search left half"));
                hi = mid - 1;
            }
        }
    }

    // =========================================================
    // Linked List Traversal
    // =========================================================
    private void setupLinkedList() {
        int[] a = {7, 15, 23, 8, 42, 16};
        frames.add(new ArrayFrame(a.clone(), -1, -1, "A linked list: each node points to the next. Traversal visits one node at a time."));
        for (int i = 0; i < a.length; i++) {
            frames.add(new ArrayFrame(a.clone(), i, -1, "Visiting node at position " + i + " (value = " + a[i] + ")"));
        }
        frames.add(new ArrayFrame(a.clone(), -1, -1, "Reached the end of the list (null)."));
    }

    // =========================================================
    // Merge Sort (Divide & Conquer)
    // =========================================================
    private void setupMergeSort() {
        Random rand = new Random();
        int[] a = new int[10];
        for (int i = 0; i < a.length; i++) a[i] = 10 + rand.nextInt(90);
        frames.add(new RangeFrame(a.clone(), 0, a.length - 1, -1, -1, "Divide & Conquer: split the array in half recursively, then merge sorted halves."));
        mergeSort(a, 0, a.length - 1);
        frames.add(new RangeFrame(a.clone(), 0, a.length - 1, -1, -1, "Array is fully sorted after all merges."));
    }

    private void mergeSort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2;
        frames.add(new RangeFrame(a.clone(), lo, hi, -1, -1, "Splitting range [" + lo + ".." + hi + "] at mid=" + mid));
        mergeSort(a, lo, mid);
        mergeSort(a, mid + 1, hi);
        merge(a, lo, mid, hi);
    }

    private void merge(int[] a, int lo, int mid, int hi) {
        int[] left = Arrays.copyOfRange(a, lo, mid + 1);
        int[] right = Arrays.copyOfRange(a, mid + 1, hi + 1);
        int i = 0, j = 0, k = lo;
        while (i < left.length && j < right.length) {
            frames.add(new RangeFrame(a.clone(), lo, hi, k, -1, "Merging [" + lo + ".." + hi + "]: comparing " + left[i] + " and " + right[j]));
            if (left[i] <= right[j]) a[k] = left[i++];
            else a[k] = right[j++];
            frames.add(new RangeFrame(a.clone(), lo, hi, k, -1, "Placed " + a[k] + " at index " + k));
            k++;
        }
        while (i < left.length) { a[k] = left[i++]; frames.add(new RangeFrame(a.clone(), lo, hi, k, -1, "Copying remaining left element " + a[k])); k++; }
        while (j < right.length) { a[k] = right[j++]; frames.add(new RangeFrame(a.clone(), lo, hi, k, -1, "Copying remaining right element " + a[k])); k++; }
        frames.add(new RangeFrame(a.clone(), lo, hi, -1, -1, "Range [" + lo + ".." + hi + "] merged and sorted."));
    }

    // =========================================================
    // BFS
    // =========================================================
    private void setupBFS() {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add("A");
        visited.add("A");
        frames.add(new GraphFrame(new LinkedHashSet<>(visited), null, new ArrayList<>(queue), null,
                "BFS starts at A, using a queue (FIFO) to explore level by level."));
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, new ArrayList<>(queue), null, "Visiting node " + cur));
            for (String neighbor : neighborsOf(cur)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, new ArrayList<>(queue), null,
                            "Discovered " + neighbor + " from " + cur + " -> added to queue"));
                }
            }
        }
        frames.add(new GraphFrame(new LinkedHashSet<>(visited), null, List.of(), null, "BFS complete — all reachable nodes visited."));
    }

    // =========================================================
    // DFS
    // =========================================================
    private void setupDFS() {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push("A");
        frames.add(new GraphFrame(new LinkedHashSet<>(), null, new ArrayList<>(stack), null,
                "DFS starts at A, using a stack (LIFO) to explore as deep as possible first."));
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (visited.contains(cur)) continue;
            visited.add(cur);
            frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, new ArrayList<>(stack), null, "Visiting node " + cur));
            List<String> neighbors = new ArrayList<>(neighborsOf(cur));
            Collections.reverse(neighbors);
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                    frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, new ArrayList<>(stack), null,
                            "Pushed " + neighbor + " onto the stack from " + cur));
                }
            }
        }
        frames.add(new GraphFrame(new LinkedHashSet<>(visited), null, List.of(), null, "DFS complete — all reachable nodes visited."));
    }

    private List<String> neighborsOf(String node) {
        List<String> result = new ArrayList<>();
        for (GEdge e : edges) {
            if (e.from().equals(node)) result.add(e.to());
            else if (e.to().equals(node)) result.add(e.from());
        }
        return result;
    }

    // =========================================================
    // Dijkstra's Algorithm
    // =========================================================
    private void setupDijkstra() {
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (GNode n : nodes) dist.put(n.name(), Integer.MAX_VALUE);
        dist.put("A", 0);
        Set<String> visited = new LinkedHashSet<>();

        frames.add(new GraphFrame(new LinkedHashSet<>(), null, List.of(), new LinkedHashMap<>(dist),
                "Dijkstra finds shortest paths from A by always expanding the closest unvisited node."));

        while (visited.size() < nodes.size()) {
            String cur = null;
            int best = Integer.MAX_VALUE;
            for (var entry : dist.entrySet()) {
                if (!visited.contains(entry.getKey()) && entry.getValue() < best) {
                    best = entry.getValue();
                    cur = entry.getKey();
                }
            }
            if (cur == null) break;
            visited.add(cur);
            frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, List.of(), new LinkedHashMap<>(dist),
                    "Expanding closest unvisited node: " + cur + " (distance " + dist.get(cur) + ")"));

            for (GEdge e : edges) {
                String neighbor = null; int weight = e.weight();
                if (e.from().equals(cur)) neighbor = e.to();
                else if (e.to().equals(cur)) neighbor = e.from();
                if (neighbor == null || visited.contains(neighbor)) continue;

                int newDist = dist.get(cur) + weight;
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    frames.add(new GraphFrame(new LinkedHashSet<>(visited), cur, List.of(), new LinkedHashMap<>(dist),
                            "Relaxed edge " + cur + "->" + neighbor + ": new shortest distance = " + newDist));
                }
            }
        }
        frames.add(new GraphFrame(new LinkedHashSet<>(visited), null, List.of(), new LinkedHashMap<>(dist),
                "Dijkstra complete — shortest distances from A finalized for every node."));
    }

    // =========================================================
    // Dynamic Programming — Fibonacci with memoization
    // =========================================================
    private void setupDP() {
        int n = 10;
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        frames.add(new DPFrame(memo.clone(), -1, -1, -1,
                "Dynamic Programming avoids recomputation by storing results in a memo table (fib(0)..fib(" + n + "))."));
        memo[0] = 0;
        frames.add(new DPFrame(memo.clone(), 0, -1, -1, "Base case: fib(0) = 0"));
        memo[1] = 1;
        frames.add(new DPFrame(memo.clone(), 1, -1, -1, "Base case: fib(1) = 1"));
        for (int i = 2; i <= n; i++) {
            frames.add(new DPFrame(memo.clone(), i, i - 1, i - 2, "Computing fib(" + i + ") = fib(" + (i - 1) + ") + fib(" + (i - 2) + ")"));
            memo[i] = memo[i - 1] + memo[i - 2];
            frames.add(new DPFrame(memo.clone(), i, i - 1, i - 2, "fib(" + i + ") = " + memo[i - 1] + " + " + memo[i - 2] + " = " + memo[i]));
        }
        frames.add(new DPFrame(memo.clone(), -1, -1, -1, "All values computed in O(n) time instead of exponential recursive calls."));
    }

    // =========================================================
    // Rendering
    // =========================================================
    private void renderFrame(int idx) {
        if (frames.isEmpty()) return;
        Frame f = frames.get(Math.min(idx, frames.size() - 1));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1e1f2e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (f instanceof GraphFrame gf) renderGraphFrame(gc, gf);
        else if (f instanceof DPFrame df) renderDPFrame(gc, df);
        else if (f instanceof ArrayFrame af) renderArrayFrame(gc, af);

        stepLabel.setText(f.description);
    }

    private void renderArrayFrame(GraphicsContext gc, ArrayFrame f) {
        int n = f.array.length;
        double gap = 12;
        double barWidth = (canvas.getWidth() - gap * (n + 1)) / n;
        double maxVal = 100;
        double chartHeight = 220;
        double baseY = 280;
        String algo = algoBox.getValue();

        for (int i = 0; i < n; i++) {
            double x = gap + i * (barWidth + gap);
            double h = (f.array[i] / maxVal) * chartHeight;
            Color color = Color.web("#5865f2");

            if (algo.equals("Bubble Sort")) {
                if (i == f.a || i == f.b) color = Color.web("#ffb454");
            } else if (algo.equals("Binary Search") && f instanceof SearchFrame sf) {
                if (i == sf.mid) color = Color.web("#3ddc97");
                else if (i < sf.lo || i > sf.hi) color = Color.web("#3a3b4a");
            } else if (algo.equals("Linked List Traversal")) {
                if (i == f.a) color = Color.web("#3ddc97");
            } else if (algo.equals("Merge Sort (Divide & Conquer)") && f instanceof RangeFrame rf) {
                if (i < rf.lo || i > rf.hi) color = Color.web("#2a2b3d");
                else if (i == rf.a) color = Color.web("#ffb454");
                else color = Color.web("#5865f2");
            }

            gc.setFill(color);
            gc.fillRoundRect(x, baseY - h, barWidth, h, 8, 8);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 13));
            gc.fillText(String.valueOf(f.array[i]), x + barWidth / 2 - 8, baseY - h - 8);
            gc.setFill(Color.web("#9aa0b4"));
            gc.fillText(String.valueOf(i), x + barWidth / 2 - 4, baseY + 18);
        }
    }

    private void renderGraphFrame(GraphicsContext gc, GraphFrame f) {
        boolean weighted = algoBox.getValue().equals("Dijkstra's Algorithm");
        Map<String, GNode> byName = new HashMap<>();
        for (GNode n : nodes) byName.put(n.name(), n);

        // edges
        gc.setStroke(Color.web("#3a3b4a"));
        gc.setLineWidth(2);
        gc.setFont(Font.font("System", 12));
        for (GEdge e : edges) {
            GNode a = byName.get(e.from()), b = byName.get(e.to());
            gc.strokeLine(a.x(), a.y(), b.x(), b.y());
            if (weighted) {
                gc.setFill(Color.web("#ffb454"));
                gc.fillText(String.valueOf(e.weight()), (a.x() + b.x()) / 2, (a.y() + b.y()) / 2 - 6);
            }
        }

        // nodes
        for (GNode n : nodes) {
            Color fill = Color.web("#2a2b3d");
            if (f.visited != null && f.visited.contains(n.name())) fill = Color.web("#3ddc97");
            if (n.name().equals(f.current)) fill = Color.web("#ffb454");

            gc.setFill(fill);
            gc.fillOval(n.x() - 20, n.y() - 20, 40, 40);
            gc.setStroke(Color.web("#5865f2"));
            gc.setLineWidth(2);
            gc.strokeOval(n.x() - 20, n.y() - 20, 40, 40);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(n.name(), n.x(), n.y() + 5);

            if (weighted && f.distances != null) {
                Integer d = f.distances.get(n.name());
                String label = (d == null || d == Integer.MAX_VALUE) ? "∞" : String.valueOf(d);
                gc.setFill(Color.web("#c3c6de"));
                gc.setFont(Font.font("System", 11));
                gc.fillText("dist=" + label, n.x(), n.y() + 34);
            }
        }
        gc.setTextAlign(TextAlignment.LEFT);

        if (f.frontier != null && !f.frontier.isEmpty()) {
            String label = algoBox.getValue().startsWith("Breadth") ? "Queue: " : "Stack: ";
            gc.setFill(Color.web("#c3c6de"));
            gc.setFont(Font.font("System", 13));
            gc.fillText(label + f.frontier, 20, 320);
        }
    }

    private void renderDPFrame(GraphicsContext gc, DPFrame f) {
        int n = f.memo.length;
        double gap = 10;
        double cellWidth = (canvas.getWidth() - gap * (n + 1)) / n;
        double cellHeight = 90;
        double y = 90;

        for (int i = 0; i < n; i++) {
            double x = gap + i * (cellWidth + gap);
            boolean computed = f.memo[i] != -1;
            Color color = Color.web("#2a2b3d");
            if (computed) color = Color.web("#5865f2");
            if (i == f.dep1 || i == f.dep2) color = Color.web("#3ddc97");
            if (i == f.current) color = Color.web("#ffb454");

            gc.setFill(color);
            gc.fillRoundRect(x, y, cellWidth, cellHeight, 10, 10);
            gc.setStroke(Color.web("#262740"));
            gc.strokeRoundRect(x, y, cellWidth, cellHeight, 10, 10);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 16));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(computed ? String.valueOf(f.memo[i]) : "?", x + cellWidth / 2, y + cellHeight / 2 + 5);

            gc.setFill(Color.web("#c3c6de"));
            gc.setFont(Font.font("System", 11));
            gc.fillText("fib(" + i + ")", x + cellWidth / 2, y + cellHeight + 18);
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }
}
