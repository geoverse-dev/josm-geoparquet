# Installing JOSM GeoParquet

The repository includes a self-contained installer for end users. It downloads
JOSM tested and the public GeoParquet release, verifies its checksum, and starts
JOSM with an isolated profile. It does not require a GitHub account, Git, Maven, or
local compilation.

## Requirements

- Linux or macOS with Bash.
- JDK 25.
- `curl`, `unzip`, and `sha256sum`.
- Internet access for the first run.

## Install and run

Clone or download this repository and run from its root:

```bash
JAVA_HOME=/path/to/jdk-25 ./scripts/install-and-run.sh
```

The script creates `./josm-geoparquet-bundle/` containing JOSM,
`geoparquet.jar`, preferences, user data, and cache. Later runs reuse the
downloads. The profile is isolated and does not modify regular JOSM preferences.

## Options

```bash
# Install and verify without opening JOSM
./scripts/install-and-run.sh --prepare-only

# Choose another directory
./scripts/install-and-run.sh --dir /path/to/my-josm

# Download JOSM and the plugin again
./scripts/install-and-run.sh --refresh
```

Select another release with:

```bash
GEOPARQUET_VERSION=0.3.0 ./scripts/install-and-run.sh
```

Run `./scripts/install-and-run.sh --help` for every option.

## Verify in JOSM

1. Confirm that GeoParquet is enabled under **Preferences → Plugins**.
2. Use **File → Open…** for a local `.parquet` or `.geoparquet` file.
3. For a remote object, use **Open remote GeoParquet URL…** with an HTTPS URL
   that supports HTTP Range.
4. Confirm that the result is a read-only reference layer.

## Manual installation

Alternatively, download the JAR from
[GitHub Releases](https://github.com/geoverse-dev/josm-geoparquet/releases), rename
it to `geoparquet.jar`, and copy it into the JOSM plugins directory. JOSM must
still run with Java 25, `--enable-preview`, and the JVM exports used by the script.
