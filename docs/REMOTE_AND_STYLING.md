# GeoParquet 0.2: import limits, styling, and remote access

## Scope

Local imports retain the original read-only geometry layer behavior. The maximum geometry count is now a JOSM preference, with a configurable keep-partial mode. A limit of 0 means unlimited and can exhaust JVM heap.

Layer styling uses a per-layer mutable style object (color, opacity, stroke width, point size). Styling is visual only; there is no feature editing or OSM upload.

Remote imports require a single HTTP(S) GeoParquet object with working HTTP Range support and a known Content-Range size. The initial viewport is captured at import time and passed to Parquetry as a spatial predicate. Geometry envelopes are checked after decoding. The current implementation uses a manual context-menu reload after panning.

## Next milestones

1. Compile and smoke-test with JDK 25 and the user's JOSM tested JAR.
2. Add automatic viewport-change listening with debounce, cancellation and stale-result protection.
3. Introduce spatial indexing and viewport-aware caching; avoid full in-memory scans for rendering.
4. Support metadata-driven geometry columns and CRS checks, and attribute-driven style rules.
5. Add remote dataset manifests, cloud authentication and optional signed URL provider.
6. Improve HTTP client connection pooling, retry/backoff and range caching.
7. Add unit/integration tests using a local HTTP Range server and a small GeoParquet fixture.

## Known limitations

The remote implementation is an initial implementation, not a verified production client. Server-side Range support is mandatory; no fallback to full-file download is attempted. Parquetry predicate pruning effectiveness depends on spatial metadata and layout.
