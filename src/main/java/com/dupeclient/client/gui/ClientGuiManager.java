package com.dupeclient.client.gui;

import com.dupeclient.client.gui.panel.UtilityPanel;
import com.dupeclient.client.gui.panel.DupedbPanel;
import com.dupeclient.client.gui.panel.HudPanel;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.gui.panel.PayAllPanel;
import com.dupeclient.client.gui.panel.McpToolsPanel;
import com.dupeclient.client.gui.panel.MacrosPanel;
import com.dupeclient.client.gui.panel.PacketUtilsPanel;
import com.dupeclient.client.gui.panel.AcAuditPanel;
import com.dupeclient.client.gui.panel.SecurityPanel;
import com.dupeclient.client.gui.panel.SocialPanel;
import com.dupeclient.client.gui.panel.WaypointsPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * Module registry for {@link ClientGuiScreen} + {@link com.dupeclient.client.gui.modern.HubShell}.
 */
public class ClientGuiManager {
    private final List<Panel> panels = new ArrayList<>();

    public void initializeDefaults() {
        if (!panels.isEmpty()) {
            return;
        }

        panels.add(new DupedbPanel(0, 0));
        panels.add(new PacketUtilsPanel(0, 0));
        panels.add(new PayAllPanel(0, 0));
        panels.add(new McpToolsPanel(0, 0));
        panels.add(new MacrosPanel(0, 0));
        panels.add(new HudPanel(0, 0));
        panels.add(new SocialPanel(0, 0));
        panels.add(new WaypointsPanel(0, 0));
        panels.add(new SecurityPanel(0, 0));
        panels.add(new AcAuditPanel(0, 0));
        panels.add(new UtilityPanel(0, 0));

        for (Panel panel : panels) {
            panel.setDraggable(false);
        }
    }

    public List<Panel> getPanels() {
        return panels;
    }

    public boolean hasFocusedTextInput() {
        return panels.stream().anyMatch(Panel::hasFocusedTextInput);
    }
}
