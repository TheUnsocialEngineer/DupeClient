# Getting started

## Install

1. Install Fabric Loader for your Minecraft version (1.21.10, 1.21.11, or 26.1.x).
2. Install Fabric API in the `mods` folder.
3. Add the DupeClient jar built for your version.
4. Launch the game.

Optional: Mod Menu, baritone-meteor (macro and waypoint pathing).

## First session

1. Join a multiplayer server or open a singleplayer world for local testing.
2. Press **Right Control** to open the module hub.

![Module hub](assets/screenshots/hub.png)

3. Pick a module from the left rail (wide layout) or top pills (compact layout).
4. Toggle features and set hotkeys from each panel.
5. Press **F7** to open the macro studio.

## Hub sections

| Section | Panels |
|---------|--------|
| Research | DupeDB |
| Network | Packet Utils, PayAll, MCPTools |
| Automation | Macros |
| Interface | HUD, Social, Waypoints |
| Security | Security, AC Audit, Utility |

Some panels require roster verification on online servers. HUD and Security stay available while verification is pending.

## Overlays vs screens

**Overlays** float in-game while you play (DupeDB, sniffer, fabricator, etc.). Toggle them from hub panels or hotkeys.

**Screens** replace the current GUI (Social, Waypoints, macro studio, HUD editor). Open from hub buttons or commands.

## Online services

Social presence, P2W registry, and MCPTools bundle sync use remote APIs configured at build time. The mod jar does not include a server.

## Next steps

- Run a DupeDB scan: [DupeDB module](modules/dupedb.md)
- Read all commands: [Command index](commands/index.md)
