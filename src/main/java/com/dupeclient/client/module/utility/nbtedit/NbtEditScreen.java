package com.dupeclient.client.module.utility.nbtedit;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.DupeClientUtilityScreen;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.IntegerStepperWidget;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishSearchableDropdownWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class NbtEditScreen extends Screen implements DupeClientUtilityScreen {
    private enum Tab {
        GENERAL,
        ENCHANTS,
        LORE,
        RAW
    }

    private record EnchantRow(String enchantId, int level) {
    }

    private static final int PAD = 16;
    private static final int INNER_X = PAD + 4;
    private static final int FIELD_H = 22;
    private static final int LABEL_LEAD = 12;
    private static final int LABEL_GAP = 5;
    private static final int ROW_GAP = 14;
    private static final int ROW_H = LABEL_LEAD + LABEL_GAP + FIELD_H + ROW_GAP;
    private static final int FOOTER_H = 40;
    private static final int GAP = 8;
    private static final int CMD_ITEM_H = FIELD_H + 6;
    private static final int CMD_TOOLBAR_H = FIELD_H + 6;

    private int contentY;
    private int cmdSectionTop;

    private final Screen parent;
    private ItemStack workingStack;
    private Tab tab = Tab.GENERAL;
    private String status = "";
    private List<String> enchantmentIds = List.of();

    private IntegerStepperWidget countStepper;
    private StylishTextFieldWidget nameField;
    private StylishTextFieldWidget itemNameField;
    private IntegerStepperWidget damageStepper;
    private IntegerStepperWidget repairStepper;
    private boolean unbreakable;
    private boolean glintOverride;
    private boolean glintSet;

    private final List<Float> cmdFloats = new ArrayList<>();
    private final List<Boolean> cmdFlags = new ArrayList<>();
    private final List<Integer> cmdColors = new ArrayList<>();
    private final List<String> cmdStrings = new ArrayList<>();
    private final List<StylishTextFieldWidget> cmdFloatFields = new ArrayList<>();
    private final List<IntegerStepperWidget> cmdColorSteppers = new ArrayList<>();
    private final List<StylishTextFieldWidget> cmdStringFields = new ArrayList<>();

    private final List<EnchantRow> enchantRows = new ArrayList<>();
    private final List<StylishSearchableDropdownWidget> enchantDropdowns = new ArrayList<>();
    private final List<IntegerStepperWidget> enchantLevelSteppers = new ArrayList<>();

    private final List<String> loreLines = new ArrayList<>();
    private final List<StylishTextFieldWidget> loreFields = new ArrayList<>();

    private SnbtTextAreaWidget rawEditor;
    private int enchantScroll;
    private int loreScroll;

    public NbtEditScreen(Screen parent, ItemStack stack) {
        super(Component.literal("NBT Editor"));
        this.parent = parent;
        this.workingStack = stack.copy();
    }

    @Override
    protected void init() {
        try {
            initContent();
        } catch (Exception ex) {
            DupeClient.LOGGER.error("Failed to init NBT editor", ex);
            status = "Editor init failed: " + ex.getMessage();
        }
    }

    private void initContent() {
        syncActiveTabFields();
        clearWidgets();
        enchantDropdowns.clear();
        enchantLevelSteppers.clear();
        loreFields.clear();
        cmdFloatFields.clear();
        cmdColorSteppers.clear();
        cmdStringFields.clear();
        enchantmentIds = loadEnchantmentIds();

        int tabY = PAD + 34;
        addTabButton(PAD, tabY, 78, 24, "General", Tab.GENERAL);
        addTabButton(PAD + 82, tabY, 78, 24, "Enchants", Tab.ENCHANTS);
        addTabButton(PAD + 164, tabY, 60, 24, "Lore", Tab.LORE);
        addTabButton(PAD + 228, tabY, 72, 24, "Raw SNBT", Tab.RAW);

        contentY = tabY + 24 + 12;
        switch (tab) {
            case GENERAL -> initGeneral();
            case ENCHANTS -> initEnchants(contentY);
            case LORE -> initLore(contentY);
            case RAW -> initRaw(contentY);
        }

        int footerY = height - FOOTER_H;
        addRenderableWidget(new StylishButtonWidget(PAD, footerY, 88, FIELD_H, Component.literal("Apply"), this::applyToHand));
        addRenderableWidget(new StylishButtonWidget(PAD + 94, footerY, 88, FIELD_H, Component.literal("Revert"), this::revert));
        addRenderableWidget(new StylishButtonWidget(PAD + 188, footerY, 96, FIELD_H, Component.literal("Copy SNBT"), this::copySnbt));
        addRenderableWidget(new StylishButtonWidget(PAD + 290, footerY, 104, FIELD_H, Component.literal("Copy /give"), this::copyGive));
        addRenderableWidget(new StylishButtonWidget(width - PAD - 88, footerY, 88, FIELD_H, CommonComponents.GUI_BACK, this::goBack));
    }

    private void addTabButton(int x, int y, int w, int h, String label, Tab target) {
        StylishButtonWidget button = new StylishButtonWidget(x, y, w, h, Component.literal(label), () -> switchTab(target));
        button.setSelected(tab == target);
        addRenderableWidget(button);
    }

    private int labelY(int row) {
        return contentY + row * ROW_H;
    }

    private int fieldY(int row) {
        return labelY(row) + LABEL_LEAD + LABEL_GAP;
    }

    private int innerWidth() {
        return width - PAD * 2 - 8;
    }

    private void drawLabel(GuiGraphicsExtractor context, Font tr, String text, int row) {
        context.text(tr, text, INNER_X, labelY(row), UiTokens.SLATE_300, false);
    }

    private void drawLabelAt(GuiGraphicsExtractor context, Font tr, String text, int row, int x) {
        context.text(tr, text, x, labelY(row), UiTokens.SLATE_300, false);
    }

    private void switchTab(Tab next) {
        if (tab != Tab.RAW && next == Tab.RAW) {
            applyStructuredToStack();
            syncRawEditor();
        } else if (tab == Tab.RAW && next != Tab.RAW) {
            parseRawEditor();
        } else if (next != Tab.RAW) {
            applyStructuredToStack();
        }
        tab = next;
        init();
    }

    private void initGeneral() {
        int x = INNER_X;
        int w = innerWidth();
        int half = (w - GAP) / 2;

        countStepper = addStepper(x, fieldY(0), 160, FIELD_H, 1, Integer.MAX_VALUE, workingStack.getCount(), null);
        nameField = addField(x, fieldY(1), w, plainText(workingStack.get(DataComponents.CUSTOM_NAME)));
        itemNameField = addField(x, fieldY(2), w, plainText(workingStack.get(DataComponents.ITEM_NAME)));

        Integer damage = workingStack.get(DataComponents.DAMAGE);
        damageStepper = addStepper(x, fieldY(3), half, FIELD_H, 0, Integer.MAX_VALUE, damage == null ? 0 : damage, null);
        Integer repair = workingStack.get(DataComponents.REPAIR_COST);
        repairStepper = addStepper(
                x + half + GAP, fieldY(3), half, FIELD_H, 0, Integer.MAX_VALUE, repair == null ? 0 : repair, null);

        unbreakable = workingStack.has(DataComponents.UNBREAKABLE);
        Boolean glint = workingStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        glintSet = glint != null;
        glintOverride = glint != null && glint;

        addRenderableWidget(new StylishButtonWidget(x, fieldY(4), half, FIELD_H,
                Component.literal("Unbreakable: " + (unbreakable ? "ON" : "OFF")), () -> {
            unbreakable = !unbreakable;
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + half + GAP, fieldY(4), half, FIELD_H,
                Component.literal("Glint: " + (glintSet ? (glintOverride ? "ON" : "OFF") : "DEFAULT")), () -> {
            if (!glintSet) {
                glintSet = true;
                glintOverride = true;
            } else if (glintOverride) {
                glintOverride = false;
            } else {
                glintSet = false;
            }
            init();
        }));

        cmdSectionTop = fieldY(4) + FIELD_H + ROW_GAP;
        initCmdSection(x, w);
    }

    private void initCmdSection(int x, int w) {
        loadCmdListsFromStack();
        int contentTop = cmdSectionTop + UiTokens.CARD_CONTENT_TOP;
        int contentBottom = height - FOOTER_H - 8;
        if (contentTop + CMD_TOOLBAR_H > contentBottom) {
            return;
        }

        int btnW = (w - 3 * GAP) / 4;
        addRenderableWidget(new StylishButtonWidget(x, contentTop, btnW, FIELD_H, Component.literal("+ Floats"), () -> {
            cmdFloats.add(0f);
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + btnW + GAP, contentTop, btnW, FIELD_H, Component.literal("+ Flags"), () -> {
            cmdFlags.add(false);
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + (btnW + GAP) * 2, contentTop, btnW, FIELD_H, Component.literal("+ Colors"), () -> {
            cmdColors.add(0);
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + (btnW + GAP) * 3, contentTop, btnW, FIELD_H, Component.literal("+ String"), () -> {
            cmdStrings.add("");
            init();
        }));

        int listY = contentTop + CMD_TOOLBAR_H;
        int maxRows = Math.max(0, (contentBottom - listY) / CMD_ITEM_H);
        int row = 0;

        for (int i = 0; i < cmdFloats.size() && row < maxRows; i++, row++) {
            addCmdFloatRow(x, w, listY + row * CMD_ITEM_H, i);
        }
        for (int i = 0; i < cmdFlags.size() && row < maxRows; i++, row++) {
            addCmdFlagRow(x, w, listY + row * CMD_ITEM_H, i);
        }
        for (int i = 0; i < cmdColors.size() && row < maxRows; i++, row++) {
            addCmdColorRow(x, w, listY + row * CMD_ITEM_H, i);
        }
        for (int i = 0; i < cmdStrings.size() && row < maxRows; i++, row++) {
            addCmdStringRow(x, w, listY + row * CMD_ITEM_H, i);
        }
    }

    private void addCmdFloatRow(int x, int w, int rowY, int index) {
        float raw = cmdFloats.get(index);
        StylishTextFieldWidget field = addField(x, rowY, w - 36, Float.toString(raw));
        cmdFloatFields.add(field);
        addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
            cmdFloats.remove(index);
            init();
        }));
    }

    private void addCmdFlagRow(int x, int w, int rowY, int index) {
        boolean on = cmdFlags.get(index);
        addRenderableWidget(new StylishButtonWidget(x, rowY, w - 36, FIELD_H,
                Component.literal("Flag " + (index + 1) + ": " + (on ? "ON" : "OFF")), () -> {
            cmdFlags.set(index, !cmdFlags.get(index));
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
            cmdFlags.remove(index);
            init();
        }));
    }

    private void addCmdColorRow(int x, int w, int rowY, int index) {
        int raw = cmdColors.get(index);
        IntegerStepperWidget stepper = addStepper(
                x, rowY, w - 36, FIELD_H, Integer.MIN_VALUE / 4, Integer.MAX_VALUE / 4, raw,
                v -> cmdColors.set(index, v));
        cmdColorSteppers.add(stepper);
        addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
            cmdColors.remove(index);
            init();
        }));
    }

    private void addCmdStringRow(int x, int w, int rowY, int index) {
        StylishTextFieldWidget field = addField(x, rowY, w - 36, cmdStrings.get(index));
        cmdStringFields.add(field);
        addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
            cmdStrings.remove(index);
            init();
        }));
    }

    private void initEnchants(int top) {
        if (enchantRows.isEmpty()) {
            loadEnchantRowsFromStack();
        }
        int x = INNER_X;
        int w = innerWidth();
        int y = top;
        int listBottom = height - FOOTER_H - 8;
        int visible = Math.max(1, (listBottom - y - 30) / CMD_ITEM_H);
        enchantScroll = Math.min(enchantScroll, Math.max(0, enchantRows.size() - visible));

        addRenderableWidget(new StylishButtonWidget(x, y, 120, FIELD_H, Component.literal("+ Add enchant"), () -> {
            enchantRows.add(new EnchantRow(defaultEnchantId(), 1));
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(x + 128, y, 140, FIELD_H, Component.literal("+ Add all enchants"), this::addAllEnchants));
        y += FIELD_H + 8;

        for (int i = 0; i < visible; i++) {
            int idx = enchantScroll + i;
            if (idx >= enchantRows.size()) {
                break;
            }
            EnchantRow row = enchantRows.get(idx);
            int rowY = y + i * CMD_ITEM_H;
            int pickerW = w - 128;
            StylishSearchableDropdownWidget picker = new StylishSearchableDropdownWidget(
                    x,
                    rowY,
                    pickerW,
                    FIELD_H,
                    "Search enchantment…",
                    enchantmentIds,
                    row.enchantId(),
                    value -> enchantRows.set(idx, new EnchantRow(value, row.level())));
            addRenderableWidget(picker);
            enchantDropdowns.add(picker);

            IntegerStepperWidget level = addStepper(
                    x + pickerW + GAP, rowY, 90, FIELD_H, 1, 255, row.level(),
                    v -> enchantRows.set(idx, new EnchantRow(row.enchantId(), v)));
            enchantLevelSteppers.add(level);

            int removeIdx = idx;
            addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
                if (removeIdx >= 0 && removeIdx < enchantRows.size()) {
                    enchantRows.remove(removeIdx);
                    init();
                }
            }));
        }
    }

    private void initLore(int top) {
        if (loreLines.isEmpty()) {
            loadLoreLinesFromStack();
        }
        int x = INNER_X;
        int w = innerWidth();
        int y = top;
        int listBottom = height - FOOTER_H - 8;
        int visible = Math.max(1, (listBottom - y - 30) / CMD_ITEM_H);
        loreScroll = Math.min(loreScroll, Math.max(0, loreLines.size() - visible));

        addRenderableWidget(new StylishButtonWidget(x, y, 110, FIELD_H, Component.literal("+ Add line"), () -> {
            loreLines.add("");
            init();
        }));
        y += FIELD_H + 8;

        for (int i = 0; i < visible; i++) {
            int idx = loreScroll + i;
            if (idx >= loreLines.size()) {
                break;
            }
            int rowY = y + i * CMD_ITEM_H;
            StylishTextFieldWidget field = addField(x, rowY, w - 36, loreLines.get(idx));
            loreFields.add(field);
            int removeIdx = idx;
            addRenderableWidget(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Component.literal("−"), () -> {
                if (removeIdx >= 0 && removeIdx < loreLines.size()) {
                    loreLines.remove(removeIdx);
                    init();
                }
            }));
        }
    }

    private void initRaw(int top) {
        syncRawEditor();
        int h = height - top - FOOTER_H - 8;
        rawEditor = new SnbtTextAreaWidget(PAD, top, width - PAD * 2, Math.max(100, h));
        rawEditor.setText(ItemStackNbtCodec.toSnbt(workingStack, registries()));
        addRenderableWidget(rawEditor);
        addRenderableWidget(new StylishButtonWidget(PAD, height - FOOTER_H - 30, 110, 22, Component.literal("Parse SNBT"), this::parseRawEditor));
    }

    private void loadCmdListsFromStack() {
        cmdFloats.clear();
        cmdFlags.clear();
        cmdColors.clear();
        cmdStrings.clear();
        CustomModelData cmd = workingStack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            cmdFloats.addAll(cmd.floats());
            cmdFlags.addAll(cmd.flags());
            cmdColors.addAll(cmd.colors());
            cmdStrings.addAll(cmd.strings());
        }
    }

    private void loadEnchantRowsFromStack() {
        enchantRows.clear();
        ItemEnchantments enchants = workingStack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchants.entrySet()) {
            Holder<Enchantment> enchant = entry.getKey();
            Optional<ResourceKey<Enchantment>> key = enchant.unwrapKey();
            String id = key.map(k -> k.identifier().toString()).orElse(defaultEnchantId());
            enchantRows.add(new EnchantRow(id, entry.getIntValue()));
        }
        if (enchantRows.isEmpty()) {
            enchantRows.add(new EnchantRow(defaultEnchantId(), 1));
        }
    }

    private void loadLoreLinesFromStack() {
        loreLines.clear();
        ItemLore lore = workingStack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                loreLines.add(line.getString());
            }
        }
        if (loreLines.isEmpty()) {
            loreLines.add("");
        }
    }

    private List<String> loadEnchantmentIds() {
        HolderLookup.RegistryLookup<Enchantment> registry = registries().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> ids = new ArrayList<>();
        registry.listElementIds().map(key -> key.identifier().toString()).forEach(ids::add);
        ids.sort(Comparator.naturalOrder());
        if (ids.isEmpty()) {
            ids.add("minecraft:sharpness");
        }
        return ids;
    }

    private String defaultEnchantId() {
        return enchantmentIds.isEmpty() ? "minecraft:sharpness" : enchantmentIds.getFirst();
    }

    private void addAllEnchants() {
        readEnchantFields();
        Set<String> existing = new HashSet<>();
        for (EnchantRow row : enchantRows) {
            if (row.enchantId() != null && !row.enchantId().isBlank()) {
                existing.add(row.enchantId());
            }
        }
        int added = 0;
        for (String id : enchantmentIds) {
            if (existing.add(id)) {
                enchantRows.add(new EnchantRow(id, 1));
                added++;
            }
        }
        enchantRows.sort(Comparator.comparing(EnchantRow::enchantId));
        enchantScroll = 0;
        status = added > 0 ? "Added " + added + " enchantment(s)" : "All enchantments already present";
        init();
    }

    private void applyStructuredToStack() {
        readEnchantFields();
        readLoreFields();
        readCmdFields();
        if (tab == Tab.GENERAL || countStepper != null) {
            applyGeneralFields();
        }
        applyEnchantFields();
        applyLoreFields();
    }

    private void applyGeneralFields() {
        if (countStepper != null) {
            workingStack.setCount(countStepper.getValue());
        }
        if (nameField != null) {
            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                workingStack.remove(DataComponents.CUSTOM_NAME);
            } else {
                workingStack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        }
        if (itemNameField != null) {
            String name = itemNameField.getValue().trim();
            if (name.isEmpty()) {
                workingStack.remove(DataComponents.ITEM_NAME);
            } else {
                workingStack.set(DataComponents.ITEM_NAME, Component.literal(name));
            }
        }
        if (damageStepper != null) {
            workingStack.set(DataComponents.DAMAGE, damageStepper.getValue());
        }
        if (repairStepper != null) {
            workingStack.set(DataComponents.REPAIR_COST, repairStepper.getValue());
        }
        if (unbreakable) {
            workingStack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            workingStack.remove(DataComponents.UNBREAKABLE);
        }
        if (glintSet) {
            workingStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glintOverride);
        } else {
            workingStack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        applyCustomModelData();
    }

    private void readCmdFields() {
        for (int i = 0; i < cmdFloatFields.size() && i < cmdFloats.size(); i++) {
            try {
                cmdFloats.set(i, Float.parseFloat(cmdFloatFields.get(i).getValue().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        for (int i = 0; i < cmdColorSteppers.size() && i < cmdColors.size(); i++) {
            cmdColors.set(i, cmdColorSteppers.get(i).getValue());
        }
        for (int i = 0; i < cmdStringFields.size() && i < cmdStrings.size(); i++) {
            cmdStrings.set(i, cmdStringFields.get(i).getValue());
        }
    }

    private void applyCustomModelData() {
        List<Float> floats = new ArrayList<>(cmdFloats);
        List<Boolean> flags = new ArrayList<>(cmdFlags);
        List<Integer> colors = new ArrayList<>(cmdColors);
        List<String> strings = cmdStrings.stream().filter(s -> s != null && !s.isBlank()).toList();
        if (floats.isEmpty() && flags.isEmpty() && colors.isEmpty() && strings.isEmpty()) {
            workingStack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }
        workingStack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(floats, flags, strings, colors));
    }

    private void syncActiveTabFields() {
        if (tab == Tab.ENCHANTS) {
            readEnchantFields();
        } else if (tab == Tab.LORE) {
            readLoreFields();
        } else if (tab == Tab.GENERAL) {
            readCmdFields();
            if (countStepper != null) {
                applyGeneralFields();
            }
        }
    }

    private void readEnchantFields() {
        for (int i = 0; i < enchantDropdowns.size(); i++) {
            int idx = enchantScroll + i;
            if (idx >= enchantRows.size()) {
                break;
            }
            enchantRows.set(idx, new EnchantRow(
                    enchantDropdowns.get(i).getValue(),
                    enchantLevelSteppers.get(i).getValue()));
        }
    }

    private void applyEnchantFields() {
        HolderLookup.Provider lookup = registries();
        var enchantments = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(
                ItemEnchantments.EMPTY);
        for (EnchantRow row : enchantRows) {
            if (row.enchantId() == null || row.enchantId().isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(row.enchantId().trim().toLowerCase(Locale.ROOT));
            if (id == null) {
                continue;
            }
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            enchantments.get(key).ifPresent(entry -> builder.set(entry, row.level()));
        }
        ItemEnchantments built = builder.toImmutable();
        if (built.isEmpty()) {
            workingStack.remove(DataComponents.ENCHANTMENTS);
        } else {
            workingStack.set(DataComponents.ENCHANTMENTS, built);
        }
    }

    private void readLoreFields() {
        for (int i = 0; i < loreFields.size(); i++) {
            int idx = loreScroll + i;
            if (idx >= loreLines.size()) {
                break;
            }
            loreLines.set(idx, loreFields.get(i).getValue());
        }
    }

    private void applyLoreFields() {
        List<Component> lines = new ArrayList<>();
        for (String line : loreLines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            lines.add(Component.literal(line).withStyle(style -> style.withItalic(true)));
        }
        if (lines.isEmpty()) {
            workingStack.remove(DataComponents.LORE);
        } else {
            workingStack.set(DataComponents.LORE, new ItemLore(lines));
        }
    }

    private void syncRawEditor() {
        if (rawEditor != null) {
            rawEditor.setText(ItemStackNbtCodec.toSnbt(workingStack, registries()));
        }
    }

    private void parseRawEditor() {
        if (rawEditor == null) {
            return;
        }
        try {
            workingStack = ItemStackNbtCodec.fromSnbt(rawEditor.text(), registries());
            status = "Parsed SNBT";
            enchantRows.clear();
            loreLines.clear();
            cmdFloats.clear();
            cmdFlags.clear();
            cmdColors.clear();
            cmdStrings.clear();
        } catch (Exception ex) {
            status = "SNBT parse failed: " + ex.getMessage();
        }
    }

    private void applyToHand() {
        if (tab == Tab.RAW) {
            parseRawEditor();
        } else {
            applyStructuredToStack();
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            status = "Not in world";
            return;
        }
        if (client.player.getMainHandItem().isEmpty()) {
            status = "Main hand is empty";
            return;
        }
        client.player.getInventory().setItem(client.player.getInventory().getSelectedSlot(), workingStack.copy());
        status = "Applied to main hand";
    }

    private void revert() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.getMainHandItem().isEmpty()) {
            status = "Nothing to revert from";
            return;
        }
        workingStack = client.player.getMainHandItem().copy();
        enchantRows.clear();
        loreLines.clear();
        status = "Reverted to held item";
        init();
    }

    private void copySnbt() {
        applyBeforeCopy();
        if (minecraft != null && minecraft.keyboardHandler != null) {
            minecraft.keyboardHandler.setClipboard(ItemStackNbtCodec.toSnbt(workingStack, registries()));
            status = "Copied SNBT";
        }
    }

    private void copyGive() {
        applyBeforeCopy();
        if (minecraft != null && minecraft.keyboardHandler != null) {
            minecraft.keyboardHandler.setClipboard(ItemStackNbtCodec.toGiveCommand(workingStack, registries()));
            status = "Copied /give command";
        }
    }

    private void applyBeforeCopy() {
        if (tab == Tab.RAW) {
            parseRawEditor();
        } else {
            applyStructuredToStack();
        }
    }

    private void goBack() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private HolderLookup.Provider registries() {
        return ItemStackNbtCodec.registries(Minecraft.getInstance());
    }

    private StylishTextFieldWidget addField(int x, int y, int w, String value) {
        StylishTextFieldWidget field = new StylishTextFieldWidget(font, x, y, w, FIELD_H, Component.empty());
        field.setValue(value == null ? "" : value);
        return addRenderableWidget(field);
    }

    private IntegerStepperWidget addStepper(
            int x,
            int y,
            int w,
            int h,
            int min,
            int max,
            int initial,
            @org.jetbrains.annotations.Nullable java.util.function.IntConsumer onChange) {
        IntegerStepperWidget stepper = new IntegerStepperWidget(font, x, y, w, h, min, max, initial, onChange);
        for (var widget : stepper.widgets()) {
            addRenderableWidget(widget);
        }
        return stepper;
    }

    private boolean anyTextInputFocused() {
        if (nameField != null && nameField.isFocused()) {
            return true;
        }
        if (itemNameField != null && itemNameField.isFocused()) {
            return true;
        }
        for (StylishTextFieldWidget field : loreFields) {
            if (field.isFocused()) {
                return true;
            }
        }
        for (StylishTextFieldWidget field : cmdFloatFields) {
            if (field.isFocused()) {
                return true;
            }
        }
        for (StylishTextFieldWidget field : cmdStringFields) {
            if (field.isFocused()) {
                return true;
            }
        }
        if (stepperFieldFocused(countStepper)
                || stepperFieldFocused(damageStepper)
                || stepperFieldFocused(repairStepper)) {
            return true;
        }
        for (IntegerStepperWidget stepper : cmdColorSteppers) {
            if (stepperFieldFocused(stepper)) {
                return true;
            }
        }
        for (IntegerStepperWidget stepper : enchantLevelSteppers) {
            if (stepperFieldFocused(stepper)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stepperFieldFocused(@org.jetbrains.annotations.Nullable IntegerStepperWidget stepper) {
        return stepper != null && stepper.valueField().isFocused();
    }

    private static String plainText(Component text) {
        return text == null ? "" : text.getString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        UiDraw.cardElevated(context, 8, 8, width - 16, height - 16, UiTokens.R_XL);

        Font tr = font;
        context.text(tr, "NBT Editor", PAD, PAD, UiTokens.TEXT, false);
        context.text(tr, ItemStackNbtCodec.itemSummary(workingStack), PAD, PAD + 14, UiTokens.MINT_300, false);

        if (tab == Tab.GENERAL) {
            drawGeneralChrome(context, tr);
        } else if (tab == Tab.ENCHANTS) {
            context.text(tr, "Search enchantment · adjust level with +/-", INNER_X, contentY - 10,
                    UiTokens.SLATE_300, false);
        } else if (tab == Tab.LORE) {
            context.text(tr, "Lore lines render italic in-game", INNER_X, contentY - 10,
                    UiTokens.SLATE_300, false);
        }

        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        if (tab == Tab.ENCHANTS) {
            for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
                dropdown.renderPopupLayer(context);
            }
        }

        if (!status.isBlank()) {
            context.centeredText(tr, Component.literal(status), width / 2, height - 12,
                    status.toLowerCase(Locale.ROOT).contains("fail") ? 0xFFF87171 : UiTokens.MINT_300);
        }
    }

    private void drawGeneralChrome(GuiGraphicsExtractor context, Font tr) {
        int half = (innerWidth() - GAP) / 2;
        drawLabel(context, tr, "Stack count", 0);
        drawLabel(context, tr, "Custom display name", 1);
        drawLabel(context, tr, "Item name component", 2);
        drawLabelAt(context, tr, "Damage", 3, INNER_X);
        drawLabelAt(context, tr, "Repair cost", 3, INNER_X + half + GAP);
        drawLabel(context, tr, "Item flags", 4);

        if (cmdSectionTop > 0) {
            int cardH = height - cmdSectionTop - FOOTER_H - 6;
            if (cardH > 48) {
                UiComponents.drawSectionCard(
                        tr, context, INNER_X - 4, cmdSectionTop, innerWidth() + 8, cardH, "Custom model data", false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (tab == Tab.ENCHANTS && handleEnchantDropdownClick(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean handleEnchantDropdownClick(double mouseX, double mouseY, int button) {
        boolean hit = false;
        for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
            if (dropdown.hitsInteractive(mouseX, mouseY)) {
                hit = true;
            }
        }
        for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
            if (dropdown.handleMouseClick(mouseX, mouseY, button)) {
                for (StylishSearchableDropdownWidget other : enchantDropdowns) {
                    if (other != dropdown) {
                        other.close();
                    }
                }
                return true;
            }
        }
        return hit;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (tab == Tab.RAW && rawEditor != null && rawEditor.handleKey(input)) {
            return true;
        }
        if (tab == Tab.ENCHANTS) {
            for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
                if (dropdown.handleKeyPressed(input.key())) {
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (tab == Tab.RAW && rawEditor != null && rawEditor.handleChar((char) input.codepoint())) {
            return true;
        }
        if (tab == Tab.ENCHANTS) {
            for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
                if (dropdown.handleCharTyped(input.codepoint())) {
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.RAW && rawEditor != null && rawEditor.isMouseOver(mouseX, mouseY)
                && rawEditor.handleScroll(verticalAmount)) {
            return true;
        }
        if (tab == Tab.ENCHANTS) {
            for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
                if (dropdown.handleMouseScrolled(mouseX, mouseY, verticalAmount)) {
                    return true;
                }
            }
            if (enchantDropdowns.stream().anyMatch(d -> d.isOpen() || d.hasTextFocus()) || anyTextInputFocused()) {
                return false;
            }
            int max = Math.max(0, enchantRows.size() - Math.max(1, (height - contentY - 40) / CMD_ITEM_H));
            if (verticalAmount > 0) {
                enchantScroll = Math.max(0, enchantScroll - 1);
            } else if (verticalAmount < 0) {
                enchantScroll = Math.min(max, enchantScroll + 1);
            }
            init();
            return true;
        }
        if (tab == Tab.LORE) {
            if (anyTextInputFocused()) {
                return false;
            }
            int max = Math.max(0, loreLines.size() - Math.max(1, (height - contentY - 40) / CMD_ITEM_H));
            if (verticalAmount > 0) {
                loreScroll = Math.max(0, loreScroll - 1);
            } else if (verticalAmount < 0) {
                loreScroll = Math.min(max, loreScroll + 1);
            }
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        goBack();
    }
}
