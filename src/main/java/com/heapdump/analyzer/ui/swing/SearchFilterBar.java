package com.heapdump.analyzer.ui.swing;

import cn.wanghw.Severity;
import cn.wanghw.SensitivityType;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class SearchFilterBar extends JPanel {

    private final JTextField searchField;
    private final JComboBox<SensitivityType> typeCombo;
    private final JComboBox<Severity> severityCombo;
    private final JLabel resultCountLabel;
    private Runnable filterListener;

    public SearchFilterBar() {
        setLayout(new MigLayout("insets 6 12 6 12, fillx",
            "[][grow, fill][][][][]push[]", "[]"));
        setBackground(ThemeConfig.getCardBackground());
        setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        JLabel searchIcon = new JLabel(new FlatSVGIcon("icons/search.svg", 14, 14));
        searchIcon.setForeground(ThemeConfig.getMutedTextColor());
        add(searchIcon, "gapright 6");

        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Filter results...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(ThemeConfig.FONT_BODY);
        add(searchField, "growx");

        // 新增：敏感类型下拉框
        add(new JLabel("Type:"), "gapleft 12");
        typeCombo = new JComboBox<>();
        typeCombo.setFont(ThemeConfig.FONT_CAPTION);
        typeCombo.setToolTipText("Filter by sensitivity type");
        for (SensitivityType type : SensitivityType.values()) {
            typeCombo.addItem(type);
        }
        // 设置渲染器，显示displayName
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SensitivityType) {
                    setText(((SensitivityType) value).getDisplayName());
                }
                return this;
            }
        });
        add(typeCombo, "gapleft 4, gapright 12");

        add(new JLabel("Severity:"), "gapleft 12");
        severityCombo = new JComboBox<>();
        severityCombo.setFont(ThemeConfig.FONT_CAPTION);
        severityCombo.addItem(null);
        for (Severity s : Severity.values()) severityCombo.addItem(s);
        add(severityCombo, "gapleft 4, gapright 12");

        resultCountLabel = new JLabel("");
        resultCountLabel.setFont(ThemeConfig.FONT_CAPTION);
        resultCountLabel.setForeground(ThemeConfig.getMutedTextColor());
        add(resultCountLabel, "gapright 12");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { fireFilter(); }
            public void removeUpdate(DocumentEvent e) { fireFilter(); }
            public void changedUpdate(DocumentEvent e) { fireFilter(); }
        });
        typeCombo.addActionListener(e -> fireFilter());
        severityCombo.addActionListener(e -> fireFilter());
    }

    private void fireFilter() {
        if (filterListener != null) filterListener.run();
    }

    public void setFilterListener(Runnable listener) {
        this.filterListener = listener;
    }

    public void setSeverityFilter(Severity severity) {
        severityCombo.setSelectedItem(severity);
    }

    public void setResultCount(int count) {
        resultCountLabel.setText(count + " results");
    }

    public String getSearchText() { return searchField.getText().trim().toLowerCase(); }

    // 新增：获取敏感类型过滤
    public SensitivityType getSensitivityTypeFilter() {
        Object sel = typeCombo.getSelectedItem();
        return sel instanceof SensitivityType ? (SensitivityType) sel : SensitivityType.ALL;
    }

    public Severity getSeverityFilter() {
        Object sel = severityCombo.getSelectedItem();
        return sel instanceof Severity ? (Severity) sel : null;
    }

    public JTextField getSearchField() { return searchField; }

    @Override
    public void requestFocus() {
        searchField.requestFocusInWindow();
    }
}
