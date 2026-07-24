package com.dupeclient.client.module.hud;

import java.util.ArrayList;
import java.util.List;

public final class HudPersistedState {
    public boolean active = true;
    public HudSettings settings = new HudSettings();
    public List<HudElementState> elements = new ArrayList<>();
}
