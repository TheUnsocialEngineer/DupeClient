# Macros

Graph-based automation with hotkeys, queue, and studio editor.

![Macros panel](assets/screenshots/panel-macros.png)

![Macro studio](assets/screenshots/screen-macro-studio.png)

## Hub Play tab

| Control | Action |
|---------|--------|
| Run | Queue macro (runs sequentially via macro scheduler) |
| Hotkey bind | Click field and press key |
| Stop | Stop active macro |

Hotkey conflicts show a warning when two macros share the same key.

## Studio tab

Open editor, prompt generator, import/export shortcuts.

## Bridges

| Source | Action |
|--------|--------|
| DupeDB overlay | Generate macro from scan commands |
| Packet sniffer | Create macro from C2S packets |

## Baritone

Movement steps can use baritone-meteor when installed. Waypoints right-click **Path here** also uses Baritone.

## Commands

[/dupeclient macro commands](../commands/macro.md)
