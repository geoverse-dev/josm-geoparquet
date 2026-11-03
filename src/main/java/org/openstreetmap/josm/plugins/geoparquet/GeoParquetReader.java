package org.openstreetmap.josm.plugins.geoparquet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import io.tileverse.parquetry.data.ParquetFileReader;
import io.tileverse.parquetry.data.ReadOptions;
import io.tileverse.parquetry.filter.Bbox;
import io.tileverse.parquetry.filter.Pred;
import io.tileverse.parquetry.filter.Predicate;
import io.tileverse.parquetry.filter.Projection;
import io.tileverse.parquetry.io.ByteRangeSource;
import io.tileverse.parquetry.record.ParquetRecord;
import io.tileverse.parquetry.schema.ColumnPath;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.WKBReader;
import org.openstreetmap.josm.data.Bounds;

/** Read local or remote GeoParquet without converting records to OSM primitives. */
public final class GeoParquetReader {
    private static final ColumnPath GEOMETRY = ColumnPath.of("geometry");
    private GeoParquetReader() {}
    public record Result(List<Geometry> geometries, boolean truncated) {}

    public static GeoParquetReferenceLayer read(File file) throws IOException {
        try (ByteRangeSource bytes = ByteRangeSource.ofFile(file.toPath())) {
            Result result = read(bytes, null);
            GeoParquetReferenceLayer layer = new GeoParquetReferenceLayer(file.getName(), result.geometries());
            layer.setAssociatedFile(file);
            layer.setTruncated(result.truncated());
            return layer;
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("GeoParquet read failed", e); }
    }

    public static Result readRemote(URI uri, Bounds bounds) throws IOException {
        return readRemote(uri, bounds, ignored -> {}, () -> false);
    }

    static Result readRemote(URI uri, Bounds bounds, IntConsumer progress, BooleanSupplier cancelled) throws IOException {
        try (ByteRangeSource bytes = new HttpRangeSource(uri)) {
            return read(bytes, bounds, progress, cancelled);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("Remote GeoParquet read failed", e); }
    }

    private static Result read(ByteRangeSource bytes, Bounds bounds) throws IOException {
        return read(bytes, bounds, ignored -> {}, () -> false);
    }

    private static Result read(ByteRangeSource bytes, Bounds bounds, IntConsumer progress, BooleanSupplier cancelled) throws IOException {
        List<Geometry> geometries = new ArrayList<>();
        WKBReader decoder = new WKBReader();
        int limit = GeoParquetSettings.maxFeatures();
        boolean truncated = false;
        try {
            ParquetFileReader source = ParquetFileReader.open(bytes);
            Predicate predicate = bounds == null ? Predicate.ALWAYS_TRUE : Pred.col("geometry").bboxIntersects(
                    Bbox.of2d(bounds.getMinLon(), bounds.getMinLat(), bounds.getMaxLon(), bounds.getMaxLat()));
            Envelope visible = bounds == null ? null : new Envelope(
                    bounds.getMinLon(), bounds.getMaxLon(), bounds.getMinLat(), bounds.getMaxLat());
            try (Stream<ParquetRecord> rows = source.read(predicate, Projection.ALL, ReadOptions.DEFAULTS)) {
                var iterator = rows.iterator();
                while (iterator.hasNext()) {
                    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
                        throw new IOException("GeoParquet import cancelled");
                    ParquetRecord row = iterator.next();
                    if (row.isNull(GEOMETRY)) continue;
                    byte[] wkb = row.getBinary(GEOMETRY);
                    if (wkb == null) continue;
                    Geometry geometry = decoder.read(wkb);
                    if (geometry.isEmpty() || (visible != null && !geometry.getEnvelopeInternal().intersects(visible))) continue;
                    if (limit > 0 && geometries.size() >= limit) {
                        if (!GeoParquetSettings.keepPartial())
                            throw new IOException("Feature limit (" + limit + ") exceeded; import cancelled by preference.");
                        truncated = true;
                        break;
                    }
                    geometries.add(geometry);
                    progress.accept(geometries.size());
                }
            }
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("GeoParquet read failed (requires WKB geometry in EPSG:4326)", e); }
        return new Result(List.copyOf(geometries), truncated);
    }
}
