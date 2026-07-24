package com.dupeclient.client.module.packet.sniffer;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;
import com.dupeclient.client.module.packet.sniffer.PacketFieldModel;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;

import com.dupeclient.client.module.packet.PacketUtils;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fabricates {@link ChatMessageC2SPacket} using the same shape as captured/resendable chat packets
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
        return packet instanceof ChatMessageC2SPacket;
    }

    public static List<PacketFieldModel> describe(Packet<?> packet) {
        if (!(packet instanceof ChatMessageC2SPacket chat)) {
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
        MessageSignatureData signature = parseSignature(fields.get("signature"));
        LastSeenMessageList.Acknowledgment acknowledgment = parseAcknowledgment(
                fields.getOrDefault("acknowledgment", defaultAcknowledgmentText()));
        return new ChatMessageC2SPacket(message, timestamp, salt, signature, acknowledgment);
    }

    public static String friendlyFieldName(RecordComponent component) {
        return MappingLabelResolver.resolveFieldName(ChatMessageC2SPacket.class, component);
    }

    public static boolean isChatField(String name) {
        return switch (name) {
            case "chatMessage", "timestamp", "salt", "signature", "acknowledgment",
                    "comp_945", "comp_946", "comp_947", "comp_948", "comp_970" -> true;
            default -> false;
        };
    }

    public static String mapLegacyFieldName(String name) {
        return MappingLabelResolver.resolveFieldName(ChatMessageC2SPacket.class, name);
    }

    private static List<PacketFieldModel> describePacket(ChatMessageC2SPacket chat) {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField());
        rows.add(field("chatMessage", String.class, chat.chatMessage()));
        rows.add(field("timestamp", Instant.class, PacketRecordCodec.encodeField(chat.timestamp())));
        rows.add(field("salt", long.class, Long.toString(chat.salt())));
        rows.add(field("signature", MessageSignatureData.class, formatSignature(chat.signature())));
        rows.add(field("acknowledgment", LastSeenMessageList.Acknowledgment.class, formatAcknowledgment(chat.acknowledgment())));
        return rows;
    }

    private static List<PacketFieldModel> templateFields() {
        List<PacketFieldModel> rows = new ArrayList<>();
        rows.add(typeField());
        rows.add(field("chatMessage", String.class, ""));
        rows.add(field("timestamp", Instant.class, Long.toString(Instant.now().toEpochMilli())));
        rows.add(field("salt", long.class, Long.toString(ThreadLocalRandom.current().nextLong())));
        rows.add(field("signature", MessageSignatureData.class, "null"));
        rows.add(field("acknowledgment", LastSeenMessageList.Acknowledgment.class, defaultAcknowledgmentText()));
        return rows;
    }

    private static PacketFieldModel typeField() {
        return new PacketFieldModel("type", "String", TYPE, false, String.class);
    }

    private static PacketFieldModel field(String name, Class<?> type, String value) {
        return new PacketFieldModel(name, PacketRecordCodec.typeLabel(type, null), value, true, type);
    }

    static String formatSignature(@Nullable MessageSignatureData signature) {
        if (signature == null) {
            return "null";
        }
        return PacketRecordCodec.encodeField(signature);
    }

    static @Nullable MessageSignatureData parseSignature(@Nullable String raw) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim()) || "unsigned".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return (MessageSignatureData) PacketRecordCodec.decodeField(MessageSignatureData.class, null, raw);
    }

    static String formatAcknowledgment(LastSeenMessageList.Acknowledgment acknowledgment) {
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

    static LastSeenMessageList.Acknowledgment parseAcknowledgment(String raw) throws PacketRecordCodec.PacketBuildException {
        if (raw == null || raw.isBlank()) {
            return defaultAcknowledgment();
        }
        return (LastSeenMessageList.Acknowledgment) PacketRecordCodec.decodeField(
                LastSeenMessageList.Acknowledgment.class, null, raw);
    }

    static String defaultAcknowledgmentText() {
        return "0;;1";
    }

    static LastSeenMessageList.Acknowledgment defaultAcknowledgment() {
        return new LastSeenMessageList.Acknowledgment(0, new BitSet(), (byte) 1);
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
