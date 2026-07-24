package com.dupeclient.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Last-sent movement state used by {@link LocalPlayer#sendPosition()}. */
@Mixin(LocalPlayer.class)
public interface ClientPlayerEntityAccessor {
    @Accessor
    double getXLast();

    @Mutable
    @Accessor
    void setXLast(double x);

    @Accessor
    double getYLast();

    @Mutable
    @Accessor
    void setYLast(double y);

    @Accessor
    double getZLast();

    @Mutable
    @Accessor
    void setZLast(double z);

    @Accessor
    float getYRotLast();

    @Mutable
    @Accessor
    void setYRotLast(float yaw);

    @Accessor
    float getXRotLast();

    @Mutable
    @Accessor
    void setXRotLast(float pitch);

    @Accessor
    boolean getLastOnGround();

    @Mutable
    @Accessor
    void setLastOnGround(boolean onGround);

    @Accessor
    boolean getLastHorizontalCollision();

    @Mutable
    @Accessor
    void setLastHorizontalCollision(boolean horizontalCollision);

    @Accessor
    int getPositionReminder();

    @Mutable
    @Accessor
    void setPositionReminder(int ticks);
}
