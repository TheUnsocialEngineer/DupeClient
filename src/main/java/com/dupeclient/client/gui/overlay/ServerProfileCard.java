package com.dupeclient.client.gui.overlay;

import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.dupedb.P2wMarkManager;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ServerProfileCard {
    private static final long AUTO_HIDE_MS = 12_000L;
    private static final List<String> LINES = new ArrayList<>();
    private static long shownAtMs;
    private static boolean visible;

    private ServerProfileCard() {
    }

    public static void showOnJoin() {
        rebuild();
        if (LINES.isEmpty()) {
            visible = false;
            return;
        }
        shownAtMs = System.currentTimeMillis();
        visible = true;
    }

    public static void dismiss() {
        visible = false;
    }

    public static void rebuild() {
        LINES.clear();
        String host = DupedbManager.INSTANCE.currentServerHost();
        if (host.isBlank() || "singleplayer".equals(host)) {
            return;
        }
        LINES.add(host);
        String p2w = P2wServerPolicy.INSTANCE.registryStatusForServer(host);
        LINES.add("P2W: " + (p2w == null || p2w.isBlank() ? "unknown" : p2w));
        if (DupedbManager.INSTANCE.hasRecentScanForServer(host)) {
            LINES.add("DupeDB: " + DupedbManager.INSTANCE.getDiscoveredPluginCount() + " plugins (recent scan)");
        } else {
            LINES.add("DupeDB: no recent scan");
        }
        String brand = AcAuditManager.INSTANCE.getMetrics().brand;
        if (brand != null && !brand.isBlank()) {
            LINES.add("Brand: " + brand);
        }
        long mins = P2wMarkManager.sessionDurationMs(host) / 60_000L;
        if (mins > 0) {
            LINES.add("Session: " + mins + " min");
        }
    }

    public static void render(GuiGraphics context, Font tr) {
        if (!visible || LINES.isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() - shownAtMs > AUTO_HIDE_MS) {
            visible = false;
            return;
        }
        int x = 8;
        int y = 48;
        int w = 230;
        int h = 12 + LINES.size() * 10;
        context.fill(x - 2, y - 2, x + w, y + h, 0xAA000000);
        int lineY = y;
        for (int i = 0; i < LINES.size(); i++) {
            ChatFormatting fmt = i == 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY;
            context.drawString(tr, Component.literal(LINES.get(i)).withStyle(fmt), x, lineY, 0xFFFFFF);
            lineY += 10;
        }
    }
}
