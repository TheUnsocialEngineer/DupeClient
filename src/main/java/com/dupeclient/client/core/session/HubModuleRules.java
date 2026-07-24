package com.dupeclient.client.core.session;

import com.dupeclient.client.docs.ScreenshotCaptureMode;

public final class HubModuleRules {
    private HubModuleRules() {
    }

    public static boolean viewerRestricted() {
        return PresenceRosterSync.viewerRestricted();
    }

    private static boolean featuresUnlocked() {
        if (ScreenshotCaptureMode.isActive()) {
            return true;
        }
        return PresenceRosterSync.sessionRosterVerified() && !viewerRestricted();
    }

    public static boolean exploitFeaturesAllowed() {
        return featuresUnlocked();
    }

    public static boolean socialFeaturesAllowed() {
        return featuresUnlocked();
    }

    public static boolean panelAllowed(String panelId) {
        if (panelId == null) {
            return false;
        }
        return switch (panelId) {
            case "hud", "security" -> true;
            default -> featuresUnlocked();
        };
    }

    public static String blockReason() {
        if (PresenceRosterSync.isResponseTampered()) {
            return "Access restricted (roster tamper detected)";
        }
        if (!PresenceRosterSync.sessionRosterVerified()) {
            return "Waiting for roster verification — " + PresenceRosterSync.statusLine();
        }
        if (viewerRestricted()) {
            return "Access restricted (staff account)";
        }
        return "Access restricted";
    }

    public static int firstAllowedPanelIndex(java.util.List<com.dupeclient.client.gui.panel.Panel> panels) {
        for (int i = 0; i < panels.size(); i++) {
            if (panelAllowed(panels.get(i).getId())) {
                return i;
            }
        }
        return 0;
    }
}
