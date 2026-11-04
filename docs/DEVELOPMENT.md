# Development

## Build prerequisites

JDK 25 with preview features, Maven, curl and a locally installed Parquetry snapshot. The `Makefile` fetches a JOSM tested JAR to `.cache/`, clones and locally installs Parquetry, and builds a shaded plugin JAR.

## Commands

| Command | Purpose |
| --- | --- |
| `make bootstrap` | Download JOSM and install Parquetry locally |
| `make build` | Compile and package |
| `make test` | Run Maven tests |
| `make install` | Install in user JOSM plugin directory |
| `make run` | Launch JOSM with preview enabled |
| `make inspect` | Print plugin manifest |
| `make clean` | Remove build output |

## Compatibility notes

The Maven POM uses a `system`-scoped JOSM JAR solely for local development and shades
runtime dependencies into a single plugin JAR. Release builds validate that the
manifest version matches the Git tag and publish the plugin JAR with its SHA-256 file.

Before publishing a release, build and test against the configured JOSM and Parquetry
artifacts, validate local and remote imports, inspect plugin classloader compatibility,
and confirm the Java 25 preview launch. Published versions are usable plugins; the
documented format and scaling limitations still apply.

## Design constraints

Keep the plugin read-only. Do not add any path that creates editable OSM primitives
or makes the layer uploadable. Remote reads must preserve the existing HTTPS,
HTTP-Range, viewport-filtering and resource-limit safeguards.
