package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityFromServerPacket;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = TranslatableTextContent.class, priority = 5000)
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
            method = "visit(Lnet/minecraft/text/StringVisitable$StyledVisitor;Lnet/minecraft/text/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofStyledVisit(StringVisitable.StyledVisitor<?> visitor, Style style, CallbackInfoReturnable<Optional<?>> cir) {
        TranslatableTextContent self = (TranslatableTextContent) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        String rep = SecurityKeyResolution.replacementForTranslatable(self);
        Optional<?> out = Text.literal(rep).visit(visitor, style);
        cir.setReturnValue(out);
        cir.cancel();
    }

    @Inject(
            method = "visit(Lnet/minecraft/text/StringVisitable$Visitor;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dupeClient$spoofPlainVisit(StringVisitable.Visitor<?> visitor, CallbackInfoReturnable<Optional<?>> cir) {
        TranslatableTextContent self = (TranslatableTextContent) (Object) this;
        if (!SecurityKeyResolution.shouldApplySpoof(self)) {
            return;
        }
        String rep = SecurityKeyResolution.replacementForTranslatable(self);
        Optional<?> out = Text.literal(rep).visit(visitor);
        cir.setReturnValue(out);
        cir.cancel();
    }
}
