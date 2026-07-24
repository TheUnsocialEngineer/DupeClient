package com.dupeclient.client.module.macro.graph;

import com.dupeclient.client.module.macro.MacroAutomation;
import com.dupeclient.client.module.macro.MacroDefinition;
import com.dupeclient.client.module.macro.MacroHoldKeys;
import com.dupeclient.client.module.macro.MacroKeyPress;
import com.dupeclient.client.module.macro.MacroPromptParser;
import com.dupeclient.client.module.macro.MacroSlotActions;
import com.dupeclient.client.module.macro.MacroStep;
import com.dupeclient.client.module.macro.MacroStepType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Builds a linear run list from either legacy {@link MacroDefinition#steps} or a node graph.
 * With {@link MacroGraphTypes#START} / {@link MacroGraphTypes#END}: runnable steps are only nodes strictly
 * between them on a single chain (empty list is allowed for Start → End with nothing in between).
 * {@link MacroGraphTypes#REPEAT} uses two outputs: {@code fromSlot} {@code "loop"} (mint, “Repeat” body) and
 * {@code "next"} (orange, “Continue” merge); the body is a linear chain ending at the same node as {@code next}.
 */
public final class MacroGraphCompiler {
    private record SlotEdge(String to, String fromSlot) {
    }

    private MacroGraphCompiler() {
    }

    public static List<MacroStep> buildRunPlan(MacroDefinition def) {
        return compileRun(def).steps();
    }

    /** Linear plan plus parallel list of graph node ids (empty string if unknown) for each step. */
    public static MacroCompiledRun compileRun(MacroDefinition def) {
        def.normalize();
        if (def.nodes != null && !def.nodes.isEmpty()) {
            return tryCompile(def.nodes, def.edges == null ? List.of() : def.edges).orElse(MacroCompiledRun.empty());
        }
        if (def.steps == null || def.steps.isEmpty()) {
            return MacroCompiledRun.empty();
        }
        List<MacroStep> steps = new ArrayList<>(def.steps);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ids.add("");
        }
        return MacroCompiledRun.linear(steps, ids);
    }

    /**
     * Converts a linear step list to a graph with Start/End bookends (for upgrading v1 files in the editor).
     */
    public static void stepsToGraph(MacroDefinition def, double startX, double startY, double gapY) {
        def.nodes = new ArrayList<>();
        def.edges = new ArrayList<>();
        MacroGraphNode start = newBookend("__dc_start", MacroGraphTypes.START, "Control", startX, startY - gapY);
        MacroGraphNode end = newBookend("__dc_end", MacroGraphTypes.END, "Control", startX, startY + gapY * Math.max(def.steps.size(), 1) + gapY);
        def.nodes.add(start);
        def.nodes.add(end);
        String prev = start.id;
        int idx = 0;
        for (MacroStep st : def.steps) {
            String id = "n" + idx;
            MacroGraphNode n = new MacroGraphNode();
            n.id = id;
            n.type = st.type == null ? "" : st.type;
            n.category = "utility";
            n.x = startX;
            n.y = startY + idx * gapY;
            n.ticks = st.ticks;
            n.text = st.text == null ? "" : st.text;
            if (MacroStepType.MOVE_FORWARD.name().equals(n.type)) {
                if (st.moveMeasure != null && "BLOCKS".equalsIgnoreCase(st.moveMeasure.trim())) {
                    n.moveForwardMeasure = "BLOCKS";
                    n.moveForwardBlocks = Math.max(1, st.moveDistanceBlocks);
                } else {
                    n.moveForwardMeasure = "TICKS";
                    n.moveForwardBlocks = 1;
                }
                n.walkFacing = MacroGraphTypes.normalizeWalkFacing(st.walkFacing);
                n.moveAuxHoldKeyId = MacroHoldKeys.normalizeAuxKey(st.moveAuxHoldKeyId);
                n.moveAuxHoldKey2Id = MacroHoldKeys.normalizeAuxKey(st.moveAuxHoldKey2Id);
                if (n.moveAuxHoldKey2Id.equals(n.moveAuxHoldKeyId)) {
                    n.moveAuxHoldKey2Id = "";
                }
            }
            if (MacroStepType.GUI_ITEM.name().equals(n.type)) {
                n.guiItemMode = st.guiItemMode == null ? "PUT" : st.guiItemMode;
                n.guiItemAnyItem = MacroAutomation.isAnyItem(st.guiItemId);
                n.guiItemId = n.guiItemAnyItem ? "minecraft:cobblestone" : (st.guiItemId == null ? "" : st.guiItemId);
                n.guiItemAmountAll = st.guiItemCount < 0;
                n.guiItemCount = st.guiItemCount < 0 ? 1 : Math.max(1, st.guiItemCount);
                n.guiItemDelayTicks = Math.max(0, Math.min(100, st.guiItemDelayTicks));
            }
            if (MacroStepType.BLOCK_INTERACT.name().equals(n.type)) {
                n.blockPreset = st.blockPreset == null ? "CHEST" : st.blockPreset;
                n.blockCustomId = st.blockCustomId == null ? "" : st.blockCustomId;
                n.blockSearchRadius = Math.max(1, st.blockSearchRadius);
                n.blockNavigateMaxTicks = Math.max(20, st.blockNavigateMaxTicks);
            }
            if (MacroStepType.USE_HOTBAR_ITEM.name().equals(n.type)) {
                n.hotbarSlot = Math.max(0, Math.min(8, st.hotbarSlot));
            }
            if (MacroStepType.KEY_HOLD.name().equals(n.type)) {
                n.holdKeyId = MacroHoldKeys.normalize(st.holdKeyId);
                n.ticks = Math.max(1, st.ticks);
            }
            if (MacroStepType.LOOK_PITCH.name().equals(n.type)) {
                n.ticks = st.ticks;
            }
            if (MacroStepType.HOTBAR_SELECT.name().equals(n.type)) {
                n.hotbarSlot = Math.max(0, Math.min(8, st.hotbarSlot));
            }
            if (MacroStepType.DROP_ITEM.name().equals(n.type)) {
                n.dropFullStack = st.dropFullStack;
            }
            if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(n.type)) {
                n.blockCustomId = st.blockCustomId == null ? "" : st.blockCustomId;
                n.ticks = Math.max(0, st.ticks);
            }
            if (MacroStepType.WAIT_LOOK_ENTITY.name().equals(n.type)) {
                n.entityTypeId = st.entityTypeId == null ? "" : st.entityTypeId;
                n.ticks = Math.max(0, st.ticks);
            }
            if (MacroStepType.CLICK_SLOT.name().equals(n.type)) {
                n.clickSlotId = st.clickSlotId;
                n.clickSlotAction = MacroSlotActions.normalize(st.clickSlotAction);
                n.clickSlotButton = Math.max(0, st.clickSlotButton);
            }
            if (MacroStepType.PRESS_BUTTON.name().equals(n.type)) {
                n.pressKeyCode = MacroKeyPress.normalizeKeyCode(st.pressKeyCode);
                n.pressKeyModifiers = MacroKeyPress.normalizeModifiers(st.pressKeyModifiers);
            }
            def.nodes.add(n);
            MacroGraphEdge e = new MacroGraphEdge();
            e.from = prev;
            e.to = id;
            e.fromSlot = "";
            def.edges.add(e);
            prev = id;
            idx++;
        }
        MacroGraphEdge toEnd = new MacroGraphEdge();
        toEnd.from = prev;
        toEnd.to = end.id;
        toEnd.fromSlot = "";
        def.edges.add(toEnd);
        def.formatVersion = 2;
    }

    private static MacroGraphNode newBookend(String id, String type, String category, double x, double y) {
        MacroGraphNode n = new MacroGraphNode();
        n.id = id;
        n.type = type;
        n.category = category;
        n.x = x;
        n.y = y;
        n.ticks = 0;
        n.text = "";
        return n;
    }

    /**
     * If a v2 graph has no Start/End nodes but has a single-entry single-exit chain, inserts bookends so older
     * editor saves keep working.
     */
    public static void ensureGraphBookends(MacroDefinition def) {
        def.normalize();
        List<MacroGraphNode> nodes = def.nodes;
        List<MacroGraphEdge> edges = def.edges;
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        boolean hasStart = nodes.stream().anyMatch(n -> MacroGraphTypes.START.equals(n.type));
        boolean hasEnd = nodes.stream().anyMatch(n -> MacroGraphTypes.END.equals(n.type));
        if (hasStart && hasEnd) {
            return;
        }
        Map<String, Integer> inDeg = new HashMap<>();
        Map<String, Integer> outDeg = new HashMap<>();
        Map<String, MacroGraphNode> byId = new HashMap<>();
        for (MacroGraphNode n : nodes) {
            if (n.id == null || n.id.isBlank()) {
                continue;
            }
            byId.put(n.id, n);
            inDeg.putIfAbsent(n.id, 0);
            outDeg.putIfAbsent(n.id, 0);
        }
        for (MacroGraphEdge e : edges) {
            if (e == null || e.from == null || e.to == null || e.from.isBlank() || e.to.isBlank()) {
                continue;
            }
            if (!byId.containsKey(e.from) || !byId.containsKey(e.to)) {
                continue;
            }
            outDeg.merge(e.from, 1, Integer::sum);
            inDeg.merge(e.to, 1, Integer::sum);
        }
        List<String> heads = new ArrayList<>();
        List<String> tails = new ArrayList<>();
        for (MacroGraphNode n : byId.values()) {
            if (inDeg.getOrDefault(n.id, 0) == 0) {
                heads.add(n.id);
            }
            if (outDeg.getOrDefault(n.id, 0) == 0) {
                tails.add(n.id);
            }
        }
        if (!hasStart && !hasEnd) {
            if (heads.size() == 1 && tails.size() == 1) {
                String h = heads.get(0);
                String t = tails.get(0);
                double minY = byId.values().stream().mapToDouble(n -> n.y).min().orElse(0);
                MacroGraphNode s = newBookend(uniqueId(byId, "__dc_start"), MacroGraphTypes.START, "Control", byId.get(h).x, minY - 80);
                MacroGraphNode e = newBookend(uniqueId(byId, "__dc_end"), MacroGraphTypes.END, "Control", byId.get(t).x, minY + 400);
                nodes.add(s);
                nodes.add(e);
                MacroGraphEdge e1 = new MacroGraphEdge();
                e1.from = s.id;
                e1.to = h;
                e1.fromSlot = "";
                edges.add(e1);
                MacroGraphEdge e2 = new MacroGraphEdge();
                e2.from = t;
                e2.to = e.id;
                e2.fromSlot = "";
                edges.add(e2);
            }
            return;
        }
        if (!hasStart && hasEnd) {
            if (heads.size() == 1) {
                String h = heads.get(0);
                MacroGraphNode s = newBookend(uniqueId(byId, "__dc_start"), MacroGraphTypes.START, "Control", byId.get(h).x - 120, byId.get(h).y);
                nodes.add(s);
                MacroGraphEdge e1 = new MacroGraphEdge();
                e1.from = s.id;
                e1.to = h;
                e1.fromSlot = "";
                edges.add(e1);
            }
            return;
        }
        if (hasStart && !hasEnd) {
            if (tails.size() == 1) {
                String t = tails.get(0);
                MacroGraphNode e = newBookend(uniqueId(byId, "__dc_end"), MacroGraphTypes.END, "Control", byId.get(t).x + 120, byId.get(t).y);
                nodes.add(e);
                MacroGraphEdge e2 = new MacroGraphEdge();
                e2.from = t;
                e2.to = e.id;
                e2.fromSlot = "";
                edges.add(e2);
            }
        }
    }

    private static String uniqueId(Map<String, MacroGraphNode> byId, String base) {
        String id = base;
        int i = 0;
        while (byId.containsKey(id)) {
            id = base + "_" + (++i);
        }
        return id;
    }

    private static Optional<MacroCompiledRun> tryCompile(List<MacroGraphNode> nodes, List<MacroGraphEdge> edges) {
        Map<String, MacroGraphNode> byId = new HashMap<>();
        for (MacroGraphNode n : nodes) {
            if (n.id != null && !n.id.isBlank()) {
                byId.put(n.id, n);
            }
        }
        if (byId.isEmpty()) {
            return Optional.empty();
        }
        Map<String, List<SlotEdge>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (MacroGraphNode n : byId.values()) {
            inDegree.putIfAbsent(n.id, 0);
            outgoing.putIfAbsent(n.id, new ArrayList<>());
        }
        for (MacroGraphEdge e : edges) {
            if (e == null || e.from == null || e.to == null || e.from.isBlank() || e.to.isBlank()) {
                continue;
            }
            if (!byId.containsKey(e.from) || !byId.containsKey(e.to)) {
                continue;
            }
            String slot = e.fromSlot == null || e.fromSlot.isBlank() ? "" : e.fromSlot.trim().toLowerCase();
            outgoing.get(e.from).add(new SlotEdge(e.to, slot));
            inDegree.merge(e.to, 1, Integer::sum);
        }
        long startCount = byId.values().stream().filter(n -> MacroGraphTypes.START.equals(n.type)).count();
        long endCount = byId.values().stream().filter(n -> MacroGraphTypes.END.equals(n.type)).count();
        if (startCount == 1 && endCount == 1) {
            String endNodeId = null;
            for (MacroGraphNode n : byId.values()) {
                if (MacroGraphTypes.END.equals(n.type)) {
                    endNodeId = n.id;
                    break;
                }
            }
            if (endNodeId != null) {
                injectImplicitRepeatNextToBookendEnd(byId, outgoing, inDegree, endNodeId);
            }
        }
        if (!validateSlotOutEdges(byId, outgoing)) {
            return Optional.empty();
        }
        boolean useBookends = startCount > 0 || endCount > 0;
        if (useBookends) {
            if (startCount != 1 || endCount != 1) {
                return Optional.empty();
            }
            String startNodeId = null;
            String endNodeId = null;
            for (MacroGraphNode n : byId.values()) {
                if (MacroGraphTypes.START.equals(n.type)) {
                    startNodeId = n.id;
                } else if (MacroGraphTypes.END.equals(n.type)) {
                    endNodeId = n.id;
                }
            }
            if (inDegree.getOrDefault(startNodeId, 0) != 0) {
                return Optional.empty();
            }
            List<SlotEdge> outEnd = outgoing.get(endNodeId);
            if (outEnd != null && !outEnd.isEmpty()) {
                return Optional.empty();
            }
            return walkBookended(startNodeId, endNodeId, byId, outgoing);
        }
        List<String> heads = new ArrayList<>();
        for (MacroGraphNode n : byId.values()) {
            if (inDegree.getOrDefault(n.id, 0) == 0) {
                heads.add(n.id);
            }
        }
        if (heads.size() != 1) {
            return Optional.empty();
        }
        return walkLegacy(heads.get(0), byId, outgoing);
    }

    private static boolean validateSlotOutEdges(Map<String, MacroGraphNode> byId, Map<String, List<SlotEdge>> outgoing) {
        for (MacroGraphNode n : byId.values()) {
            List<SlotEdge> es = outgoing.getOrDefault(n.id, List.of());
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                long loops = es.stream().filter(s -> "loop".equals(s.fromSlot)).count();
                long nexts = es.stream().filter(s -> "next".equals(s.fromSlot)).count();
                long defaults = es.stream().filter(s -> s.fromSlot.isEmpty()).count();
                if (defaults != 0 || loops != 1 || nexts != 1 || es.size() != 2) {
                    return false;
                }
            } else {
                long defaults = es.stream().filter(s -> s.fromSlot.isEmpty()).count();
                long special = es.stream().filter(s -> !s.fromSlot.isEmpty()).count();
                if (defaults > 1 || special > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Optional<String> singleDefaultSuccessor(String from, Map<String, List<SlotEdge>> outgoing) {
        List<SlotEdge> es = outgoing.getOrDefault(from, List.of());
        List<SlotEdge> def = es.stream().filter(s -> s.fromSlot.isEmpty()).toList();
        if (def.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(def.getFirst().to);
    }

    private static Optional<String> repeatSlotTarget(String repeatId, String slot, Map<String, List<SlotEdge>> outgoing) {
        return outgoing.getOrDefault(repeatId, List.of()).stream()
                .filter(s -> slot.equals(s.fromSlot))
                .map(SlotEdge::to)
                .findFirst();
    }

    /**
     * When a {@link MacroGraphTypes#REPEAT} has a {@code loop} wire but no {@code next} wire, and the graph has
     * exactly one Start and one End bookend, adds a synthetic {@code next} edge to End if the loop body is a
     * single default-successor chain (no nested repeats) that reaches that End.
     */
    private static void injectImplicitRepeatNextToBookendEnd(
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            Map<String, Integer> inDegree,
            String endNodeId) {
        for (MacroGraphNode n : byId.values()) {
            if (!MacroGraphTypes.isRepeatNode(n.type)) {
                continue;
            }
            String rid = n.id;
            List<SlotEdge> es = outgoing.get(rid);
            long loops = es.stream().filter(s -> "loop".equals(s.fromSlot)).count();
            long nexts = es.stream().filter(s -> "next".equals(s.fromSlot)).count();
            if (loops != 1 || nexts >= 1) {
                continue;
            }
            Optional<String> loopHead = repeatSlotTarget(rid, "loop", outgoing);
            if (loopHead.isEmpty()) {
                continue;
            }
            if (!uniqueDefaultChainReachesEnd(byId, outgoing, loopHead.get(), endNodeId)) {
                continue;
            }
            es.add(new SlotEdge(endNodeId, "next"));
            inDegree.merge(endNodeId, 1, Integer::sum);
        }
    }

    /**
     * Follows default (non-slot) edges from {@code from}; fails on cycles, branches, nested repeats, or missing links.
     */
    private static boolean uniqueDefaultChainReachesEnd(
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            String from,
            String endNodeId) {
        Set<String> seen = new HashSet<>();
        String cur = from;
        while (true) {
            if (!seen.add(cur)) {
                return false;
            }
            MacroGraphNode node = byId.get(cur);
            if (node == null) {
                return false;
            }
            if (MacroGraphTypes.END.equals(node.type)) {
                return endNodeId.equals(cur);
            }
            if (MacroGraphTypes.isRepeatNode(node.type)) {
                return false;
            }
            Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
            if (nx.isEmpty()) {
                return false;
            }
            cur = nx.get();
        }
    }

    /**
     * Walks from {@code from} (exclusive of merge) to {@code mergeId}, appending runnable steps.
     * Nested {@link MacroGraphTypes#REPEAT} is expanded inline.
     */
    /** @return true if the body from {@code from} reaches {@code mergeId} legally */
    private static boolean appendBodyUntilMerge(
            String from,
            String mergeId,
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            Set<String> loopGuard,
            Set<String> visitedAll,
            List<MacroStep> plan,
            List<String> ids,
            List<MacroInfiniteLoop> infiniteLoops) {
        String cur = from;
        while (!cur.equals(mergeId)) {
            if (!loopGuard.add(cur)) {
                return false;
            }
            if (!visitedAll.add(cur)) {
                return false;
            }
            MacroGraphNode n = byId.get(cur);
            if (n == null || MacroGraphTypes.END.equals(n.type)) {
                return false;
            }
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                Optional<String> after = expandRepeatIntoPlan(cur, byId, outgoing, visitedAll, plan, ids, infiniteLoops);
                if (after.isEmpty()) {
                    return false;
                }
                cur = after.get();
                continue;
            }
            plan.add(nodeToStep(n));
            ids.add(cur);
            Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
            if (nx.isEmpty()) {
                return false;
            }
            cur = nx.get();
        }
        return true;
    }

    private static Optional<String> expandRepeatIntoPlan(
            String repeatId,
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            Set<String> visitedAll,
            List<MacroStep> plan,
            List<String> ids,
            List<MacroInfiniteLoop> infiniteLoops) {
        MacroGraphNode r = byId.get(repeatId);
        if (r == null) {
            return Optional.empty();
        }
        Optional<String> loopHead = repeatSlotTarget(repeatId, "loop", outgoing);
        Optional<String> nextMerge = repeatSlotTarget(repeatId, "next", outgoing);
        if (loopHead.isEmpty() || nextMerge.isEmpty()) {
            return Optional.empty();
        }
        List<MacroStep> oneRoundSteps = new ArrayList<>();
        List<String> oneRoundIds = new ArrayList<>();
        if (!appendBodyUntilMerge(
                loopHead.get(),
                nextMerge.get(),
                byId,
                outgoing,
                new HashSet<>(),
                visitedAll,
                oneRoundSteps,
                oneRoundIds,
                infiniteLoops)) {
            return Optional.empty();
        }
        int times = r.ticks;
        if (times <= 0) {
            if (oneRoundSteps.isEmpty()) {
                return Optional.empty();
            }
            int start = plan.size();
            plan.addAll(oneRoundSteps);
            ids.addAll(oneRoundIds);
            infiniteLoops.add(new MacroInfiniteLoop(start, plan.size()));
            return Optional.of(nextMerge.get());
        }
        int nTimes = Math.max(1, times);
        for (int i = 0; i < nTimes; i++) {
            plan.addAll(oneRoundSteps);
            ids.addAll(oneRoundIds);
        }
        return Optional.of(nextMerge.get());
    }

    private static Optional<MacroCompiledRun> walkBookended(
            String startNodeId,
            String endNodeId,
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing) {
        List<MacroStep> plan = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<MacroInfiniteLoop> infiniteLoops = new ArrayList<>();
        Set<String> visitedAll = new HashSet<>();
        String cur = startNodeId;
        while (true) {
            MacroGraphNode node = byId.get(cur);
            if (node == null) {
                return Optional.empty();
            }
            if (MacroGraphTypes.END.equals(node.type)) {
                if (!endNodeId.equals(cur)) {
                    return Optional.empty();
                }
                if (!visitedAll.add(cur)) {
                    return Optional.empty();
                }
                break;
            }
            if (!visitedAll.add(cur)) {
                return Optional.empty();
            }
            if (MacroGraphTypes.START.equals(node.type)) {
                Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
                if (nx.isEmpty()) {
                    return Optional.empty();
                }
                cur = nx.get();
                continue;
            }
            if (MacroGraphTypes.isRepeatNode(node.type)) {
                Optional<String> after = expandRepeatIntoPlan(cur, byId, outgoing, visitedAll, plan, ids, infiniteLoops);
                if (after.isEmpty()) {
                    return Optional.empty();
                }
                cur = after.get();
                continue;
            }
            plan.add(nodeToStep(node));
            ids.add(cur);
            Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
            if (nx.isEmpty()) {
                return Optional.empty();
            }
            cur = nx.get();
        }
        if (visitedAll.size() != byId.size()) {
            return Optional.empty();
        }
        return Optional.of(new MacroCompiledRun(plan, ids, infiniteLoops));
    }

    private static Optional<MacroCompiledRun> walkLegacy(String startId, Map<String, MacroGraphNode> byId, Map<String, List<SlotEdge>> outgoing) {
        List<MacroStep> plan = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<MacroInfiniteLoop> infiniteLoops = new ArrayList<>();
        Set<String> visitedAll = new HashSet<>();
        String cur = startId;
        while (cur != null) {
            MacroGraphNode node = byId.get(cur);
            if (node == null) {
                return Optional.empty();
            }
            if (!visitedAll.add(cur)) {
                return Optional.empty();
            }
            if (MacroGraphTypes.isRepeatNode(node.type)) {
                Optional<String> after = expandRepeatIntoPlan(cur, byId, outgoing, visitedAll, plan, ids, infiniteLoops);
                if (after.isEmpty()) {
                    return Optional.empty();
                }
                cur = after.get();
                continue;
            }
            plan.add(nodeToStep(node));
            ids.add(cur);
            Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
            if (nx.isEmpty()) {
                cur = null;
            } else {
                cur = nx.get();
            }
        }
        if (visitedAll.size() != byId.size()) {
            return Optional.empty();
        }
        return Optional.of(new MacroCompiledRun(plan, ids, infiniteLoops));
    }

    private static MacroStep nodeToStep(MacroGraphNode n) {
        String t = n.type == null ? "" : n.type.trim();
        MacroStep s = new MacroStep();
        switch (t) {
            case "SEND_CHAT" -> {
                s.type = MacroStepType.SEND_CHAT.name();
                s.text = n.text == null || n.text.isBlank() ? "Hello" : n.text;
                s.ticks = 0;
            }
            case "WAIT_TICKS" -> {
                s.type = MacroStepType.WAIT_TICKS.name();
                s.ticks = Math.max(1, n.ticks);
                s.text = "";
            }
            case "CLOSE_SCREEN" -> {
                s.type = MacroStepType.CLOSE_SCREEN.name();
                s.ticks = 0;
                s.text = "";
            }
            case "CLOSE_GUI" -> {
                s.type = MacroStepType.CLOSE_GUI.name();
                s.ticks = 0;
                s.text = "";
            }
            case "UI_UTILS_TOGGLE_DELAY" -> {
                s.type = MacroStepType.UI_UTILS_TOGGLE_DELAY.name();
                s.ticks = 0;
                s.text = "";
            }
            case "UI_UTILS_FLUSH_QUEUE" -> {
                s.type = MacroStepType.UI_UTILS_FLUSH_QUEUE.name();
                s.ticks = 0;
                s.text = "";
            }
            case "PACKET_DELAY_TOGGLE" -> {
                s.type = MacroStepType.PACKET_DELAY_TOGGLE.name();
                s.ticks = 0;
                s.text = "";
            }
            case "PACKET_DELAY_FLUSH" -> {
                s.type = MacroStepType.PACKET_DELAY_FLUSH.name();
                s.ticks = 0;
                s.text = "";
            }
            case "FABRICATOR_SEND" -> {
                s.type = MacroStepType.FABRICATOR_SEND.name();
                s.ticks = 0;
                s.text = "";
                s.fabricatorSlot = n.fabricatorSlot == null || n.fabricatorSlot.isBlank() ? "0" : n.fabricatorSlot.trim();
                s.fabricatorTimes = Math.max(1, n.fabricatorTimes);
                s.fabricatorActionIndex = Math.max(0, n.fabricatorActionIndex);
            }
            case "MOVE_FORWARD" -> {
                s.type = MacroStepType.MOVE_FORWARD.name();
                if ("BLOCKS".equalsIgnoreCase(n.moveForwardMeasure)) {
                    s.moveMeasure = "BLOCKS";
                    s.moveDistanceBlocks = Math.max(1, n.moveForwardBlocks);
                    s.ticks = 0;
                } else {
                    s.moveMeasure = "TICKS";
                    s.moveDistanceBlocks = 1;
                    s.ticks = Math.max(1, n.ticks);
                }
                s.text = "";
                s.walkFacing = MacroGraphTypes.normalizeWalkFacing(n.walkFacing);
                s.moveAuxHoldKeyId = MacroHoldKeys.normalizeAuxKey(n.moveAuxHoldKeyId);
                s.moveAuxHoldKey2Id = MacroHoldKeys.normalizeAuxKey(n.moveAuxHoldKey2Id);
                if (s.moveAuxHoldKey2Id.equals(s.moveAuxHoldKeyId)) {
                    s.moveAuxHoldKey2Id = "";
                }
            }
            case MacroGraphTypes.REPEAT -> throw new IllegalStateException("REPEAT must be compiled out");
            case "LOOK_TURN" -> {
                s.type = MacroStepType.LOOK_TURN.name();
                s.ticks = Math.max(-3600, Math.min(3600, n.ticks));
                s.text = "";
            }
            case "LOOK_PITCH" -> {
                s.type = MacroStepType.LOOK_PITCH.name();
                s.ticks = Math.max(-1800, Math.min(1800, n.ticks));
                s.text = "";
            }
            case "KEY_HOLD", "ATTACK" -> {
                s.type = MacroStepType.KEY_HOLD.name();
                s.ticks = Math.max(1, n.ticks);
                s.text = "";
                s.holdKeyId = "ATTACK".equals(t) ? "ATTACK" : MacroHoldKeys.normalize(n.holdKeyId);
            }
            case "GUI_ITEM" -> {
                s.type = MacroStepType.GUI_ITEM.name();
                s.ticks = 0;
                s.text = "";
                s.guiItemMode = n.guiItemMode == null || n.guiItemMode.isBlank() ? "PUT" : n.guiItemMode.trim();
                boolean anyItem = n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId);
                s.guiItemId = anyItem
                        ? MacroPromptParser.ANY_ITEM
                        : (n.guiItemId == null ? "" : n.guiItemId.trim());
                s.guiItemCount = n.guiItemAmountAll || n.guiItemCount < 0 ? -1 : Math.max(1, n.guiItemCount);
                s.guiItemDelayTicks = Math.max(0, Math.min(100, n.guiItemDelayTicks));
            }
            case "CLICK_SLOT" -> {
                s.type = MacroStepType.CLICK_SLOT.name();
                s.ticks = 0;
                s.text = "";
                s.clickSlotId = n.clickSlotId;
                s.clickSlotAction = MacroSlotActions.normalize(n.clickSlotAction);
                s.clickSlotButton = Math.max(0, n.clickSlotButton);
            }
            case "PRESS_BUTTON" -> {
                s.type = MacroStepType.PRESS_BUTTON.name();
                s.ticks = 0;
                s.text = "";
                s.pressKeyCode = MacroKeyPress.normalizeKeyCode(n.pressKeyCode);
                s.pressKeyModifiers = MacroKeyPress.normalizeModifiers(n.pressKeyModifiers);
            }
            case "BLOCK_INTERACT", "USE_BLOCK" -> {
                s.type = MacroStepType.BLOCK_INTERACT.name();
                s.ticks = 0;
                s.text = "";
                s.blockPreset = MacroAutomation.normalizeBlockPreset(n.blockPreset);
                s.blockCustomId = n.blockCustomId == null ? "" : n.blockCustomId.trim();
                s.blockSearchRadius = Math.max(1, Math.min(32, n.blockSearchRadius));
                s.blockNavigateMaxTicks = Math.max(20, n.blockNavigateMaxTicks);
            }
            case "USE_HOTBAR_ITEM", "USE_ITEM" -> {
                s.type = MacroStepType.USE_HOTBAR_ITEM.name();
                s.ticks = 0;
                s.text = "";
                s.hotbarSlot = Math.max(0, Math.min(8, n.hotbarSlot));
            }
            case "HOTBAR_SELECT" -> {
                s.type = MacroStepType.HOTBAR_SELECT.name();
                s.ticks = 0;
                s.text = "";
                s.hotbarSlot = Math.max(0, Math.min(8, n.hotbarSlot));
            }
            case "SWAP_OFFHAND" -> {
                s.type = MacroStepType.KEY_HOLD.name();
                s.ticks = 2;
                s.text = "";
                s.holdKeyId = MacroHoldKeys.normalize("SWAP_HANDS");
            }
            case "DROP_ITEM" -> {
                s.type = MacroStepType.DROP_ITEM.name();
                s.ticks = 0;
                s.text = "";
                s.dropFullStack = n.dropFullStack;
            }
            case "WAIT_LOOK_BLOCK" -> {
                s.type = MacroStepType.WAIT_LOOK_BLOCK.name();
                s.ticks = Math.max(0, n.ticks);
                s.text = "";
                s.blockCustomId = n.blockCustomId == null ? "" : n.blockCustomId.trim();
            }
            case "WAIT_LOOK_ENTITY" -> {
                s.type = MacroStepType.WAIT_LOOK_ENTITY.name();
                s.ticks = Math.max(0, n.ticks);
                s.text = "";
                s.entityTypeId = n.entityTypeId == null ? "" : n.entityTypeId.trim();
            }
            default -> {
                MacroStepType kt = MacroStepType.fromString(t);
                if (kt != MacroStepType.UNKNOWN) {
                    s.type = kt.name();
                    s.ticks = Math.max(0, n.ticks);
                    s.text = n.text == null ? "" : n.text;
                } else {
                    s.type = MacroStepType.WAIT_TICKS.name();
                    s.ticks = 1;
                    s.text = "";
                }
            }
        }
        return s;
    }

    /**
     * Ordered human-readable compile/validation issues (empty if the graph compiles).
     * Safe to call from UI threads; does not mutate the definition beyond {@link MacroDefinition#normalize()}.
     */
    public static List<String> collectDiagnostics(MacroDefinition def) {
        List<String> out = new ArrayList<>();
        def.normalize();
        List<MacroGraphNode> nodes = def.nodes != null ? def.nodes : List.of();
        List<MacroGraphEdge> edges = def.edges != null ? def.edges : List.of();
        if (nodes.isEmpty()) {
            out.add("Graph is empty — drag nodes from the palette onto the canvas.");
            return out;
        }
        boolean sawNullNode = false;
        int blankIdCount = 0;
        Map<String, Integer> idCounts = new HashMap<>();
        for (MacroGraphNode n : nodes) {
            if (n == null) {
                sawNullNode = true;
                continue;
            }
            if (n.id == null || n.id.isBlank()) {
                blankIdCount++;
                continue;
            }
            idCounts.merge(n.id.trim(), 1, Integer::sum);
        }
        if (sawNullNode) {
            out.add("Graph contains null node entries (file may be corrupt).");
        }
        if (blankIdCount > 0) {
            out.add(blankIdCount == 1
                    ? "One node has a blank id — every node needs a stable id for edges."
                    : blankIdCount + " nodes have blank ids — every node needs a stable id for edges.");
        }
        for (Map.Entry<String, Integer> e : idCounts.entrySet()) {
            if (e.getValue() > 1) {
                out.add("Duplicate id \"" + e.getKey() + "\" on " + e.getValue() + " nodes — remove duplicates so edges resolve correctly.");
            }
        }
        if (!out.isEmpty()) {
            return out;
        }

        Map<String, MacroGraphNode> byId = new HashMap<>();
        for (MacroGraphNode n : nodes) {
            if (n != null && n.id != null && !n.id.isBlank()) {
                byId.put(n.id.trim(), n);
            }
        }
        for (MacroGraphEdge e : edges) {
            if (e == null || e.from == null || e.to == null) {
                continue;
            }
            if (e.from.isBlank() || e.to.isBlank()) {
                out.add("An edge has a blank from/to field.");
                continue;
            }
            if (!byId.containsKey(e.from.trim())) {
                out.add("Edge references unknown source id \"" + e.from + "\".");
            }
            if (!byId.containsKey(e.to.trim())) {
                out.add("Edge references unknown target id \"" + e.to + "\".");
            }
        }
        if (!out.isEmpty()) {
            return out;
        }

        Map<String, List<SlotEdge>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (MacroGraphNode n : byId.values()) {
            inDegree.putIfAbsent(n.id, 0);
            outgoing.putIfAbsent(n.id, new ArrayList<>());
        }
        for (MacroGraphEdge e : edges) {
            if (e == null || e.from == null || e.to == null || e.from.isBlank() || e.to.isBlank()) {
                continue;
            }
            String from = e.from.trim();
            String to = e.to.trim();
            if (!byId.containsKey(from) || !byId.containsKey(to)) {
                continue;
            }
            String slot = e.fromSlot == null || e.fromSlot.isBlank() ? "" : e.fromSlot.trim().toLowerCase();
            outgoing.get(from).add(new SlotEdge(to, slot));
            inDegree.merge(to, 1, Integer::sum);
        }

        long startCountEarly = byId.values().stream().filter(n -> MacroGraphTypes.START.equals(n.type)).count();
        long endCountEarly = byId.values().stream().filter(n -> MacroGraphTypes.END.equals(n.type)).count();
        if (startCountEarly == 1 && endCountEarly == 1) {
            String endEarly = null;
            for (MacroGraphNode n : byId.values()) {
                if (MacroGraphTypes.END.equals(n.type)) {
                    endEarly = n.id;
                    break;
                }
            }
            if (endEarly != null) {
                injectImplicitRepeatNextToBookendEnd(byId, outgoing, inDegree, endEarly);
            }
        }

        collectSlotOutEdgeIssues(byId, outgoing, out);

        long startCount = byId.values().stream().filter(n -> MacroGraphTypes.START.equals(n.type)).count();
        long endCount = byId.values().stream().filter(n -> MacroGraphTypes.END.equals(n.type)).count();
        boolean useBookends = startCount > 0 || endCount > 0;
        if (useBookends) {
            if (startCount == 0) {
                out.add("Graph has an End node but no Start node — add exactly one Start.");
            } else if (startCount > 1) {
                out.add("Multiple Start nodes (" + startCount + ") — keep exactly one.");
            }
            if (endCount == 0) {
                out.add("Graph has a Start node but no End node — add exactly one End.");
            } else if (endCount > 1) {
                out.add("Multiple End nodes (" + endCount + ") — keep exactly one.");
            }
            if (startCount == 1 && endCount == 1) {
                String startNodeId = null;
                String endNodeId = null;
                for (MacroGraphNode n : byId.values()) {
                    if (MacroGraphTypes.START.equals(n.type)) {
                        startNodeId = n.id;
                    } else if (MacroGraphTypes.END.equals(n.type)) {
                        endNodeId = n.id;
                    }
                }
                if (startNodeId != null && inDegree.getOrDefault(startNodeId, 0) != 0) {
                    out.add("START must have no incoming wires (disconnect edges going into Start).");
                }
                if (endNodeId != null) {
                    List<SlotEdge> outEnd = outgoing.get(endNodeId);
                    if (outEnd != null && !outEnd.isEmpty()) {
                        out.add("END must have no outgoing wires (disconnect edges leaving End).");
                    }
                }
            }
        } else {
            List<String> heads = new ArrayList<>();
            for (MacroGraphNode n : byId.values()) {
                if (inDegree.getOrDefault(n.id, 0) == 0) {
                    heads.add(n.id);
                }
            }
            if (heads.isEmpty()) {
                out.add("Legacy graph has no head (in-degree 0) — every node is wired into; add Start/End or break a cycle.");
            } else if (heads.size() > 1) {
                out.add("Legacy graph has " + heads.size() + " head nodes (" + String.join(", ", heads) + ") — use one chain or add Start/End bookends.");
            }
        }

        if (!out.isEmpty()) {
            return out;
        }

        boolean compileOk = tryCompile(nodes, edges).isPresent();
        if (!compileOk) {
            int before = out.size();
            if (useBookends && startCount == 1 && endCount == 1) {
                String startNodeId = null;
                for (MacroGraphNode n : byId.values()) {
                    if (MacroGraphTypes.START.equals(n.type)) {
                        startNodeId = n.id;
                        break;
                    }
                }
                if (startNodeId != null) {
                    appendUnreachableFromStart(out, startNodeId, byId, outgoing, true);
                }
            } else if (!useBookends) {
                List<String> heads = new ArrayList<>();
                for (MacroGraphNode n : byId.values()) {
                    if (inDegree.getOrDefault(n.id, 0) == 0) {
                        heads.add(n.id);
                    }
                }
                if (heads.size() == 1) {
                    appendUnreachableFromStart(out, heads.getFirst(), byId, outgoing, false);
                }
            }
            if (out.size() == before) {
                out.add("Could not compile — wire Start→End, one default output per non-repeat node; Repeat needs a single body chain to End or explicit loop+next wires, no stray branches.");
            }
        } else {
            appendRepeatEmptyBodyHints(byId, outgoing, out);
        }
        collectMacroNodeSemanticWarnings(byId.values(), out);
        return out;
    }

    private static void collectMacroNodeSemanticWarnings(Collection<MacroGraphNode> nodes, List<String> out) {
        for (MacroGraphNode n : nodes) {
            if (n == null || n.type == null) {
                continue;
            }
            String t = n.type.trim();
            if ("GUI_ITEM".equals(t)) {
                boolean anyItem = n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId);
                if (!anyItem) {
                    if (n.guiItemId == null || n.guiItemId.isBlank()) {
                        out.add("GUI item node \"" + n.id + "\": set an item id (e.g. cobblestone or minecraft:diamond).");
                    } else if (MacroAutomation.resolveItem(n.guiItemId) == Items.AIR) {
                        out.add("GUI item node \"" + n.id + "\": unknown item id \"" + n.guiItemId + "\".");
                    }
                }
            }
            if ("BLOCK_INTERACT".equals(t) || "USE_BLOCK".equals(t)) {
                if ("OTHER".equals(MacroAutomation.normalizeBlockPreset(n.blockPreset))) {
                    if (n.blockCustomId == null || n.blockCustomId.isBlank()) {
                        out.add("Block interact node \"" + n.id + "\": preset OTHER needs a block id (minecraft:…).");
                    }
                }
            }
            if ("USE_HOTBAR_ITEM".equals(t) || "USE_ITEM".equals(t)) {
                if (n.hotbarSlot < 0 || n.hotbarSlot > 8) {
                    out.add("Use hotbar item node \"" + n.id + "\": hotbar slot must be 0–8 (0 = leftmost).");
                }
            }
            if ("WAIT_LOOK_BLOCK".equals(t)) {
                if (n.blockCustomId == null || n.blockCustomId.isBlank()) {
                    out.add("Wait look (block) node \"" + n.id + "\": set a block id (crosshair block must match).");
                } else {
                    Identifier bid = MacroAutomation.parseItemId(n.blockCustomId);
                    if (bid == null || BuiltInRegistries.BLOCK.getValue(bid) == null || BuiltInRegistries.BLOCK.getValue(bid) == Blocks.AIR) {
                        out.add("Wait look (block) node \"" + n.id + "\": unknown block id \"" + n.blockCustomId + "\".");
                    }
                }
            }
            if ("WAIT_LOOK_ENTITY".equals(t)) {
                if (n.entityTypeId == null || n.entityTypeId.isBlank()) {
                    out.add("Wait look (entity) node \"" + n.id + "\": set an entity type id (crosshair entity must match).");
                } else {
                    Identifier eid = MacroAutomation.parseItemId(n.entityTypeId);
                    EntityType<?> et = eid == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(eid);
                    if (et == null) {
                        out.add("Wait look (entity) node \"" + n.id + "\": unknown entity type id \"" + n.entityTypeId + "\".");
                    }
                }
            }
        }
    }

    /**
     * Detects Repeat nodes that run multiple times but have no runnable steps between {@code loop} and {@code next}
     * (e.g. chat wired before Repeat instead of inside the loop branch).
     */
    private static void appendRepeatEmptyBodyHints(
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            List<String> out) {
        for (MacroGraphNode r : byId.values()) {
            if (!MacroGraphTypes.isRepeatNode(r.type)) {
                continue;
            }
            int times = r.ticks;
            if (times > 0 && times <= 1) {
                continue;
            }
            Optional<String> loopHead = repeatSlotTarget(r.id, "loop", outgoing);
            Optional<String> merge = repeatSlotTarget(r.id, "next", outgoing);
            if (loopHead.isEmpty() || merge.isEmpty()) {
                continue;
            }
            int bodySteps = countRepeatBodyRunnableSteps(byId, outgoing, loopHead.get(), merge.get());
            if (bodySteps == 0) {
                String head = times <= 0 ? "Repeat ∞" : "Repeat ×" + Math.max(1, times);
                out.add(head + " has no steps inside its loop — wire Start → Repeat first, then mint \"Repeat\" into Send chat (and on to End), not Send chat before Repeat.");
            }
        }
    }

    /**
     * Counts non-control runnable nodes on the default-edge path from {@code from} up to but excluding {@code mergeId}.
     *
     * @return -1 if the path is invalid (branch, cycle, nested repeat, hits End early)
     */
    private static int countRepeatBodyRunnableSteps(
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            String from,
            String mergeId) {
        String cur = from;
        int count = 0;
        Set<String> seen = new HashSet<>();
        while (!cur.equals(mergeId)) {
            if (!seen.add(cur)) {
                return -1;
            }
            MacroGraphNode n = byId.get(cur);
            if (n == null || MacroGraphTypes.END.equals(n.type)) {
                return -1;
            }
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                return -1;
            }
            if (!MacroGraphTypes.START.equals(n.type) && !MacroGraphTypes.isControlNode(n.type)) {
                count++;
            }
            Optional<String> nx = singleDefaultSuccessor(cur, outgoing);
            if (nx.isEmpty()) {
                return -1;
            }
            cur = nx.get();
        }
        return count;
    }

    private static void collectSlotOutEdgeIssues(
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            List<String> out) {
        for (MacroGraphNode n : byId.values()) {
            List<SlotEdge> es = outgoing.getOrDefault(n.id, List.of());
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                long loops = es.stream().filter(s -> "loop".equals(s.fromSlot)).count();
                long nexts = es.stream().filter(s -> "next".equals(s.fromSlot)).count();
                long defaults = es.stream().filter(s -> s.fromSlot.isEmpty()).count();
                if (defaults != 0 || loops != 1 || nexts != 1 || es.size() != 2) {
                    out.add("Repeat \"" + n.id + "\": one mint \"Repeat\" wire; add orange \"Continue\" only if merge is not the graph End, or the body is not one chain to End.");
                }
            } else if (MacroGraphTypes.START.equals(n.type)) {
                long defaults = es.stream().filter(s -> s.fromSlot.isEmpty()).count();
                long special = es.stream().filter(s -> !s.fromSlot.isEmpty()).count();
                if (special > 0) {
                    out.add("START must use a single default output wire (not loop/next ports).");
                } else if (defaults == 0) {
                    out.add("START needs one outgoing wire to the first step.");
                } else if (defaults > 1) {
                    out.add("START has " + defaults + " outgoing wires — keep exactly one.");
                }
            } else if (MacroGraphTypes.END.equals(n.type)) {
                if (!es.isEmpty()) {
                    out.add("END node \"" + n.id + "\" has outgoing wire(s) — END must be last (no outputs).");
                }
            } else {
                long defaults = es.stream().filter(s -> s.fromSlot.isEmpty()).count();
                long special = es.stream().filter(s -> !s.fromSlot.isEmpty()).count();
                if (special > 0) {
                    out.add("Node \"" + n.id + "\" uses loop/next ports but is not a Repeat — remove slot wires or change node type.");
                } else if (defaults == 0) {
                    out.add("Node \"" + n.id + "\" has no outgoing wire — connect its output.");
                } else if (defaults > 1) {
                    out.add("Node \"" + n.id + "\" has " + defaults + " outgoing wires — non-repeat nodes allow only one.");
                }
            }
        }
    }

    /**
     * Breadth-first reachability from {@code startId} following default edges, or loop+next for repeats.
     * When {@code stopAtEnd} is true, expansion stops at END nodes (bookended graphs).
     */
    private static void appendUnreachableFromStart(
            List<String> out,
            String startId,
            Map<String, MacroGraphNode> byId,
            Map<String, List<SlotEdge>> outgoing,
            boolean stopAtEnd) {
        Set<String> reached = new LinkedHashSet<>();
        ArrayDeque<String> dq = new ArrayDeque<>();
        dq.add(startId);
        while (!dq.isEmpty()) {
            String cur = dq.poll();
            if (!reached.add(cur)) {
                continue;
            }
            MacroGraphNode n = byId.get(cur);
            if (n == null) {
                continue;
            }
            if (stopAtEnd && MacroGraphTypes.END.equals(n.type)) {
                continue;
            }
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                repeatSlotTarget(cur, "loop", outgoing).ifPresent(dq::addLast);
                repeatSlotTarget(cur, "next", outgoing).ifPresent(dq::addLast);
            } else {
                singleDefaultSuccessor(cur, outgoing).ifPresent(dq::addLast);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String id : byId.keySet()) {
            if (!reached.contains(id)) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            missing.sort(String.CASE_INSENSITIVE_ORDER);
            int cap = 12;
            if (missing.size() > cap) {
                out.add("Not reachable from START: " + String.join(", ", missing.subList(0, cap)) + ", … (" + missing.size() + " total)");
            } else {
                out.add("Not reachable from START: " + String.join(", ", missing));
            }
        }
    }

    /** Human-readable validation message, or null if OK. */
    public static String validateGraph(MacroDefinition def) {
        List<String> d = collectDiagnostics(def);
        return d.isEmpty() ? null : d.getFirst();
    }

    public static String summarizePlan(MacroDefinition def) {
        def.normalize();
        MacroCompiledRun cr = compileRun(def);
        List<MacroStep> p = cr.steps();
        if (p.isEmpty()) {
            if (def.nodes != null && tryCompile(def.nodes, def.edges == null ? List.of() : def.edges).isPresent()) {
                return "(Start → End, no steps)";
            }
            return "(empty)";
        }
        List<String> bits = new ArrayList<>();
        for (MacroStep s : p) {
            bits.add(Objects.toString(s.type, "?"));
        }
        return String.join(" → ", bits);
    }
}
