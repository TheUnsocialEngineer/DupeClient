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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

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
        super(Text.literal("NBT Editor"));
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
        clearChildren();
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
        addDrawableChild(new StylishButtonWidget(PAD, footerY, 88, FIELD_H, Text.literal("Apply"), this::applyToHand));
        addDrawableChild(new StylishButtonWidget(PAD + 94, footerY, 88, FIELD_H, Text.literal("Revert"), this::revert));
        addDrawableChild(new StylishButtonWidget(PAD + 188, footerY, 96, FIELD_H, Text.literal("Copy SNBT"), this::copySnbt));
        addDrawableChild(new StylishButtonWidget(PAD + 290, footerY, 104, FIELD_H, Text.literal("Copy /give"), this::copyGive));
        addDrawableChild(new StylishButtonWidget(width - PAD - 88, footerY, 88, FIELD_H, ScreenTexts.BACK, this::goBack));
    }

    private void addTabButton(int x, int y, int w, int h, String label, Tab target) {
        StylishButtonWidget button = new StylishButtonWidget(x, y, w, h, Text.literal(label), () -> switchTab(target));
        button.setSelected(tab == target);
        addDrawableChild(button);
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

    private void drawLabel(DrawContext context, TextRenderer tr, String text, int row) {
        context.drawText(tr, text, INNER_X, labelY(row), UiTokens.SLATE_300, false);
    }

    private void drawLabelAt(DrawContext context, TextRenderer tr, String text, int row, int x) {
        context.drawText(tr, text, x, labelY(row), UiTokens.SLATE_300, false);
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
        nameField = addField(x, fieldY(1), w, plainText(workingStack.get(DataComponentTypes.CUSTOM_NAME)));
        itemNameField = addField(x, fieldY(2), w, plainText(workingStack.get(DataComponentTypes.ITEM_NAME)));

        Integer damage = workingStack.get(DataComponentTypes.DAMAGE);
        damageStepper = addStepper(x, fieldY(3), half, FIELD_H, 0, Integer.MAX_VALUE, damage == null ? 0 : damage, null);
        Integer repair = workingStack.get(DataComponentTypes.REPAIR_COST);
        repairStepper = addStepper(
                x + half + GAP, fieldY(3), half, FIELD_H, 0, Integer.MAX_VALUE, repair == null ? 0 : repair, null);

        unbreakable = workingStack.contains(DataComponentTypes.UNBREAKABLE);
        Boolean glint = workingStack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        glintSet = glint != null;
        glintOverride = glint != null && glint;

        addDrawableChild(new StylishButtonWidget(x, fieldY(4), half, FIELD_H,
                Text.literal("Unbreakable: " + (unbreakable ? "ON" : "OFF")), () -> {
            unbreakable = !unbreakable;
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + half + GAP, fieldY(4), half, FIELD_H,
                Text.literal("Glint: " + (glintSet ? (glintOverride ? "ON" : "OFF") : "DEFAULT")), () -> {
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
        addDrawableChild(new StylishButtonWidget(x, contentTop, btnW, FIELD_H, Text.literal("+ Floats"), () -> {
            cmdFloats.add(0f);
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + btnW + GAP, contentTop, btnW, FIELD_H, Text.literal("+ Flags"), () -> {
            cmdFlags.add(false);
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + (btnW + GAP) * 2, contentTop, btnW, FIELD_H, Text.literal("+ Colors"), () -> {
            cmdColors.add(0);
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + (btnW + GAP) * 3, contentTop, btnW, FIELD_H, Text.literal("+ String"), () -> {
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
        addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
            cmdFloats.remove(index);
            init();
        }));
    }

    private void addCmdFlagRow(int x, int w, int rowY, int index) {
        boolean on = cmdFlags.get(index);
        addDrawableChild(new StylishButtonWidget(x, rowY, w - 36, FIELD_H,
                Text.literal("Flag " + (index + 1) + ": " + (on ? "ON" : "OFF")), () -> {
            cmdFlags.set(index, !cmdFlags.get(index));
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
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
        addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
            cmdColors.remove(index);
            init();
        }));
    }

    private void addCmdStringRow(int x, int w, int rowY, int index) {
        StylishTextFieldWidget field = addField(x, rowY, w - 36, cmdStrings.get(index));
        cmdStringFields.add(field);
        addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
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

        addDrawableChild(new StylishButtonWidget(x, y, 120, FIELD_H, Text.literal("+ Add enchant"), () -> {
            enchantRows.add(new EnchantRow(defaultEnchantId(), 1));
            init();
        }));
        addDrawableChild(new StylishButtonWidget(x + 128, y, 140, FIELD_H, Text.literal("+ Add all enchants"), this::addAllEnchants));
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
            addDrawableChild(picker);
            enchantDropdowns.add(picker);

            IntegerStepperWidget level = addStepper(
                    x + pickerW + GAP, rowY, 90, FIELD_H, 1, 255, row.level(),
                    v -> enchantRows.set(idx, new EnchantRow(row.enchantId(), v)));
            enchantLevelSteppers.add(level);

            int removeIdx = idx;
            addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
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

        addDrawableChild(new StylishButtonWidget(x, y, 110, FIELD_H, Text.literal("+ Add line"), () -> {
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
            addDrawableChild(new StylishButtonWidget(x + w - 30, rowY, 30, FIELD_H, Text.literal("−"), () -> {
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
        addDrawableChild(rawEditor);
        addDrawableChild(new StylishButtonWidget(PAD, height - FOOTER_H - 30, 110, 22, Text.literal("Parse SNBT"), this::parseRawEditor));
    }

    private void loadCmdListsFromStack() {
        cmdFloats.clear();
        cmdFlags.clear();
        cmdColors.clear();
        cmdStrings.clear();
        CustomModelDataComponent cmd = workingStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            cmdFloats.addAll(cmd.floats());
            cmdFlags.addAll(cmd.flags());
            cmdColors.addAll(cmd.colors());
            cmdStrings.addAll(cmd.strings());
        }
    }

    private void loadEnchantRowsFromStack() {
        enchantRows.clear();
        ItemEnchantmentsComponent enchants = workingStack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (var entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchant = entry.getKey();
            Optional<RegistryKey<Enchantment>> key = enchant.getKey();
            String id = key.map(k -> k.getValue().toString()).orElse(defaultEnchantId());
            enchantRows.add(new EnchantRow(id, entry.getIntValue()));
        }
        if (enchantRows.isEmpty()) {
            enchantRows.add(new EnchantRow(defaultEnchantId(), 1));
        }
    }

    private void loadLoreLinesFromStack() {
        loreLines.clear();
        LoreComponent lore = workingStack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                loreLines.add(line.getString());
            }
        }
        if (loreLines.isEmpty()) {
            loreLines.add("");
        }
    }

    private List<String> loadEnchantmentIds() {
        RegistryWrapper.Impl<Enchantment> registry = registries().getOrThrow(RegistryKeys.ENCHANTMENT);
        List<String> ids = new ArrayList<>();
        registry.streamKeys().map(key -> key.getValue().toString()).forEach(ids::add);
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
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                workingStack.remove(DataComponentTypes.CUSTOM_NAME);
            } else {
                workingStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            }
        }
        if (itemNameField != null) {
            String name = itemNameField.getText().trim();
            if (name.isEmpty()) {
                workingStack.remove(DataComponentTypes.ITEM_NAME);
            } else {
                workingStack.set(DataComponentTypes.ITEM_NAME, Text.literal(name));
            }
        }
        if (damageStepper != null) {
            workingStack.set(DataComponentTypes.DAMAGE, damageStepper.getValue());
        }
        if (repairStepper != null) {
            workingStack.set(DataComponentTypes.REPAIR_COST, repairStepper.getValue());
        }
        if (unbreakable) {
            workingStack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        } else {
            workingStack.remove(DataComponentTypes.UNBREAKABLE);
        }
        if (glintSet) {
            workingStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glintOverride);
        } else {
            workingStack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        }
        applyCustomModelData();
    }

    private void readCmdFields() {
        for (int i = 0; i < cmdFloatFields.size() && i < cmdFloats.size(); i++) {
            try {
                cmdFloats.set(i, Float.parseFloat(cmdFloatFields.get(i).getText().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        for (int i = 0; i < cmdColorSteppers.size() && i < cmdColors.size(); i++) {
            cmdColors.set(i, cmdColorSteppers.get(i).getValue());
        }
        for (int i = 0; i < cmdStringFields.size() && i < cmdStrings.size(); i++) {
            cmdStrings.set(i, cmdStringFields.get(i).getText());
        }
    }

    private void applyCustomModelData() {
        List<Float> floats = new ArrayList<>(cmdFloats);
        List<Boolean> flags = new ArrayList<>(cmdFlags);
        List<Integer> colors = new ArrayList<>(cmdColors);
        List<String> strings = cmdStrings.stream().filter(s -> s != null && !s.isBlank()).toList();
        if (floats.isEmpty() && flags.isEmpty() && colors.isEmpty() && strings.isEmpty()) {
            workingStack.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
            return;
        }
        workingStack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                new CustomModelDataComponent(floats, flags, strings, colors));
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
        RegistryWrapper.WrapperLookup lookup = registries();
        var enchantments = lookup.getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                ItemEnchantmentsComponent.DEFAULT);
        for (EnchantRow row : enchantRows) {
            if (row.enchantId() == null || row.enchantId().isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(row.enchantId().trim().toLowerCase(Locale.ROOT));
            if (id == null) {
                continue;
            }
            RegistryKey<Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
            enchantments.getOptional(key).ifPresent(entry -> builder.set(entry, row.level()));
        }
        ItemEnchantmentsComponent built = builder.build();
        if (built.isEmpty()) {
            workingStack.remove(DataComponentTypes.ENCHANTMENTS);
        } else {
            workingStack.set(DataComponentTypes.ENCHANTMENTS, built);
        }
    }

    private void readLoreFields() {
        for (int i = 0; i < loreFields.size(); i++) {
            int idx = loreScroll + i;
            if (idx >= loreLines.size()) {
                break;
            }
            loreLines.set(idx, loreFields.get(i).getText());
        }
    }

    private void applyLoreFields() {
        List<Text> lines = new ArrayList<>();
        for (String line : loreLines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            lines.add(Text.literal(line).styled(style -> style.withItalic(true)));
        }
        if (lines.isEmpty()) {
            workingStack.remove(DataComponentTypes.LORE);
        } else {
            workingStack.set(DataComponentTypes.LORE, new LoreComponent(lines));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            status = "Not in world";
            return;
        }
        if (client.player.getMainHandStack().isEmpty()) {
            status = "Main hand is empty";
            return;
        }
        client.player.getInventory().setStack(client.player.getInventory().getSelectedSlot(), workingStack.copy());
        status = "Applied to main hand";
    }

    private void revert() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.getMainHandStack().isEmpty()) {
            status = "Nothing to revert from";
            return;
        }
        workingStack = client.player.getMainHandStack().copy();
        enchantRows.clear();
        loreLines.clear();
        status = "Reverted to held item";
        init();
    }

    private void copySnbt() {
        applyBeforeCopy();
        if (client != null && client.keyboard != null) {
            client.keyboard.setClipboard(ItemStackNbtCodec.toSnbt(workingStack, registries()));
            status = "Copied SNBT";
        }
    }

    private void copyGive() {
        applyBeforeCopy();
        if (client != null && client.keyboard != null) {
            client.keyboard.setClipboard(ItemStackNbtCodec.toGiveCommand(workingStack, registries()));
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
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private RegistryWrapper.WrapperLookup registries() {
        return ItemStackNbtCodec.registries(MinecraftClient.getInstance());
    }

    private StylishTextFieldWidget addField(int x, int y, int w, String value) {
        StylishTextFieldWidget field = new StylishTextFieldWidget(textRenderer, x, y, w, FIELD_H, Text.empty());
        field.setText(value == null ? "" : value);
        return addDrawableChild(field);
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
        IntegerStepperWidget stepper = new IntegerStepperWidget(textRenderer, x, y, w, h, min, max, initial, onChange);
        for (var widget : stepper.widgets()) {
            addDrawableChild(widget);
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

    private static String plainText(Text text) {
        return text == null ? "" : text.getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        UiDraw.cardElevated(context, 8, 8, width - 16, height - 16, UiTokens.R_XL);

        TextRenderer tr = textRenderer;
        context.drawText(tr, "NBT Editor", PAD, PAD, UiTokens.TEXT, false);
        context.drawText(tr, ItemStackNbtCodec.itemSummary(workingStack), PAD, PAD + 14, UiTokens.MINT_300, false);

        if (tab == Tab.GENERAL) {
            drawGeneralChrome(context, tr);
        } else if (tab == Tab.ENCHANTS) {
            context.drawText(tr, "Search enchantment · adjust level with +/-", INNER_X, contentY - 10,
                    UiTokens.SLATE_300, false);
        } else if (tab == Tab.LORE) {
            context.drawText(tr, "Lore lines render italic in-game", INNER_X, contentY - 10,
                    UiTokens.SLATE_300, false);
        }

        super.render(context, mouseX, mouseY, deltaTicks);

        if (tab == Tab.ENCHANTS) {
            for (StylishSearchableDropdownWidget dropdown : enchantDropdowns) {
                dropdown.renderPopupLayer(context);
            }
        }

        if (!status.isBlank()) {
            context.drawCenteredTextWithShadow(tr, Text.literal(status), width / 2, height - 12,
                    status.toLowerCase(Locale.ROOT).contains("fail") ? 0xFFF87171 : UiTokens.MINT_300);
        }
    }

    private void drawGeneralChrome(DrawContext context, TextRenderer tr) {
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
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public boolean keyPressed(KeyInput input) {
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
    public boolean charTyped(CharInput input) {
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
    public void close() {
        goBack();
    }
}
