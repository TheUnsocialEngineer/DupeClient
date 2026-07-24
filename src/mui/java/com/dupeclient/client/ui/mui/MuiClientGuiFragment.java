package com.dupeclient.client.ui.mui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.ClientGuiEmbeddedController;
import com.dupeclient.client.gui.GuiClickHelper;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.ui.MuiSurfaceHostTick;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * mVUS shell for DupeClient: top title bar, left scroller with module list (MUI
 * {@link Button}s), and a single {@link MinecraftSurfaceView} for the active module — matches the
 * ModernUI-MC layout pattern.
 */
public final class MuiClientGuiFragment extends Fragment {
    private Screen returnScreen;
    private final ClientGuiEmbeddedController embedded = new ClientGuiEmbeddedController();
    private MinecraftSurfaceView surfaceView;
    /** GL FBO from {@code onSurfaceChanged} — can differ from each {@code onDraw} pass; do not max into layout. */
    private int surfaceBufferW;
    private int surfaceBufferH;
    private int lastW = 800;
    private int lastH = 600;
    private boolean pointerDown;
    private int activeButton = -1;
    @Nullable
    private Button[] moduleNavButtons;

    void setReturnScreen(Screen returnScreen) {
        this.returnScreen = returnScreen;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, DataSet saved) {
        var c = requireContext();
        embedded.setUseExternalMuiNav(true);
        embedded.setReserveTopForVanillaCloseButton(false);
        var dm = c.getResources().getDisplayMetrics();
        float d = dm.density;
        int topBarH = Math.round(44 * d);
        int sidebarW = Math.round(196 * d);

        var root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        var topBar = new LinearLayout(c);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        {
            var lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topBarH);
            root.addView(topBar, lp);
        }
        var title = new TextView(c);
        title.setText("DupeClient");
        {
            @SuppressWarnings("deprecation")
            var titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            topBar.addView(title, titleLp);
        }
        MuiDupeStyle.styleTopTitle(title);
        var close = new Button(c);
        close.setText("Close");
        MuiDupeStyle.styleFrameButton(close);
        close.setOnClickListener(b -> {
            Screen r = returnScreen;
            if (r != null) {
                MinecraftClient.getInstance().setScreen(r);
            } else {
                MinecraftClient.getInstance().setScreen(null);
            }
        });
        {
            int pad = Math.round(8 * d);
            int chipW = Math.round(88 * d);
            var blp = new LinearLayout.LayoutParams(chipW, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.setMarginEnd(pad);
            blp.setMarginStart(pad);
            topBar.addView(close, blp);
        }
        topBar.setPadding(Math.round(12 * d), 0, 0, 0);
        topBar.bringChildToFront(close);
        topBar.setElevation(8f);

        var body = new LinearLayout(c);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(0, 0, 0, 0);
        {
            @SuppressWarnings("deprecation")
            var bodyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f);
            root.addView(body, bodyLp);
        }
        var navScroll = new ScrollView(c);
        navScroll.setFillViewport(true);
        {
            var navLp = new LinearLayout.LayoutParams(sidebarW, ViewGroup.LayoutParams.MATCH_PARENT);
            navLp.setMargins(0, 0, 0, 0);
            body.addView(navScroll, navLp);
        }
        var navCol = new LinearLayout(c);
        navCol.setOrientation(LinearLayout.VERTICAL);
        int navPadH = Math.round(6 * d);
        int navPadV = Math.round(6 * d);
        navCol.setPadding(navPadH, navPadV, navPadH, navPadV);
        // MATCH_PARENT height + setFillViewport(true) so the nav column is as tall as the body when
        // the module list is short (otherwise the ScrollView can leave a dead band in the bar).
        navScroll.addView(navCol, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        var panels = DupeClient.getGuiManager().getPanels();
        if (!panels.isEmpty()) {
            moduleNavButtons = new Button[panels.size()];
            for (int i = 0; i < panels.size(); i++) {
                Panel p = panels.get(i);
                var b = new Button(c);
                b.setText(p.getTitle().getString());
                b.setPadding(Math.round(14 * d), 0, Math.round(10 * d), 0);
                MuiDupeStyle.styleNavButton(b, i == embedded.getSelectedModuleIndex());
                int idx = i;
                b.setOnClickListener(v -> {
                    embedded.setSelectedModuleIndex(idx);
                    syncNavSelection();
                    if (surfaceView != null) {
                        surfaceView.invalidate();
                    }
                });
                int gap = Math.round(6 * d);
                int bh = Math.round(48 * d);
                var blp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        bh);
                blp.setMargins(0, i == 0 ? 0 : gap, 0, 0);
                navCol.addView(b, blp);
                moduleNavButtons[i] = b;
            }
        } else {
            moduleNavButtons = new Button[0];
        }

        var surface = new MinecraftSurfaceView(c);
        this.surfaceView = surface;
        surface.setPadding(0, 0, 0, 0);
        surface.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        {
            var slp = (LinearLayout.LayoutParams) surface.getLayoutParams();
            slp.setMargins(0, 0, 0, 0);
            surface.setLayoutParams(slp);
        }
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        surface.setClickable(true);
        surface.setRenderer(new MinecraftSurfaceView.Renderer() {
            @Override
            public void onSurfaceChanged(int w, int h) {
                surfaceBufferW = w;
                surfaceBufferH = h;
                lastW = Math.max(1, w);
                lastH = Math.max(1, h);
                embedded.syncViewport(lastW, lastH);
                embedded.onScreenOpen();
                syncNavSelection();
            }

            @Override
            public void onDraw(DrawContext context, int w, int h, float partialTick, double scaleInv, float alpha) {
                int rw = w > 0 ? w : 1;
                int rh = h > 0 ? h : 1;
                if (w <= 0 || h <= 0) {
                    int[] fb = MuiSurfaceBufferSize.resolveForDraw(surfaceView, 1, 1, scaleInv);
                    rw = Math.max(1, fb[0]);
                    rh = Math.max(1, fb[1]);
                }
                // Single source of truth: this pass size only (never max with FBO — that desyncs from DrawContext).
                lastW = rw;
                lastH = rh;
                embedded.syncViewport(rw, rh);
                MinecraftClient mc = MinecraftClient.getInstance();
                double[] mouseInSurface = MuiSurfaceMouseMapper.windowToSurface(surfaceView, rw, rh, mc);
                int mix = (int) mouseInSurface[0];
                int miy = (int) mouseInSurface[1];
                embedded.updateNavHover(mix, miy);
                embedded.render(context, mc.textRenderer, mix, miy, partialTick, rw, rh);
            }
        });
        surface.setOnTouchListener((v, e) -> handleSurfaceTouch(v, e));
        surface.setOnGenericMotionListener((v, e) -> handleGenericMotion(e, v));
        surface.setOnKeyListener((v, kc, e) -> handleKey(e));
        body.addView(surface);
        surface.setZ(0f);
        surface.post(surface::requestFocus);
        return root;
    }

    private void syncNavSelection() {
        if (moduleNavButtons == null) {
            return;
        }
        int n = moduleNavButtons.length;
        for (int i = 0; i < n; i++) {
            Button b = moduleNavButtons[i];
            MuiDupeStyle.styleNavButton(b, i == embedded.getSelectedModuleIndex());
        }
    }

    private boolean handleGenericMotion(MotionEvent event, View v) {
        if (event.getActionMasked() != MotionEvent.ACTION_SCROLL) {
            return false;
        }
        float dy = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        if (dy == 0) {
            return false;
        }
        double mx = localToGuiX(v, event.getX());
        double my = localToGuiY(v, event.getY());
        return embedded.handleContentScroll(mx, my, 0, dy);
    }

    private boolean handleKey(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        int k = e.getKeyCode();
        if (k == KeyEvent.KEY_ESCAPE) {
            Screen r = returnScreen;
            if (r != null) {
                MinecraftClient.getInstance().setScreen(r);
            } else {
                MinecraftClient.getInstance().setScreen(null);
            }
            return true;
        }
        var ki = GuiClickHelper.muiToKey(e.getKeyCode(), e.getScanCode(), e.getModifiers());
        for (var panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible() && panel.keyPressed(ki.key(), ki.scancode(), ki.modifiers())) {
                return true;
            }
        }
        if (e.getMappedChar() != 0) {
            int cp = e.getMappedChar();
            for (var panel : DupeClient.getGuiManager().getPanels()) {
                if (panel.isVisible() && panel.charTyped(cp, ki.modifiers())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Touch/motion in view local coords → same space as last {@link #lastW} / height from the
     * surface renderer. Avoid scaling by live {@code v.getWidth()}; it can change during relayout
     * and warps input with the same symptom as a “resizing” GUI.
     */
    private double localToGuiX(View v, float lx) {
        if (v == null || lastW <= 0) {
            return lx;
        }
        return MathHelper.clamp((double) lx, 0.0, (double) Math.max(0, lastW - 1));
    }

    private double localToGuiY(View v, float ly) {
        if (v == null || lastH <= 0) {
            return ly;
        }
        return MathHelper.clamp((double) ly, 0.0, (double) Math.max(0, lastH - 1));
    }

    private boolean handleSurfaceTouch(View v, MotionEvent event) {
        int a = event.getActionMasked();
        double mx = localToGuiX(v, event.getX());
        double my = localToGuiY(v, event.getY());
        if (a == MotionEvent.ACTION_SCROLL) {
            float dy = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (dy != 0) {
                return embedded.handleContentScroll(mx, my, 0, dy);
            }
            return false;
        }
        if (a == MotionEvent.ACTION_DOWN) {
            pointerDown = true;
            activeButton = 0;
            if (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
                activeButton = 1;
            } else if (event.isButtonPressed(MotionEvent.BUTTON_TERTIARY)) {
                activeButton = 2;
            }
            if (embedded.handleNavClick(mx, my, activeButton)) {
                return true;
            }
            if (embedded.handlePanelClick(mx, my, activeButton)) {
                return true;
            }
            return true;
        }
        if (a == MotionEvent.ACTION_MOVE) {
            if (!pointerDown) {
                return false;
            }
            embedded.handlePanelDrag(mx, my, activeButton);
            return true;
        }
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            embedded.handlePanelRelease(mx, my, activeButton);
            pointerDown = false;
            activeButton = -1;
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        MuiSurfaceHostTick.set(embedded::tick);
        View v = getView();
        if (v != null) {
            v.post(() -> {
                ViewGroup.LayoutParams p = v.getLayoutParams();
                if (p != null
                        && (p.width != ViewGroup.LayoutParams.MATCH_PARENT
                        || p.height != ViewGroup.LayoutParams.MATCH_PARENT)) {
                    p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    p.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    v.setLayoutParams(p);
                }
            });
        }
    }

    @Override
    public void onStop() {
        MuiSurfaceHostTick.set(null);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        embedded.onRemoved();
        super.onDestroyView();
    }
}
