package com.dupeclient.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Short-lived HUD toasts for DupeClient actions. */
public final class DupeClientToasts {
    private DupeClientToasts() {
    }

    public static void show(String title, String body) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String safeTitle = title == null || title.isBlank() ? "DupeClient" : title.trim();
        String safeBody = body == null ? "" : body.trim();
        if (safeBody.length() > 120) {
            safeBody = safeBody.substring(0, 117) + "…";
        }
        String finalBody = safeBody;
        client.execute(() -> SystemToast.add(
                client.gui.toastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(safeTitle),
                Component.literal(finalBody)));
    }
}
