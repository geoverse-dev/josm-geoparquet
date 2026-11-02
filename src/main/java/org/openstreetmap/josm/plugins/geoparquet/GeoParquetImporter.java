package org.openstreetmap.josm.plugins.geoparquet;

import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

/** Handles File > Open for local .parquet / .geoparquet files. */
public final class GeoParquetImporter extends FileImporter {
    public GeoParquetImporter() {
        super(new ExtensionFileFilter("parquet,geoparquet", "parquet",
                "GeoParquet files (*.parquet, *.geoparquet)"));
    }

    @Override
    public void importData(File file, ProgressMonitor monitor) throws IOException, IllegalDataException {
        if (file == null || !file.isFile()) throw new IOException("Not a local file");
        try {
            GeoParquetReferenceLayer layer = GeoParquetReader.read(file);
            // FileImporter can run off the EDT; map/layer mutations must run on the EDT.
            if (SwingUtilities.isEventDispatchThread()) {
                MainApplication.getLayerManager().addLayer(layer);
            } else {
                SwingUtilities.invokeAndWait(() -> MainApplication.getLayerManager().addLayer(layer));
            }
        } catch (Exception e) {
            if (e instanceof IOException io) throw io;
            throw new IOException("Could not open GeoParquet file: " + file.getName(), e);
        }
    }
}
