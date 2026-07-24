# Packet Utils

Packet queue, fabricator, and sniffer tools.

![Packet Utils panel]({{ site.baseurl }}/assets/screenshots/panel-packet-utils.png)

## Subsystems

### Core queue

Delay, desync, and advanced module toggles in the hub panel. Configure packet kinds and per-tick limits.

### Packet Fabricator

Build inventory click packets from an overlay.

![Packet fabricator overlay]({{ site.baseurl }}/assets/screenshots/overlay-fabricator.png)

- Fabricate tab: slot, action, times, send/queue.
- Delay tab: packet delay settings.
- Presets: save/load fabricator state from hub panel (Packet Utils > Fabricator section).

### Packet Sniffer

Log, filter, block, replay, and diff packets.

![Packet sniffer overlay]({{ site.baseurl }}/assets/screenshots/overlay-sniffer.png)

Right-click a log line:

| Menu item | Action |
|-----------|--------|
| Repeat packet | Replay C2S entry |
| Send to fabricator | Open workbench prefilled |
| Create macro | Single-packet macro |
| Macro from all C2S | Batch macro from visible C2S |
| Compare to last session | Diff vs previous connection (S2C) |

## AC-aware scanning

When AC Audit has detected anticheat plugins, DupeDB probes prioritize those namespaces first.
