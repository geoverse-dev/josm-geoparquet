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

The Maven POM uses a `system`-scoped JOSM JAR solely for local development and shades runtime dependencies into a single plugin JAR. Before release, replace the pinned baseline placeholder with the actual revision used for compilation and validate the `Plugin-Mainversion` manifest value against it.

**Verification required:** build against actual JOSM and Parquetry artifacts, run a manual import test, inspect plugin classloader collisions, and confirm Java 25 preview launch. This repository is a source scaffold, not a certified release.

## Design constraints

Keep the plugin read-only. Do not add any path that creates editable OSM primitives or makes the layer uploadable. Keep file reads local for the initial milestone.
