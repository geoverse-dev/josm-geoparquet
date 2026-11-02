package org.openstreetmap.josm.plugins.geoparquet;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/** Installs a read-only local GeoParquet file importer. */
public final class GeoParquetPlugin extends Plugin {
    public GeoParquetPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addImporterFirst(new GeoParquetImporter());
    }
}
