package com.heapdump.analyzer.ui.swing.renderer;

import com.heapdump.analyzer.ui.swing.ThemeConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class StatusIconRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        String status = value != null ? value.toString() : "";
        if ("Found".equals(status)) {
            setForeground(ThemeConfig.SEVERITY_LOW);
            setText("✓ Found");
        } else {
            setForeground(ThemeConfig.getMutedTextColor());
            setText("✗ Not Found");
        }

        if (isSelected) {
            setForeground(table.getSelectionForeground());
        }

        setFont(ThemeConfig.FONT_CAPTION);
        return this;
    }
}
