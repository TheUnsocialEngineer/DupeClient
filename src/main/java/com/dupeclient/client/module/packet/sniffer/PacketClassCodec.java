package com.dupeclient.client.module.packet.sniffer;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;
import com.dupeclient.client.module.packet.sniffer.PacketFieldModel;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;

import com.dupeclient.client.module.packet.PacketUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

/**
 * Builds and describes C2S packets that are plain classes (not records), via public constructors or
 * a small set of custom factories.
 */
public final class PacketClassCodec {
    private static final Map<Class<?>, Descriptor> DESCRIPTORS = new ConcurrentHashMap<>();

    private PacketClassCodec() {
    }

    public static boolean supports(Class<? extends Packet<?>> clazz) {
        if (clazz.isRecord() || Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        if (ServerboundMovePlayerPacket.class.equals(clazz)) {
            return false;
        }
        return resolve(clazz) != null;
    }

    @SuppressWarnings("unchecked")
    public static List<PacketFieldModel> describe(Packet<?> packet) {
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        Descriptor descriptor = resolve(clazz);
        if (descriptor == null) {
            return List.of();
        }
        if (descriptor.singleton) {
            return singletonFields(PacketUtils.getPacketTypeName(packet));
        }
        if (descriptor.custom == CustomKind.PLAYER_INTERACT_ENTITY) {
            return describePlayerInteractEntity((ServerboundInteractPacket) packet);
        }
        if (descriptor.custom == CustomKind.CLIENT_COMMAND) {
            return describeClientCommand((ServerboundPlayerCommandPacket) packet);
        }
        if (descriptor.custom == CustomKind.PLAYER_ABILITIES) {
            return describePlayerAbilities((ServerboundPlayerAbilitiesPacket) packet);
        }
        if (descriptor.custom == CustomKind.UPDATE_SIGN) {
            return describeUpdateSign((ServerboundSignUpdatePacket) packet);
        }
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(PacketUtils.getPacketTypeName(packet)));
        for (Binding binding : descriptor.bindings) {
            rows.add(binding.toModel(readBinding(packet, binding)));
        }
        return rows;
    }

    public static List<PacketFieldModel> describeType(
            String typeName,
            Class<? extends Packet<?>> clazz,
            @Nullable Minecraft client) {
        Descriptor descriptor = resolve(clazz);
        if (descriptor == null) {
            return List.of();
        }
        if (descriptor.singleton) {
            return singletonFields(typeName);
        }
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(typeName));
        for (Binding binding : descriptor.bindings) {
            rows.add(binding.toModel(PacketRecordCodec.defaultField(binding.type, binding.genericType)));
        }
        return rows;
    }

    public static Packet<?> build(
            Class<? extends Packet<?>> clazz,
            Map<String, String> fields,
            @Nullable Minecraft client) throws PacketRecordCodec.PacketBuildException {
        Descriptor descriptor = resolve(clazz);
        if (descriptor == null) {
            throw new PacketRecordCodec.PacketBuildException("No class builder for " + clazz.getSimpleName());
        }
        if (descriptor.singleton) {
            Packet<?> instance = singletonInstance(clazz);
            if (instance == null) {
                throw new PacketRecordCodec.PacketBuildException("Missing singleton for " + clazz.getSimpleName());
            }
            return instance;
        }
        return switch (descriptor.custom) {
            case PLAYER_INTERACT_ENTITY -> buildPlayerInteractEntity(fields, client);
            case CLIENT_COMMAND -> buildClientCommand(fields, client);
            case PLAYER_ABILITIES -> buildPlayerAbilities(fields);
            case UPDATE_SIGN -> buildUpdateSign(fields);
            case NONE -> buildFromConstructor(clazz, descriptor, fields, client);
        };
    }

    private static Packet<?> buildFromConstructor(
            Class<? extends Packet<?>> clazz,
            Descriptor descriptor,
            Map<String, String> fields,
            @Nullable Minecraft client) throws PacketRecordCodec.PacketBuildException {
        Object[] args = new Object[descriptor.bindings.length];
        for (int i = 0; i < descriptor.bindings.length; i++) {
            Binding binding = descriptor.bindings[i];
            args[i] = decodeBinding(binding, fields, client);
        }
        try {
            return (Packet<?>) descriptor.constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new PacketRecordCodec.PacketBuildException("Build failed: " + e.getMessage());
        }
    }

    private static Object decodeBinding(
            Binding binding,
            Map<String, String> fields,
            @Nullable Minecraft client) throws PacketRecordCodec.PacketBuildException {
        if (binding.type == Entity.class) {
            return resolveEntity(fields, client);
        }
        String raw = fields.get(binding.name);
        if (raw == null) {
            throw new PacketRecordCodec.PacketBuildException("Missing field: " + binding.name);
        }
        return PacketRecordCodec.decodeField(binding.type, binding.genericType, raw);
    }

    private static Entity resolveEntity(Map<String, String> fields, @Nullable Minecraft client)
            throws PacketRecordCodec.PacketBuildException {
        String raw = fields.get("entityId");
        if (raw == null || raw.isBlank()) {
            throw new PacketRecordCodec.PacketBuildException("Missing field: entityId");
        }
        int entityId = Integer.parseInt(raw.trim());
        if (client == null || client.level == null) {
            throw new PacketRecordCodec.PacketBuildException("Join a world to resolve entityId " + entityId);
        }
        Entity entity = client.level.getEntity(entityId);
        if (entity == null) {
            throw new PacketRecordCodec.PacketBuildException("Unknown entity id: " + entityId);
        }
        return entity;
    }

    private static @Nullable Descriptor resolve(Class<? extends Packet<?>> clazz) {
        return DESCRIPTORS.computeIfAbsent(clazz, PacketClassCodec::buildDescriptor);
    }

    private static @Nullable Descriptor buildDescriptor(Class<?> clazz) {
        CustomKind custom = customKind(clazz);
        if (custom != CustomKind.NONE) {
            return new Descriptor(custom, null, bindingsForCustom(custom), false);
        }
        Packet<?> singleton = singletonInstance(clazz);
        if (singleton != null) {
            return new Descriptor(CustomKind.NONE, null, Binding.EMPTY, true);
        }
        Constructor<?> ctor = pickConstructor(clazz);
        if (ctor == null) {
            return null;
        }
        Binding[] bindings = bindConstructor(clazz, ctor);
        if (bindings == null) {
            return null;
        }
        return new Descriptor(CustomKind.NONE, ctor, bindings, false);
    }

    private static CustomKind customKind(Class<?> clazz) {
        if (ServerboundInteractPacket.class.equals(clazz)) {
            return CustomKind.PLAYER_INTERACT_ENTITY;
        }
        if (ServerboundPlayerCommandPacket.class.equals(clazz)) {
            return CustomKind.CLIENT_COMMAND;
        }
        if (ServerboundPlayerAbilitiesPacket.class.equals(clazz)) {
            return CustomKind.PLAYER_ABILITIES;
        }
        if (ServerboundSignUpdatePacket.class.equals(clazz)) {
            return CustomKind.UPDATE_SIGN;
        }
        return CustomKind.NONE;
    }

    private static Binding[] bindingsForCustom(CustomKind kind) {
        return switch (kind) {
            case PLAYER_INTERACT_ENTITY -> new Binding[] {
                    new Binding("interactType", String.class, null, null),
                    new Binding("entityId", int.class, null, null),
                    new Binding("sneaking", boolean.class, null, null),
                    new Binding("hand", InteractionHand.class, null, null),
                    new Binding("targetX", double.class, null, null),
                    new Binding("targetY", double.class, null, null),
                    new Binding("targetZ", double.class, null, null),
            };
            case CLIENT_COMMAND -> new Binding[] {
                    new Binding("entityId", int.class, null, null),
                    new Binding("mode", ServerboundPlayerCommandPacket.Action.class, null, null),
                    new Binding("mountJumpHeight", int.class, null, null),
            };
            case PLAYER_ABILITIES -> new Binding[] {
                    new Binding("invulnerable", boolean.class, null, null),
                    new Binding("flying", boolean.class, null, null),
                    new Binding("allowFlying", boolean.class, null, null),
                    new Binding("creativeMode", boolean.class, null, null),
                    new Binding("allowModifyWorld", boolean.class, null, null),
            };
            case UPDATE_SIGN -> new Binding[] {
                    new Binding("pos", net.minecraft.core.BlockPos.class, null, null),
                    new Binding("front", boolean.class, null, null),
                    new Binding("line1", String.class, null, null),
                    new Binding("line2", String.class, null, null),
                    new Binding("line3", String.class, null, null),
                    new Binding("line4", String.class, null, null),
            };
            default -> Binding.EMPTY;
        };
    }

    private static @Nullable Constructor<?> pickConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getConstructors();
        Constructor<?> best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Constructor<?> ctor : ctors) {
            if (ctor.getDeclaringClass() != clazz) {
                continue;
            }
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 0) {
                continue;
            }
            if (containsUnsupportedParam(params)) {
                continue;
            }
            int score = params.length;
            if (containsEntity(params)) {
                score -= 100;
            }
            if (score > bestScore) {
                bestScore = score;
                best = ctor;
            }
        }
        return best;
    }

    private static boolean containsEntity(Class<?>[] params) {
        for (Class<?> param : params) {
            if (param == Entity.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsUnsupportedParam(Class<?>[] params) {
        for (Class<?> param : params) {
            if (!PacketRecordCodec.supportsFieldType(param, null)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Binding[] bindConstructor(Class<?> clazz, Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        Parameter[] parameters = ctor.getParameters();
        Binding[] bindings = new Binding[params.length];
        List<Method> getters = getterMethods(clazz);
        boolean[] used = new boolean[getters.size()];
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i];
            Method getter = matchGetter(getters, used, paramType);
            String name = null;
            if (parameters.length > i && parameters[i].isNamePresent()) {
                String paramName = parameters[i].getName();
                if (!paramName.matches("arg\\d+")) {
                    name = MappingLabelResolver.resolveFieldName(clazz, paramName);
                }
            }
            if (name == null && getter != null) {
                name = getterToName(getter, clazz);
            } else if (name == null && paramType == Entity.class) {
                name = "entityId";
            } else if (name == null) {
                name = fallbackParamName(clazz, paramType, i);
            }
            bindings[i] = new Binding(name, paramType, null, getter);
        }
        return bindings;
    }

    private static String fallbackParamName(Class<?> clazz, Class<?> paramType, int index) {
        String simple = clazz.getSimpleName();
        if (paramType == int.class || paramType == Integer.class) {
            if (simple.contains("Minecart") || simple.contains("Entity") || simple.contains("Command")) {
                return "entityId";
            }
            if (simple.contains("Teleport") || simple.contains("Confirm")) {
                return index == 0 ? "teleportId" : "arg" + index;
            }
            if (simple.contains("Query")) {
                return index == 0 ? "transactionId" : "entityId";
            }
        }
        return "arg" + index;
    }

    private static List<Method> getterMethods(Class<?> clazz) {
        List<Method> out = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            String name = method.getName();
            if (!name.startsWith("get") && !name.startsWith("is")) {
                continue;
            }
            if ("getPacketType".equals(name) || "getClass".equals(name) || "getType".equals(name)) {
                continue;
            }
            out.add(method);
        }
        out.sort(Comparator.comparing(Method::getName));
        return out;
    }

    private static @Nullable Method matchGetter(List<Method> getters, boolean[] used, Class<?> paramType) {
        for (int i = 0; i < getters.size(); i++) {
            if (used[i]) {
                continue;
            }
            Method getter = getters.get(i);
            if (isCompatible(getter.getReturnType(), paramType)) {
                used[i] = true;
                return getter;
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> getterType, Class<?> paramType) {
        if (getterType.equals(paramType)) {
            return true;
        }
        if (paramType.isPrimitive()) {
            if (paramType == int.class && (getterType == Integer.class || getterType == int.class)) {
                return true;
            }
            if (paramType == boolean.class && (getterType == Boolean.class || getterType == boolean.class)) {
                return true;
            }
            if (paramType == byte.class && (getterType == Byte.class || getterType == byte.class)) {
                return true;
            }
            if (paramType == short.class && (getterType == Short.class || getterType == short.class)) {
                return true;
            }
            if (paramType == long.class && (getterType == Long.class || getterType == long.class)) {
                return true;
            }
            if (paramType == float.class && (getterType == Float.class || getterType == float.class)) {
                return true;
            }
            if (paramType == double.class && (getterType == Double.class || getterType == double.class)) {
                return true;
            }
        }
        return false;
    }

    private static String getterToName(Method getter, Class<?> owner) {
        String name = getter.getName();
        if (name.startsWith("is")) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        if (name.startsWith("get")) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return MappingLabelResolver.resolveMethodName(owner, name);
    }

    private static Object readBinding(Object packet, Binding binding) {
        if (binding.getter != null) {
            try {
                Object value = binding.getter.invoke(packet);
                if (binding.type == Entity.class && value instanceof Entity entity) {
                    return Integer.toString(entity.getId());
                }
                return value;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return PacketRecordCodec.defaultField(binding.type, binding.genericType);
    }

    private static List<PacketFieldModel> singletonFields(String typeName) {
        return List.of(
                new PacketFieldModel("type", "String", typeName, false, String.class),
                new PacketFieldModel("note", "String", "singleton (no fields)", false, String.class));
    }

    private static PacketFieldModel typeField(String typeName) {
        return new PacketFieldModel("type", "String", typeName, false, String.class);
    }

    private static @Nullable Packet<?> singletonInstance(Class<?> clazz) {
        if (ServerboundFinishConfigurationPacket.class.equals(clazz)) {
            return ServerboundFinishConfigurationPacket.INSTANCE;
        }
        if (ServerboundStatusRequestPacket.class.equals(clazz)) {
            return ServerboundStatusRequestPacket.INSTANCE;
        }
        if (ServerboundLoginAcknowledgedPacket.class.equals(clazz)) {
            return ServerboundLoginAcknowledgedPacket.INSTANCE;
        }
        try {
            Field field = clazz.getField("INSTANCE");
            if (Modifier.isStatic(field.getModifiers()) && Packet.class.isAssignableFrom(clazz)) {
                return (Packet<?>) field.get(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Packet<?>) ctor.newInstance();
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static List<PacketFieldModel> describePlayerInteractEntity(ServerboundInteractPacket packet) {
        InteractCapture capture = new InteractCapture();
        packet.dispatch(capture);
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(PacketUtils.getPacketTypeName(packet)));
        rows.add(new PacketFieldModel("interactType", "String", capture.interactType, true, String.class));
        rows.add(new PacketFieldModel("entityId", "int", Integer.toString(readInteractEntityId(packet)), true, int.class));
        rows.add(new PacketFieldModel("sneaking", "boolean", Boolean.toString(packet.isUsingSecondaryAction()), true, boolean.class));
        rows.add(new PacketFieldModel("hand", "Hand", capture.hand.name(), true, InteractionHand.class));
        rows.add(new PacketFieldModel("targetX", "double", Double.toString(capture.targetX), true, double.class));
        rows.add(new PacketFieldModel("targetY", "double", Double.toString(capture.targetY), true, double.class));
        rows.add(new PacketFieldModel("targetZ", "double", Double.toString(capture.targetZ), true, double.class));
        return rows;
    }

    private static Packet<?> buildPlayerInteractEntity(Map<String, String> fields, @Nullable Minecraft client)
            throws PacketRecordCodec.PacketBuildException {
        String interactType = fields.getOrDefault("interactType", "ATTACK").trim().toUpperCase();
        Entity entity = resolveEntity(fields, client);
        boolean sneaking = Boolean.parseBoolean(fields.getOrDefault("sneaking", "false"));
        InteractionHand hand = InteractionHand.valueOf(fields.getOrDefault("hand", InteractionHand.MAIN_HAND.name()).trim().toUpperCase());
        return switch (interactType) {
            case "INTERACT" -> ServerboundInteractPacket.createInteractionPacket(entity, sneaking, hand);
            case "INTERACT_AT" -> {
                double x = Double.parseDouble(fields.getOrDefault("targetX", "0"));
                double y = Double.parseDouble(fields.getOrDefault("targetY", "0"));
                double z = Double.parseDouble(fields.getOrDefault("targetZ", "0"));
                yield ServerboundInteractPacket.createInteractionPacket(entity, sneaking, hand, new Vec3(x, y, z));
            }
            default -> ServerboundInteractPacket.createAttackPacket(entity, sneaking);
        };
    }

    private static List<PacketFieldModel> describeClientCommand(ServerboundPlayerCommandPacket packet) {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(PacketUtils.getPacketTypeName(packet)));
        rows.add(new PacketFieldModel("entityId", "int", Integer.toString(packet.getId()), true, int.class));
        rows.add(new PacketFieldModel("mode", "Mode", packet.getAction().name(), true, ServerboundPlayerCommandPacket.Action.class));
        rows.add(new PacketFieldModel("mountJumpHeight", "int", Integer.toString(packet.getData()), true, int.class));
        return rows;
    }

    private static Packet<?> buildClientCommand(Map<String, String> fields, @Nullable Minecraft client)
            throws PacketRecordCodec.PacketBuildException {
        Entity entity = resolveEntity(fields, client);
        String modeRaw = fields.get("mode");
        if (modeRaw == null || modeRaw.isBlank()) {
            throw new PacketRecordCodec.PacketBuildException("Missing field: mode");
        }
        ServerboundPlayerCommandPacket.Action mode = ServerboundPlayerCommandPacket.Action.valueOf(modeRaw.trim().toUpperCase());
        int mountJumpHeight = Integer.parseInt(fields.getOrDefault("mountJumpHeight", "0").trim());
        if (mountJumpHeight != 0) {
            return new ServerboundPlayerCommandPacket(entity, mode, mountJumpHeight);
        }
        return new ServerboundPlayerCommandPacket(entity, mode);
    }

    private static List<PacketFieldModel> describePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        Abilities abilities = new Abilities();
        abilities.flying = packet.isFlying();
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(PacketUtils.getPacketTypeName(packet)));
        rows.add(new PacketFieldModel("invulnerable", "boolean", Boolean.toString(abilities.invulnerable), true, boolean.class));
        rows.add(new PacketFieldModel("flying", "boolean", Boolean.toString(abilities.flying), true, boolean.class));
        rows.add(new PacketFieldModel("allowFlying", "boolean", Boolean.toString(abilities.mayfly), true, boolean.class));
        rows.add(new PacketFieldModel("creativeMode", "boolean", Boolean.toString(abilities.instabuild), true, boolean.class));
        rows.add(new PacketFieldModel("allowModifyWorld", "boolean", Boolean.toString(abilities.mayBuild), true, boolean.class));
        return rows;
    }

    private static Packet<?> buildPlayerAbilities(Map<String, String> fields) {
        Abilities abilities = new Abilities();
        abilities.invulnerable = Boolean.parseBoolean(fields.getOrDefault("invulnerable", "false"));
        abilities.flying = Boolean.parseBoolean(fields.getOrDefault("flying", "false"));
        abilities.mayfly = Boolean.parseBoolean(fields.getOrDefault("allowFlying", "false"));
        abilities.instabuild = Boolean.parseBoolean(fields.getOrDefault("creativeMode", "false"));
        abilities.mayBuild = Boolean.parseBoolean(fields.getOrDefault("allowModifyWorld", "true"));
        return new ServerboundPlayerAbilitiesPacket(abilities);
    }

    private static List<PacketFieldModel> describeUpdateSign(ServerboundSignUpdatePacket packet) {
        String[] lines = packet.getLines();
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField(PacketUtils.getPacketTypeName(packet)));
        rows.add(new PacketFieldModel("pos", "BlockPos", PacketRecordCodec.encodeField(packet.getPos()), true, net.minecraft.core.BlockPos.class));
        rows.add(new PacketFieldModel("front", "boolean", Boolean.toString(packet.isFrontText()), true, boolean.class));
        for (int i = 0; i < 4; i++) {
            String line = i < lines.length ? lines[i] : "";
            rows.add(new PacketFieldModel("line" + (i + 1), "String", line, true, String.class));
        }
        return rows;
    }

    private static Packet<?> buildUpdateSign(Map<String, String> fields) throws PacketRecordCodec.PacketBuildException {
        net.minecraft.core.BlockPos pos = (net.minecraft.core.BlockPos) PacketRecordCodec.decodeField(
                net.minecraft.core.BlockPos.class, null, fields.get("pos"));
        boolean front = Boolean.parseBoolean(fields.getOrDefault("front", "true"));
        return new ServerboundSignUpdatePacket(
                pos,
                front,
                fields.getOrDefault("line1", ""),
                fields.getOrDefault("line2", ""),
                fields.getOrDefault("line3", ""),
                fields.getOrDefault("line4", ""));
    }

    private static int readInteractEntityId(ServerboundInteractPacket packet) {
        try {
            Field field = ServerboundInteractPacket.class.getDeclaredField("entityId");
            field.setAccessible(true);
            return field.getInt(packet);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static final class InteractCapture implements ServerboundInteractPacket.Handler {
        String interactType = "ATTACK";
        InteractionHand hand = InteractionHand.MAIN_HAND;
        double targetX;
        double targetY;
        double targetZ;

        @Override
        public void onInteraction(InteractionHand hand) {
            interactType = "INTERACT";
            this.hand = hand;
        }

        @Override
        public void onInteraction(InteractionHand hand, Vec3 pos) {
            interactType = "INTERACT_AT";
            this.hand = hand;
            targetX = pos.x;
            targetY = pos.y;
            targetZ = pos.z;
        }

        @Override
        public void onAttack() {
            interactType = "ATTACK";
        }
    }

    private enum CustomKind {
        NONE,
        PLAYER_INTERACT_ENTITY,
        CLIENT_COMMAND,
        PLAYER_ABILITIES,
        UPDATE_SIGN
    }

    private record Descriptor(CustomKind custom, @Nullable Constructor<?> constructor, Binding[] bindings, boolean singleton) {
    }

    private record Binding(String name, Class<?> type, @Nullable Type genericType, @Nullable Method getter) {
        static final Binding[] EMPTY = new Binding[0];

        PacketFieldModel toModel(String value) {
            return new PacketFieldModel(name, PacketRecordCodec.typeLabel(type, genericType), value, true, type, genericType);
        }

        PacketFieldModel toModel(Object value) {
            return toModel(PacketRecordCodec.encodeField(value));
        }
    }
}
