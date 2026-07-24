package com.dupeclient.client.module.acaudit;

import java.util.Collections;
import java.util.List;

public final class AcAuditMetrics {
    public final double tps;
    public final int ping;
    public final int setbackRate;
    public final int inRate;
    public final int outRate;
    public final int setbacksMoving;
    public final int setbacksStill;
    public final String brand;
    public final String platform;
    public final String lastDisconnect;
    public final long correctionRttMin;
    public final long correctionRttMax;
    public final long correctionRttAvg;
    public final int correctionCount;
    public final List<String> topPackets;
    public final List<String> anticheatPlugins;
    public final List<String> pluginNamespaces;
    public final int discoveredCommandCount;
    public final String slotSyncProbeLabel;
    public final int slotSyncProbeIndex;
    public final int slotSyncProbeTotal;
    public final int slotSyncPacketsSent;

    public AcAuditMetrics(
            double tps,
            int ping,
            int setbackRate,
            int inRate,
            int outRate,
            int setbacksMoving,
            int setbacksStill,
            String brand,
            String platform,
            String lastDisconnect,
            long correctionRttMin,
            long correctionRttMax,
            long correctionRttAvg,
            int correctionCount,
            List<String> topPackets,
            List<String> anticheatPlugins,
            List<String> pluginNamespaces,
            int discoveredCommandCount,
            String slotSyncProbeLabel,
            int slotSyncProbeIndex,
            int slotSyncProbeTotal,
            int slotSyncPacketsSent) {
        this.tps = tps;
        this.ping = ping;
        this.setbackRate = setbackRate;
        this.inRate = inRate;
        this.outRate = outRate;
        this.setbacksMoving = setbacksMoving;
        this.setbacksStill = setbacksStill;
        this.brand = brand;
        this.platform = platform;
        this.lastDisconnect = lastDisconnect;
        this.correctionRttMin = correctionRttMin;
        this.correctionRttMax = correctionRttMax;
        this.correctionRttAvg = correctionRttAvg;
        this.correctionCount = correctionCount;
        this.topPackets = topPackets != null ? List.copyOf(topPackets) : List.of();
        this.anticheatPlugins = anticheatPlugins != null ? List.copyOf(anticheatPlugins) : List.of();
        this.pluginNamespaces = pluginNamespaces != null ? List.copyOf(pluginNamespaces) : List.of();
        this.discoveredCommandCount = discoveredCommandCount;
        this.slotSyncProbeLabel = slotSyncProbeLabel;
        this.slotSyncProbeIndex = slotSyncProbeIndex;
        this.slotSyncProbeTotal = slotSyncProbeTotal;
        this.slotSyncPacketsSent = slotSyncPacketsSent;
    }

    public static AcAuditMetrics empty() {
        return new AcAuditMetrics(
                20.0,
                -1,
                0,
                0,
                0,
                0,
                0,
                null,
                "unknown",
                null,
                Long.MAX_VALUE,
                0,
                0,
                0,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                null,
                0,
                0,
                0);
    }
}
