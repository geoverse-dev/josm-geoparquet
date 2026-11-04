# JOSM GeoParquet

[English version](README.md)

Plugin funcional de JOSM para visualizar GeoParquet como capas de referencia de
solo lectura, basado en [Parquetry](https://github.com/tileverse-io/parquetry).

Permite abrir archivos locales o cargar un objeto GeoParquet desde un endpoint
HTTP(S), incluidos buckets S3 públicos o almacenamiento compatible. Las geometrías
se muestran junto a los datos editables de OpenStreetMap, pero no se convierten en
objetos OSM.

## Estado

**Versión 0.3 — API pública de importación entre plugins y flujos locales/remotos
validados en JOSM.**

Se verificaron los siguientes flujos:

- Apertura y visualización de datasets GeoParquet locales.
- Visualización de geometrías de transporte de Overture Maps junto a datos OSM.
- Lectura de un objeto GeoParquet publicado en S3 mediante HTTPS.
- Uso del viewport actual de JOSM como consulta espacial para cargas remotas.
- Límites de geometrías y personalización visual de las capas de referencia.

Estas validaciones manuales de punta a punta complementan las pruebas automatizadas.
Las limitaciones de formato y escala se documentan más abajo.

### API entre plugins

`GeoParquetImportService.loadRemote(...)` es el punto de entrada estable para
plugins como `josm-overturemaps`. Recibe una URI HTTPS, nombre de capa, viewport
WGS84, progreso y cancelación. La lectura se ejecuta fuera del hilo de Swing y el
resultado se registra como `GeoParquetReferenceLayer` de solo lectura.

## Funcionalidades

### GeoParquet local

Usar **Archivo → Abrir…** y seleccionar un archivo `.parquet` o `.geoparquet`. El
plugin decodifica geometrías WKB mediante Parquetry y crea una capa de referencia.

Se renderizan puntos, líneas, polígonos, multigeometrías y colecciones. Actualmente
se espera una columna WKB de primer nivel llamada `geometry` con coordenadas
longitud/latitud en EPSG:4326.

### GeoParquet remoto

1. Ubicar JOSM en el área que se desea inspeccionar.
2. Seleccionar **Archivo → Open remote GeoParquet URL…**.
3. Ingresar la URL HTTPS de un único objeto GeoParquet.
4. El plugin usa solicitudes HTTP Range y el viewport actual como filtro espacial.
5. JOSM agrega una capa de referencia de solo lectura.

Para S3 se debe usar la URL HTTPS accesible del objeto, no una URI `s3://`. Se
admiten endpoints públicos y URLs prefirmadas cuando el servidor responde con Range
(`206 Partial Content` y un `Content-Range` válido).

Después de mover o ampliar el mapa, usar **Reload GeoParquet for current viewport**
en el menú contextual de la capa. La actualización del viewport es manual.

### Límites y apariencia

En **Archivo → GeoParquet import settings…** se configura el máximo de geometrías y
si se conserva una capa parcial al alcanzar el límite. El valor `0` desactiva el
límite y puede agotar la memoria de la JVM.

Desde **GeoParquet style…** en el menú contextual de la capa se pueden cambiar
color, ancho de línea, opacidad y tamaño de punto. El estilo es visual, pertenece a
la capa en memoria y no modifica los datos de origen ni los datos OSM.

### Solo lectura por diseño

El contenido se muestra en una capa de referencia, no en un `OsmDataLayer`. El
plugin no crea primitivas OSM editables, no hace conflación, no modifica el archivo
GeoParquet y no sube esas geometrías a OpenStreetMap.

## Requisitos

- JDK 25, porque Parquetry utiliza funcionalidades preview de Java.
- Maven 3.9+, Git, curl y make para compilar.
- JOSM tested y Parquetry `1.0-SNAPSHOT`, preparados por `make bootstrap`.

## Instalar una release publicada

Los JAR se publican en la página pública de
[GitHub Releases](https://github.com/geoverse-dev/josm-geoparquet/releases).
Descargar `geoparquet-0.3.0.jar`, renombrarlo como `geoparquet.jar`, copiarlo a la
carpeta de plugins de JOSM y habilitarlo en sus preferencias. JOSM debe iniciarse
con Java 25 y las funcionalidades preview activadas. No se requieren una cuenta de
GitHub ni credenciales.

Para la instalación autocontenida recomendada, ejecutar el instalador incluido en
este repositorio. Descarga JOSM y GeoParquet, verifica el checksum de la release y
usa un perfil aislado:

```bash
JAVA_HOME=/ruta/al/jdk-25 ./scripts/install-and-run.sh
```

Consultar la [guía de instalación](docs/INSTALLATION.es.md) o su
[versión en inglés](docs/INSTALLATION.md).

## Compilar y ejecutar

```bash
make bootstrap
make build
make install
make run
```

Después de modificar el código normalmente alcanza con:

```bash
make install
make run
```

En Linux, la instalación predeterminada es
`~/.local/share/JOSM/plugins/geoparquet.jar`. Puede cambiarse con
`make install JOSM_PLUGINS_DIR=/ruta/JOSM/plugins`.

## Releases

Al pushear un tag semántico como `v0.3.0`, GitHub Actions ejecuta las pruebas con
Java 25, empaqueta el plugin, valida la versión del manifiesto y publica
`geoparquet-0.3.0.jar` junto con su SHA-256.

## Limitaciones actuales

- La columna geométrica debe llamarse `geometry` y estar codificada como WKB.
- Se asume EPSG:4326; no hay detección ni reproyección automática de CRS.
- La carga remota admite un único objeto HTTP(S) con byte ranges, no URIs `s3://`,
  prefijos, directorios o comodines.
- No se implementan credenciales AWS ni firma SigV4. Las URLs prefirmadas deben
  tratarse como información sensible.
- La recarga por movimiento del viewport es manual.
- Los estilos no persisten entre sesiones y todavía no hay estilos por atributos.
- En archivos grandes, el I/O y la memoria dependen del layout y los metadatos
  espaciales; conviene usar límites de geometrías.

## Arquitectura y documentación

El plugin expone `GeoParquetImportService`, que también utiliza JOSM Overture Maps,
y mantiene toda la lectura y renderización separada de los objetos OSM editables.

Consultar [Instalación](docs/INSTALLATION.es.md),
[Arquitectura](docs/ARCHITECTURE.md),
[Desarrollo](docs/DEVELOPMENT.md) y
[Acceso remoto y estilos](docs/REMOTE_AND_STYLING.md). La documentación técnica se
mantiene en inglés; este README ofrece la guía de usuario equivalente en español.

## Próximas mejoras

1. Recarga automática por cambios del viewport con debounce y cancelación.
2. Caché espacial y renderización optimizada para datasets grandes.
3. Detección de columna geométrica y CRS desde metadatos GeoParquet.
4. Inspección de atributos y estilos basados en atributos.
5. Autenticación cloud y datasets compuestos por varios objetos.
6. Más pruebas de integración y rendimiento.
