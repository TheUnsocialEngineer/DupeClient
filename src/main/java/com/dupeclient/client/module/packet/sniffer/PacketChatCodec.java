package com.dupeclient.client.module.packet.sniffer;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;
import com.dupeclient.client.module.packet.sniffer.PacketFieldModel;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;

import com.dupeclient.client.module.packet.PacketUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

/**
 * Fabricates {@link ServerboundChatPacket} using the same shape as captured/resendable chat packets
 * (unsigned signature + vanilla-style acknowledgment stub).
 */
public final class PacketChatCodec {
    public static final String TYPE = "ChatMessageC2SPacket";

    private PacketChatCodec() {
    }

    public static boolean supportsType(String typeName) {
        return TYPE.equals(typeName);
    }

    public static boolean isChatMessagePacket(Packet<?> packet) {
        return packet instanceof ServerboundChatPacket;
    }

    public static List<PacketFieldModel> describe(Packet<?> packet) {
        if (!(packet instanceof ServerboundChatPacket chat)) {
            return List.of();
        }
        return describePacket(chat);
    }

    public static List<PacketFieldModel> describeType(String typeName) {
        if (!supportsType(typeName)) {
            return List.of();
        }
        return templateFields();
    }

    public static String toEditable(Packet<?> packet) {
        List<PacketFieldModel> rows = describe(packet);
        if (rows.isEmpty()) {
            return "";
        }
        return PacketRecordCodec.buildEditable(rows);
    }

    public static Packet<?> build(Map<String, String> fields) throws PacketRecordCodec.PacketBuildException {
        String message = fields.getOrDefault("chatMessage", "");
        Instant timestamp = parseTimestamp(fields.get("timestamp"));
        long salt = parseLong(fields.get("salt"), ThreadLocalRandom.current().nextLong());
        MessageSignature signature = parseSignature(fields.get("signature"));
        LastSeenMessages.Update acknowledgment = parseAcknowledgment(
                fields.getOrDefault("acknowledgment", defaultAcknowledgmentText()));
        return new ServerboundChatPacket(message, timestamp, salt, signature, acknowledgment);
    }

    public static String friendlyFieldName(RecordComponent component) {
        return MappingLabelResolver.resolveFieldName(ServerboundChatPacket.class, component);
    }

    public static boolean isChatField(String name) {
        return switch (name) {
            case "chatMessage", "timestamp", "salt", "signature", "acknowledgment",
                    "comp_945", "comp_946", "comp_947", "comp_948", "comp_970" -> true;
            default -> false;
        };
    }

    public static String mapLegacyFieldName(String name) {
        return MappingLabelResolver.resolveFieldName(ServerboundChatPacket.class, name);
    }

    private static List<PacketFieldModel> describePacket(ServerboundChatPacket chat) {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField());
        rows.add(field("chatMessage", String.class, chat.message()));
        rows.add(field("timestamp", Instant.class, PacketRecordCodec.encodeField(chat.timeStamp())));
        rows.add(field("salt", long.class, Long.toString(chat.salt())));
        rows.add(field("signature", MessageSignature.class, formatSignature(chat.signature())));
        rows.add(field("acknowledgment", LastSeenMessages.Update.class, formatAcknowledgment(chat.lastSeenMessages())));
        return rows;
    }

    private static List<PacketFieldModel> templateFields() {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField());
        rows.add(field("chatMessage", String.class, ""));
        rows.add(field("timestamp", Instant.class, Long.toString(Instant.now().toEpochMilli())));
        rows.add(field("salt", long.class, Long.toString(ThreadLocalRandom.current().nextLong())));
        rows.add(field("signature", MessageSignature.class, "null"));
        rows.add(field("acknowledgment", LastSeenMessages.Update.class, defaultAcknowledgmentText()));
        return rows;
    }

    private static PacketFieldModel typeField() {
        return new PacketFieldModel("type", "String", TYPE, false, String.class);
    }

    private static PacketFieldModel field(String name, Class<?> type, String value) {
        return new PacketFieldModel(name, PacketRecordCodec.typeLabel(type, null), value, true, type);
    }

    static String formatSignature(@Nullable MessageSignature signature) {
        if (signature == null) {
            return "null";
        }
        return PacketRecordCodec.encodeField(signature);
    }

    static @Nullable MessageSignature parseSignature(@Nullable String raw) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim()) || "unsigned".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return (MessageSignature) PacketRecordCodec.decodeField(MessageSignature.class, null, raw);
    }

    static String formatAcknowledgment(LastSeenMessages.Update acknowledgment) {
        if (acknowledgment == null) {
            return defaultAcknowledgmentText();
        }
        StringBuilder bits = new StringBuilder();
        BitSet set = acknowledgment.acknowledged();
        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
            if (!bits.isEmpty()) {
                bits.append(',');
            }
            bits.append(i);
        }
        return acknowledgment.offset() + ";" + bits + ";" + acknowledgment.checksum();
    }

    static LastSeenMessages.Update parseAcknowledgment(String raw) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank()) {
            return defaultAcknowledgment();
        }
        return (LastSeenMessages.Update) PacketRecordCodec.decodeField(
                LastSeenMessages.Update.class, null, raw);
    }

    static String defaultAcknowledgmentText() {
        return "0;;1";
    }

    static LastSeenMessages.Update defaultAcknowledgment() {
        return new LastSeenMessages.Update(0, new BitSet(), (byte) 1);
    }

    private static Instant parseTimestamp(@Nullable String raw) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        return (Instant) PacketRecordCodec.decodeField(Instant.class, null, raw);
    }

    private static long parseLong(@Nullable String raw, long fallback) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Long.parseLong(raw.trim());
    }

    public static Map<String, String> normalizeFields(Map<String, String> fields) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = mapLegacyFieldName(entry.getKey());
            out.putIfAbsent(key, entry.getValue());
        }
        return out;
    }
}
