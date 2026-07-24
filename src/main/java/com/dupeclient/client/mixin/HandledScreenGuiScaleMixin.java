package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenGuiScaleMixin {
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected int imageWidth;
    @Shadow
    protected int imageHeight;

    @Shadow
    @Nullable
    protected abstract Slot getHoveredSlot(double x, double y);

    @Shadow
    private ItemStack lastQuickMoved;

    @Unique
    private boolean dupeclient$shiftClick;
    @Unique
    private final Map<GuiEventListener, int[]> dupeclient$widgetBaseLayout = new HashMap<>();
    @Unique
    private final Map<GuiEventListener, int[]> dupeclient$fixedOverlayLayout = new HashMap<>();
    @Unique
    private int dupeclient$lastSyncedX = Integer.MIN_VALUE;
    @Unique
    private int dupeclient$lastSyncedY = Integer.MIN_VALUE;

    @Unique
    private void dupeclient$syncGuiPosition() {
        Minecraft client = Minecraft.getInstance();
        int[] pos = new int[2];
        HandledScreenGuiScale.syncGuiPosition(
                pos,
                imageWidth,
                imageHeight,
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight());
        HandledScreenGuiScale.bindPanelSize(imageWidth, imageHeight);
        if (pos[0] != dupeclient$lastSyncedX || pos[1] != dupeclient$lastSyncedY) {
            dupeclient$widgetBaseLayout.clear();
            dupeclient$lastSyncedX = pos[0];
            dupeclient$lastSyncedY = pos[1];
        }
        leftPos = pos[0];
        topPos = pos[1];
    }

    @Unique
    private int dupeclient$childCount = -1;

    @Unique
    private boolean dupeclient$skipWidgetLayout(GuiEventListener child) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        return self instanceof CreativeModeInventoryScreen
                && child instanceof EditBox
                && HandledScreenGuiScale.isActive();
    }

    @Unique
    private void dupeclient$layoutHandledWidgets() {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int count = self.children().size();
        if (count != dupeclient$childCount) {
            dupeclient$widgetBaseLayout.keySet().removeIf(child -> !self.children().contains(child));
            dupeclient$childCount = count;
        }
        for (GuiEventListener child : self.children()) {
            if (!(child instanceof AbstractWidget widget)) {
                continue;
            }
            if (HandledScreenGuiScale.isScreenFixedOverlayWidget(child)) {
                HandledScreenGuiScale.pinScreenFixedWidget(widget, dupeclient$fixedOverlayLayout);
                continue;
            }
            if (dupeclient$skipWidgetLayout(child)) {
                continue;
            }
            if (!HandledScreenGuiScale.shouldScaleWithPanel(child, leftPos, topPos, imageWidth, imageHeight)) {
                continue;
            }
            int[] base = dupeclient$widgetBaseLayout.computeIfAbsent(child, ignored ->
                    HandledScreenGuiScale.captureWidgetLocalBounds(widget, leftPos, topPos, imageWidth, imageHeight));
            HandledScreenGuiScale.layoutWidget(widget, base[0], base[1], base[2], base[3], leftPos, topPos, imageWidth, imageHeight);
        }
    }

    @Unique
    private int dupeclient$scalePushes;

    @Unique
    private void dupeclient$pushBackgroundScale(GuiGraphicsExtractor context) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenGuiScale.pushScaleScreen(context, leftPos, topPos, imageWidth, imageHeight);
        dupeclient$scalePushes++;
    }

    @Unique
    private void dupeclient$popOneScale(GuiGraphicsExtractor context) {
        if (dupeclient$scalePushes <= 0) {
            return;
        }
        HandledScreenGuiScale.popScale(context);
        dupeclient$scalePushes--;
    }

    @Unique
    private void dupeclient$clearLeakedScales(GuiGraphicsExtractor context) {
        while (dupeclient$scalePushes > 0) {
            HandledScreenGuiScale.popScale(context);
            dupeclient$scalePushes--;
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void dupeclient$syncBeforeBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$syncGuiPosition();
    }

    /** {@link AbstractContainerScreen#renderBg} is abstract; wrap the call from {@link AbstractContainerScreen#renderBackground}. */
    @Inject(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FII)V",
                    shift = At.Shift.BEFORE))
    private void dupeclient$scaleBackgroundHead(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$pushBackgroundScale(context);
    }

    @Inject(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FII)V",
                    shift = At.Shift.AFTER))
    private void dupeclient$scaleBackgroundTail(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$popOneScale(context);
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void dupeclient$syncBeforeMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$syncGuiPosition();
        dupeclient$layoutHandledWidgets();
    }

    @Inject(
            method = "extractContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
                    shift = At.Shift.BEFORE))
    private void dupeclient$scaleSlotsHead(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (HandledScreenGuiScale.isActive()) {
            HandledScreenGuiScale.pushScaleLocal(context, imageWidth, imageHeight);
            dupeclient$scalePushes++;
        }
    }

    @Inject(
            method = "extractContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
                    shift = At.Shift.AFTER))
    private void dupeclient$scaleSlotsTail(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$popOneScale(context);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$clearScaleStackAfterRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$clearLeakedScales(context);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void dupeclient$clearScaleStackAfterBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$clearLeakedScales(context);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        dupeclient$shiftClick = click.hasShiftDown();
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseDragged(
            MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Redirect(
            method = "getHoveredSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z"))
    private boolean dupeclient$scaleIsPointOverSlot(AbstractContainerScreen screen, Slot slot, double pointX, double pointY) {
        if (HandledScreenGuiScale.isActive()) {
            return HandledScreenGuiScale.isPointOverScaledSlot(
                    pointX, pointY, slot, leftPos, topPos, imageWidth, imageHeight);
        }
        double localX = pointX - leftPos;
        double localY = pointY - topPos;
        return localX >= slot.x - 1 && localX < slot.x + 17
                && localY >= slot.y - 1 && localY < slot.y + 17;
    }

    // Scaled GUI: vanilla mis-labels panel clicks as THROW (-999).
    @ModifyVariable(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private ContainerInput dupeclient$correctMisclassifiedThrow(
            ContainerInput actionType, Slot slot, int slotId, int button) {
        if (!HandledScreenGuiScale.isActive() || slot == null) {
            return actionType;
        }
        if (dupeclient$shiftClick) {
            if (actionType == ContainerInput.PICKUP
                    || (actionType == ContainerInput.THROW && slotId == -999)) {
                return ContainerInput.QUICK_MOVE;
            }
            return actionType;
        }
        if (actionType == ContainerInput.THROW && slotId == -999) {
            return ContainerInput.PICKUP;
        }
        return actionType;
    }

    /** Mirror vanilla quick-move stack bookkeeping when we remap shift clicks. */
    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"))
    private void dupeclient$prepareQuickMoveStack(
            Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (!HandledScreenGuiScale.isActive() || actionType != ContainerInput.QUICK_MOVE || slot == null) {
            return;
        }
        lastQuickMoved = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
    }

    // Outside-bounds click + resolved slot => accidental drop without this.
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaleClickOutside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        dupeclient$syncGuiPosition();
        if (getHoveredSlot(mouseX, mouseY) != null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        cir.setReturnValue(HandledScreenGuiScale.isClickOutsideScaled(
                mouseX, mouseY, left, top, imageWidth, imageHeight));
        cir.cancel();
    }

    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scalePointWithinBounds(
            int localX,
            int localY,
            int width,
            int height,
            double pointX,
            double pointY,
            CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        dupeclient$syncGuiPosition();
        cir.setReturnValue(HandledScreenGuiScale.isPointWithinScaledBounds(
                pointX, pointY, localX, localY, width, height, leftPos, topPos, imageWidth, imageHeight));
        cir.cancel();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void dupeclient$clearWidgetLayoutOnClose(CallbackInfo ci) {
        dupeclient$widgetBaseLayout.clear();
        dupeclient$fixedOverlayLayout.clear();
        dupeclient$childCount = -1;
        dupeclient$scalePushes = 0;
        HandledScreenGuiScale.clearPanelSize();
    }
}
