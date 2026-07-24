package com.dupeclient.client.module.hud;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.core.LookTargetUtil;
import com.dupeclient.client.core.LookTargetUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HudManager {
    public static final HudManager INSTANCE = new HudManager();

    private final Map<String, HudElementDefinition> defs = new LinkedHashMap<>();
    private final List<HudElementState> activeElements = new ArrayList<>();
    private final Deque<Double> tpsSamples = new ArrayDeque<>();
    private HudSettings settings = new HudSettings();
    private boolean active = true;

    private long lastTickAtMs;
    private boolean bindWasDown;
    private final int[] measureScratch = new int[2];


    private HudManager() {
    }

    public void initialize() {
        if (!defs.isEmpty()) {
            return;
        }
        registerDefaults();
        load();
    }

    public HudSettings settings() {
        return settings;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
        save();
    }

    public Collection<HudElementDefinition> definitions() {
        return defs.values();
    }

    public List<HudElementState> elements() {
        return activeElements;
    }

    public boolean hasElement(String id) {
        return findElement(id) != null;
    }

    @Nullable
    public HudElementState findElement(String id) {
        for (HudElementState s : activeElements) {
            if (s != null && s.id != null && s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    public void addElement(String id) {
        if (id == null || id.isBlank() || hasElement(id)) {
            return;
        }
        HudElementDefinition def = defs.get(id);
        if (def == null) {
            return;
        }
        HudElementState st = new HudElementState();
        st.id = id;
        st.x = def.defaultX();
        st.y = def.defaultY();
        st.active = true;
        activeElements.add(st);
        save();
    }

    public void removeElement(String id) {
        activeElements.removeIf(e -> e != null && id.equals(e.id));
        save();
    }

    public void resetToDefaultElements() {
        activeElements.clear();
        addElement("watermark");
        addElement("fps");
        addElement("tps");
        addElement("ping");
        addElement("speed");
        addElement("looking_at");
        addElement("position");
        addElement("opposite_position");
        addElement("rotation");
        save();
    }

    public void load() {
        HudPersistedState state = HudConfigManager.load();
        this.active = state.active;
        this.settings = state.settings != null ? state.settings : new HudSettings();
        this.activeElements.clear();
        if (state.elements != null) {
            for (HudElementState e : state.elements) {
                if (e == null || e.id == null || !defs.containsKey(e.id)) {
                    continue;
                }
                activeElements.add(e);
            }
        }
        if (activeElements.isEmpty()) {
            resetToDefaultElements();
        }
        sanitizeSettings();
    }

    public void save() {
        sanitizeSettings();
        HudPersistedState out = new HudPersistedState();
        out.active = active;
        out.settings = settings;
        out.elements = new ArrayList<>(activeElements);
        HudConfigManager.save(out);
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            bindWasDown = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTickAtMs > 0L) {
            double dt = Math.max(1.0, now - lastTickAtMs);
            double est = Math.min(20.0, 1000.0 / dt);
            tpsSamples.addLast(est);
            if (tpsSamples.size() > 40) {
                tpsSamples.removeFirst();
            }
        }
        lastTickAtMs = now;

        if (settings.bindKey < 0) {
            bindWasDown = false;
            return;
        }
        long win = client.getWindow().getHandle();
        if (!modsSatisfied(win, settings.bindMods)) {
            bindWasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(win, settings.bindKey) == GLFW.GLFW_PRESS;
        if (down && !bindWasDown) {
            active = !active;
            save();
        }
        bindWasDown = down;
    }

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return;
        }
        if (!active) {
            return;
        }
        if (settings.hideInMenus && client.currentScreen != null) {
            return;
        }
        if (client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        for (HudElementState st : activeElements) {
            if (st == null || !st.active) {
                continue;
            }
            HudElementDefinition def = defs.get(st.id);
            if (def == null) {
                continue;
            }
            String text = def.textProvider().text(client, this);
            if (text == null || text.isEmpty()) {
                continue;
            }
            int[] size = measureElement(st, client);
            int x = anchoredX(st, size[0], client);
            int y = anchoredY(st, size[1], client);
            // Match the editor visuals: soft box + white text, no color cycling.
            context.fill(x - 1, y - 1, x + size[0] + 1, y + size[1] + 1, 0x664B6AA8);
            context.fill(x, y, x + size[0], y + size[1], 0x33223555);
            drawScaledText(context, client.textRenderer, text, x, y, 0xFFFFFFFF);
        }
    }

    public int[] measureElement(HudElementState st, MinecraftClient client) {
        HudElementDefinition def = st == null ? null : defs.get(st.id);
        if (def == null || client == null || client.textRenderer == null) {
            measureScratch[0] = 0;
            measureScratch[1] = 0;
            return measureScratch;
        }
        String text = def.textProvider().text(client, this);
        if (text == null) {
            text = "";
        }
        float sc = (float) settings.textScale;
        measureScratch[0] = Math.round(client.textRenderer.getWidth(text) * sc);
        measureScratch[1] = Math.round(client.textRenderer.fontHeight * sc);
        return measureScratch;
    }

    public double averageTps() {
        if (tpsSamples.isEmpty()) {
            return 20.0;
        }
        double sum = 0.0;
        for (double d : tpsSamples) {
            sum += d;
        }
        return sum / tpsSamples.size();
    }

    private void drawScaledText(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        float scale = (float) settings.textScale;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(tr, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private static int anchoredX(HudElementState st, int widthPx, MinecraftClient client) {
        int screenW = client.getWindow().getScaledWidth();
        return st.x >= 0 ? st.x : screenW + st.x - widthPx;
    }

    private static int anchoredY(HudElementState st, int heightPx, MinecraftClient client) {
        int screenH = client.getWindow().getScaledHeight();
        return st.y >= 0 ? st.y : screenH + st.y - heightPx;
    }

    private void sanitizeSettings() {
        if (settings == null) {
            settings = new HudSettings();
        }
        if (settings.textScale < 0.5) {
            settings.textScale = 0.5;
        } else if (settings.textScale > 3.0) {
            settings.textScale = 3.0;
        }
        if (settings.border < 0) {
            settings.border = 0;
        } else if (settings.border > 20) {
            settings.border = 20;
        }
        if (settings.snappingRange < 0) {
            settings.snappingRange = 0;
        } else if (settings.snappingRange > 20) {
            settings.snappingRange = 20;
        }
        if (settings.textColors == null || settings.textColors.isEmpty()) {
            settings.textColors = new ArrayList<>(List.of(0xFFFFFFFF));
        }
    }

    private void registerDefaults() {
        defs.put("watermark", new HudElementDefinition("watermark", "Watermark", 4, 4,
                (client, hud) -> "DupeClient " + DupeClient.BUILD_TAG));
        defs.put("fps", new HudElementDefinition("fps", "FPS", 4, 16,
                (client, hud) -> "FPS: " + client.getCurrentFps()));
        defs.put("tps", new HudElementDefinition("tps", "TPS", 4, 28,
                (client, hud) -> "TPS: " + String.format(Locale.US, "%.1f", hud.averageTps())));
        defs.put("ping", new HudElementDefinition("ping", "Ping", 4, 40,
                (client, hud) -> {
                    PlayerListEntry e = client.player == null || client.getNetworkHandler() == null
                            ? null
                            : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                    return "Ping: " + (e == null ? "-" : e.getLatency()) + " ms";
                }));
        defs.put("speed", new HudElementDefinition("speed", "Speed", 4, 52,
                (client, hud) -> {
                    Vec3d v = client.player == null ? Vec3d.ZERO : client.player.getVelocity();
                    double bps = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
                    return "Speed: " + String.format(Locale.US, "%.2f b/s", bps);
                }));
        defs.put("looking_at", new HudElementDefinition("looking_at", "Looking At", 4, 64,
                (client, hud) -> {
                    String s = LookTargetUtil.describe(client);
                    return s == null ? "Looking: none" : s;
                }));
        defs.put("position", new HudElementDefinition("position", "Position", -4, -4,
                (client, hud) -> {
                    if (client.player == null) return "Pos: -";
                    BlockPos p = client.player.getBlockPos();
                    return "Pos: " + p.getX() + " " + p.getY() + " " + p.getZ();
                }));
        defs.put("opposite_position", new HudElementDefinition("opposite_position", "Opposite Position", -4, -16,
                (client, hud) -> {
                    if (client.player == null) return "Nether Pos: -";
                    double px = client.player.getX();
                    double pz = client.player.getZ();
                    boolean inNether = client.world != null && World.NETHER.equals(client.world.getRegistryKey());
                    double k = inNether ? 8.0 : 0.125;
                    return "Nether Pos: " + (int) Math.floor(px * k) + " " + (int) Math.floor(pz * k);
                }));
        defs.put("rotation", new HudElementDefinition("rotation", "Rotation", -4, -28,
                (client, hud) -> {
                    if (client.player == null) return "Rot: -";
                    return "Rot: "
                            + String.format(Locale.US, "%.1f", client.player.getYaw()) + " "
                            + String.format(Locale.US, "%.1f", client.player.getPitch());
                }));
    }

    private static boolean modsSatisfied(long window, int required) {
        if (required == 0) {
            return true;
        }
        int cur = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) cur |= GLFW.GLFW_MOD_SHIFT;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) cur |= GLFW.GLFW_MOD_CONTROL;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) cur |= GLFW.GLFW_MOD_ALT;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS) cur |= GLFW.GLFW_MOD_SUPER;
        return (cur & required) == required;
    }
}
