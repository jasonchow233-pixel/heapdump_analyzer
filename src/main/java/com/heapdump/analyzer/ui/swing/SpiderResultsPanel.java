package com.heapdump.analyzer.ui.swing;

import cn.wanghw.Severity;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.heapdump.analyzer.ui.swing.renderer.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpiderResultsPanel extends JPanel {

    private final HeapDumpAnalyzerGUI mainGUI;

    private SearchFilterBar filterBar;
    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel emptyState;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JCheckBox rawMemoryCheckbox;
    private JButton openButton;
    private JButton exportButton;

    private final List<Object[]> resultsData = new ArrayList<>();
    private TableRowSorter<DefaultTableModel> rowSorter;
    private int hoverRow = -1;

    public SpiderResultsPanel(HeapDumpAnalyzerGUI gui) {
        this.mainGUI = gui;
        setLayout(new MigLayout("insets 0, fill, wrap 1",
            "[grow, fill]", "[][][grow, fill][]"));
        initComponents();
    }

    private void initComponents() {
        add(createToolbar(), "growx");

        filterBar = createFilterBar();
        add(filterBar, "growx");

        add(createTablePanel(), "growx, growy");

        add(createStatusBar(), "growx");
    }

    private SearchFilterBar createFilterBar() {
        SearchFilterBar bar = new SearchFilterBar();
        bar.setFilterListener(this::applyFilter);
        return bar;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 8 12 8 12, fillx",
            "[][][][]push[]", "[]"));
        toolbar.setBackground(ThemeConfig.getCardBackground());
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        openButton = new JButton("Open", new FlatSVGIcon("icons/open.svg", 16, 16));
        openButton.putClientProperty("JButton.buttonType", "roundRect");
        openButton.setFont(ThemeConfig.FONT_CAPTION);
        openButton.setToolTipText("Open heap dump file (Ctrl+O)");
        openButton.addActionListener(e -> mainGUI.openFile());
        toolbar.add(openButton, "gapright 8");

        rawMemoryCheckbox = new JCheckBox("Raw Memory");
        rawMemoryCheckbox.setFont(ThemeConfig.FONT_CAPTION);
        rawMemoryCheckbox.setToolTipText("Enable raw memory scanning on next analysis");
        toolbar.add(rawMemoryCheckbox, "gapright 8");

        exportButton = new JButton("Export", new FlatSVGIcon("icons/export.svg", 16, 16));
        exportButton.putClientProperty("JButton.buttonType", "roundRect");
        exportButton.setFont(ThemeConfig.FONT_CAPTION);
        exportButton.setToolTipText("Export results (Ctrl+S)");
        exportButton.addActionListener(e -> mainGUI.exportResults());
        toolbar.add(exportButton, "gapright 8");

        return toolbar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));

        tableModel = new DefaultTableModel(
            new Object[]{"Spider Name", "Category", "Severity", "Preview", "Full Data"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(ThemeConfig.FONT_CAPTION.deriveFont(Font.BOLD));
        table.setRowHeight(32);
        table.setFont(ThemeConfig.FONT_BODY);
        table.setGridColor(ThemeConfig.getBorderColor());
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);

        table.getColumnModel().getColumn(0).setMinWidth(150);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(0).setMaxWidth(300);
        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setMaxWidth(140);
        table.getColumnModel().getColumn(2).setMinWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setMaxWidth(110);
        table.getColumnModel().getColumn(3).setMinWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);
        table.getColumnModel().getColumn(4).setMinWidth(0);
        table.getColumnModel().getColumn(4).setMaxWidth(0);
        table.getColumnModel().getColumn(4).setPreferredWidth(0);

        HoverAware nameRenderer = new NameRenderer();
        nameRenderer.setHoverRowSupplier(() -> hoverRow);
        table.getColumnModel().getColumn(0).setCellRenderer((NameRenderer) nameRenderer);

        HoverAware chipRenderer = new CategoryChipRenderer();
        chipRenderer.setHoverRowSupplier(() -> hoverRow);
        table.getColumnModel().getColumn(1).setCellRenderer((CategoryChipRenderer) chipRenderer);

        HoverAware badgeRenderer = new SeverityBadgeRenderer();
        badgeRenderer.setHoverRowSupplier(() -> hoverRow);
        table.getColumnModel().getColumn(2).setCellRenderer((SeverityBadgeRenderer) badgeRenderer);

        HoverAware previewRenderer = new PreviewTextRenderer();
        previewRenderer.setHoverRowSupplier(() -> hoverRow);
        table.getColumnModel().getColumn(3).setCellRenderer((PreviewTextRenderer) previewRenderer);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Hover highlight without selection change
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyCellToClipboard(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mainGUI.onSpiderSelectionChanged(getSelectedFullData());
            }
        });

        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyValueItem = new JMenuItem("Copy Value", new FlatSVGIcon("icons/copy.svg", 14, 14));
        JMenuItem copyAllItem = new JMenuItem("Copy All Info", new FlatSVGIcon("icons/copy.svg", 14, 14));

        copyValueItem.addActionListener(e -> copySelectedValue());
        copyAllItem.addActionListener(e -> mainGUI.copyDetailToClipboard());

        contextMenu.add(copyValueItem);
        contextMenu.add(copyAllItem);
        table.setComponentPopupMenu(contextMenu);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        panel.add(scrollPane, "growx, growy");

        emptyState = createEmptyStatePanel("No results yet", "Open a heap dump file to start analysis.");
        panel.add(emptyState, "growx, growy, pos 0 0 100% 100%, hidemode 3");

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new MigLayout("insets 6 12 6 12, fillx",
            "[grow, fill][]", "[]"));
        panel.setBackground(ThemeConfig.getCardBackground());
        panel.setBorder(new MatteBorder(1, 0, 0, 0, ThemeConfig.getBorderColor()));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(ThemeConfig.FONT_CAPTION);
        statusLabel.setForeground(ThemeConfig.getMutedTextColor());
        panel.add(statusLabel, "cell 0 0");

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(160, 14));
        panel.add(progressBar, "cell 1 0, gapright 8");

        cancelButton = new JButton(new FlatSVGIcon("icons/stop.svg", 14, 14));
        cancelButton.setToolTipText("Cancel analysis");
        cancelButton.setVisible(false);
        cancelButton.putClientProperty("JButton.buttonType", "borderless");
        cancelButton.addActionListener(e -> mainGUI.cancelCurrentAnalysis());
        panel.add(cancelButton, "cell 1 0");

        return panel;
    }

    private JPanel createEmptyStatePanel(String title, String subtitle) {
        JPanel panel = new JPanel(new MigLayout("insets 0, align center center", "[grow, center]", "[][grow, center]"));
        panel.setBackground(ThemeConfig.getCardBackground());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeConfig.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeConfig.getMutedTextColor());
        panel.add(titleLabel, "gapbottom 8");

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(ThemeConfig.FONT_CAPTION);
        subtitleLabel.setForeground(ThemeConfig.getMutedTextColor());
        panel.add(subtitleLabel);

        return panel;
    }

    public void setToolbarActions(Runnable openAction, Runnable exportAction) {
        // Toolbar is created separately; this is a hook if needed.
    }

    public JCheckBox getRawMemoryCheckbox() {
        return rawMemoryCheckbox;
    }

    public boolean isRawMemoryEnabled() {
        return rawMemoryCheckbox != null && rawMemoryCheckbox.isSelected();
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setProgressVisible(boolean visible) {
        progressBar.setVisible(visible);
        cancelButton.setVisible(visible);
        if (!visible) {
            progressBar.setIndeterminate(false);
        }
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    public void setProgressValue(int value) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(value);
    }

    public void clearResults() {
        resultsData.clear();
        tableModel.setRowCount(0);
        filterBar.setResultCount(0);
        setEmptyStateVisible(true);
    }

    public void addResult(Object[] row) {
        resultsData.add(row);
        tableModel.addRow(row);
        filterBar.setResultCount(tableModel.getRowCount());
        setEmptyStateVisible(tableModel.getRowCount() == 0);
    }

    public void updateResults(List<Object[]> results) {
        resultsData.clear();
        resultsData.addAll(results);
        tableModel.setRowCount(0);
        for (Object[] row : results) {
            tableModel.addRow(row);
        }
        filterBar.setResultCount(results.size());
        setEmptyStateVisible(results.isEmpty());
        applyFilter();
    }

    public List<Object[]> getResultsData() {
        return new ArrayList<>(resultsData);
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    private void setEmptyStateVisible(boolean visible) {
        emptyState.setVisible(visible);
        emptyState.getParent().revalidate();
        emptyState.getParent().repaint();
    }

    private void applyFilter() {
        if (rowSorter == null) {
            rowSorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(rowSorter);
        }

        String search = filterBar.getSearchText();
        Severity severity = filterBar.getSeverityFilter();

        rowSorter.setRowFilter(RowFilter.andFilter(Arrays.asList(
            createSearchFilter(search, new int[]{0, 1, 4}),
            createSeverityFilter(severity, 2)
        )));

        filterBar.setResultCount(rowSorter.getViewRowCount());
    }

    private RowFilter<DefaultTableModel, Integer> createSearchFilter(String search, int[] cols) {
        if (search == null || search.isEmpty()) {
            return RowFilter.regexFilter(".*");
        }
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int col : cols) {
                    Object val = entry.getValue(col);
                    if (val != null && val.toString().toLowerCase().contains(search)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private RowFilter<DefaultTableModel, Integer> createSeverityFilter(Severity severity, int col) {
        if (severity == null) {
            return RowFilter.regexFilter(".*");
        }
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object val = entry.getValue(col);
                if (val == null) return true;
                try {
                    Severity rowSev = Severity.valueOf(val.toString().toUpperCase());
                    return rowSev.ordinal() <= severity.ordinal();
                } catch (Exception e) {
                    return true;
                }
            }
        };
    }

    private void copyCellToClipboard(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if (row >= 0 && col >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            int modelCol = table.convertColumnIndexToModel(col);
            Object value = table.getModel().getValueAt(modelRow, modelCol);
            if (value != null) {
                String text = value.toString();
                if (!text.isEmpty()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(text), null);
                    setStatus("Copied: " + (text.length() > 40 ? text.substring(0, 40) + "..." : text));
                }
            }
        }
    }

    private void copySelectedValue() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            Object value = tableModel.getValueAt(modelRow, 4); // Full Data
            if (value != null) {
                String text = value.toString();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(text), null);
                setStatus("Value copied to clipboard");
            }
        }
    }

    public String getSelectedFullData() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int modelRow = table.convertRowIndexToModel(row);
            Object value = tableModel.getValueAt(modelRow, 4);
            return value != null ? value.toString() : "";
        }
        return "";
    }

    public String getAllSelectedDetails() {
        StringBuilder sb = new StringBuilder();
        int[] rows = table.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            int modelRow = table.convertRowIndexToModel(rows[i]);
            Object value = tableModel.getValueAt(modelRow, 4);
            if (value != null) {
                if (i > 0) sb.append("\n\n");
                sb.append(value);
            }
        }
        return sb.toString();
    }

    public void requestSearchFocus() {
        filterBar.requestFocus();
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    public Object[] getRowData(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        int cols = tableModel.getColumnCount();
        Object[] data = new Object[cols];
        for (int i = 0; i < cols; i++) {
            data[i] = tableModel.getValueAt(modelRow, i);
        }
        return data;
    }

    public void selectAllRows() {
        table.selectAll();
    }

    public void clearSelection() {
        table.clearSelection();
    }
}