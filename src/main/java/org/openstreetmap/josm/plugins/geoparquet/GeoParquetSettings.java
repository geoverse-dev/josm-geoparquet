package org.openstreetmap.josm.plugins.geoparquet;

import org.openstreetmap.josm.spi.preferences.Config;

/** Persistent plugin-wide import preferences. */
public final class GeoParquetSettings {
    private GeoParquetSettings() {}
    private static final String PREFIX = "geoparquet.";
    public static int maxFeatures() {
        return Math.max(0, Config.getPref().getInt(PREFIX + "maxFeatures", 1_000_000));
    }
    public static boolean keepPartial() {
        return Config.getPref().getBoolean(PREFIX + "keepPartial", true);
    }
    public static void save(int limit, boolean partial) {
        Config.getPref().putInt(PREFIX + "maxFeatures", Math.max(0, limit));
        Config.getPref().putBoolean(PREFIX + "keepPartial", partial);
    }
}
