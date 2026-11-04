# Instalación de JOSM GeoParquet

El repositorio incluye un instalador autocontenido para usuarios finales. Descarga
JOSM tested y la release pública de GeoParquet, valida su checksum y ejecuta JOSM
con un perfil aislado. No requiere una cuenta de GitHub, Git, Maven ni compilar.

## Requisitos

- Linux o macOS con Bash.
- JDK 25.
- `curl`, `unzip` y `sha256sum`.
- Conexión a Internet durante la primera ejecución.

## Instalar y ejecutar

Clonar o descargar este repositorio y, desde su raíz, ejecutar:

```bash
JAVA_HOME=/ruta/al/jdk-25 ./scripts/install-and-run.sh
```

Se crea `./josm-geoparquet-bundle/` con JOSM, `geoparquet.jar`, preferencias,
datos y caché. Las ejecuciones siguientes reutilizan las descargas.

El perfil es independiente de cualquier instalación previa de JOSM y no modifica
sus preferencias habituales.

## Opciones

```bash
# Instalar y verificar sin abrir JOSM
./scripts/install-and-run.sh --prepare-only

# Elegir otra carpeta
./scripts/install-and-run.sh --dir /ruta/a/mi-josm

# Volver a descargar JOSM y el plugin
./scripts/install-and-run.sh --refresh
```

Para seleccionar otra release:

```bash
GEOPARQUET_VERSION=0.3.0 ./scripts/install-and-run.sh
```

Consultar `./scripts/install-and-run.sh --help` para ver todas las opciones.

## Verificación en JOSM

1. Confirmar que GeoParquet aparece habilitado en **Preferencias → Plugins**.
2. Usar **Archivo → Abrir…** para un archivo `.parquet` o `.geoparquet` local.
3. Para un objeto remoto, usar **Open remote GeoParquet URL…** con una URL HTTPS
   que soporte HTTP Range.
4. Verificar que el resultado sea una capa de referencia de solo lectura.

## Instalación manual

También se puede descargar el JAR desde
[GitHub Releases](https://github.com/geoverse-dev/josm-geoparquet/releases),
renombrarlo como `geoparquet.jar` y copiarlo en la carpeta de plugins de JOSM. En
ese caso, JOSM igualmente debe iniciarse con Java 25, `--enable-preview` y los
exports de JVM usados por el script.
