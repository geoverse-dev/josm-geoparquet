package org.openstreetmap.josm.plugins.geoparquet;

import java.awt.Color;

/** Mutable style owned by a reference layer, never by OSM primitives. */
public final class GeoParquetStyle {
    private Color color = new Color(25, 105, 220);
    private float width = 1.5f;
    private int opacity = 190;
    private int pointSize = 6;
    public Color color() { return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity); }
    public Color baseColor() { return color; }
    public float width() { return width; }
    public int opacity() { return opacity; }
    public int pointSize() { return pointSize; }
    public void update(Color color, float width, int opacity, int pointSize) {
        this.color = color;
        this.width = Math.max(0.5f, width);
        this.opacity = Math.clamp(opacity, 0, 255);
        this.pointSize = Math.max(1, pointSize);
    }
}
