package com.ui_utils;

import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

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

    public static Action actionFor(ClientboundResourcePackPushPacket packet) {
        if (SharedVariables.resourcePackForceDeny) {
            return Action.DECLINED;
        }
        if (SharedVariables.bypassResourcePack && packet.required()) {
            return Action.BYPASSED;
        }
        return Action.NONE;
    }

    public static ServerboundResourcePackPacket statusPacket(UUID packId, ServerboundResourcePackPacket.Action action) {
        return new ServerboundResourcePackPacket(packId, action);
    }
}
