package com.dupeclient.client.module.macro.graph;

import com.dupeclient.client.module.macro.MacroStep;

import java.util.List;

/**
 * Result of compiling a graph (or legacy steps) into a linear run list plus the graph node id that produced
 * each step (for debugging or future features).
 */
public record MacroCompiledRun(List<MacroStep> steps, List<String> sourceNodeIds, List<MacroInfiniteLoop> infiniteLoops) {
    public MacroCompiledRun {
        steps = steps == null ? List.of() : List.copyOf(steps);
        sourceNodeIds = sourceNodeIds == null ? List.of() : List.copyOf(sourceNodeIds);
        infiniteLoops = infiniteLoops == null ? List.of() : List.copyOf(infiniteLoops);
        if (steps.size() != sourceNodeIds.size()) {
            throw new IllegalArgumentException("steps and sourceNodeIds must match in length");
        }
    }

    public static MacroCompiledRun empty() {
        return new MacroCompiledRun(List.of(), List.of(), List.of());
    }

    /** Linear legacy plan with no graph loops. */
    public static MacroCompiledRun linear(List<MacroStep> steps, List<String> sourceNodeIds) {
        return new MacroCompiledRun(steps, sourceNodeIds, List.of());
    }
}
