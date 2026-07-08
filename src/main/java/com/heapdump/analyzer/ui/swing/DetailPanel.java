package com.heapdump.analyzer.ui.swing;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class DetailPanel extends JPanel {

    private JLabel titleLabel;
    private HighlightTextPane textPane;
    private JButton copyValueBtn;
    private JButton copyAllBtn;
    private JButton clearBtn;

    private String currentDetail = "";

    public DetailPanel() {
        setLayout(new MigLayout("insets 0, fill, wrap 1",
            "[grow, fill]", "[][grow, fill][]"));
        setBackground(ThemeConfig.getCardBackground());

        add(createTitleBar(), "growx");

        textPane = new HighlightTextPane();
        textPane.setFont(ThemeConfig.FONT_MONO);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        add(scrollPane, "growx, growy");

        add(createButtonBar(), "growx");
    }

    private JPanel createTitleBar() {
        JPanel panel = new JPanel(new MigLayout("insets 10 12 10 12, fillx",
            "[][grow, fill]", "[]"));
        panel.setBackground(ThemeConfig.getCardBackground());
        panel.setBorder(new MatteBorder(0, 0, 1, 0, ThemeConfig.getBorderColor()));

        titleLabel = new JLabel("Detail View");
        titleLabel.setFont(ThemeConfig.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeConfig.ACCENT);
        panel.add(titleLabel, "cell 0 0");

        return panel;
    }

    private JPanel createButtonBar() {
        JPanel panel = new JPanel(new MigLayout("insets 8 12 8 12, flowx",
            "[][][]", "[]"));
        panel.setBackground(ThemeConfig.getCardBackground());
        panel.setBorder(new MatteBorder(1, 0, 0, 0, ThemeConfig.getBorderColor()));

        copyValueBtn = new JButton("Copy Value", new FlatSVGIcon("icons/copy.svg", 14, 14));
        copyValueBtn.setFont(ThemeConfig.FONT_CAPTION);
        copyValueBtn.putClientProperty("JButton.buttonType", "borderless");
        copyValueBtn.addActionListener(e -> copyValue());
        panel.add(copyValueBtn);

        copyAllBtn = new JButton("Copy All", new FlatSVGIcon("icons/copy.svg", 14, 14));
        copyAllBtn.setFont(ThemeConfig.FONT_CAPTION);
        copyAllBtn.putClientProperty("JButton.buttonType", "borderless");
        copyAllBtn.addActionListener(e -> copyAll());
        panel.add(copyAllBtn);

        clearBtn = new JButton("Clear", new FlatSVGIcon("icons/clear.svg", 14, 14));
        clearBtn.setFont(ThemeConfig.FONT_CAPTION);
        clearBtn.putClientProperty("JButton.buttonType", "borderless");
        clearBtn.addActionListener(e -> clear());
        panel.add(clearBtn);

        return panel;
    }

    public void setDetail(String title, String detail) {
        currentDetail = detail != null ? detail : "";
        titleLabel.setText(title != null && !title.isEmpty() ? title : "Detail View");
        textPane.setHighlightedText(currentDetail);
    }

    public void setDetailFromSpider(String spiderName, String category, String severityLabel, String fullData) {
        if (fullData == null || fullData.isEmpty()) {
            clear();
            return;
        }

        String title = spiderName + " [" + severityLabel + "]";
        setDetail(title, fullData);
    }

    public void clear() {
        currentDetail = "";
        titleLabel.setText("Detail View");
        textPane.setHighlightedText("");
    }

    public String getDetailText() {
        return textPane.getPlainText();
    }

    private void copyValue() {
        if (!currentDetail.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(currentDetail), null);
            showCopyFeedback(copyValueBtn, "Copied", "Copy Value");
        }
    }

    private void copyAll() {
        String text = textPane.getPlainText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null);
            showCopyFeedback(copyAllBtn, "Copied", "Copy All");
        }
    }

    private void showCopyFeedback(JButton btn, String feedback, String original) {
        btn.setText(feedback);
        Timer timer = new Timer(1500, e -> btn.setText(original));
        timer.setRepeats(false);
        timer.start();
    }
}