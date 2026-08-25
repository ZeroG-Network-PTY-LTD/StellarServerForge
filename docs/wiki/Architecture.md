# Architecture

## Package layout

```
com.zerog.stellarserverforge
├── Main                 Entry point — launches the Swing EDT and MainFrame
├── gui/                 Swing UI
│   └── theme/            The "Nocturne" dark design system (StellarTheme, StellarButton, etc.)
├── model/                Plain data types: ServerSettings, ModLoader, McVersion, JavaOverrideMode
├── settings/             settings.json / server.properties / eula.txt persistence, encrypted secrets
├── mojang/                Mojang version manifest + vanilla server jar install
├── modloader/            Forge/NeoForge/Fabric/Quilt version resolution + installers
├── javamanaged/          Java version rules, system JDK detection, Adoptium provisioning
├── launch/               JVM argument construction + server process lifecycle
├── net/                  HTTP fetcher, checksums, rate limiting (shared infra)
├── net_port/             Port conflict detection, UPnP, firewall checks
├── mods/                 Client-only mod scanning, MCreator detection
├── zerogmods/            ZeroG Network mod catalog + Modrinth/CurseForge install services
├── curseforge/           CurseForge profile import (registry/manual folder detection)
└── utility/              Icon generator, server-pack ZIP export, run script generation, purge
```

## UI model: CardLayout, not modal dialogs

`MainFrame` hosts a single `CardLayout` (`gui/MainFrame.java`) that swaps between full-window
panels — the setup wizard, dashboard, settings, utilities, mods, and ZeroG mods screens are all
`JPanel`s added as cards, not separate `JDialog` windows. Each screen is reached via a callback
passed into the previous one (e.g. `DashboardPanel`'s constructor takes `onSettings`,
`onUtilities`, `onMods`, `onZeroGMods` runnables) and returns via a `Back` button that calls back
into `MainFrame`.

Most secondary screens (`SettingsPanel`, `UtilitiesPanel`, `ModsPanel`, `ZeroGModsPanel`) are
rebuilt fresh each time they're navigated to, rather than kept alive and hidden — cheap to
construct, and guarantees they always reflect current settings without a separate refresh path.
When a screen has in-flight background work (e.g. `ZeroGModsPanel`'s install `SwingWorker`),
`MainFrame` calls a `cancelPendingWork()`-style hook before discarding the old instance so
background work doesn't keep running against a detached panel.

## Background work: SwingWorker

Anything that blocks (HTTP calls, downloads, process I/O) runs on a `SwingWorker`, never the EDT.
UI updates happen in `done()`/`process()`, which run back on the EDT automatically. The server
process's console output is streamed into the dashboard via a background reader thread feeding a
`SwingWorker`'s `publish`/`process` pipeline.

## Settings and persistence

- `settings.json` — the server's persisted configuration (`ServerSettings`, via `SettingsService`),
  stored next to the server files. JSON, not an executable script, unlike the legacy
  `settings-universalator.txt` this replaces.
- `server.properties` — read/created/repaired by `ServerPropertiesService`.
- `eula.txt` — accepted in-app via `EulaService`, not by manually editing a file.
- A per-user secret key at `~/.stellarserverforge/secret.key`, outside the repo and outside the
  server directory, used to encrypt the optional personal CurseForge API key field in
  `settings.json` (see [Security](Security.md)).

## Data flow: installing a mod loader

1. `ModLoaderMetadataService` fetches version metadata (Maven XML for Fabric/Quilt/NeoForge,
   `promotions_slim.json` for Forge).
2. `ModLoaderVersionResolver` picks the right version for the selected Minecraft version,
   including NeoForge's dual legacy-vs-hotfix numbering scheme.
3. `ForgeNeoForgeInstaller` / `FabricQuiltInstaller` download and run the installer, verifying
   checksums.
4. `ModLoaderLaunchLine` builds the correct launch command for that loader (including
   `win_args.txt`/`user_jvm_args.txt` handling where applicable).
5. `LaunchArgsBuilder` + `ServerProcessRunner` combine loader launch args with JVM/RAM args and
   start the process.

## Data flow: installing a ZeroG mod

See [ZeroG Mods Catalog](ZeroG-Mods-Catalog.md) for the full picture — in short,
`ZeroGModCatalogService` fetches a JSON catalog, `ModrinthInstallService` /
`CurseForgeInstallService` resolve and download the actual file (through a proxy for CurseForge,
so no personal API key is required), and both sanitize the remote-reported file name before
writing anywhere under `mods/`.
