package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityFromServerPacket;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import com.dupeclient.client.module.security.SecurityPacketContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindContents;

/**
 * Spoofs keybind resolution for probes by delegating to a marked {@link Component#translatable} tree so JSON fallbacks
 * match vanilla semantics (see OpSec {@code KeybindContentsMixin}).
 */
@Mixin(value = KeybindContents.class, priority = 5000)
public abstract class KeybindTextContentMixin implements SecurityFromServerPacket {
    @Unique
    private boolean dupeclient$fromServerPacket;

    @Override
    public void dupeclient$setFromServerPacket(boolean fromServer) {
        this.dupeclient$fromServerPacket = fromServer;
    }

    @Override
    public boolean dupeclient$isFromServerPacket() {
        return this.dupeclient$fromServerPacket;
    }

    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("TAIL"))
    private void dupeclient$onCreated(String key, CallbackInfo ci) {
        if (SecurityPacketContext.isProcessingPacket()) {
            this.dupeclient$fromServerPacket = true;
            return;
        }
        // Network thread may decode keybinds before handlePacket.
        Minecraft c = Minecraft.getInstance();
        if (c != null && !c.isSameThread() && key != null && SecurityKeyResolution.shouldSpoofTranslationKey(key)) {
            this.dupeclient$fromServerPacket = true;
        }
    }

    // getTranslated() bypasses visit(); spoof here too.
    @Inject(method = "getNestedComponent", at = @At("HEAD"), cancellable = true)
    private void dupeClient$spoofGetTranslated(CallbackInfoReturnable<Component> cir) {
        KeybindContents self = (KeybindContents) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        cir.setReturnValue(Component.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(self.getName())));
    }

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofStyledVisit(FormattedText.StyledContentConsumer<?> visitor, Style style, CallbackInfoReturnable<Optional<?>> cir) {
        KeybindContents self = (KeybindContents) (Object) this;
        String id = self.getName();
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        // Never use Text.translatable(id) here: that re-resolves mod keybinds to real keys (e.g. Meteor) on signs.
        MutableComponent inner = Component.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(id));
        Optional<?> out = inner.visit(visitor, style);
        cir.setReturnValue(out);
        cir.cancel();
    }

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofPlainVisit(FormattedText.ContentConsumer<?> visitor, CallbackInfoReturnable<Optional<?>> cir) {
        KeybindContents self = (KeybindContents) (Object) this;
        String id = self.getName();
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        MutableComponent inner = Component.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(id));
        Optional<?> out = inner.visit(visitor);
        cir.setReturnValue(out);
        cir.cancel();
    }
}
