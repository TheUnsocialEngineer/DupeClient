# Macro commands

Macros are registered under `/dupeclient macro` (not `/macro` alone).

## List and run

```
/dupeclient macro list
/dupeclient macro run my_macro
/dupeclient macro stop
```

## Manage files

```
/dupeclient macro delete old_macro
/dupeclient macro folder
```

`folder` prints the path to `config/dupeclient/macros/`.

## Studio (editor)

```
/dupeclient macro studio
/dupeclient macro studio my_macro
```

Same as pressing **F7** with an optional load id.

## Prompt generation

```
/dupeclient macro prompt
/dupeclient macro prompt walk forward and jump
/dupeclient macro generate demo "open inventory and click slot 0"
```

## Import and export

```
/dupeclient macro export my_macro
/dupeclient macro import
/dupeclient macro importclip
/dupeclient macro importclip custom_id
/dupeclient macro importoverwrite my_macro
/dupeclient macro importjson {"id":"test","steps":[]}
/dupeclient macro importfile C:/path/to/macro.json
```

## Help

```
/dupeclient
/dupeclient macro
```

Prints the full subcommand list.

See [Macros module](../modules/macros.md).
