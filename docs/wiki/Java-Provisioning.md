# Java Provisioning

Minecraft server versions require specific Java major versions, and getting this wrong is one of
the most common reasons a modded server refuses to start. StellarServerForge resolves and (if
needed) provisions the right JDK automatically, independent of whatever Java the app itself is
running on.

## The version rule table

`JavaVersionRules.resolve(McVersion)` (in `javamanaged/`) maps a Minecraft version to a default
Java major version and a list of acceptable alternatives:

| Minecraft version        | Default Java | Also accepted |
|---------------------------|---------------|-----------------|
| ≤ 1.16.4                 | 8             | —               |
| 1.16.5                   | 8             | 8, 11           |
| 1.17.x                   | 16            | —               |
| 1.18–1.19.x               | 17            | 17, 21, 25      |
| 1.20.0–1.20.4             | 17            | 17, 21, 25      |
| 1.20.5+                  | 21            | 21, 25          |
| 1.21.x                   | 21            | 21, 25          |
| Later (unreleased at time of writing) | 25 | — |

(1.20.5's jump to Java 21 ahead of the usual "one bump per Minecraft major version" cadence is
real — confirmed against Mojang's own 1.20.5 release notes, not a guess.)

## Java handling modes (`JavaOverrideMode`)

Chosen in the setup wizard, changeable later:

- **`AUTOMATIC`** (default) — `SystemJavaDetector` scans for an installed JDK matching the
  required major version; if none is found, falls back to `JavaProvisioningService` downloading
  and managing one via the Adoptium API.
- **`SYSTEM_PATH`** — always use whatever `java` resolves to on the OS `PATH`, no detection or
  provisioning. For users who manage their own JDKs and know what they're doing.
- **`FORCE_MANAGED`** — always use an app-managed Adoptium download, skipping system JDK detection
  entirely, even if a matching system JDK exists.

## Managed JDK downloads

When a managed JDK is needed, `JavaProvisioningService`:

1. Queries the Adoptium API for a build matching the required major version and current OS/arch.
2. Downloads the archive and verifies its checksum before extracting.
3. Extracts it into an app-managed directory (not system-wide — no admin rights required).
4. Re-checks staleness roughly every 6 months so a managed install doesn't silently drift onto an
   unsupported patch release forever.

`AdoptiumProvisioner` handles the actual download/extract/verify; its zip extraction includes a
zip-slip guard (path-normalizes each entry and confirms it stays under the destination directory
before writing) since it's unpacking a downloaded archive.
