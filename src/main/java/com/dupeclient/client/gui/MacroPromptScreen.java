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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        super(Text.literal("Generate macro from prompt"));
        this.parent = parent;
    }

    public static void open(MinecraftClient client, @Nullable Screen parent) {
        if (client == null) {
            return;
        }
        Screen back = parent != null ? parent : client.currentScreen;
        client.setScreen(new MacroPromptScreen(back));
    }

    @Override
    protected void init() {
        contentWidth = Math.min(580, width - PAD * 2);
        contentLeft = (width - contentWidth) / 2;
        int y = 48;

        promptField = new StylishTextFieldWidget(textRenderer, contentLeft, y, contentWidth, FIELD_H,
                Text.literal("Describe steps in plain English…"));
        promptField.setMaxLength(512);
        promptField.setPlaceholder(Text.literal("run /pv enter, put all items, close pv, take all items, close without packet"));
        promptField.setChangedListener(ignored -> refreshPreview());
        addDrawableChild(promptField);
        y += FIELD_H + GAP + 4;

        int half = (contentWidth - GAP) / 2;
        idField = new StylishTextFieldWidget(textRenderer, contentLeft, y, half, FIELD_H, Text.literal("id"));
        idField.setMaxLength(48);
        idField.setPlaceholder(Text.literal("auto-generated"));
        addDrawableChild(idField);

        nameField = new StylishTextFieldWidget(textRenderer, contentLeft + half + GAP, y, half, FIELD_H, Text.literal("name"));
        nameField.setMaxLength(80);
        nameField.setPlaceholder(Text.literal("optional display name"));
        addDrawableChild(nameField);
        y += FIELD_H + GAP + 6;

        int btnW = (contentWidth - GAP * 2) / 3;
        addDrawableChild(new StylishButtonWidget(contentLeft, y, btnW, BTN_H, Text.literal("Refresh preview"), this::refreshPreview));
        addDrawableChild(new StylishButtonWidget(contentLeft + btnW + GAP, y, btnW, BTN_H, Text.literal("Save macro"), this::saveMacro));
        addDrawableChild(new StylishButtonWidget(contentLeft + (btnW + GAP) * 2, y, btnW, BTN_H, Text.literal("Save & edit"), this::saveAndEdit));

        previewTop = y + BTN_H + 16;
        previewBottom = height - PAD - BTN_H - 12;

        addDrawableChild(new StylishButtonWidget(contentLeft, height - PAD - BTN_H, 96, BTN_H, Text.literal("Back"), () -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }));
        addDrawableChild(new StylishButtonWidget(contentLeft + contentWidth - 148, height - PAD - BTN_H, 148, BTN_H,
                Text.literal("Load example"), this::fillExample));

        refreshPreview();
        setInitialFocus(promptField);
    }

    private void fillExample() {
        promptField.setText("run /pv enter, put all items, close pv, open pv, take all items, close without packet");
        if (idField.getText().isBlank()) {
            idField.setText("pv_dump_restore");
        }
        if (nameField.getText().isBlank()) {
            nameField.setText("PV dump + restore (no close packet)");
        }
        refreshPreview();
    }

    private void refreshPreview() {
        lastParse = MacroPromptParser.parse(promptField.getText());
        if (idField.getText().isBlank() && !promptField.getText().isBlank()) {
            idField.setText(MacroPromptGenerator.suggestId(promptField.getText()));
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
                promptField.getText(),
                idField.getText(),
                nameField.getText());
        if (!gen.parse().ok()) {
            feedback("Nothing to save — fix the prompt first.", Formatting.RED);
            return;
        }
        try {
            MacroStorage.save(gen.definition());
            feedback("Saved macro \"" + gen.definition().id + "\" (" + gen.definition().steps.size() + " steps).",
                    Formatting.GREEN);
            if (openEditor) {
                MacroEditorScreen.open(client, gen.definition().id);
            }
        } catch (IOException e) {
            feedback(e.getMessage() == null ? "Save failed" : e.getMessage(), Formatting.RED);
        }
    }

    private void feedback(String msg, Formatting color) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[Macro] ").formatted(Formatting.GOLD)
                    .append(Text.literal(msg).formatted(color)), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, width, height);
        context.fill(0, 0, width, 40, 0xCC0A0E14);
        context.fill(0, 39, width, 40, 0x66334155);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, UiTokens.ACCENT);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Separate steps with commas or \"then\" — chat, GUI moves, waits, closes"),
                width / 2, 26, UiTokens.TEXT_DIM);

        int previewH = Math.max(96, previewBottom - previewTop);
        UiComponents.drawSurfaceCard(context, contentLeft, previewTop, contentWidth, previewH);
        context.drawTextWithShadow(textRenderer, Text.literal("Step preview"), contentLeft + 12, previewTop + 8, UiTokens.ACCENT);

        List<String> lines = buildPreviewLines();
        int lineY = previewTop + 22;
        int maxLines = (previewH - 30) / 11;
        int start = Math.max(0, Math.min(previewScroll, Math.max(0, lines.size() - maxLines)));
        for (int i = start; i < lines.size() && i < start + maxLines; i++) {
            String line = lines.get(i);
            int color = line.startsWith("!") ? 0xFFFF8A80 : (line.startsWith("·") ? UiTokens.TEXT_DIM : UiTokens.TEXT);
            context.drawTextWithShadow(textRenderer, Text.literal(line), contentLeft + 14, lineY, color);
            lineY += 11;
        }
        if (lines.size() > maxLines) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Scroll preview · " + lines.size() + " lines"),
                    contentLeft + 14, previewTop + previewH - 14, UiTokens.TEXT_DIM);
        } else if (lines.isEmpty()) {
            MidnightShapes.fillRoundedRect(context, contentLeft + 12, previewTop + 24, contentWidth - 24, 32, 6, 0x33000000);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Type a prompt above to see steps"),
                    contentLeft + contentWidth / 2, previewTop + 36, UiTokens.TEXT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
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
