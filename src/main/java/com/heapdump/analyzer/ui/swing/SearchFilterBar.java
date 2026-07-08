package com.heapdump.analyzer.ui.swing;

import cn.wanghw.Severity;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SearchFilterBar extends JPanel {

    private final JTextField searchField;
    private final JComboBox<Severity> severityCombo;
    private final JLabel resultCountLabel;
    private Runnable filterListener;

    public SearchFilterBar() {
        setLayout(new MigLayout("insets 6 12 6 12, fillx",
            "[][grow, fill][][][]push[]", "[]"));
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
