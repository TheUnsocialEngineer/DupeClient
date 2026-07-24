package com.dupeclient.client.module.macro;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.macro.graph.MacroCompiledRun;
import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;
import com.dupeclient.client.module.macro.graph.MacroGraphTypes;
import com.dupeclient.client.module.macro.graph.MacroInfiniteLoop;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.dupeclient.client.module.packet.fabricator.PacketFabricator;
import com.dupeclient.client.core.LookTargetUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Client macro runner (graph-compiled linear steps). {@link MacroEngine} delegates here.
 */
public final class MacroRuntime {
    public static final MacroRuntime INSTANCE = new MacroRuntime();

    private static final int LOOK_MATCH_STREAK_TICKS = 1;
    private static final int GUI_ITEM_BURST_MAX = 64;

    private boolean running;
    private String activeMacroId = "";
    private String activeDisplayName = "";
    private MacroCompiledRun compiledRun = MacroCompiledRun.empty();
    private int stepIndex;
    private int sessionAtStepIndex = -1;

    private int waitTicksRemaining;
    private boolean moveForwardHeld;
    private int moveTicksRemaining;
    private BlockPos moveBlockStart;
    private int moveBlockTargetCheb;
    private int keyHoldTicksRemaining;
    @Nullable
    private KeyBinding keyHoldBinding;
    @Nullable
    private BlockPos blockInteractTarget;
    private int blockInteractNavigateTicks;
    private boolean blockInteractPathStarted;
    private int guiItemRemaining;
    private int guiItemCooldown;
    @Nullable
    private KeyBinding moveAuxBinding;
    @Nullable
    private KeyBinding moveAux2Binding;
    private int waitLookStreak;
    private int waitLookBudget;

    private MacroRuntime() {
    }

    public boolean isRunning() {
        return running;
    }

    @Nullable
    public String getActiveMacroId() {
        return activeMacroId.isEmpty() ? null : activeMacroId;
    }

    public String getRunLabel() {
        if (!running) {
            return "";
        }
        if (activeDisplayName != null && !activeDisplayName.isBlank()) {
            return activeDisplayName + " (" + activeMacroId + ")";
        }
        return activeMacroId;
    }

    public void start(MinecraftClient client, String id) {
        stop(client, false);
        try {
            MacroDefinition def = MacroStorage.load(id);
            MacroCompiledRun run = MacroGraphCompiler.compileRun(def);
            if (run.steps().isEmpty()) {
                DupeClient.LOGGER.warn("Macro \"{}\" has no runnable steps.", MacroStorage.filenameId(id));
                return;
            }
            running = true;
            activeMacroId = MacroStorage.filenameId(def.id);
            activeDisplayName = def.displayName == null ? "" : def.displayName.trim();
            compiledRun = run;
            stepIndex = 0;
            sessionAtStepIndex = -1;
            DupeClient.LOGGER.info("Macro started: {}", activeMacroId);
        } catch (IOException e) {
            DupeClient.LOGGER.warn("Macro start failed: {}", e.getMessage());
        }
    }

    public void stop(MinecraftClient client) {
        stop(client, true);
    }

    private void stop(MinecraftClient client, boolean announce) {
        boolean wasRunning = running;
        cleanupMovementKeys(client);
        if (wasRunning) {
            MacroBaritoneSupport.cancelPathing(client);
        }
        if (running && announce) {
            DupeClient.LOGGER.info("Macro stopped: {}", activeMacroId);
        }
        running = false;
        activeMacroId = "";
        activeDisplayName = "";
        compiledRun = MacroCompiledRun.empty();
        stepIndex = 0;
        sessionAtStepIndex = -1;
        clearSessionFields();
    }

    public void tick(MinecraftClient client) {
        if (!running) {
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            stop(client, false);
            return;
        }
        List<MacroStep> steps = compiledRun.steps();
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            stop(client, true);
            return;
        }
        MacroStep step = steps.get(stepIndex);
        if (stepIndex != sessionAtStepIndex) {
            onStepEnter(client, step);
            sessionAtStepIndex = stepIndex;
        }
        MacroStepType type = MacroStepType.fromString(step.type);
        if (type == MacroStepType.UNKNOWN) {
            finishStep(client, type);
            advanceStepIndex();
            endIfDone(client);
            return;
        }
        boolean advance = switch (type) {
            case WAIT_TICKS -> tickWaitTicks();
            case SEND_CHAT -> {
                tickSendChat(client, step);
                yield true;
            }
            case CLOSE_SCREEN -> {
                client.setScreen(null);
                yield true;
            }
            case CLOSE_GUI -> {
                client.player.closeHandledScreen();
                yield true;
            }
            case UI_UTILS_TOGGLE_DELAY -> {
                PacketUtilsManager.INSTANCE.toggleUiUtilsDelay();
                yield true;
            }
            case UI_UTILS_FLUSH_QUEUE -> {
                PacketUtilsManager.INSTANCE.flushUiUtilsQueueNow(client);
                yield true;
            }
            case PACKET_DELAY_TOGGLE -> {
                PacketUtilsManager mgr = PacketUtilsManager.INSTANCE;
                mgr.setPacketDelayEnabled(!mgr.getSettings().packetDelayEnabled);
                yield true;
            }
            case PACKET_DELAY_FLUSH -> {
                PacketUtilsManager.INSTANCE.flushPacketDelayQueue();
                yield true;
            }
            case FABRICATOR_SEND -> {
                PacketUtilsSettings settings = PacketFabricator.INSTANCE.settings();
                String prevSlot = settings.fabricatorSlot;
                String prevItem = settings.fabricatorItemName;
                String prevTimes = settings.fabricatorTimes;
                int prevAction = settings.fabricatorActionIndex;
                settings.fabricatorSlot = step.fabricatorSlot == null ? "0" : step.fabricatorSlot;
                settings.fabricatorItemName = "";
                settings.fabricatorTimes = Integer.toString(Math.max(1, step.fabricatorTimes));
                settings.fabricatorActionIndex = Math.max(0, step.fabricatorActionIndex);
                PacketFabricator.INSTANCE.send(false);
                settings.fabricatorSlot = prevSlot;
                settings.fabricatorItemName = prevItem;
                settings.fabricatorTimes = prevTimes;
                settings.fabricatorActionIndex = prevAction;
                PacketUtilsManager.INSTANCE.save();
                yield true;
            }
            case MOVE_FORWARD -> tickMoveForward(client, step);
            case LOOK_TURN -> {
                client.player.setYaw(client.player.getYaw() + step.ticks);
                client.player.setBodyYaw(client.player.getYaw());
                client.player.setHeadYaw(client.player.getYaw());
                yield true;
            }
            case LOOK_PITCH -> {
                float p = MathHelper.clamp(client.player.getPitch() + step.ticks, -90f, 90f);
                client.player.setPitch(p);
                yield true;
            }
            case GUI_ITEM -> tickGuiItem(client, step);
            case CLICK_SLOT -> {
                if (client.currentScreen instanceof HandledScreen<?> hs) {
                    ScreenHandler handler = hs.getScreenHandler();
                    SlotActionType action = MacroSlotActions.toVanilla(step.clickSlotAction);
                    MacroAutomation.clickSlot(client, handler, step.clickSlotId, action, step.clickSlotButton);
                }
                yield true;
            }
            case BLOCK_INTERACT -> tickBlockInteract(client, step);
            case USE_HOTBAR_ITEM -> {
                MacroAutomation.tryUseHotbarSlot(client, step.hotbarSlot);
                yield true;
            }
            case KEY_HOLD -> tickKeyHold();
            case PRESS_BUTTON -> {
                MacroKeyPress.simulatePress(client, step.pressKeyCode, step.pressKeyModifiers);
                yield true;
            }
            case HOTBAR_SELECT -> {
                client.player.getInventory().setSelectedSlot(MathHelper.clamp(step.hotbarSlot, 0, 8));
                yield true;
            }
            case DROP_ITEM -> {
                client.player.getInventory().dropSelectedItem(step.dropFullStack);
                yield true;
            }
            case WAIT_LOOK_BLOCK -> tickWaitLookBlock(client, step);
            case WAIT_LOOK_ENTITY -> tickWaitLookEntity(client, step);
            case UNKNOWN -> false;
        };
        if (advance) {
            finishStep(client, type);
            advanceStepIndex();
            endIfDone(client);
        }
    }

    private void endIfDone(MinecraftClient client) {
        if (running && stepIndex >= compiledRun.steps().size()) {
            stop(client, true);
        }
    }

    private void onStepEnter(MinecraftClient client, MacroStep step) {
        MacroStepType type = MacroStepType.fromString(step.type);
        waitTicksRemaining = Math.max(0, step.ticks);
        moveForwardHeld = false;
        moveTicksRemaining = 0;
        moveBlockStart = null;
        moveBlockTargetCheb = 0;
        keyHoldTicksRemaining = 0;
        keyHoldBinding = null;
        releaseMoveAuxBindings();
        blockInteractTarget = null;
        blockInteractNavigateTicks = 0;
        blockInteractPathStarted = false;
        guiItemRemaining = 0;
        guiItemCooldown = 0;
        waitLookStreak = 0;
        waitLookBudget = Math.max(0, step.ticks);

        if (type == MacroStepType.MOVE_FORWARD) {
            if (!"PLAYER".equals(MacroGraphTypes.normalizeWalkFacing(step.walkFacing))) {
                float yaw = yawCardinalDegrees(step.walkFacing);
                ClientPlayerEntity p = client.player;
                p.setYaw(yaw);
                p.setBodyYaw(yaw);
                p.setHeadYaw(yaw);
            }
            if ("BLOCKS".equalsIgnoreCase(step.moveMeasure == null ? "" : step.moveMeasure.trim())) {
                moveBlockStart = client.player.getBlockPos();
                moveBlockTargetCheb = Math.max(1, step.moveDistanceBlocks);
            } else {
                moveTicksRemaining = Math.max(1, step.ticks);
            }
            moveForwardHeld = true;
            if (client.options != null) {
                client.options.forwardKey.setPressed(true);
            }
            applyMoveAuxBindings(client, step);
        } else if (type == MacroStepType.KEY_HOLD) {
            keyHoldBinding = MacroHoldKeys.binding(client, step.holdKeyId);
            keyHoldTicksRemaining = Math.max(1, step.ticks);
            if (keyHoldBinding != null) {
                keyHoldBinding.setPressed(true);
            }
        } else if (type == MacroStepType.GUI_ITEM) {
            guiItemRemaining = step.guiItemCount < 0 ? -1 : Math.max(1, step.guiItemCount);
            guiItemCooldown = 0;
        } else if (type == MacroStepType.BLOCK_INTERACT) {
            blockInteractTarget = MacroAutomation.findNearestMatchingBlock(
                    client, step.blockPreset, step.blockCustomId, step.blockSearchRadius);
            blockInteractNavigateTicks = 0;
            blockInteractPathStarted = false;
        } else if (type == MacroStepType.WAIT_LOOK_BLOCK || type == MacroStepType.WAIT_LOOK_ENTITY) {
            waitLookStreak = 0;
            waitLookBudget = Math.max(0, step.ticks);
        }
    }

    private void finishStep(MinecraftClient client, MacroStepType type) {
        if (type == MacroStepType.MOVE_FORWARD) {
            moveForwardHeld = false;
            if (client.options != null) {
                client.options.forwardKey.setPressed(false);
            }
            releaseMoveAuxBindings();
        } else if (type == MacroStepType.KEY_HOLD) {
            if (keyHoldBinding != null) {
                keyHoldBinding.setPressed(false);
            }
            keyHoldBinding = null;
        }
    }

    private void cleanupMovementKeys(@Nullable MinecraftClient client) {
        if (client != null && client.options != null) {
            client.options.forwardKey.setPressed(false);
        }
        if (keyHoldBinding != null) {
            keyHoldBinding.setPressed(false);
            keyHoldBinding = null;
        }
        releaseMoveAuxBindings();
        moveForwardHeld = false;
    }

    private void clearSessionFields() {
        waitTicksRemaining = 0;
        moveTicksRemaining = 0;
        moveBlockStart = null;
        keyHoldTicksRemaining = 0;
        blockInteractTarget = null;
        blockInteractNavigateTicks = 0;
        blockInteractPathStarted = false;
        guiItemRemaining = 0;
        guiItemCooldown = 0;
        moveAuxBinding = null;
        moveAux2Binding = null;
        waitLookStreak = 0;
        waitLookBudget = 0;
    }

    private void releaseMoveAuxBindings() {
        if (moveAuxBinding != null) {
            moveAuxBinding.setPressed(false);
            moveAuxBinding = null;
        }
        if (moveAux2Binding != null) {
            moveAux2Binding.setPressed(false);
            moveAux2Binding = null;
        }
    }

    private void applyMoveAuxBindings(MinecraftClient client, MacroStep step) {
        releaseMoveAuxBindings();
        if (client.options == null) {
            return;
        }
        String a1 = step.moveAuxHoldKeyId == null ? "" : step.moveAuxHoldKeyId.trim();
        if (!a1.isEmpty()) {
            KeyBinding kb = MacroHoldKeys.binding(client, a1);
            if (kb != null && kb != client.options.forwardKey) {
                moveAuxBinding = kb;
                kb.setPressed(true);
            }
        }
        String a2 = step.moveAuxHoldKey2Id == null ? "" : step.moveAuxHoldKey2Id.trim();
        if (!a2.isEmpty()) {
            KeyBinding kb = MacroHoldKeys.binding(client, a2);
            if (kb != null && kb != client.options.forwardKey && kb != moveAuxBinding) {
                moveAux2Binding = kb;
                kb.setPressed(true);
            }
        }
    }

    private void refreshMoveAuxPressed(MinecraftClient client) {
        if (client.options == null) {
            return;
        }
        if (moveAuxBinding != null) {
            moveAuxBinding.setPressed(true);
        }
        if (moveAux2Binding != null) {
            moveAux2Binding.setPressed(true);
        }
    }

    private boolean tickWaitTicks() {
        if (waitTicksRemaining <= 1) {
            return true;
        }
        waitTicksRemaining--;
        return false;
    }

    private void tickSendChat(MinecraftClient client, MacroStep step) {
        String msg = step.text == null ? "" : step.text.trim();
        if (msg.isEmpty() || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        if (msg.startsWith("/")) {
            client.getNetworkHandler().sendChatCommand(msg.substring(1));
        } else {
            client.getNetworkHandler().sendChatMessage(msg);
        }
    }

    private boolean tickMoveForward(MinecraftClient client, MacroStep step) {
        if (client.player == null || client.options == null) {
            return true;
        }
        if (!moveForwardHeld) {
            moveForwardHeld = true;
            client.options.forwardKey.setPressed(true);
        }
        refreshMoveAuxPressed(client);
        if ("BLOCKS".equalsIgnoreCase(step.moveMeasure == null ? "" : step.moveMeasure.trim())) {
            if (moveBlockStart == null) {
                moveBlockStart = client.player.getBlockPos();
            }
            BlockPos cur = client.player.getBlockPos();
            int cheb = Math.max(
                    Math.abs(cur.getX() - moveBlockStart.getX()),
                    Math.abs(cur.getZ() - moveBlockStart.getZ()));
            return cheb >= moveBlockTargetCheb;
        }
        moveTicksRemaining--;
        return moveTicksRemaining <= 0;
    }

    private boolean tickKeyHold() {
        if (keyHoldTicksRemaining <= 1) {
            if (keyHoldBinding != null) {
                keyHoldBinding.setPressed(false);
            }
            return true;
        }
        keyHoldTicksRemaining--;
        return false;
    }

    private boolean tickGuiItem(MinecraftClient client, MacroStep step) {
        if (!(client.currentScreen instanceof HandledScreen<?> handled)) {
            return false;
        }
        ScreenHandler handler = handled.getScreenHandler();
        if (handler == null || client.player == null || client.interactionManager == null) {
            return false;
        }
        if (!MacroAutomation.hasNonPlayerInventorySlots(handler, client.player)) {
            return false;
        }
        Item item = MacroAutomation.resolveItem(step.guiItemId);
        boolean anyItem = MacroAutomation.isAnyItem(step.guiItemId);
        if (!anyItem && (item == null || item == net.minecraft.item.Items.AIR)) {
            return true;
        }
        if (guiItemCooldown > 0) {
            guiItemCooldown--;
            return false;
        }
        int delayAfter = Math.max(0, step.guiItemDelayTicks);
        int maxBurst = delayAfter <= 0 ? GUI_ITEM_BURST_MAX : 1;
        for (int i = 0; i < maxBurst; i++) {
            boolean done = guiItemTryOneQuickMove(client, step, handler, item, anyItem);
            if (done) {
                return true;
            }
            if (delayAfter > 0) {
                guiItemCooldown = delayAfter;
                return false;
            }
        }
        return false;
    }

    /**
     * Performs one shift-quick-move if possible.
     *
     * @return {@code true} when this macro step is finished (advance).
     */
    private boolean guiItemTryOneQuickMove(MinecraftClient client, MacroStep step, ScreenHandler handler, Item item, boolean anyItem) {
        boolean takeFromContainer = "TAKE".equalsIgnoreCase(step.guiItemMode == null ? "" : step.guiItemMode.trim());
        if (guiItemRemaining < 0) {
            Slot slot = anyItem
                    ? MacroAutomation.findAnyQuickMoveSourceSlot(handler, client.player, takeFromContainer)
                    : MacroAutomation.findQuickMoveSourceSlot(handler, client.player, item, takeFromContainer);
            if (slot == null) {
                return true;
            }
            MacroAutomation.quickMoveSlot(client, handler, slot);
            return false;
        }
        if (guiItemRemaining == 0) {
            return true;
        }
        Slot pick = anyItem
                ? MacroAutomation.findSmallestAnyStackUnderCap(handler, client.player, takeFromContainer, guiItemRemaining)
                : MacroAutomation.findSmallestStackUnderCap(handler, client.player, item, takeFromContainer, guiItemRemaining);
        if (pick == null) {
            pick = anyItem
                    ? MacroAutomation.findSmallestAnyStack(handler, client.player, takeFromContainer)
                    : MacroAutomation.findSmallestMatchingStack(handler, client.player, item, takeFromContainer);
        }
        if (pick == null) {
            return true;
        }
        int before = pick.getStack().getCount();
        MacroAutomation.quickMoveSlot(client, handler, pick);
        int moved = Math.max(0, before - pick.getStack().getCount());
        if (moved <= 0) {
            return true;
        }
        guiItemRemaining -= moved;
        return guiItemRemaining <= 0;
    }

    private boolean tickBlockInteract(MinecraftClient client, MacroStep step) {
        if (blockInteractTarget == null) {
            return true;
        }
        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null || client.interactionManager == null) {
            return true;
        }
        double reach = player.getBlockInteractionRange() + 0.5;
        if (player.squaredDistanceTo(Vec3d.ofCenter(blockInteractTarget)) <= reach * reach) {
            return MacroAutomation.tryInteractBlock(client, blockInteractTarget);
        }
        blockInteractNavigateTicks++;
        if (!blockInteractPathStarted) {
            blockInteractPathStarted = MacroBaritoneSupport.startPathToBlock(client, blockInteractTarget);
        }
        return blockInteractNavigateTicks >= Math.max(1, step.blockNavigateMaxTicks);
    }

    private boolean tickWaitLookBlock(MinecraftClient client, MacroStep step) {
        Identifier want = parseIdWithMinecraftDefault(step.blockCustomId);
        if (want == null) {
            return false;
        }
        if (!Registries.BLOCK.containsId(want)) {
            return false;
        }
        Block wantBlock = Registries.BLOCK.get(want);
        if (wantBlock == Blocks.AIR) {
            return false;
        }
        LookTargetUtil.LookTarget pick = LookTargetUtil.pick(client);
        boolean match = pick != null
                && pick.block() != null
                && client.world != null
                && client.world.getBlockState(pick.block().getBlockPos()).isOf(wantBlock);
        return advanceWaitLook(match, step.ticks);
    }

    private boolean tickWaitLookEntity(MinecraftClient client, MacroStep step) {
        Identifier wantType = parseIdWithMinecraftDefault(step.entityTypeId);
        if (wantType == null) {
            return false;
        }
        if (!Registries.ENTITY_TYPE.containsId(wantType)) {
            return false;
        }
        EntityType<?> wantEt = Registries.ENTITY_TYPE.get(wantType);
        LookTargetUtil.LookTarget pick = LookTargetUtil.pick(client);
        boolean match = pick != null
                && pick.entity() != null
                && pick.entity().getEntity().getType() == wantEt;
        return advanceWaitLook(match, step.ticks);
    }

    @Nullable
    private static Identifier parseIdWithMinecraftDefault(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return Identifier.tryParse(s);
    }

    private boolean advanceWaitLook(boolean match, int maxTicks) {
        if (match) {
            waitLookStreak++;
            return waitLookStreak >= LOOK_MATCH_STREAK_TICKS;
        }
        waitLookStreak = 0;
        if (maxTicks > 0) {
            waitLookBudget--;
            return waitLookBudget <= 0;
        }
        return false;
    }


    private void advanceStepIndex() {
        stepIndex++;
        int bestStart = -1;
        for (MacroInfiniteLoop loop : compiledRun.infiniteLoops()) {
            if (loop.endExclusive() == stepIndex && loop.startInclusive() > bestStart) {
                bestStart = loop.startInclusive();
            }
        }
        if (bestStart >= 0) {
            stepIndex = bestStart;
        }
        sessionAtStepIndex = -1;
    }

    private static float yawCardinalDegrees(String walkFacing) {
        return switch (MacroGraphTypes.normalizeWalkFacing(walkFacing)) {
            case "N" -> 180f;
            case "E" -> -90f;
            case "W" -> 90f;
            default -> 0f;
        };
    }
}
