# Utilities

Reached from the dashboard's **Utilities** screen (`UtilitiesPanel`).

## Server icon generator

`IconGeneratorService` generates a `server-icon.png` for the server (Minecraft's client-visible
server list icon) via `Graphics2D`/`BufferedImage`/`ImageIO` — a native Java replacement for the
legacy tool's `System.Drawing`-based generator. Two modes:

- **Generate default icon** — a built-in default design.
- **Generate custom icon** — build from your own source image.

## Server pack export

`ServerPackZipService` exports a ZIP of the server's mod/config set (via
`java.util.zip.ZipOutputStream`) suitable for distributing to players who need the matching client
pack.

## Run script generation

`RunScriptGeneratorService` writes `run.sh` and `run.bat` launch scripts for the server, matching
the JVM/loader arguments StellarServerForge itself uses to launch — useful for running the server
outside the app (e.g. on a remote host) with the same configuration.

## Purge

`PurgeService` deletes the installed server jar(s), mod loader `libraries/`/`.fabric` folders, and
cached/downloaded installer, Java, and version metadata — forcing a full re-download and reinstall
on next launch. It never touches your mods, configs, world saves, or `settings.json`; this is a
"force a clean reinstall" tool, not a full server reset. Still a real, non-trivial action, so it's
reached via a clearly marked danger-styled button and should only be used when you actually want
to force everything to redownload.
