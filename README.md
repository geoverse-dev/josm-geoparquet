# JOSM GeoParquet

Experimental, read-only local GeoParquet visualization plugin for JOSM, powered by [Parquetry](https://github.com/tileverse-io/parquetry).

**Status: initial proof of concept. Not yet compiled or runtime-tested against JOSM.**

## Scope

- Open local `.parquet` / `.geoparquet` files through **File → Open**.
- Decode WKB geometry from a column named `geometry` using Parquetry `ParquetFileReader`.
- Draw points, lines, polygons, multi-geometries, and collections in a **dedicated reference layer**.
- No OSM primitive creation, editing, upload, remote access, conflation, or file modification.
- Import up to 100,000 geometries by default; reject larger files rather than silently truncating.

## Requirements

- **JDK 25** and `--enable-preview` at runtime (required by Parquetry).
- **Maven 3.9+**, `make`, and `curl`.
- Parquetry `1.0-SNAPSHOT` available in local Maven cache or configured snapshot repository.
- A JOSM tested JAR for compilation.

## Build

Requirements: JDK 25 (including `JAVA_HOME`), Git, curl, and Maven. Parquetry's Maven wrapper is used to install its snapshot dependencies locally.

```bash
java -version                 # Must report Java 25
mvn -version                  # Must use Java 25
make bootstrap                # Downloads JOSM; clones and builds Parquetry locally
make build                    # Creates target/geoparquet.jar
make inspect                  # Check manifest metadata
```

`make bootstrap` reuses `.cache/josm-tested.jar` and `.cache/parquetry` on subsequent runs, but reruns the Parquetry Maven install. Override `PARQUETRY_DIR` to use an existing Parquetry checkout. The bootstrap requires network access the first time.

## Install (Linux)

```bash
make install
```

By default this copies `target/geoparquet.jar` to `~/.local/share/JOSM/plugins/geoparquet.jar`.
To override: `make install JOSM_PLUGINS_DIR=/custom/JOSM/plugins`.

**Start JOSM with preview features enabled**, e.g.:

```bash
java --enable-preview -jar .cache/josm-tested.jar
```

Ensure the `geoparquet` plugin is enabled in JOSM preferences; restart JOSM after installing.

## Manual smoke test

1. Launch JOSM with JDK 25 and `--enable-preview`.
2. Choose **File → Open**.
3. Select a small local GeoParquet file with EPSG:4326 WKB `geometry` column.
4. Confirm that a separate layer named after the file appears.
5. Zoom to the layer and confirm its shapes render over the OSM background.
6. Confirm that the layer cannot be edited, merged into OSM data, saved as OSM data, or uploaded.
7. Try an invalid `.parquet` file; confirm an error and no partial layer.

## Current limitations

- **Only** WKB in a top-level column named `geometry` is supported.
- Coordinates are assumed to be longitude/latitude **EPSG:4326**. Files using other CRSs must be rejected or reprojected in a future iteration; currently they may render incorrectly. **Do not use this PoC for positional validation of unknown-CRS data.**
- No BBOX pushdown yet, no metadata-driven geometry selection, no remote access, no styling or attribute inspector.
- A memory cap on feature count is not a byte cap; use small local files initially.
- JOSM and Parquetry APIs may change; compilation/runtime verification is pending.

## Architecture

`GeoParquetPlugin` registers `GeoParquetImporter` with JOSM. `GeoParquetReader` reads WKB using Parquetry. `GeoParquetReferenceLayer` paints geometry directly without producing `OsmDataLayer`.

This plugin has **no dependency on the JOSM OpenData plugin**. A future Overture Maps plugin may reuse its reader and layer APIs.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
