package org.openstreetmap.josm.plugins.geoparquet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import org.locationtech.jts.geom.Envelope;
import java.awt.geom.Path2D;
import java.util.List;
import java.net.URI;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.openstreetmap.josm.gui.MainApplication;
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
    private volatile List<Geometry> geometries;
    private final GeoParquetStyle style = new GeoParquetStyle();
    private URI remoteUri;
    private volatile boolean truncated;
    public GeoParquetStyle style() { return style; }
    public void setTruncated(boolean value) { truncated = value; }
    public void setRemoteUri(URI value) { remoteUri = value; }
    public void replaceGeometries(GeoParquetReader.Result result) {
        geometries = List.copyOf(result.geometries());
        truncated = result.truncated();
        invalidate();
    }
    public void invalidate() {
        if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null)
            MainApplication.getMap().mapView.repaint();
    }
    public GeoParquetReferenceLayer(String name, List<Geometry> geometries) {
        super(name);
        this.geometries = List.copyOf(geometries);
        setBackgroundLayer(true);
    }
    @Override public Icon getIcon() { return UIManager.getIcon("FileView.fileIcon"); }
    @Override public String getToolTipText() { return geometries.size() + " GeoParquet geometries (read-only)" + (truncated ? " [partial: limit reached]" : ""); }
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
    @Override public Action[] getMenuEntries() {
        Action styleAction = new AbstractAction("GeoParquet style...") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { GeoParquetDialogs.style(GeoParquetReferenceLayer.this); }
        };
        Action settingsAction = new AbstractAction("GeoParquet import settings...") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { GeoParquetDialogs.settings(); }
        };
        if (remoteUri == null) return new Action[]{styleAction, settingsAction};
        Action refresh = new AbstractAction("Reload GeoParquet for current viewport") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                var map = MainApplication.getMap();
                if (map == null || map.mapView == null) return;
                Bounds visible = map.mapView.getRealBounds();
                new SwingWorker<GeoParquetReader.Result, Void>() {
                    @Override protected GeoParquetReader.Result doInBackground() throws Exception {
                        return GeoParquetReader.readRemote(remoteUri, visible);
                    }
                    @Override protected void done() {
                        try { replaceGeometries(get()); }
                        catch (Exception ex) { JOptionPane.showMessageDialog(null, ex.getMessage(), "GeoParquet remote reload failed", JOptionPane.ERROR_MESSAGE); }
                    }
                }.execute();
            }
        };
        return new Action[]{styleAction, settingsAction, refresh};
    }
    @Override public void paint(Graphics2D g, MapView mapView, Bounds bounds) {
        Graphics2D out = (Graphics2D) g.create();
        try {
            out.setColor(style.color());
            out.setStroke(new BasicStroke(style.width()));
            Envelope viewport = new Envelope(bounds.getMinLon(), bounds.getMaxLon(), bounds.getMinLat(), bounds.getMaxLat());
            for (Geometry geom : geometries) {
                if (geom.getEnvelopeInternal().intersects(viewport)) draw(out, mapView, geom, style.pointSize());
            }
        } finally { out.dispose(); }
    }
    private static void draw(Graphics2D g, MapView view, Geometry geom, int pointSize) {
        if (geom instanceof GeometryCollection collection) {
            for (int i = 0; i < collection.getNumGeometries(); i++) draw(g, view, collection.getGeometryN(i), pointSize);
        } else if (geom instanceof Polygon poly) {
            drawLine(g, view, poly.getExteriorRing(), true);
            for (int i = 0; i < poly.getNumInteriorRing(); i++) drawLine(g, view, poly.getInteriorRingN(i), true);
        } else if (geom instanceof LineString line) {
            drawLine(g, view, line, false);
        } else if (geom instanceof org.locationtech.jts.geom.Point pt) {
            Point p = view.getPoint(new LatLon(pt.getY(), pt.getX()));
            g.fillOval(p.x - pointSize / 2, p.y - pointSize / 2, pointSize, pointSize);
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
