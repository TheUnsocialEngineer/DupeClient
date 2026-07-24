package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenGuiScaleMixin {
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    /** Vanilla creative search field (GUI-local). */
    private static final int SEARCH_LOCAL_X = 82;
    private static final int SEARCH_LOCAL_Y = 6;
    private static final int SEARCH_LOCAL_W = 80;
    private static final int SEARCH_LOCAL_H = 9;
    /** Vanilla {@code drawForeground} search label position (GUI-local). */
    private static final int SEARCH_LABEL_LOCAL_X = 10;
    private static final int SEARCH_LABEL_LOCAL_Y = 6;

    @Shadow
    protected EditBox searchBox;

    @Shadow
    private int getTabX(CreativeModeTab group) {
        throw new AssertionError();
    }

    @Shadow
    private int getTabY(CreativeModeTab group) {
        throw new AssertionError();
    }

    @Unique
    private boolean dupeclient$shiftClick;

    @Unique
    private HandledScreenAccessor dupeclient$gui() {
        return (HandledScreenAccessor) this;
    }

    @Unique
    private CreativeInventoryScreenAccessor dupeclient$creative() {
        return (CreativeInventoryScreenAccessor) this;
    }

    @Unique
    private boolean dupeclient$isOverAnySlot(double mouseX, double mouseY) {
        HandledScreenAccessor gui = dupeclient$gui();
        int x = gui.getX();
        int y = gui.getY();
        int bgW = gui.getImageWidth();
        int bgH = gui.getImageHeight();
        for (Slot slot : gui.getMenu().slots) {
            if (HandledScreenGuiScale.isPointOverScaledSlot(mouseX, mouseY, slot, x, y, bgW, bgH)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void dupeclient$layoutSearchBox() {
        if (!HandledScreenGuiScale.isActive() || searchBox == null) {
            return;
        }
        HandledScreenAccessor gui = dupeclient$gui();
        HandledScreenGuiScale.layoutWidget(
                searchBox,
                SEARCH_LOCAL_X,
                SEARCH_LOCAL_Y,
                SEARCH_LOCAL_W,
                SEARCH_LOCAL_H,
                gui.getX(),
                gui.getY(),
                gui.getImageWidth(),
                gui.getImageHeight());
    }

    @Unique
    private boolean dupeclient$isOverSearchBox(double mouseX, double mouseY) {
        if (searchBox == null || !searchBox.active) {
            return false;
        }
        if (HandledScreenGuiScale.isActive()) {
            HandledScreenAccessor gui = dupeclient$gui();
            return HandledScreenGuiScale.isPointWithinScaledBounds(
                    mouseX,
                    mouseY,
                    SEARCH_LOCAL_X,
                    SEARCH_LOCAL_Y,
                    SEARCH_LOCAL_W,
                    SEARCH_LOCAL_H,
                    gui.getX(),
                    gui.getY(),
                    gui.getImageWidth(),
                    gui.getImageHeight());
        }
        int sx = searchBox.getX();
        int sy = searchBox.getY();
        return mouseX >= sx && mouseX < sx + searchBox.getWidth()
                && mouseY >= sy && mouseY < sy + searchBox.getHeight();
    }

    @Unique
    private void dupeclient$syncGuiPosition() {
        HandledScreenAccessor gui = dupeclient$gui();
        Minecraft client = Minecraft.getInstance();
        int[] pos = new int[2];
        HandledScreenGuiScale.syncGuiPosition(
                pos,
                gui.getImageWidth(),
                gui.getImageHeight(),
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight());
        gui.setX(pos[0]);
        gui.setY(pos[1]);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dupeclient$syncBeforeCreativeRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutSearchBox();
        }
    }

    /**
     * Vanilla draws the search field inside {@code drawBackground}'s scale matrix; {@link HandledScreenGuiScaleMixin}
     * already positions it in screen space — defer to {@code render} tail to avoid double scaling.
     */
    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void dupeclient$deferScaledSearchRender(
            EditBox instance, GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!HandledScreenGuiScale.isActive()) {
            instance.extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$drawScaledSearchUi(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!HandledScreenGuiScale.isActive() || searchBox == null || !searchBox.active) {
            return;
        }
        dupeclient$layoutSearchBox();
        searchBox.extractRenderState(context, mouseX, mouseY, delta);
        HandledScreenAccessor gui = dupeclient$gui();
        int guiLeft = gui.getX();
        int guiTop = gui.getY();
        int bgW = gui.getImageWidth();
        int bgH = gui.getImageHeight();
        Minecraft client = Minecraft.getInstance();
        Font tr = client.font;
        Component label = Component.translatable("itemGroup.search");
        int labelW = tr.width(label);
        int gap = Math.max(3, Math.round(4.0f * HandledScreenGuiScale.getScale()));
        int lx = searchBox.getX() - labelW - gap;
        int ly = searchBox.getY() + (searchBox.getHeight() - 8) / 2;
        float[] oldTopLeft = HandledScreenGuiScale.localToScreen(
                SEARCH_LABEL_LOCAL_X, SEARCH_LABEL_LOCAL_Y, guiLeft, guiTop, bgW, bgH);
        int coverRight = Math.max(searchBox.getX(), (int) oldTopLeft[0] + labelW + gap);
        int coverTop = Math.min(ly, (int) oldTopLeft[1]) - 1;
        int coverBottom = Math.max(ly + 9, (int) oldTopLeft[1] + 10);
        context.fill((int) oldTopLeft[0] - 2, coverTop, coverRight, coverBottom, 0xFFC6C6C6);
        context.text(tr, label, lx, ly, 0xFF404040);
    }

    @Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaleIsClickInTab(CreativeModeTab group, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenAccessor gui = dupeclient$gui();
        dupeclient$syncGuiPosition();
        int x = gui.getX();
        int y = gui.getY();
        int bgW = gui.getImageWidth();
        int bgH = gui.getImageHeight();
        boolean hit = HandledScreenGuiScale.isCreativeTabHit(
                mouseX, mouseY, getTabX(group), getTabY(group), TAB_WIDTH, TAB_HEIGHT, x, y, bgW, bgH);
        cir.setReturnValue(hit);
        cir.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void dupeclient$syncBeforeCreativeMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        dupeclient$shiftClick = click.hasShiftDown();
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutSearchBox();
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaleCreativeClickOutside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        dupeclient$syncGuiPosition();
        HandledScreenAccessor gui = dupeclient$gui();
        if (dupeclient$isOverAnySlot(mouseX, mouseY) || dupeclient$isOverSearchBox(mouseX, mouseY)) {
            dupeclient$creative().setHasClickedOutside(false);
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        boolean outside = HandledScreenGuiScale.isClickOutsideScaled(
                mouseX, mouseY, left, top, gui.getImageWidth(), gui.getImageHeight());
        if (outside) {
            int x = gui.getX();
            int y = gui.getY();
            int bgW = gui.getImageWidth();
            int bgH = gui.getImageHeight();
            for (CreativeModeTab group : CreativeModeTabs.tabs()) {
                if (HandledScreenGuiScale.isCreativeTabHit(
                        mouseX, mouseY,
                        getTabX(group), getTabY(group),
                        TAB_WIDTH, TAB_HEIGHT,
                        x, y, bgW, bgH)) {
                    outside = false;
                    break;
                }
            }
        }
        dupeclient$creative().setHasClickedOutside(outside);
        cir.setReturnValue(outside);
        cir.cancel();
    }

    @ModifyVariable(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private ContainerInput dupeclient$correctCreativeMisclassifiedThrow(
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

    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"))
    private void dupeclient$prepareCreativeQuickMoveStack(
            Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (!HandledScreenGuiScale.isActive() || actionType != ContainerInput.QUICK_MOVE || slot == null) {
            return;
        }
        dupeclient$gui().setLastQuickMoved(slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void dupeclient$syncBeforeCreativeMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$syncGuiPosition();
            dupeclient$layoutSearchBox();
        }
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void dupeclient$onCreativeResize(CallbackInfo ci) {
        if (HandledScreenGuiScale.isActive()) {
            dupeclient$layoutSearchBox();
        }
    }

    @Inject(method = "insideScrollbar", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaleIsClickInScrollbar(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenAccessor gui = dupeclient$gui();
        dupeclient$syncGuiPosition();
        float[] local = HandledScreenGuiScale.screenToLocal(
                mouseX, mouseY, gui.getX(), gui.getY(), gui.getImageWidth(), gui.getImageHeight());
        boolean hit = local[0] >= 175
                && local[0] < 175 + 14
                && local[1] >= 18
                && local[1] < 18 + 112;
        cir.setReturnValue(hit);
        cir.cancel();
    }
}
