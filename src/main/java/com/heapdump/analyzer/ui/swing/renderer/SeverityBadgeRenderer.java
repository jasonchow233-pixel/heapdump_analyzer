package com.heapdump.analyzer.ui.swing.renderer;

import cn.wanghw.Severity;
import com.heapdump.analyzer.ui.swing.ThemeConfig;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.IntSupplier;

public class SeverityBadgeRenderer extends JPanel implements TableCellRenderer, HoverAware {

    private final JLabel label;
    private IntSupplier hoverRowSupplier = () -> -1;

    public SeverityBadgeRenderer() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        label = new JLabel();
        label.setFont(ThemeConfig.FONT_CAPTION.deriveFont(Font.BOLD));
        label.setOpaque(true);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        add(label);
    }

    @Override
    public void setHoverRowSupplier(IntSupplier supplier) {
        this.hoverRowSupplier = supplier != null ? supplier : () -> -1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        String text = value != null ? value.toString() : "";
        label.setText(text);

        Severity sev;
        try {
            sev = Severity.valueOf(text.toUpperCase());
        } catch (Exception e) {
            sev = Severity.INFO;
        }
        label.setBackground(ThemeConfig.getSeverityColor(sev));

        boolean hovered = row == hoverRowSupplier.getAsInt();
        Color bg = table.getBackground();
        if (hovered && !isSelected) {
            bg = ThemeConfig.getAlternateRowColor();
        }

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
            label.setBackground(ThemeConfig.getSeverityColor(sev));
        } else {
            setBackground(bg);
            label.setForeground(Color.WHITE);
        }

        return this;
    }
}
