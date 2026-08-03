package com.heapdump.analyzer.ui.swing;

import cn.wanghw.IHeapHolder;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomSearchDialog extends JDialog {

    private final IHeapHolder heapHolder;
    private JTextField searchField;
    private JCheckBox regexCheckbox;
    private JCheckBox caseSensitiveCheckbox;
    private JSpinner contextSizeSpinner;
    private JButton searchButton;
    private JButton exportButton;
    private JButton closeButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<SearchResult> searchResults = new ArrayList<>();

    public CustomSearchDialog(Window parent, IHeapHolder heapHolder) {
        super(parent, "Custom String Search", ModalityType.MODELESS);
        this.heapHolder = heapHolder;
        initComponents();
        layoutComponents();
        registerKeyBindings();

        setSize(900, 600);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(700, 400));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void initComponents() {
        searchField = new JTextField(30);
        searchField.putClientProperty("JTextField.placeholderText", "Enter search string...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(ThemeConfig.FONT_BODY);

        regexCheckbox = new JCheckBox("Regex");
        regexCheckbox.setFont(ThemeConfig.FONT_CAPTION);
        regexCheckbox.setToolTipText("Treat search string as regular expression");

        caseSensitiveCheckbox = new JCheckBox("Case Sensitive");
        caseSensitiveCheckbox.setFont(ThemeConfig.FONT_CAPTION);
        caseSensitiveCheckbox.setToolTipText("Enable case-sensitive matching");

        contextSizeSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1000, 50));
        contextSizeSpinner.setFont(ThemeConfig.FONT_CAPTION);
        contextSizeSpinner.setToolTipText("Number of characters to show before and after match");

        searchButton = new JButton("Search", new FlatSVGIcon("icons/search.svg", 14, 14));
        searchButton.putClientProperty("JButton.buttonType", "roundRect");
        searchButton.setFont(ThemeConfig.FONT_CAPTION);
        searchButton.addActionListener(e -> performSearch());

        exportButton = new JButton("Export", new FlatSVGIcon("icons/export.svg", 14, 14));
        exportButton.putClientProperty("JButton.buttonType", "roundRect");
        exportButton.setFont(ThemeConfig.FONT_CAPTION);
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportResults());

        closeButton = new JButton("Close");
        closeButton.setFont(ThemeConfig.FONT_CAPTION);
        closeButton.addActionListener(e -> dispose());

        tableModel = new DefaultTableModel(
            new Object[]{"Match", "Context"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setFont(ThemeConfig.FONT_BODY);
        resultsTable.setRowHeight(28);
        resultsTable.getTableHeader().setFont(ThemeConfig.FONT_CAPTION.deriveFont(Font.BOLD));
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(600);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showContextInDetailPanel(e);
            }
        });

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(ThemeConfig.FONT_CAPTION);
        statusLabel.setForeground(ThemeConfig.getMutedTextColor());

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 16));
    }

    private void layoutComponents() {
        setLayout(new MigLayout("insets 12, fill, wrap 1", "[grow, fill]", "[][][grow, fill][]"));

        // Search panel
        JPanel searchPanel = new JPanel(new MigLayout("insets 0, fillx", "[][grow, fill][][][][]", "[]"));
        searchPanel.setBackground(ThemeConfig.getCardBackground());
        searchPanel.setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        searchPanel.add(new JLabel("Search:"), "gapright 8");
        searchPanel.add(searchField, "growx");
        searchPanel.add(regexCheckbox, "gapleft 12");
        searchPanel.add(caseSensitiveCheckbox, "gapleft 8");
        searchPanel.add(new JLabel("Context:"), "gapleft 12, gapright 4");
        searchPanel.add(contextSizeSpinner, "w 60!");
        searchPanel.add(searchButton, "gapleft 12");

        add(searchPanel, "growx");

        // Results table
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(null);
        add(scrollPane, "growx, growy");

        // Button panel
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, fillx", "push[][][]", "[]"));
        buttonPanel.setBackground(ThemeConfig.getCardBackground());
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, ThemeConfig.getBorderColor()));

        buttonPanel.add(statusLabel, "pushx");
        buttonPanel.add(progressBar, "gapright 8");
        buttonPanel.add(exportButton, "gapright 8");
        buttonPanel.add(closeButton);

        add(buttonPanel, "growx");
    }

    private void registerKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                performSearch();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        actionMap.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a search string",
                "Input Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (heapHolder == null) {
            JOptionPane.showMessageDialog(this,
                "No heap dump loaded",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        exportButton.setEnabled(false);
        statusLabel.setText("Searching...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        executor.submit(() -> {
            try {
                List<SearchResult> results = executeSearch(searchText);
                SwingUtilities.invokeLater(() -> {
                    displayResults(results);
                    searchButton.setEnabled(true);
                    progressBar.setVisible(false);
                    exportButton.setEnabled(!results.isEmpty());
                    statusLabel.setText(String.format("Found %d matches", results.size()));
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    searchButton.setEnabled(true);
                    progressBar.setVisible(false);
                });
            }
        });
    }

    private List<SearchResult> executeSearch(String searchText) {
        List<SearchResult> results = new ArrayList<>();
        int contextSize = (Integer) contextSizeSpinner.getValue();
        boolean useRegex = regexCheckbox.isSelected();
        boolean caseSensitive = caseSensitiveCheckbox.isSelected();

        Pattern pattern;
        if (useRegex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(searchText, flags);
        } else {
            String escaped = Pattern.quote(searchText);
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            pattern = Pattern.compile(escaped, flags);
        }

        // Use searchAll to search both Java objects and raw memory
        List<String> matches = heapHolder.searchAll(pattern);

        for (String match : matches) {
            // Extract context around the match
            Matcher matcher = pattern.matcher(match);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String matchedText = matcher.group();

                // Calculate context boundaries
                int contextStart = Math.max(0, start - contextSize);
                int contextEnd = Math.min(match.length(), end + contextSize);
                String context = match.substring(contextStart, contextEnd);

                // Mark the matched portion
                String before = match.substring(contextStart, start);
                String after = match.substring(end, contextEnd);

                SearchResult result = new SearchResult(matchedText, before, after, context, match);
                results.add(result);
            }
        }

        return results;
    }

    private void displayResults(List<SearchResult> results) {
        searchResults.clear();
        searchResults.addAll(results);
        tableModel.setRowCount(0);

        for (SearchResult result : results) {
            tableModel.addRow(new Object[]{result.matchedText, result.getDisplayContext()});
        }
    }

    private void showContextInDetailPanel(javax.swing.event.ListSelectionEvent e) {
        int row = resultsTable.getSelectedRow();
        if (row >= 0 && row < searchResults.size()) {
            SearchResult result = searchResults.get(row);
            String detail = buildDetailText(result);
            // Find the main GUI and show in detail panel
            Container parent = getParent();
            while (parent != null && !(parent instanceof HeapDumpAnalyzerGUI)) {
                parent = parent.getParent();
            }
            if (parent instanceof HeapDumpAnalyzerGUI) {
                HeapDumpAnalyzerGUI gui = (HeapDumpAnalyzerGUI) parent;
                gui.showSearchResultDetail("Custom Search Result", detail);
            }
        }
    }

    private String buildDetailText(SearchResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matched Text:\n");
        sb.append(result.matchedText).append("\n\n");
        sb.append("Full Context:\n");
        sb.append(result.before).append("【").append(result.matchedText).append("】").append(result.after).append("\n\n");
        sb.append("Full String:\n");
        sb.append(result.fullString);
        return sb.toString();
    }

    private void exportResults() {
        if (searchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No results to export",
                "Export",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Search Results");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text File", "txt"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV File", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = buildExportContent(file.getName());
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                statusLabel.setText("Exported to: " + file.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Export failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String buildExportContent(String fileName) {
        String ext = getFileExtension(fileName);
        if ("csv".equalsIgnoreCase(ext)) {
            return buildCsvContent();
        } else {
            return buildTextContent();
        }
    }

    private String buildTextContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("Custom Search Results\n");
        sb.append("Search String: ").append(searchField.getText()).append("\n");
        sb.append("Total Matches: ").append(searchResults.size()).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = searchResults.get(i);
            sb.append("Match #").append(i + 1).append(":\n");
            sb.append("  Matched: ").append(result.matchedText).append("\n");
            sb.append("  Context: ").append(result.getDisplayContext()).append("\n");
            sb.append("  Full String: ").append(result.fullString).append("\n\n");
        }

        return sb.toString();
    }

    private String buildCsvContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("Match,Context,Full String\n");

        for (SearchResult result : searchResults) {
            sb.append(escapeCsv(result.matchedText)).append(",");
            sb.append(escapeCsv(result.getDisplayContext())).append(",");
            sb.append("\"").append(result.fullString.replace("\"", "\"\"")).append("\"\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "txt";
    }

    private static class SearchResult {
        final String matchedText;
        final String before;
        final String after;
        final String context;
        final String fullString;

        SearchResult(String matchedText, String before, String after, String context, String fullString) {
            this.matchedText = matchedText;
            this.before = before;
            this.after = after;
            this.context = context;
            this.fullString = fullString;
        }

        String getDisplayContext() {
            return before + "【" + matchedText + "】" + after;
        }
    }
}
