package com.dupeclient.client.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Keyboard.class)
public interface KeyboardInvoker {
    @Invoker("onKey")
    void dupeclient$invokeOnKey(long window, int action, KeyInput input);

    @Invoker("onChar")
    void dupeclient$invokeOnChar(long window, CharInput input);
}
