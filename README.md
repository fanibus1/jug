# jug

## Version

Fabric Minecraft 1.20.1 for now...

## Description

Adds `/repc <command>`, which prepends `/<command>` to the chat every time it is opened with the "Open Chat" hotkey, until `/repc reset` or a new prefix is set.

Available commands:
  - `/repc <command>` - Prepends `/<command>` to all following chat messages until changed or reset.
  - `/repc reset` - Clear prefix.
  - `/repc -m <command>` - Explicit mode (same as `/repc <command>`).
  - `/repc -r <text>` - Raw mode, prepend `<text>` WITHOUT a leading slash.