package com.dupeclient.client.module.packet.sniffer;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;

import com.dupeclient.client.module.packet.PacketUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serialize, build, and replay {@link PlayerMoveC2SPacket} and {@link PlayerInputC2SPacket}. */
public final class PacketMoveCodec {
    private static Boolean hasHorizontalCollisionParam;

    private PacketMoveCodec() {
    }

    public static boolean isPlayerMovePacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket;
    }

    public static boolean isPlayerMoveType(String typeName) {
        return typeName != null && typeName.startsWith("PlayerMoveC2SPacket");
    }

    public static boolean isPlayerInputPacket(Packet<?> packet) {
        return packet instanceof PlayerInputC2SPacket;
    }

    public static boolean isPlayerInputType(String typeName) {
        return "PlayerInputC2SPacket".equals(typeName);
    }

    public static boolean supportsType(String typeName) {
        return isPlayerMoveType(typeName) || isPlayerInputType(typeName);
    }

    public static List<PacketFieldModel> describe(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket move) {
            return describeMove(PacketUtils.getPacketTypeName(packet), move);
        }
        if (packet instanceof PlayerInputC2SPacket input) {
            return describePlayerInput(input);
        }
        return List.of();
    }

    public static List<PacketFieldModel> describeType(String typeName, @org.jetbrains.annotations.Nullable MinecraftClient client) {
        if (isPlayerInputType(typeName)) {
            return describePlayerInputFields(defaultPlayerInput(client));
        }
        if (isPlayerMoveType(typeName)) {
            return describeMove(typeName, defaultMove(typeName, client));
        }
        return List.of();
    }

    public static String toEditable(Packet<?> packet) {
        List<PacketFieldModel> rows = describe(packet);
        if (rows.isEmpty()) {
            return "";
        }
        String type = PacketUtils.getPacketTypeName(packet);
        return buildEditableText(type, rows);
    }

    public static Packet<?> build(String typeName, Map<String, String> fields) throws PacketRecordCodec.PacketBuildException {
        if (isPlayerInputType(typeName)) {
            return buildPlayerInput(fields);
        }
        if (isPlayerMoveType(typeName)) {
            return buildMove(typeName, fields);
        }
        throw new PacketRecordCodec.PacketBuildException("Unsupported movement packet: " + typeName);
    }

    public static void applyClientPrediction(MinecraftClient client, Packet<?> packet) {
        if (client == null || client.player == null || !(packet instanceof PlayerMoveC2SPacket move)) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (move.changesPosition()) {
            player.setPosition(move.getX(player.getX()), move.getY(player.getY()), move.getZ(player.getZ()));
        }
        if (move.changesLook()) {
            player.setYaw(move.getYaw(player.getYaw()));
            player.setPitch(move.getPitch(player.getPitch()));
        }
        player.setOnGround(move.isOnGround());
    }

    private static List<PacketFieldModel> describeMove(String typeName, PlayerMoveC2SPacket move) {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(field("type", "String", typeName, false, String.class));
        if (move.changesPosition() || isPositionType(typeName)) {
            rows.add(field("x", "double", fmt(move.getX(0)), true, double.class));
            rows.add(field("y", "double", fmt(move.getY(0)), true, double.class));
            rows.add(field("z", "double", fmt(move.getZ(0)), true, double.class));
        }
        if (move.changesLook() || isLookType(typeName)) {
            rows.add(field("yaw", "float", fmt(move.getYaw(0)), true, float.class));
            rows.add(field("pitch", "float", fmt(move.getPitch(0)), true, float.class));
        }
        rows.add(field("onGround", "boolean", String.valueOf(move.isOnGround()), true, boolean.class));
        rows.add(field("horizontalCollision", "boolean", String.valueOf(move.horizontalCollision()), true, boolean.class));
        return rows;
    }

    private static List<PacketFieldModel> describePlayerInput(PlayerInputC2SPacket packet) {
        return describePlayerInputFields(packet.input());
    }

    private static List<PacketFieldModel> describePlayerInputFields(PlayerInput input) {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(field("type", "String", "PlayerInputC2SPacket", false, String.class));
        rows.add(field("forward", "boolean", String.valueOf(input.forward()), true, boolean.class));
        rows.add(field("backward", "boolean", String.valueOf(input.backward()), true, boolean.class));
        rows.add(field("left", "boolean", String.valueOf(input.left()), true, boolean.class));
        rows.add(field("right", "boolean", String.valueOf(input.right()), true, boolean.class));
        rows.add(field("jump", "boolean", String.valueOf(input.jump()), true, boolean.class));
        rows.add(field("sneak", "boolean", String.valueOf(input.sneak()), true, boolean.class));
        rows.add(field("sprint", "boolean", String.valueOf(input.sprint()), true, boolean.class));
        return rows;
    }

    private static PlayerMoveC2SPacket defaultMove(String typeName, @org.jetbrains.annotations.Nullable MinecraftClient client) {
        double x = 0;
        double y = 0;
        double z = 0;
        float yaw = 0;
        float pitch = 0;
        boolean onGround = true;
        boolean horizontalCollision = false;
        if (client != null && client.player != null) {
            x = client.player.getX();
            y = client.player.getY();
            z = client.player.getZ();
            yaw = client.player.getYaw();
            pitch = client.player.getPitch();
            onGround = client.player.isOnGround();
            horizontalCollision = client.player.horizontalCollision;
        }
        return switch (typeName) {
            case "PlayerMoveC2SPacket.OnGroundOnly" -> createOnGroundOnly(onGround, horizontalCollision);
            case "PlayerMoveC2SPacket.LookAndOnGround" -> createLookAndOnGround(yaw, pitch, onGround, horizontalCollision);
            case "PlayerMoveC2SPacket.Full" -> createFull(x, y, z, yaw, pitch, onGround, horizontalCollision);
            case "PlayerMoveC2SPacket.PositionAndOnGround" -> createPositionAndOnGround(x, y, z, onGround, horizontalCollision);
            default -> createPositionAndOnGround(x, y, z, onGround, horizontalCollision);
        };
    }

    private static PlayerInput defaultPlayerInput(@org.jetbrains.annotations.Nullable MinecraftClient client) {
        if (client != null && client.player != null && client.player.input != null) {
            var keys = client.player.input.playerInput;
            if (keys != null) {
                return keys;
            }
        }
        return PlayerInput.DEFAULT;
    }

    private static PlayerMoveC2SPacket buildMove(String typeName, Map<String, String> fields)
            throws PacketRecordCodec.PacketBuildException {
        boolean onGround = parseBool(fields, "onGround", true);
        boolean horizontalCollision = parseBool(fields, "horizontalCollision", false);
        return switch (typeName) {
            case "PlayerMoveC2SPacket.OnGroundOnly" -> createOnGroundOnly(onGround, horizontalCollision);
            case "PlayerMoveC2SPacket.LookAndOnGround" -> createLookAndOnGround(
                    parseFloat(fields, "yaw", 0),
                    parseFloat(fields, "pitch", 0),
                    onGround,
                    horizontalCollision);
            case "PlayerMoveC2SPacket.Full" -> createFull(
                    parseDouble(fields, "x", 0),
                    parseDouble(fields, "y", 0),
                    parseDouble(fields, "z", 0),
                    parseFloat(fields, "yaw", 0),
                    parseFloat(fields, "pitch", 0),
                    onGround,
                    horizontalCollision);
            case "PlayerMoveC2SPacket.PositionAndOnGround" -> createPositionAndOnGround(
                    parseDouble(fields, "x", 0),
                    parseDouble(fields, "y", 0),
                    parseDouble(fields, "z", 0),
                    onGround,
                    horizontalCollision);
            default -> throw new PacketRecordCodec.PacketBuildException("Unknown move packet type: " + typeName);
        };
    }

    private static PlayerInputC2SPacket buildPlayerInput(Map<String, String> fields) {
        PlayerInput input = new PlayerInput(
                parseBool(fields, "forward", false),
                parseBool(fields, "backward", false),
                parseBool(fields, "left", false),
                parseBool(fields, "right", false),
                parseBool(fields, "jump", false),
                parseBool(fields, "sneak", false),
                parseBool(fields, "sprint", false));
        return new PlayerInputC2SPacket(input);
    }

    private static PlayerMoveC2SPacket createPositionAndOnGround(
            double x, double y, double z, boolean onGround, boolean horizontalCollision) {
        try {
            if (hasHorizontalCollisionParam()) {
                Constructor<PlayerMoveC2SPacket.PositionAndOnGround> ctor =
                        PlayerMoveC2SPacket.PositionAndOnGround.class.getConstructor(
                                double.class, double.class, double.class, boolean.class, boolean.class);
                return ctor.newInstance(x, y, z, onGround, horizontalCollision);
            }
            Constructor<PlayerMoveC2SPacket.PositionAndOnGround> ctor =
                    PlayerMoveC2SPacket.PositionAndOnGround.class.getConstructor(
                            double.class, double.class, double.class, boolean.class);
            return ctor.newInstance(x, y, z, onGround);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to build PositionAndOnGround packet", e);
        }
    }

    private static PlayerMoveC2SPacket createOnGroundOnly(boolean onGround, boolean horizontalCollision) {
        try {
            if (hasHorizontalCollisionParam()) {
                Constructor<PlayerMoveC2SPacket.OnGroundOnly> ctor =
                        PlayerMoveC2SPacket.OnGroundOnly.class.getConstructor(boolean.class, boolean.class);
                return ctor.newInstance(onGround, horizontalCollision);
            }
            Constructor<PlayerMoveC2SPacket.OnGroundOnly> ctor =
                    PlayerMoveC2SPacket.OnGroundOnly.class.getConstructor(boolean.class);
            return ctor.newInstance(onGround);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to build OnGroundOnly packet", e);
        }
    }

    private static PlayerMoveC2SPacket createLookAndOnGround(
            float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        try {
            if (hasHorizontalCollisionParam()) {
                Constructor<PlayerMoveC2SPacket.LookAndOnGround> ctor =
                        PlayerMoveC2SPacket.LookAndOnGround.class.getConstructor(
                                float.class, float.class, boolean.class, boolean.class);
                return ctor.newInstance(yaw, pitch, onGround, horizontalCollision);
            }
            Constructor<PlayerMoveC2SPacket.LookAndOnGround> ctor =
                    PlayerMoveC2SPacket.LookAndOnGround.class.getConstructor(float.class, float.class, boolean.class);
            return ctor.newInstance(yaw, pitch, onGround);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to build LookAndOnGround packet", e);
        }
    }

    private static PlayerMoveC2SPacket createFull(
            double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        try {
            if (hasHorizontalCollisionParam()) {
                Constructor<PlayerMoveC2SPacket.Full> ctor = PlayerMoveC2SPacket.Full.class.getConstructor(
                        double.class, double.class, double.class, float.class, float.class, boolean.class, boolean.class);
                return ctor.newInstance(x, y, z, yaw, pitch, onGround, horizontalCollision);
            }
            Constructor<PlayerMoveC2SPacket.Full> ctor = PlayerMoveC2SPacket.Full.class.getConstructor(
                    double.class, double.class, double.class, float.class, float.class, boolean.class);
            return ctor.newInstance(x, y, z, yaw, pitch, onGround);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to build Full move packet", e);
        }
    }

    private static boolean hasHorizontalCollisionParam() {
        if (hasHorizontalCollisionParam == null) {
            try {
                PlayerMoveC2SPacket.OnGroundOnly.class.getConstructor(boolean.class, boolean.class);
                hasHorizontalCollisionParam = true;
            } catch (NoSuchMethodException e) {
                hasHorizontalCollisionParam = false;
            }
        }
        return hasHorizontalCollisionParam;
    }

    private static boolean isPositionType(String typeName) {
        return "PlayerMoveC2SPacket.PositionAndOnGround".equals(typeName)
                || "PlayerMoveC2SPacket.Full".equals(typeName);
    }

    private static boolean isLookType(String typeName) {
        return "PlayerMoveC2SPacket.LookAndOnGround".equals(typeName)
                || "PlayerMoveC2SPacket.Full".equals(typeName);
    }

    private static PacketFieldModel field(String name, String type, String value, boolean editable, Class<?> valueType) {
        return new PacketFieldModel(name, type, value, editable, valueType);
    }

    private static String buildEditableText(String type, List<PacketFieldModel> rows) {
        Map<String, String> map = new LinkedHashMap<>();
        for (PacketFieldModel row : rows) {
            if (!"type".equals(row.name)) {
                map.put(row.name, row.value);
            }
        }
        StringBuilder sb = new StringBuilder("type=").append(type).append('\n');
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return sb.toString().trim();
    }

    private static String fmt(double value) {
        return Double.toString(value);
    }

    private static String fmt(float value) {
        return Float.toString(value);
    }

    private static boolean parseBool(Map<String, String> fields, String key, boolean fallback) {
        String raw = fields.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static double parseDouble(Map<String, String> fields, String key, double fallback)
            throws PacketRecordCodec.PacketBuildException {
        String raw = fields.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new PacketRecordCodec.PacketBuildException("Invalid double for " + key + ": " + raw);
        }
    }

    private static float parseFloat(Map<String, String> fields, String key, float fallback)
            throws PacketRecordCodec.PacketBuildException {
        String raw = fields.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            throw new PacketRecordCodec.PacketBuildException("Invalid float for " + key + ": " + raw);
        }
    }
}
