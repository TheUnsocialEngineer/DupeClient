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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

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
    private KeyMapping keyHoldBinding;
    @Nullable
    private BlockPos blockInteractTarget;
    private int blockInteractNavigateTicks;
    private boolean blockInteractPathStarted;
    private int guiItemRemaining;
    private int guiItemCooldown;
    @Nullable
    private KeyMapping moveAuxBinding;
    @Nullable
    private KeyMapping moveAux2Binding;
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

    public void start(Minecraft client, String id) {
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

    public void stop(Minecraft client) {
        stop(client, true);
    }

    private void stop(Minecraft client, boolean announce) {
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

    public void tick(Minecraft client) {
        if (!running) {
            return;
        }
        if (client == null || client.player == null || client.level == null) {
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
                client.gui.setScreen(null);
                yield true;
            }
            case CLOSE_GUI -> {
                client.player.closeContainer();
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
                client.player.setYRot(client.player.getYRot() + step.ticks);
                client.player.setYBodyRot(client.player.getYRot());
                client.player.setYHeadRot(client.player.getYRot());
                yield true;
            }
            case LOOK_PITCH -> {
                float p = Mth.clamp(client.player.getXRot() + step.ticks, -90f, 90f);
                client.player.setXRot(p);
                yield true;
            }
            case GUI_ITEM -> tickGuiItem(client, step);
            case CLICK_SLOT -> {
                if (client.gui.screen() instanceof AbstractContainerScreen<?> hs) {
                    AbstractContainerMenu handler = hs.getMenu();
                    ContainerInput action = MacroSlotActions.toVanilla(step.clickSlotAction);
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
                client.player.getInventory().setSelectedSlot(Mth.clamp(step.hotbarSlot, 0, 8));
                yield true;
            }
            case DROP_ITEM -> {
                client.player.getInventory().removeFromSelected(step.dropFullStack);
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

    private void endIfDone(Minecraft client) {
        if (running && stepIndex >= compiledRun.steps().size()) {
            stop(client, true);
        }
    }

    private void onStepEnter(Minecraft client, MacroStep step) {
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
                LocalPlayer p = client.player;
                p.setYRot(yaw);
                p.setYBodyRot(yaw);
                p.setYHeadRot(yaw);
            }
            if ("BLOCKS".equalsIgnoreCase(step.moveMeasure == null ? "" : step.moveMeasure.trim())) {
                moveBlockStart = client.player.blockPosition();
                moveBlockTargetCheb = Math.max(1, step.moveDistanceBlocks);
            } else {
                moveTicksRemaining = Math.max(1, step.ticks);
            }
            moveForwardHeld = true;
            if (client.options != null) {
                client.options.keyUp.setDown(true);
            }
            applyMoveAuxBindings(client, step);
        } else if (type == MacroStepType.KEY_HOLD) {
            keyHoldBinding = MacroHoldKeys.binding(client, step.holdKeyId);
            keyHoldTicksRemaining = Math.max(1, step.ticks);
            if (keyHoldBinding != null) {
                keyHoldBinding.setDown(true);
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

    private void finishStep(Minecraft client, MacroStepType type) {
        if (type == MacroStepType.MOVE_FORWARD) {
            moveForwardHeld = false;
            if (client.options != null) {
                client.options.keyUp.setDown(false);
            }
            releaseMoveAuxBindings();
        } else if (type == MacroStepType.KEY_HOLD) {
            if (keyHoldBinding != null) {
                keyHoldBinding.setDown(false);
            }
            keyHoldBinding = null;
        }
    }

    private void cleanupMovementKeys(@Nullable Minecraft client) {
        if (client != null && client.options != null) {
            client.options.keyUp.setDown(false);
        }
        if (keyHoldBinding != null) {
            keyHoldBinding.setDown(false);
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
            moveAuxBinding.setDown(false);
            moveAuxBinding = null;
        }
        if (moveAux2Binding != null) {
            moveAux2Binding.setDown(false);
            moveAux2Binding = null;
        }
    }

    private void applyMoveAuxBindings(Minecraft client, MacroStep step) {
        releaseMoveAuxBindings();
        if (client.options == null) {
            return;
        }
        String a1 = step.moveAuxHoldKeyId == null ? "" : step.moveAuxHoldKeyId.trim();
        if (!a1.isEmpty()) {
            KeyMapping kb = MacroHoldKeys.binding(client, a1);
            if (kb != null && kb != client.options.keyUp) {
                moveAuxBinding = kb;
                kb.setDown(true);
            }
        }
        String a2 = step.moveAuxHoldKey2Id == null ? "" : step.moveAuxHoldKey2Id.trim();
        if (!a2.isEmpty()) {
            KeyMapping kb = MacroHoldKeys.binding(client, a2);
            if (kb != null && kb != client.options.keyUp && kb != moveAuxBinding) {
                moveAux2Binding = kb;
                kb.setDown(true);
            }
        }
    }

    private void refreshMoveAuxPressed(Minecraft client) {
        if (client.options == null) {
            return;
        }
        if (moveAuxBinding != null) {
            moveAuxBinding.setDown(true);
        }
        if (moveAux2Binding != null) {
            moveAux2Binding.setDown(true);
        }
    }

    private boolean tickWaitTicks() {
        if (waitTicksRemaining <= 1) {
            return true;
        }
        waitTicksRemaining--;
        return false;
    }

    private void tickSendChat(Minecraft client, MacroStep step) {
        String msg = step.text == null ? "" : step.text.trim();
        if (msg.isEmpty() || client.player == null || client.getConnection() == null) {
            return;
        }
        if (msg.startsWith("/")) {
            client.getConnection().sendCommand(msg.substring(1));
        } else {
            client.getConnection().sendChat(msg);
        }
    }

    private boolean tickMoveForward(Minecraft client, MacroStep step) {
        if (client.player == null || client.options == null) {
            return true;
        }
        if (!moveForwardHeld) {
            moveForwardHeld = true;
            client.options.keyUp.setDown(true);
        }
        refreshMoveAuxPressed(client);
        if ("BLOCKS".equalsIgnoreCase(step.moveMeasure == null ? "" : step.moveMeasure.trim())) {
            if (moveBlockStart == null) {
                moveBlockStart = client.player.blockPosition();
            }
            BlockPos cur = client.player.blockPosition();
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
                keyHoldBinding.setDown(false);
            }
            return true;
        }
        keyHoldTicksRemaining--;
        return false;
    }

    private boolean tickGuiItem(Minecraft client, MacroStep step) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> handled)) {
            return false;
        }
        AbstractContainerMenu handler = handled.getMenu();
        if (handler == null || client.player == null || client.gameMode == null) {
            return false;
        }
        if (!MacroAutomation.hasNonPlayerInventorySlots(handler, client.player)) {
            return false;
        }
        Item item = MacroAutomation.resolveItem(step.guiItemId);
        boolean anyItem = MacroAutomation.isAnyItem(step.guiItemId);
        if (!anyItem && (item == null || item == net.minecraft.world.item.Items.AIR)) {
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
    private boolean guiItemTryOneQuickMove(Minecraft client, MacroStep step, AbstractContainerMenu handler, Item item, boolean anyItem) {
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
        int before = pick.getItem().getCount();
        MacroAutomation.quickMoveSlot(client, handler, pick);
        int moved = Math.max(0, before - pick.getItem().getCount());
        if (moved <= 0) {
            return true;
        }
        guiItemRemaining -= moved;
        return guiItemRemaining <= 0;
    }

    private boolean tickBlockInteract(Minecraft client, MacroStep step) {
        if (blockInteractTarget == null) {
            return true;
        }
        LocalPlayer player = client.player;
        Level world = client.level;
        if (player == null || world == null || client.gameMode == null) {
            return true;
        }
        double reach = player.blockInteractionRange() + 0.5;
        if (player.distanceToSqr(Vec3.atCenterOf(blockInteractTarget)) <= reach * reach) {
            return MacroAutomation.tryInteractBlock(client, blockInteractTarget);
        }
        blockInteractNavigateTicks++;
        if (!blockInteractPathStarted) {
            blockInteractPathStarted = MacroBaritoneSupport.startPathToBlock(client, blockInteractTarget);
        }
        return blockInteractNavigateTicks >= Math.max(1, step.blockNavigateMaxTicks);
    }

    private boolean tickWaitLookBlock(Minecraft client, MacroStep step) {
        Identifier want = parseIdWithMinecraftDefault(step.blockCustomId);
        if (want == null) {
            return false;
        }
        if (!BuiltInRegistries.BLOCK.containsKey(want)) {
            return false;
        }
        Block wantBlock = BuiltInRegistries.BLOCK.getValue(want);
        if (wantBlock == Blocks.AIR) {
            return false;
        }
        LookTargetUtil.LookTarget pick = LookTargetUtil.pick(client);
        boolean match = pick != null
                && pick.block() != null
                && client.level != null
                && client.level.getBlockState(pick.block().getBlockPos()).is(wantBlock);
        return advanceWaitLook(match, step.ticks);
    }

    private boolean tickWaitLookEntity(Minecraft client, MacroStep step) {
        Identifier wantType = parseIdWithMinecraftDefault(step.entityTypeId);
        if (wantType == null) {
            return false;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(wantType)) {
            return false;
        }
        EntityType<?> wantEt = BuiltInRegistries.ENTITY_TYPE.getValue(wantType);
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
