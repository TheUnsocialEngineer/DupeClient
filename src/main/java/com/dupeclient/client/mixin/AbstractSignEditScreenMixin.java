package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityKeyResolution;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.security.SecurityTextMarking;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks sign line {@link Text} trees as server-sourced so key-resolution spoofing can run without breaking local UI.
 */
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Shadow
    @Final
    protected SignText text;

    @Inject(method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZ)V", at = @At("TAIL"))
    private void dupeclient$markSignTextNoTitle(SignBlockEntity blockEntity, boolean front, boolean filtered, CallbackInfo ci) {
        dupeclient$markAllLines();
    }

    @Inject(method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void dupeclient$markSignTextWithTitle(SignBlockEntity blockEntity, boolean front, boolean filtered, Text title, CallbackInfo ci) {
        dupeclient$markAllLines();
    }

    @Unique
    private void dupeclient$markAllLines() {
        SignText st = this.text;
        if (st == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Text plain = st.getMessage(i, false);
            if (plain != null) {
                SecurityTextMarking.markServerSourced(plain);
            }
            Text filtered = st.getMessage(i, true);
            if (filtered != null && filtered != plain) {
                SecurityTextMarking.markServerSourced(filtered);
            }
        }
        dupeclient$notifyIfSignKeyProbe();
    }

    @Unique
    private void dupeclient$notifyIfSignKeyProbe() {
        SignText st = this.text;
        if (st == null) {
            return;
        }
        if (!SecurityKeyResolution.inRemoteMultiplayer()) {
            return;
        }
        if (!SecurityManager.INSTANCE.getSettings().keyResolutionProtection) {
            return;
        }
        if (!SecurityKeyResolution.signTextHasKeyResolutionProbe(st)) {
            return;
        }
        SecurityManager.INSTANCE.notifySignEditScreenKeyProbe();
    }
}
