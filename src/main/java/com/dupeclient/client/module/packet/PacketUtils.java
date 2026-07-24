package com.dupeclient.client.module.packet;

import net.minecraft.network.packet.Packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Vanilla {@link Packet} class registry for C2S/S2C lookups. Names match Meteor-style conventions
 * (including nested types like {@code PlayerMoveC2SPacket.Full}).
 */
public final class PacketUtils {
    private static final Map<Class<? extends Packet<?>>, String> S2C_PACKETS = new HashMap<>();
    private static final Map<Class<? extends Packet<?>>, String> C2S_PACKETS = new HashMap<>();
    private static final Map<String, Class<? extends Packet<?>>> S2C_PACKETS_R = new HashMap<>();
    private static final Map<String, Class<? extends Packet<?>>> C2S_PACKETS_R = new HashMap<>();

    public static final Set<Class<? extends Packet<?>>> PACKETS;

    static {
        putC2s(net.minecraft.network.packet.c2s.config.AcceptCodeOfConductC2SPacket.class, "AcceptCodeOfConductC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket.class, "AcknowledgeChunksC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.AcknowledgeReconfigurationC2SPacket.class, "AcknowledgeReconfigurationC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket.class, "AdvancementTabC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.BoatPaddleStateC2SPacket.class, "BoatPaddleStateC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket.class, "BookUpdateC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket.class, "BundleItemSelectedC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket.class, "ButtonClickC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ChangeGameModeC2SPacket.class, "ChangeGameModeC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket.class, "ChatCommandSignedC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket.class, "ChatMessageC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket.class, "ClickSlotC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class, "ClientCommandC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket.class, "ClientOptionsC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.class, "ClientStatusC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket.class, "ClientTickEndC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket.class, "CloseHandledScreenC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket.class, "CommandExecutionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.CommonPongC2SPacket.class, "CommonPongC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket.class, "CookieResponseC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket.class, "CraftRequestC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket.class, "CreativeInventoryActionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket.class, "CustomClickActionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket.class, "CustomPayloadC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.DebugSubscriptionRequestC2SPacket.class, "DebugSubscriptionRequestC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket.class, "EnterConfigurationC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.HandSwingC2SPacket.class, "HandSwingC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket.class, "HandshakeC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket.class, "JigsawGeneratingC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket.class, "KeepAliveC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket.class, "LoginHelloC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket.class, "LoginKeyC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket.class, "LoginQueryResponseC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.MessageAcknowledgmentC2SPacket.class, "MessageAcknowledgmentC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket.class, "PickItemFromBlockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket.class, "PickItemFromEntityC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class, "PlayerActionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket.class, "PlayerInputC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket.class, "PlayerInteractBlockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.class, "PlayerInteractEntityC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket.class, "PlayerInteractItemC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerLoadedC2SPacket.class, "PlayerLoadedC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class, "PlayerMoveC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full.class, "PlayerMoveC2SPacket.Full");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround.class, "PlayerMoveC2SPacket.LookAndOnGround");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly.class, "PlayerMoveC2SPacket.OnGroundOnly");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround.class, "PlayerMoveC2SPacket.PositionAndOnGround");
        putC2s(net.minecraft.network.packet.c2s.play.PlayerSessionC2SPacket.class, "PlayerSessionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket.class, "QueryBlockNbtC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket.class, "QueryEntityNbtC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.query.QueryPingC2SPacket.class, "QueryPingC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket.class, "QueryRequestC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.config.ReadyC2SPacket.class, "ReadyC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.RecipeBookDataC2SPacket.class, "RecipeBookDataC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket.class, "RecipeCategoryOptionsC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.RenameItemC2SPacket.class, "RenameItemC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket.class, "RequestCommandCompletionsC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.class, "ResourcePackStatusC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket.class, "SelectKnownPacksC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket.class, "SelectMerchantTradeC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.SetTestBlockC2SPacket.class, "SetTestBlockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket.class, "SlotChangedStateC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket.class, "SpectatorTeleportC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket.class, "TeleportConfirmC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.TestInstanceBlockActionC2SPacket.class, "TestInstanceBlockActionC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket.class, "UpdateBeaconC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket.class, "UpdateCommandBlockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateCommandBlockMinecartC2SPacket.class, "UpdateCommandBlockMinecartC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateDifficultyC2SPacket.class, "UpdateDifficultyC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateDifficultyLockC2SPacket.class, "UpdateDifficultyLockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateJigsawC2SPacket.class, "UpdateJigsawC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket.class, "UpdatePlayerAbilitiesC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket.class, "UpdateSelectedSlotC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket.class, "UpdateSignC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket.class, "UpdateStructureBlockC2SPacket");
        putC2s(net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket.class, "VehicleMoveC2SPacket");

        putS2c(net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket.class, "AdvancementUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket.class, "BlockBreakingProgressS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.class, "BlockEntityUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BlockEventS2CPacket.class, "BlockEventS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket.class, "BlockUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BlockValueDebugS2CPacket.class, "BlockValueDebugS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.BossBarS2CPacket.class, "BossBarS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket.class, "ChatMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChatSuggestionsS2CPacket.class, "ChatSuggestionsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkBiomeDataS2CPacket.class, "ChunkBiomeDataS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket.class, "ChunkDataS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket.class, "ChunkDeltaUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket.class, "ChunkLoadDistanceS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket.class, "ChunkRenderDistanceCenterS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkSentS2CPacket.class, "ChunkSentS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ChunkValueDebugS2CPacket.class, "ChunkValueDebugS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ClearDialogS2CPacket.class, "ClearDialogS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket.class, "ClearTitleS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket.class, "CloseScreenS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.CodeOfConductS2CPacket.class, "CodeOfConductS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket.class, "CommandSuggestionsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket.class, "CommandTreeS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.CommonPingS2CPacket.class, "CommonPingS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket.class, "CookieRequestS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket.class, "CooldownUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.CraftFailedResponseS2CPacket.class, "CraftFailedResponseS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket.class, "CustomPayloadS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.CustomReportDetailsS2CPacket.class, "CustomReportDetailsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket.class, "DamageTiltS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket.class, "DeathMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.DebugSampleS2CPacket.class, "DebugSampleS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.DifficultyS2CPacket.class, "DifficultyS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.DisconnectS2CPacket.class, "DisconnectS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket.class, "DynamicRegistriesS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EndCombatS2CPacket.class, "EndCombatS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EnterCombatS2CPacket.class, "EnterCombatS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket.class, "EnterReconfigurationS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket.class, "EntitiesDestroyS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket.class, "EntityAnimationS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket.class, "EntityAttachS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.class, "EntityAttributesS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket.class, "EntityDamageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket.class, "EntityEquipmentUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket.class, "EntityPassengersSetS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket.class, "EntityPositionS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket.class, "EntityPositionSyncS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityS2CPacket.class, "EntityS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityS2CPacket.MoveRelative.class, "EntityS2CPacket.MoveRelative");
        putS2c(net.minecraft.network.packet.s2c.play.EntityS2CPacket.Rotate.class, "EntityS2CPacket.Rotate");
        putS2c(net.minecraft.network.packet.s2c.play.EntityS2CPacket.RotateAndMoveRelative.class, "EntityS2CPacket.RotateAndMoveRelative");
        putS2c(net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket.class, "EntitySetHeadYawS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket.class, "EntitySpawnS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket.class, "EntityStatusEffectS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket.class, "EntityStatusS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket.class, "EntityTrackerUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityValueDebugS2CPacket.class, "EntityValueDebugS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket.class, "EntityVelocityUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.EventDebugS2CPacket.class, "EventDebugS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket.class, "ExperienceBarUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket.class, "ExplosionS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.FeaturesS2CPacket.class, "FeaturesS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.GameJoinS2CPacket.class, "GameJoinS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.GameMessageS2CPacket.class, "GameMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket.class, "GameStateChangeS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.GameTestHighlightPosS2CPacket.class, "GameTestHighlightPosS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket.class, "HealthUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.InventoryS2CPacket.class, "InventoryS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket.class, "ItemPickupAnimationS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket.class, "KeepAliveS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket.class, "LightUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket.class, "LoginCompressionS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket.class, "LoginDisconnectS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket.class, "LoginHelloS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket.class, "LoginQueryRequestS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket.class, "LoginSuccessS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.LookAtS2CPacket.class, "LookAtS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket.class, "MapUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.MoveMinecartAlongTrackS2CPacket.class, "MoveMinecartAlongTrackS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.NbtQueryResponseS2CPacket.class, "NbtQueryResponseS2CPacket");
        putS2cIfClassPresent("net.minecraft.network.packet.s2c.play.OpenMountScreenS2CPacket", "OpenMountScreenS2CPacket");
        putS2cIfClassPresent("net.minecraft.network.packet.s2c.play.OpenHorseScreenS2CPacket", "OpenMountScreenS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket.class, "OpenScreenS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket.class, "OpenWrittenBookS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket.class, "OverlayMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ParticleS2CPacket.class, "ParticleS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.query.PingResultS2CPacket.class, "PingResultS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket.class, "PlaySoundFromEntityS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket.class, "PlaySoundS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket.class, "PlayerAbilitiesS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket.class, "PlayerActionResponseS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket.class, "PlayerListHeaderS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.class, "PlayerListS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.class, "PlayerPositionLookS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket.class, "PlayerRemoveS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket.class, "PlayerRespawnS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket.class, "PlayerRotationS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket.class, "PlayerSpawnPositionS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket.class, "ProfilelessChatMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ProjectilePowerS2CPacket.class, "ProjectilePowerS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket.class, "QueryResponseS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.ReadyS2CPacket.class, "ReadyS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.RecipeBookAddS2CPacket.class, "RecipeBookAddS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.RecipeBookRemoveS2CPacket.class, "RecipeBookRemoveS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.RecipeBookSettingsS2CPacket.class, "RecipeBookSettingsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket.class, "RemoveEntityStatusEffectS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.RemoveMessageS2CPacket.class, "RemoveMessageS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.ResetChatS2CPacket.class, "ResetChatS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ResourcePackRemoveS2CPacket.class, "ResourcePackRemoveS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket.class, "ResourcePackSendS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket.class, "ScoreboardDisplayS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket.class, "ScoreboardObjectiveUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket.class, "ScoreboardScoreResetS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket.class, "ScoreboardScoreUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket.class, "ScreenHandlerPropertyUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket.class, "ScreenHandlerSlotUpdateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SelectAdvancementTabS2CPacket.class, "SelectAdvancementTabS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.config.SelectKnownPacksS2CPacket.class, "SelectKnownPacksS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ServerLinksS2CPacket.class, "ServerLinksS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket.class, "ServerMetadataS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket.class, "ServerTransferS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket.class, "SetCameraEntityS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SetCursorItemS2CPacket.class, "SetCursorItemS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket.class, "SetPlayerInventoryS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket.class, "SetTradeOffersS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket.class, "ShowDialogS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket.class, "SignEditorOpenS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket.class, "SimulationDistanceS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.StartChunkSendS2CPacket.class, "StartChunkSendS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.StatisticsS2CPacket.class, "StatisticsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.StopSoundS2CPacket.class, "StopSoundS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket.class, "StoreCookieS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SubtitleS2CPacket.class, "SubtitleS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket.class, "SynchronizeRecipesS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket.class, "SynchronizeTagsS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.TeamS2CPacket.class, "TeamS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.TestInstanceBlockStatusS2CPacket.class, "TestInstanceBlockStatusS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.TickStepS2CPacket.class, "TickStepS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket.class, "TitleFadeS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.TitleS2CPacket.class, "TitleS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket.class, "UnloadChunkS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket.class, "UpdateSelectedSlotS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket.class, "UpdateTickRateS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket.class, "VehicleMoveS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WaypointS2CPacket.class, "WaypointS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderCenterChangedS2CPacket.class, "WorldBorderCenterChangedS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket.class, "WorldBorderInitializeS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket.class, "WorldBorderInterpolateSizeS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderSizeChangedS2CPacket.class, "WorldBorderSizeChangedS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderWarningBlocksChangedS2CPacket.class, "WorldBorderWarningBlocksChangedS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldBorderWarningTimeChangedS2CPacket.class, "WorldBorderWarningTimeChangedS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldEventS2CPacket.class, "WorldEventS2CPacket");
        putS2c(net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket.class, "WorldTimeUpdateS2CPacket");

        Set<Class<? extends Packet<?>>> all = new HashSet<>();
        all.addAll(C2S_PACKETS.keySet());
        all.addAll(S2C_PACKETS.keySet());
        PACKETS = Collections.unmodifiableSet(all);
    }

    private PacketUtils() {
    }

    private static void putC2s(Class<? extends Packet<?>> clazz, String name) {
        C2S_PACKETS.put(clazz, name);
        C2S_PACKETS_R.put(name, clazz);
    }

    @SuppressWarnings("unchecked")
    private static void putS2cIfClassPresent(String className, String name) {
        try {
            Class<? extends Packet<?>> c = (Class<? extends Packet<?>>) (Class<?>) Class.forName(className);
            putS2c(c, name);
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void putS2c(Class<? extends Packet<?>> clazz, String name) {
        S2C_PACKETS.put(clazz, name);
        S2C_PACKETS_R.put(name, clazz);
    }

    public static String getName(Class<? extends Packet<?>> packetClass) {
        String name = S2C_PACKETS.get(packetClass);
        if (name != null) {
            return name;
        }
        return C2S_PACKETS.get(packetClass);
    }

    public static Class<? extends Packet<?>> getPacket(String name) {
        Class<? extends Packet<?>> packet = S2C_PACKETS_R.get(name);
        if (packet != null) {
            return packet;
        }
        return C2S_PACKETS_R.get(name);
    }

    public static Set<Class<? extends Packet<?>>> getS2CPackets() {
        return S2C_PACKETS.keySet();
    }

    public static Set<Class<? extends Packet<?>>> getC2SPackets() {
        return C2S_PACKETS.keySet();
    }

    /**
     * Returns a Meteor-style display name for logging (falls back to {@link Class#getSimpleName()}).
     */
    public static String getPacketTypeName(Packet<?> packet) {
        Objects.requireNonNull(packet, "packet");
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        String registered = getName(clazz);
        if (registered != null && !registered.isBlank()) {
            return registered;
        }
        Class<?> enc = clazz.getEnclosingClass();
        if (enc != null && Packet.class.isAssignableFrom(clazz)) {
            return enc.getSimpleName() + "." + clazz.getSimpleName();
        }
        return clazz.getSimpleName();
    }

    /**
     * Resolves {@code clazz} to a C2S packet class, including assignment-compatible subclasses
     * (same idea as {@code PacketListSetting} contains checks).
     */
    public static Class<? extends Packet<?>> resolveC2sPacketClass(Class<? extends Packet<?>> clazz) {
        if (C2S_PACKETS.containsKey(clazz)) {
            return clazz;
        }
        for (Class<? extends Packet<?>> known : C2S_PACKETS.keySet()) {
            if (known.isAssignableFrom(clazz)) {
                return known;
            }
        }
        return clazz;
    }

    public static boolean isRegisteredC2s(Class<? extends Packet<?>> clazz) {
        Class<? extends Packet<?>> resolved = resolveC2sPacketClass(clazz);
        return C2S_PACKETS.containsKey(resolved);
    }

    /**
     * Resolves {@code clazz} to a registered S2C packet class, including assignment-compatible subclasses.
     */
    public static Class<? extends Packet<?>> resolveS2cPacketClass(Class<? extends Packet<?>> clazz) {
        if (S2C_PACKETS.containsKey(clazz)) {
            return clazz;
        }
        for (Class<? extends Packet<?>> known : S2C_PACKETS.keySet()) {
            if (known.isAssignableFrom(clazz)) {
                return known;
            }
        }
        return clazz;
    }

    public static boolean isRegisteredS2c(Class<? extends Packet<?>> clazz) {
        Class<? extends Packet<?>> resolved = resolveS2cPacketClass(clazz);
        return S2C_PACKETS.containsKey(resolved);
    }

    public static Set<Class<? extends Packet<?>>> c2sPacketSetFromNames(Iterable<String> names) {
        Set<Class<? extends Packet<?>>> out = new HashSet<>();
        if (names == null) {
            return out;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Class<? extends Packet<?>> c = getPacket(name.trim());
            if (c != null && C2S_PACKETS.containsKey(c)) {
                out.add(c);
            }
        }
        return out;
    }

    public static Set<Class<? extends Packet<?>>> s2cPacketSetFromNames(Iterable<String> names) {
        Set<Class<? extends Packet<?>>> out = new HashSet<>();
        if (names == null) {
            return out;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Class<? extends Packet<?>> c = getPacket(name.trim());
            if (c != null && S2C_PACKETS.containsKey(c)) {
                out.add(c);
            }
        }
        return out;
    }

    public static Set<String> namesFromC2sPacketSet(Set<Class<? extends Packet<?>>> classes) {
        Set<String> out = new HashSet<>();
        if (classes == null) {
            return out;
        }
        for (Class<? extends Packet<?>> c : classes) {
            if (c == null) {
                continue;
            }
            String n = getName(c);
            if (n != null && !n.isBlank()) {
                out.add(n);
            }
        }
        return out;
    }
}
