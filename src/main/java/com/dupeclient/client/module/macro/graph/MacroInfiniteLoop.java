package com.dupeclient.client.module.macro.graph;

/**
 * Marks a compiled step range that should loop while the macro is running when {@code repeat} count was zero.
 * When {@code stepIndex} reaches {@link #endExclusive()}, it is reset to {@link #startInclusive()}.
 */
public record MacroInfiniteLoop(int startInclusive, int endExclusive) {
    public MacroInfiniteLoop {
        if (startInclusive < 0 || endExclusive < startInclusive) {
            throw new IllegalArgumentException("invalid loop span");
        }
    }
}
