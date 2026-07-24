package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import com.dupeclient.client.module.macro.MacroImportResult;
import com.dupeclient.client.module.macro.MacroShare;
import com.dupeclient.client.module.macro.MacroStorage;
import com.dupeclient.client.module.utility.nbtedit.SnbtTextAreaWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Paste or edit macro JSON, then import to the local macro library.
 */
public final class MacroShareScreen extends Screen {
    private static final int PAD = 20;
    private static final int BTN_H = 20;
    private static final int FIELD_H = 20;
    private static final int GAP = 8;

    private final Screen parent;
    @Nullable
    private final String suggestedId;
    private SnbtTextAreaWidget jsonArea;
    private StylishTextFieldWidget idField;
    private String statusLine = "Paste a macro export bundle or raw macro JSON.";
    private int statusColor = UiTokens.TEXT_DIM;
    private int contentLeft;
    private int contentWidth;

    public MacroShareScreen(Screen parent, @Nullable String suggestedId) {
        super(Component.literal("Import macro"));
        this.parent = parent;
        this.suggestedId = suggestedId;
    }

    public static void open(Minecraft client, @Nullable Screen parent, @Nullable String suggestedId) {
        if (client == null) {
            return;
        }
        Screen back = parent != null ? parent : client.screen;
        client.setScreen(new MacroShareScreen(back, suggestedId));
    }

    @Override
    protected void init() {
        contentWidth = Math.min(620, width - PAD * 2);
        contentLeft = (width - contentWidth) / 2;
        int y = 48;

        int areaH = Math.max(160, height - 220);
        jsonArea = new SnbtTextAreaWidget(contentLeft, y, contentWidth, areaH);
        addRenderableWidget(jsonArea);
        y += areaH + GAP;

        String clip = minecraft != null ? minecraft.keyboardHandler.getClipboard() : "";
        if (clip != null && !clip.isBlank() && looksLikeMacroJson(clip)) {
            jsonArea.setText(clip.trim());
        }

        int half = (contentWidth - GAP) / 2;
        idField = StylishTextFieldWidget.create(font, contentLeft, y, half, FIELD_H, Component.literal("Save as id"));
        idField.setMaxLength(64);
        idField.setHint(Component.literal("auto from JSON"));
        if (suggestedId != null && !suggestedId.isBlank()) {
            idField.setValue(suggestedId);
        }
        addRenderableWidget(idField);
        y += FIELD_H + GAP;

        int btnW = (contentWidth - GAP * 2) / 3;
        addRenderableWidget(new StylishButtonWidget(contentLeft, y, btnW, BTN_H, Component.literal("Paste clipboard"), this::pasteClipboard));
        addRenderableWidget(new StylishButtonWidget(contentLeft + btnW + GAP, y, btnW, BTN_H, Component.literal("Validate"), this::validateJson));
        addRenderableWidget(new StylishButtonWidget(contentLeft + (btnW + GAP) * 2, y, btnW, BTN_H, Component.literal("Import"), this::importJson));

        addRenderableWidget(new StylishButtonWidget(contentLeft, height - PAD - BTN_H, 96, BTN_H, Component.literal("Back"), () -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }));
        addRenderableWidget(new StylishButtonWidget(contentLeft + contentWidth - 148, height - PAD - BTN_H, 148, BTN_H,
                Component.literal("Import & edit"), this::importAndEdit));

        setInitialFocus(jsonArea);
        validateJson();
    }

    private static boolean looksLikeMacroJson(String text) {
        String t = text.trim();
        return t.startsWith("{") && (t.contains("\"definition\"") || t.contains("\"formatVersion\"") || t.contains("\"dupeclientMacro\""));
    }

    private void pasteClipboard() {
        if (minecraft == null) {
            return;
        }
        String clip = minecraft.keyboardHandler.getClipboard();
        if (clip == null || clip.isBlank()) {
            setStatus("Clipboard is empty.", 0xFFFF6B6B);
            return;
        }
        jsonArea.setText(clip.trim());
        validateJson();
    }

    private void validateJson() {
        try {
            var def = MacroStorage.parseImportDefinition(jsonArea.text());
            String idHint = idField.getValue().isBlank() ? def.id : idField.getValue().trim();
            if (idField.getValue().isBlank() && def.id != null && !def.id.isBlank()) {
                idField.setValue(def.id);
            }
            String name = def.displayName == null || def.displayName.isBlank() ? idHint : def.displayName;
            int nodes = def.nodes == null ? 0 : def.nodes.size();
            int steps = def.steps == null ? 0 : def.steps.size();
            String shape = def.formatVersion >= 2
                    ? nodes + " graph node(s)"
                    : steps + " linear step(s)";
            setStatus("Valid macro \"" + name + "\" — " + shape + ".", UiTokens.MINT_300);
        } catch (Exception e) {
            setStatus(e.getMessage() == null ? "Invalid JSON" : e.getMessage(), 0xFFFF6B6B);
        }
    }

    private void importJson() {
        performImport(false);
    }

    private void importAndEdit() {
        performImport(true);
    }

    private void performImport(boolean openEditor) {
        if (minecraft == null) {
            return;
        }
        String targetId = idField.getValue().isBlank() ? null : idField.getValue().trim();
        MacroImportResult result = MacroShare.importJson(minecraft, jsonArea.text(), targetId, false);
        if (!result.success()) {
            setStatus(result.error() == null ? "Import failed" : result.error(), 0xFFFF6B6B);
            return;
        }
        if (openEditor) {
            MacroEditorScreen.open(minecraft, result.savedId());
        } else {
            minecraft.setScreen(parent);
        }
    }

    private void setStatus(String line, int color) {
        statusLine = line;
        statusColor = color;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, width, height);
        context.fill(0, 0, width, 40, 0xCC0A0E14);
        context.fill(0, 39, width, 40, 0x66334155);
        context.centeredText(font, title, width / 2, 14, UiTokens.ACCENT);
        context.centeredText(font,
                Component.literal("Paste a DupeClient export bundle or raw macro JSON"),
                width / 2, 26, UiTokens.TEXT_DIM);
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(font, Component.literal(statusLine), contentLeft, height - PAD - BTN_H - 14, statusColor);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
