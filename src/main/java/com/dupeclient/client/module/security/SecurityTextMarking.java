package com.dupeclient.client.module.security;

import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayDeque;

/**
 * Marks {@link Text} trees so key-resolution spoofing can match OpSec-style "from packet" semantics.
 */
public final class SecurityTextMarking {
    private SecurityTextMarking() {
    }

    public static void markServerSourced(Text root) {
        if (root == null) {
            return;
        }
        ArrayDeque<Text> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Text node = stack.pop();
            TextContent content = node.getContent();
            if (content instanceof SecurityFromServerPacket p) {
                p.dupeclient$setFromServerPacket(true);
            }
            if (content instanceof TranslatableTextContent translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Text argText) {
                        stack.push(argText);
                    }
                }
            }
            for (Text sibling : node.getSiblings()) {
                stack.push(sibling);
            }
        }
    }
}
