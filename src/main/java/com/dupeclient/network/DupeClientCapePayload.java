package com.dupeclient.network;

import com.dupeclient.DupeConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Announces that a player is using DupeClient so other clients can show the shared cape texture.
 * Relayed by the server when all participants have this mod installed (e.g. integrated / LAN / modded server).
 */
public record DupeClientCapePayload(UUID playerId) implements CustomPayload {
    public static final CustomPayload.Id<DupeClientCapePayload> PAYLOAD_ID =
            new CustomPayload.Id<>(Identifier.of(DupeConstants.MOD_ID, "cape_sync"));
    public static final PacketCodec<RegistryByteBuf, DupeClientCapePayload> CODEC =
            PacketCodec.tuple(Uuids.PACKET_CODEC, DupeClientCapePayload::playerId, DupeClientCapePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
