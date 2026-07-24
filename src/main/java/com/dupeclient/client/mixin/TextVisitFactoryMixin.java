package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public abstract class TextVisitFactoryMixin {
    @ModifyVariable(
            method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private static String dupeclient$replaceDisplayedName(String text) {
        SecurityManager manager = SecurityManager.INSTANCE;
        if (manager.getSettings().nameChangerOnlyInGame && Minecraft.getInstance().player == null) {
            return text;
        }
        return manager.replaceDisplayedName(text);
    }
}
