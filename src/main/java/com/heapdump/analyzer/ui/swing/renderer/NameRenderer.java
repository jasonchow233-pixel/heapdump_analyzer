package com.heapdump.analyzer.ui.swing.renderer;

import com.heapdump.analyzer.ui.swing.ThemeConfig;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.function.IntSupplier;

public class NameRenderer extends DefaultTableCellRenderer implements HoverAware {

    private IntSupplier hoverRowSupplier = () -> -1;

    public NameRenderer() {
        setFont(ThemeConfig.FONT_BODY);
    }

    @Override
    public void setHoverRowSupplier(IntSupplier supplier) {
        this.hoverRowSupplier = supplier != null ? supplier : () -> -1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        setFont(ThemeConfig.FONT_BODY);
        setText(value != null ? value.toString() : "");

        boolean hovered = row == hoverRowSupplier.getAsInt();
        if (hovered && !isSelected) {
            setBackground(ThemeConfig.getAlternateRowColor());
        }

        return this;
    }
}
