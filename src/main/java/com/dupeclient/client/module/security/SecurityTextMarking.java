package com.dupeclient.client.module.security;

import java.util.ArrayDeque;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Marks {@link Component} trees so key-resolution spoofing can match OpSec-style "from packet" semantics.
 */
public final class SecurityTextMarking {
    private SecurityTextMarking() {
    }

    public static void markServerSourced(Component root) {
        if (root == null) {
            return;
        }
        ArrayDeque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component node = stack.pop();
            ComponentContents content = node.getContents();
            if (content instanceof SecurityFromServerPacket p) {
                p.dupeclient$setFromServerPacket(true);
            }
            if (content instanceof TranslatableContents translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component argText) {
                        stack.push(argText);
                    }
                }
            }
            for (Component sibling : node.getSiblings()) {
                stack.push(sibling);
            }
        }
    }
}
