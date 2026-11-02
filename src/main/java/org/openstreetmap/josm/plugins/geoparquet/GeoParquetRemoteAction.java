package org.openstreetmap.josm.plugins.geoparquet;

import java.net.URI;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;

/** Opens a remote range-enabled HTTP(S) GeoParquet over the current JOSM viewport. */
public final class GeoParquetRemoteAction {
    private GeoParquetRemoteAction() {}
    public static void open() {
        var map = MainApplication.getMap();
        if (map == null || map.mapView == null) {
            JOptionPane.showMessageDialog(null, "Open the JOSM map and navigate to the target area first.");
            return;
        }
        String value = JOptionPane.showInputDialog(null, "HTTP(S) URL to a GeoParquet object (supports Range requests):",
                "Open remote GeoParquet", JOptionPane.QUESTION_MESSAGE);
        if (value == null || value.isBlank()) return;
        final URI uri;
        try { uri = URI.create(value.trim()); }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Invalid URL", JOptionPane.ERROR_MESSAGE); return;
        }
        Bounds visible = map.mapView.getRealBounds();
        new SwingWorker<GeoParquetReader.Result, Void>() {
            @Override protected GeoParquetReader.Result doInBackground() throws Exception {
                return GeoParquetReader.readRemote(uri, visible);
            }
            @Override protected void done() {
                try {
                    GeoParquetReader.Result result = get();
                    if (result.geometries().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "No geometries intersect the current viewport."); return;
                    }
                    String path = uri.getPath();
                    String name = path == null || path.isBlank() ? uri.getHost() : path.substring(path.lastIndexOf('/') + 1);
                    GeoParquetReferenceLayer layer = new GeoParquetReferenceLayer(name, result.geometries());
                    layer.setRemoteUri(uri);
                    layer.setTruncated(result.truncated());
                    MainApplication.getLayerManager().addLayer(layer);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Remote GeoParquet import failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
