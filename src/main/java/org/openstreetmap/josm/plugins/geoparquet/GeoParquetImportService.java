package org.openstreetmap.josm.plugins.geoparquet;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;

/** Stable cross-plugin entry point for loading read-only remote GeoParquet layers. */
public final class GeoParquetImportService {
    private GeoParquetImportService() {}

    public record Progress(String stage, int geometriesRead) {
        public Progress {
            Objects.requireNonNull(stage, "stage");
            if (geometriesRead < 0) throw new IllegalArgumentException("geometriesRead must not be negative");
        }
    }

    public record ImportResult(String layerName, int geometriesRead, boolean truncated) {}

    /**
     * Reads an HTTPS GeoParquet object away from Swing EDT and adds a read-only
     * reference layer on EDT. The returned future completes after layer registration.
     */
    public static CompletableFuture<ImportResult> loadRemote(
            URI uri, String layerName, Bounds bbox,
            Consumer<Progress> progress, BooleanSupplier cancelled) {
        validate(uri, layerName, bbox);
        Consumer<Progress> reporter = progress == null ? ignored -> {} : progress;
        BooleanSupplier cancellation = cancelled == null ? () -> false : cancelled;
        return CompletableFuture.supplyAsync(() -> {
            try {
                checkCancelled(cancellation);
                reporter.accept(new Progress("opening", 0));
                GeoParquetReader.Result result = GeoParquetReader.readRemote(
                        uri, bbox, count -> reporter.accept(new Progress("reading", count)), cancellation);
                checkCancelled(cancellation);
                if (result.geometries().isEmpty())
                    throw new IOException("No geometries intersect the requested viewport");
                GeoParquetReferenceLayer layer = new GeoParquetReferenceLayer(layerName, result.geometries());
                layer.setRemoteUri(uri);
                layer.setTruncated(result.truncated());
                runOnEdtAndWait(() -> MainApplication.getLayerManager().addLayer(layer));
                reporter.accept(new Progress("loaded", result.geometries().size()));
                return new ImportResult(layerName, result.geometries().size(), result.truncated());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    static void validate(URI uri, String layerName, Bounds bbox) {
        if (uri == null || !uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
            throw new IllegalArgumentException("GeoParquet URI must be absolute HTTPS");
        if (layerName == null || layerName.isBlank()) throw new IllegalArgumentException("layerName is required");
        if (bbox == null) throw new IllegalArgumentException("bbox is required");
    }

    private static void checkCancelled(BooleanSupplier cancelled) throws IOException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new IOException("GeoParquet import cancelled");
    }

    private static void runOnEdtAndWait(Runnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeAndWait(action);
    }
}
