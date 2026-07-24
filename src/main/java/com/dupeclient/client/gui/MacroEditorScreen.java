package com.dupeclient.client.gui;

import com.dupeclient.client.core.KeyboardConsumingScreen;
import com.dupeclient.client.gui.macro.MacroCyclingStringWidgets;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.render.FastGuiDraw;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import com.dupeclient.client.gui.widget.StylishCyclingButtonWidget;
import com.dupeclient.client.module.macro.MacroAutomation;
import com.dupeclient.client.module.macro.MacroDefinition;
import com.dupeclient.client.module.macro.MacroHoldKeys;
import com.dupeclient.client.module.macro.MacroKeyPress;
import com.dupeclient.client.module.macro.MacroSlotActions;
import com.dupeclient.client.module.macro.MacroStep;
import com.dupeclient.client.module.macro.MacroStepType;
import com.dupeclient.client.module.macro.MacroShare;
import com.dupeclient.client.module.macro.MacroStorage;
import com.dupeclient.client.module.macro.graph.MacroGraphClipboard;
import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;
import com.dupeclient.client.module.macro.graph.MacroGraphEdge;
import com.dupeclient.client.module.macro.graph.MacroGraphGroup;
import com.dupeclient.client.module.macro.graph.MacroGraphNode;
import com.dupeclient.client.module.macro.graph.MacroGraphTypes;
import com.dupeclient.client.module.macro.graph.MacroNodePalette;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import org.jetbrains.annotations.Nullable;

public class MacroEditorScreen
extends Screen implements KeyboardConsumingScreen {
    private static final int LIB_ROW_H = 14;
    private static final int LIB_ROW_GAP = 2;
    private static final int HEADER_H = 40;
    private static final int CANVAS_TOP = 40;
    private static final int NODE_W = 108;
    private static final int NODE_H = 42;
    private static final int PORT = 9;
    private static final int PALETTE_ROW = 17;
    private static final int PALETTE_CATEGORY_HEADER_H = 13;
    private static final int PALETTE_SCROLL_LINES = 4;
    private final Screen parent;
    @Nullable
    private final String loadIdOrNull;
    private MacroDefinition def;
    private final List<MacroGraphNode> graphNodes = new ArrayList<MacroGraphNode>();
    private final List<MacroGraphEdge> graphEdges = new ArrayList<MacroGraphEdge>();
    private final List<MacroGraphGroup> graphGroups = new ArrayList<MacroGraphGroup>();
    private final Map<String, Boolean> paletteCategoryExpanded = new HashMap<String, Boolean>();
    private final Map<String, MacroGraphNode> renderNodeById = new HashMap<String, MacroGraphNode>();
    private int paletteScroll;
    private double canvasZoom = 1.0;
    private boolean editorDirty;
    private boolean autoSaveEnabled;
    private int autoSaveCountdownTicks;
    private int autosaveFlashTicks;
    @Nullable
    private StylishButtonWidget autoSaveButton;
    private StylishTextFieldWidget idField;
    private StylishTextFieldWidget nameField;
    private StylishTextFieldWidget inspTicks;
    private StylishTextFieldWidget inspText;
    @Nullable
    private StylishTextFieldWidget itemPickerFilter;
    private RegistryPickerKind registryPickerKind = RegistryPickerKind.NONE;
    private int registryPickerScroll;
    private int itemPickerListTop;
    private int itemPickerListHeight;
    private int itemPickerPx;
    private int itemPickerPy;
    private int itemPickerPw;
    private int itemPickerPh;
    private StylishCyclingButtonWidget holdKeyCycle;
    private StylishCyclingButtonWidget hotbarSlotCycle;
    private StylishButtonWidget guiItemPickButton;
    private StylishButtonWidget dropFullStackButton;
    private StylishButtonWidget facingButton;
    private StylishButtonWidget walkMeasureButton;
    private StylishButtonWidget guiItemModeButton;
    private StylishButtonWidget guiItemFilterButton;
    private StylishButtonWidget guiItemAmountAllButton;
    private StylishButtonWidget clickSlotActionButton;
    private StylishButtonWidget pressKeyCaptureButton;
    private StylishCyclingButtonWidget blockPresetCycle;
    private StylishTextFieldWidget inspBlockSearchRadius;
    private StylishTextFieldWidget inspGuiItemDelayTicks;
    private StylishButtonWidget blockRegistryPickButton;
    private StylishButtonWidget entityTypePickButton;
    private StylishTextFieldWidget inspEntityType;
    private StylishButtonWidget hotbarSlotByItemButton;
    private double panX;
    private double panY;
    private boolean panning;
    private double panGrabMx;
    private double panGrabMy;
    private double panGrabPx;
    private double panGrabPy;
    private final LinkedHashSet<String> selectedNodeIds = new LinkedHashSet();
    @Nullable
    private String linkFromId;
    private String linkFromSlot = "";
    @Nullable
    private String draggingNodeId;
    @Nullable
    private MacroGraphGroup dragActiveCollapsedGroup;
    @Nullable
    private MacroNodePalette.Entry paletteDragEntry;
    private boolean canvasMarqueeCandidate;
    private boolean marqueeActive;
    private double marqueeAX;
    private double marqueeAY;
    private double marqueeBX;
    private double marqueeBY;
    private final Map<String, double[]> dragNodeOrigins = new HashMap<String, double[]>();
    private double dragWorldAnchorX;
    private double dragWorldAnchorY;
    private boolean contextMenuOpen;
    private int contextMenuX;
    private int contextMenuY;
    private int contextMenuW;
    private int contextMenuH;
    private final List<ContextMenuEntry> contextMenuEntries = new ArrayList<ContextMenuEntry>();
    private static final int CONTEXT_MENU_ROW = 14;
    private final List<String> libraryIds = new ArrayList<String>();
    private int libraryScroll;
    private int libraryListTop;
    private int libraryVisibleRows;
    private int libraryRescanCooldown;
    private boolean applyingInspectorLoad;
    private boolean capturingPressKey;
    private final List<String> liveCompileDiagnostics = new ArrayList<String>();
    private int compileRecheckCooldown = 8;
    private int compilePanelTop = 100;
    private int compilePanelHeight = 40;
    @Nullable
    private StylishButtonWidget macroSaveButton;
    private SavePhase savePhase = SavePhase.IDLE;
    private int savePhaseTicks;
    private String saveOverlayDetail = "";

    public MacroEditorScreen(Screen parent, @Nullable String loadIdOrNull) {
        super(Component.literal("Macro editor"));
        this.parent = parent;
        this.loadIdOrNull = loadIdOrNull;
        this.autoSaveEnabled = MacroStorage.loadMacroEditorPreferences().autosaveEnabled;
    }

    private int sidebarW() {
        return Math.clamp((long)((int)((float)this.width * 0.2f)), 96, 176);
    }

    private int inspectorW() {
        return Math.clamp((long)((int)((float)this.width * 0.25f)), 132, 240);
    }

    public static void open(Minecraft client, @Nullable String loadMacroIdOrNull) {
        if (client == null) {
            return;
        }
        Screen cur = client.screen;
        if (cur instanceof MacroEditorScreen) {
            return;
        }
        client.setScreen((Screen)new MacroEditorScreen(cur, loadMacroIdOrNull));
    }

    protected void init() {
        if (this.def == null) {
            this.def = this.loadInitialDefinition();
            this.graphNodes.clear();
            this.graphEdges.clear();
            if (this.def.nodes != null) {
                this.graphNodes.addAll(this.def.nodes);
            }
            if (this.def.edges != null) {
                this.graphEdges.addAll(this.def.edges);
            }
            this.graphGroups.clear();
            if (this.def.graphGroups != null) {
                this.graphGroups.addAll(this.def.graphGroups);
            }
            if (this.graphNodes.isEmpty() && this.def.steps != null && !this.def.steps.isEmpty()) {
                MacroGraphCompiler.stepsToGraph(this.def, 40.0, 40.0, 72.0);
                this.graphNodes.clear();
                this.graphEdges.clear();
                this.graphNodes.addAll(this.def.nodes);
                this.graphEdges.addAll(this.def.edges);
            }
        } else {
            this.syncInspectorToSelection();
            this.captureHeader();
            this.applyInspectorToSelection();
        }
        this.syncDefFromGraph();
        this.clearWidgets();
        int headerY = 10;
        int topH = 20;
        int topGap = 6;
        int right = this.width - 8;
        int topActionsW = 456;
        this.addRenderableWidget(new StylishButtonWidget(right - 68, headerY, 68, topH, Component.literal("Done"), this::tryClose));
        this.macroSaveButton = new StylishButtonWidget(right - 142, headerY, 68, topH, Component.literal("Save"), this::startSaveMacro);
        this.addRenderableWidget(this.macroSaveButton);
        this.autoSaveButton = new StylishButtonWidget(right - 236, headerY, 88, topH, Component.literal((this.autoSaveEnabled ? "Autosave: on" : "Autosave: off")), () -> {
            boolean bl = this.autoSaveEnabled = !this.autoSaveEnabled;
            if (this.autoSaveButton != null) {
                this.autoSaveButton.setMessage(Component.literal((this.autoSaveEnabled ? "Autosave: on" : "Autosave: off")));
            }
            MacroStorage.saveMacroEditorPreferences(new MacroStorage.MacroEditorPreferences(this.autoSaveEnabled));
            if (this.autoSaveEnabled) {
                this.autoSaveCountdownTicks = 5;
            }
        });
        this.addRenderableWidget(this.autoSaveButton);
        this.addRenderableWidget(new StylishButtonWidget(right - 332, headerY, 88, topH, Component.literal("Prompt ✦"), () -> {
            if (this.minecraft != null) {
                MacroPromptScreen.open(this.minecraft, this);
            }
        }));
        this.addRenderableWidget(new StylishButtonWidget(right - topActionsW, headerY, 56, topH, Component.literal("Import"), () -> {
            if (this.minecraft != null) {
                MacroShareScreen.open(this.minecraft, this, this.def == null ? null : this.def.id);
            }
        }));
        this.addRenderableWidget(new StylishButtonWidget(right - topActionsW + 56 + topGap, headerY, 56, topH, Component.literal("Export"), this::exportCurrentMacro));
        int titleW = this.font.width(this.title.getString());
        int idX = 12 + titleW + 12;
        int idW = Math.min(128, Math.max(88, (right - topActionsW - idX - 120) / 3));
        int nameX = idX + idW + 6;
        int nameW = Math.max(96, right - topActionsW - 8 - nameX);
        this.idField = StylishTextFieldWidget.create(this.font, idX, headerY, idW, topH, Component.literal("id"));
        this.idField.setMaxLength(64);
        this.idField.setHint(Component.literal("macro id"));
        this.idField.setValue(this.def.id);
        this.addRenderableWidget(this.idField);
        this.nameField = StylishTextFieldWidget.create(this.font, nameX, headerY, nameW, topH, Component.literal("name"));
        this.nameField.setMaxLength(128);
        this.nameField.setHint(Component.literal("display name"));
        this.nameField.setValue(this.def.displayName == null ? "" : this.def.displayName);
        this.addRenderableWidget(this.nameField);
        int ix = this.width - this.inspectorW() + 8;
        this.facingButton = new StylishButtonWidget(ix, 92, this.inspectorW() - 16, 12, Component.literal("Facing: S"), this::cycleFacing);
        this.facingButton.visible = false;
        this.addRenderableWidget(this.facingButton);
        this.walkMeasureButton = new StylishButtonWidget(ix, 108, this.inspectorW() - 16, 12, Component.literal("Walk: ticks"), this::cycleWalkMeasure);
        this.walkMeasureButton.visible = false;
        this.addRenderableWidget(this.walkMeasureButton);
        this.guiItemModeButton = new StylishButtonWidget(ix, 124, this.inspectorW() - 16, 12, Component.literal("Into/Out: put"), this::cycleGuiItemMode);
        this.guiItemModeButton.visible = false;
        this.addRenderableWidget(this.guiItemModeButton);
        this.guiItemFilterButton = new StylishButtonWidget(ix, 138, this.inspectorW() - 16, 12, Component.literal("Filter: specific"), this::cycleGuiItemFilter);
        this.guiItemFilterButton.visible = false;
        this.addRenderableWidget(this.guiItemFilterButton);
        this.guiItemAmountAllButton = new StylishButtonWidget(ix, 152, this.inspectorW() - 16, 12, Component.literal("Amount: count"), this::cycleGuiItemAmountMode);
        this.guiItemAmountAllButton.visible = false;
        this.addRenderableWidget(this.guiItemAmountAllButton);
        this.clickSlotActionButton = new StylishButtonWidget(ix, 124, this.inspectorW() - 16, 12, Component.literal("Action: QUICK_MOVE"), this::cycleClickSlotAction);
        this.clickSlotActionButton.visible = false;
        this.addRenderableWidget(this.clickSlotActionButton);
        this.pressKeyCaptureButton = new StylishButtonWidget(ix, 124, this.inspectorW() - 16, 12, Component.literal("Key: SPACE"), this::startPressKeyCapture);
        this.pressKeyCaptureButton.visible = false;
        this.addRenderableWidget(this.pressKeyCaptureButton);
        this.blockPresetCycle = MacroCyclingStringWidgets.blockPreset(ix, 124, this.inspectorW() - 16, 16, Component.literal("Target block"), (button, value) -> {
            MacroGraphNode n;
            if (this.applyingInspectorLoad) {
                return;
            }
            MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n == null) {
                return;
            }
            String t = MacroEditorScreen.trimType(n.type);
            if (!"BLOCK_INTERACT".equals(t) && !"USE_BLOCK".equals(t)) {
                return;
            }
            n.blockPreset = MacroAutomation.normalizeBlockPreset(value);
            this.syncDefFromGraph();
            this.refreshCompileDiagnostics();
            this.loadInspectorFromSelection();
        });
        this.blockPresetCycle.visible = false;
        this.addRenderableWidget(this.blockPresetCycle);
        this.inspBlockSearchRadius = StylishTextFieldWidget.create(this.font, ix, 192, this.inspectorW() - 16, 18, Component.literal("search"));
        this.inspBlockSearchRadius.setMaxLength(2);
        this.inspBlockSearchRadius.setResponder(s -> {
            if (!this.applyingInspectorLoad) {
                this.applyInspectorToSelection();
            }
        });
        this.inspBlockSearchRadius.visible = false;
        this.addRenderableWidget(this.inspBlockSearchRadius);
        this.inspTicks = StylishTextFieldWidget.create(this.font, ix, 192, this.inspectorW() - 16, 18, Component.literal("value"));
        this.inspTicks.setMaxLength(8);
        this.inspTicks.setEditable(true);
        this.inspTicks.setResponder(s -> {
            if (!this.applyingInspectorLoad) {
                this.applyInspectorToSelection();
            }
        });
        this.addRenderableWidget(this.inspTicks);
        this.inspGuiItemDelayTicks = StylishTextFieldWidget.create(this.font, ix, 192, this.inspectorW() - 16, 18, Component.literal("gui delay"));
        this.inspGuiItemDelayTicks.setMaxLength(3);
        this.inspGuiItemDelayTicks.setHint(Component.literal("delay ticks (0=burst)"));
        this.inspGuiItemDelayTicks.setResponder(s -> {
            if (!this.applyingInspectorLoad) {
                this.applyInspectorToSelection();
            }
        });
        this.inspGuiItemDelayTicks.visible = false;
        this.addRenderableWidget(this.inspGuiItemDelayTicks);
        this.inspText = StylishTextFieldWidget.create(this.font, ix, 214, this.inspectorW() - 16, 52, Component.literal("text"));
        this.inspText.setMaxLength(512);
        this.inspText.setResponder(s -> {
            if (!this.applyingInspectorLoad) {
                this.applyInspectorToSelection();
            }
        });
        this.addRenderableWidget(this.inspText);
        this.itemPickerFilter = StylishTextFieldWidget.create(this.font, 0, 0, 120, 16, Component.literal("filter"));
        this.itemPickerFilter.setMaxLength(128);
        this.itemPickerFilter.setVisible(false);
        this.itemPickerFilter.setHint(Component.literal("type to filter\u2026"));
        this.itemPickerFilter.setResponder(s -> {
            this.registryPickerScroll = 0;
        });
        this.addRenderableWidget(this.itemPickerFilter);
        List<String> hk = Arrays.asList(MacroHoldKeys.IDS);
        this.holdKeyCycle = MacroCyclingStringWidgets.holdKeyCycle(ix, 200, this.inspectorW() - 16, 16, hk, Component.literal("Hold key"), (button, value) -> {
            MacroGraphNode n;
            if (this.applyingInspectorLoad) {
                return;
            }
            MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n != null && ("KEY_HOLD".equals(MacroEditorScreen.trimType(n.type)) || "ATTACK".equals(MacroEditorScreen.trimType(n.type)))) {
                n.holdKeyId = MacroHoldKeys.normalize(value);
                this.syncDefFromGraph();
                this.refreshCompileDiagnostics();
            }
        });
        this.holdKeyCycle.visible = false;
        this.addRenderableWidget(this.holdKeyCycle);
        ArrayList<String> slots = new ArrayList<String>();
        for (int si = 0; si <= 8; ++si) {
            slots.add(String.valueOf(si));
        }
        this.hotbarSlotCycle = MacroCyclingStringWidgets.hotbarSlotCycle(ix, 220, this.inspectorW() - 16, 16, slots, Component.literal("Hotbar slot"), (button, value) -> {
            MacroGraphNode n;
            if (this.applyingInspectorLoad) {
                return;
            }
            MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n == null) {
                return;
            }
            try {
                n.hotbarSlot = Mth.clamp((int)Integer.parseInt(value.trim()), (int)0, (int)8);
            }
            catch (NumberFormatException e) {
                n.hotbarSlot = 0;
            }
            this.syncDefFromGraph();
            this.refreshCompileDiagnostics();
        });
        this.hotbarSlotCycle.visible = false;
        this.addRenderableWidget(this.hotbarSlotCycle);
        this.hotbarSlotByItemButton = new StylishButtonWidget(ix, 200, this.inspectorW() - 16, 14, Component.literal("Pick slot by item\u2026"), this::openHotbarSlotByItemPicker);
        this.hotbarSlotByItemButton.visible = false;
        this.addRenderableWidget(this.hotbarSlotByItemButton);
        this.blockRegistryPickButton = new StylishButtonWidget(ix, 200, this.inspectorW() - 16, 14, Component.literal("Browse blocks\u2026"), this::openBlockRegistryPicker);
        this.blockRegistryPickButton.visible = false;
        this.addRenderableWidget(this.blockRegistryPickButton);
        this.entityTypePickButton = new StylishButtonWidget(ix, 200, this.inspectorW() - 16, 14, Component.literal("Browse entity types\u2026"), this::openEntityTypePicker);
        this.entityTypePickButton.visible = false;
        this.addRenderableWidget(this.entityTypePickButton);
        this.inspEntityType = StylishTextFieldWidget.create(this.font, ix, 200, this.inspectorW() - 16, 18, Component.literal("entity type"));
        this.inspEntityType.setMaxLength(256);
        this.inspEntityType.setHint(Component.literal("minecraft:zombie"));
        this.inspEntityType.setResponder(s -> {
            if (!this.applyingInspectorLoad) {
                this.applyInspectorToSelection();
            }
        });
        this.inspEntityType.visible = false;
        this.addRenderableWidget(this.inspEntityType);
        this.guiItemPickButton = new StylishButtonWidget(ix, 200, this.inspectorW() - 16, 14, Component.literal("Browse items\u2026"), this::openItemPicker);
        this.guiItemPickButton.visible = false;
        this.addRenderableWidget(this.guiItemPickButton);
        this.dropFullStackButton = new StylishButtonWidget(ix, 200, this.inspectorW() - 16, 14, Component.literal("Drop: one"), () -> {
            MacroGraphNode n;
            MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n != null && "DROP_ITEM".equals(MacroEditorScreen.trimType(n.type))) {
                n.dropFullStack = !n.dropFullStack;
                this.dropFullStackButton.setMessage(Component.literal((n.dropFullStack ? "Drop: stack" : "Drop: one")));
                this.syncDefFromGraph();
            }
        });
        this.dropFullStackButton.visible = false;
        this.addRenderableWidget(this.dropFullStackButton);
        this.layoutInspectorWidgets();
        this.refreshLibraryIds();
        this.loadInspectorFromSelection();
        this.refreshCompileDiagnostics();
        this.layoutInspectorWidgets();
        this.updateSaveButtonLabel();
        this.compileRecheckCooldown = 10;
        this.clearDirty();
        this.clampPaletteScroll();
    }

    private void refreshLibraryIds() {
        MacroStorage.prepare();
        this.libraryIds.clear();
        this.libraryIds.addAll(MacroStorage.listMacroIds());
        int maxScroll = Math.max(0, this.libraryIds.size() - this.libraryVisibleRows);
        this.libraryScroll = Math.max(0, Math.min(this.libraryScroll, maxScroll));
    }

    public void tick() {
        super.tick();
        if (this.autosaveFlashTicks > 0) {
            --this.autosaveFlashTicks;
        }
        if (this.autoSaveEnabled && this.editorDirty && this.savePhase == SavePhase.IDLE && --this.autoSaveCountdownTicks <= 0) {
            this.autoSaveCountdownTicks = 40;
            this.trySilentAutosave();
        }
        if (--this.libraryRescanCooldown <= 0) {
            this.libraryRescanCooldown = 15;
            this.refreshLibraryIds();
        }
        this.tickSaveUi();
        if (this.savePhase == SavePhase.IDLE && --this.compileRecheckCooldown <= 0) {
            this.compileRecheckCooldown = 10;
            this.refreshCompileDiagnostics();
            this.layoutInspectorWidgets();
        }
    }

    private MacroDefinition loadInitialDefinition() {
        if (this.loadIdOrNull != null && !this.loadIdOrNull.isBlank()) {
            try {
                return MacroStorage.load(this.loadIdOrNull);
            }
            catch (Exception e) {
                return MacroEditorScreen.blankDef(MacroEditorScreen.sanitizeIdFallback(this.loadIdOrNull));
            }
        }
        return MacroEditorScreen.blankDef("new_macro");
    }

    private void applyLoadedDefinition(MacroDefinition d) {
        d.normalize();
        this.def = d;
        this.graphNodes.clear();
        this.graphEdges.clear();
        if (this.def.nodes != null) {
            this.graphNodes.addAll(this.def.nodes);
        }
        if (this.def.edges != null) {
            this.graphEdges.addAll(this.def.edges);
        }
        this.graphGroups.clear();
        if (this.def.graphGroups != null) {
            this.graphGroups.addAll(this.def.graphGroups);
        }
        if (this.graphNodes.isEmpty() && this.def.steps != null && !this.def.steps.isEmpty()) {
            MacroGraphCompiler.stepsToGraph(this.def, 40.0, 40.0, 72.0);
            this.graphNodes.clear();
            this.graphEdges.clear();
            this.graphNodes.addAll(this.def.nodes);
            this.graphEdges.addAll(this.def.edges);
        }
        this.syncDefFromGraph();
    }

    private void loadMacroIntoEditor(String id) {
        try {
            this.applyLoadedDefinition(MacroStorage.load(id));
            this.panX = 0.0;
            this.panY = 0.0;
            this.selectedNodeIds.clear();
            this.linkFromId = null;
            this.draggingNodeId = null;
            this.dragActiveCollapsedGroup = null;
            this.paletteDragEntry = null;
            this.init();
        }
        catch (Exception e) {
            MacroEditorScreen.toast("Could not load: " + e.getMessage());
        }
    }

    private boolean inLibraryPanel(double mx, double my) {
        int ix = this.width - this.inspectorW() + 4;
        int listBottom = this.libraryListTop + 22 + this.libraryVisibleRows * (LIB_ROW_H + LIB_ROW_GAP) + 2;
        return mx >= (double)ix && mx < (double)(this.width - 4) && my >= (double)this.libraryListTop && my < (double)listBottom;
    }

    private int libraryRowIndexAt(double mx, double my) {
        if (!this.inLibraryPanel(mx, my) || this.libraryIds.isEmpty()) {
            return -1;
        }
        int rowY = this.libraryListTop + 22;
        if (my < (double)rowY) {
            return -1;
        }
        int row = (int)((my - (double)rowY) / (double)(LIB_ROW_H + LIB_ROW_GAP));
        if (row < 0 || row >= this.libraryVisibleRows) {
            return -1;
        }
        int idx = this.libraryScroll + row;
        if (idx < 0 || idx >= this.libraryIds.size()) {
            return -1;
        }
        return idx;
    }

    private boolean tryClickLibrary(double mx, double my, boolean doubleClick) {
        int idx = this.libraryRowIndexAt(mx, my);
        if (idx < 0) {
            return false;
        }
        String id = this.libraryIds.get(idx);
        if (doubleClick) {
            this.loadMacroIntoEditor(id);
            MacroEditorScreen.toast("Loaded \"" + id + "\".");
            return true;
        }
        if (MacroStorage.filenameId(this.def.id).equalsIgnoreCase(id)) {
            return true;
        }
        this.loadMacroIntoEditor(id);
        return true;
    }

    private void confirmDeleteMacroFromLibrary(String macroFileId) {
        Minecraft c = this.minecraft;
        if (c == null) {
            return;
        }
        MacroEditorScreen self = this;
        String targetId = macroFileId;
        c.setScreen((Screen)new ConfirmScreen(yes -> {
            c.setScreen((Screen)self);
            if (!yes) {
                return;
            }
            try {
                MacroStorage.deleteMacro(targetId);
                self.refreshLibraryIds();
                if (MacroStorage.filenameId(self.def.id).equalsIgnoreCase(MacroStorage.filenameId(targetId))) {
                    if (!self.libraryIds.isEmpty()) {
                        self.loadMacroIntoEditor(self.libraryIds.getFirst());
                    } else {
                        self.applyLoadedDefinition(MacroEditorScreen.blankDef("new_macro"));
                        self.init();
                    }
                    self.clearDirty();
                }
                MacroEditorScreen.toast("Deleted macro \"" + MacroStorage.filenameId(targetId) + "\".");
            }
            catch (Exception e) {
                MacroEditorScreen.toast(e.getMessage() == null ? "Delete failed" : e.getMessage());
            }
        }, Component.literal("Delete macro?"), Component.literal(("Removes " + MacroStorage.filenameId(targetId) + ".json from disk. This cannot be undone."))));
    }

    private static MacroDefinition blankDef(String id) {
        MacroDefinition d = new MacroDefinition();
        d.formatVersion = 2;
        d.displayName = d.id = MacroEditorScreen.sanitizeIdFallback(id);
        d.steps = new ArrayList<MacroStep>();
        d.nodes = new ArrayList<MacroGraphNode>();
        d.edges = new ArrayList<MacroGraphEdge>();
        MacroGraphNode start = new MacroGraphNode();
        start.id = "__tmpl_start";
        start.type = "GRAPH_START";
        start.category = "Control";
        start.x = 48.0;
        start.y = 36.0;
        MacroGraphNode end = new MacroGraphNode();
        end.id = "__tmpl_end";
        end.type = "GRAPH_END";
        end.category = "Control";
        end.x = 48.0;
        end.y = 220.0;
        d.nodes.add(start);
        d.nodes.add(end);
        MacroGraphEdge bridge = new MacroGraphEdge();
        bridge.from = start.id;
        bridge.to = end.id;
        bridge.fromSlot = "";
        d.edges.add(bridge);
        return d;
    }

    private static String sanitizeIdFallback(String id) {
        if (id == null || id.isBlank()) {
            return "new_macro";
        }
        return id.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
    }

    private void syncDefFromGraph() {
        this.def.nodes = new ArrayList<MacroGraphNode>(this.graphNodes);
        this.def.edges = new ArrayList<MacroGraphEdge>(this.graphEdges);
        this.def.graphGroups = new ArrayList<MacroGraphGroup>(this.graphGroups);
        if (!this.graphNodes.isEmpty()) {
            this.def.formatVersion = 2;
        }
        this.markDirty();
        this.autoSaveCountdownTicks = 15;
    }

    private void markDirty() {
        this.editorDirty = true;
    }

    private void clearDirty() {
        this.editorDirty = false;
    }

    private void trySilentAutosave() {
        if (!this.autoSaveEnabled || !this.editorDirty || this.savePhase != SavePhase.IDLE || this.def == null) {
            return;
        }
        try {
            this.captureHeader();
            this.applyInspectorToSelection();
            this.syncDefFromGraphWithoutDirty();
            this.def.normalize();
            this.def.steps.clear();
            MacroStorage.save(this.def);
            this.clearDirty();
            this.autosaveFlashTicks = 28;
            this.refreshCompileDiagnostics();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void syncDefFromGraphWithoutDirty() {
        this.def.nodes = new ArrayList<MacroGraphNode>(this.graphNodes);
        this.def.edges = new ArrayList<MacroGraphEdge>(this.graphEdges);
        this.def.graphGroups = new ArrayList<MacroGraphGroup>(this.graphGroups);
        if (!this.graphNodes.isEmpty()) {
            this.def.formatVersion = 2;
        }
    }

    private void captureHeader() {
        if (this.idField != null) {
            this.def.id = this.idField.getValue().trim();
        }
        if (this.nameField != null) {
            this.def.displayName = this.nameField.getValue().trim();
        }
    }

    private int canvasLeft() {
        return this.sidebarW();
    }

    private int canvasRight() {
        return this.width - this.inspectorW();
    }

    private int canvasWidth() {
        return Math.max(40, this.canvasRight() - this.canvasLeft());
    }

    private int canvasHeight() {
        return Math.max(40, this.height - CANVAS_TOP - 8);
    }

    private double toWorldX(double screenMx) {
        return (screenMx - (double)this.canvasLeft()) / this.canvasZoom - this.panX;
    }

    private double toWorldY(double screenMy) {
        return (screenMy - (double)CANVAS_TOP) / this.canvasZoom - this.panY;
    }

    private int toScreenX(double wx) {
        return (int)((double)this.canvasLeft() + (this.panX + wx) * this.canvasZoom);
    }

    private int toScreenY(double wy) {
        return (int)((double)CANVAS_TOP + (this.panY + wy) * this.canvasZoom);
    }

    private void fillWorldRect(GuiGraphicsExtractor context, double wx0, double wy0, double wx1, double wy1, int color) {
        int sx0 = this.toScreenX(wx0);
        int sy0 = this.toScreenY(wy0);
        int sx1 = this.toScreenX(wx1);
        int sy1 = this.toScreenY(wy1);
        int x0 = Math.min(sx0, sx1);
        int x1 = Math.max(sx0, sx1);
        int y0 = Math.min(sy0, sy1);
        int y1 = Math.max(sy0, sy1);
        if (x1 > x0 && y1 > y0) {
            context.fill(x0, y0, x1, y1, color);
        }
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= (double)this.canvasLeft() && mx < (double)this.canvasRight() && my >= (double)CANVAS_TOP && my < (double)(this.height - 8);
    }

    private boolean isNodeInCollapsedGroup(MacroGraphNode n) {
        if (n == null) {
            return false;
        }
        for (MacroGraphGroup g : this.graphGroups) {
            if (!g.collapsed || g.memberNodeIds == null || !g.memberNodeIds.contains(n.id)) continue;
            return true;
        }
        return false;
    }

    private boolean isNodeIdInCollapsedGroup(String nodeId) {
        return this.isNodeInCollapsedGroup(this.findNode(nodeId));
    }

    @Nullable
    private MacroGraphGroup collapsedGroupContaining(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        for (MacroGraphGroup g : this.graphGroups) {
            if (g == null || !g.collapsed || g.memberNodeIds == null || !g.memberNodeIds.contains(nodeId)) continue;
            return g;
        }
        return null;
    }

    private boolean collapsedChipWorldBounds(MacroGraphGroup g, double[] out4) {
        if (g == null || !g.collapsed || g.memberNodeIds == null || g.memberNodeIds.isEmpty()) {
            return false;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        for (String mid : g.memberNodeIds) {
            MacroGraphNode n = this.findNode(mid);
            if (n == null) continue;
            minX = Math.min(minX, n.x);
            minY = Math.min(minY, n.y);
        }
        if (minX == Double.POSITIVE_INFINITY) {
            return false;
        }
        double chipTop = minY - 14.0;
        double chipBottom = chipTop + 12.0 / Math.max(0.001, this.canvasZoom);
        out4[0] = minX - 4.0;
        out4[1] = chipTop;
        out4[2] = minX + 108.0 + 4.0;
        out4[3] = chipBottom;
        return true;
    }

    private double memberStackWorldYSpan(MacroGraphGroup g, double[] outMinY, double[] outMaxY) {
        double minNy = Double.POSITIVE_INFINITY;
        double maxNy = Double.NEGATIVE_INFINITY;
        if (g.memberNodeIds != null) {
            for (String mid : g.memberNodeIds) {
                MacroGraphNode n = this.findNode(mid);
                if (n == null) continue;
                minNy = Math.min(minNy, n.y);
                maxNy = Math.max(maxNy, n.y + 42.0);
            }
        }
        if (minNy == Double.POSITIVE_INFINITY) {
            outMinY[0] = 0.0;
            outMaxY[0] = 42.0;
            return 42.0;
        }
        outMinY[0] = minNy;
        outMaxY[0] = maxNy;
        return Math.max(1.0E-6, maxNy - minNy);
    }

    private double collapsedChipMappedWorldY(MacroGraphGroup g, double worldYAlongMembers) {
        double[] chip = new double[4];
        if (!this.collapsedChipWorldBounds(g, chip)) {
            return worldYAlongMembers;
        }
        double[] minY = new double[1];
        double[] maxY = new double[1];
        double span = this.memberStackWorldYSpan(g, minY, maxY);
        double t = (worldYAlongMembers - minY[0]) / span;
        t = Mth.clamp((double)t, (double)0.06, (double)0.94);
        return chip[1] + t * (chip[3] - chip[1]);
    }

    private double outPortWorldY(MacroGraphNode n, String fromSlot) {
        String s;
        String string = s = fromSlot == null ? "" : fromSlot.trim().toLowerCase();
        if (MacroGraphTypes.isRepeatNode(n.type)) {
            if (this.repeatShowsMergePort(n)) {
                if ("loop".equals(s)) {
                    return n.y + 9.24;
                }
                if ("next".equals(s)) {
                    return n.y + 32.76;
                }
            }
            return n.y + 21.0;
        }
        return n.y + 21.0;
    }

    private int edgeFromScreenX(MacroGraphNode a, @Nullable MacroGraphGroup collapsedFrom) {
        if (collapsedFrom == null) {
            return this.toScreenX(a.x + 108.0);
        }
        double[] chip = new double[4];
        return this.collapsedChipWorldBounds(collapsedFrom, chip) ? this.toScreenX(chip[2]) : this.toScreenX(a.x + 108.0);
    }

    private int edgeFromScreenY(MacroGraphNode a, @Nullable MacroGraphGroup collapsedFrom, String fromSlot) {
        if (collapsedFrom == null) {
            return this.outPortScreenY(a, fromSlot);
        }
        return this.toScreenY(this.collapsedChipMappedWorldY(collapsedFrom, this.outPortWorldY(a, fromSlot)));
    }

    private int edgeToScreenX(MacroGraphNode b, @Nullable MacroGraphGroup collapsedTo) {
        if (collapsedTo == null) {
            return this.toScreenX(b.x);
        }
        double[] chip = new double[4];
        return this.collapsedChipWorldBounds(collapsedTo, chip) ? this.toScreenX(chip[0]) : this.toScreenX(b.x);
    }

    private int edgeToScreenY(MacroGraphNode b, @Nullable MacroGraphGroup collapsedTo) {
        if (collapsedTo == null) {
            return this.toScreenY(b.y + 21.0);
        }
        return this.toScreenY(this.collapsedChipMappedWorldY(collapsedTo, b.y + 21.0));
    }

    @Nullable
    private int[] expandedGroupScreenRect(MacroGraphGroup g) {
        if (g == null || g.collapsed || g.memberNodeIds == null || g.memberNodeIds.isEmpty()) {
            return null;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (String mid : g.memberNodeIds) {
            MacroGraphNode n = this.findNode(mid);
            if (n == null) continue;
            minX = Math.min(minX, n.x);
            minY = Math.min(minY, n.y);
            maxX = Math.max(maxX, n.x + 108.0);
            maxY = Math.max(maxY, n.y + 42.0);
        }
        if (minX == Double.POSITIVE_INFINITY) {
            return null;
        }
        int pad = 6;
        int sx0 = this.toScreenX(minX - (double)pad);
        int sy0 = this.toScreenY(minY - (double)pad - 10.0);
        int sx1 = this.toScreenX(maxX + (double)pad);
        int sy1 = this.toScreenY(maxY + (double)pad);
        int x0 = Math.min(sx0, sx1);
        int x1 = Math.max(sx0, sx1);
        int y0 = Math.min(sy0, sy1);
        int y1 = Math.max(sy0, sy1);
        return new int[]{x0, y0, x1, y1};
    }

    @Nullable
    private MacroGraphGroup expandedGroupTitleHitScreen(int mx, int my) {
        for (int gi = this.graphGroups.size() - 1; gi >= 0; --gi) {
            MacroGraphGroup g = this.graphGroups.get(gi);
            int[] r = this.expandedGroupScreenRect(g);
            if (r == null) continue;
            int titleY1 = Math.min(r[1] + 12, r[3]);
            if (mx < r[0] || mx >= r[2] || my < r[1] || my >= titleY1) continue;
            return g;
        }
        return null;
    }

    @Nullable
    private MacroGraphGroup collapsedGroupHitScreen(int mx, int my) {
        for (int gi = this.graphGroups.size() - 1; gi >= 0; --gi) {
            MacroGraphGroup g = this.graphGroups.get(gi);
            if (g == null || !g.collapsed || g.memberNodeIds == null || g.memberNodeIds.isEmpty()) continue;
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            for (String mid : g.memberNodeIds) {
                MacroGraphNode n = this.findNode(mid);
                if (n == null) continue;
                minX = Math.min(minX, n.x);
                minY = Math.min(minY, n.y);
            }
            if (minX == Double.POSITIVE_INFINITY) continue;
            double chipTop = minY - 14.0;
            double chipBottom = chipTop + 12.0 / Math.max(0.001, this.canvasZoom);
            int sx0 = this.toScreenX(minX - 4.0);
            int sx1 = this.toScreenX(minX + 108.0 + 4.0);
            int sy0 = this.toScreenY(chipTop);
            int sy1 = this.toScreenY(chipBottom);
            int hx0 = Math.min(sx0, sx1);
            int hx1 = Math.max(sx0, sx1);
            int hy0 = Math.min(sy0, sy1);
            int hy1 = Math.max(sy0, sy1);
            if (mx < hx0 || mx >= hx1 || my < hy0 || my >= hy1) continue;
            return g;
        }
        return null;
    }

    private void openGroupChromeContextMenu(int mx, int my, MacroGraphGroup g) {
        MacroGraphGroup gg = g;
        ArrayList<ContextMenuEntry> items = new ArrayList<ContextMenuEntry>();
        items.add(new ContextMenuEntry("Collapse group", () -> {
            gg.collapsed = true;
            this.pruneSelectionForHiddenNodes();
            this.syncDefFromGraph();
            this.refreshCompileDiagnostics();
            this.loadInspectorFromSelection();
        }));
        items.add(new ContextMenuEntry("Rename & colors\u2026", () -> this.openGroupStyleEditor(gg)));
        items.add(new ContextMenuEntry("Copy group", () -> this.copyGroupToClipboard(gg)));
        items.add(new ContextMenuEntry("Delete group\u2026", () -> this.confirmDeleteGroup(gg)));
        this.openContextMenu(mx, my, items);
    }

    private void copyGroupToClipboard(MacroGraphGroup g) {
        if (g == null || g.memberNodeIds == null || g.memberNodeIds.isEmpty()) {
            MacroEditorScreen.toast("Group is empty.");
            return;
        }
        LinkedHashSet<String> prev = new LinkedHashSet<String>(this.selectedNodeIds);
        this.selectedNodeIds.clear();
        for (String id : g.memberNodeIds) {
            if (id == null || this.findNode(id) == null) continue;
            this.selectedNodeIds.add(id);
        }
        if (this.selectedNodeIds.isEmpty()) {
            this.selectedNodeIds.addAll(prev);
            MacroEditorScreen.toast("Group has no nodes on the canvas.");
            return;
        }
        this.copySelectionToClipboard();
        this.selectedNodeIds.clear();
        this.selectedNodeIds.addAll(prev);
    }

    private void openGroupStyleEditor(MacroGraphGroup g) {
        if (this.minecraft != null && g != null) {
            this.minecraft.setScreen((Screen)new MacroGroupStyleEditScreen(g));
        }
    }

    void applyGroupStyleFromDialog(MacroGraphGroup g, String label, int borderArgb, int fillArgb) {
        if (g == null) {
            return;
        }
        String lab = label == null ? "" : label.trim();
        g.label = lab.isEmpty() ? "Group" : lab;
        g.borderArgb = borderArgb;
        g.fillArgb = fillArgb;
        this.syncDefFromGraph();
        this.refreshCompileDiagnostics();
    }

    private static int parseArgbHex(String raw) throws NumberFormatException {
        String s;
        String string = s = raw == null ? "" : raw.trim();
        if (s.length() < 3) {
            throw new NumberFormatException("too short");
        }
        return (int)(s.startsWith("0x") || s.startsWith("0X") ? Long.parseLong(s.substring(2), 16) : Long.parseLong(s, 16));
    }

    private void pruneSelectionForHiddenNodes() {
        this.selectedNodeIds.removeIf(this::isNodeIdInCollapsedGroup);
    }

    private void closeContextMenu() {
        this.contextMenuOpen = false;
        this.contextMenuEntries.clear();
    }

    private void openContextMenu(int anchorX, int anchorY, List<ContextMenuEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        this.closeRegistryPicker();
        this.contextMenuEntries.clear();
        this.contextMenuEntries.addAll(entries);
        int padX = 10;
        int w = 80;
        for (ContextMenuEntry e : this.contextMenuEntries) {
            w = Math.max(w, this.font.width(e.label()) + padX * 2);
        }
        int h = 4 + this.contextMenuEntries.size() * 14;
        int x = anchorX;
        int y = anchorY;
        x = Math.max(4, Math.min(x, this.width - w - 4));
        y = Math.max(74, Math.min(y, this.height - h - 4));
        this.contextMenuX = x;
        this.contextMenuY = y;
        this.contextMenuW = w;
        this.contextMenuH = h;
        this.contextMenuOpen = true;
    }

    private void renderContextMenu(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!this.contextMenuOpen || this.contextMenuEntries.isEmpty()) {
            return;
        }
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, -266853832);
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + 1, -11899184);
        context.fill(this.contextMenuX, this.contextMenuY + this.contextMenuH - 1, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, -11899184);
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + 1, this.contextMenuY + this.contextMenuH, -11899184);
        context.fill(this.contextMenuX + this.contextMenuW - 1, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, -11899184);
        int rowY = this.contextMenuY + 2;
        for (int i = 0; i < this.contextMenuEntries.size(); ++i) {
            boolean hot;
            ContextMenuEntry e = this.contextMenuEntries.get(i);
            boolean bl = hot = mouseX >= this.contextMenuX && mouseX < this.contextMenuX + this.contextMenuW && mouseY >= rowY && mouseY < rowY + 14;
            if (hot) {
                context.fill(this.contextMenuX + 1, rowY, this.contextMenuX + this.contextMenuW - 1, rowY + 14, -1440071576);
            }
            context.text(this.font, Component.literal(e.label()), this.contextMenuX + 8, rowY + 3, -1380097);
            rowY += 14;
        }
    }

    private boolean contextMenuContains(double mx, double my) {
        return this.contextMenuOpen && mx >= (double)this.contextMenuX && mx < (double)(this.contextMenuX + this.contextMenuW) && my >= (double)this.contextMenuY && my < (double)(this.contextMenuY + this.contextMenuH);
    }

    private boolean graphKeyboardShortcutsAllowed() {
        return !this.isRegistryPickerOpen() && !(this.getFocused() instanceof StylishTextFieldWidget);
    }

    private static boolean isMultiSelectModifierClick(MouseButtonEvent click) {
        int m = click.buttonInfo().modifiers();
        return (m & 0xB) != 0;
    }

    private void confirmDeleteGroup(MacroGraphGroup target) {
        Minecraft c = this.minecraft;
        if (c == null || target == null) {
            return;
        }
        MacroEditorScreen self = this;
        c.setScreen((Screen)new ConfirmScreen(yes -> {
            c.setScreen((Screen)self);
            if (yes) {
                this.graphGroups.removeIf(g -> g != null && target.id.equals(g.id));
                self.syncDefFromGraph();
                self.loadInspectorFromSelection();
            }
        }, Component.literal("Delete group?"), Component.literal("Removes the colored frame only; nodes stay on the canvas.")));
    }

    private void newGroupFromSelection() {
        if (this.selectedNodeIds.isEmpty()) {
            MacroEditorScreen.toast("Select nodes first.");
            return;
        }
        MacroGraphGroup g = new MacroGraphGroup();
        g.id = "g" + UUID.randomUUID().toString().substring(0, 8);
        g.label = "Group";
        g.borderArgb = -11899184;
        g.fillArgb = 1075855456;
        g.collapsed = false;
        g.memberNodeIds = new ArrayList<String>(this.selectedNodeIds);
        this.graphGroups.add(g);
        this.syncDefFromGraph();
        this.refreshCompileDiagnostics();
        this.loadInspectorFromSelection();
        this.openGroupStyleEditor(g);
    }

    private void deleteGraphNodeById(String nid) {
        MacroGraphNode sel = this.findNode(nid);
        if (sel == null) {
            return;
        }
        if (MacroGraphTypes.isControlNode(sel.type)) {
            MacroEditorScreen.toast("Can't delete control nodes.");
            return;
        }
        this.graphEdges.removeIf(e -> e.from.equals(nid) || e.to.equals(nid));
        this.graphNodes.removeIf(n -> n.id.equals(nid));
        this.removeNodeFromAllGroups(nid);
        this.selectedNodeIds.remove(nid);
        if (Objects.equals(this.linkFromId, nid)) {
            this.linkFromId = null;
            this.linkFromSlot = "";
        }
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void copyHitOrSelection(MacroGraphNode hit) {
        if (hit == null) {
            return;
        }
        if (this.selectedNodeIds.contains(hit.id)) {
            this.copySelectionToClipboard();
            return;
        }
        LinkedHashSet<String> prev = new LinkedHashSet<String>(this.selectedNodeIds);
        this.selectedNodeIds.clear();
        this.selectedNodeIds.add(hit.id);
        this.copySelectionToClipboard();
        this.selectedNodeIds.clear();
        this.selectedNodeIds.addAll(prev);
    }

    @Nullable
    private MacroGraphGroup nonCollapsedGroupContaining(String nodeId) {
        for (MacroGraphGroup g : this.graphGroups) {
            if (g == null || g.collapsed || g.memberNodeIds == null || !g.memberNodeIds.contains(nodeId)) continue;
            return g;
        }
        return null;
    }

    @Nullable
    private MacroGraphNode nodeAtWorld(double wx, double wy) {
        for (int i = this.graphNodes.size() - 1; i >= 0; --i) {
            MacroGraphNode n = this.graphNodes.get(i);
            if (this.isNodeInCollapsedGroup(n) || !(wx >= n.x) || !(wx <= n.x + 108.0) || !(wy >= n.y) || !(wy <= n.y + 42.0)) continue;
            return n;
        }
        return null;
    }

    private static boolean portBoxHit(double wx, double wy, double px, double py) {
        return wx >= px && wx <= px + 9.0 && wy >= py && wy <= py + 9.0;
    }

    @Nullable
    private OutPortHit pickOutPort(double wx, double wy) {
        for (int i = this.graphNodes.size() - 1; i >= 0; --i) {
            double py;
            MacroGraphNode n = this.graphNodes.get(i);
            if (this.isNodeInCollapsedGroup(n) || "GRAPH_END".equals(n.type)) continue;
            double px = n.x + 108.0 - 4.5;
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                if (this.repeatShowsMergePort(n)) {
                    double pyLoop = n.y + 9.24 - 4.5;
                    double pyNext = n.y + 32.76 - 4.5;
                    if (MacroEditorScreen.portBoxHit(wx, wy, px, pyLoop)) {
                        return new OutPortHit(n.id, "loop");
                    }
                    if (!MacroEditorScreen.portBoxHit(wx, wy, px, pyNext)) continue;
                    return new OutPortHit(n.id, "next");
                }
                py = n.y + 21.0 - 4.5;
                if (!MacroEditorScreen.portBoxHit(wx, wy, px, py)) continue;
                return new OutPortHit(n.id, "loop");
            }
            py = n.y + 21.0 - 4.5;
            if (!MacroEditorScreen.portBoxHit(wx, wy, px, py)) continue;
            return new OutPortHit(n.id, "");
        }
        return null;
    }

    private boolean hitInPort(MacroGraphNode n, double wx, double wy) {
        if ("GRAPH_START".equals(n.type)) {
            return false;
        }
        double px = n.x - 4.5;
        double py = n.y + 21.0 - 4.5;
        return wx >= px && wx <= px + 9.0 && wy >= py && wy <= py + 9.0;
    }

    @Nullable
    private String nodeIdForInPort(double wx, double wy) {
        for (int i = this.graphNodes.size() - 1; i >= 0; --i) {
            MacroGraphNode n = this.graphNodes.get(i);
            if (this.isNodeInCollapsedGroup(n) || !this.hitInPort(n, wx, wy)) continue;
            return n.id;
        }
        return null;
    }

    private MacroGraphNode findNode(String id) {
        for (MacroGraphNode n : this.graphNodes) {
            if (!n.id.equals(id)) continue;
            return n;
        }
        return null;
    }

    @Nullable
    private String soleSelectedId() {
        if (this.selectedNodeIds.size() == 1) {
            return this.selectedNodeIds.iterator().next();
        }
        return null;
    }

    private void removeNodeFromAllGroups(String nodeId) {
        for (MacroGraphGroup g : this.graphGroups) {
            if (g.memberNodeIds == null) continue;
            g.memberNodeIds.removeIf(id -> id.equals(nodeId));
        }
    }

    private void removeSelectionFromAllGroups() {
        if (this.selectedNodeIds.isEmpty()) {
            MacroEditorScreen.toast("Select nodes first.");
            return;
        }
        for (String id : new ArrayList<String>(this.selectedNodeIds)) {
            this.removeNodeFromAllGroups(id);
        }
        this.graphGroups.removeIf(g -> g.memberNodeIds == null || g.memberNodeIds.isEmpty());
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private static List<String> sortedItemIdsForPicker() {
        return BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static List<String> sortedBlockIdsForPicker() {
        return BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static List<String> sortedEntityTypeIdsForPicker() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static int findHotbarSlotForItem(@Nullable Minecraft client, String itemIdString) {
        if (client == null || client.player == null || itemIdString == null || itemIdString.isBlank()) {
            return -1;
        }
        Identifier id = Identifier.tryParse(itemIdString.trim().toLowerCase(Locale.ROOT));
        if (id == null) {
            return -1;
        }
        Optional opt = BuiltInRegistries.ITEM.getOptional(id);
        if (opt.isEmpty() || opt.get() == Items.AIR) {
            return -1;
        }
        Item want = (Item)opt.get();
        for (int i = 0; i < 9; ++i) {
            ItemStack st = client.player.getInventory().getItem(i);
            if (st.isEmpty() || !st.is(want)) continue;
            return i;
        }
        return -1;
    }

    private boolean isRegistryPickerOpen() {
        return this.registryPickerKind != RegistryPickerKind.NONE;
    }

    private List<String> currentPickerSourceList() {
        return switch (this.registryPickerKind.ordinal()) {
            case 1, 3 -> MacroEditorScreen.sortedItemIdsForPicker();
            case 2 -> MacroEditorScreen.sortedBlockIdsForPicker();
            case 4 -> MacroEditorScreen.sortedEntityTypeIdsForPicker();
            default -> List.of();
        };
    }

    private Component pickerTitleForKind() {
        return switch (this.registryPickerKind.ordinal()) {
            case 1 -> Component.literal("Pick item id");
            case 2 -> Component.literal("Pick block id");
            case 3 -> Component.literal("Pick item \u2192 hotbar slot");
            case 4 -> Component.literal("Pick entity type id");
            default -> Component.literal("Pick");
        };
    }

    private void finalizeMarqueeSelection(boolean mergeSelection) {
        int x0 = (int)Math.min(this.marqueeAX, this.marqueeBX);
        int x1 = (int)Math.max(this.marqueeAX, this.marqueeBX);
        int y0 = (int)Math.min(this.marqueeAY, this.marqueeBY);
        int y1 = (int)Math.max(this.marqueeAY, this.marqueeBY);
        double wx0 = this.toWorldX(x0);
        double wx1 = this.toWorldX(x1);
        double wy0 = this.toWorldY(y0);
        double wy1 = this.toWorldY(y1);
        double minx = Math.min(wx0, wx1);
        double maxx = Math.max(wx0, wx1);
        double miny = Math.min(wy0, wy1);
        double maxy = Math.max(wy0, wy1);
        LinkedHashSet<String> picked = new LinkedHashSet<String>();
        for (MacroGraphNode n : this.graphNodes) {
            if (this.isNodeInCollapsedGroup(n) || n.x + 108.0 < minx || n.x > maxx || n.y + 42.0 < miny || n.y > maxy) continue;
            picked.add(n.id);
        }
        if (!mergeSelection) {
            this.selectedNodeIds.clear();
        }
        this.selectedNodeIds.addAll(picked);
        this.syncDefFromGraph();
    }

    private void layoutItemPickerPanel() {
        this.itemPickerPw = Math.min(280, this.width - 40);
        this.itemPickerPh = Math.min(320, this.height - 40);
        this.itemPickerPx = (this.width - this.itemPickerPw) / 2;
        this.itemPickerPy = (this.height - this.itemPickerPh) / 2;
        this.itemPickerListTop = this.itemPickerPy + 32;
        this.itemPickerListHeight = this.itemPickerPh - 40;
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setX(this.itemPickerPx + 8);
            this.itemPickerFilter.setY(this.itemPickerPy + 8);
            this.itemPickerFilter.setWidth(this.itemPickerPw - 16);
        }
    }

    private void openItemPicker() {
        this.registryPickerKind = RegistryPickerKind.ITEM_GUI;
        this.registryPickerScroll = 0;
        this.layoutItemPickerPanel();
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setValue("");
            this.itemPickerFilter.setHint(Component.literal("type to filter\u2026"));
            this.itemPickerFilter.setVisible(true);
            this.itemPickerFilter.setEditable(true);
        }
    }

    private void openBlockRegistryPicker() {
        this.registryPickerKind = RegistryPickerKind.BLOCK_REGISTRY;
        this.registryPickerScroll = 0;
        this.layoutItemPickerPanel();
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setValue("");
            this.itemPickerFilter.setHint(Component.literal("filter block ids\u2026"));
            this.itemPickerFilter.setVisible(true);
            this.itemPickerFilter.setEditable(true);
        }
    }

    private void openHotbarSlotByItemPicker() {
        this.registryPickerKind = RegistryPickerKind.ITEM_HOTBAR_SLOT;
        this.registryPickerScroll = 0;
        this.layoutItemPickerPanel();
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setValue("");
            this.itemPickerFilter.setHint(Component.literal("filter items\u2026"));
            this.itemPickerFilter.setVisible(true);
            this.itemPickerFilter.setEditable(true);
        }
    }

    private void openEntityTypePicker() {
        this.registryPickerKind = RegistryPickerKind.ENTITY_TYPE_ID;
        this.registryPickerScroll = 0;
        this.layoutItemPickerPanel();
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setValue("");
            this.itemPickerFilter.setHint(Component.literal("filter entity ids\u2026"));
            this.itemPickerFilter.setVisible(true);
            this.itemPickerFilter.setEditable(true);
        }
    }

    private void closeRegistryPicker() {
        this.registryPickerKind = RegistryPickerKind.NONE;
        this.registryPickerScroll = 0;
        if (this.itemPickerFilter != null) {
            this.itemPickerFilter.setVisible(false);
        }
    }

    private boolean registryPickerHandleClick(double mx, double my) {
        if (mx < (double)this.itemPickerPx || mx >= (double)(this.itemPickerPx + this.itemPickerPw) || my < (double)this.itemPickerPy || my >= (double)(this.itemPickerPy + this.itemPickerPh)) {
            this.closeRegistryPicker();
            return true;
        }
        List<String> filtered = this.filteredPickerRows();
        int rowH = 11;
        int rows = Math.max(1, this.itemPickerListHeight / rowH);
        int listY = this.itemPickerListTop;
        if (my >= (double)listY && my < (double)(listY + rows * rowH)) {
            int idx = (int)((my - (double)listY) / (double)rowH) + this.registryPickerScroll;
            if (idx >= 0 && idx < filtered.size()) {
                MacroGraphNode n;
                String pickedId = filtered.get(idx);
                MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
                if (n != null) {
                    switch (this.registryPickerKind.ordinal()) {
                        case 1: {
                            n.guiItemAnyItem = false;
                            n.guiItemId = pickedId;
                            break;
                        }
                        case 2: {
                            if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(MacroEditorScreen.trimType(n.type))) {
                                n.blockCustomId = pickedId;
                                break;
                            }
                            n.blockPreset = "OTHER";
                            n.blockCustomId = pickedId;
                            break;
                        }
                        case 3: {
                            int slot = MacroEditorScreen.findHotbarSlotForItem(this.minecraft, pickedId);
                            if (slot < 0) {
                                MacroEditorScreen.toast("No hotbar stack matches that item.");
                                break;
                            }
                            n.hotbarSlot = slot;
                            if (this.hotbarSlotCycle != null) {
                                this.hotbarSlotCycle.setValue(String.valueOf(slot));
                            }
                            MacroEditorScreen.toast("Hotbar slot set to " + slot + ".");
                            break;
                        }
                        case 4: {
                            n.entityTypeId = pickedId;
                            if (this.inspEntityType == null) break;
                            this.inspEntityType.setValue(pickedId);
                            break;
                        }
                    }
                    this.syncDefFromGraph();
                    this.refreshCompileDiagnostics();
                    this.loadInspectorFromSelection();
                }
            }
            this.closeRegistryPicker();
            return true;
        }
        return false;
    }

    private List<String> filteredPickerRows() {
        String q = this.itemPickerFilter != null ? this.itemPickerFilter.getValue().trim().toLowerCase(Locale.ROOT) : "";
        List<String> all = this.currentPickerSourceList();
        return q.isEmpty() ? all : all.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    private void renderItemPickerLayer(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int idx;
        if (!this.isRegistryPickerOpen()) {
            return;
        }
        this.layoutItemPickerPanel();
        context.fill(0, 0, this.width, this.height, -2012739568);
        context.fill(this.itemPickerPx, this.itemPickerPy, this.itemPickerPx + this.itemPickerPw, this.itemPickerPy + this.itemPickerPh, -267380696);
        context.fill(this.itemPickerPx, this.itemPickerPy, this.itemPickerPx + this.itemPickerPw, this.itemPickerPy + 2, -11899184);
        context.centeredText(this.font, this.pickerTitleForKind(), this.itemPickerPx + this.itemPickerPw / 2, this.itemPickerPy + 6, -1380097);
        List<String> filtered = this.filteredPickerRows();
        int rowH = 11;
        int rows = Math.max(1, this.itemPickerListHeight / rowH);
        int maxScroll = Math.max(0, filtered.size() - rows);
        this.registryPickerScroll = Mth.clamp((int)this.registryPickerScroll, (int)0, (int)maxScroll);
        int y = this.itemPickerListTop;
        for (int i = 0; i < rows && (idx = this.registryPickerScroll + i) < filtered.size(); ++i) {
            boolean hot;
            String row = filtered.get(idx);
            String shown = this.font.plainSubstrByWidth(row, this.itemPickerPw - 20);
            boolean bl = hot = mouseX >= this.itemPickerPx + 6 && mouseX < this.itemPickerPx + this.itemPickerPw - 6 && mouseY >= y && mouseY < y + rowH;
            if (hot) {
                context.fill(this.itemPickerPx + 4, y - 1, this.itemPickerPx + this.itemPickerPw - 4, y + rowH, 1714442384);
            }
            context.text(this.font, Component.literal(shown), this.itemPickerPx + 8, y, -1642753);
            y += rowH;
        }
        if (filtered.size() > rows) {
            context.text(this.font, Component.literal("Scroll wheel"), this.itemPickerPx + 8, this.itemPickerPy + this.itemPickerPh - 14, -8747362);
        }
    }

    private void addNode(MacroNodePalette.Entry ref, double wx, double wy) {
        String typeId = ref.typeId();
        String category = ref.category();
        if ("GRAPH_START".equals(typeId) && this.graphNodes.stream().anyMatch(x -> "GRAPH_START".equals(x.type))) {
            MacroEditorScreen.toast("Only one Start node is allowed.");
            return;
        }
        if ("GRAPH_END".equals(typeId) && this.graphNodes.stream().anyMatch(x -> "GRAPH_END".equals(x.type))) {
            MacroEditorScreen.toast("Only one End node is allowed.");
            return;
        }
        MacroGraphNode n = new MacroGraphNode();
        n.id = "n" + UUID.randomUUID().toString().substring(0, 8);
        n.type = typeId;
        n.category = category;
        n.x = wx - 54.0;
        n.y = wy - 21.0;
        switch (typeId) {
            case "WAIT_TICKS": {
                n.ticks = 20;
                break;
            }
            case "SEND_CHAT": {
                n.text = "Hello";
                break;
            }
            case "MOVE_FORWARD": {
                n.ticks = 40;
                n.moveForwardMeasure = "TICKS";
                n.moveForwardBlocks = 1;
                n.walkFacing = "S";
                n.moveAuxHoldKeyId = "";
                n.moveAuxHoldKey2Id = "";
                break;
            }
            case "REPEAT": {
                n.ticks = 3;
                break;
            }
            case "LOOK_TURN": {
                n.ticks = 90;
                break;
            }
            case "LOOK_PITCH": {
                n.ticks = -10;
                break;
            }
            case "KEY_HOLD": {
                n.ticks = 20;
                String preset = ref.holdKeyPreset();
                n.holdKeyId = preset == null || preset.isBlank() ? "FORWARD" : MacroHoldKeys.normalize(preset);
                break;
            }
            case "GUI_ITEM": {
                n.guiItemMode = "PUT";
                n.guiItemId = "minecraft:cobblestone";
                n.guiItemAnyItem = false;
                n.guiItemAmountAll = false;
                n.guiItemCount = 64;
                n.guiItemDelayTicks = 0;
                break;
            }
            case "CLICK_SLOT": {
                n.clickSlotId = 0;
                n.clickSlotAction = "QUICK_MOVE";
                n.clickSlotButton = 0;
                break;
            }
            case "PRESS_BUTTON": {
                n.pressKeyCode = GLFW.GLFW_KEY_SPACE;
                n.pressKeyModifiers = 0;
                break;
            }
            case "BLOCK_INTERACT": {
                n.blockPreset = "CHEST";
                n.blockCustomId = "";
                n.blockSearchRadius = 10;
                n.blockNavigateMaxTicks = 400;
                break;
            }
            case "USE_HOTBAR_ITEM": {
                n.hotbarSlot = 0;
                break;
            }
            case "HOTBAR_SELECT": {
                n.hotbarSlot = 0;
                break;
            }
            case "DROP_ITEM": {
                n.dropFullStack = false;
                break;
            }
            case "WAIT_LOOK_BLOCK": {
                n.blockCustomId = "minecraft:stone";
                n.ticks = 400;
                break;
            }
            case "WAIT_LOOK_ENTITY": {
                n.entityTypeId = "minecraft:cow";
                n.ticks = 400;
                break;
            }
            default: {
                n.ticks = 0;
                n.text = "";
            }
        }
        this.graphNodes.add(n);
        this.selectedNodeIds.clear();
        this.selectedNodeIds.add(n.id);
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void deleteSelected() {
        if (this.selectedNodeIds.isEmpty()) {
            return;
        }
        HashSet<String> toRemove = new HashSet<String>(this.selectedNodeIds);
        for (String nid : toRemove) {
            MacroGraphNode sel = this.findNode(nid);
            if (sel != null && MacroGraphTypes.isControlNode(sel.type)) continue;
            this.graphEdges.removeIf(e -> e.from.equals(nid) || e.to.equals(nid));
            this.graphNodes.removeIf(n -> n.id.equals(nid));
            this.removeNodeFromAllGroups(nid);
        }
        this.selectedNodeIds.clear();
        this.linkFromId = null;
        this.linkFromSlot = "";
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void loadInspectorFromSelection() {
        this.applyingInspectorLoad = true;
        try {
            MacroGraphNode n;
            MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n == null) {
                this.inspTicks.setHint(Component.literal((this.selectedNodeIds.size() > 1 ? "Multi-select" : "")));
                this.inspTicks.setValue("");
                this.inspText.setValue("");
                this.inspTicks.setEditable(false);
                this.inspText.setEditable(false);
                if (this.inspBlockSearchRadius != null) {
                    this.inspBlockSearchRadius.setVisible(false);
                    this.inspBlockSearchRadius.setEditable(false);
                }
                this.setAdvancedInspectorVisible(false);
                return;
            }
            if (MacroGraphTypes.isControlNode(n.type)) {
                this.inspTicks.setHint(Component.literal(""));
                this.inspTicks.setEditable(false);
                this.inspText.setEditable(false);
                this.inspTicks.setValue("");
                this.inspText.setValue("");
                if (this.inspBlockSearchRadius != null) {
                    this.inspBlockSearchRadius.setVisible(false);
                    this.inspBlockSearchRadius.setEditable(false);
                }
                this.setAdvancedInspectorVisible(false);
                return;
            }
            InspectorMode mode = this.inspectorMode();
            if (mode != InspectorMode.PRESS_BUTTON) {
                this.capturingPressKey = false;
            }
            switch (mode.ordinal()) {
                case 2: {
                    this.inspTicks.setEditable(false);
                    this.inspText.setEditable(true);
                    this.inspText.setValue(n.text == null ? "" : n.text);
                    return;
                }
                case 3: {
                    String a2;
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(true);
                    if ("BLOCKS".equalsIgnoreCase(n.moveForwardMeasure)) {
                        this.inspTicks.setHint(Component.literal("blocks"));
                        this.inspTicks.setValue(String.valueOf(Math.max(1, n.moveForwardBlocks)));
                    } else {
                        this.inspTicks.setHint(Component.literal("ticks"));
                        this.inspTicks.setValue(String.valueOf(Math.max(1, n.ticks)));
                    }
                    this.inspText.setHint(Component.literal("also hold: USE+SNEAK (optional)"));
                    String a1 = n.moveAuxHoldKeyId == null ? "" : n.moveAuxHoldKeyId.trim();
                    String string = a2 = n.moveAuxHoldKey2Id == null ? "" : n.moveAuxHoldKey2Id.trim();
                    if (a1.isEmpty() && a2.isEmpty()) {
                        this.inspText.setValue("");
                        return;
                    }
                    if (a2.isEmpty()) {
                        this.inspText.setValue(a1);
                        return;
                    }
                    this.inspText.setValue(a1 + "+" + a2);
                    return;
                }
                case 5: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("times (0=\u221e)"));
                    this.inspTicks.setValue(String.valueOf(Math.max(0, n.ticks)));
                    return;
                }
                case 6: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("degrees"));
                    this.inspTicks.setValue(String.valueOf(n.ticks));
                    return;
                }
                case 4: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("ticks"));
                    this.inspTicks.setValue(String.valueOf(Math.max(1, n.ticks)));
                    return;
                }
                case 15: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(true);
                    this.inspText.setHint(Component.literal("block id"));
                    this.inspTicks.setHint(Component.literal("max wait (0=\u221e)"));
                    this.inspTicks.setValue(String.valueOf(Math.max(0, n.ticks)));
                    this.inspText.setValue(n.blockCustomId == null ? "" : n.blockCustomId);
                    return;
                }
                case 16: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("max wait (0=\u221e)"));
                    this.inspTicks.setValue(String.valueOf(Math.max(0, n.ticks)));
                    if (this.inspEntityType == null) return;
                    this.inspEntityType.setEditable(true);
                    this.inspEntityType.setValue(n.entityTypeId == null ? "" : n.entityTypeId);
                    return;
                }
                case 10: {
                    this.inspTicks.setEditable(!n.guiItemAmountAll);
                    boolean anyItem = n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId);
                    this.inspText.setEditable(!anyItem);
                    this.inspText.setHint(Component.literal(anyItem ? "all items" : "item id"));
                    this.inspText.setValue(anyItem ? "" : (n.guiItemId == null ? "" : n.guiItemId));
                    if (n.guiItemAmountAll) {
                        this.inspTicks.setHint(Component.literal("all"));
                        this.inspTicks.setValue("");
                    } else {
                        this.inspTicks.setHint(Component.literal("count"));
                        this.inspTicks.setValue(String.valueOf(Math.max(1, n.guiItemCount)));
                    }
                    if (this.inspGuiItemDelayTicks == null) return;
                    this.inspGuiItemDelayTicks.setEditable(true);
                    this.inspGuiItemDelayTicks.setValue(String.valueOf(Math.max(0, Math.min(100, n.guiItemDelayTicks))));
                    return;
                }
                case 11: {
                    this.blockPresetCycle.setValue(MacroAutomation.normalizeBlockPreset(n.blockPreset));
                    this.inspBlockSearchRadius.setEditable(true);
                    this.inspBlockSearchRadius.setHint(Component.literal("blocks"));
                    this.inspBlockSearchRadius.setValue(String.valueOf(Math.max(1, Math.min(32, n.blockSearchRadius))));
                    this.inspTicks.setEditable(true);
                    this.inspTicks.setHint(Component.literal("max walk ticks"));
                    this.inspTicks.setValue(String.valueOf(Math.max(20, n.blockNavigateMaxTicks)));
                    boolean other = "OTHER".equals(MacroAutomation.normalizeBlockPreset(n.blockPreset));
                    this.inspText.setEditable(other);
                    this.inspText.setHint((Component)(other ? Component.literal("minecraft:\u2026") : Component.literal("")));
                    this.inspText.setValue(other && n.blockCustomId != null ? n.blockCustomId : "");
                    if (this.inspEntityType == null) return;
                    this.inspEntityType.setEditable(true);
                    this.inspEntityType.setValue(n.entityTypeId == null ? "" : n.entityTypeId);
                    return;
                }
                case 7: {
                    this.inspTicks.setEditable(false);
                    this.inspText.setEditable(false);
                    this.inspTicks.setValue("");
                    this.inspText.setValue("");
                    return;
                }
                case 8: 
                case 9: {
                    this.inspTicks.setEditable(false);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setValue("");
                    if (this.hotbarSlotCycle == null) return;
                    this.hotbarSlotCycle.setValue(String.valueOf(Mth.clamp((int)n.hotbarSlot, (int)0, (int)8)));
                    return;
                }
                case 13: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("pitch \u00b0"));
                    this.inspTicks.setValue(String.valueOf(n.ticks));
                    return;
                }
                case 12: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("ticks"));
                    this.inspTicks.setValue(String.valueOf(Math.max(1, n.ticks)));
                    if (this.holdKeyCycle == null) return;
                    this.holdKeyCycle.setValue(MacroHoldKeys.normalize(n.holdKeyId));
                    return;
                }
                case 14: {
                    this.inspTicks.setEditable(false);
                    this.inspText.setEditable(false);
                    this.inspTicks.setValue("");
                    this.inspText.setValue("");
                    if (this.dropFullStackButton == null) return;
                    this.dropFullStackButton.setMessage(Component.literal((n.dropFullStack ? "Drop: stack" : "Drop: one")));
                    return;
                }
                case 17: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(true);
                    this.inspTicks.setHint(Component.literal("times"));
                    this.inspTicks.setValue(String.valueOf(Math.max(1, n.fabricatorTimes)));
                    this.inspText.setHint(Component.literal("slot id or item filter"));
                    this.inspText.setValue(n.fabricatorSlot == null ? "0" : n.fabricatorSlot);
                    if (this.inspGuiItemDelayTicks == null) return;
                    this.inspGuiItemDelayTicks.setEditable(true);
                    this.inspGuiItemDelayTicks.setHint(Component.literal("action index"));
                    this.inspGuiItemDelayTicks.setValue(String.valueOf(Math.max(0, n.fabricatorActionIndex)));
                    return;
                }
                case 18: {
                    this.inspTicks.setEditable(true);
                    this.inspText.setEditable(false);
                    this.inspText.setValue("");
                    this.inspTicks.setHint(Component.literal("slot id"));
                    this.inspTicks.setValue(String.valueOf(n.clickSlotId));
                    if (this.inspGuiItemDelayTicks == null) return;
                    this.inspGuiItemDelayTicks.setEditable(true);
                    this.inspGuiItemDelayTicks.setHint(Component.literal("mouse button (0/1)"));
                    this.inspGuiItemDelayTicks.setValue(String.valueOf(Math.max(0, n.clickSlotButton)));
                    return;
                }
                case 19: {
                    this.inspTicks.setEditable(false);
                    this.inspText.setEditable(false);
                    this.inspTicks.setValue("");
                    this.inspText.setValue("");
                    return;
                }
            }
            return;
        }
        finally {
            this.applyingInspectorLoad = false;
            this.refreshInspectorControls();
            this.layoutInspectorWidgets();
        }
    }

    private void setAdvancedInspectorVisible(boolean visible) {
        if (this.holdKeyCycle != null) {
            this.holdKeyCycle.visible = visible;
        }
        if (this.hotbarSlotCycle != null) {
            this.hotbarSlotCycle.visible = visible;
        }
        if (this.guiItemPickButton != null) {
            this.guiItemPickButton.visible = visible;
        }
        if (this.dropFullStackButton != null) {
            this.dropFullStackButton.visible = visible;
        }
        if (this.blockRegistryPickButton != null) {
            this.blockRegistryPickButton.visible = visible;
        }
        if (this.entityTypePickButton != null) {
            this.entityTypePickButton.visible = visible;
        }
        if (this.hotbarSlotByItemButton != null) {
            this.hotbarSlotByItemButton.visible = visible;
        }
        if (this.inspEntityType != null) {
            this.inspEntityType.setVisible(visible);
        }
    }

    private InspectorMode inspectorMode() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null) {
            return InspectorMode.EMPTY;
        }
        if (MacroGraphTypes.isControlNode(n.type)) {
            return InspectorMode.CONTROL;
        }
        String t = MacroEditorScreen.trimType(n.type);
        if (MacroStepType.SEND_CHAT.name().equals(t)) {
            return InspectorMode.CHAT;
        }
        if (MacroStepType.MOVE_FORWARD.name().equals(t)) {
            return InspectorMode.WALK;
        }
        if (MacroGraphTypes.isRepeatNode(t)) {
            return InspectorMode.REPEAT;
        }
        if ("LOOK_TURN".equals(t)) {
            return InspectorMode.TURN;
        }
        if ("LOOK_PITCH".equals(t)) {
            return InspectorMode.LOOK_PITCH;
        }
        if ("KEY_HOLD".equals(t) || "ATTACK".equals(t)) {
            return InspectorMode.KEY_HOLD;
        }
        if ("GUI_ITEM".equals(t)) {
            return InspectorMode.GUI_ITEM;
        }
        if ("HOTBAR_SELECT".equals(t)) {
            return InspectorMode.HOTBAR_SELECT;
        }
        if ("USE_HOTBAR_ITEM".equals(t) || "USE_ITEM".equals(t)) {
            return InspectorMode.HOTBAR_USE;
        }
        if ("DROP_ITEM".equals(t)) {
            return InspectorMode.DROP_ITEM;
        }
        if ("BLOCK_INTERACT".equals(t) || "USE_BLOCK".equals(t)) {
            return InspectorMode.BLOCK_INTERACT;
        }
        if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(t)) {
            return InspectorMode.LOOK_GATE_BLOCK;
        }
        if (MacroStepType.WAIT_LOOK_ENTITY.name().equals(t)) {
            return InspectorMode.LOOK_GATE_ENTITY;
        }
        if (MacroStepType.WAIT_TICKS.name().equals(t) || MacroEditorScreen.isStubTicks(t)) {
            return InspectorMode.WAIT_OR_STUB;
        }
        if (MacroStepType.FABRICATOR_SEND.name().equals(t)) {
            return InspectorMode.FABRICATOR;
        }
        if (MacroStepType.CLICK_SLOT.name().equals(t)) {
            return InspectorMode.CLICK_SLOT;
        }
        if (MacroStepType.PRESS_BUTTON.name().equals(t)) {
            return InspectorMode.PRESS_BUTTON;
        }
        if (MacroStepType.CLOSE_SCREEN.name().equals(t) || MacroStepType.CLOSE_GUI.name().equals(t) || MacroStepType.UI_UTILS_TOGGLE_DELAY.name().equals(t) || MacroStepType.UI_UTILS_FLUSH_QUEUE.name().equals(t) || MacroStepType.PACKET_DELAY_TOGGLE.name().equals(t) || MacroStepType.PACKET_DELAY_FLUSH.name().equals(t) || "SWAP_OFFHAND".equals(t)) {
            return InspectorMode.UTILITY_NO_FIELDS;
        }
        return InspectorMode.WAIT_OR_STUB;
    }

    private String inspectorOverlayHint() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && MacroGraphTypes.isRepeatNode(n.type)) {
            return "Mint \"Repeat\" = body each time (chain \u2192 End). Double-click for orange \"Continue\" port.";
        }
        String t = n != null ? MacroEditorScreen.trimType(n.type) : "";
        return switch (this.inspectorMode().ordinal()) {
            case 0 -> "Select a node to edit.";
            case 1 -> "Control nodes have no parameters.";
            case 2 -> "Chat or /command";
            case 6 -> "Yaw change in degrees (+ right, \u2212 left). Applied once when the macro runs.";
            case 7 -> {
                if ("SWAP_OFFHAND".equals(t)) {
                    yield "Swaps main/offhand (holds F briefly when the macro runs).";
                }
                if (MacroStepType.CLOSE_GUI.name().equals(t)) {
                    yield "Vanilla container close: notifies the server (like Esc on a chest).";
                }
                if (MacroStepType.CLOSE_SCREEN.name().equals(t)) {
                    yield "Client-only setScreen(null). Does not send CloseHandledScreen \u2014 use Close GUI (server) for inventories.";
                }
                if (MacroStepType.PACKET_DELAY_TOGGLE.name().equals(t)) {
                    yield "Toggles packet delay on/off (same as Packet Utils delay toggle).";
                }
                if (MacroStepType.PACKET_DELAY_FLUSH.name().equals(t)) {
                    yield "Flushes the packet delay queue immediately.";
                }
                yield "This node has no extra settings.";
            }
            case 17 -> "Sends a fabricated click packet using Packet Fabricator settings for this step only.";
            case 18 -> "Clicks a handler slot while a container GUI is open. Slot id is the handler index (e.g. -999 = outside cursor). Button is 0 = left / 1 = right for pickup and throw actions.";
            case 19 -> "Presses any keyboard key once. Click the button below, then press the desired key (Shift/Ctrl/Alt combos supported). Works in chat, text fields, and bound game keys.";
            case 8 -> "Right-click / use the stack in that hotbar column (0 = left, 8 = right). Your selected slot is restored after. Pick slot by item\u2026 finds the first hotbar column holding that item.";
            case 9 -> "Only changes your selected hotbar slot (no right-click). Pick slot by item\u2026 matches your inventory hotbar.";
            case 13 -> "Pitch delta in degrees (negative looks up). Clamped to \u00b190\u00b0 view.";
            case 12 -> "Holds a vanilla key for N client ticks (movement, attack, use, inventory, etc.).";
            case 14 -> "Drops from the currently selected hotbar slot (one item vs whole stack).";
            case 10 -> "Shift-moves items in the open container. PUT = into chest, TAKE = to inventory. Filter = specific item or all item types. Amount = exact count or all stacks. Delay = client ticks between each shift-click (0 = burst up to 64 per tick).";
            case 11 -> "Sphere search + path to block. Optional entity id below is editor-only (not used when this node runs).";
            case 15 -> "Macro pauses until your crosshair is on that block id. Timeout = max client ticks waiting (0 = wait forever).";
            case 16 -> "Macro pauses until your crosshair is on that entity type. Timeout = max client ticks (0 = forever).";
            case 3 -> "Holds forward for N ticks (or block distance). Optional second field: extra keys to hold at the same time (e.g. USE+SNEAK for speedbridge \u2014 vanilla key names, + or comma between).";
            default -> "";
        };
    }

    private void layoutInspectorWidgets() {
        boolean lookGateLayout;
        boolean showLookEntityExtras;
        if (this.walkMeasureButton == null) {
            return;
        }
        int ix = this.width - this.inspectorW() + 8;
        InspectorMode mode = this.inspectorMode();
        String hint = this.inspectorOverlayHint();
        int y = CANVAS_TOP + (hint.isEmpty() ? 18 : 30);
        this.facingButton.visible = mode == InspectorMode.WALK;
        this.walkMeasureButton.visible = mode == InspectorMode.WALK;
        this.guiItemModeButton.visible = mode == InspectorMode.GUI_ITEM;
        this.guiItemFilterButton.visible = mode == InspectorMode.GUI_ITEM;
        this.guiItemAmountAllButton.visible = mode == InspectorMode.GUI_ITEM;
        if (this.clickSlotActionButton != null) {
            this.clickSlotActionButton.visible = mode == InspectorMode.CLICK_SLOT;
        }
        if (this.pressKeyCaptureButton != null) {
            this.pressKeyCaptureButton.visible = mode == InspectorMode.PRESS_BUTTON;
        }
        this.blockPresetCycle.visible = mode == InspectorMode.BLOCK_INTERACT;
        boolean showBlockSearch = mode == InspectorMode.BLOCK_INTERACT;
        MacroGraphNode selNode = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        boolean guiItemAll = selNode != null && selNode.guiItemAmountAll;
        boolean guiItemAny = selNode != null && (selNode.guiItemAnyItem || MacroAutomation.isAnyItem(selNode.guiItemId));
        boolean showGuiItemDelay = mode == InspectorMode.GUI_ITEM || mode == InspectorMode.FABRICATOR || mode == InspectorMode.CLICK_SLOT;
        boolean blockOther = selNode != null && "OTHER".equals(MacroAutomation.normalizeBlockPreset(selNode.blockPreset));
        boolean showTicks = mode == InspectorMode.WALK || mode == InspectorMode.WAIT_OR_STUB || mode == InspectorMode.REPEAT || mode == InspectorMode.TURN || mode == InspectorMode.LOOK_PITCH || mode == InspectorMode.KEY_HOLD || mode == InspectorMode.BLOCK_INTERACT || mode == InspectorMode.LOOK_GATE_BLOCK || mode == InspectorMode.LOOK_GATE_ENTITY || mode == InspectorMode.FABRICATOR || mode == InspectorMode.CLICK_SLOT || mode == InspectorMode.GUI_ITEM && !guiItemAll;
        boolean showText = mode == InspectorMode.CHAT || mode == InspectorMode.WALK || mode == InspectorMode.GUI_ITEM && !guiItemAny || mode == InspectorMode.FABRICATOR || mode == InspectorMode.BLOCK_INTERACT && blockOther || mode == InspectorMode.LOOK_GATE_BLOCK;
        boolean showHoldKey = mode == InspectorMode.KEY_HOLD;
        boolean showHotbarPick = mode == InspectorMode.HOTBAR_USE || mode == InspectorMode.HOTBAR_SELECT;
        boolean showGuiPick = mode == InspectorMode.GUI_ITEM && !guiItemAny;
        boolean showDropBtn = mode == InspectorMode.DROP_ITEM;
        boolean showBlockInspectorExtras = mode == InspectorMode.BLOCK_INTERACT;
        boolean showLookBlockExtras = mode == InspectorMode.LOOK_GATE_BLOCK;
        boolean bl = showLookEntityExtras = mode == InspectorMode.LOOK_GATE_ENTITY;
        if (this.holdKeyCycle != null) {
            this.holdKeyCycle.visible = showHoldKey;
        }
        if (this.hotbarSlotCycle != null) {
            this.hotbarSlotCycle.visible = showHotbarPick;
        }
        if (this.guiItemPickButton != null) {
            this.guiItemPickButton.visible = showGuiPick;
        }
        if (this.dropFullStackButton != null) {
            this.dropFullStackButton.visible = showDropBtn;
        }
        if (this.blockRegistryPickButton != null) {
            boolean bl2 = this.blockRegistryPickButton.visible = showBlockInspectorExtras || showLookBlockExtras;
        }
        if (this.entityTypePickButton != null) {
            boolean bl3 = this.entityTypePickButton.visible = showBlockInspectorExtras || showLookEntityExtras;
        }
        if (this.hotbarSlotByItemButton != null) {
            this.hotbarSlotByItemButton.visible = showHotbarPick;
        }
        if (this.inspEntityType != null) {
            this.inspEntityType.setVisible(showBlockInspectorExtras || showLookEntityExtras);
        }
        this.inspTicks.setVisible(showTicks);
        if (this.inspGuiItemDelayTicks != null) {
            this.inspGuiItemDelayTicks.setVisible(showGuiItemDelay);
        }
        this.inspText.setVisible(showText);
        if (this.inspBlockSearchRadius != null) {
            this.inspBlockSearchRadius.setVisible(showBlockSearch);
        }
        boolean bl4 = lookGateLayout = mode == InspectorMode.LOOK_GATE_BLOCK || mode == InspectorMode.LOOK_GATE_ENTITY;
        if (lookGateLayout && showTicks) {
            this.inspTicks.setX(ix);
            this.inspTicks.setY(y);
            y += 22;
        }
        if (mode == InspectorMode.LOOK_GATE_BLOCK && this.blockRegistryPickButton != null && this.blockRegistryPickButton.visible) {
            this.blockRegistryPickButton.setX(ix);
            this.blockRegistryPickButton.setY(y);
            y += 16;
        }
        if (mode == InspectorMode.LOOK_GATE_BLOCK && showText) {
            this.inspText.setX(ix);
            this.inspText.setY(y);
            y += 54;
        }
        if (mode == InspectorMode.LOOK_GATE_ENTITY && this.entityTypePickButton != null && this.entityTypePickButton.visible) {
            this.entityTypePickButton.setX(ix);
            this.entityTypePickButton.setY(y);
            y += 16;
        }
        if (mode == InspectorMode.LOOK_GATE_ENTITY && this.inspEntityType != null && this.inspEntityType.visible) {
            this.inspEntityType.setX(ix);
            this.inspEntityType.setY(y);
            y += 22;
        }
        if (this.facingButton.visible) {
            this.facingButton.setX(ix);
            this.facingButton.setY(y);
            y += 14;
        }
        if (this.walkMeasureButton.visible) {
            this.walkMeasureButton.setX(ix);
            this.walkMeasureButton.setY(y);
            y += 14;
        }
        if (this.guiItemModeButton.visible) {
            this.guiItemModeButton.setX(ix);
            this.guiItemModeButton.setY(y);
            y += 14;
        }
        if (this.guiItemFilterButton.visible) {
            this.guiItemFilterButton.setX(ix);
            this.guiItemFilterButton.setY(y);
            y += 14;
        }
        if (this.guiItemAmountAllButton.visible) {
            this.guiItemAmountAllButton.setX(ix);
            this.guiItemAmountAllButton.setY(y);
            y += 14;
        }
        if (showGuiItemDelay && this.inspGuiItemDelayTicks != null && mode != InspectorMode.CLICK_SLOT) {
            this.inspGuiItemDelayTicks.setX(ix);
            this.inspGuiItemDelayTicks.setY(y);
            y += 22;
        }
        if (this.blockPresetCycle.visible) {
            this.blockPresetCycle.setX(ix);
            this.blockPresetCycle.setY(y);
            this.blockPresetCycle.setWidth(this.inspectorW() - 16);
            y += 18;
        }
        if (this.blockRegistryPickButton != null && this.blockRegistryPickButton.visible && mode != InspectorMode.LOOK_GATE_BLOCK) {
            this.blockRegistryPickButton.setX(ix);
            this.blockRegistryPickButton.setY(y);
            y += 16;
        }
        if (showBlockSearch && this.inspBlockSearchRadius != null) {
            this.inspBlockSearchRadius.setX(ix);
            this.inspBlockSearchRadius.setY(y);
            y += 22;
        }
        if (this.entityTypePickButton != null && this.entityTypePickButton.visible && mode != InspectorMode.LOOK_GATE_ENTITY) {
            this.entityTypePickButton.setX(ix);
            this.entityTypePickButton.setY(y);
            y += 16;
        }
        if (showBlockInspectorExtras && this.inspEntityType != null && this.inspEntityType.visible && mode != InspectorMode.LOOK_GATE_ENTITY) {
            this.inspEntityType.setX(ix);
            this.inspEntityType.setY(y);
            y += 22;
        }
        if (mode == InspectorMode.GUI_ITEM && this.guiItemPickButton != null && this.guiItemPickButton.visible) {
            this.guiItemPickButton.setX(ix);
            this.guiItemPickButton.setY(y);
            y += 16;
        }
        if (showHoldKey && this.holdKeyCycle != null) {
            this.holdKeyCycle.setX(ix);
            this.holdKeyCycle.setY(y);
            y += 18;
        }
        if (showHotbarPick && this.hotbarSlotCycle != null) {
            this.hotbarSlotCycle.setX(ix);
            this.hotbarSlotCycle.setY(y);
            y += 18;
        }
        if (this.hotbarSlotByItemButton != null && this.hotbarSlotByItemButton.visible) {
            this.hotbarSlotByItemButton.setX(ix);
            this.hotbarSlotByItemButton.setY(y);
            y += 16;
        }
        if (showTicks && !lookGateLayout && mode != InspectorMode.CLICK_SLOT) {
            this.inspTicks.setX(ix);
            this.inspTicks.setY(y);
            y += 22;
        }
        if (mode == InspectorMode.CLICK_SLOT) {
            if (showTicks) {
                this.inspTicks.setX(ix);
                this.inspTicks.setY(y);
                y += 22;
            }
            if (this.clickSlotActionButton != null && this.clickSlotActionButton.visible) {
                this.clickSlotActionButton.setX(ix);
                this.clickSlotActionButton.setY(y);
                y += 14;
            }
            if (showGuiItemDelay && this.inspGuiItemDelayTicks != null) {
                this.inspGuiItemDelayTicks.setX(ix);
                this.inspGuiItemDelayTicks.setY(y);
                y += 22;
            }
        }
        if (this.pressKeyCaptureButton != null && this.pressKeyCaptureButton.visible) {
            this.pressKeyCaptureButton.setX(ix);
            this.pressKeyCaptureButton.setY(y);
            y += 14;
        }
        if (showDropBtn && this.dropFullStackButton != null) {
            this.dropFullStackButton.setX(ix);
            this.dropFullStackButton.setY(y);
            y += 16;
        }
        if (showText && mode != InspectorMode.LOOK_GATE_BLOCK) {
            this.inspText.setX(ix);
            this.inspText.setY(y);
            y += 54;
        }
        if (mode == InspectorMode.EMPTY || mode == InspectorMode.CONTROL || mode == InspectorMode.UTILITY_NO_FIELDS) {
            y += 6;
        }
        this.compilePanelTop = Math.min(y + 8, this.height - 48);
        this.compilePanelHeight = Math.min(132, Math.max(30, this.measureCompilePanelHeight()));
        int belowCompile = this.compilePanelTop + this.compilePanelHeight + 4;
        this.libraryListTop = Math.min(belowCompile + 14, this.height - 36);
        this.libraryVisibleRows = Math.max(2, Math.min(10, (this.height - this.libraryListTop - 8) / (LIB_ROW_H + LIB_ROW_GAP)));
    }

    private void refreshInspectorControls() {
        this.updateWalkMeasureButton();
        this.updateFacingButton();
        this.updateGuiItemButtons();
        this.updateClickSlotActionButton();
        this.updatePressKeyCaptureButton();
    }

    private void cycleFacing() {
        String f;
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) != MacroStepType.MOVE_FORWARD) {
            return;
        }
        n.walkFacing = switch (f = MacroGraphTypes.normalizeWalkFacing(n.walkFacing)) {
            case "PLAYER" -> "N";
            case "N" -> "E";
            case "E" -> "S";
            case "S" -> "W";
            case "W" -> "PLAYER";
            default -> "PLAYER";
        };
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void updateFacingButton() {
        MacroGraphNode n;
        if (this.facingButton == null) {
            return;
        }
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) == MacroStepType.MOVE_FORWARD) {
            String f = MacroGraphTypes.normalizeWalkFacing(n.walkFacing);
            String label = "PLAYER".equals(f) ? "Facing: view" : "Facing: " + f;
            this.facingButton.setMessage(Component.literal(label));
        }
    }

    private boolean repeatHasExplicitNextWire(String repeatNodeId) {
        for (MacroGraphEdge e : this.graphEdges) {
            if (e == null || e.from == null || !e.from.equals(repeatNodeId) || !"next".equalsIgnoreCase(e.fromSlot == null ? "" : e.fromSlot.trim())) continue;
            return true;
        }
        return false;
    }

    private boolean repeatShowsMergePort(MacroGraphNode n) {
        return MacroGraphTypes.isRepeatNode(n.type) && (n.repeatShowNextPort || this.repeatHasExplicitNextWire(n.id));
    }

    private static String trimType(String type) {
        return type == null ? "" : type.trim();
    }

    private static boolean nodeUsesTicksField(MacroGraphNode n) {
        String t = MacroEditorScreen.trimType(n.type);
        if (MacroGraphTypes.isRepeatNode(t)) {
            return true;
        }
        MacroStepType k = MacroStepType.fromString(t);
        if (k == MacroStepType.WAIT_TICKS || k == MacroStepType.MOVE_FORWARD || k == MacroStepType.LOOK_TURN || k == MacroStepType.WAIT_LOOK_BLOCK || k == MacroStepType.WAIT_LOOK_ENTITY) {
            return true;
        }
        if ("GUI_ITEM".equals(t)) {
            return false;
        }
        if ("BLOCK_INTERACT".equals(t) || "USE_BLOCK".equals(t)) {
            return false;
        }
        if ("USE_HOTBAR_ITEM".equals(t) || "USE_ITEM".equals(t)) {
            return false;
        }
        return MacroEditorScreen.isStubTicks(t);
    }

    private static boolean isStubTicks(String typeTrimmed) {
        return false;
    }

    private void applyWalkAuxKeysFromInspector(MacroGraphNode n) {
        String raw;
        n.moveAuxHoldKeyId = "";
        n.moveAuxHoldKey2Id = "";
        String string = raw = this.inspText.getValue() == null ? "" : this.inspText.getValue().trim();
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("[+;,|]+");
        ArrayList<String> keys = new ArrayList<String>();
        for (String p : parts) {
            String k;
            if (p == null || (k = MacroHoldKeys.normalizeAuxKey(p.trim())).isEmpty()) continue;
            boolean dup = false;
            for (String e : keys) {
                if (!e.equals(k)) continue;
                dup = true;
                break;
            }
            if (dup) continue;
            keys.add(k);
        }
        if (!keys.isEmpty()) {
            n.moveAuxHoldKeyId = keys.getFirst();
        }
        if (keys.size() > 1) {
            n.moveAuxHoldKey2Id = keys.get(1);
        }
    }

    private void applyInspectorToSelection() {
        String typeStr2;
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || MacroGraphTypes.isControlNode(n.type)) {
            return;
        }
        if (MacroEditorScreen.nodeUsesTicksField(n)) {
            MacroStepType kt = MacroStepType.fromString(MacroEditorScreen.trimType(n.type));
            String typeStr = MacroEditorScreen.trimType(n.type);
            if ("LOOK_TURN".equals(typeStr)) {
                try {
                    n.ticks = Math.max(-3600, Math.min(3600, Integer.parseInt(this.inspTicks.getValue().trim())));
                }
                catch (NumberFormatException e) {
                    n.ticks = 90;
                }
            } else if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(typeStr) || MacroStepType.WAIT_LOOK_ENTITY.name().equals(typeStr)) {
                try {
                    n.ticks = Math.max(0, Integer.parseInt(this.inspTicks.getValue().trim()));
                }
                catch (NumberFormatException e) {
                    n.ticks = 400;
                }
            } else {
                try {
                    int v = Integer.parseInt(this.inspTicks.getValue().trim());
                    if (MacroGraphTypes.isRepeatNode(typeStr)) {
                        n.ticks = Math.max(0, v);
                    } else {
                        int vv = Math.max(1, v);
                        if (kt == MacroStepType.MOVE_FORWARD && "BLOCKS".equalsIgnoreCase(n.moveForwardMeasure)) {
                            n.moveForwardBlocks = vv;
                        } else {
                            n.ticks = vv;
                        }
                    }
                }
                catch (NumberFormatException e) {
                    if (kt == MacroStepType.MOVE_FORWARD && "BLOCKS".equalsIgnoreCase(n.moveForwardMeasure)) {
                        n.moveForwardBlocks = 1;
                    }
                    n.ticks = MacroGraphTypes.isRepeatNode(typeStr) ? 3 : 20;
                }
            }
        }
        if (MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) == MacroStepType.SEND_CHAT) {
            n.text = this.inspText.getValue();
        }
        if (MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) == MacroStepType.MOVE_FORWARD) {
            this.applyWalkAuxKeysFromInspector(n);
        }
        if ("GUI_ITEM".equals(typeStr2 = MacroEditorScreen.trimType(n.type))) {
            boolean anyItem = n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId);
            if (!anyItem) {
                n.guiItemId = this.inspText.getValue().trim();
            }
            if (n.guiItemAmountAll) {
                n.guiItemCount = -1;
            } else {
                try {
                    n.guiItemCount = Math.max(1, Integer.parseInt(this.inspTicks.getValue().trim()));
                }
                catch (NumberFormatException e) {
                    n.guiItemCount = 1;
                }
            }
            if (this.inspGuiItemDelayTicks != null) {
                try {
                    n.guiItemDelayTicks = Math.max(0, Math.min(100, Integer.parseInt(this.inspGuiItemDelayTicks.getValue().trim())));
                }
                catch (NumberFormatException e) {
                    n.guiItemDelayTicks = 0;
                }
            }
        }
        if ("BLOCK_INTERACT".equals(typeStr2) || "USE_BLOCK".equals(typeStr2)) {
            try {
                n.blockSearchRadius = Math.max(1, Math.min(32, Integer.parseInt(this.inspBlockSearchRadius.getValue().trim())));
            }
            catch (NumberFormatException e) {
                n.blockSearchRadius = 10;
            }
            try {
                n.blockNavigateMaxTicks = Math.max(20, Integer.parseInt(this.inspTicks.getValue().trim()));
            }
            catch (NumberFormatException e) {
                n.blockNavigateMaxTicks = 400;
            }
            if ("OTHER".equals(MacroAutomation.normalizeBlockPreset(n.blockPreset))) {
                n.blockCustomId = this.inspText.getValue().trim();
            }
            if (this.inspEntityType != null) {
                n.entityTypeId = this.inspEntityType.getValue().trim();
            }
        }
        if ("USE_HOTBAR_ITEM".equals(typeStr2) || "USE_ITEM".equals(typeStr2) || "HOTBAR_SELECT".equals(typeStr2)) {
            try {
                n.hotbarSlot = this.hotbarSlotCycle != null ? Mth.clamp((int)Integer.parseInt(this.hotbarSlotCycle.getValue().trim()), (int)0, (int)8) : Mth.clamp((int)Integer.parseInt(this.inspTicks.getValue().trim()), (int)0, (int)8);
            }
            catch (NumberFormatException e) {
                n.hotbarSlot = 0;
            }
        }
        if ("LOOK_PITCH".equals(typeStr2)) {
            try {
                n.ticks = Mth.clamp((int)Integer.parseInt(this.inspTicks.getValue().trim()), (int)-1800, (int)1800);
            }
            catch (NumberFormatException e) {
                n.ticks = -10;
            }
        }
        if ("KEY_HOLD".equals(typeStr2) || "ATTACK".equals(typeStr2)) {
            try {
                n.ticks = Math.max(1, Integer.parseInt(this.inspTicks.getValue().trim()));
            }
            catch (NumberFormatException e) {
                n.ticks = 20;
            }
        }
        if (MacroStepType.WAIT_LOOK_BLOCK.name().equals(typeStr2)) {
            n.blockCustomId = this.inspText.getValue().trim();
        }
        if (MacroStepType.WAIT_LOOK_ENTITY.name().equals(typeStr2) && this.inspEntityType != null) {
            n.entityTypeId = this.inspEntityType.getValue().trim();
        }
        if (MacroStepType.FABRICATOR_SEND.name().equals(typeStr2)) {
            n.fabricatorSlot = this.inspText.getValue().trim();
            if (n.fabricatorSlot.isBlank()) {
                n.fabricatorSlot = "0";
            }
            try {
                n.fabricatorTimes = Math.max(1, Integer.parseInt(this.inspTicks.getValue().trim()));
            }
            catch (NumberFormatException e) {
                n.fabricatorTimes = 1;
            }
            if (this.inspGuiItemDelayTicks != null) {
                try {
                    n.fabricatorActionIndex = Math.max(0, Integer.parseInt(this.inspGuiItemDelayTicks.getValue().trim()));
                }
                catch (NumberFormatException e) {
                    n.fabricatorActionIndex = 0;
                }
            }
        }
        if (MacroStepType.CLICK_SLOT.name().equals(typeStr2)) {
            try {
                n.clickSlotId = Integer.parseInt(this.inspTicks.getValue().trim());
            }
            catch (NumberFormatException e) {
                n.clickSlotId = 0;
            }
            if (this.inspGuiItemDelayTicks != null) {
                try {
                    n.clickSlotButton = Math.max(0, Integer.parseInt(this.inspGuiItemDelayTicks.getValue().trim()));
                }
                catch (NumberFormatException e) {
                    n.clickSlotButton = 0;
                }
            }
        }
    }

    private void syncInspectorToSelection() {
        if (this.soleSelectedId() == null || this.inspTicks == null || this.inspBlockSearchRadius == null) {
            return;
        }
        this.applyInspectorToSelection();
    }

    private void cycleWalkMeasure() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) != MacroStepType.MOVE_FORWARD) {
            return;
        }
        n.moveForwardMeasure = "BLOCKS".equalsIgnoreCase(n.moveForwardMeasure) ? "TICKS" : "BLOCKS";
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void updateWalkMeasureButton() {
        MacroGraphNode n;
        if (this.walkMeasureButton == null) {
            return;
        }
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && MacroStepType.fromString(MacroEditorScreen.trimType(n.type)) == MacroStepType.MOVE_FORWARD) {
            boolean blocks = "BLOCKS".equalsIgnoreCase(n.moveForwardMeasure);
            this.walkMeasureButton.setMessage(Component.literal((blocks ? "Measure: blocks" : "Measure: ticks")));
        }
    }

    private void updateGuiItemButtons() {
        MacroGraphNode n;
        if (this.guiItemModeButton == null) {
            return;
        }
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && "GUI_ITEM".equals(MacroEditorScreen.trimType(n.type))) {
            boolean take = "TAKE".equalsIgnoreCase(n.guiItemMode);
            boolean anyItem = n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId);
            this.guiItemModeButton.setMessage(Component.literal((take ? "Into/Out: take" : "Into/Out: put")));
            this.guiItemFilterButton.setMessage(Component.literal((anyItem ? "Filter: all items" : "Filter: specific")));
            this.guiItemAmountAllButton.setMessage(Component.literal((n.guiItemAmountAll ? "Amount: all" : "Amount: count")));
        }
    }

    private void cycleGuiItemMode() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || !"GUI_ITEM".equals(MacroEditorScreen.trimType(n.type))) {
            return;
        }
        n.guiItemMode = "TAKE".equalsIgnoreCase(n.guiItemMode) ? "PUT" : "TAKE";
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void cycleGuiItemFilter() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || !"GUI_ITEM".equals(MacroEditorScreen.trimType(n.type))) {
            return;
        }
        boolean nextAny = !(n.guiItemAnyItem || MacroAutomation.isAnyItem(n.guiItemId));
        n.guiItemAnyItem = nextAny;
        if (nextAny) {
            if (!MacroAutomation.isAnyItem(n.guiItemId) && n.guiItemId != null && !n.guiItemId.isBlank()) {
                // keep guiItemId as template for when user switches back to specific
            } else if (n.guiItemId == null || n.guiItemId.isBlank() || MacroAutomation.isAnyItem(n.guiItemId)) {
                n.guiItemId = "minecraft:cobblestone";
            }
        } else if (n.guiItemId == null || n.guiItemId.isBlank() || MacroAutomation.isAnyItem(n.guiItemId)) {
            n.guiItemId = "minecraft:cobblestone";
        }
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void cycleGuiItemAmountMode() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || !"GUI_ITEM".equals(MacroEditorScreen.trimType(n.type))) {
            return;
        }
        boolean bl = n.guiItemAmountAll = !n.guiItemAmountAll;
        if (!n.guiItemAmountAll && n.guiItemCount < 1) {
            n.guiItemCount = 1;
        }
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void cycleClickSlotAction() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || !MacroStepType.CLICK_SLOT.name().equals(MacroEditorScreen.trimType(n.type))) {
            return;
        }
        n.clickSlotAction = MacroSlotActions.next(n.clickSlotAction);
        this.syncDefFromGraph();
        this.loadInspectorFromSelection();
    }

    private void updateClickSlotActionButton() {
        MacroGraphNode n;
        if (this.clickSlotActionButton == null) {
            return;
        }
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && MacroStepType.CLICK_SLOT.name().equals(MacroEditorScreen.trimType(n.type))) {
            this.clickSlotActionButton.setMessage(Component.literal("Action: " + MacroSlotActions.normalize(n.clickSlotAction)));
        }
    }

    private void startPressKeyCapture() {
        MacroGraphNode n;
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n == null || !MacroStepType.PRESS_BUTTON.name().equals(MacroEditorScreen.trimType(n.type))) {
            return;
        }
        this.capturingPressKey = true;
        if (this.pressKeyCaptureButton != null) {
            this.pressKeyCaptureButton.setMessage(Component.literal("Press any key\u2026"));
        }
    }

    @Override
    public boolean consumesGlobalHotkeys() {
        return this.capturingPressKey;
    }

    private void updatePressKeyCaptureButton() {
        MacroGraphNode n;
        if (this.pressKeyCaptureButton == null || this.capturingPressKey) {
            return;
        }
        MacroGraphNode macroGraphNode = n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
        if (n != null && MacroStepType.PRESS_BUTTON.name().equals(MacroEditorScreen.trimType(n.type))) {
            this.pressKeyCaptureButton.setMessage(Component.literal("Key: " + MacroKeyPress.keyLabel(n.pressKeyCode, n.pressKeyModifiers)));
        }
    }

    private void refreshCompileDiagnostics() {
        if (this.def == null) {
            return;
        }
        this.syncDefFromGraphWithoutDirty();
        this.def.normalize();
        this.liveCompileDiagnostics.clear();
        this.liveCompileDiagnostics.addAll(MacroGraphCompiler.collectDiagnostics(this.def));
    }

    private void exportCurrentMacro() {
        if (this.minecraft == null || this.def == null) {
            return;
        }
        this.captureHeader();
        this.applyInspectorToSelection();
        this.syncDefFromGraphWithoutDirty();
        this.def.normalize();
        MacroShare.exportDefinitionToClipboard(this.minecraft, this.def);
    }

    private int measureCompilePanelHeight() {
        int lineCount;
        int maxW = Math.max(40, this.inspectorW() - 16);
        if (this.liveCompileDiagnostics.isEmpty()) {
            String plan = this.def != null ? MacroGraphCompiler.summarizePlan(this.def) : "";
            lineCount = Math.min(4, this.wrapPlain("OK \u2014 " + plan, maxW).size());
            lineCount = Math.max(1, lineCount);
        } else {
            lineCount = 0;
            for (String d : this.liveCompileDiagnostics) {
                lineCount += this.wrapPlain(d, maxW).size();
            }
        }
        int maxShown = 9;
        int clipped = Math.max(0, lineCount - maxShown);
        lineCount = Math.min(lineCount, maxShown) + (clipped > 0 ? 1 : 0);
        return 14 + lineCount * 10;
    }

    private List<String> wrapPlain(String text, int maxWidth) {
        ArrayList<String> out = new ArrayList<String>();
        if (text == null || text.isBlank()) {
            out.add("");
            return out;
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String attempt;
            String string = attempt = line.isEmpty() ? w : String.valueOf(line) + " " + w;
            if (this.font.width(attempt) > maxWidth && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(w);
                continue;
            }
            line = new StringBuilder(attempt);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out.isEmpty() ? List.of("") : out;
    }

    private void startSaveMacro() {
        if (this.savePhase != SavePhase.IDLE || this.def == null) {
            return;
        }
        this.savePhase = SavePhase.VALIDATING;
        this.savePhaseTicks = 0;
        this.saveOverlayDetail = "";
        this.updateSaveButtonLabel();
    }

    private void tickSaveUi() {
        switch (this.savePhase.ordinal()) {
            case 0: {
                break;
            }
            case 1: {
                ++this.savePhaseTicks;
                if (this.savePhaseTicks < 3) {
                    return;
                }
                this.captureHeader();
                this.applyInspectorToSelection();
                this.syncDefFromGraph();
                this.def.normalize();
                String hkErr = this.validateMacroRunHotkeyUnique();
                if (hkErr != null) {
                    this.saveOverlayDetail = hkErr;
                    this.savePhase = SavePhase.FAILED;
                    this.savePhaseTicks = 0;
                    this.sendMacroChatLine(Component.literal(hkErr).withStyle(ChatFormatting.RED));
                    this.updateSaveButtonLabel();
                    return;
                }
                this.savePhase = SavePhase.PERSISTING;
                this.savePhaseTicks = 0;
                this.updateSaveButtonLabel();
                break;
            }
            case 2: {
                ++this.savePhaseTicks;
                if (this.savePhaseTicks < 2) {
                    return;
                }
                this.captureHeader();
                this.applyInspectorToSelection();
                this.syncDefFromGraph();
                this.def.normalize();
                List<String> diags = MacroGraphCompiler.collectDiagnostics(this.def);
                boolean graphOk = diags.isEmpty();
                try {
                    this.def.steps.clear();
                    MacroStorage.save(this.def);
                    this.refreshCompileDiagnostics();
                    this.layoutInspectorWidgets();
                    if (graphOk) {
                        this.saveOverlayDetail = MacroGraphCompiler.summarizePlan(this.def);
                        this.savePhase = SavePhase.DONE_OK;
                        this.clearDirty();
                        this.sendMacroChatLine(Component.literal(("Saved graph \u2192 " + this.saveOverlayDetail)).withStyle(ChatFormatting.GREEN));
                    } else {
                        this.saveOverlayDetail = diags.getFirst() + (diags.size() > 1 ? "  (see Compile panel for +" + (diags.size() - 1) + " more)" : "");
                        this.savePhase = SavePhase.DONE_WARN;
                        this.clearDirty();
                        this.sendMacroChatLine(Component.literal("Saved file; fix compile issues to run: ").withStyle(ChatFormatting.YELLOW).append(Component.literal(diags.getFirst()).withStyle(ChatFormatting.RED)));
                    }
                }
                catch (Exception e) {
                    this.savePhase = SavePhase.FAILED;
                    this.saveOverlayDetail = e.getMessage() == null ? "Unknown error" : e.getMessage();
                    this.sendMacroChatLine(Component.literal(("Save failed: " + this.saveOverlayDetail)).withStyle(ChatFormatting.RED));
                }
                this.savePhaseTicks = 0;
                this.updateSaveButtonLabel();
                break;
            }
            case 3: 
            case 4: 
            case 5: {
                ++this.savePhaseTicks;
                if (this.savePhaseTicks <= 55) break;
                this.savePhase = SavePhase.IDLE;
                this.saveOverlayDetail = "";
                this.updateSaveButtonLabel();
            }
        }
    }

    private void updateSaveButtonLabel() {
        if (this.macroSaveButton == null) {
            return;
        }
        this.macroSaveButton.active = this.savePhase == SavePhase.IDLE;
        String label = switch (this.savePhase.ordinal()) {
            case 1 -> "Checking\u2026";
            case 2 -> "Saving\u2026";
            default -> "Save";
        };
        this.macroSaveButton.setMessage(Component.literal(label));
    }

    private void sendMacroChatLine(Component message) {
        Minecraft c = Minecraft.getInstance();
        if (c.player != null) {
            c.player.sendSystemMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD).append(message));
        }
    }

    @Nullable
    private String validateMacroRunHotkeyUnique() {
        if (this.def.hotkeyKey < 0) {
            return null;
        }
        long packed = MacroDefinition.packHotkey(this.def.hotkeyKey, this.def.hotkeyMods);
        if (packed < 0L) {
            return null;
        }
        String myId = MacroStorage.filenameId(this.def.id);
        for (String other : MacroStorage.listMacroIds()) {
            if (other.equalsIgnoreCase(myId)) continue;
            try {
                MacroDefinition od = MacroStorage.load(other);
                if (od.hotkeyKey < 0 || MacroDefinition.packHotkey(od.hotkeyKey, od.hotkeyMods) != packed) continue;
                return "Another macro (" + other + ") already uses this run hotkey.";
            }
            catch (Exception exception) {
            }
        }
        return null;
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, this.width, this.height);
        context.fill(0, 0, this.width, HEADER_H, 0xCC0A0E14);
        context.fill(0, HEADER_H - 1, this.width, HEADER_H, UiTokens.ACCENT & 0x44FFFFFF | 0x88000000);
        context.text(this.font, this.title, 12, 14, UiTokens.ACCENT);
        if (this.autosaveFlashTicks > 0) {
            context.text(this.font, Component.literal("Autosaved").withStyle(ChatFormatting.DARK_GREEN),
                    this.width - 420, 14, 0xFF4ADE9A);
        }
        this.renderSidebar(context, mouseX, mouseY);
        this.renderCanvas(context, mouseX, mouseY);
        this.renderInspectorFrame(context, mouseX, mouseY);
        this.renderFooterHint(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.renderItemPickerLayer(context, mouseX, mouseY);
        this.renderContextMenu(context, mouseX, mouseY);
        this.renderSaveProgressOverlay(context, delta);
    }

    private void renderFooterHint(GuiGraphicsExtractor context) {
        if (this.height < 24) {
            return;
        }
        context.text(this.font,
                Component.literal("Drag nodes · Del delete · RMB ports · wheel zoom / palette scroll"),
                12, this.height - 14, UiTokens.TEXT_DIM);
    }

    private boolean categoryExpanded(String cat) {
        return this.paletteCategoryExpanded.getOrDefault(cat, Boolean.TRUE);
    }

    private void togglePaletteCategory(String cat) {
        this.paletteCategoryExpanded.put(cat, !this.categoryExpanded(cat));
        this.clampPaletteScroll();
    }

    private int paletteViewportTop() {
        return CANVAS_TOP + 12;
    }

    private int paletteViewportHeight() {
        return Math.max(32, this.height - this.paletteViewportTop() - 2);
    }

    private int paletteContentHeight() {
        int h = 0;
        List<MacroNodePalette.Entry> entries = MacroNodePalette.entries();
        int i = 0;
        while (i < entries.size()) {
            String cat = entries.get(i).category();
            boolean expanded = this.categoryExpanded(cat);
            h += 13;
            while (i < entries.size() && entries.get(i).category().equals(cat)) {
                if (expanded) {
                    h += 17;
                }
                ++i;
            }
        }
        return h;
    }

    private void clampPaletteScroll() {
        int max = Math.max(0, this.paletteContentHeight() - this.paletteViewportHeight());
        this.paletteScroll = Math.max(0, Math.min(this.paletteScroll, max));
    }

    private void renderSidebar(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.fill(0, CANVAS_TOP, this.sidebarW(), this.height, UiTokens.BG_PANEL);
        context.fill(this.sidebarW() - 1, CANVAS_TOP, this.sidebarW(), this.height, -11870592);
        int titleY = CANVAS_TOP + 6;
        context.text(this.font, Component.literal("Nodes"), 8, titleY, -11870592);
        int vpTop = this.paletteViewportTop();
        int vpH = this.paletteViewportHeight();
        this.clampPaletteScroll();
        int maxScroll = Math.max(0, this.paletteContentHeight() - vpH);
        context.enableScissor(0, vpTop, this.sidebarW(), vpTop + vpH);
        int labelMaxW = this.sidebarW() - 18;
        int vy = 0;
        List<MacroNodePalette.Entry> entries = MacroNodePalette.entries();
        int i = 0;
        while (i < entries.size()) {
            int headBg;
            String cat = entries.get(i).category();
            boolean expanded = this.categoryExpanded(cat);
            int sy = vpTop + vy - this.paletteScroll;
            boolean headHot = mouseX >= 4 && mouseX < this.sidebarW() - 4 && mouseY >= sy && mouseY < sy + 13;
            int n = headBg = headHot ? -1438633920 : 1141838360;
            if (sy + 13 > vpTop && sy < vpTop + vpH) {
                context.fill(4, sy, this.sidebarW() - 4, sy + 13 - 1, headBg);
                String tri = expanded ? "\u25bc " : "\u25b6 ";
                String head = this.font.plainSubstrByWidth(tri + cat, labelMaxW);
                context.text(this.font, Component.literal(head), 8, sy + 2, -11870592);
            }
            vy += 13;
            while (i < entries.size() && entries.get(i).category().equals(cat)) {
                if (expanded) {
                    int bg;
                    MacroNodePalette.Entry row = entries.get(i);
                    int rowSy = vpTop + vy - this.paletteScroll;
                    boolean hot = mouseX >= 4 && mouseX < this.sidebarW() - 4 && mouseY >= rowSy && mouseY < rowSy + 17;
                    int n2 = bg = hot ? -1437579208 : 1712328728;
                    if (rowSy + 17 > vpTop && rowSy < vpTop + vpH) {
                        context.fill(4, rowSy, this.sidebarW() - 4, rowSy + 17 - 1, bg);
                        String lab = this.font.plainSubstrByWidth(row.label(), labelMaxW);
                        context.text(this.font, Component.literal(lab), 8, rowSy + 4, -1380097);
                    }
                    vy += 17;
                }
                ++i;
            }
        }
        context.disableScissor();
        if (maxScroll > 0) {
            int trackX = this.sidebarW() - 6;
            int trackTop = vpTop + 2;
            int trackBot = vpTop + vpH - 2;
            context.fill(trackX, trackTop, trackX + 4, trackBot, 1141905448);
            int contentH = this.paletteContentHeight();
            int thumbH = Math.max(12, (int)((double)vpH * ((double)vpH / (double)Math.max(1, contentH))));
            int span = Math.max(0, trackBot - trackTop - thumbH);
            int ty = trackTop + (int)((double)span * ((double)this.paletteScroll / (double)maxScroll));
            context.fill(trackX, ty, trackX + 4, ty + thumbH, -11899184);
        }
    }

    @Nullable
    private MacroNodePalette.Entry paletteEntryAt(int mouseX, int mouseY) {
        if (mouseX < 0 || mouseX >= this.sidebarW()) {
            return null;
        }
        int vpTop = this.paletteViewportTop();
        int vpH = this.paletteViewportHeight();
        if (mouseY < vpTop || mouseY >= vpTop + vpH) {
            return null;
        }
        int ly = mouseY - vpTop + this.paletteScroll;
        int vy = 0;
        List<MacroNodePalette.Entry> entries = MacroNodePalette.entries();
        int i = 0;
        while (i < entries.size()) {
            String cat = entries.get(i).category();
            boolean expanded = this.categoryExpanded(cat);
            vy += 13;
            while (i < entries.size() && entries.get(i).category().equals(cat)) {
                MacroNodePalette.Entry row = entries.get(i);
                if (expanded) {
                    if (ly >= vy && ly < vy + 17) {
                        return row;
                    }
                    vy += 17;
                }
                ++i;
            }
        }
        return null;
    }

    private boolean tryClickPaletteCategoryHeader(double mx, double my) {
        if (mx < 4.0 || mx >= (double)(this.sidebarW() - 4)) {
            return false;
        }
        int vpTop = this.paletteViewportTop();
        int vpH = this.paletteViewportHeight();
        if (my < (double)vpTop || my >= (double)(vpTop + vpH)) {
            return false;
        }
        int ly = (int)my - vpTop + this.paletteScroll;
        int vy = 0;
        List<MacroNodePalette.Entry> entries = MacroNodePalette.entries();
        int i = 0;
        while (i < entries.size()) {
            String cat = entries.get(i).category();
            if (ly >= vy && ly < vy + 13) {
                this.togglePaletteCategory(cat);
                return true;
            }
            vy += 13;
            while (i < entries.size() && entries.get(i).category().equals(cat)) {
                if (this.categoryExpanded(cat)) {
                    vy += 17;
                }
                ++i;
            }
        }
        return false;
    }

    private void renderCanvas(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        MacroGraphNode a;
        int cl = this.canvasLeft();
        int cw = this.canvasWidth();
        int ch = this.canvasHeight();
        context.fill(cl, CANVAS_TOP, cl + cw, CANVAS_TOP + ch, -1609822702);
        context.fill(cl, CANVAS_TOP, cl + cw, CANVAS_TOP + 1, UiTokens.ACCENT_MUTED);
        int gridStep = this.canvasZoom < 0.85 ? 64 : 32;
        for (int gx = 0; gx < cw; gx += gridStep) {
            int sx = cl + gx + (int)(this.panX % (double)gridStep);
            context.verticalLine(sx, CANVAS_TOP, CANVAS_TOP + ch, 856692776);
        }
        for (int gy = 0; gy < ch; gy += gridStep) {
            int sy = CANVAS_TOP + gy + (int)(this.panY % (double)gridStep);
            context.horizontalLine(cl, cl + cw, sy, 856692776);
        }
        for (MacroGraphGroup g : this.graphGroups) {
            if (g.memberNodeIds == null || g.memberNodeIds.isEmpty()) continue;
            if (g.collapsed) {
                double minX = Double.POSITIVE_INFINITY;
                double minY = Double.POSITIVE_INFINITY;
                for (String mid : g.memberNodeIds) {
                    MacroGraphNode n = this.findNode(mid);
                    if (n == null) continue;
                    minX = Math.min(minX, n.x);
                    minY = Math.min(minY, n.y);
                }
                if (minX == Double.POSITIVE_INFINITY) continue;
                double chipTop = minY - 14.0;
                double chipBottom = chipTop + 12.0 / Math.max(0.001, this.canvasZoom);
                int sx0 = this.toScreenX(minX - 4.0);
                int sx1 = this.toScreenX(minX + 108.0 + 4.0);
                int sy0 = this.toScreenY(chipTop);
                int sy1 = this.toScreenY(chipBottom);
                int bx0 = Math.min(sx0, sx1);
                int bx1 = Math.max(sx0, sx1);
                int by0 = Math.min(sy0, sy1);
                int by1 = Math.max(sy0, sy1);
                context.fill(bx0, by0, bx1, by1, g.fillArgb);
                context.fill(bx0, by0, bx1, by0 + 1, g.borderArgb);
                context.fill(bx0, by1 - 1, bx1, by1, g.borderArgb);
                context.fill(bx0, by0, bx0 + 1, by1, g.borderArgb);
                context.fill(bx1 - 1, by0, bx1, by1, g.borderArgb);
                String lab = g.label == null || g.label.isBlank() ? "Group" : g.label;
                String shortLab = lab.length() > 18 ? lab.substring(0, 18) + "\u2026" : lab;
                context.text(this.font, Component.literal((shortLab + " \u25b6")), bx0 + 3, by0 + 2, g.borderArgb & 0xFFFFFF | 0xFF000000);
                continue;
            }
            int[] rect = this.expandedGroupScreenRect(g);
            if (rect == null) continue;
            int x0 = rect[0];
            int y0 = rect[1];
            int x1 = rect[2];
            int y1 = rect[3];
            context.fill(x0, y0, x1, y1, g.fillArgb);
            context.fill(x0, y0, x1, y0 + 1, g.borderArgb);
            context.fill(x0, y1 - 1, x1, y1, g.borderArgb);
            context.fill(x0, y0, x0 + 1, y1, g.borderArgb);
            context.fill(x1 - 1, y0, x1, y1, g.borderArgb);
            String lab = g.label == null || g.label.isBlank() ? "Group" : g.label;
            context.text(this.font, Component.literal(lab), x0 + 3, y0 + 2, g.borderArgb & 0xFFFFFF | 0xFF000000);
        }
        this.renderNodeById.clear();
        for (MacroGraphNode n : this.graphNodes) {
            this.renderNodeById.put(n.id, n);
        }
        for (MacroGraphEdge edge : this.graphEdges) {
            MacroGraphNode a2 = this.renderNodeById.get(edge.from);
            MacroGraphNode b = this.renderNodeById.get(edge.to);
            if (a2 == null || b == null) continue;
            MacroGraphGroup ca = this.collapsedGroupContaining(a2.id);
            MacroGraphGroup cb = this.collapsedGroupContaining(b.id);
            if (ca != null && ca == cb) continue;
            int x1 = this.edgeFromScreenX(a2, ca);
            int y1 = this.edgeFromScreenY(a2, ca, edge.fromSlot);
            int x2 = this.edgeToScreenX(b, cb);
            int y2 = this.edgeToScreenY(b, cb);
            int col = "loop".equalsIgnoreCase(edge.fromSlot) ? -9764890 : ("next".equalsIgnoreCase(edge.fromSlot) ? -18325 : -9789697);
            FastGuiDraw.drawLine(context, x1, y1, x2, y2, col);
        }
        if (this.marqueeActive || this.canvasMarqueeCandidate) {
            int rx0 = (int)Math.min(this.marqueeAX, this.marqueeBX);
            int ry0 = (int)Math.min(this.marqueeAY, this.marqueeBY);
            int rx1 = (int)Math.max(this.marqueeAX, this.marqueeBX);
            int ry1 = (int)Math.max(this.marqueeAY, this.marqueeBY);
            context.fill(rx0, ry0, rx1, ry1, 857751680);
            context.horizontalLine(rx0, rx1, ry0, -9789697);
            context.horizontalLine(rx0, rx1, ry1, -9789697);
            context.verticalLine(rx0, ry0, ry1, -9789697);
            context.verticalLine(rx1, ry0, ry1, -9789697);
        }
        for (MacroGraphNode n : this.graphNodes) {
            if (this.isNodeInCollapsedGroup(n)) continue;
            int sx0 = this.toScreenX(n.x);
            int sy0 = this.toScreenY(n.y);
            int sx1 = this.toScreenX(n.x + 108.0);
            int sy1 = this.toScreenY(n.y + 42.0);
            int rx0 = Math.min(sx0, sx1);
            int rx1 = Math.max(sx0, sx1);
            int ry0 = Math.min(sy0, sy1);
            int ry1 = Math.max(sy0, sy1);
            boolean sel = this.selectedNodeIds.contains(n.id);
            boolean control = MacroGraphTypes.isControlNode(n.type);
            int fill = sel ? -868595110 : (control ? -1441128408 : -1441654752);
            context.fill(rx0, ry0, rx1, ry1, fill);
            int barH = 2;
            int textPad = 4;
            context.fill(rx0, ry0, rx1, ry0 + barH, control ? -11870566 : -11870592);
            String rawTitle = control ? ("GRAPH_START".equals(n.type) ? "START" : "END") : n.type;
            int innerW = Math.max(8, rx1 - rx0 - 2 * textPad);
            String title = this.font.plainSubstrByWidth(rawTitle, innerW);
            context.text(this.font, Component.literal(title), rx0 + textPad, ry0 + textPad, -1642753);
            if (!"GRAPH_START".equals(n.type)) {
                this.fillWorldRect(context, n.x - 4.5, n.y + 21.0 - 4.5, n.x + 4.5, n.y + 21.0 + 4.5, -12198260);
            }
            if ("GRAPH_END".equals(n.type)) continue;
            if (MacroGraphTypes.isRepeatNode(n.type)) {
                int pxR;
                if (this.repeatShowsMergePort(n)) {
                    this.fillWorldRect(context, n.x + 108.0 - 4.5, n.y + 9.24 - 4.5, n.x + 108.0 + 4.5, n.y + 9.24 + 4.5, -10616888);
                    this.fillWorldRect(context, n.x + 108.0 - 4.5, n.y + 32.76 - 4.5, n.x + 108.0 + 4.5, n.y + 32.76 + 4.5, -19605);
                    pxR = Math.max(this.toScreenX(n.x + 108.0 - 4.5), this.toScreenX(n.x + 108.0 + 4.5));
                    int pyLoop = this.toScreenY(n.y + 9.24);
                    int pyNext = this.toScreenY(n.y + 32.76);
                    int tdx = 2;
                    context.text(this.font, Component.literal("Repeat"), pxR + tdx, pyLoop - 4, -5308440);
                    context.text(this.font, Component.literal("Continue"), pxR + tdx, pyNext - 4, -11096);
                    continue;
                }
                this.fillWorldRect(context, n.x + 108.0 - 4.5, n.y + 21.0 - 4.5, n.x + 108.0 + 4.5, n.y + 21.0 + 4.5, -10616888);
                pxR = Math.max(this.toScreenX(n.x + 108.0 - 4.5), this.toScreenX(n.x + 108.0 + 4.5));
                int pyC = this.toScreenY(n.y + 21.0);
                context.text(this.font, Component.literal("Repeat"), pxR + 2, pyC - 4, -5308440);
                continue;
            }
            this.fillWorldRect(context, n.x + 108.0 - 4.5, n.y + 21.0 - 4.5, n.x + 108.0 + 4.5, n.y + 21.0 + 4.5, -19605);
        }
        if (this.linkFromId != null && (a = this.findNode(this.linkFromId)) != null) {
            MacroGraphGroup lc = this.collapsedGroupContaining(this.linkFromId);
            int x1 = this.edgeFromScreenX(a, lc);
            int y1 = this.edgeFromScreenY(a, lc, this.linkFromSlot);
            FastGuiDraw.drawLine(context, x1, y1, mouseX, Math.min(mouseY, CANVAS_TOP + ch - 2), -1996499800);
        }
    }

    private int outPortScreenY(MacroGraphNode a, String fromSlot) {
        String s;
        String string = s = fromSlot == null ? "" : fromSlot.trim().toLowerCase();
        if (MacroGraphTypes.isRepeatNode(a.type)) {
            if (this.repeatShowsMergePort(a)) {
                if ("loop".equals(s)) {
                    return this.toScreenY(a.y + 9.24);
                }
                if ("next".equals(s)) {
                    return this.toScreenY(a.y + 32.76);
                }
            }
            return this.toScreenY(a.y + 21.0);
        }
        return this.toScreenY(a.y + 21.0);
    }

    private static boolean sameFromSlot(String a, String b) {
        String x = a == null || a.isBlank() ? "" : a.trim().toLowerCase();
        String y = b == null || b.isBlank() ? "" : b.trim().toLowerCase();
        return x.equals(y);
    }

    private void renderCompileStrip(GuiGraphicsExtractor context, int ix) {
        block8: {
            int w = this.inspectorW() - 10;
            int x = ix + 5;
            int y = this.compilePanelTop;
            context.fill(x, y, x + w, y + this.compilePanelHeight, 1712330792);
            context.fill(x, y, x + w, y + 1, -12886369);
            context.text(this.font, Component.literal("Compile"), x + 4, y + 4, -14217);
            int maxW = Math.max(24, w - 8);
            int ty = y + 16;
            if (this.liveCompileDiagnostics.isEmpty()) {
                String plan = this.def != null ? MacroGraphCompiler.summarizePlan(this.def) : "";
                for (String line : this.wrapPlain("OK \u2014 " + plan, maxW)) {
                    if (ty <= y + this.compilePanelHeight - 8) {
                        context.text(this.font, Component.literal(line), x + 4, ty, -10823512);
                        ty += 10;
                        continue;
                    }
                    break;
                }
            } else {
                int shown = 0;
                for (String msg : this.liveCompileDiagnostics) {
                    for (String line : this.wrapPlain(msg, maxW)) {
                        if (shown >= 9) {
                            if (ty > y + this.compilePanelHeight - 8) break block8;
                            context.text(this.font, Component.literal("\u2026"), x + 4, ty, -8747362);
                            break block8;
                        }
                        if (ty <= y + this.compilePanelHeight - 8) {
                            context.text(this.font, Component.literal(line), x + 4, ty, -25718);
                            ty += 10;
                            ++shown;
                            continue;
                        }
                        break block8;
                    }
                }
            }
        }
    }

    private void renderSaveProgressOverlay(GuiGraphicsExtractor context, float delta) {
        if (this.savePhase == SavePhase.IDLE) {
            return;
        }
        int panelW = 240;
        int panelH = this.savePhase == SavePhase.VALIDATING || this.savePhase == SavePhase.PERSISTING ? 46 : 58;
        int px = (this.width - panelW) / 2;
        int py = 6;
        context.fill(px - 8, py - 4, px + panelW + 8, py + panelH + 6, -267380696);
        context.fill(px - 8, py - 4, px + panelW + 8, py - 2, -11899184);
        String title = switch (this.savePhase.ordinal()) {
            case 1 -> "Validating macro\u2026";
            case 2 -> "Saving to disk\u2026";
            case 3 -> "Saved";
            case 4 -> "Saved (graph has issues)";
            case 5 -> "Save blocked / failed";
            default -> "";
        };
        context.centeredText(this.font, Component.literal(title), px + panelW / 2, py + 2, -1380097);
        if (this.savePhase == SavePhase.VALIDATING || this.savePhase == SavePhase.PERSISTING) {
            int bx = px + 12;
            int by = py + 20;
            int bw = panelW - 24;
            context.fill(bx, by, bx + bw, by + 6, -15392711);
            long t = System.nanoTime() / 1000000L;
            float anim = (float)(t % 880L) / 880.0f;
            int seg = Math.max(28, (int)((float)bw * 0.36f));
            int off = (int)((float)(bw - seg) * anim);
            context.fill(bx + off, by, bx + off + seg, by + 6, -10646273);
        } else {
            int ty = py + 20;
            int col = this.savePhase == SavePhase.FAILED ? -29830 : (this.savePhase == SavePhase.DONE_WARN ? -11126 : -10823512);
            String body = this.saveOverlayDetail.isEmpty() ? "\u2014" : this.saveOverlayDetail;
            for (String line : this.wrapPlain(body, panelW - 20)) {
                context.centeredText(this.font, Component.literal(line), px + panelW / 2, ty, col);
                ty += 10;
            }
        }
    }

    private void renderInspectorFrame(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int ix = this.width - this.inspectorW();
        context.fill(ix, CANVAS_TOP, this.width, this.height, UiTokens.BG_PANEL);
        context.fill(ix, CANVAS_TOP, ix + 1, this.height, -11870592);
        context.text(this.font, Component.literal("Inspector"), ix + 8, CANVAS_TOP + 6, UiTokens.ACCENT);
        String hint = this.inspectorOverlayHint();
        if (!hint.isEmpty()) {
            context.text(this.font, Component.literal(hint), ix + 8, CANVAS_TOP + 18, UiTokens.TEXT_DIM);
        }
        this.renderCompileStrip(context, ix);
        int runKeyY = this.compilePanelTop + this.compilePanelHeight + 4;
        context.text(this.font, Component.literal("Run key: DupeClient → Macros panel"), ix + 8, runKeyY, UiTokens.TEXT_DIM);
        context.text(this.font, Component.literal("Macro library"), ix + 8, this.libraryListTop, UiTokens.ACCENT);
        context.text(this.font, Component.literal("Click · dbl-click · RMB del"), ix + 8, this.libraryListTop + 10, UiTokens.TEXT_DIM);
        int rowY = this.libraryListTop + 22;
        String currentId = MacroStorage.filenameId(this.def.id);
        for (int i = 0; i < this.libraryVisibleRows; ++i) {
            int idx = this.libraryScroll + i;
            int ry = rowY + i * (LIB_ROW_H + LIB_ROW_GAP);
            if (idx >= this.libraryIds.size()) {
                break;
            }
            String id = this.libraryIds.get(idx);
            boolean current = id.equalsIgnoreCase(currentId);
            boolean hot = mouseX >= ix + 4 && mouseX < this.width - 4 && mouseY >= ry && mouseY < ry + LIB_ROW_H;
            com.dupeclient.client.gui.modern.UiComponents.drawListRowBack(context, ix + 4, ry, this.inspectorW() - 12, LIB_ROW_H, current);
            if (hot && !current) {
                context.fill(ix + 5, ry + 1, this.width - 5, ry + LIB_ROW_H - 1, 0x18FFFFFF);
            }
            String shown = this.font.plainSubstrByWidth(id, this.inspectorW() - 28);
            context.text(this.font, Component.literal(shown), ix + 10, ry + 3,
                    current ? UiTokens.MINT_300 : UiTokens.TEXT);
        }
    }

    private static void toast(String message) {
        Minecraft c = Minecraft.getInstance();
        if (c.player != null) {
            c.player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
        }
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        OutPortHit outHit;
        int libIdx;
        double wy;
        double mx = click.x();
        double my = click.y();
        if (this.isRegistryPickerOpen() && click.button() == 0 && this.registryPickerHandleClick(mx, my)) {
            return true;
        }
        if (this.contextMenuOpen) {
            if (this.contextMenuContains(mx, my) && click.button() == 0) {
                int row = ((int)my - this.contextMenuY - 2) / 14;
                if (row >= 0 && row < this.contextMenuEntries.size()) {
                    this.contextMenuEntries.get(row).action().run();
                }
                this.closeContextMenu();
                return true;
            }
            this.closeContextMenu();
            if (click.button() == 0) {
                return true;
            }
        }
        if (click.button() == 0 && mx < (double)this.sidebarW() && my >= (double)CANVAS_TOP && this.tryClickPaletteCategoryHeader(mx, my)) {
            return true;
        }
        if (doubleClick && click.button() == 0 && this.inCanvas(mx, my)) {
            this.setFocused(null);
            double wx = this.toWorldX(mx);
            wy = this.toWorldY(my);
            MacroGraphGroup dblCollapsed = this.collapsedGroupHitScreen((int)mx, (int)my);
            if (dblCollapsed != null) {
                dblCollapsed.collapsed = false;
                this.syncDefFromGraph();
                this.refreshCompileDiagnostics();
                return true;
            }
            MacroGraphNode hit = this.nodeAtWorld(wx, wy);
            if (hit != null && MacroGraphTypes.isRepeatNode(hit.type)) {
                hit.repeatShowNextPort = !hit.repeatShowNextPort;
                MacroEditorScreen.toast(hit.repeatShowNextPort ? "Repeat: orange Continue port shown." : "Repeat: simple mode \u2014 mint Repeat wire only; Continue at End inferred when possible.");
                this.syncDefFromGraph();
                return true;
            }
        }
        if (click.button() == 0 && this.tryClickLibrary(mx, my, doubleClick)) {
            return true;
        }
        if (click.button() == 1 && (libIdx = this.libraryRowIndexAt(mx, my)) >= 0) {
            this.setFocused(null);
            this.confirmDeleteMacroFromLibrary(this.libraryIds.get(libIdx));
            return true;
        }
        if (click.button() == 1 && this.inCanvas(mx, my)) {
            this.setFocused(null);
            double wx = this.toWorldX(mx);
            wy = this.toWorldY(my);
            String inId = this.nodeIdForInPort(wx, wy);
            if (inId != null) {
                this.graphEdges.removeIf(e -> e.to.equals(inId));
                this.linkFromId = null;
                this.syncDefFromGraph();
                return true;
            }
            outHit = this.pickOutPort(wx, wy);
            if (outHit != null) {
                this.graphEdges.removeIf(e -> e.from.equals(outHit.nodeId()) && MacroEditorScreen.sameFromSlot(e.fromSlot, outHit.fromSlot()));
                this.linkFromId = null;
                this.linkFromSlot = "";
                this.syncDefFromGraph();
                return true;
            }
            MacroGraphGroup chip = this.collapsedGroupHitScreen((int)mx, (int)my);
            if (chip != null) {
                MacroGraphGroup gChip = chip;
                this.openContextMenu((int)mx, (int)my, List.of(new ContextMenuEntry("Expand group", () -> {
                    gChip.collapsed = false;
                    this.syncDefFromGraph();
                    this.refreshCompileDiagnostics();
                }), new ContextMenuEntry("Rename & colors\u2026", () -> this.openGroupStyleEditor(gChip)), new ContextMenuEntry("Copy group", () -> this.copyGroupToClipboard(gChip)), new ContextMenuEntry("Delete group\u2026", () -> this.confirmDeleteGroup(gChip))));
                return true;
            }
            MacroGraphGroup gTitleRmb = this.expandedGroupTitleHitScreen((int)mx, (int)my);
            if (gTitleRmb != null) {
                this.openGroupChromeContextMenu((int)mx, (int)my, gTitleRmb);
                return true;
            }
            MacroGraphNode ctxHit = this.nodeAtWorld(wx, wy);
            if (ctxHit != null) {
                MacroGraphGroup ng;
                MacroGraphNode h = ctxHit;
                ArrayList<ContextMenuEntry> items = new ArrayList<ContextMenuEntry>();
                items.add(new ContextMenuEntry("Copy", () -> this.copyHitOrSelection(h)));
                if (!MacroGraphTypes.isControlNode(h.type)) {
                    items.add(new ContextMenuEntry("Delete", () -> {
                        if (this.selectedNodeIds.contains(h.id)) {
                            this.deleteSelected();
                        } else {
                            this.deleteGraphNodeById(h.id);
                        }
                    }));
                }
                if (!this.selectedNodeIds.isEmpty()) {
                    items.add(new ContextMenuEntry("Remove selected from groups", this::removeSelectionFromAllGroups));
                }
                if (this.selectedNodeIds.isEmpty() || !this.selectedNodeIds.contains(h.id)) {
                    items.add(new ContextMenuEntry("Remove this node from groups", () -> {
                        this.removeNodeFromAllGroups(h.id);
                        this.graphGroups.removeIf(g -> g.memberNodeIds == null || g.memberNodeIds.isEmpty());
                        this.syncDefFromGraph();
                        this.loadInspectorFromSelection();
                    }));
                }
                if ((ng = this.nonCollapsedGroupContaining(h.id)) != null) {
                    MacroGraphGroup gCollapse = ng;
                    items.add(new ContextMenuEntry("Collapse group", () -> {
                        gCollapse.collapsed = true;
                        this.pruneSelectionForHiddenNodes();
                        this.syncDefFromGraph();
                        this.refreshCompileDiagnostics();
                        this.loadInspectorFromSelection();
                    }));
                }
                this.openContextMenu((int)mx, (int)my, items);
                return true;
            }
            ArrayList<ContextMenuEntry> emptyItems = new ArrayList<ContextMenuEntry>();
            emptyItems.add(new ContextMenuEntry("Paste", this::pasteFromClipboard));
            if (!this.selectedNodeIds.isEmpty()) {
                emptyItems.add(new ContextMenuEntry("Remove selected from groups", this::removeSelectionFromAllGroups));
            }
            emptyItems.add(new ContextMenuEntry("New group from selection", this::newGroupFromSelection));
            this.openContextMenu((int)mx, (int)my, emptyItems);
            return true;
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        MacroNodePalette.Entry pal = this.paletteEntryAt((int)mx, (int)my);
        if (pal != null && click.button() == 0) {
            this.paletteDragEntry = pal;
            return true;
        }
        if (click.button() == 2 && this.inCanvas(mx, my)) {
            this.panning = true;
            this.panGrabMx = mx;
            this.panGrabMy = my;
            this.panGrabPx = this.panX;
            this.panGrabPy = this.panY;
            return true;
        }
        if (click.button() == 0 && this.inCanvas(mx, my)) {
            this.setFocused(null);
            double wx = this.toWorldX(mx);
            double wy2 = this.toWorldY(my);
            outHit = this.pickOutPort(wx, wy2);
            if (outHit != null) {
                this.linkFromId = outHit.nodeId();
                this.linkFromSlot = outHit.fromSlot();
                return true;
            }
            String inId = this.nodeIdForInPort(wx, wy2);
            if (inId != null && this.linkFromId != null && !this.linkFromId.equals(inId)) {
                this.graphEdges.removeIf(e -> e.from.equals(this.linkFromId) && MacroEditorScreen.sameFromSlot(e.fromSlot, this.linkFromSlot));
                MacroGraphEdge e2 = new MacroGraphEdge();
                e2.from = this.linkFromId;
                e2.to = inId;
                MacroGraphNode src = this.findNode(this.linkFromId);
                e2.fromSlot = src != null && MacroGraphTypes.isRepeatNode(src.type) ? this.linkFromSlot : "";
                this.graphEdges.add(e2);
                this.linkFromId = null;
                this.linkFromSlot = "";
                return true;
            }
            MacroGraphGroup chipDrag = this.collapsedGroupHitScreen((int)mx, (int)my);
            if (chipDrag != null && chipDrag.memberNodeIds != null && !chipDrag.memberNodeIds.isEmpty()) {
                this.canvasMarqueeCandidate = false;
                this.marqueeActive = false;
                this.dragActiveCollapsedGroup = chipDrag;
                if (MacroEditorScreen.isMultiSelectModifierClick(click)) {
                    boolean allIn = chipDrag.memberNodeIds.stream().filter(Objects::nonNull).allMatch(this.selectedNodeIds::contains);
                    if (allIn) {
                        for (String id : chipDrag.memberNodeIds) {
                            if (id == null) continue;
                            this.selectedNodeIds.remove(id);
                        }
                    } else {
                        for (String id : chipDrag.memberNodeIds) {
                            if (id == null) continue;
                            this.selectedNodeIds.add(id);
                        }
                    }
                } else {
                    this.selectedNodeIds.clear();
                    this.selectedNodeIds.addAll(chipDrag.memberNodeIds);
                }
                String dragIdChip = null;
                for (String id : chipDrag.memberNodeIds) {
                    if (id == null || this.findNode(id) == null) continue;
                    dragIdChip = id;
                    break;
                }
                if (dragIdChip == null) {
                    this.dragActiveCollapsedGroup = null;
                    this.loadInspectorFromSelection();
                    return true;
                }
                this.draggingNodeId = dragIdChip;
                this.dragWorldAnchorX = wx;
                this.dragWorldAnchorY = wy2;
                this.dragNodeOrigins.clear();
                for (String id : this.selectedNodeIds) {
                    MacroGraphNode nn = this.findNode(id);
                    if (nn == null) continue;
                    this.dragNodeOrigins.put(id, new double[]{nn.x, nn.y});
                }
                this.loadInspectorFromSelection();
                return true;
            }
            MacroGraphGroup gTitleLmb = this.expandedGroupTitleHitScreen((int)mx, (int)my);
            if (gTitleLmb != null && gTitleLmb.memberNodeIds != null && !gTitleLmb.memberNodeIds.isEmpty()) {
                this.canvasMarqueeCandidate = false;
                this.marqueeActive = false;
                this.dragActiveCollapsedGroup = null;
                if (MacroEditorScreen.isMultiSelectModifierClick(click)) {
                    boolean allIn = gTitleLmb.memberNodeIds.stream().filter(Objects::nonNull).allMatch(this.selectedNodeIds::contains);
                    if (allIn) {
                        for (String id : gTitleLmb.memberNodeIds) {
                            if (id == null) continue;
                            this.selectedNodeIds.remove(id);
                        }
                    } else {
                        for (String id : gTitleLmb.memberNodeIds) {
                            if (id == null) continue;
                            this.selectedNodeIds.add(id);
                        }
                    }
                } else {
                    this.selectedNodeIds.clear();
                    this.selectedNodeIds.addAll(gTitleLmb.memberNodeIds);
                }
                String dragId = null;
                for (String id : gTitleLmb.memberNodeIds) {
                    if (id == null || this.findNode(id) == null) continue;
                    dragId = id;
                    break;
                }
                if (dragId == null) {
                    this.loadInspectorFromSelection();
                    return true;
                }
                this.draggingNodeId = dragId;
                this.dragWorldAnchorX = wx;
                this.dragWorldAnchorY = wy2;
                this.dragNodeOrigins.clear();
                for (String id : this.selectedNodeIds) {
                    MacroGraphNode nn = this.findNode(id);
                    if (nn == null) continue;
                    this.dragNodeOrigins.put(id, new double[]{nn.x, nn.y});
                }
                this.loadInspectorFromSelection();
                return true;
            }
            MacroGraphNode hit = this.nodeAtWorld(wx, wy2);
            if (hit != null) {
                this.canvasMarqueeCandidate = false;
                this.marqueeActive = false;
                this.dragActiveCollapsedGroup = null;
                if (MacroEditorScreen.isMultiSelectModifierClick(click)) {
                    if (!this.selectedNodeIds.add(hit.id)) {
                        this.selectedNodeIds.remove(hit.id);
                    }
                } else if (!this.selectedNodeIds.contains(hit.id)) {
                    this.selectedNodeIds.clear();
                    this.selectedNodeIds.add(hit.id);
                }
                this.draggingNodeId = hit.id;
                this.dragWorldAnchorX = wx;
                this.dragWorldAnchorY = wy2;
                this.dragNodeOrigins.clear();
                for (String id : this.selectedNodeIds) {
                    MacroGraphNode nn = this.findNode(id);
                    if (nn == null) continue;
                    this.dragNodeOrigins.put(id, new double[]{nn.x, nn.y});
                }
                this.loadInspectorFromSelection();
                return true;
            }
            this.canvasMarqueeCandidate = true;
            this.marqueeActive = false;
            this.marqueeAX = this.marqueeBX = mx;
            this.marqueeAY = this.marqueeBY = my;
            this.linkFromId = null;
            this.linkFromSlot = "";
            this.loadInspectorFromSelection();
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 2) {
            this.panning = false;
        }
        if (click.button() == 0 && this.paletteDragEntry != null) {
            if (this.inCanvas(click.x(), click.y())) {
                this.addNode(this.paletteDragEntry, this.toWorldX(click.x()), this.toWorldY(click.y()));
            }
            this.paletteDragEntry = null;
            return true;
        }
        if (click.button() == 0 && (this.canvasMarqueeCandidate || this.marqueeActive)) {
            if (this.marqueeActive) {
                int m = click.buttonInfo().modifiers();
                boolean merge = (m & 0xB) != 0;
                this.finalizeMarqueeSelection(merge);
            } else {
                this.selectedNodeIds.clear();
            }
            this.canvasMarqueeCandidate = false;
            this.marqueeActive = false;
            this.loadInspectorFromSelection();
        }
        this.draggingNodeId = null;
        this.dragActiveCollapsedGroup = null;
        return super.mouseReleased(click);
    }

    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (this.panning && click.button() == 2) {
            this.panX = this.panGrabPx + (click.x() - this.panGrabMx);
            this.panY = this.panGrabPy + (click.y() - this.panGrabMy);
            return true;
        }
        if (click.button() == 0 && this.canvasMarqueeCandidate) {
            this.marqueeBX = click.x();
            this.marqueeBY = click.y();
            if (Math.hypot(this.marqueeBX - this.marqueeAX, this.marqueeBY - this.marqueeAY) > 4.0) {
                this.marqueeActive = true;
            }
            return true;
        }
        if (this.draggingNodeId != null && click.button() == 0) {
            double wx = this.toWorldX(click.x());
            double wy = this.toWorldY(click.y());
            double dwx = wx - this.dragWorldAnchorX;
            double dwy = wy - this.dragWorldAnchorY;
            for (Map.Entry<String, double[]> en : this.dragNodeOrigins.entrySet()) {
                boolean inDraggedCollapsed;
                MacroGraphNode nn = this.findNode(en.getKey());
                if (nn == null) continue;
                boolean bl = inDraggedCollapsed = this.dragActiveCollapsedGroup != null && this.dragActiveCollapsedGroup.memberNodeIds != null && this.dragActiveCollapsedGroup.memberNodeIds.contains(nn.id);
                if (this.isNodeInCollapsedGroup(nn) && !inDraggedCollapsed) continue;
                nn.x = en.getValue()[0] + dwx;
                nn.y = en.getValue()[1] + dwy;
            }
            this.syncDefFromGraph();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= 0.0 && mouseX < (double)this.sidebarW() && mouseY >= (double)this.paletteViewportTop() && mouseY < (double)(this.paletteViewportTop() + this.paletteViewportHeight())) {
            int max = Math.max(0, this.paletteContentHeight() - this.paletteViewportHeight());
            int notch = (int)Math.signum(verticalAmount);
            int mag = Math.max(1, (int)Math.ceil(Math.abs(verticalAmount)));
            int delta = notch * mag * 4;
            this.paletteScroll = Math.max(0, Math.min(max, this.paletteScroll - delta));
            return true;
        }
        if (this.isRegistryPickerOpen() && mouseX >= (double)this.itemPickerPx && mouseX < (double)(this.itemPickerPx + this.itemPickerPw) && mouseY >= (double)this.itemPickerPy && mouseY < (double)(this.itemPickerPy + this.itemPickerPh)) {
            List<String> filtered = this.filteredPickerRows();
            int rowH = 11;
            int rows = Math.max(1, this.itemPickerListHeight / rowH);
            int maxScroll = Math.max(0, filtered.size() - rows);
            int delta = (int)Math.signum(verticalAmount) * Math.max(1, (int)Math.ceil(Math.abs(verticalAmount)));
            this.registryPickerScroll = Math.max(0, Math.min(maxScroll, this.registryPickerScroll - delta));
            return true;
        }
        if (this.inLibraryPanel(mouseX, mouseY) && !this.libraryIds.isEmpty()) {
            int maxScroll = Math.max(0, this.libraryIds.size() - this.libraryVisibleRows);
            int delta = (int)Math.signum(verticalAmount) * Math.max(1, (int)Math.ceil(Math.abs(verticalAmount)));
            this.libraryScroll = Math.max(0, Math.min(maxScroll, this.libraryScroll - delta));
            return true;
        }
        if (this.inCanvas(mouseX, mouseY)) {
            double oldZoom = this.canvasZoom;
            this.canvasZoom = Mth.clamp((double)(this.canvasZoom * (verticalAmount > 0.0 ? 1.1 : 0.91)), (double)0.25, (double)2.5);
            double wx = (mouseX - (double)this.canvasLeft()) / oldZoom - this.panX;
            double wy = (mouseY - (double)CANVAS_TOP) / oldZoom - this.panY;
            this.panX = (mouseX - (double)this.canvasLeft()) / this.canvasZoom - wx;
            this.panY = (mouseY - (double)CANVAS_TOP) / this.canvasZoom - wy;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean keyPressed(KeyEvent keyInput) {
        if (this.capturingPressKey) {
            this.capturingPressKey = false;
            MacroGraphNode n = this.soleSelectedId() == null ? null : this.findNode(this.soleSelectedId());
            if (n != null && MacroStepType.PRESS_BUTTON.name().equals(MacroEditorScreen.trimType(n.type))) {
                if (keyInput.key() != GLFW.GLFW_KEY_ESCAPE) {
                    n.pressKeyCode = keyInput.key();
                    n.pressKeyModifiers = keyInput.modifiers();
                    this.syncDefFromGraph();
                }
            }
            this.loadInspectorFromSelection();
            return true;
        }
        boolean ctrl;
        if (keyInput.key() == 256 && this.isRegistryPickerOpen()) {
            this.closeRegistryPicker();
            return true;
        }
        if (keyInput.key() == 256 && this.contextMenuOpen) {
            this.closeContextMenu();
            return true;
        }
        int mods = keyInput.modifiers();
        boolean bl = ctrl = (mods & 2) != 0;
        if (this.graphKeyboardShortcutsAllowed()) {
            if (ctrl && keyInput.key() == 67) {
                this.copySelectionToClipboard();
                return true;
            }
            if (ctrl && keyInput.key() == 86) {
                this.pasteFromClipboard();
                return true;
            }
            if (keyInput.key() == 261 || keyInput.key() == 259) {
                this.deleteSelected();
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    private void copySelectionToClipboard() {
        if (this.selectedNodeIds.isEmpty()) {
            MacroEditorScreen.toast("Select a node to copy.");
            return;
        }
        HashSet<String> ids = new HashSet<String>(this.selectedNodeIds);
        ArrayList<MacroGraphNode> nodes = new ArrayList<MacroGraphNode>();
        ArrayList<MacroGraphEdge> edges = new ArrayList<MacroGraphEdge>();
        for (MacroGraphNode macroGraphNode : this.graphNodes) {
            if (!ids.contains(macroGraphNode.id)) continue;
            nodes.add(macroGraphNode);
        }
        for (MacroGraphEdge macroGraphEdge : this.graphEdges) {
            if (macroGraphEdge == null || !ids.contains(macroGraphEdge.from) || !ids.contains(macroGraphEdge.to)) continue;
            edges.add(macroGraphEdge);
        }
        ArrayList<MacroGraphGroup> groups = new ArrayList<MacroGraphGroup>();
        for (MacroGraphGroup g : this.graphGroups) {
            if (g.memberNodeIds == null || g.memberNodeIds.isEmpty() || !ids.containsAll(g.memberNodeIds)) continue;
            groups.add(g);
        }
        MacroGraphClipboard macroGraphClipboard = new MacroGraphClipboard();
        macroGraphClipboard.nodes = nodes;
        macroGraphClipboard.edges = edges;
        macroGraphClipboard.groups = groups;
        if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
            this.minecraft.keyboardHandler.setClipboard(MacroStorage.toJson(macroGraphClipboard));
        }
        String extra = groups.isEmpty() ? "" : " +" + groups.size() + " group(s)";
        MacroEditorScreen.toast("Copied " + nodes.size() + " node(s)" + extra + ".");
    }

    private void pasteFromClipboard() {
        MacroGraphClipboard clip;
        if (this.minecraft == null || this.minecraft.keyboardHandler == null) {
            return;
        }
        String raw = this.minecraft.keyboardHandler.getClipboard();
        if (raw == null || raw.isBlank()) {
            MacroEditorScreen.toast("Clipboard empty.");
            return;
        }
        try {
            clip = MacroStorage.fromJson(raw, MacroGraphClipboard.class);
        }
        catch (Exception e) {
            MacroEditorScreen.toast("Clipboard is not macro node JSON.");
            return;
        }
        if (clip == null || clip.nodes == null || clip.nodes.isEmpty()) {
            MacroEditorScreen.toast("Nothing to paste.");
            return;
        }
        HashMap<String, String> idMap = new HashMap<String, String>();
        double ox = 28.0;
        double oy = 28.0;
        for (MacroGraphNode n : clip.nodes) {
            if (n == null) continue;
            String oldId = n.id;
            MacroGraphNode c = new MacroGraphNode();
            c.id = "n" + UUID.randomUUID().toString().substring(0, 8);
            c.type = n.type;
            c.category = n.category;
            c.x = n.x + ox;
            c.y = n.y + oy;
            c.ticks = n.ticks;
            c.text = n.text;
            c.moveForwardMeasure = n.moveForwardMeasure;
            c.moveForwardBlocks = n.moveForwardBlocks;
            c.walkFacing = n.walkFacing;
            c.moveAuxHoldKeyId = n.moveAuxHoldKeyId;
            c.moveAuxHoldKey2Id = n.moveAuxHoldKey2Id;
            c.repeatShowNextPort = n.repeatShowNextPort;
            c.guiItemMode = n.guiItemMode;
            c.guiItemId = n.guiItemId;
            c.guiItemAnyItem = n.guiItemAnyItem;
            c.guiItemCount = n.guiItemCount;
            c.guiItemAmountAll = n.guiItemAmountAll;
            c.guiItemDelayTicks = n.guiItemDelayTicks;
            c.blockPreset = n.blockPreset;
            c.blockCustomId = n.blockCustomId;
            c.blockSearchRadius = n.blockSearchRadius;
            c.blockNavigateMaxTicks = n.blockNavigateMaxTicks;
            c.entityTypeId = n.entityTypeId == null ? "" : n.entityTypeId;
            c.hotbarSlot = n.hotbarSlot;
            c.holdKeyId = n.holdKeyId;
            c.dropFullStack = n.dropFullStack;
            idMap.put(oldId, c.id);
            this.graphNodes.add(c);
        }
        if (clip.edges != null) {
            for (MacroGraphEdge e : clip.edges) {
                if (e == null || e.from == null || e.to == null) continue;
                String nf = idMap.get(e.from);
                String nt = idMap.get(e.to);
                if (nf == null || nt == null) continue;
                MacroGraphEdge ne = new MacroGraphEdge();
                ne.from = nf;
                ne.to = nt;
                ne.fromSlot = e.fromSlot == null ? "" : e.fromSlot;
                this.graphEdges.add(ne);
            }
        }
        if (clip.groups != null) {
            for (MacroGraphGroup g : clip.groups) {
                if (g == null || g.memberNodeIds == null) continue;
                MacroGraphGroup ng = new MacroGraphGroup();
                ng.id = "g" + UUID.randomUUID().toString().substring(0, 8);
                ng.label = g.label;
                ng.borderArgb = g.borderArgb;
                ng.fillArgb = g.fillArgb;
                ng.collapsed = g.collapsed;
                for (String mid : g.memberNodeIds) {
                    String nid = idMap.get(mid);
                    if (nid == null) continue;
                    ng.memberNodeIds.add(nid);
                }
                if (ng.memberNodeIds.isEmpty()) continue;
                this.graphGroups.add(ng);
            }
        }
        this.selectedNodeIds.clear();
        this.selectedNodeIds.addAll(idMap.values());
        this.syncDefFromGraph();
        this.refreshCompileDiagnostics();
        this.loadInspectorFromSelection();
        int gCount = clip.groups == null ? 0 : (int)clip.groups.stream().filter(x -> x != null && x.memberNodeIds != null && !x.memberNodeIds.isEmpty()).count();
        MacroEditorScreen.toast("Pasted " + idMap.size() + " node(s)" + (gCount == 0 ? "" : " +" + gCount + " group(s)") + ".");
    }

    private void tryClose() {
        this.closeRegistryPicker();
        this.captureHeader();
        this.applyInspectorToSelection();
        if (!this.editorDirty) {
            this.exitToParent();
            return;
        }
        Minecraft c = this.minecraft;
        if (c == null) {
            return;
        }
        MacroEditorScreen self = this;
        c.setScreen((Screen)new ConfirmScreen(confirmed -> {
            if (confirmed) {
                try {
                    self.captureHeader();
                    self.applyInspectorToSelection();
                    self.syncDefFromGraphWithoutDirty();
                    self.def.normalize();
                    self.def.steps.clear();
                    MacroStorage.save(self.def);
                    self.clearDirty();
                    c.setScreen(self.parent);
                }
                catch (Exception e) {
                    c.setScreen((Screen)self);
                    MacroEditorScreen.toast(e.getMessage() == null ? "Save failed" : e.getMessage());
                }
            } else {
                self.clearDirty();
                c.setScreen(self.parent);
            }
        }, Component.literal("Save macro?"), Component.literal("You have unsaved changes. Yes writes to disk; No discards.")));
    }

    private void exitToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public void onClose() {
        this.tryClose();
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static enum RegistryPickerKind {
        NONE,
        ITEM_GUI,
        BLOCK_REGISTRY,
        ITEM_HOTBAR_SLOT,
        ENTITY_TYPE_ID;

    }

    private static enum SavePhase {
        IDLE,
        VALIDATING,
        PERSISTING,
        DONE_OK,
        DONE_WARN,
        FAILED;

    }

    private record ContextMenuEntry(String label, Runnable action) {
    }

    private final class MacroGroupStyleEditScreen
    extends Screen {
        private final MacroGraphGroup target;
        private StylishTextFieldWidget labelField;
        private StylishTextFieldWidget borderField;
        private StylishTextFieldWidget fillField;

        MacroGroupStyleEditScreen(MacroGraphGroup target) {
            super(Component.literal("Group style"));
            this.target = target;
        }

        protected void init() {
            int cx = this.width / 2 - 100;
            int y = 72;
            this.labelField = StylishTextFieldWidget.create(MacroEditorScreen.this.font, cx, y, 200, 20, Component.literal("Label"));
            this.labelField.setMaxLength(64);
            this.labelField.setValue(this.target.label == null ? "" : this.target.label);
            this.addRenderableWidget(this.labelField);
            this.borderField = StylishTextFieldWidget.create(MacroEditorScreen.this.font, cx, y += 28, 200, 20, Component.literal("Border ARGB"));
            this.borderField.setMaxLength(12);
            this.borderField.setValue(String.format("0x%08X", this.target.borderArgb));
            this.addRenderableWidget(this.borderField);
            this.fillField = StylishTextFieldWidget.create(MacroEditorScreen.this.font, cx, y += 28, 200, 20, Component.literal("Fill ARGB"));
            this.fillField.setMaxLength(12);
            this.fillField.setValue(String.format("0x%08X", this.target.fillArgb));
            this.addRenderableWidget(this.fillField);
            this.addRenderableWidget(new StylishButtonWidget(cx, y += 36, 96, 20, Component.literal("Done"), this::applyAndClose));
            this.addRenderableWidget(new StylishButtonWidget(cx + 104, y, 96, 20, Component.literal("Cancel"), () -> {
                if (MacroEditorScreen.this.minecraft != null) {
                    MacroEditorScreen.this.minecraft.setScreen((Screen)MacroEditorScreen.this);
                }
            }));
        }

        private void applyAndClose() {
            try {
                int b = MacroEditorScreen.parseArgbHex(this.borderField.getValue());
                int f = MacroEditorScreen.parseArgbHex(this.fillField.getValue());
                MacroEditorScreen.this.applyGroupStyleFromDialog(this.target, this.labelField.getValue(), b, f);
                if (MacroEditorScreen.this.minecraft != null) {
                    MacroEditorScreen.this.minecraft.setScreen((Screen)MacroEditorScreen.this);
                }
            }
            catch (Exception e) {
                MacroEditorScreen.toast("Use hex like 0xFF4A6ED0 for colors.");
            }
        }

        public boolean keyPressed(KeyEvent keyInput) {
            if (keyInput.key() == 256) {
                if (MacroEditorScreen.this.minecraft != null) {
                    MacroEditorScreen.this.minecraft.setScreen((Screen)MacroEditorScreen.this);
                }
                return true;
            }
            return super.keyPressed(keyInput);
        }

        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            UiDraw.fillMidnightBackground(context, this.width, this.height);
            context.centeredText(this.font, this.title, this.width / 2, 48, -1380097);
            context.centeredText(this.font, Component.literal("ARGB: alpha + RGB (e.g. fill 0x40204060)"), this.width / 2, 62, -8747362);
            super.extractRenderState(context, mouseX, mouseY, delta);
        }

        public boolean isPauseScreen() {
            return false;
        }
    }

    private record OutPortHit(String nodeId, String fromSlot) {
    }

    private static enum InspectorMode {
        EMPTY,
        CONTROL,
        CHAT,
        WALK,
        WAIT_OR_STUB,
        REPEAT,
        TURN,
        UTILITY_NO_FIELDS,
        HOTBAR_USE,
        HOTBAR_SELECT,
        GUI_ITEM,
        BLOCK_INTERACT,
        KEY_HOLD,
        LOOK_PITCH,
        DROP_ITEM,
        LOOK_GATE_BLOCK,
        LOOK_GATE_ENTITY,
        FABRICATOR,
        CLICK_SLOT,
        PRESS_BUTTON;

    }
}

