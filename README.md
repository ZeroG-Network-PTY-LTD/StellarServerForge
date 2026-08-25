# StellarServerForge

A native Java/Swing desktop app for setting up, launching, and maintaining modded Minecraft
servers — Forge, NeoForge, Fabric, Quilt, and Vanilla. It's a from-scratch reimplementation of a
legacy Windows batch-script tool ([`Universalator`](batfile/UNIVERSALATOR_SPEC.md)) as a proper
cross-platform desktop app, built by [ZeroG Network](https://github.com/ZeroG-Network-PTY-LTD).

No terminal menus, no manually editing JVM flags, no hunting down the right Java version by hand —
StellarServerForge handles version resolution, Java provisioning, port/UPnP setup, mod scanning,
and mod installs from a dark-themed in-window UI ("Nocturne"), and gets out of your way once the
server's running.

Full feature-by-feature documentation lives in **[`docs/wiki/`](docs/wiki/Home.md)** — this README
is the quick-start.

## Features

- **Setup wizard**: pick a Minecraft version (validated live against Mojang's manifest), a mod
  loader, a Java handling mode, and a RAM allocation — settings are persisted to `settings.json`
  next to the server.
- **Automatic Java provisioning**: detects a matching system JDK, or downloads and manages one via
  Adoptium — no manual JDK installs required. See [Java Provisioning](docs/wiki/Java-Provisioning.md).
- **Mod loader installs**: Forge, NeoForge, Fabric, and Quilt version resolution and installer
  automation, including the modern NeoForge legacy-vs-hotfix version scheme.
- **Live console + lifecycle control**: launch/stop the server process from the dashboard with
  streamed console output.
- **Networking tools**: port-conflict detection, UPnP port forwarding, firewall checks.
- **Mod tooling**: client-only mod scanner (Forge/NeoForge/Fabric/Quilt), MCreator mod detection,
  and a CurseForge instance importer.
- **ZeroG Network mods catalog**: a curated, zero-setup mod install screen backed by Modrinth's
  public API and a hardened CurseForge proxy — no personal API key required. See
  [ZeroG Mods Catalog](docs/wiki/ZeroG-Mods-Catalog.md).
- **Utilities**: server icon generator, server-pack ZIP export, `run.sh`/`run.bat` generation, and
  a purge tool.
- **Security-conscious by design**: encrypted secret storage (AES-256-GCM) for the optional
  personal CurseForge API key, path sanitization on every remote-sourced download, and rate
  limiting shared across the app and its proxy. See [Security](docs/wiki/Security.md).

## Requirements

- Java 21 (used both to build and, once packaged, to run the app — the setup wizard provisions a
  separate JDK for the *Minecraft server process* independently of this).
- Windows, macOS, or Linux. The native `.exe` packaging step is Windows-only; the shaded jar runs
  anywhere with a JRE.

## Getting started

```bash
git clone https://github.com/ZeroG-Network-PTY-LTD/StellarServerForge.git
cd StellarServerForge
./gradlew shadowJar
java -jar build/libs/StellarServerForge-<version>-all.jar
```

On Windows, you can instead build a native app image:

```powershell
./gradlew jpackage
```

which produces `build/jpackage/StellarServerForge/StellarServerForge.exe`.

Run it in an empty folder (or one where you want a server to live) — the setup wizard walks you
through the rest. See [Getting Started](docs/wiki/Getting-Started.md) for the full first-run walkthrough.

## Building from source

| Task                    | Command                 | What it does                                              |
|--------------------------|--------------------------|-------------------------------------------------------------|
| Compile                 | `./gradlew compileJava` | Compiles the app.                                          |
| Test                    | `./gradlew test`        | Runs the unit test suite (excludes live-network tests).    |
| Live network tests      | `./gradlew testLive`    | Runs tests that hit real services (Mojang, Modrinth, etc.). |
| Fat jar                 | `./gradlew shadowJar`   | Produces a runnable `StellarServerForge-<version>-all.jar`. |
| Windows native app      | `./gradlew jpackage`    | Produces `StellarServerForge.exe` via `jpackage`.           |
| Full build              | `./gradlew clean build` | Compile + test + package.                                   |

## Project layout

```
src/main/java/com/zerog/stellarserverforge/
  gui/            Swing UI — MainFrame + CardLayout screens, gui/theme/ has the Nocturne design system
  model/          Plain data types (ServerSettings, ModLoader, McVersion, JavaOverrideMode)
  settings/       settings.json / server.properties / eula.txt persistence, encrypted secret storage
  mojang/         Version manifest + vanilla server jar install
  modloader/      Forge/NeoForge/Fabric/Quilt version resolution + installers
  javamanaged/    Java version rules + system detection + Adoptium provisioning
  launch/         JVM arg construction + server process lifecycle
  net/            HTTP fetcher, checksums, rate limiting
  net_port/       Port conflict detection, UPnP, firewall checks
  mods/           Client-only mod scanning, MCreator detection
  zerogmods/      ZeroG Network mod catalog + Modrinth/CurseForge install services
  curseforge/     CurseForge profile import
  utility/        Icon generator, server-pack export, run script generation, purge
```

Full architecture notes are in [Architecture](docs/wiki/Architecture.md).

## Documentation

The wiki lives in this repo at [`docs/wiki/`](docs/wiki/Home.md) so it's versioned alongside the
code (and can be synced to the GitHub wiki). Start at [Home](docs/wiki/Home.md).

## Contributing

Issues and PRs are welcome. Read [Architecture](docs/wiki/Architecture.md) and
[Security](docs/wiki/Security.md) first if you're touching the ZeroG mods or networking code.

## License

See [`LICENSE`](LICENSE) if present in this repository, or contact ZeroG Network for licensing terms.
