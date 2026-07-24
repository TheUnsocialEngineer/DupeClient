# Command reference

All commands are **client-side**. They run in chat locally and are not sent to the server (except where a command intentionally sends packets as part of normal gameplay).

## Index

| Command | Page |
|---------|------|
| `/dupedb` | [DupeDB commands](dupedb.md) |
| `/p2w` | [P2W commands](p2w.md) |
| `/dupeclient macro` | [Macro commands](macro.md) |
| `/hud` | [HUD commands](hud.md) |
| `/vault` | [Vault commands](vault.md) |
| `/server`, `/serversearch` | [Server commands](server.md) |
| `/looknbt`, `/nbtedit`, `/dupe` | [Utility commands](utility.md) |

## Exploit-gated commands

On servers where exploit modules are locked (non-P2W policy or roster restrictions), these commands return without running:

- `/dupedb scan`, `/server plugins`, `/serversearch`
- `/dupeclient macro run`, `studio`, `prompt`, `generate`, import/export variants
- `/p2w mark`

HUD, vault, and read-only macro commands (`list`, `folder`, `stop`) stay available unless blocked elsewhere.
