package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.module.packet.PacketUtils;
import com.dupeclient.client.module.packet.fabricator.ClickSlotPackets;
import com.dupeclient.client.module.packet.sniffer.FieldValueLabels;
import com.dupeclient.client.module.packet.sniffer.MappingLabelResolver;
import com.dupeclient.client.module.packet.sniffer.PacketChatCodec;
import com.dupeclient.client.module.packet.sniffer.PacketClassCodec;
import com.dupeclient.client.module.packet.sniffer.PacketFieldModel;
import com.dupeclient.client.module.packet.sniffer.PacketMoveCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PacketRecordCodec {
    private static final Map<Class<?>, Registry<?>> REGISTRY_FOR_TYPE = new HashMap();

    private PacketRecordCodec() {
    }

    public static List<PacketFieldModel> describe(Packet<?> packet) {
        Objects.requireNonNull(packet, "packet");
        List<PacketFieldModel> move = PacketMoveCodec.describe(packet);
        if (!move.isEmpty()) {
            return move;
        }
        List<PacketFieldModel> chat = PacketChatCodec.describe(packet);
        if (!chat.isEmpty()) {
            return chat;
        }
        List<PacketFieldModel> classDesc = PacketClassCodec.describe(packet);
        if (!classDesc.isEmpty()) {
            return classDesc;
        }
        String type = PacketUtils.getPacketTypeName(packet);
        ArrayList<PacketFieldModel> rows = new ArrayList<PacketFieldModel>();
        rows.add(new PacketFieldModel("type", "String", type, false, String.class));
        if (packet.getClass().isRecord()) {
            Class recordClass = packet.getClass();
            for (RecordComponent component : recordClass.getRecordComponents()) {
                try {
                    Object value = component.getAccessor().invoke(packet, new Object[0]);
                    rows.add(PacketRecordCodec.fieldFromComponent(recordClass, component, PacketRecordCodec.encodeValue(value)));
                }
                catch (ReflectiveOperationException reflectiveOperationException) {
                    // empty catch block
                }
            }
        } else {
            rows.add(new PacketFieldModel("raw", "String", String.valueOf(packet), false, String.class));
        }
        return rows;
    }

    public static List<PacketFieldModel> describeType(String typeName) {
        return PacketRecordCodec.describeType(typeName, Minecraft.getInstance());
    }

    public static List<PacketFieldModel> describeType(String typeName, @Nullable Minecraft client) {
        List<PacketFieldModel> move = PacketMoveCodec.describeType(typeName, client);
        if (!move.isEmpty()) {
            return move;
        }
        List<PacketFieldModel> chat = PacketChatCodec.describeType(typeName);
        if (!chat.isEmpty()) {
            return chat;
        }
        Class<? extends Packet<?>> clazz = PacketUtils.getPacket(typeName);
        if (clazz == null) {
            return List.of(new PacketFieldModel("type", "String", typeName, false, String.class));
        }
        if (ServerboundContainerClickPacket.class.isAssignableFrom(clazz)) {
            return List.of(new PacketFieldModel("type", "String", typeName, false, String.class), new PacketFieldModel("syncId", "int", "0", true, Integer.TYPE), new PacketFieldModel("revision", "int", "0", true, Integer.TYPE), new PacketFieldModel("slot", "short", "0", true, Short.TYPE), new PacketFieldModel("button", "byte", "0", true, Byte.TYPE), new PacketFieldModel("actionType", "SlotActionType", ContainerInput.PICKUP.name(), true, ContainerInput.class));
        }
        List<PacketFieldModel> classDesc = PacketClassCodec.describeType(typeName, clazz, client);
        if (!classDesc.isEmpty()) {
            return classDesc;
        }
        if (!clazz.isRecord()) {
            return List.of(new PacketFieldModel("type", "String", typeName, false, String.class), new PacketFieldModel("raw", "String", "# Fabrication not supported", false, String.class));
        }
        ArrayList<PacketFieldModel> rows = new ArrayList<PacketFieldModel>();
        rows.add(new PacketFieldModel("type", "String", typeName, false, String.class));
        for (RecordComponent component : clazz.getRecordComponents()) {
            rows.add(PacketRecordCodec.fieldFromComponent(clazz, component, PacketRecordCodec.defaultFor(component.getType(), component.getGenericType())));
        }
        return rows;
    }

    public static String buildEditable(List<PacketFieldModel> fields) {
        StringBuilder sb = new StringBuilder();
        for (PacketFieldModel field : fields) {
            if (!"type".equals(field.name)) continue;
            sb.append("type=").append(field.value).append('\n');
        }
        for (PacketFieldModel field : fields) {
            if ("type".equals(field.name)) continue;
            sb.append(field.name).append('=').append(field.value).append('\n');
        }
        return sb.toString().trim();
    }

    public static Map<String, String> parseFieldsMap(String text) {
        return PacketRecordCodec.parseFields(text);
    }

    public static List<PacketFieldModel> fieldsFromEditable(String text) {
        Map<String, String> map = PacketRecordCodec.parseFields(text);
        String type = map.getOrDefault("type", "");
        Class<? extends Packet<?>> clazz = type.isBlank() ? null : PacketUtils.getPacket(type);
        ArrayList<PacketFieldModel> rows = new ArrayList<PacketFieldModel>();
        rows.add(new PacketFieldModel("type", "String", type, false, String.class));
        if (PacketMoveCodec.supportsType(type)) {
            for (PacketFieldModel template : PacketMoveCodec.describeType(type, Minecraft.getInstance())) {
                if ("type".equals(template.name)) {
                    rows.add(template);
                    continue;
                }
                rows.add(new PacketFieldModel(template.name, template.typeName, map.getOrDefault(template.name, template.value), template.editable, template.valueType));
            }
        } else if (PacketChatCodec.supportsType(type)) {
            Map<String, String> normalized = PacketChatCodec.normalizeFields(map);
            for (PacketFieldModel template : PacketChatCodec.describeType(type)) {
                if ("type".equals(template.name)) {
                    rows.add(template);
                    continue;
                }
                rows.add(new PacketFieldModel(template.name, template.typeName, normalized.getOrDefault(template.name, template.value), template.editable, template.valueType));
            }
        } else if (clazz != null && PacketClassCodec.supports(clazz)) {
            for (PacketFieldModel template : PacketClassCodec.describeType(type, clazz, Minecraft.getInstance())) {
                if ("type".equals(template.name) || "note".equals(template.name)) {
                    if (!"type".equals(template.name)) continue;
                    rows.add(template);
                    continue;
                }
                rows.add(new PacketFieldModel(template.name, template.typeName, map.getOrDefault(template.name, template.value), template.editable, template.valueType, template.genericType));
            }
        } else if (clazz != null && clazz.isRecord()) {
            for (RecordComponent component : clazz.getRecordComponents()) {
                String raw = MappingLabelResolver.fieldValueFromMap(map, clazz, component, PacketRecordCodec.defaultFor(component.getType(), component.getGenericType()));
                rows.add(PacketRecordCodec.fieldFromComponent(clazz, component, raw));
            }
        } else if (map.containsKey("raw")) {
            rows.add(new PacketFieldModel("raw", "String", map.get("raw"), false, String.class));
        } else {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if ("type".equals(entry.getKey())) continue;
                rows.add(new PacketFieldModel(entry.getKey(), "String", entry.getValue(), true, String.class));
            }
        }
        return rows;
    }

    public static String cycleEnumValue(Class<?> enumType, String current) {
        if (!enumType.isEnum()) {
            return current;
        }
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return current;
        }
        int idx = 0;
        for (int i = 0; i < constants.length; ++i) {
            if (!((Enum)constants[i]).name().equalsIgnoreCase(current == null ? "" : current.trim())) continue;
            idx = (i + 1) % constants.length;
            break;
        }
        return ((Enum)constants[idx]).name();
    }

    public static boolean isBooleanType(@Nullable Class<?> type) {
        return type == Boolean.TYPE || type == Boolean.class;
    }

    public static String toggleBooleanValue(String current) {
        return Boolean.parseBoolean(current == null ? "" : current.trim()) ? "false" : "true";
    }

    public static boolean isLabeledField(String packetType, String fieldName) {
        return FieldValueLabels.isLabeledField(packetType, fieldName);
    }

    public static boolean isCyclableLabeledField(String packetType, String fieldName) {
        return FieldValueLabels.isCyclableField(packetType, fieldName);
    }

    public static String displayFieldValue(String packetType, String fieldName, String raw) {
        return FieldValueLabels.formatDisplay(packetType, fieldName, raw);
    }

    public static String cycleLabeledValue(String packetType, String fieldName, String current) {
        return FieldValueLabels.cycle(packetType, fieldName, current);
    }

    public static String parseLabeledRaw(String packetType, String fieldName, String input) {
        return FieldValueLabels.parseRaw(packetType, fieldName, input);
    }

    public static boolean supportsFieldType(Class<?> type, @Nullable Type genericType) {
        return PacketRecordCodec.isEditableType(type, genericType);
    }

    public static String encodeField(Object value) {
        return PacketRecordCodec.encodeValue(value);
    }

    public static Object decodeField(Class<?> type, @Nullable Type genericType, String raw) throws PacketBuildException {
        return PacketRecordCodec.decodeValue(type, genericType, raw);
    }

    public static String defaultField(Class<?> type, @Nullable Type genericType) {
        return PacketRecordCodec.defaultFor(type, genericType);
    }

    public static String typeLabel(Class<?> type, @Nullable Type genericType) {
        return PacketRecordCodec.formatTypeName(type, genericType);
    }

    private static PacketFieldModel fieldFromComponent(Class<?> recordClass, RecordComponent component, String value) {
        Class<?> valueType = component.getType();
        Type genericType = component.getGenericType();
        String name = MappingLabelResolver.resolveFieldName(recordClass, component);
        return new PacketFieldModel(name, PacketRecordCodec.formatTypeName(valueType, genericType), value, PacketRecordCodec.isEditableType(valueType, genericType), valueType, genericType);
    }

    private static boolean isEditableType(Class<?> type, @Nullable Type genericType) {
        if (type == String.class || type == Integer.TYPE || type == Integer.class || type == Short.TYPE || type == Short.class || type == Byte.TYPE || type == Byte.class || type == Long.TYPE || type == Long.class || type == Float.TYPE || type == Float.class || type == Double.TYPE || type == Double.class || type == Boolean.TYPE || type == Boolean.class || type == UUID.class || type == Identifier.class || type == RecipeDisplayId.class || type == BlockHitResult.class || type == ItemStack.class || type == Instant.class || type == Vec3i.class || type == GameType.class || type == Difficulty.class || type == Tag.class || type == MessageSignature.class || type == LastSeenMessages.Update.class || type.isEnum() || type == InteractionHand.class || type == BlockPos.class || type == Direction.class || type == Vec3.class || type == ContainerInput.class) {
            return true;
        }
        if (type.isArray()) {
            return type.getComponentType() == Byte.TYPE || PacketRecordCodec.isEditableType(type.getComponentType(), null);
        }
        if (Set.class.isAssignableFrom(type)) {
            return true;
        }
        if (PacketRecordCodec.registryFor(type) != null) {
            return true;
        }
        if (List.class.isAssignableFrom(type)) {
            return true;
        }
        if (type == Optional.class) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType inner;
                ParameterizedType registryParam;
                ParameterizedType parameterized = (ParameterizedType)genericType;
                Type arg = parameterized.getActualTypeArguments()[0];
                if (arg instanceof ParameterizedType && Holder.class.isAssignableFrom(PacketRecordCodec.erasure((registryParam = (ParameterizedType)arg).getRawType()))) {
                    return true;
                }
                return PacketRecordCodec.isEditableType(PacketRecordCodec.erasure(arg), arg instanceof ParameterizedType ? (inner = (ParameterizedType)arg) : null);
            }
            return true;
        }
        return false;
    }

    public static String toEditable(Packet<?> packet) {
        Objects.requireNonNull(packet, "packet");
        String move = PacketMoveCodec.toEditable(packet);
        if (!move.isBlank()) {
            return move;
        }
        String chat = PacketChatCodec.toEditable(packet);
        if (!chat.isBlank()) {
            return chat;
        }
        List<PacketFieldModel> classDesc = PacketClassCodec.describe(packet);
        if (!classDesc.isEmpty()) {
            return PacketRecordCodec.buildEditable(classDesc);
        }
        String type = PacketUtils.getPacketTypeName(packet);
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(type).append('\n');
        if (packet.getClass().isRecord()) {
            for (RecordComponent component : packet.getClass().getRecordComponents()) {
                try {
                    Object value = component.getAccessor().invoke(packet, new Object[0]);
                    sb.append(MappingLabelResolver.resolveFieldName(packet.getClass(), component)).append('=').append(PacketRecordCodec.encodeValue(value)).append('\n');
                }
                catch (ReflectiveOperationException reflectiveOperationException) {
                    // empty catch block
                }
            }
        } else {
            sb.append("raw=").append(packet).append('\n');
        }
        return sb.toString().trim();
    }

    public static String templateForType(String typeName) {
        List<PacketFieldModel> move = PacketMoveCodec.describeType(typeName, Minecraft.getInstance());
        if (!move.isEmpty()) {
            return PacketRecordCodec.buildEditable(move);
        }
        List<PacketFieldModel> chat = PacketChatCodec.describeType(typeName);
        if (!chat.isEmpty()) {
            return PacketRecordCodec.buildEditable(chat);
        }
        Class<? extends Packet<?>> clazz = PacketUtils.getPacket(typeName);
        if (clazz == null) {
            return "type=" + typeName + "\n# Unknown packet type";
        }
        if (ServerboundContainerClickPacket.class.isAssignableFrom(clazz)) {
            return "type=ClickSlotC2SPacket\nsyncId=0\nrevision=0\nslot=0\nbutton=0\nactionType=PICKUP\n";
        }
        List<PacketFieldModel> classDesc = PacketClassCodec.describeType(typeName, clazz, Minecraft.getInstance());
        if (!classDesc.isEmpty()) {
            return PacketRecordCodec.buildEditable(classDesc);
        }
        if (clazz.isRecord()) {
            StringBuilder sb = new StringBuilder();
            sb.append("type=").append(typeName).append('\n');
            for (RecordComponent component : clazz.getRecordComponents()) {
                sb.append(component.getName()).append('=').append(PacketRecordCodec.defaultFor(component.getType(), component.getGenericType())).append('\n');
            }
            return sb.toString().trim();
        }
        return "type=" + typeName + "\n# Fabrication not supported for this class";
    }

    public static Packet<?> fromEditable(String text) throws PacketBuildException {
        Packet<?> singleton;
        Map<String, String> fields = PacketRecordCodec.parseFields(text);
        String type = fields.get("type");
        if (type == null || type.isBlank()) {
            throw new PacketBuildException("Missing type= line");
        }
        if (fields.containsKey("raw") && fields.size() <= 3) {
            throw new PacketBuildException("This packet is view-only; use Resend Original");
        }
        Class<? extends Packet<?>> clazz = PacketUtils.getPacket(type);
        if (clazz == null) {
            throw new PacketBuildException("Unknown packet type: " + type);
        }
        if (ServerboundContainerClickPacket.class.isAssignableFrom(clazz)) {
            return PacketRecordCodec.buildClickSlot(fields);
        }
        if (PacketMoveCodec.supportsType(type)) {
            return PacketMoveCodec.build(type, fields);
        }
        if (PacketChatCodec.supportsType(type)) {
            return PacketChatCodec.build(PacketChatCodec.normalizeFields(fields));
        }
        if (PacketClassCodec.supports(clazz)) {
            return PacketClassCodec.build(clazz, fields, Minecraft.getInstance());
        }
        if (!clazz.isRecord()) {
            throw new PacketBuildException("Cannot build non-record packet: " + type);
        }
        RecordComponent[] components = clazz.getRecordComponents();
        if (components.length == 0 && (singleton = PacketRecordCodec.singletonRecord(clazz)) != null) {
            return singleton;
        }
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; ++i) {
            RecordComponent component = components[i];
            String raw = MappingLabelResolver.fieldValueFromMap(fields, clazz, component, null);
            if (raw == null) {
                throw new PacketBuildException("Missing field: " + MappingLabelResolver.resolveFieldName(clazz, component));
            }
            String fieldName = MappingLabelResolver.resolveFieldName(clazz, component);
            raw = FieldValueLabels.parseRaw(type, fieldName, raw);
            args[i] = PacketRecordCodec.decodeValue(component.getType(), component.getGenericType(), raw);
        }
        try {
            var ctor = clazz.getDeclaredConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new));
            ctor.setAccessible(true);
            return (Packet<?>) ctor.newInstance(args);
        }
        catch (ReflectiveOperationException e) {
            throw new PacketBuildException("Build failed: " + e.getMessage());
        }
    }

    public static ServerboundContainerClickPacket refreshClickSlot(ServerboundContainerClickPacket packet, Minecraft client) {
        if (client == null || client.player == null || client.player.containerMenu == null) {
            return packet;
        }
        return ClickSlotPackets.refresh(packet, client.player.containerMenu);
    }

    private static ServerboundContainerClickPacket buildClickSlot(Map<String, String> fields) throws PacketBuildException {
        int syncId = PacketRecordCodec.parseInt(fields, "syncId", 0);
        int revision = PacketRecordCodec.parseInt(fields, "revision", 0);
        int slot = PacketRecordCodec.parseInt(fields, "slot", 0);
        int button = PacketRecordCodec.parseLabeledInt("ClickSlotC2SPacket", "button", fields, "button", 0);
        ContainerInput action = PacketRecordCodec.parseEnum(fields.get("actionType"), ContainerInput.class, ContainerInput.PICKUP);
        return new ServerboundContainerClickPacket(syncId, revision, (short)slot, (byte)button, action, (Int2ObjectMap)new Int2ObjectArrayMap(), HashedStack.EMPTY);
    }

    private static Map<String, String> parseFields(String text) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (String line : text.split("\n")) {
            int eq;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || (eq = trimmed.indexOf(61)) <= 0) continue;
            out.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
        }
        return out;
    }

    private static String encodeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof MessageSignature) {
            MessageSignature signature = (MessageSignature)value;
            return PacketRecordCodec.encodeBytes(signature.bytes());
        }
        if (value instanceof LastSeenMessages.Update) {
            LastSeenMessages.Update acknowledgment = (LastSeenMessages.Update)value;
            return PacketChatCodec.formatAcknowledgment(acknowledgment);
        }
        if (value instanceof Optional) {
            Optional opt = (Optional)value;
            return opt.isEmpty() ? "" : PacketRecordCodec.encodeValue(opt.get());
        }
        if (value instanceof List) {
            List list = (List)value;
            return PacketRecordCodec.encodeList(list);
        }
        if (value.getClass().isArray()) {
            return PacketRecordCodec.encodeArray(value);
        }
        if (value instanceof Enum) {
            Enum e = (Enum)value;
            return e.name();
        }
        if (value instanceof UUID) {
            UUID uuid = (UUID)value;
            return uuid.toString();
        }
        if (value instanceof Identifier) {
            Identifier id = (Identifier)value;
            return id.toString();
        }
        if (value instanceof RecipeDisplayId) {
            RecipeDisplayId recipeId = (RecipeDisplayId)value;
            return Integer.toString(recipeId.index());
        }
        if (value instanceof BlockHitResult) {
            BlockHitResult hit = (BlockHitResult)value;
            return PacketRecordCodec.formatBlockHitResult(hit);
        }
        if (value instanceof ItemStack) {
            ItemStack stack = (ItemStack)value;
            return PacketRecordCodec.formatItemStack(stack);
        }
        if (value instanceof Instant) {
            Instant instant = (Instant)value;
            return Long.toString(instant.toEpochMilli());
        }
        if (value instanceof Vec3i) {
            Vec3i vec = (Vec3i)value;
            return vec.getX() + "," + vec.getY() + "," + vec.getZ();
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[])value;
            return PacketRecordCodec.encodeBytes(bytes);
        }
        if (value instanceof Tag) {
            Tag nbt = (Tag)value;
            return nbt.toString();
        }
        if (value instanceof Holder<?> entry && entry.value() instanceof MobEffect effect) {
            return BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
        }
        String registryEncoded = PacketRecordCodec.encodeRegistryValue(value);
        if (registryEncoded != null) {
            return registryEncoded;
        }
        if (value instanceof Set) {
            Set set = (Set)value;
            return PacketRecordCodec.encodeList(new ArrayList(set));
        }
        String raw = String.valueOf(value);
        if (raw.contains("\n") || raw.contains("=") || raw.contains(",") || raw.contains("[") || raw.contains("\"")) {
            return "\"" + PacketRecordCodec.escapeJsonString(raw) + "\"";
        }
        return raw;
    }

    private static Object decodeValue(Class<?> type, @Nullable Type genericType, String raw) throws PacketBuildException {
        if (raw == null) {
            throw new PacketBuildException("Empty value");
        }
        String trimmed = raw.trim();
        if (type == Optional.class) {
            return PacketRecordCodec.decodeOptional(genericType, trimmed);
        }
        if (List.class.isAssignableFrom(type)) {
            return PacketRecordCodec.decodeList(genericType, trimmed);
        }
        if (type.isArray()) {
            return PacketRecordCodec.decodeArray(type.getComponentType(), trimmed);
        }
        String value = PacketRecordCodec.unquote(trimmed);
        if (type == String.class) {
            return value;
        }
        if (type == Integer.TYPE || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == Short.TYPE || type == Short.class) {
            return Short.parseShort(value);
        }
        if (type == Byte.TYPE || type == Byte.class) {
            return Byte.parseByte(value);
        }
        if (type == Long.TYPE || type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == Float.TYPE || type == Float.class) {
            return Float.valueOf(Float.parseFloat(value));
        }
        if (type == Double.TYPE || type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == UUID.class) {
            return UUID.fromString(value);
        }
        if (type == Identifier.class) {
            return PacketRecordCodec.parseIdentifier(value);
        }
        if (type == RecipeDisplayId.class) {
            return new RecipeDisplayId(Integer.parseInt(value));
        }
        if (type == BlockHitResult.class) {
            return PacketRecordCodec.parseBlockHitResult(value);
        }
        if (type == ItemStack.class) {
            return PacketRecordCodec.parseItemStack(value);
        }
        if (type == Instant.class) {
            return PacketRecordCodec.parseInstant(value);
        }
        if (type == Vec3i.class) {
            return PacketRecordCodec.parseVec3i(value);
        }
        if (type == GameType.class) {
            return GameType.valueOf((String)value.toUpperCase(Locale.ROOT));
        }
        if (type == Difficulty.class) {
            return Difficulty.valueOf((String)value.toUpperCase(Locale.ROOT));
        }
        if (type == Tag.class) {
            return PacketRecordCodec.parseNbt(value);
        }
        if (type == MessageSignature.class) {
            return PacketRecordCodec.parseMessageSignature(value);
        }
        if (type == LastSeenMessages.Update.class) {
            return PacketRecordCodec.parseChatAcknowledgment(value);
        }
        if (Set.class.isAssignableFrom(type)) {
            return PacketRecordCodec.decodeSet(genericType, trimmed);
        }
        if (type.isEnum()) {
            return PacketRecordCodec.parseEnumLoose(value, type);
        }
        if (type == InteractionHand.class) {
            return InteractionHand.valueOf((String)value.toUpperCase(Locale.ROOT));
        }
        if (type == BlockPos.class) {
            return PacketRecordCodec.parseBlockPos(value);
        }
        if (type == Direction.class) {
            return Direction.valueOf((String)value.toUpperCase(Locale.ROOT));
        }
        if (type == Vec3.class) {
            return PacketRecordCodec.parseVec3d(value);
        }
        if (type == ContainerInput.class) {
            return ContainerInput.valueOf((String)value.toUpperCase(Locale.ROOT));
        }
        Object registryValue = PacketRecordCodec.decodeRegistryValue(type, value);
        if (registryValue != null) {
            return registryValue;
        }
        throw new PacketBuildException("Unsupported field type: " + PacketRecordCodec.formatTypeName(type, genericType));
    }

    private static String formatTypeName(Class<?> type, @Nullable Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType)genericType;
            Class<?> raw = PacketRecordCodec.erasure(parameterized.getRawType());
            Type[] args = parameterized.getActualTypeArguments();
            if (raw == Optional.class && args.length == 1) {
                return "Optional<" + PacketRecordCodec.formatTypeArg(args[0]) + ">";
            }
            if (List.class.isAssignableFrom(raw) && args.length == 1) {
                return "List<" + PacketRecordCodec.formatTypeArg(args[0]) + ">";
            }
            if (Set.class.isAssignableFrom(raw) && args.length == 1) {
                return "Set<" + PacketRecordCodec.formatTypeArg(args[0]) + ">";
            }
        }
        if (type.isArray()) {
            return PacketRecordCodec.formatTypeName(type.getComponentType(), null) + "[]";
        }
        return MappingLabelResolver.resolveClassName(type.getSimpleName());
    }

    private static String formatTypeArg(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class)type;
            return MappingLabelResolver.resolveClassName(clazz.getSimpleName());
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType)type;
            return PacketRecordCodec.formatTypeName(PacketRecordCodec.erasure(parameterized.getRawType()), parameterized);
        }
        return String.valueOf(type);
    }

    private static Class<?> erasure(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class)type;
            return clazz;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType)type;
            return PacketRecordCodec.erasure(parameterized.getRawType());
        }
        return Object.class;
    }

    private static Class<?> elementType(@Nullable Type genericType, Class<?> fallback) {
        ParameterizedType parameterized;
        Type[] args;
        if (genericType instanceof ParameterizedType && (args = (parameterized = (ParameterizedType)genericType).getActualTypeArguments()).length == 1) {
            return PacketRecordCodec.erasure(args[0]);
        }
        return fallback;
    }

    private static String encodeList(List<?> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); ++i) {
            Object element;
            if (i > 0) {
                sb.append(',');
            }
            if ((element = list.get(i)) instanceof String) {
                String s = (String)element;
                sb.append('\"').append(PacketRecordCodec.escapeJsonString(s)).append('\"');
                continue;
            }
            sb.append(PacketRecordCodec.encodeValue(element));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String encodeArray(Object array) {
        int len = Array.getLength(array);
        if (len == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(PacketRecordCodec.encodeValue(Array.get(array, i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static Object decodeOptional(@Nullable Type genericType, String raw) throws PacketBuildException {
        ParameterizedType parameterized;
        if (raw.isEmpty() || "null".equalsIgnoreCase(raw) || "empty".equalsIgnoreCase(raw) || "optional.empty".equalsIgnoreCase(raw)) {
            return Optional.empty();
        }
        Class<?> inner = PacketRecordCodec.elementType(genericType, String.class);
        if (genericType instanceof ParameterizedType && Holder.class.isAssignableFrom(PacketRecordCodec.erasure((parameterized = (ParameterizedType)genericType).getActualTypeArguments()[0]))) {
            return Optional.of(PacketRecordCodec.parseStatusEffectEntry(raw));
        }
        return Optional.of(PacketRecordCodec.decodeValue(inner, null, raw));
    }

    private static List<Object> decodeList(@Nullable Type genericType, String raw) throws PacketBuildException {
        Class<?> elementType = PacketRecordCodec.elementType(genericType, String.class);
        if (raw.isEmpty() || "[]".equals(raw)) {
            return new ArrayList<Object>();
        }
        if (!raw.startsWith("[") || !raw.endsWith("]")) {
            throw new PacketBuildException("List value must use [item,item] syntax");
        }
        String inner = raw.substring(1, raw.length() - 1).trim();
        if (inner.isEmpty()) {
            return new ArrayList<Object>();
        }
        List<String> tokens = PacketRecordCodec.splitListElements(inner);
        ArrayList<Object> out = new ArrayList<Object>(tokens.size());
        for (String token : tokens) {
            out.add(PacketRecordCodec.decodeValue(elementType, null, token));
        }
        return out;
    }

    private static Object decodeArray(Class<?> componentType, String raw) throws PacketBuildException {
        if (raw.isEmpty() || "[]".equals(raw)) {
            return Array.newInstance(componentType, 0);
        }
        if (!raw.startsWith("[") || !raw.endsWith("]")) {
            throw new PacketBuildException("Array value must use [item,item] syntax");
        }
        String inner = raw.substring(1, raw.length() - 1).trim();
        if (inner.isEmpty()) {
            return Array.newInstance(componentType, 0);
        }
        List<String> tokens = PacketRecordCodec.splitListElements(inner);
        Object array = Array.newInstance(componentType, tokens.size());
        for (int i = 0; i < tokens.size(); ++i) {
            Array.set(array, i, PacketRecordCodec.decodeValue(componentType, null, tokens.get(i)));
        }
        return array;
    }

    private static List<String> splitListElements(String inner) throws PacketBuildException {
        ArrayList<String> out = new ArrayList<String>();
        int i = 0;
        while (i < inner.length()) {
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) {
                ++i;
            }
            if (i >= inner.length()) break;
            if (inner.charAt(i) == '\"') {
                ++i;
                StringBuilder token = new StringBuilder();
                while (i < inner.length()) {
                    char ch;
                    if ((ch = inner.charAt(i++)) == '\\' && i < inner.length()) {
                        char next = inner.charAt(i++);
                        token.append(switch (next) {
                            case 'n' -> '\n';
                            case 'r' -> '\r';
                            case 't' -> '\t';
                            case '\"' -> '\"';
                            case '\\' -> '\\';
                            default -> next;
                        });
                        continue;
                    }
                    if (ch == '\"') {
                        out.add("\"" + String.valueOf(token) + "\"");
                        break;
                    }
                    token.append(ch);
                }
                if (i >= inner.length() || inner.charAt(i) != ',') continue;
                ++i;
                continue;
            }
            int start = i;
            while (i < inner.length() && inner.charAt(i) != ',') {
                ++i;
            }
            out.add(inner.substring(start, i).trim());
            if (i >= inner.length() || inner.charAt(i) != ',') continue;
            ++i;
        }
        return out;
    }

    private static String escapeJsonString(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"");
        }
        return value;
    }

    private static BlockPos parseBlockPos(String value) throws PacketBuildException {
        String[] parts = value.replace("BlockPos", "").replace("{", "").replace("}", "").split("[,\\s]+");
        ArrayList<Integer> nums = new ArrayList<Integer>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=");
            String num = kv.length == 2 ? kv[1] : part;
            try {
                nums.add(Integer.parseInt(num.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (nums.size() >= 3) {
            return new BlockPos(((Integer)nums.get(0)).intValue(), ((Integer)nums.get(1)).intValue(), ((Integer)nums.get(2)).intValue());
        }
        throw new PacketBuildException("Could not parse BlockPos: " + value);
    }

    private static Vec3 parseVec3d(String value) throws PacketBuildException {
        String cleaned = value.replace("Vec3d", "").replace("{", "").replace("}", "");
        String[] parts = cleaned.split("[,\\s]+");
        ArrayList<Double> nums = new ArrayList<Double>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=");
            String num = kv.length == 2 ? kv[1] : part;
            try {
                nums.add(Double.parseDouble(num.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (nums.size() >= 3) {
            return new Vec3(((Double)nums.get(0)).doubleValue(), ((Double)nums.get(1)).doubleValue(), ((Double)nums.get(2)).doubleValue());
        }
        throw new PacketBuildException("Could not parse Vec3d: " + value);
    }

    private static int parseLabeledInt(String packetType, String fieldName, Map<String, String> fields, String key, int fallback) throws PacketBuildException {
        String raw = fields.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String parsed = FieldValueLabels.parseRaw(packetType, fieldName, raw);
        try {
            return Integer.parseInt(parsed.trim());
        }
        catch (NumberFormatException e) {
            throw new PacketBuildException("Invalid int for " + key + ": " + raw);
        }
    }

    private static int parseInt(Map<String, String> fields, String key, int fallback) throws PacketBuildException {
        String raw = fields.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnumLoose(String raw, Class<?> type) throws PacketBuildException {
        return PacketRecordCodec.parseEnum(raw, (Class) type, null);
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, E fallback) throws PacketBuildException {
        if (raw == null || raw.isBlank()) {
            if (fallback != null) {
                return fallback;
            }
            throw new PacketBuildException("Missing enum value for " + type.getSimpleName());
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (Enum constant : (Enum[])type.getEnumConstants()) {
            if (!constant.name().equalsIgnoreCase(normalized)) continue;
            return (E)constant;
        }
        if (fallback != null) {
            return fallback;
        }
        throw new PacketBuildException("Unknown " + type.getSimpleName() + " value: " + raw);
    }

    private static String defaultFor(Class<?> type, @Nullable Type genericType) {
        Object[] constants;
        if (type == Optional.class) {
            return "";
        }
        if (List.class.isAssignableFrom(type)) {
            return "[]";
        }
        if (type.isArray()) {
            return "[]";
        }
        if (type == String.class) {
            return "";
        }
        if (type == Integer.TYPE || type == Integer.class || type == Short.TYPE || type == Short.class || type == Byte.TYPE || type == Byte.class || type == Long.TYPE || type == Long.class) {
            return "0";
        }
        if (type == Float.TYPE || type == Float.class || type == Double.TYPE || type == Double.class) {
            return "0.0";
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            return "false";
        }
        if (type == UUID.class) {
            return "00000000-0000-0000-0000-000000000000";
        }
        if (type == InteractionHand.class) {
            return InteractionHand.MAIN_HAND.name();
        }
        if (type == BlockPos.class) {
            return "0,0,0";
        }
        if (type == Direction.class) {
            return Direction.UP.name();
        }
        if (type == Vec3.class) {
            return "0.0,0.0,0.0";
        }
        if (type == ContainerInput.class) {
            return ContainerInput.PICKUP.name();
        }
        if (type == Identifier.class) {
            return "minecraft:stone";
        }
        if (type == RecipeDisplayId.class) {
            return "0";
        }
        if (type == BlockHitResult.class) {
            return "0,64,0;up;0.5,1.0,0.5;false";
        }
        if (type == ItemStack.class) {
            return "minecraft:air";
        }
        if (type == Instant.class) {
            return Long.toString(Instant.now().toEpochMilli());
        }
        if (type == Vec3i.class) {
            return "1,1,1";
        }
        if (type == GameType.class) {
            return GameType.SURVIVAL.name();
        }
        if (type == Difficulty.class) {
            return Difficulty.NORMAL.name();
        }
        if (type == Tag.class) {
            return "{}";
        }
        if (type == MessageSignature.class) {
            return "null";
        }
        if (type == LastSeenMessages.Update.class) {
            return PacketChatCodec.defaultAcknowledgmentText();
        }
        if (type.isArray() && type.getComponentType() == Byte.TYPE) {
            return "[]";
        }
        if (Set.class.isAssignableFrom(type)) {
            return "[]";
        }
        if (type.isEnum() && (constants = type.getEnumConstants()) != null && constants.length > 0) {
            return ((Enum)constants[0]).name();
        }
        return "";
    }

    @Nullable
    private static Packet<?> singletonRecord(Class<? extends Packet<?>> clazz) {
        try {
            Field field = clazz.getField("INSTANCE");
            if (Modifier.isStatic(field.getModifiers())) {
                return (Packet)field.get(null);
            }
        }
        catch (ReflectiveOperationException field) {
            // empty catch block
        }
        try {
            var ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Packet<?>) ctor.newInstance();
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            return null;
        }
    }

    private static Identifier parseIdentifier(String value) throws PacketBuildException {
        String normalized = value.contains(":") ? value : "minecraft:" + value;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) {
            throw new PacketBuildException("Invalid identifier: " + value);
        }
        return id;
    }

    private static Instant parseInstant(String value) throws PacketBuildException {
        try {
            if (value.contains("T") || value.contains("-")) {
                return Instant.parse(value);
            }
            return Instant.ofEpochMilli(Long.parseLong(value));
        }
        catch (Exception e) {
            throw new PacketBuildException("Could not parse Instant: " + value);
        }
    }

    private static Vec3i parseVec3i(String value) throws PacketBuildException {
        String[] parts = value.split(",");
        if (parts.length < 3) {
            throw new PacketBuildException("Vec3i expects x,y,z");
        }
        return new Vec3i(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
    }

    private static String formatBlockHitResult(BlockHitResult hit) {
        Vec3 pos = hit.getLocation();
        return hit.getBlockPos().getX() + "," + hit.getBlockPos().getY() + "," + hit.getBlockPos().getZ() + ";" + hit.getDirection().name().toLowerCase(Locale.ROOT) + ";" + pos.x + "," + pos.y + "," + pos.z + ";" + hit.isInside();
    }

    private static BlockHitResult parseBlockHitResult(String value) throws PacketBuildException {
        String[] parts = value.split(";");
        if (parts.length < 4) {
            throw new PacketBuildException("BlockHitResult expects blockPos;side;hitPos;insideBlock");
        }
        BlockPos blockPos = PacketRecordCodec.parseBlockPos(parts[0].trim());
        Direction side = Direction.valueOf((String)parts[1].trim().toUpperCase(Locale.ROOT));
        Vec3 hitPos = PacketRecordCodec.parseVec3d(parts[2].trim());
        boolean inside = Boolean.parseBoolean(parts[3].trim());
        return new BlockHitResult(hitPos, side, blockPos, inside);
    }

    private static String formatItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stack.getCount() <= 1) {
            return id.toString();
        }
        return String.valueOf(id) + ":" + stack.getCount();
    }

    private static ItemStack parseItemStack(String value) throws PacketBuildException {
        Item item;
        String tail;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "empty".equalsIgnoreCase(trimmed) || "minecraft:air".equalsIgnoreCase(trimmed)) {
            return ItemStack.EMPTY;
        }
        int count = 1;
        String idPart = trimmed;
        int lastColon = trimmed.lastIndexOf(58);
        if (lastColon > 0 && (tail = trimmed.substring(lastColon + 1)).chars().allMatch(Character::isDigit)) {
            count = Integer.parseInt(tail);
            idPart = trimmed.substring(0, lastColon);
        }
        item = BuiltInRegistries.ITEM.getValue(PacketRecordCodec.parseIdentifier(idPart));
        if (item == Items.AIR && !idPart.endsWith("air")) {
            throw new PacketBuildException("Unknown item: " + value);
        }
        return new ItemStack(item, count);
    }

    @Nullable
    private static MessageSignature parseMessageSignature(String value) throws PacketBuildException {
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "unsigned".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return new MessageSignature(PacketRecordCodec.decodeHexBytes(trimmed));
    }

    private static LastSeenMessages.Update parseChatAcknowledgment(String value) throws PacketBuildException {
        String[] parts = value.split(";", 3);
        if (parts.length < 3) {
            throw new PacketBuildException("Acknowledgment expects offset;bits;checksum (e.g. 0;;1)");
        }
        int offset = Integer.parseInt(parts[0].trim());
        BitSet acknowledged = new BitSet();
        String bits = parts[1].trim();
        if (!bits.isEmpty()) {
            for (String token : bits.split(",")) {
                if (token.isBlank()) continue;
                acknowledged.set(Integer.parseInt(token.trim()));
            }
        }
        byte checksum = (byte)Integer.parseInt(parts[2].trim());
        return new LastSeenMessages.Update(offset, acknowledged, checksum);
    }

    private static byte[] decodeHexBytes(String raw) throws PacketBuildException {
        String hex = raw.startsWith("0x") ? raw.substring(2) : raw;
        if ((hex = hex.replace(" ", "")).length() % 2 != 0) {
            throw new PacketBuildException("Hex byte string must have even length");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; ++i) {
            int idx = i * 2;
            out[i] = (byte)Integer.parseInt(hex.substring(idx, idx + 2), 16);
        }
        return out;
    }

    private static Tag parseNbt(String value) throws PacketBuildException {
        try {
            return TagParser.parseCompoundFully((String)value);
        }
        catch (Exception e) {
            throw new PacketBuildException("Invalid NBT: " + e.getMessage());
        }
    }

    private static String encodeBytes(byte[] bytes) {
        if (bytes.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static Holder<MobEffect> parseStatusEffectEntry(String raw) throws PacketBuildException {
        Identifier id = PacketRecordCodec.parseIdentifier(raw);
        return BuiltInRegistries.MOB_EFFECT.get(id).orElseThrow(() -> new PacketBuildException("Unknown status effect: " + raw));
    }

    private static Set<Object> decodeSet(@Nullable Type genericType, String raw) throws PacketBuildException {
        List<Object> list = PacketRecordCodec.decodeList(genericType, raw);
        return new HashSet<Object>(list);
    }

    @Nullable
    private static String encodeRegistryValue(Object value) {
        if (value == null) {
            return null;
        }
        Registry<?> registry = PacketRecordCodec.registryFor(value.getClass());
        if (registry == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Registry<Object> typed = (Registry<Object>) registry;
            return Integer.toString(typed.getId(value));
        }
        catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Object decodeRegistryValue(Class<?> type, String raw) throws PacketBuildException {
        Registry<?> registry = PacketRecordCodec.registryFor(type);
        if (registry == null) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.chars().allMatch(Character::isDigit) || trimmed.startsWith("-") && trimmed.length() > 1) {
                int id = Integer.parseInt(trimmed);
                Object value = registry.byId(id);
                if (value == null) {
                    throw new PacketBuildException("Unknown registry id " + id + " for " + type.getSimpleName());
                }
                return value;
            }
            Identifier identifier = PacketRecordCodec.parseIdentifier(trimmed);
            Object value = registry.getValue(identifier);
            if (value == null) {
                throw new PacketBuildException("Unknown registry entry: " + trimmed);
            }
            return value;
        }
        catch (NumberFormatException e) {
            throw new PacketBuildException("Invalid registry value: " + raw);
        }
    }

    @Nullable
    private static Registry<?> registryFor(Class<?> type) {
        if (type == null) {
            return null;
        }
        Registry<?> cached = REGISTRY_FOR_TYPE.get(type);
        if (cached != null) {
            return cached;
        }
        for (Field field : BuiltInRegistries.class.getFields()) {
            if (!Registry.class.isAssignableFrom(field.getType())) continue;
            try {
                Registry registry = (Registry)field.get(null);
                if (registry == null || registry.size() == 0) continue;
                for (Object entry : registry) {
                    if (entry == null || !type.isInstance(entry)) continue;
                    REGISTRY_FOR_TYPE.put(type, registry);
                    return registry;
                }
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                // empty catch block
            }
        }
        return null;
    }

    public static final class PacketBuildException
    extends Exception {
        public PacketBuildException(String message) {
            super(message);
        }
    }
}

