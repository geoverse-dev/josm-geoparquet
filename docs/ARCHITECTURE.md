# Architecture

## Components

1. `GeoParquetPlugin`: JOSM plugin entry point and importer registration.
2. `GeoParquetImporter`: local file entry point; schedules layer registration on Swing EDT.
3. `GeoParquetReader`: Parquetry read path; WKB decoding; bounded feature accumulation.
4. `GeoParquetReferenceLayer`: draws JTS geometries as read-only map overlay.
5. `GeoParquetImportService`: stable asynchronous API for dependent JOSM plugins.

## Public import contract

`GeoParquetImportService.loadRemote` accepts the object URI, layer name, WGS84
viewport, progress callback and cancellation token. It validates HTTPS input, reads
on a background executor and registers the resulting layer on Swing EDT. The future
completes only after registration. The service never returns editable OSM data.

## Safety boundary

The reader never creates OSM `Node`, `Way`, `Relation`, `DataSet`, or `OsmDataLayer` instances. The custom `Layer` has no save, merge, or upload behavior. Overture data must never be added to an editable OSM layer.

## Next iterations

- Read GeoParquet metadata and select primary geometry column.
- Validate/reproject CRS rather than assuming EPSG:4326.
- BBOX filtering via `Pred.col("geometry").bboxIntersects(Bbox.of2d(...))`.
- Virtualized/streaming rendering for large datasets.
- Read-only attribute inspector, progress and cancellation.
- Extend the public API only compatibly; it is now used by `josm-overturemaps`.
