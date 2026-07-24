package com.dupeclient.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Last-sent movement state used by {@link ClientPlayerEntity#sendMovementPackets()}. */
@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor
    double getLastXClient();

    @Mutable
    @Accessor
    void setLastXClient(double x);

    @Accessor
    double getLastYClient();

    @Mutable
    @Accessor
    void setLastYClient(double y);

    @Accessor
    double getLastZClient();

    @Mutable
    @Accessor
    void setLastZClient(double z);

    @Accessor
    float getLastYawClient();

    @Mutable
    @Accessor
    void setLastYawClient(float yaw);

    @Accessor
    float getLastPitchClient();

    @Mutable
    @Accessor
    void setLastPitchClient(float pitch);

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
    int getTicksSinceLastPositionPacketSent();

    @Mutable
    @Accessor
    void setTicksSinceLastPositionPacketSent(int ticks);
}
