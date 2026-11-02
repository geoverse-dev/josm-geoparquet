package org.openstreetmap.josm.plugins.geoparquet;

import java.awt.Component;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/** Registers the remote import beside File > Open, and settings before File > Exit. */
public final class GeoParquetPlugin extends Plugin {
    public GeoParquetPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addImporterFirst(new GeoParquetImporter());

        JMenuItem remote = new JMenuItem("Open remote GeoParquet URL...");
        remote.addActionListener(e -> GeoParquetRemoteAction.open());

        JMenuItem settings = new JMenuItem("GeoParquet import settings...");
        settings.addActionListener(e -> GeoParquetDialogs.settings());

        JMenu fileMenu = MainApplication.getMenu().fileMenu;
        int openIndex = findActionIndex(fileMenu, "OpenFileAction", "Open...");
        if (openIndex < 0) {
            openIndex = findActionIndex(fileMenu, "OpenFileAction", "Open");
        }
        if (openIndex >= 0) {
            fileMenu.insert(remote, openIndex + 1);
        } else {
            // If the JOSM menu differs, keep the action accessible before Exit.
            int exitIndex = findExitIndex(fileMenu);
            if (exitIndex >= 0) fileMenu.insert(remote, exitIndex);
            else fileMenu.add(remote);
        }

        // Resolve Exit again because inserting the remote item changes indices.
        int exitIndex = findExitIndex(fileMenu);
        if (exitIndex >= 0) {
            fileMenu.insert(settings, exitIndex);
            fileMenu.insertSeparator(exitIndex + 1);
        } else {
            fileMenu.addSeparator();
            fileMenu.add(settings);
        }
    }

    private static int findActionIndex(JMenu menu, String actionClass, String text) {
        Component[] components = menu.getMenuComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JMenuItem item
                    && item.getAction() != null
                    && actionClass.equals(item.getAction().getClass().getSimpleName())) {
                return i;
            }
        }
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JMenuItem item
                    && text.equalsIgnoreCase(item.getText())) {
                return i;
            }
        }
        return -1;
    }

    private static int findExitIndex(JMenu menu) {
        int index = findActionIndex(menu, "ExitAction", "Exit");
        return index >= 0 ? index : findActionIndex(menu, "ExitAction", "Quit");
    }
}
