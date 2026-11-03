package org.openstreetmap.josm.plugins.geoparquet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;

class GeoParquetImportServiceTest {
    private static final Bounds TANDIL = new Bounds(-37.5, -59.3, -37.2, -59.0);

    @Test void acceptsACompleteReadOnlyImportRequest() {
        assertDoesNotThrow(() -> GeoParquetImportService.validate(
                URI.create("https://example.test/data.parquet"), "Overture segment", TANDIL));
    }

    @Test void rejectsInsecureOrIncompleteRequests() {
        assertThrows(IllegalArgumentException.class, () -> GeoParquetImportService.validate(
                URI.create("http://example.test/data.parquet"), "layer", TANDIL));
        assertThrows(IllegalArgumentException.class, () -> GeoParquetImportService.validate(
                URI.create("https://example.test/data.parquet"), " ", TANDIL));
        assertThrows(IllegalArgumentException.class, () -> GeoParquetImportService.validate(
                URI.create("https://example.test/data.parquet"), "layer", null));
    }

    @Test void progressRejectsInvalidCounts() {
        assertEquals("reading", new GeoParquetImportService.Progress("reading", 3).stage());
        assertThrows(IllegalArgumentException.class,
                () -> new GeoParquetImportService.Progress("reading", -1));
    }
}
