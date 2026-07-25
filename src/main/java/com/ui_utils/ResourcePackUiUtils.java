package com.ui_utils;

import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

import java.util.UUID;

/** Client-side resource pack bypass / force-deny for UI Utils multiplayer toggles. */
public final class ResourcePackUiUtils {
    private ResourcePackUiUtils() {
    }

    public enum Action {
        NONE,
        BYPASSED,
        DECLINED
    }

    public static Action actionFor(ResourcePackSendS2CPacket packet) {
        if (SharedVariables.resourcePackForceDeny) {
            return Action.DECLINED;
        }
        if (SharedVariables.bypassResourcePack && packet.required()) {
            return Action.BYPASSED;
        }
        return Action.NONE;
    }

    public static ResourcePackStatusC2SPacket statusPacket(UUID packId, ResourcePackStatusC2SPacket.Status status) {
        return new ResourcePackStatusC2SPacket(packId, status);
    }
}
