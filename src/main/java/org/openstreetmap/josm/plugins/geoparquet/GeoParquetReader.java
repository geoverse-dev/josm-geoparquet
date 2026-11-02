package org.openstreetmap.josm.plugins.geoparquet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import io.tileverse.parquetry.data.ParquetFileReader;
import io.tileverse.parquetry.data.ReadOptions;
import io.tileverse.parquetry.filter.Predicate;
import io.tileverse.parquetry.filter.Projection;
import io.tileverse.parquetry.io.ByteRangeSource;
import io.tileverse.parquetry.record.ParquetRecord;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

/**
 * Minimal local reader. Deliberately bounded: no remote access, no writing,
 * no geometry-to-OSM conversion, and no unbounded feature accumulation.
 */
public final class GeoParquetReader {
    private static final int MAX_FEATURES = Integer.getInteger("josm.geoparquet.maxFeatures", 1_000_000);
    private GeoParquetReader() {}

    public static GeoParquetReferenceLayer read(File file) throws IOException {
        List<Geometry> geometries = new ArrayList<>();
        WKBReader decoder = new WKBReader();
        try (ByteRangeSource bytes = ByteRangeSource.ofFile(file.toPath())) {
            ParquetFileReader source = ParquetFileReader.open(bytes);
            try (Stream<ParquetRecord> rows = source.read(Predicate.ALWAYS_TRUE, Projection.ALL, ReadOptions.DEFAULTS)) {
                var iterator = rows.iterator();
                while (iterator.hasNext()) {
                    ParquetRecord row = iterator.next();
                    if (row.isNull(io.tileverse.parquetry.schema.ColumnPath.of("geometry"))) continue;
                    byte[] wkb = row.getBinary(io.tileverse.parquetry.schema.ColumnPath.of("geometry"));
                    if (wkb == null) continue;
                    Geometry geometry = decoder.read(wkb);
                    if (geometry.isEmpty()) continue;
                    if (geometries.size() >= MAX_FEATURES) {
                        throw new IOException("Feature limit (" + MAX_FEATURES
                                + ") exceeded. No partial layer imported. Use a smaller local file.");
                    }
                    geometries.add(geometry);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("GeoParquet read failed (requires a WKB column named 'geometry')", e);
        }
        if (geometries.isEmpty()) throw new IOException("No supported geometries in " + file.getName());
        GeoParquetReferenceLayer layer = new GeoParquetReferenceLayer(file.getName(), geometries);
        layer.setAssociatedFile(file);
        return layer;
    }
}
