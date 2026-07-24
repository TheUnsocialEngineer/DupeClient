package com.dupeclient.client.module.macro;

import com.dupeclient.client.module.macro.graph.MacroGraphEdge;
import com.dupeclient.client.module.macro.graph.MacroGraphGroup;
import com.dupeclient.client.module.macro.graph.MacroGraphNode;
import com.dupeclient.client.module.macro.graph.MacroGraphTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Macro file: {@code formatVersion} 1 = linear {@link #steps} only; 2 adds {@link #nodes} / {@link #edges} graph
 * (runtime still uses a compiled linear plan).
 */
public final class MacroDefinition {
    public int formatVersion = 1;
    public String id = "";
    public String displayName = "";
    public List<MacroStep> steps = new ArrayList<>();
    public List<MacroGraphNode> nodes = new ArrayList<>();
    public List<MacroGraphEdge> edges = new ArrayList<>();
    /** Optional editor-only groups (format v2). */
    public List<MacroGraphGroup> graphGroups = new ArrayList<>();
    /**
     * Optional GLFW hotkey to start/stop this whole macro from the world (no menus open).
     * {@code -1} = unset.
     */
    public int hotkeyKey = -1;
    public int hotkeyMods = 0;
    /** Optional notes shown in export bundles and the macro library. */
    public String description = "";
    /** Optional author tag preserved across export/import. */
    public String author = "";
    /** ISO-8601 timestamp of last save (set by {@link MacroStorage#save}). */
    public String modifiedAt = "";

    public void normalize() {
        if (id == null) {
            id = "";
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = id;
        }
        if (description == null) {
            description = "";
        }
        if (author == null) {
            author = "";
        }
        if (modifiedAt == null) {
            modifiedAt = "";
        }
        if (steps == null) {
            steps = new ArrayList<>();
        }
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        if (edges == null) {
            edges = new ArrayList<>();
        }
        if (graphGroups == null) {
            graphGroups = new ArrayList<>();
        }
        for (MacroGraphEdge e : edges) {
            if (e == null) {
                continue;
            }
            if (e.fromSlot == null || e.fromSlot.isBlank()) {
                e.fromSlot = "";
            } else {
                e.fromSlot = e.fromSlot.trim().toLowerCase();
            }
        }
        for (MacroGraphNode n : nodes) {
            if (n == null) {
                continue;
            }
            if (n.moveForwardMeasure == null || n.moveForwardMeasure.isBlank()) {
                n.moveForwardMeasure = "TICKS";
            } else {
                n.moveForwardMeasure = n.moveForwardMeasure.trim().toUpperCase();
            }
            if (n.moveForwardBlocks < 1) {
                n.moveForwardBlocks = 1;
            }
            n.walkFacing = MacroGraphTypes.normalizeWalkFacing(n.walkFacing);
            n.moveAuxHoldKeyId = MacroHoldKeys.normalizeAuxKey(n.moveAuxHoldKeyId);
            n.moveAuxHoldKey2Id = MacroHoldKeys.normalizeAuxKey(n.moveAuxHoldKey2Id);
            if (n.moveAuxHoldKey2Id.equals(n.moveAuxHoldKeyId)) {
                n.moveAuxHoldKey2Id = "";
            }
            if (!MacroGraphTypes.isRepeatNode(n.type)) {
                n.repeatShowNextPort = false;
            }
            if (n.guiItemMode == null || n.guiItemMode.isBlank()) {
                n.guiItemMode = "PUT";
            } else {
                String g = n.guiItemMode.trim().toUpperCase(Locale.ROOT);
                n.guiItemMode = "TAKE".equals(g) ? "TAKE" : "PUT";
            }
            if (n.guiItemId == null) {
                n.guiItemId = "";
            }
            if (MacroAutomation.isAnyItem(n.guiItemId)) {
                n.guiItemAnyItem = true;
            }
            if (n.guiItemAmountAll || n.guiItemCount < 0) {
                n.guiItemAmountAll = true;
                n.guiItemCount = -1;
            } else if (n.guiItemCount < 1) {
                n.guiItemCount = 1;
            }
            if (n.guiItemDelayTicks < 0) {
                n.guiItemDelayTicks = 0;
            } else if (n.guiItemDelayTicks > 100) {
                n.guiItemDelayTicks = 100;
            }
            n.blockPreset = MacroAutomation.normalizeBlockPreset(n.blockPreset);
            if (n.blockCustomId == null) {
                n.blockCustomId = "";
            } else {
                n.blockCustomId = n.blockCustomId.trim();
            }
            if (n.blockSearchRadius < 1) {
                n.blockSearchRadius = 1;
            } else if (n.blockSearchRadius > 32) {
                n.blockSearchRadius = 32;
            }
            if (n.blockNavigateMaxTicks < 20) {
                n.blockNavigateMaxTicks = 20;
            } else if (n.blockNavigateMaxTicks > 6000) {
                n.blockNavigateMaxTicks = 6000;
            }
            if (n.entityTypeId == null) {
                n.entityTypeId = "";
            } else {
                n.entityTypeId = n.entityTypeId.trim();
            }
        }
        for (MacroStep s : steps) {
            if (s == null) {
                continue;
            }
            if (s.moveMeasure == null || s.moveMeasure.isBlank()) {
                s.moveMeasure = "TICKS";
            } else {
                s.moveMeasure = s.moveMeasure.trim().toUpperCase();
            }
            if (s.moveDistanceBlocks < 1) {
                s.moveDistanceBlocks = 1;
            }
            s.walkFacing = MacroGraphTypes.normalizeWalkFacing(s.walkFacing);
            s.moveAuxHoldKeyId = MacroHoldKeys.normalizeAuxKey(s.moveAuxHoldKeyId);
            s.moveAuxHoldKey2Id = MacroHoldKeys.normalizeAuxKey(s.moveAuxHoldKey2Id);
            if (s.moveAuxHoldKey2Id.equals(s.moveAuxHoldKeyId)) {
                s.moveAuxHoldKey2Id = "";
            }
            if (s.guiItemMode == null || s.guiItemMode.isBlank()) {
                s.guiItemMode = "PUT";
            } else {
                String g = s.guiItemMode.trim().toUpperCase(Locale.ROOT);
                s.guiItemMode = "TAKE".equals(g) ? "TAKE" : "PUT";
            }
            if (s.guiItemId == null) {
                s.guiItemId = "";
            }
            if (s.guiItemCount < -1) {
                s.guiItemCount = -1;
            } else if (s.guiItemCount == 0) {
                s.guiItemCount = 1;
            }
            if (s.guiItemDelayTicks < 0) {
                s.guiItemDelayTicks = 0;
            } else if (s.guiItemDelayTicks > 100) {
                s.guiItemDelayTicks = 100;
            }
            s.blockPreset = MacroAutomation.normalizeBlockPreset(s.blockPreset);
            if (s.blockCustomId == null) {
                s.blockCustomId = "";
            } else {
                s.blockCustomId = s.blockCustomId.trim();
            }
            if (s.blockSearchRadius < 1) {
                s.blockSearchRadius = 1;
            } else if (s.blockSearchRadius > 32) {
                s.blockSearchRadius = 32;
            }
            if (s.blockNavigateMaxTicks < 20) {
                s.blockNavigateMaxTicks = 20;
            } else if (s.blockNavigateMaxTicks > 6000) {
                s.blockNavigateMaxTicks = 6000;
            }
            if (s.entityTypeId == null) {
                s.entityTypeId = "";
            } else {
                s.entityTypeId = s.entityTypeId.trim();
            }
            if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(s.type) || MacroStepType.WAIT_LOOK_ENTITY.name().equals(s.type)) {
                if (s.ticks < 0) {
                    s.ticks = 0;
                }
            }
        }
    }

    public static long packHotkey(int key, int mods) {
        if (key < 0) {
            return -1L;
        }
        return (key & 0xFFFF_FFFFL) | ((long) mods << 32);
    }
}
