package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityFromServerPacket;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import com.dupeclient.client.module.security.SecurityPacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Spoofs keybind resolution for probes by delegating to a marked {@link Text#translatable} tree so JSON fallbacks
 * match vanilla semantics (see OpSec {@code KeybindContentsMixin}).
 */
@Mixin(value = KeybindTextContent.class, priority = 5000)
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
        MinecraftClient c = MinecraftClient.getInstance();
        if (c != null && !c.isOnThread() && key != null && SecurityKeyResolution.shouldSpoofTranslationKey(key)) {
            this.dupeclient$fromServerPacket = true;
        }
    }

    // getTranslated() bypasses visit(); spoof here too.
    @Inject(method = "getTranslated", at = @At("HEAD"), cancellable = true)
    private void dupeClient$spoofGetTranslated(CallbackInfoReturnable<Text> cir) {
        KeybindTextContent self = (KeybindTextContent) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        cir.setReturnValue(Text.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(self.getKey())));
    }

    @Inject(
            method = "visit(Lnet/minecraft/text/StringVisitable$StyledVisitor;Lnet/minecraft/text/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofStyledVisit(StringVisitable.StyledVisitor<?> visitor, Style style, CallbackInfoReturnable<Optional<?>> cir) {
        KeybindTextContent self = (KeybindTextContent) (Object) this;
        String id = self.getKey();
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        // Never use Text.translatable(id) here: that re-resolves mod keybinds to real keys (e.g. Meteor) on signs.
        MutableText inner = Text.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(id));
        Optional<?> out = inner.visit(visitor, style);
        cir.setReturnValue(out);
        cir.cancel();
    }

    @Inject(
            method = "visit(Lnet/minecraft/text/StringVisitable$Visitor;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofPlainVisit(StringVisitable.Visitor<?> visitor, CallbackInfoReturnable<Optional<?>> cir) {
        KeybindTextContent self = (KeybindTextContent) (Object) this;
        String id = self.getKey();
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        MutableText inner = Text.literal(SecurityKeyResolution.displayStringForSpoofedKeybind(id));
        Optional<?> out = inner.visit(visitor);
        cir.setReturnValue(out);
        cir.cancel();
    }
}
