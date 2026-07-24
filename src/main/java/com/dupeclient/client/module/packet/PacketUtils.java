package com.dupeclient.client.module.packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.protocol.Packet;

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
        putC2s(net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket.class, "AcceptCodeOfConductC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket.class, "AcknowledgeChunksC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket.class, "AcknowledgeReconfigurationC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket.class, "AdvancementTabC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket.class, "BoatPaddleStateC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundEditBookPacket.class, "BookUpdateC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket.class, "BundleItemSelectedC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket.class, "ButtonClickC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChangeGameModePacket.class, "ChangeGameModeC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket.class, "ChatCommandSignedC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChatPacket.class, "ChatMessageC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundContainerClickPacket.class, "ClickSlotC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.class, "ClientCommandC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundClientInformationPacket.class, "ClientOptionsC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.class, "ClientStatusC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket.class, "ClientTickEndC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundContainerClosePacket.class, "CloseHandledScreenC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChatCommandPacket.class, "CommandExecutionC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundPongPacket.class, "CommonPongC2SPacket");
        putC2s(net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket.class, "CookieResponseC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket.class, "CraftRequestC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket.class, "CreativeInventoryActionC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket.class, "CustomClickActionC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket.class, "CustomPayloadC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket.class, "DebugSubscriptionRequestC2SPacket");
        putC2s(net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket.class, "EnterConfigurationC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSwingPacket.class, "HandSwingC2SPacket");
        putC2s(net.minecraft.network.protocol.handshake.ClientIntentionPacket.class, "HandshakeC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket.class, "JigsawGeneratingC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundKeepAlivePacket.class, "KeepAliveC2SPacket");
        putC2s(net.minecraft.network.protocol.login.ServerboundHelloPacket.class, "LoginHelloC2SPacket");
        putC2s(net.minecraft.network.protocol.login.ServerboundKeyPacket.class, "LoginKeyC2SPacket");
        putC2s(net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket.class, "LoginQueryResponseC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChatAckPacket.class, "MessageAcknowledgmentC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket.class, "PickItemFromBlockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket.class, "PickItemFromEntityC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.class, "PlayerActionC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlayerInputPacket.class, "PlayerInputC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket.class, "PlayerInteractBlockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundInteractPacket.class, "PlayerInteractEntityC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundUseItemPacket.class, "PlayerInteractItemC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket.class, "PlayerLoadedC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.class, "PlayerMoveC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot.class, "PlayerMoveC2SPacket.Full");
        putC2s(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot.class, "PlayerMoveC2SPacket.LookAndOnGround");
        putC2s(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly.class, "PlayerMoveC2SPacket.OnGroundOnly");
        putC2s(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos.class, "PlayerMoveC2SPacket.PositionAndOnGround");
        putC2s(net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket.class, "PlayerSessionC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket.class, "QueryBlockNbtC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket.class, "QueryEntityNbtC2SPacket");
        putC2s(net.minecraft.network.protocol.ping.ServerboundPingRequestPacket.class, "QueryPingC2SPacket");
        putC2s(net.minecraft.network.protocol.status.ServerboundStatusRequestPacket.class, "QueryRequestC2SPacket");
        putC2s(net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.class, "ReadyC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket.class, "RecipeBookDataC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket.class, "RecipeCategoryOptionsC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundRenameItemPacket.class, "RenameItemC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket.class, "RequestCommandCompletionsC2SPacket");
        putC2s(net.minecraft.network.protocol.common.ServerboundResourcePackPacket.class, "ResourcePackStatusC2SPacket");
        putC2s(net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks.class, "SelectKnownPacksC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSelectTradePacket.class, "SelectMerchantTradeC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket.class, "SetTestBlockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket.class, "SlotChangedStateC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket.class, "SpectatorTeleportC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket.class, "TeleportConfirmC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket.class, "TestInstanceBlockActionC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetBeaconPacket.class, "UpdateBeaconC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket.class, "UpdateCommandBlockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket.class, "UpdateCommandBlockMinecartC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket.class, "UpdateDifficultyC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket.class, "UpdateDifficultyLockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket.class, "UpdateJigsawC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket.class, "UpdatePlayerAbilitiesC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket.class, "UpdateSelectedSlotC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket.class, "UpdateSignC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket.class, "UpdateStructureBlockC2SPacket");
        putC2s(net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.class, "VehicleMoveC2SPacket");

        putS2c(net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket.class, "AdvancementUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket.class, "BlockBreakingProgressS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, "BlockEntityUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBlockEventPacket.class, "BlockEventS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket.class, "BlockUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket.class, "BlockValueDebugS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBossEventPacket.class, "BossBarS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerChatPacket.class, "ChatMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.class, "ChatSuggestionsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.class, "ChunkBiomeDataS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket.class, "ChunkDataS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket.class, "ChunkDeltaUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket.class, "ChunkLoadDistanceS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket.class, "ChunkRenderDistanceCenterS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket.class, "ChunkSentS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket.class, "ChunkValueDebugS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundClearDialogPacket.class, "ClearDialogS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundClearTitlesPacket.class, "ClearTitleS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundContainerClosePacket.class, "CloseScreenS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket.class, "CodeOfConductS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket.class, "CommandSuggestionsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundCommandsPacket.class, "CommandTreeS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundPingPacket.class, "CommonPingS2CPacket");
        putS2c(net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket.class, "CookieRequestS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundCooldownPacket.class, "CooldownUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket.class, "CraftFailedResponseS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class, "CustomPayloadS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket.class, "CustomReportDetailsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket.class, "DamageTiltS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket.class, "DeathMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDebugSamplePacket.class, "DebugSampleS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket.class, "DifficultyS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundDisconnectPacket.class, "DisconnectS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket.class, "DynamicRegistriesS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket.class, "EndCombatS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket.class, "EnterCombatS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket.class, "EnterReconfigurationS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket.class, "EntitiesDestroyS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundAnimatePacket.class, "EntityAnimationS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket.class, "EntityAttachS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.class, "EntityAttributesS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDamageEventPacket.class, "EntityDamageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket.class, "EntityEquipmentUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetPassengersPacket.class, "EntityPassengersSetS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket.class, "EntityPositionS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket.class, "EntityPositionSyncS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.class, "EntityS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos.class, "EntityS2CPacket.MoveRelative");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot.class, "EntityS2CPacket.Rotate");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot.class, "EntityS2CPacket.RotateAndMoveRelative");
        putS2c(net.minecraft.network.protocol.game.ClientboundRotateHeadPacket.class, "EntitySetHeadYawS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundAddEntityPacket.class, "EntitySpawnS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket.class, "EntityStatusEffectS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundEntityEventPacket.class, "EntityStatusS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.class, "EntityTrackerUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket.class, "EntityValueDebugS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class, "EntityVelocityUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDebugEventPacket.class, "EventDebugS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetExperiencePacket.class, "ExperienceBarUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundExplodePacket.class, "ExplosionS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket.class, "FeaturesS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundLoginPacket.class, "GameJoinS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSystemChatPacket.class, "GameMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundGameEventPacket.class, "GameStateChangeS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket.class, "GameTestHighlightPosS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetHealthPacket.class, "HealthUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket.class, "InventoryS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket.class, "ItemPickupAnimationS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundKeepAlivePacket.class, "KeepAliveS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundLightUpdatePacket.class, "LightUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket.class, "LoginCompressionS2CPacket");
        putS2c(net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket.class, "LoginDisconnectS2CPacket");
        putS2c(net.minecraft.network.protocol.login.ClientboundHelloPacket.class, "LoginHelloS2CPacket");
        putS2c(net.minecraft.network.protocol.login.ClientboundCustomQueryPacket.class, "LoginQueryRequestS2CPacket");
        putS2c(net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket.class, "LoginSuccessS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket.class, "LookAtS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMapItemDataPacket.class, "MapUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket.class, "MoveMinecartAlongTrackS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTagQueryPacket.class, "NbtQueryResponseS2CPacket");
        putS2cIfClassPresent("net.minecraft.network.packet.s2c.play.OpenMountScreenS2CPacket", "OpenMountScreenS2CPacket");
        putS2cIfClassPresent("net.minecraft.network.packet.s2c.play.OpenHorseScreenS2CPacket", "OpenMountScreenS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundOpenScreenPacket.class, "OpenScreenS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundOpenBookPacket.class, "OpenWrittenBookS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket.class, "OverlayMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class, "ParticleS2CPacket");
        putS2c(net.minecraft.network.protocol.ping.ClientboundPongResponsePacket.class, "PingResultS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSoundEntityPacket.class, "PlaySoundFromEntityS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSoundPacket.class, "PlaySoundS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket.class, "PlayerAbilitiesS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket.class, "PlayerActionResponseS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTabListPacket.class, "PlayerListHeaderS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.class, "PlayerListS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class, "PlayerPositionLookS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket.class, "PlayerRemoveS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRespawnPacket.class, "PlayerRespawnS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket.class, "PlayerRotationS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket.class, "PlayerSpawnPositionS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket.class, "ProfilelessChatMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket.class, "ProjectilePowerS2CPacket");
        putS2c(net.minecraft.network.protocol.status.ClientboundStatusResponsePacket.class, "QueryResponseS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket.class, "ReadyS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket.class, "RecipeBookAddS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket.class, "RecipeBookRemoveS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket.class, "RecipeBookSettingsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket.class, "RemoveEntityStatusEffectS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundDeleteChatPacket.class, "RemoveMessageS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundResetChatPacket.class, "ResetChatS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket.class, "ResourcePackRemoveS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket.class, "ResourcePackSendS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket.class, "ScoreboardDisplayS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetObjectivePacket.class, "ScoreboardObjectiveUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundResetScorePacket.class, "ScoreboardScoreResetS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetScorePacket.class, "ScoreboardScoreUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket.class, "ScreenHandlerPropertyUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.class, "ScreenHandlerSlotUpdateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket.class, "SelectAdvancementTabS2CPacket");
        putS2c(net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks.class, "SelectKnownPacksS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundServerLinksPacket.class, "ServerLinksS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundServerDataPacket.class, "ServerMetadataS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundTransferPacket.class, "ServerTransferS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetCameraPacket.class, "SetCameraEntityS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket.class, "SetCursorItemS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket.class, "SetPlayerInventoryS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket.class, "SetTradeOffersS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundShowDialogPacket.class, "ShowDialogS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket.class, "SignEditorOpenS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket.class, "SimulationDistanceS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket.class, "StartChunkSendS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundAwardStatsPacket.class, "StatisticsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundStopSoundPacket.class, "StopSoundS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundStoreCookiePacket.class, "StoreCookieS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket.class, "SubtitleS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket.class, "SynchronizeRecipesS2CPacket");
        putS2c(net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket.class, "SynchronizeTagsS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.class, "TeamS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus.class, "TestInstanceBlockStatusS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTickingStepPacket.class, "TickStepS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket.class, "TitleFadeS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket.class, "TitleS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket.class, "UnloadChunkS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket.class, "UpdateSelectedSlotS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTickingStatePacket.class, "UpdateTickRateS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket.class, "VehicleMoveS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket.class, "WaypointS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket.class, "WorldBorderCenterChangedS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket.class, "WorldBorderInitializeS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket.class, "WorldBorderInterpolateSizeS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket.class, "WorldBorderSizeChangedS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket.class, "WorldBorderWarningBlocksChangedS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket.class, "WorldBorderWarningTimeChangedS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundLevelEventPacket.class, "WorldEventS2CPacket");
        putS2c(net.minecraft.network.protocol.game.ClientboundSetTimePacket.class, "WorldTimeUpdateS2CPacket");

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
