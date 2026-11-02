package org.openstreetmap.josm.plugins.geoparquet;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Small Swing dialogs for the initial configuration and style workflow. */
public final class GeoParquetDialogs {
    private GeoParquetDialogs() {}
    public static void settings() {
        JSpinner limit = new JSpinner(new SpinnerNumberModel(GeoParquetSettings.maxFeatures(), 0, Integer.MAX_VALUE, 100000));
        JCheckBox partial = new JCheckBox("Keep partial layer when limit is reached", GeoParquetSettings.keepPartial());
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Maximum geometries (0 = unlimited, memory risk):"));
        panel.add(limit);
        panel.add(partial);
        if (JOptionPane.showConfirmDialog(null, panel, "GeoParquet import settings", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            GeoParquetSettings.save(((Number) limit.getValue()).intValue(), partial.isSelected());
        }
    }
    public static void style(GeoParquetReferenceLayer layer) {
        GeoParquetStyle style = layer.style();
        Color color = JColorChooser.showDialog(null, "GeoParquet line and point color", style.baseColor());
        if (color == null) return;
        JSpinner width = new JSpinner(new SpinnerNumberModel((double) style.width(), 0.5, 20.0, 0.5));
        JSpinner opacity = new JSpinner(new SpinnerNumberModel(style.opacity(), 0, 255, 5));
        JSpinner pointSize = new JSpinner(new SpinnerNumberModel(style.pointSize(), 1, 30, 1));
        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        panel.add(new JLabel("Line width (px):")); panel.add(width);
        panel.add(new JLabel("Opacity (0-255):")); panel.add(opacity);
        panel.add(new JLabel("Point size (px):")); panel.add(pointSize);
        if (JOptionPane.showConfirmDialog(null, panel, "GeoParquet layer style", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            style.update(color, ((Number) width.getValue()).floatValue(),
                    ((Number) opacity.getValue()).intValue(), ((Number) pointSize.getValue()).intValue());
            layer.invalidate();
        }
    }
}
