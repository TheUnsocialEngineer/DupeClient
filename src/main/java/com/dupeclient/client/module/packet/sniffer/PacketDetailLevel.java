package com.dupeclient.client.module.packet.sniffer;

public enum PacketDetailLevel {
    NAME("Name only"),
    SUMMARY("Summary"),
    FULL("Full");

    public final String label;

    PacketDetailLevel(String label) {
        this.label = label;
    }

    public static PacketDetailLevel fromString(String raw) {
        if (raw == null) {
            return SUMMARY;
        }
        return switch (raw.toLowerCase()) {
            case "name" -> NAME;
            case "full" -> FULL;
            default -> SUMMARY;
        };
    }

    public String configValue() {
        return name().toLowerCase();
    }

    public PacketDetailLevel next() {
        return switch (this) {
            case NAME -> SUMMARY;
            case SUMMARY -> FULL;
            case FULL -> NAME;
        };
    }
}
