package com.dupeclient.client.module.packet.sniffer;

import net.minecraft.network.packet.Packet;

import java.lang.reflect.RecordComponent;
import java.util.List;

public final class PacketDetailFormatter {
    private static final int SUMMARY_MAX = 160;
    private static final int FULL_MAX = 800;

    private PacketDetailFormatter() {
    }

    public static String format(Packet<?> packet, PacketDetailLevel level) {
        if (level == PacketDetailLevel.NAME || packet == null) {
            return "";
        }
        if (level == PacketDetailLevel.FULL) {
            return truncate(fullData(packet).replace("\n", " | "), FULL_MAX);
        }
        String summary = formatRecordSummary(packet);
        if (summary.isBlank() && (PacketMoveCodec.isPlayerMovePacket(packet) || PacketMoveCodec.isPlayerInputPacket(packet))) {
            summary = formatMoveSummary(packet);
        }
        if (summary.isBlank()) {
            summary = safeToString(packet);
        }
        return truncate(summary, SUMMARY_MAX);
    }

    /** Full multiline packet field dump for editor / export. */
    public static String fullData(Packet<?> packet) {
        if (packet == null) {
            return "";
        }
        String move = PacketMoveCodec.toEditable(packet);
        if (!move.isBlank()) {
            return move;
        }
        return PacketRecordCodec.toEditable(packet);
    }

    private static String formatMoveSummary(Packet<?> packet) {
        List<PacketFieldModel> rows = PacketMoveCodec.describe(packet);
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PacketFieldModel row : rows) {
            if ("type".equals(row.name)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(row.name).append('=').append(row.value);
        }
        return sb.toString();
    }

    private static String formatRecordSummary(Packet<?> packet) {
        if (!packet.getClass().isRecord()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (RecordComponent component : packet.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(packet);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(component.getName()).append('=').append(formatValue(value));
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return sb.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        String raw = String.valueOf(value);
        if (raw.length() > 48) {
            return raw.substring(0, 48) + "…";
        }
        return raw;
    }

    private static String safeToString(Packet<?> packet) {
        try {
            return String.valueOf(packet);
        } catch (RuntimeException e) {
            return packet.getClass().getSimpleName();
        }
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return "";
        }
        String oneLine = raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max) + "…";
    }
}
