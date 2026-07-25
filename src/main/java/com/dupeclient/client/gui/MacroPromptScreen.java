package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import com.dupeclient.client.module.macro.MacroPromptGenerator;
import com.dupeclient.client.module.macro.MacroPromptParser;
import com.dupeclient.client.module.macro.MacroStep;
import com.dupeclient.client.module.macro.MacroStorage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Describe a macro in plain English and generate steps automatically.
 */
public final class MacroPromptScreen extends Screen {
    private static final int PAD = 20;
    private static final int BTN_H = 20;
    private static final int FIELD_H = 20;
    private static final int GAP = 8;

    private final Screen parent;
    private StylishTextFieldWidget promptField;
    private StylishTextFieldWidget idField;
    private StylishTextFieldWidget nameField;
    private MacroPromptParser.ParseResult lastParse = new MacroPromptParser.ParseResult(List.of(), List.of(), List.of());
    private int previewScroll;
    private int previewTop;
    private int previewBottom;
    private int contentLeft;
    private int contentWidth;

    public MacroPromptScreen(Screen parent) {
        super(Component.literal("Generate macro from prompt"));
        this.parent = parent;
    }

    public static void open(Minecraft client, @Nullable Screen parent) {
        if (client == null) {
            return;
        }
        Screen back = parent != null ? parent : client.gui.screen();
        client.gui.setScreen(new MacroPromptScreen(back));
    }

    @Override
    protected void init() {
        contentWidth = Math.min(580, width - PAD * 2);
        contentLeft = (width - contentWidth) / 2;
        int y = 48;

        promptField = new StylishTextFieldWidget(font, contentLeft, y, contentWidth, FIELD_H,
                Component.literal("Describe steps in plain English…"));
        promptField.setMaxLength(512);
        promptField.setHint(Component.literal("run /pv enter, put all items, close pv, take all items, close without packet"));
        promptField.setResponder(ignored -> refreshPreview());
        addRenderableWidget(promptField);
        y += FIELD_H + GAP + 4;

        int half = (contentWidth - GAP) / 2;
        idField = new StylishTextFieldWidget(font, contentLeft, y, half, FIELD_H, Component.literal("id"));
        idField.setMaxLength(48);
        idField.setHint(Component.literal("auto-generated"));
        addRenderableWidget(idField);

        nameField = new StylishTextFieldWidget(font, contentLeft + half + GAP, y, half, FIELD_H, Component.literal("name"));
        nameField.setMaxLength(80);
        nameField.setHint(Component.literal("optional display name"));
        addRenderableWidget(nameField);
        y += FIELD_H + GAP + 6;

        int btnW = (contentWidth - GAP * 2) / 3;
        addRenderableWidget(new StylishButtonWidget(contentLeft, y, btnW, BTN_H, Component.literal("Refresh preview"), this::refreshPreview));
        addRenderableWidget(new StylishButtonWidget(contentLeft + btnW + GAP, y, btnW, BTN_H, Component.literal("Save macro"), this::saveMacro));
        addRenderableWidget(new StylishButtonWidget(contentLeft + (btnW + GAP) * 2, y, btnW, BTN_H, Component.literal("Save & edit"), this::saveAndEdit));

        previewTop = y + BTN_H + 16;
        previewBottom = height - PAD - BTN_H - 12;

        addRenderableWidget(new StylishButtonWidget(contentLeft, height - PAD - BTN_H, 96, BTN_H, Component.literal("Back"), () -> {
            if (minecraft != null) {
                minecraft.gui.setScreen(parent);
            }
        }));
        addRenderableWidget(new StylishButtonWidget(contentLeft + contentWidth - 148, height - PAD - BTN_H, 148, BTN_H,
                Component.literal("Load example"), this::fillExample));

        refreshPreview();
        setInitialFocus(promptField);
    }

    private void fillExample() {
        promptField.setValue("run /pv enter, put all items, close pv, open pv, take all items, close without packet");
        if (idField.getValue().isBlank()) {
            idField.setValue("pv_dump_restore");
        }
        if (nameField.getValue().isBlank()) {
            nameField.setValue("PV dump + restore (no close packet)");
        }
        refreshPreview();
    }

    private void refreshPreview() {
        lastParse = MacroPromptParser.parse(promptField.getValue());
        if (idField.getValue().isBlank() && !promptField.getValue().isBlank()) {
            idField.setValue(MacroPromptGenerator.suggestId(promptField.getValue()));
        }
        previewScroll = 0;
    }

    private void saveMacro() {
        save(false);
    }

    private void saveAndEdit() {
        save(true);
    }

    private void save(boolean openEditor) {
        MacroPromptGenerator.Generated gen = MacroPromptGenerator.generate(
                promptField.getValue(),
                idField.getValue(),
                nameField.getValue());
        if (!gen.parse().ok()) {
            feedback("Nothing to save — fix the prompt first.", ChatFormatting.RED);
            return;
        }
        try {
            MacroStorage.save(gen.definition());
            feedback("Saved macro \"" + gen.definition().id + "\" (" + gen.definition().steps.size() + " steps).",
                    ChatFormatting.GREEN);
            if (openEditor) {
                MacroEditorScreen.open(minecraft, gen.definition().id);
            }
        } catch (IOException e) {
            feedback(e.getMessage() == null ? "Save failed" : e.getMessage(), ChatFormatting.RED);
        }
    }

    private void feedback(String msg, ChatFormatting color) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(msg).withStyle(color)));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, width, height);
        context.fill(0, 0, width, 40, 0xCC0A0E14);
        context.fill(0, 39, width, 40, 0x66334155);
        context.centeredText(font, title, width / 2, 14, UiTokens.ACCENT);
        context.centeredText(font,
                Component.literal("Separate steps with commas or \"then\" — chat, GUI moves, waits, closes"),
                width / 2, 26, UiTokens.TEXT_DIM);

        int previewH = Math.max(96, previewBottom - previewTop);
        UiComponents.drawSurfaceCard(context, contentLeft, previewTop, contentWidth, previewH);
        context.text(font, Component.literal("Step preview"), contentLeft + 12, previewTop + 8, UiTokens.ACCENT);

        List<String> lines = buildPreviewLines();
        int lineY = previewTop + 22;
        int maxLines = (previewH - 30) / 11;
        int start = Math.max(0, Math.min(previewScroll, Math.max(0, lines.size() - maxLines)));
        for (int i = start; i < lines.size() && i < start + maxLines; i++) {
            String line = lines.get(i);
            int color = line.startsWith("!") ? 0xFFFF8A80 : (line.startsWith("·") ? UiTokens.TEXT_DIM : UiTokens.TEXT);
            context.text(font, Component.literal(line), contentLeft + 14, lineY, color);
            lineY += 11;
        }
        if (lines.size() > maxLines) {
            context.text(font,
                    Component.literal("Scroll preview · " + lines.size() + " lines"),
                    contentLeft + 14, previewTop + previewH - 14, UiTokens.TEXT_DIM);
        } else if (lines.isEmpty()) {
            MidnightShapes.fillRoundedRect(context, contentLeft + 12, previewTop + 24, contentWidth - 24, 32, 6, 0x33000000);
            context.centeredText(font, Component.literal("Type a prompt above to see steps"),
                    contentLeft + contentWidth / 2, previewTop + 36, UiTokens.TEXT_DIM);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private List<String> buildPreviewLines() {
        List<String> lines = new ArrayList<>();
        if (lastParse.steps().isEmpty()) {
            lines.add("No steps yet.");
            for (String w : lastParse.warnings()) {
                lines.add("! " + w);
            }
            return lines;
        }
        int i = 1;
        for (MacroStep step : lastParse.steps()) {
            lines.add(i++ + ". " + describeStep(step));
        }
        for (String note : lastParse.notes()) {
            lines.add("· " + note);
        }
        for (String w : lastParse.warnings()) {
            lines.add("! " + w);
        }
        return lines;
    }

    private static String describeStep(MacroStep s) {
        String type = s.type == null ? "?" : s.type;
        return switch (type) {
            case "WAIT_TICKS" -> "Wait " + s.ticks + " tick(s)";
            case "SEND_CHAT" -> "Chat: " + (s.text == null ? "" : s.text);
            case "CLOSE_SCREEN" -> "Close screen (no packet)";
            case "CLOSE_GUI" -> "Close GUI (vanilla packet)";
            case "GUI_ITEM" -> {
                String mode = "TAKE".equalsIgnoreCase(s.guiItemMode) ? "Take" : "Put";
                String item = MacroPromptParser.ANY_ITEM.equals(s.guiItemId) ? "any item" : s.guiItemId;
                String amt = s.guiItemCount < 0 ? "all" : String.valueOf(s.guiItemCount);
                yield mode + " " + amt + " × " + item;
            }
            case "MOVE_FORWARD" -> "Walk "
                    + ("BLOCKS".equalsIgnoreCase(s.moveMeasure) ? s.moveDistanceBlocks + " block(s)" : s.ticks + " tick(s)");
            case "LOOK_TURN" -> "Turn yaw " + s.ticks + "°";
            case "BLOCK_INTERACT" -> "Use nearby " + s.blockPreset;
            case "PACKET_DELAY_FLUSH" -> "Flush packet delay queue";
            case "PACKET_DELAY_TOGGLE" -> "Toggle packet delay";
            default -> type;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentLeft && mouseX <= contentLeft + contentWidth
                && mouseY >= previewTop && mouseY <= previewBottom) {
            previewScroll -= (int) Math.signum(verticalAmount);
            previewScroll = Math.max(0, previewScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
