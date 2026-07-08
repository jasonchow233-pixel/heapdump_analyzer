package com.heapdump.analyzer.ui.swing;

import cn.wanghw.IHeapHolder;
import cn.wanghw.ISpider;
import cn.wanghw.Main;
import cn.wanghw.SensitivityCategory;
import cn.wanghw.Severity;
import cn.wanghw.SpiderRegistry;
import cn.wanghw.rule.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.intellijthemes.FlatArcIJTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.concurrent.*;

public class HeapDumpAnalyzerGUI extends JFrame {

    private SpiderResultsPanel spiderResultsPanel;
    private DetailPanel detailPanel;

    private File currentFile;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Future<?> currentTask;

    private final List<ISpider> allSpiders = SpiderRegistry.getInstance().getSpiders();
    private final Preferences prefs = Preferences.userNodeForPackage(HeapDumpAnalyzerGUI.class);
    private static final String RECENT_FILES_KEY = "recentFiles";
    private static final int MAX_RECENT_FILES = 5;

    public HeapDumpAnalyzerGUI() {
        super("HeapDump Analyzer v2.1");

        setupTheme();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        initComponents();
        layoutComponents();
        registerKeyBindings();
        setupDragAndDrop();
    }

    private void setupTheme() {
        try {
            FlatArcIJTheme.setup();
            ThemeConfig.installUIDefaults();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
    }

    private void initComponents() {
        spiderResultsPanel = new SpiderResultsPanel(this);
        detailPanel = new DetailPanel();
    }

    private void layoutComponents() {
        setJMenuBar(createMenuBar());

        setLayout(new MigLayout("insets 0, fill, wrap 1",
            "[grow, fill]", "[grow, fill]"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            spiderResultsPanel, detailPanel);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);
        split.setBorder(null);

        add(split, "growx, growy");

        // Ensure 1:1 divider location after layout
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.5));
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem openItem = new JMenuItem("Open…", new FlatSVGIcon("icons/open.svg", 14, 14));
        openItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        openItem.addActionListener(e -> openFile());
        fileMenu.add(openItem);

        JMenu recentMenu = new JMenu("Recent Files");
        refreshRecentFilesMenu(recentMenu);
        fileMenu.add(recentMenu);

        fileMenu.addSeparator();

        JMenuItem exportItem = new JMenuItem("Export…", new FlatSVGIcon("icons/export.svg", 14, 14));
        exportItem.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        exportItem.addActionListener(e -> exportResults());
        fileMenu.add(exportItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');

        JMenuItem copyItem = new JMenuItem("Copy", new FlatSVGIcon("icons/copy.svg", 14, 14));
        copyItem.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
        copyItem.addActionListener(e -> copyDetailToClipboard());
        editMenu.add(copyItem);

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke("ctrl A"));
        selectAllItem.addActionListener(e -> spiderResultsPanel.selectAllRows());
        editMenu.add(selectAllItem);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke("ctrl PLUS"));
        zoomInItem.addActionListener(e -> zoomIn());
        viewMenu.add(zoomInItem);

        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("ctrl MINUS"));
        zoomOutItem.addActionListener(e -> zoomOut());
        viewMenu.add(zoomOutItem);

        JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
        resetZoomItem.addActionListener(e -> resetZoom());
        viewMenu.add(resetZoomItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);

        return menuBar;
    }

    private void refreshRecentFilesMenu(JMenu recentMenu) {
        recentMenu.removeAll();
        List<String> recent = getRecentFiles();
        if (recent.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No recent files");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
        } else {
            for (String path : recent) {
                File file = new File(path);
                JMenuItem item = new JMenuItem(file.getName());
                item.setToolTipText(path);
                item.addActionListener(e -> loadFileAndAnalyze(file));
                recentMenu.add(item);
            }
        }
    }

    private void registerKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("ctrl O"), "openFile");
        actionMap.put("openFile", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { openFile(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), "export");
        actionMap.put("export", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { exportResults(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl F"), "focusSearch");
        actionMap.put("focusSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { spiderResultsPanel.requestSearchFocus(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl PLUS"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { zoomIn(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl MINUS"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { zoomOut(); }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl EQUALS"), "zoomIn");

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "clearSelection");
        actionMap.put("clearSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { spiderResultsPanel.clearSelection(); }
        });
    }

    private void setupDragAndDrop() {
        TransferHandler handler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                try {
                    Transferable t = support.getTransferable();
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        if (file.isFile()) {
                            loadFileAndAnalyze(file);
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    spiderResultsPanel.setStatus("Drag error: " + ex.getMessage());
                }
                return false;
            }
        };
        setTransferHandler(handler);
        spiderResultsPanel.setTransferHandler(handler);
        detailPanel.setTransferHandler(handler);
    }

    public void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Heap Dump File");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadFileAndAnalyze(chooser.getSelectedFile());
        }
    }

    public void loadFileAndAnalyze(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(this,
                "Please select a valid heap dump file.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentFile = file;
        addRecentFile(file.getAbsolutePath());
        updateMenuRecentFiles();

        setTitle("HeapDump Analyzer - " + file.getName());
        spiderResultsPanel.clearResults();
        spiderResultsPanel.setStatus("Loading " + file.getName() + "…");
        spiderResultsPanel.setProgressVisible(true);
        spiderResultsPanel.setProgressIndeterminate(true);
        detailPanel.clear();

        currentTask = executor.submit(() -> runAnalysis(file));
    }

    private void runAnalysis(File file) {
        long startTime = System.currentTimeMillis();
        try {
            Main mainInstance = new Main();
            IHeapHolder holder = mainInstance.createHeapHolder(file);

            SwingUtilities.invokeLater(() ->
                spiderResultsPanel.setStatus("Running spider scan…"));

            Severity minSev = Severity.INFO;
            List<ISpider> toRun = new ArrayList<>();
            for (ISpider spider : allSpiders) {
                if (spider.getSeverity().ordinal() <= minSev.ordinal()) {
                    toRun.add(spider);
                }
            }

            int total = toRun.size();
            int foundCount = 0;
            List<Object[]> results = new ArrayList<>();

            for (int i = 0; i < total; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Analysis cancelled");
                }

                ISpider spider = toRun.get(i);
                String result;
                try {
                    result = spider.sniff(holder);
                } catch (Exception ex) {
                    result = "Error: " + ex.getMessage();
                }

                boolean found = result != null && !result.isEmpty();
                if (found) foundCount++;

                if (found) {
                    String preview = result.length() > 200 ? result.substring(0, 200) + "…" : result;
                    Object[] row = new Object[]{
                        spider.getName(),
                        spider.getCategory(),
                        spider.getSeverity().getLabel(),
                        preview.replace("\n", " "),
                        result
                    };
                    results.add(row);

                    final int progress = i + 1;
                    final int fc = foundCount;
                    SwingUtilities.invokeLater(() -> {
                        spiderResultsPanel.addResult(row);
                        spiderResultsPanel.setProgressValue(progress * 100 / total);
                        spiderResultsPanel.setStatus(String.format("Scanning… %d/%d (%d found)", progress, total, fc));
                    });
                } else {
                    final int progress = i + 1;
                    final int fc = foundCount;
                    SwingUtilities.invokeLater(() -> {
                        spiderResultsPanel.setProgressValue(progress * 100 / total);
                        spiderResultsPanel.setStatus(String.format("Scanning… %d/%d (%d found)", progress, total, fc));
                    });
                }
            }

            // Run Rule Engine if raw memory scan is enabled, and merge findings.
            if (!Thread.currentThread().isInterrupted() && spiderResultsPanel.isRawMemoryEnabled()) {
                SwingUtilities.invokeLater(() ->
                    spiderResultsPanel.setStatus("Running rule engine (raw memory)…"));
                List<Object[]> ruleRows = runRuleEngine(holder);
                results.addAll(ruleRows);
            }

            final int finalFound = results.size();
            long elapsed = System.currentTimeMillis() - startTime;
            SwingUtilities.invokeLater(() -> {
                spiderResultsPanel.updateResults(results);
                spiderResultsPanel.setStatus(String.format("Done in %s • %d results",
                    formatDuration(elapsed), finalFound));
                spiderResultsPanel.setProgressVisible(false);
            });

        } catch (InterruptedException | CancellationException e) {
            SwingUtilities.invokeLater(() -> {
                spiderResultsPanel.setStatus("Analysis cancelled");
                spiderResultsPanel.setProgressVisible(false);
            });
        } catch (Exception e) {
            // 检查是否是缓存错误
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("HPROF time mismatch")) {
                handleCacheMismatch(file, errorMsg);
            } else {
                SwingUtilities.invokeLater(() -> {
                    spiderResultsPanel.setStatus("Error: " + errorMsg);
                    spiderResultsPanel.setProgressVisible(false);
                });
            }
        }
    }

    private List<Object[]> runRuleEngine(IHeapHolder holder) {
        List<Object[]> rows = new ArrayList<>();
        try {
            List<Rule> rules = YamlRuleLoader.loadAll(null);
            RuleEngine engine = new RuleEngine(rules);
            engine.setMinSeverity(Severity.INFO);
            engine.setUseRawMemory(true);

            List<RuleResult> ruleResults = engine.execute(holder);
            for (RuleResult rr : ruleResults) {
                if (!rr.isFound()) continue;
                Rule rule = rr.getRule();
                SensitivityCategory category = rule.getCategory() != null
                    ? SensitivityCategory.fromString(rule.getCategory())
                    : SensitivityCategory.CONFIG;

                List<EnhancedResult> enhanced = EnhancedResult.fromRuleResult(rr, category);
                for (EnhancedResult er : enhanced) {
                    String preview = er.getDisplayValue();
                    String fullData = buildRuleResultDetail(er, rr);
                    rows.add(new Object[]{
                        rule.getName(),
                        category.getName(),
                        rule.getSeverity().getLabel(),
                        preview,
                        fullData
                    });
                }
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                spiderResultsPanel.setStatus("Rule engine error: " + e.getMessage()));
        }
        return rows;
    }

    private String buildRuleResultDetail(EnhancedResult er, RuleResult rr) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule: ").append(er.getType()).append("\n");
        sb.append("Category: ").append(er.getCategoryDisplay()).append("\n");
        sb.append("Severity: ").append(er.getSeverity().getLabel()).append("\n");
        sb.append("Rule ID: ").append(er.getRuleId()).append("\n");
        sb.append("Pattern: ").append(er.getPattern()).append("\n");
        sb.append("\n").append("═".repeat(80)).append("\n");
        sb.append("Full Value (NO MASKING):\n");
        sb.append("═".repeat(80)).append("\n");
        sb.append(er.getFullValue()).append("\n");
        return sb.toString();
    }

    public void cancelCurrentAnalysis() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    /**
     * 处理缓存不匹配错误，提示用户清除缓存
     */
    private void handleCacheMismatch(File heapFile, String errorMsg) {
        SwingUtilities.invokeLater(() -> {
            spiderResultsPanel.setProgressVisible(false);

            // 提示用户清除缓存
            int option = JOptionPane.showConfirmDialog(this,
                "Heap dump cache mismatch detected.\n\n" +
                "The heap dump file has been modified or replaced, " +
                "causing the cached analysis data to become invalid.\n\n" +
                "Clear cache and reload the file?\n\n" +
                "Error: " + errorMsg,
                "Cache Mismatch",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                clearCacheAndReload(heapFile);
            } else {
                spiderResultsPanel.setStatus("Cache error: Please clear cache manually");
            }
        });
    }

    /**
     * 清除缓存并重新加载文件
     */
    private void clearCacheAndReload(File heapFile) {
        try {
            // 清除 .hwcache 目录
            File cacheDir = new File(heapFile.getParentFile(), heapFile.getName() + ".hwcache");
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                deleteDirectory(cacheDir);
                spiderResultsPanel.setStatus("Cache cleared successfully");
            }

            // 重新加载文件
            loadFileAndAnalyze(heapFile);
        } catch (Exception e) {
            spiderResultsPanel.setStatus("Failed to clear cache: " + e.getMessage());
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(File directory) throws Exception {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.delete(directory.toPath());
    }

    public void onSpiderSelectionChanged(String fullData) {
        int row = spiderResultsPanel.getSelectedRow();
        if (row >= 0) {
            Object[] data = spiderResultsPanel.getRowData(row);
            if (data != null) {
                detailPanel.setDetailFromSpider(
                    String.valueOf(data[0]),
                    String.valueOf(data[1]),
                    String.valueOf(data[2]),
                    String.valueOf(data[4])
                );
                return;
            }
        }
        detailPanel.clear();
    }

    public void copyDetailToClipboard() {
        String text = detailPanel.getDetailText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null);
            spiderResultsPanel.setStatus("Detail copied to clipboard");
        }
    }

    public void exportResults() {
        if (spiderResultsPanel.getResultsData().isEmpty()) {
            spiderResultsPanel.setStatus("No results to export");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Results");
        if (currentFile != null) {
            String base = currentFile.getName();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            chooser.setSelectedFile(new File(base + "_results.json"));
        }
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV File", "csv"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text File", "txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String ext = getFileExtension(file);
                String content = buildExportContent(ext);
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                spiderResultsPanel.setStatus("Exported to: " + file.getName());
            } catch (Exception ex) {
                spiderResultsPanel.setStatus("Export error: " + ex.getMessage());
            }
        }
    }

    private String buildExportContent(String format) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Object[]> rows = spiderResultsPanel.getResultsData();

        if ("json".equalsIgnoreCase(format)) {
            JsonArray array = new JsonArray();
            for (Object[] row : rows) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", String.valueOf(row[0]));
                obj.addProperty("category", String.valueOf(row[1]));
                obj.addProperty("severity", String.valueOf(row[2]));
                obj.addProperty("preview", String.valueOf(row[3]));
                obj.addProperty("fullValue", String.valueOf(row[4]));
                array.add(obj);
            }
            JsonObject root = new JsonObject();
            root.add("spiderResults", array);
            return gson.toJson(root);
        } else if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Name,Category,Severity,Value\n");
            for (Object[] row : rows) {
                sb.append(escapeCsv(String.valueOf(row[0]))).append(",")
                  .append(escapeCsv(String.valueOf(row[1]))).append(",")
                  .append(escapeCsv(String.valueOf(row[2]))).append(",")
                  .append("\"").append(String.valueOf(row[4]).replace("\"", "\"\"")).append("\"\n");
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Spider Results ===\n\n");
            for (Object[] row : rows) {
                sb.append("[").append(row[2]).append("] ")
                  .append(row[0]).append(" (").append(row[1]).append(")\n")
                  .append("  ").append(row[4]).append("\n\n");
            }
            return sb.toString();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "txt";
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        return String.format("%.1fs", ms / 1000.0);
    }

    private void zoomIn() {
        ThemeConfig.setFontScaleFactor(ThemeConfig.getFontScaleFactor() + 0.1f);
        applyFontScale();
        spiderResultsPanel.setStatus("Font size: " + Math.round(ThemeConfig.getFontScaleFactor() * 100) + "%");
    }

    private void zoomOut() {
        ThemeConfig.setFontScaleFactor(ThemeConfig.getFontScaleFactor() - 0.1f);
        applyFontScale();
        spiderResultsPanel.setStatus("Font size: " + Math.round(ThemeConfig.getFontScaleFactor() * 100) + "%");
    }

    private void resetZoom() {
        ThemeConfig.setFontScaleFactor(1.0f);
        applyFontScale();
        spiderResultsPanel.setStatus("Font size: 100%");
    }

    private void applyFontScale() {
        Font baseFont = ThemeConfig.scaleFont(ThemeConfig.FONT_BODY);
        UIManager.put("Label.font", baseFont);
        UIManager.put("Button.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("TextArea.font", ThemeConfig.scaleFont(ThemeConfig.FONT_MONO));
        UIManager.put("ComboBox.font", baseFont);
        UIManager.put("Table.font", baseFont);
        UIManager.put("TableHeader.font", ThemeConfig.scaleFont(ThemeConfig.FONT_CAPTION));
        SwingUtilities.updateComponentTreeUI(this);
    }

    private List<String> getRecentFiles() {
        String stored = prefs.get(RECENT_FILES_KEY, "");
        List<String> list = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (String path : stored.split("\\|")) {
                if (!path.isEmpty() && new File(path).exists()) {
                    list.add(path);
                }
            }
        }
        return list;
    }

    private void addRecentFile(String path) {
        List<String> recent = getRecentFiles();
        recent.remove(path);
        recent.add(0, path);
        while (recent.size() > MAX_RECENT_FILES) {
            recent.remove(recent.size() - 1);
        }
        prefs.put(RECENT_FILES_KEY, String.join("|", recent));
    }

    private void updateMenuRecentFiles() {
        JMenuBar menuBar = getJMenuBar();
        if (menuBar == null) return;
        JMenu fileMenu = menuBar.getMenu(0);
        if (fileMenu == null) return;
        for (int i = 0; i < fileMenu.getItemCount(); i++) {
            JMenuItem item = fileMenu.getItem(i);
            if (item instanceof JMenu && "Recent Files".equals(item.getText())) {
                refreshRecentFilesMenu((JMenu) item);
                break;
            }
        }
    }

    public static void launchGUI(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeapDumpAnalyzerGUI gui = new HeapDumpAnalyzerGUI();
            gui.setVisible(true);

            if (args != null && args.length > 0) {
                File autoFile = new File(args[0]);
                if (autoFile.exists() && autoFile.isFile()) {
                    gui.loadFileAndAnalyze(autoFile);
                }
            }
        });
    }
}