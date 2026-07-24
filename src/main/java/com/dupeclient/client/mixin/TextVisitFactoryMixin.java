package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public abstract class TextVisitFactoryMixin {
    @ModifyVariable(
            method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private static String dupeclient$replaceDisplayedName(String text) {
        SecurityManager manager = SecurityManager.INSTANCE;
        if (manager.getSettings().nameChangerOnlyInGame && MinecraftClient.getInstance().player == null) {
            return text;
        }
        return manager.replaceDisplayedName(text);
    }
}
