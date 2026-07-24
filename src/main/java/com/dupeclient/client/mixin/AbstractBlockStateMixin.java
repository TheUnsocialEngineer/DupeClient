package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {
    @Unique
    private static final long UNIFORM_RENDERING_SEED = 67L;

    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void dupeclient$uniformRenderingSeed(BlockPos pos, CallbackInfoReturnable<Long> cir) {
        if (SecurityManager.INSTANCE.getSettings().noTextureRotations) {
            cir.setReturnValue(UNIFORM_RENDERING_SEED);
        }
    }
}
