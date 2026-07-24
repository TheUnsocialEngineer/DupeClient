package com.dupeclient.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/** Short-lived HUD toasts for DupeClient actions. */
public final class DupeClientToasts {
    private DupeClientToasts() {
    }

    public static void show(String title, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        String safeTitle = title == null || title.isBlank() ? "DupeClient" : title.trim();
        String safeBody = body == null ? "" : body.trim();
        if (safeBody.length() > 120) {
            safeBody = safeBody.substring(0, 117) + "…";
        }
        String finalBody = safeBody;
        client.execute(() -> client.getToastManager().add(
                SystemToast.create(
                        client,
                        SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.literal(safeTitle),
                        Text.literal(finalBody))));
    }
}
