# DupeDB

Plugin discovery, DupeDB OAuth, exploit matching, and P2W scoring.

![DupeDB panel](assets/screenshots/panel-dupedb.png)

![DupeDB overlay](assets/screenshots/overlay-dupedb.png)

## Hub panel

| Control | Purpose |
|---------|---------|
| Scan / overlay toggle | Start probe or show DupeDB overlay |
| Mode | Command vs auto scan |
| OAuth | Login, revoke, developer link |
| P2W score toggle | Weighted score after scan |
| Background rescans | Periodic rescans while connected |

## Overlay

| Action | Description |
|--------|-------------|
| Scan Now | Manual plugin probe |
| Generate macro | Build macro from observed commands/plugins |
| Probe delay slider | Milliseconds between completion probes |

## Typical workflow

1. Join server.
2. `/dupedb login` (once per machine).
3. `/dupedb scan` or enable auto mode.
4. Review exploit matches in chat.
5. Optional: `/p2w mark` after score and session requirements met.

## P2W join policy

When the registry lists the server:

- **P2W**: alert screen on join.
- **Non-P2W**: disclaimer and exploit modules disabled.

Server profile card on join shows P2W status and recent scan summary.

## Commands

[DupeDB commands](../commands/dupedb.md), [P2W commands](../commands/p2w.md).
