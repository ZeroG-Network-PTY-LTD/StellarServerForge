# StellarServerForge Wiki

StellarServerForge is a Java/Swing desktop app for setting up, launching, and maintaining modded
Minecraft servers (Forge, NeoForge, Fabric, Quilt, Vanilla). It replaces
[`Universalator`](../../batfile/UNIVERSALATOR_SPEC.md), a legacy Windows batch-script tool, with a
proper cross-platform GUI built by [ZeroG Network](https://github.com/ZeroG-Network-PTY-LTD).

This wiki is versioned in-repo at `docs/wiki/` so it stays in sync with the code it documents.

## Pages

- **[Getting Started](Getting-Started.md)** — installing, first run, the setup wizard.
- **[Architecture](Architecture.md)** — package layout, the CardLayout screen model, data flow.
- **[Java Provisioning](Java-Provisioning.md)** — how the app finds or downloads a matching JDK.
- **[Mod Loaders](Mod-Loaders.md)** — Forge/NeoForge/Fabric/Quilt version resolution and installs.
- **[ZeroG Mods Catalog](ZeroG-Mods-Catalog.md)** — the zero-setup Modrinth/CurseForge mod install screen.
- **[Networking](Networking.md)** — port conflicts, UPnP, firewall checks.
- **[Mod Scanning](Mod-Scanning.md)** — client-only mod detection and MCreator scanning.
- **[Utilities](Utilities.md)** — icon generation, server-pack export, run scripts, purge.
- **[Settings Reference](Settings-Reference.md)** — every field in `settings.json`.
- **[Security](Security.md)** — secret storage, path sanitization, rate limiting.
- **[Building and CI](Building-and-CI.md)** — Gradle tasks, packaging, the release/Discord pipeline.

## Quick links

- Repo: https://github.com/ZeroG-Network-PTY-LTD/StellarServerForge
- Legacy tool spec (what this app replaces): [`batfile/UNIVERSALATOR_SPEC.md`](../../batfile/UNIVERSALATOR_SPEC.md)
- CurseForge proxy: [`proxy-php/README.md`](../../proxy-php/README.md)
