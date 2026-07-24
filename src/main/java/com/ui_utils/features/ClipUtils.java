package com.ui_utils.features;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;

public class ClipUtils {
    private static Boolean hasHorizontalCollisionParam = null;
    private static Boolean hasFromVehicleMethod = null;

    private static boolean hasHorizontalCollisionParam() {
        if (hasHorizontalCollisionParam == null) {
            try {
                ServerboundMovePlayerPacket.StatusOnly.class.getConstructor(Boolean.TYPE, Boolean.TYPE);
                hasHorizontalCollisionParam = true;
            }
            catch (NoSuchMethodException e) {
                hasHorizontalCollisionParam = false;
            }
        }
        return hasHorizontalCollisionParam;
    }

    private static boolean hasFromVehicleMethod() {
        if (hasFromVehicleMethod == null) {
            try {
                ServerboundMoveVehiclePacket.class.getMethod("fromVehicle", Entity.class);
                hasFromVehicleMethod = true;
            }
            catch (NoSuchMethodException e) {
                hasFromVehicleMethod = false;
            }
        }
        return hasFromVehicleMethod;
    }

    private static ServerboundMoveVehiclePacket createVehiclePacket(Entity vehicle) {
        try {
            if (ClipUtils.hasFromVehicleMethod()) {
                Method method = ServerboundMoveVehiclePacket.class.getMethod("fromVehicle", Entity.class);
                return (ServerboundMoveVehiclePacket)method.invoke(null, vehicle);
            }
            Constructor constructor = ServerboundMoveVehiclePacket.class.getConstructor(Entity.class);
            return (ServerboundMoveVehiclePacket)constructor.newInstance(vehicle);
        }
        catch (Exception e) {
            try {
                Constructor constructor = ServerboundMoveVehiclePacket.class.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
                return (ServerboundMoveVehiclePacket)constructor.newInstance(vehicle.getX(), vehicle.getY(), vehicle.getZ(), Float.valueOf(vehicle.getYRot()), Float.valueOf(vehicle.getXRot()));
            }
            catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }
    }

    private static ServerboundMovePlayerPacket createOnGroundPacket(boolean onGround, boolean horizontalCollision) {
        try {
            if (ClipUtils.hasHorizontalCollisionParam()) {
                Constructor constructor = ServerboundMovePlayerPacket.StatusOnly.class.getConstructor(Boolean.TYPE, Boolean.TYPE);
                return (ServerboundMovePlayerPacket)constructor.newInstance(onGround, horizontalCollision);
            }
            Constructor constructor = ServerboundMovePlayerPacket.StatusOnly.class.getConstructor(Boolean.TYPE);
            return (ServerboundMovePlayerPacket)constructor.newInstance(onGround);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ServerboundMovePlayerPacket createPositionPacket(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
        try {
            if (ClipUtils.hasHorizontalCollisionParam()) {
                Constructor constructor = ServerboundMovePlayerPacket.Pos.class.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE, Boolean.TYPE);
                return (ServerboundMovePlayerPacket)constructor.newInstance(x, y, z, onGround, horizontalCollision);
            }
            Constructor constructor = ServerboundMovePlayerPacket.Pos.class.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE);
            return (ServerboundMovePlayerPacket)constructor.newInstance(x, y, z, onGround);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void vClip(double blocks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        int packetsRequired = (int)Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) {
            packetsRequired = 1;
        }
        if (mc.player.isPassenger()) {
            for (int i = 0; i < packetsRequired - 1; ++i) {
                ServerboundMoveVehiclePacket packet = ClipUtils.createVehiclePacket(mc.player.getVehicle());
                if (packet == null) continue;
                mc.getConnection().send((Packet)packet);
            }
            mc.player.getVehicle().setPos(mc.player.getVehicle().getX(), mc.player.getVehicle().getY() + blocks, mc.player.getVehicle().getZ());
            ServerboundMoveVehiclePacket packet = ClipUtils.createVehiclePacket(mc.player.getVehicle());
            if (packet != null) {
                mc.getConnection().send((Packet)packet);
            }
        } else {
            for (int i = 0; i < packetsRequired - 1; ++i) {
                ServerboundMovePlayerPacket packet = ClipUtils.createOnGroundPacket(true, mc.player.horizontalCollision);
                if (packet == null) continue;
                mc.getConnection().send((Packet)packet);
            }
            ServerboundMovePlayerPacket packet = ClipUtils.createPositionPacket(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ(), true, mc.player.horizontalCollision);
            if (packet != null) {
                mc.getConnection().send((Packet)packet);
            }
            mc.player.setPos(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
        }
    }

    public static void hClip(double blocks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        int packetsRequired = (int)Math.ceil(Math.abs(blocks / 10.0));
        if (packetsRequired > 20) {
            packetsRequired = 1;
        }
        float yaw = mc.player.getYRot();
        double radians = Math.toRadians(yaw);
        double deltaX = -Math.sin(radians) * blocks;
        double deltaZ = Math.cos(radians) * blocks;
        if (mc.player.isPassenger()) {
            for (int i = 0; i < packetsRequired - 1; ++i) {
                ServerboundMoveVehiclePacket packet = ClipUtils.createVehiclePacket(mc.player.getVehicle());
                if (packet == null) continue;
                mc.getConnection().send((Packet)packet);
            }
            mc.player.getVehicle().setPos(mc.player.getVehicle().getX() + deltaX, mc.player.getVehicle().getY(), mc.player.getVehicle().getZ() + deltaZ);
            ServerboundMoveVehiclePacket packet = ClipUtils.createVehiclePacket(mc.player.getVehicle());
            if (packet != null) {
                mc.getConnection().send((Packet)packet);
            }
        } else {
            for (int i = 0; i < packetsRequired - 1; ++i) {
                ServerboundMovePlayerPacket packet = ClipUtils.createOnGroundPacket(true, mc.player.horizontalCollision);
                if (packet == null) continue;
                mc.getConnection().send((Packet)packet);
            }
            ServerboundMovePlayerPacket packet = ClipUtils.createPositionPacket(mc.player.getX() + deltaX, mc.player.getY(), mc.player.getZ() + deltaZ, true, mc.player.horizontalCollision);
            if (packet != null) {
                mc.getConnection().send((Packet)packet);
            }
            mc.player.setPos(mc.player.getX() + deltaX, mc.player.getY(), mc.player.getZ() + deltaZ);
        }
    }
}

