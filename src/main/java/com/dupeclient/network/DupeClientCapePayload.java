package com.dupeclient.network;

import com.dupeclient.DupeConstants;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Announces that a player is using DupeClient so other clients can show the shared cape texture.
 * Relayed by the server when all participants have this mod installed (e.g. integrated / LAN / modded server).
 */
public record DupeClientCapePayload(UUID playerId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DupeClientCapePayload> PAYLOAD_ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DupeConstants.MOD_ID, "cape_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DupeClientCapePayload> CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, DupeClientCapePayload::playerId, DupeClientCapePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PAYLOAD_ID;
    }
}
