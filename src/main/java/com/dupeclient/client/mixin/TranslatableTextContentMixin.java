package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityFromServerPacket;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

@Mixin(value = TranslatableContents.class, priority = 5000)
public abstract class TranslatableTextContentMixin implements SecurityFromServerPacket {
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

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofStyledVisit(FormattedText.StyledContentConsumer<?> visitor, Style style, CallbackInfoReturnable<Optional<?>> cir) {
        TranslatableContents self = (TranslatableContents) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        String rep = SecurityKeyResolution.replacementForTranslatable(self);
        Optional<?> out = Component.literal(rep).visit(visitor, style);
        cir.setReturnValue(out);
        cir.cancel();
    }

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofPlainVisit(FormattedText.ContentConsumer<?> visitor, CallbackInfoReturnable<Optional<?>> cir) {
        TranslatableContents self = (TranslatableContents) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        String rep = SecurityKeyResolution.replacementForTranslatable(self);
        Optional<?> out = Component.literal(rep).visit(visitor);
        cir.setReturnValue(out);
        cir.cancel();
    }
}
