# Mod Scanning

Reached from the dashboard's **Mods** screen (`ModsPanel`).

## Client-only mod scanner

Flags mods that only belong on the client, so they can be moved out of a dedicated server's `mods`
folder without breaking anything that depends on them server-side.

- **Fabric / Quilt** (`FabricQuiltModScanner`) — parses each mod's `fabric.mod.json` /
  `quilt.mod.json`, cross-checks declared environment (`client`/`server`/`*`) and dependencies, and
  keeps a mod if something else still depends on it even when the mod itself is client-only —
  reported separately in the UI as "kept because required as a dependency."
- **Forge / NeoForge** (`ForgeNeoForgeModScanner`) — parses each mod's `mods.toml` (via a real TOML
  parser, `com.electronwill.night-config`) and cross-references a fetched list of known
  client-only mod IDs (`ClientOnlyModListService`) rather than relying on TOML metadata alone,
  since not every mod declares its side accurately.

Flagged mods can be moved into a `CLIENTMODS` folder from the scan results list (multi-select
supported) so they're out of the way but not deleted.

## MCreator mod detection

`McreatorScanner` flags mods that were built with MCreator — these are a common source of
performance issues and instability on modded servers, so the scanner surfaces them with an
explanatory note rather than silently listing them alongside everything else.

## Plain folder listing

A simple "List mods folder" action for when you just want to see what's actually installed,
without running either scanner.
