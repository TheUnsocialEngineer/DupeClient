package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import com.dupeclient.client.module.macro.MacroImportResult;
import com.dupeclient.client.module.macro.MacroShare;
import com.dupeclient.client.module.macro.MacroStorage;
import com.dupeclient.client.module.utility.nbtedit.SnbtTextAreaWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
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
        super(Text.literal("Import macro"));
        this.parent = parent;
        this.suggestedId = suggestedId;
    }

    public static void open(MinecraftClient client, @Nullable Screen parent, @Nullable String suggestedId) {
        if (client == null) {
            return;
        }
        Screen back = parent != null ? parent : client.currentScreen;
        client.setScreen(new MacroShareScreen(back, suggestedId));
    }

    @Override
    protected void init() {
        contentWidth = Math.min(620, width - PAD * 2);
        contentLeft = (width - contentWidth) / 2;
        int y = 48;

        int areaH = Math.max(160, height - 220);
        jsonArea = new SnbtTextAreaWidget(contentLeft, y, contentWidth, areaH);
        addDrawableChild(jsonArea);
        y += areaH + GAP;

        String clip = client != null ? client.keyboard.getClipboard() : "";
        if (clip != null && !clip.isBlank() && looksLikeMacroJson(clip)) {
            jsonArea.setText(clip.trim());
        }

        int half = (contentWidth - GAP) / 2;
        idField = StylishTextFieldWidget.create(textRenderer, contentLeft, y, half, FIELD_H, Text.literal("Save as id"));
        idField.setMaxLength(64);
        idField.setPlaceholder(Text.literal("auto from JSON"));
        if (suggestedId != null && !suggestedId.isBlank()) {
            idField.setText(suggestedId);
        }
        addDrawableChild(idField);
        y += FIELD_H + GAP;

        int btnW = (contentWidth - GAP * 2) / 3;
        addDrawableChild(new StylishButtonWidget(contentLeft, y, btnW, BTN_H, Text.literal("Paste clipboard"), this::pasteClipboard));
        addDrawableChild(new StylishButtonWidget(contentLeft + btnW + GAP, y, btnW, BTN_H, Text.literal("Validate"), this::validateJson));
        addDrawableChild(new StylishButtonWidget(contentLeft + (btnW + GAP) * 2, y, btnW, BTN_H, Text.literal("Import"), this::importJson));

        addDrawableChild(new StylishButtonWidget(contentLeft, height - PAD - BTN_H, 96, BTN_H, Text.literal("Back"), () -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }));
        addDrawableChild(new StylishButtonWidget(contentLeft + contentWidth - 148, height - PAD - BTN_H, 148, BTN_H,
                Text.literal("Import & edit"), this::importAndEdit));

        setInitialFocus(jsonArea);
        validateJson();
    }

    private static boolean looksLikeMacroJson(String text) {
        String t = text.trim();
        return t.startsWith("{") && (t.contains("\"definition\"") || t.contains("\"formatVersion\"") || t.contains("\"dupeclientMacro\""));
    }

    private void pasteClipboard() {
        if (client == null) {
            return;
        }
        String clip = client.keyboard.getClipboard();
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
            String idHint = idField.getText().isBlank() ? def.id : idField.getText().trim();
            if (idField.getText().isBlank() && def.id != null && !def.id.isBlank()) {
                idField.setText(def.id);
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
        if (client == null) {
            return;
        }
        String targetId = idField.getText().isBlank() ? null : idField.getText().trim();
        MacroImportResult result = MacroShare.importJson(client, jsonArea.text(), targetId, false);
        if (!result.success()) {
            setStatus(result.error() == null ? "Import failed" : result.error(), 0xFFFF6B6B);
            return;
        }
        if (openEditor) {
            MacroEditorScreen.open(client, result.savedId());
        } else {
            client.setScreen(parent);
        }
    }

    private void setStatus(String line, int color) {
        statusLine = line;
        statusColor = color;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, width, height);
        context.fill(0, 0, width, 40, 0xCC0A0E14);
        context.fill(0, 39, width, 40, 0x66334155);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, UiTokens.ACCENT);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Paste a DupeClient export bundle or raw macro JSON"),
                width / 2, 26, UiTokens.TEXT_DIM);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, Text.literal(statusLine), contentLeft, height - PAD - BTN_H - 14, statusColor);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
