package com.dupeclient.client.ui.mui;

import com.dupeclient.client.gui.GuiClickHelper;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.MuiHostable;
import com.dupeclient.client.gui.SocialScreen;
import com.dupeclient.client.hud.HudEditorScreen;
import com.dupeclient.client.macro.MacroEditorMuiGate;
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
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Hosts a {@link MuiHostable} {@link Screen} in a {@link MinecraftSurfaceView} with a Modern UI
 * title bar (matches DupeClient mVUS shell).
 */
public final class MuiHostedScreenFragment extends Fragment {
    private enum Kind {
        SOCIAL,
        MACRO,
        HUD
    }

    private Screen returnScreen;
    private MuiHostable host;
    private Kind kind;
    private MinecraftSurfaceView surfaceView;
    /** GL FBO from {@code Renderer#onSurfaceChanged} — not the same as {@code onDraw} root w/h. */
    private int surfaceBufferW;
    private int surfaceBufferH;
    private int lastW = 800;
    private int lastH = 600;
    private boolean pointerDown;
    private int activeButton;
    private double lastDragX;
    private double lastDragY;

    static MuiHostedScreenFragment forSocial(Screen parent) {
        MuiHostedScreenFragment f = new MuiHostedScreenFragment();
        f.returnScreen = parent;
        f.kind = Kind.SOCIAL;
        f.host = (MuiHostable) new SocialScreen(parent);
        return f;
    }

    static MuiHostedScreenFragment forMacro(Screen parent, @Nullable String loadId) {
        MuiHostedScreenFragment f = new MuiHostedScreenFragment();
        f.returnScreen = parent;
        f.kind = Kind.MACRO;
        f.host = (MuiHostable) new MacroEditorScreen(parent, loadId);
        return f;
    }

    static MuiHostedScreenFragment forHud(Screen parent) {
        MuiHostedScreenFragment f = new MuiHostedScreenFragment();
        f.returnScreen = parent;
        f.kind = Kind.HUD;
        f.host = (MuiHostable) new HudEditorScreen(parent);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, DataSet saved) {
        var c = requireContext();
        float d = c.getResources().getDisplayMetrics().density;
        int topBarH = Math.round(40 * d);
        int pad = Math.round(8 * d);

        var screen = (Screen) host;
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
            @SuppressWarnings("deprecation")
            var topLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    topBarH);
            root.addView(topBar, topLp);
        }
        topBar.setPadding(Math.round(12 * d), 0, 0, 0);
        var title = new TextView(c);
        title.setText(screen.getTitle().getString());
        MuiDupeStyle.styleTopTitle(title);
        {
            @SuppressWarnings("deprecation")
            var tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            topBar.addView(title, tlp);
        }
        var close = new Button(c);
        close.setText("Close");
        MuiDupeStyle.styleFrameButton(close);
        close.setClickable(true);
        close.setOnClickListener(b -> {
            if (kind == Kind.SOCIAL) {
                MinecraftClient.getInstance().setScreen(returnScreen);
            } else {
                screen.close();
            }
        });
        {
            int chipW = Math.round(88 * d);
            var clp = new LinearLayout.LayoutParams(chipW, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMarginEnd(pad);
            clp.setMarginStart(pad);
            topBar.addView(close, clp);
        }
        topBar.setElevation(8f);

        var body = new FrameLayout(c);
        body.setPadding(0, 0, 0, 0);
        {
            @SuppressWarnings("deprecation")
            var bodyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f);
            root.addView(body, bodyLp);
        }
        var surface = new MinecraftSurfaceView(c);
        this.surfaceView = surface;
        surface.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        surface.setFocusable(true);
        surface.setFocusableInTouchMode(true);
        surface.setClickable(true);
        surface.setRenderer(new MinecraftSurfaceView.Renderer() {
            @Override
            public void onSurfaceChanged(int w, int h) {
                surfaceBufferW = w;
                surfaceBufferH = h;
                lastW = w;
                lastH = h;
                var mc = MinecraftClient.getInstance();
                host.dupeMuiBindSurface(mc, w, h);
            }

            @Override
            public void onDraw(DrawContext ctx, int w, int h, float partialTick, double scaleInv, float alpha) {
                int rw;
                int rh;
                if (surfaceBufferW > 0 && surfaceBufferH > 0) {
                    rw = surfaceBufferW;
                    rh = surfaceBufferH;
                } else {
                    int[] buf = MuiSurfaceBufferSize.resolveForDraw(surfaceView, w, h, scaleInv);
                    rw = buf[0];
                    rh = buf[1];
                }
                lastW = rw;
                lastH = rh;
                MinecraftClient mc = MinecraftClient.getInstance();
                double[] m = MuiSurfaceMouseMapper.windowToSurface(surfaceView, rw, rh, mc);
                screen.render(ctx, (int) m[0], (int) m[1], partialTick);
            }
        });
        surface.setOnTouchListener((v, e) -> handleTouch(v, e, screen));
        surface.setOnGenericMotionListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                float dy = e.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (dy != 0) {
                    double mx = localX(v, e.getX());
                    double my = localY(v, e.getY());
                    return screen.mouseScrolled(mx, my, 0, dy);
                }
            }
            return false;
        });
        surface.setOnKeyListener((v, k, e) -> handleKey(e, screen));
        body.addView(surface);
        surface.setZ(0f);
        surface.post(surface::requestFocus);
        return root;
    }

    private boolean handleKey(KeyEvent e, Screen screen) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        int kc = e.getKeyCode();
        if (kc == KeyEvent.KEY_ESCAPE) {
            if (kind == Kind.SOCIAL) {
                MinecraftClient.getInstance().setScreen(returnScreen);
            } else {
                screen.close();
            }
            return true;
        }
        var ki = GuiClickHelper.muiToKey(e.getKeyCode(), e.getScanCode(), e.getModifiers());
        if (screen.keyPressed(ki)) {
            return true;
        }
        if (e.getMappedChar() != 0) {
            return screen.charTyped(GuiClickHelper.charIn(e.getMappedChar(), e.getModifiers()));
        }
        return false;
    }

    private double localX(View v, float x) {
        if (lastW <= 0) {
            return x;
        }
        return MathHelper.clamp((double) x, 0.0, (double) Math.max(0, lastW - 1));
    }

    private double localY(View v, float y) {
        if (lastH <= 0) {
            return y;
        }
        return MathHelper.clamp((double) y, 0.0, (double) Math.max(0, lastH - 1));
    }

    private boolean handleTouch(View v, MotionEvent e, Screen screen) {
        int a = e.getActionMasked();
        double mx = localX(v, e.getX());
        double my = localY(v, e.getY());
        if (a == MotionEvent.ACTION_SCROLL) {
            float dy = e.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (dy != 0) {
                return screen.mouseScrolled(mx, my, 0, dy);
            }
        }
        int glfw = glfwFor(e);
        if (a == MotionEvent.ACTION_DOWN) {
            pointerDown = true;
            activeButton = glfw;
            lastDragX = mx;
            lastDragY = my;
            return screen.mouseClicked(GuiClickHelper.at(mx, my, glfw), false);
        }
        if (a == MotionEvent.ACTION_MOVE) {
            if (pointerDown) {
                double dx = mx - lastDragX;
                double dy2 = my - lastDragY;
                lastDragX = mx;
                lastDragY = my;
                return screen.mouseDragged(GuiClickHelper.at(mx, my, glfw), dx, dy2);
            }
        }
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            boolean r = screen.mouseReleased(GuiClickHelper.at(mx, my, glfw));
            pointerDown = false;
            return r;
        }
        return false;
    }

    private int glfwFor(MotionEvent e) {
        if (e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
            return GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        }
        if (e.isButtonPressed(MotionEvent.BUTTON_TERTIARY)) {
            return GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        }
        return GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (kind == Kind.MACRO) {
            MacroEditorMuiGate.open = true;
        }
        MuiSurfaceHostTick.set(this::hostTick);
    }

    private void hostTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        if (host instanceof SocialScreen s) {
            s.tick();
        } else if (host instanceof MacroEditorScreen m) {
            m.tick();
        }
    }

    @Override
    public void onStop() {
        if (kind == Kind.MACRO) {
            MacroEditorMuiGate.open = false;
        }
        MuiSurfaceHostTick.set(null);
        super.onStop();
    }
}
