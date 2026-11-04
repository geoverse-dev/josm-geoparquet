# GeoParquet 0.2: import limits, styling, and remote access

## Scope

Local imports retain the original read-only geometry layer behavior. The maximum geometry count is now a JOSM preference, with a configurable keep-partial mode. A limit of 0 means unlimited and can exhaust JVM heap.

Layer styling uses a per-layer mutable style object (color, opacity, stroke width, point size). Styling is visual only; there is no feature editing or OSM upload.

Remote imports require a single HTTP(S) GeoParquet object with working HTTP Range support and a known Content-Range size. The initial viewport is captured at import time and passed to Parquetry as a spatial predicate. Geometry envelopes are checked after decoding. The current implementation uses a manual context-menu reload after panning.

## Further improvements

1. Add automatic viewport-change listening with debounce, cancellation and stale-result protection.
2. Introduce spatial indexing and viewport-aware caching; avoid full in-memory scans for rendering.
3. Support metadata-driven geometry columns and CRS checks, and attribute-driven style rules.
4. Add remote dataset manifests, cloud authentication and optional signed URL provider.
5. Improve HTTP client connection pooling, retry/backoff and range caching.
6. Expand integration coverage using a local HTTP Range server and GeoParquet fixtures.

## Known limitations

Remote loading is available for single HTTP(S) GeoParquet objects. Server-side Range
support is mandatory and there is intentionally no fallback to downloading the full
file. Parquetry predicate-pruning effectiveness depends on spatial metadata and file
layout; users should apply geometry limits for large or unfamiliar datasets.

## Plugin integration

The public `GeoParquetImportService` preserves the same Range-based, viewport-filtered
reader for dependent plugins. It adds asynchronous progress/cancellation and enforces
the read-only layer boundary; callers do not receive Parquetry or mutable OSM objects.
