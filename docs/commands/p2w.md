# P2W commands

Community registry marks for pay-to-win and non-P2W servers. Marks require verification (recent DupeDB scan, session time, score thresholds).

## `/p2w mark`

Start a verified P2W mark request for the current server.

```
/p2w mark
```

Follow chat prompts, then confirm:

```
/p2w confirm mark
```

## `/p2w unmark`

Remove a P2W mark (or request non-P2W listing removal depending on registry state).

```
/p2w unmark
/p2w confirm unmark
```

## `/p2w abort`

Cancel a pending mark or unmark.

```
/p2w abort
```

## Requirements (typical)

| Check | Purpose |
|-------|---------|
| DupeDB scan within 24h | Evidence of plugins on this server |
| 5+ minutes on server | Reduces drive-by marking |
| P2W score thresholds | Mark needs score >= 20% and plugins; unmark needs low score |

See [DupeDB module](../modules/dupedb.md) for scan and score setup.
