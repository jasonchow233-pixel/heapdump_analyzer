package com.heapdump.analyzer.ui.swing;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ModernCardPanel extends JPanel {

    private final JLabel titleLabel;

    public ModernCardPanel(String title) {
        this(title, true);
    }

    public ModernCardPanel(String title, boolean showBorder) {
        setLayout(new MigLayout("insets 12, fill, wrap 1", "[grow, fill]", ""));
        setOpaque(true);
        setBackground(ThemeConfig.getCardBackground());

        if (showBorder) {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 0, 6, 0),
                new RoundedBorder(8, ThemeConfig.getBorderColor())
            ));
        }

        if (title != null) {
            titleLabel = new JLabel(title.toUpperCase());
            titleLabel.setFont(ThemeConfig.FONT_CAPTION);
            titleLabel.setForeground(ThemeConfig.getMutedTextColor());
            add(titleLabel, "gapbottom 8");
        } else {
            titleLabel = null;
        }
    }

    public void setTitle(String title) {
        if (titleLabel != null) {
            titleLabel.setText(title.toUpperCase());
        }
    }
}
