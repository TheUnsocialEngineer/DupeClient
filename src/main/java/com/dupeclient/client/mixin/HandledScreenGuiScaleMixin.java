package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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

@Mixin(HandledScreen.class)
public abstract class HandledScreenGuiScaleMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;

    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double x, double y);

    @Shadow
    private ItemStack quickMovingStack;

    @Unique
    private boolean dupeclient$shiftClick;
    @Unique
    private final Map<Element, int[]> dupeclient$widgetBaseLayout = new HashMap<>();
    @Unique
    private final Map<Element, int[]> dupeclient$fixedOverlayLayout = new HashMap<>();
    @Unique
    private int dupeclient$lastSyncedX = Integer.MIN_VALUE;
    @Unique
    private int dupeclient$lastSyncedY = Integer.MIN_VALUE;

    @Unique
    private void dupeclient$syncGuiPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        int[] pos = new int[2];
        HandledScreenGuiScale.syncGuiPosition(
                pos,
                backgroundWidth,
                backgroundHeight,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight());
        HandledScreenGuiScale.bindPanelSize(backgroundWidth, backgroundHeight);
        if (pos[0] != dupeclient$lastSyncedX || pos[1] != dupeclient$lastSyncedY) {
            dupeclient$widgetBaseLayout.clear();
            dupeclient$lastSyncedX = pos[0];
            dupeclient$lastSyncedY = pos[1];
        }
        x = pos[0];
        y = pos[1];
    }

    @Unique
    private int dupeclient$childCount = -1;

    @Unique
    private boolean dupeclient$skipWidgetLayout(Element child) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        return self instanceof CreativeInventoryScreen
                && child instanceof TextFieldWidget
                && HandledScreenGuiScale.isActive();
    }

    @Unique
    private void dupeclient$layoutHandledWidgets() {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        int count = self.children().size();
        if (count != dupeclient$childCount) {
            dupeclient$widgetBaseLayout.keySet().removeIf(child -> !self.children().contains(child));
            dupeclient$childCount = count;
        }
        for (Element child : self.children()) {
            if (!(child instanceof ClickableWidget widget)) {
                continue;
            }
            if (HandledScreenGuiScale.isScreenFixedOverlayWidget(child)) {
                HandledScreenGuiScale.pinScreenFixedWidget(widget, dupeclient$fixedOverlayLayout);
                continue;
            }
            if (dupeclient$skipWidgetLayout(child)) {
                continue;
            }
            if (!HandledScreenGuiScale.shouldScaleWithPanel(child, x, y, backgroundWidth, backgroundHeight)) {
                continue;
            }
            int[] base = dupeclient$widgetBaseLayout.computeIfAbsent(child, ignored ->
                    HandledScreenGuiScale.captureWidgetLocalBounds(widget, x, y, backgroundWidth, backgroundHeight));
            HandledScreenGuiScale.layoutWidget(widget, base[0], base[1], base[2], base[3], x, y, backgroundWidth, backgroundHeight);
        }
    }

    @Unique
    private int dupeclient$scalePushes;

    @Unique
    private void dupeclient$pushBackgroundScale(DrawContext context) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenGuiScale.pushScaleScreen(context, x, y, backgroundWidth, backgroundHeight);
        dupeclient$scalePushes++;
    }

    @Unique
    private void dupeclient$popOneScale(DrawContext context) {
        if (dupeclient$scalePushes <= 0) {
            return;
        }
        HandledScreenGuiScale.popScale(context);
        dupeclient$scalePushes--;
    }

    @Unique
    private void dupeclient$clearLeakedScales(DrawContext context) {
        while (dupeclient$scalePushes > 0) {
            HandledScreenGuiScale.popScale(context);
            dupeclient$scalePushes--;
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void dupeclient$syncBeforeBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$syncGuiPosition();
    }

    /** {@link HandledScreen#drawBackground} is abstract; wrap the call from {@link HandledScreen#renderBackground}. */
    @Inject(
            method = "renderBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
                    shift = At.Shift.BEFORE))
    private void dupeclient$scaleBackgroundHead(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$pushBackgroundScale(context);
    }

    @Inject(
            method = "renderBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
                    shift = At.Shift.AFTER))
    private void dupeclient$scaleBackgroundTail(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$popOneScale(context);
    }

    @Inject(method = "renderMain", at = @At("HEAD"))
    private void dupeclient$syncBeforeMain(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$syncGuiPosition();
        dupeclient$layoutHandledWidgets();
    }

    @Inject(
            method = "renderMain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
                    shift = At.Shift.BEFORE))
    private void dupeclient$scaleSlotsHead(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (HandledScreenGuiScale.isActive()) {
            HandledScreenGuiScale.pushScaleLocal(context, backgroundWidth, backgroundHeight);
            dupeclient$scalePushes++;
        }
    }

    @Inject(
            method = "renderMain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlightFront(Lnet/minecraft/client/gui/DrawContext;)V",
                    shift = At.Shift.AFTER))
    private void dupeclient$scaleSlotsTail(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$popOneScale(context);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$clearScaleStackAfterRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$clearLeakedScales(context);
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void dupeclient$clearScaleStackAfterBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        dupeclient$clearLeakedScales(context);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        dupeclient$shiftClick = click.hasShift();
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void dupeclient$syncBeforeMouseDragged(
            Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutHandledWidgets();
        }
    }

    @Redirect(
            method = "getSlotAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;isPointOverSlot(Lnet/minecraft/screen/slot/Slot;DD)Z"))
    private boolean dupeclient$scaleIsPointOverSlot(HandledScreen screen, Slot slot, double pointX, double pointY) {
        if (HandledScreenGuiScale.isActive()) {
            return HandledScreenGuiScale.isPointOverScaledSlot(
                    pointX, pointY, slot, x, y, backgroundWidth, backgroundHeight);
        }
        double localX = pointX - x;
        double localY = pointY - y;
        return localX >= slot.x - 1 && localX < slot.x + 17
                && localY >= slot.y - 1 && localY < slot.y + 17;
    }

    // Scaled GUI: vanilla mis-labels panel clicks as THROW (-999).
    @ModifyVariable(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private SlotActionType dupeclient$correctMisclassifiedThrow(
            SlotActionType actionType, Slot slot, int slotId, int button) {
        if (!HandledScreenGuiScale.isActive() || slot == null) {
            return actionType;
        }
        if (dupeclient$shiftClick) {
            if (actionType == SlotActionType.PICKUP
                    || (actionType == SlotActionType.THROW && slotId == -999)) {
                return SlotActionType.QUICK_MOVE;
            }
            return actionType;
        }
        if (actionType == SlotActionType.THROW && slotId == -999) {
            return SlotActionType.PICKUP;
        }
        return actionType;
    }

    /** Mirror vanilla quick-move stack bookkeeping when we remap shift clicks. */
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"))
    private void dupeclient$prepareQuickMoveStack(
            Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!HandledScreenGuiScale.isActive() || actionType != SlotActionType.QUICK_MOVE || slot == null) {
            return;
        }
        quickMovingStack = slot.hasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
    }

    // Outside-bounds click + resolved slot => accidental drop without this.
    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
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
        if (getSlotAt(mouseX, mouseY) != null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        cir.setReturnValue(HandledScreenGuiScale.isClickOutsideScaled(
                mouseX, mouseY, left, top, backgroundWidth, backgroundHeight));
        cir.cancel();
    }

    @Inject(method = "isPointWithinBounds", at = @At("HEAD"), cancellable = true)
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
                pointX, pointY, localX, localY, width, height, x, y, backgroundWidth, backgroundHeight));
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
