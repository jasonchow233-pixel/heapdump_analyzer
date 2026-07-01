package com.heapdump.analyzer.ui;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.Main;
import cn.wanghw.Severity;
import cn.wanghw.SpiderRegistry;
import cn.wanghw.rule.Rule;
import cn.wanghw.rule.RuleEngine;
import cn.wanghw.rule.RuleResult;
import cn.wanghw.rule.YamlRuleLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeapDumpGUI extends Application {

    private final ObservableList<SpiderResult> allResults = FXCollections.observableArrayList();
    private final FilteredList<SpiderResult> filteredResults = new FilteredList<>(allResults);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ISpider> allSpiders = SpiderRegistry.getInstance().getSpiders();
    private IHeapHolder currentHeapHolder;
    private File currentFile;
    private Label statusLabel;
    private Label fileLabel;
    private ProgressBar progressBar;
    private ComboBox<Severity> severityFilterCombo;
    private ComboBox<String> categoryFilterCombo;
    private TextField searchField;
    private TextArea detailArea;
    private TableView<SpiderResult> resultTable;
    private TabPane tabPane;
    private TextArea ruleResultArea;
    private Label[] sevCardLabels = new Label[5];
    private boolean lightTheme = false;
    private BorderPane rootPane;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HeapDump Analyzer v1.0");
        primaryStage.setWidth(1400);
        primaryStage.setHeight(900);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        rootPane = new BorderPane();
        VBox top = new VBox(createTopBar(), createSeverityCards());
        rootPane.setTop(top);
        rootPane.setCenter(createCenterArea());
        rootPane.setBottom(createStatusBar());

        Scene scene = new Scene(rootPane);
        java.net.URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        applyTheme(rootPane);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Auto-load heap file if passed as command-line argument
        java.util.List<String> params = getParameters().getUnnamed();
        if (!params.isEmpty()) {
            File autoFile = new File(params.get(0));
            if (autoFile.exists() && autoFile.isFile()) {
                currentFile = autoFile;
                fileLabel.setText("File: " + autoFile.getAbsolutePath() + "  (" + formatSize(autoFile.length()) + ")");
                statusLabel.setText("File loaded: " + autoFile.getName() + " (click 'Run Spider Scan' to analyze)");
            }
        }
    }

    /** Severity summary cards (CRITICAL/HIGH/MEDIUM/LOW/INFO counts). */
    private HBox createSeverityCards() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(8, 15, 8, 15));
        box.setStyle("-fx-background-color: #181825;");
        Severity[] sevs = {Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO};
        for (int i = 0; i < sevs.length; i++) {
            Severity s = sevs[i];
            VBox card = new VBox(2);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(180);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #313244; -fx-background-radius: 10; -fx-border-color: "
                    + s.getColor() + "; -fx-border-width: 2 0 0 0;");
            Label num = new Label("0");
            num.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + s.getColor() + ";");
            Label lbl = new Label(s.getLabel().toUpperCase());
            lbl.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
            card.getChildren().addAll(num, lbl);
            sevCardLabels[i] = num;
            box.getChildren().add(card);
        }
        return box;
    }

    private void updateSeverityCards() {
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) counts.put(s, 0);
        for (SpiderResult r : allResults) {
            if ("Found".equals(r.getStatus())) {
                try {
                    counts.merge(Severity.valueOf(r.getSeverity().toUpperCase()), 1, Integer::sum);
                } catch (Exception ignored) {}
            }
        }
        Severity[] sevs = {Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO};
        for (int i = 0; i < sevs.length; i++) {
            if (sevCardLabels[i] != null) sevCardLabels[i].setText(String.valueOf(counts.get(sevs[i])));
        }
    }

    private void applyTheme(BorderPane root) {
        if (lightTheme) {
            root.setStyle("-fx-base: #eff1f5; -fx-background: #eff1f5; -fx-control-inner-background: #ffffff; -fx-text-background-color: #4c4f69; -fx-mark-color: #4c4f69;");
        } else {
            root.setStyle("-fx-base: #1e1e2e; -fx-background: #1e1e2e; -fx-control-inner-background: #313244; -fx-text-background-color: #cdd6f4; -fx-mark-color: #cdd6f4;");
        }
    }

    private VBox createTopBar() {
        // Toolbar
        ToolBar toolbar = new ToolBar();
        Button openBtn = new Button("Open HeapDump");
        openBtn.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        openBtn.setOnAction(e -> openFile());

        Button runBtn = new Button("Run Spider Scan");
        runBtn.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        runBtn.setOnAction(e -> runAnalysis());

        Button runRulesBtn = new Button("Run Rule Engine");
        runRulesBtn.setStyle("-fx-background-color: #f9e2af; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        runRulesBtn.setOnAction(e -> runRules());

        Button exportBtn = new Button("Export Results");
        exportBtn.setOnAction(e -> exportResults());

        Button themeBtn = new Button("🌙 Theme");
        themeBtn.setStyle("-fx-background-color: #45475a; -fx-text-fill: #cdd6f4;");
        themeBtn.setOnAction(e -> {
            lightTheme = !lightTheme;
            applyTheme(rootPane);
            themeBtn.setText(lightTheme ? "☀️ Theme" : "🌙 Theme");
        });

        Separator sep1 = new Separator();
        Label sevLabel = new Label("Min Severity:");
        severityFilterCombo = new ComboBox<>(FXCollections.observableArrayList(Severity.values()));
        severityFilterCombo.setValue(Severity.INFO);
        severityFilterCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Severity s) { return s == null ? "" : s.getLabel(); }
            @Override public Severity fromString(String s) { return Severity.INFO; }
        });
        severityFilterCombo.setOnAction(e -> applyFilter());

        Separator sep2 = new Separator();
        Label catLabel = new Label("Category:");
        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.getItems().add("All");
        categoryFilterCombo.setValue("All");
        categoryFilterCombo.setOnAction(e -> applyFilter());

        Separator sep3 = new Separator();
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Filter results...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

        toolbar.getItems().addAll(openBtn, runBtn, runRulesBtn, exportBtn, themeBtn,
                sep1, sevLabel, severityFilterCombo,
                sep2, catLabel, categoryFilterCombo,
                sep3, searchLabel, searchField);

        // File info
        fileLabel = new Label("No file loaded");
        fileLabel.setStyle("-fx-text-fill: #a6adc8; -fx-padding: 2 10 2 10;");

        VBox top = new VBox(toolbar, fileLabel);
        return top;
    }

    private TabPane createCenterArea() {
        tabPane = new TabPane();

        // Spider Results Tab
        Tab spiderTab = new Tab("Spider Results");
        spiderTab.setClosable(false);

        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // Result table
        resultTable = new TableView<>(filteredResults);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SpiderResult, String> nameCol = new TableColumn<>("Spider Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<SpiderResult, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);

        TableColumn<SpiderResult, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severityCol.setPrefWidth(90);

        TableColumn<SpiderResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(90);

        TableColumn<SpiderResult, String> previewCol = new TableColumn<>("Preview");
        previewCol.setCellValueFactory(new PropertyValueFactory<>("preview"));
        previewCol.setPrefWidth(400);

        resultTable.getColumns().addAll(nameCol, categoryCol, severityCol, statusCol, previewCol);

        // Detail area
        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setStyle("-fx-font-family: 'Menlo', 'Consolas', monospace; -fx-font-size: 12px; -fx-control-inner-background: #1e1e2e; -fx-text-fill: #cdd6f4;");
        detailArea.setPrefHeight(300);

        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) detailArea.setText(val.getData());
        });

        split.getItems().addAll(resultTable, detailArea);
        split.setDividerPositions(0.55);
        spiderTab.setContent(split);

        // Rule Results Tab
        Tab ruleTab = new Tab("Rule Engine Results");
        ruleTab.setClosable(false);
        ruleResultArea = new TextArea();
        ruleResultArea.setEditable(false);
        ruleResultArea.setWrapText(true);
        ruleResultArea.setStyle("-fx-font-family: 'Menlo', 'Consolas', monospace; -fx-font-size: 12px; -fx-control-inner-background: #1e1e2e; -fx-text-fill: #cdd6f4;");
        ruleTab.setContent(ruleResultArea);

        // Spider List Tab
        Tab spiderListTab = new Tab("Spider Plugins (" + allSpiders.size() + ")");
        spiderListTab.setClosable(false);
        VBox listBox = new VBox(5);
        listBox.setPadding(new Insets(10));
        listBox.setStyle("-fx-background-color: #181825;");

        // Group by category
        Map<String, List<ISpider>> grouped = new LinkedHashMap<>(SpiderRegistry.getInstance().getByCategory());
        for (Map.Entry<String, List<ISpider>> entry : grouped.entrySet()) {
            Label catLabel = new Label(entry.getKey().toUpperCase() + " (" + entry.getValue().size() + ")");
            catLabel.setStyle("-fx-text-fill: #cba6f7; -fx-font-weight: bold; -fx-font-size: 13px;");
            listBox.getChildren().add(catLabel);
            for (ISpider spider : entry.getValue()) {
                String color = switch (spider.getSeverity()) {
                    case CRITICAL -> "#f38ba8";
                    case HIGH -> "#fab387";
                    case MEDIUM -> "#f9e2af";
                    case LOW -> "#a6e3a1";
                    case INFO -> "#89dceb";
                };
                Label spiderLabel = new Label("  " + spider.getName() + " - " + spider.getDescription());
                spiderLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
                listBox.getChildren().add(spiderLabel);
            }
        }
        ScrollPane spiderScroll = new ScrollPane(listBox);
        spiderScroll.setFitToWidth(true);
        spiderListTab.setContent(spiderScroll);

        tabPane.getTabs().addAll(spiderTab, ruleTab, spiderListTab);
        return tabPane;
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready - " + allSpiders.size() + " spiders loaded");
        statusLabel.setStyle("-fx-text-fill: #a6adc8;");
        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setPrefWidth(250);
        progressBar.setVisible(false);

        HBox bar = new HBox(15, statusLabel, progressBar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 15, 5, 15));
        bar.setStyle("-fx-background-color: #181825;");
        return bar;
    }

    private void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Heap Dump File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Heap Dump Files", "*.hprof", "*.heapdump"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(null);
        if (file != null) {
            currentFile = file;
            fileLabel.setText("File: " + file.getAbsolutePath() + "  (" + formatSize(file.length()) + ")");
            statusLabel.setText("File loaded: " + file.getName());
        }
    }

    private void runAnalysis() {
        if (currentFile == null) {
            statusLabel.setText("Error: Please open a heap dump file first!");
            return;
        }

        allResults.clear();
        detailArea.clear();
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        statusLabel.setText("Parsing heap dump and running spiders...");
        tabPane.getSelectionModel().select(0);

        Severity minSev = severityFilterCombo.getValue();

        CompletableFuture.runAsync(() -> {
            try {
                Main mainInstance = new Main();
                IHeapHolder holder = mainInstance.createHeapHolder(currentFile);
                currentHeapHolder = holder;

                List<ISpider> toRun = new ArrayList<>();
                for (ISpider spider : allSpiders) {
                    if (spider.getSeverity().ordinal() <= minSev.ordinal()) {
                        toRun.add(spider);
                    }
                }

                int total = toRun.size();
                int foundCount = 0;
                Set<String> categories = new TreeSet<>();
                for (int i = 0; i < total; i++) {
                    ISpider spider = toRun.get(i);
                    String result = null;
                    try {
                        result = spider.sniff(holder);
                    } catch (Exception ex) {
                        result = "Error: " + ex.getMessage();
                    }
                    boolean found = result != null && !result.isEmpty();
                    if (found) foundCount++;
                    categories.add(spider.getCategory());
                    String preview = found ? (result.length() > 200 ? result.substring(0, 200) + "..." : result) : "";
                    SpiderResult sr = new SpiderResult(
                            spider.getName(),
                            spider.getCategory(),
                            spider.getSeverity().getLabel(),
                            found ? "Found" : "Not Found",
                            found ? result : "",
                            preview.replace("\n", " ")
                    );
                    final int progress = i + 1;
                    final int fc = foundCount;
                    Platform.runLater(() -> {
                        allResults.add(sr);
                        progressBar.setProgress((double) progress / total);
                        statusLabel.setText(String.format("Scanning... %d/%d (%d found)", progress, total, fc));
                    });
                }

                // Update category filter
                final int finalFound = foundCount;
                Platform.runLater(() -> {
                    categoryFilterCombo.getItems().clear();
                    categoryFilterCombo.getItems().add("All");
                    categoryFilterCombo.getItems().addAll(categories);
                    categoryFilterCombo.setValue("All");
                    updateSeverityCards();
                    statusLabel.setText(String.format("Scan complete! %d/%d spiders found results", finalFound, total));
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }, executor);
    }

    private void runRules() {
        if (currentFile == null) {
            statusLabel.setText("Error: Please open a heap dump file first!");
            return;
        }
        if (currentHeapHolder == null) {
            // Need to parse first
            try {
                Main mainInstance = new Main();
                currentHeapHolder = mainInstance.createHeapHolder(currentFile);
            } catch (Exception e) {
                statusLabel.setText("Error loading heap: " + e.getMessage());
                return;
            }
        }

        statusLabel.setText("Running rule engine...");
        tabPane.getSelectionModel().select(1);

        CompletableFuture.runAsync(() -> {
            try {
                List<Rule> rules = YamlRuleLoader.loadAll(null);
                RuleEngine engine = new RuleEngine(rules);
                engine.setMinSeverity(severityFilterCombo.getValue());
                engine.setParallelEnabled(true);
                List<RuleResult> results = engine.execute(currentHeapHolder);

                StringWriter sw = new StringWriter();
                sw.write("Rule Engine Results\n");
                sw.write("═".repeat(80) + "\n");
                sw.write(String.format("Total rules: %d | Matched: %d\n\n", rules.size(), results.size()));

                for (RuleResult rr : results) {
                    sw.write(rr.toString());
                    sw.write("\n");
                }

                if (results.isEmpty()) {
                    sw.write("No rules matched.\n");
                }

                final String output = sw.toString();
                Platform.runLater(() -> {
                    ruleResultArea.setText(output);
                    statusLabel.setText(String.format("Rule engine complete: %d/%d rules matched", results.size(), rules.size()));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    ruleResultArea.setText("Error: " + e.getMessage());
                    statusLabel.setText("Rule engine error: " + e.getMessage());
                });
            }
        }, executor);
    }

    private void applyFilter() {
        Severity minSev = severityFilterCombo.getValue();
        String category = categoryFilterCombo.getValue();
        String search = searchField.getText().toLowerCase();

        filteredResults.setPredicate(r -> {
            if (category != null && !"All".equals(category) && !r.getCategory().equalsIgnoreCase(category)) {
                return false;
            }
            if (search != null && !search.isEmpty()) {
                if (!r.getName().toLowerCase().contains(search) &&
                    !r.getData().toLowerCase().contains(search) &&
                    !r.getCategory().toLowerCase().contains(search)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void exportResults() {
        if (allResults.isEmpty()) {
            statusLabel.setText("No results to export!");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Results");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML Report", "*.html"),
                new FileChooser.ExtensionFilter("Text", "*.txt"),
                new FileChooser.ExtensionFilter("JSON", "*.json"),
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );
        File file = fc.showSaveDialog(null);
        if (file != null) {
            try {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".html")) {
                    java.nio.file.Files.writeString(file.toPath(), buildHtmlReport());
                } else if (name.endsWith(".json")) {
                    java.nio.file.Files.writeString(file.toPath(), buildJsonReport());
                } else if (name.endsWith(".csv")) {
                    java.nio.file.Files.writeString(file.toPath(), buildCsvReport());
                } else {
                    java.nio.file.Files.writeString(file.toPath(), buildTextReport());
                }
                statusLabel.setText("Exported to: " + file.getName());
            } catch (Exception e) {
                statusLabel.setText("Export error: " + e.getMessage());
            }
        }
    }

    private java.util.List<cn.wanghw.report.ScanReport.SpiderEntry> buildSpiderEntries() {
        java.util.List<cn.wanghw.report.ScanReport.SpiderEntry> list = new ArrayList<>();
        for (SpiderResult r : allResults) {
            boolean found = "Found".equals(r.getStatus());
            Severity sev;
            try { sev = Severity.valueOf(r.getSeverity().toUpperCase()); }
            catch (Exception e) { sev = Severity.MEDIUM; }
            list.add(new cn.wanghw.report.ScanReport.SpiderEntry(
                    r.getName(), r.getCategory(), sev, "", found, r.getData()));
        }
        return list;
    }

    private String buildHtmlReport() {
        cn.wanghw.report.ScanReport report = new cn.wanghw.report.ScanReport(
                currentFile != null ? currentFile.getName() : "heapdump",
                currentFile != null ? currentFile.length() : 0,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "1.0.0", allSpiders.size(), 0, buildSpiderEntries(), java.util.Collections.emptyList());
        return cn.wanghw.report.HtmlReportRenderer.render(report);
    }

    private String buildTextReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeapDump Analyzer v1.0 - Analysis Report\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("File: ").append(currentFile != null ? currentFile.getAbsolutePath() : "N/A").append("\n");
        sb.append("═".repeat(80)).append("\n\n");
        long foundCount = allResults.stream().filter(r -> "Found".equals(r.getStatus())).count();
        sb.append(String.format("Summary: %d/%d spiders found results\n\n", foundCount, allResults.size()));
        for (SpiderResult r : allResults) {
            sb.append("── ").append(r.getName()).append(" [").append(r.getSeverity()).append("] ──\n");
            sb.append(r.getData().isEmpty() ? "not found!\n" : r.getData() + "\n\n");
        }
        String ruleText = ruleResultArea.getText();
        if (ruleText != null && !ruleText.isEmpty()) {
            sb.append("\n").append("═".repeat(80)).append("\n").append(ruleText);
        }
        return sb.toString();
    }

    private String buildJsonReport() {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (SpiderResult r : allResults) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("name", r.getName());
            o.addProperty("category", r.getCategory());
            o.addProperty("severity", r.getSeverity());
            o.addProperty("found", "Found".equals(r.getStatus()));
            if ("Found".equals(r.getStatus())) o.addProperty("data", r.getData());
            arr.add(o);
        }
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(arr);
    }

    private String buildCsvReport() {
        StringBuilder sb = new StringBuilder("name,category,severity,found,data\n");
        for (SpiderResult r : allResults) {
            boolean found = "Found".equals(r.getStatus());
            String d = found ? "\"" + r.getData().replace("\"", "\"\"").replace("\n", "\\n") + "\"" : "\"\"";
            sb.append(String.format("\"%s\",\"%s\",\"%s\",%s,%s%n", r.getName(), r.getCategory(), r.getSeverity(), found, d));
        }
        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static class SpiderResult {
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty category;
        private final javafx.beans.property.SimpleStringProperty severity;
        private final javafx.beans.property.SimpleStringProperty status;
        private final javafx.beans.property.SimpleStringProperty preview;
        private final String data;

        public SpiderResult(String name, String category, String severity, String status, String data, String preview) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.category = new javafx.beans.property.SimpleStringProperty(category);
            this.severity = new javafx.beans.property.SimpleStringProperty(severity);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
            this.preview = new javafx.beans.property.SimpleStringProperty(preview);
            this.data = data;
        }

        public javafx.beans.property.SimpleStringProperty nameProperty() { return name; }
        public javafx.beans.property.SimpleStringProperty categoryProperty() { return category; }
        public javafx.beans.property.SimpleStringProperty severityProperty() { return severity; }
        public javafx.beans.property.SimpleStringProperty statusProperty() { return status; }
        public javafx.beans.property.SimpleStringProperty previewProperty() { return preview; }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public String getSeverity() { return severity.get(); }
        public String getStatus() { return status.get(); }
        public String getPreview() { return preview.get(); }
        public String getData() { return data; }
    }

    public static void launchGUI(String[] args) {
        launch(args);
    }
}
