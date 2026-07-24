package com.dupeclient.client.module.hud;

public record HudElementDefinition(
        String id,
        String displayName,
        int defaultX,
        int defaultY,
        HudTextProvider textProvider
) {
}
