package org.openstreetmap.josm.plugins.geoparquet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.UIManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;

/** Non-OSM, non-editable, non-uploadable reference geometry layer. */
public final class GeoParquetReferenceLayer extends Layer {
    private final List<Geometry> geometries;
    public GeoParquetReferenceLayer(String name, List<Geometry> geometries) {
        super(name);
        this.geometries = List.copyOf(geometries);
        setBackgroundLayer(true);
    }
    @Override public Icon getIcon() { return UIManager.getIcon("FileView.fileIcon"); }
    @Override public String getToolTipText() { return geometries.size() + " GeoParquet geometries (read-only)"; }
    @Override public void mergeFrom(Layer from) { throw new UnsupportedOperationException("Read-only"); }
    @Override public boolean isMergable(Layer other) { return false; }
    @Override public void visitBoundingBox(BoundingXYVisitor visitor) {
        for (Geometry geom : geometries) {
            var e = geom.getEnvelopeInternal();
            if (!e.isNull()) {
                visitor.visit(new LatLon(e.getMinY(), e.getMinX()));
                visitor.visit(new LatLon(e.getMaxY(), e.getMaxX()));
            }
        }
    }
    @Override public Object getInfoComponent() { return getToolTipText(); }
    @Override public Action[] getMenuEntries() { return new Action[0]; }
    @Override public void paint(Graphics2D g, MapView mapView, Bounds bounds) {
        Graphics2D out = (Graphics2D) g.create();
        try {
            out.setColor(new Color(25, 105, 220, 190));
            out.setStroke(new BasicStroke(1.5f));
            for (Geometry geom : geometries) draw(out, mapView, geom);
        } finally { out.dispose(); }
    }
    private static void draw(Graphics2D g, MapView view, Geometry geom) {
        if (geom instanceof GeometryCollection collection) {
            for (int i = 0; i < collection.getNumGeometries(); i++) draw(g, view, collection.getGeometryN(i));
        } else if (geom instanceof Polygon poly) {
            drawLine(g, view, poly.getExteriorRing(), true);
            for (int i = 0; i < poly.getNumInteriorRing(); i++) drawLine(g, view, poly.getInteriorRingN(i), true);
        } else if (geom instanceof LineString line) {
            drawLine(g, view, line, false);
        } else if (geom instanceof org.locationtech.jts.geom.Point pt) {
            Point p = view.getPoint(new LatLon(pt.getY(), pt.getX()));
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
    }
    private static void drawLine(Graphics2D g, MapView view, LineString line, boolean close) {
        Coordinate[] coords = line.getCoordinates();
        if (coords.length == 0) return;
        Path2D path = new Path2D.Double();
        for (int i = 0; i < coords.length; i++) {
            Point p = view.getPoint(new LatLon(coords[i].y, coords[i].x));
            if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
        }
        if (close) path.closePath();
        g.draw(path);
    }
}
