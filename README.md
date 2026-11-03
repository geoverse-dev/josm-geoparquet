# JOSM GeoParquet

A **working, read-only GeoParquet reference-layer plugin for JOSM**, powered by [Parquetry](https://github.com/tileverse-io/parquetry).

Load GeoParquet files from your local filesystem or directly from an HTTP(S) object endpoint, including GeoParquet published in a public S3 bucket or compatible object storage. Display the geometries alongside editable OpenStreetMap data without importing them as OSM objects.

## Status

**Version 0.3 — Public cross-plugin import API added; core 0.2 workflows were functionally validated in JOSM.**

The plugin has been built, installed, and exercised in JOSM by the project maintainer. Confirmed workflows include:

- Opening and displaying local GeoParquet datasets.
- Displaying Overture Maps transportation geometries alongside OSM data.
- Reading a GeoParquet object published in S3 through its HTTPS endpoint.
- Using the current JOSM viewport as the spatial query for remote loading.
- Configuring geometry-count limits and customizing reference-layer appearance.

These are manual end-to-end validations, **not** a claim of comprehensive automated test coverage, compatibility with every GeoParquet file, or production hardening.

### Cross-plugin API

`GeoParquetImportService.loadRemote(...)` is the stable entry point for plugins such as
`josm-overturemaps`. It accepts an HTTPS URI, layer name, WGS84 viewport, progress
consumer and cancellation supplier. Reading runs off the Swing event thread; the
result is registered on that thread as a read-only `GeoParquetReferenceLayer`.

## Features

### Local GeoParquet

Use **File → Open…** and select a `.parquet` or `.geoparquet` file. The plugin decodes WKB geometries using Parquetry and displays them in a separate reference layer.

Supported geometry rendering includes points, lines, polygons, multi-geometries, and geometry collections. The current reader expects a top-level WKB column named `geometry` and longitude/latitude coordinates in EPSG:4326.

### Remote GeoParquet (S3 and HTTP-compatible storage)

1. Navigate JOSM to the area you want to inspect.
2. Select **File → Open remote GeoParquet URL…**, located near **Open…**.
3. Enter the HTTPS URL of a **single GeoParquet object**.
4. The plugin uses HTTP byte-range requests and a spatial filter based on the current viewport.
5. A read-only reference layer is added to JOSM.

For S3, provide the object's accessible **HTTPS URL**, not an `s3://` URI. Public endpoints and presigned HTTPS URLs are supported when the server implements HTTP Range (`206 Partial Content` and a valid `Content-Range`).

**After panning or zooming**, right-click the remote layer and select **Reload GeoParquet for current viewport** to fetch data for the new visible area. Viewport refresh is **manual** in this version.

The reader does not intentionally download the entire object. Actual network I/O and spatial pruning depend on the GeoParquet file's row-group/page organization and spatial metadata; an unoptimized file can still require substantial reads.

### Configurable geometry limits

Select **File → GeoParquet import settings…**, located near the bottom of the File menu before Exit.

- Set the maximum number of geometries to import.
- Use `0` to disable the count limit.
- Configure whether to keep a partial layer when the limit is reached.

The count limit is a safety control, not a memory quota. Unlimited imports or large geometry objects can exhaust the JVM heap.

### Per-layer appearance

Right-click a GeoParquet reference layer and select **GeoParquet style…** to adjust:

- Color
- Stroke width
- Opacity
- Point size

Styles are associated with each layer in memory; they are not currently persisted between JOSM sessions. Styling affects only rendering, not source geometries or OSM data.

### Read-only by design

GeoParquet content is displayed as a **reference layer**, not as an `OsmDataLayer`. The plugin does not create editable OSM primitives, perform conflation, modify the source GeoParquet, or upload reference geometries to OpenStreetMap.

## Requirements

- **JDK 25** (Parquetry uses Java preview features).
- **Maven 3.9+**, Git, curl, and make.
- JOSM tested JAR (downloaded by the bootstrap target).
- Parquetry `1.0-SNAPSHOT` (built locally by the bootstrap target).

## Build and run

```bash
java -version
mvn -version

make bootstrap   # First-time setup: download JOSM and build Parquetry
make build       # Build target/geoparquet.jar
make install     # Install plugin into JOSM's plugin directory
make run         # Launch JOSM with required JVM flags
```

`make bootstrap` is normally only needed once. After changing the plugin's source code, use:

```bash
make install
make run
```

On Linux, the default installation destination is:

```text
~/.local/share/JOSM/plugins/geoparquet.jar
```

Override with `make install JOSM_PLUGINS_DIR=/custom/JOSM/plugins` if necessary.

The `make run` target includes `--enable-preview` and the additional JVM exports required for this JOSM/Java combination. If you start JOSM another way, provide equivalent JVM options. Ensure the plugin is enabled in JOSM preferences.

## Quick verification

**Local:** Open a small GeoParquet with a WKB `geometry` column, confirm that its geometries appear in a dedicated layer, and change its color through the layer context menu.

**Remote:** Navigate to an area covered by a GeoParquet hosted on an HTTP Range-capable S3 endpoint, open its HTTPS URL, verify the reference geometries, pan to a nearby area, and use **Reload GeoParquet for current viewport**.

**Safety:** Confirm that GeoParquet geometries are not editable OSM primitives and are not included in OSM uploads.

## Current limitations

- Geometry column is currently expected to be named `geometry` and encoded as WKB.
- Coordinates are assumed to be **EPSG:4326**; automatic CRS discovery/reprojection is not implemented. Unknown-CRS files may render in the wrong location.
- Remote import supports a **single HTTP(S) object** with working byte-range support, not `s3://` URIs, directory/prefix datasets, or wildcard collections.
- AWS credential providers and SigV4 signing are not implemented. Use a public or appropriately presigned HTTPS URL; treat presigned URLs as sensitive.
- Remote viewport refresh is manual; no automatic map-movement listener, request cancellation, or persistent spatial cache yet.
- Styles are per layer and not persisted; attribute-based styling and an attribute inspector are not implemented.
- Large files may still require substantial I/O and heap, even with a viewport filter; spatial pruning depends on file layout and metadata.
- Automated integration tests, broader format compatibility, and production-scale performance evaluation remain future work.

## Architecture and documentation

- `GeoParquetPlugin` registers JOSM menu actions and the local importer.
- `GeoParquetImporter` handles local file selection.
- `GeoParquetReader` decodes GeoParquet geometries using Parquetry.
- `HttpRangeSource` provides positional HTTP access for remote objects.
- `GeoParquetReferenceLayer` renders the read-only layer and exposes layer actions.
- `GeoParquetImportService` is the public asynchronous handoff used by other plugins.
- `GeoParquetSettings`, `GeoParquetDialogs`, and `GeoParquetStyle` handle limits and visual configuration.

The plugin is independent of the JOSM OpenData plugin and may serve as a foundation for future Overture Maps integrations.

See [Architecture](docs/ARCHITECTURE.md), [Development](docs/DEVELOPMENT.md), and [Remote access and styling](docs/REMOTE_AND_STYLING.md).

## Roadmap

1. Automatically refresh remote data on viewport changes, with debounce and cancellation.
2. Cache spatial requests and optimize large-file rendering.
3. Support GeoParquet metadata-driven geometry column and CRS detection.
4. Add attribute-based styles and feature inspection.
5. Support cloud authentication and multi-object datasets.
6. Expand automated tests and performance benchmarks.
