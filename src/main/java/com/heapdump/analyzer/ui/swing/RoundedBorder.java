package com.heapdump.analyzer.ui.swing;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class RoundedBorder extends AbstractBorder {

    private final int radius;
    private final Color color;
    private final int thickness;

    public RoundedBorder(int radius, Color color) {
        this(radius, color, 1);
    }

    public RoundedBorder(int radius, Color color, int thickness) {
        this.radius = radius;
        this.color = color;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
            width - thickness, height - thickness, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int pad = radius / 2 + thickness;
        return new Insets(pad, pad, pad, pad);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int pad = radius / 2 + thickness;
        insets.set(pad, pad, pad, pad);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
