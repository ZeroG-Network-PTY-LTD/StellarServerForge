# Getting Started

## Installing

**Option 1 — download a release.** Every push to `main` builds a fresh
`StellarServerForge-<version>-all.jar` and attaches it to a GitHub Release (see
[Building and CI](Building-and-CI.md)). Grab the latest one from the repo's Releases page.

**Option 2 — build from source:**

```bash
git clone https://github.com/ZeroG-Network-PTY-LTD/StellarServerForge.git
cd StellarServerForge
./gradlew shadowJar
```

The jar is produced at `build/libs/StellarServerForge-<version>-all.jar`.

**Option 3 — Windows native app:** `./gradlew jpackage` produces
`build/jpackage/StellarServerForge/StellarServerForge.exe`, no separate Java install needed for
end users (it bundles its own runtime image via `jpackage`).

## Where to run it

Run the jar/exe **in the folder you want the server to live in** — or, if you launched a
packaged (jar/exe) build, StellarServerForge uses the folder the jar/exe itself sits in, not your
terminal's working directory. That's so double-clicking it from a shortcut with a different
"start in" folder still lands `settings.json`, `mods/`, `server.properties`, and the world data in
the right place. Running via `./gradlew run` or an IDE instead uses the working directory — so
build output doesn't get treated as a real server folder.

## First run — the setup wizard

If no `settings.json` exists in the server folder yet, StellarServerForge opens the setup wizard
instead of the dashboard:

1. **Minecraft version** — validated live against Mojang's official version manifest (release
   versions only). An invalid or unreleased version is flagged inline before you can continue.
2. **Mod loader** — Forge, NeoForge, Fabric, Quilt, or Vanilla.
3. **Java handling** — see [Java Provisioning](Java-Provisioning.md) for what each mode does:
   - *Automatic* (default) — detect a matching system JDK, or download/manage one via Adoptium.
   - *System PATH* — always use whatever `java` resolves to on PATH.
   - *Force managed* — always use an app-managed Adoptium download, skipping system detection.
4. **RAM allocation** — how much memory to give the server JVM.

Completing the wizard writes `settings.json` into the server folder and opens the dashboard.
Re-opening StellarServerForge in the same folder later skips straight to the dashboard.

## The dashboard

The dashboard is the main screen once setup is done:

- **Launch server / Stop server** — starts/stops the Minecraft server process, with live console
  output streamed into the window. Stop attempts a graceful shutdown first.
- **Mods** — the client-only mod scanner and MCreator detector (see [Mod Scanning](Mod-Scanning.md)).
- **ZeroG Network mods** — the curated, zero-setup mod catalog (see [ZeroG Mods Catalog](ZeroG-Mods-Catalog.md)).
- **Import CurseForge profile** — import an existing CurseForge instance's mods into this server.
- **Utilities** — icon generation, server-pack export, run script generation, purge (see [Utilities](Utilities.md)).
- **Settings** — re-open configuration (RAM, mod loader version, UPnP, firewall check, ZeroG mods
  connection endpoints) without re-running the whole wizard.

## Re-running setup

The Settings screen has a "Re-run setup wizard" button if you want to change the Minecraft
version or mod loader from scratch, rather than editing individual settings.
