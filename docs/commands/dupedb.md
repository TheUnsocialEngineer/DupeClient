# DupeDB commands

## `/dupedb scan`

Starts a full plugin probe scan using command completion.

```
/dupedb scan
```

Example workflow:

1. Join a server.
2. Run `/dupedb scan`.
3. Wait for chat status lines showing discovered plugins.
4. If authenticated, matching DupeDB exploits are listed.

## `/dupedb plugins`

Same as scan but focused on plugin-list style probes.

```
/dupedb plugins
```

## `/dupedb login`

Opens the DupeDB OAuth flow in your browser.

```
/dupedb login
```

Complete login in the browser. Token is stored under `config/dupeclient/`.

## `/dupedb status`

```
/dupedb status
```

Prints whether OAuth is connected.

## `/dupedb revoke`

Clears stored token and opens DupeDB settings.

```
/dupedb revoke
```

## `/dupedb token`

Set a DupeDB personal access token directly.

```
/dupedb token dupedb_pat_abc123...
```

## `/dupedb appid`

Set the OAuth app slug (default `dupeclient` when blank).

```
/dupedb appid dupeclient
```

## `/dupedb developer`

Opens DupeDB developer settings in the default browser.

```
/dupedb developer
```

## `/dupedb mode`

Switch DupeDB behavior:

```
/dupedb mode auto      Auto-scan when server context changes
/dupedb mode command   Manual scans only
```

## Related

- [DupeDB module guide](../modules/dupedb.md)
- [P2W commands](p2w.md)
