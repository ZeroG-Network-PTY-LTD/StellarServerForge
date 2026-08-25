# Security

## Secret storage

The optional personal CurseForge API key (see [ZeroG Mods Catalog](ZeroG-Mods-Catalog.md)) is
stored encrypted at rest, not as plaintext in `settings.json`:

- Encryption: AES-256-GCM.
- Key location: `~/.stellarserverforge/secret.key`, generated on first use — outside the repo and
  outside the server directory, so it isn't accidentally committed, shipped in a server pack
  export, or included when copying a server folder around.
- In memory, the value is always plaintext (`ServerSettings.getCurseForgeApiKey()`); only the
  on-disk `settings.json` copy is ciphertext, via `EncryptedStringSerializer`/
  `EncryptedStringDeserializer`.

For the CurseForge proxy's own key handling (server-side, not this app), see
[`proxy-php/README.md`](../../proxy-php/README.md) — it documents where the real API key lives on
the hosting side and the tradeoffs of each option.

## Path sanitization on downloads

Every install path that resolves a remote-reported file name against a local directory sanitizes
that name first, since trusting it unmodified would let a compromised/malicious response write
outside the intended directory (`../` traversal or an absolute-path override):

- `ModrinthInstallService` / `CurseForgeInstallService` — reduce the API-reported file name to its
  bare `Path.getFileName()` component before resolving against `mods/`, rejecting blank/`.`/`..`
  results and catching `InvalidPathException` (which also blocks embedded-colon tricks like NTFS
  alternate data streams on Windows).
- `AdoptiumProvisioner`'s zip extraction — normalizes each zip entry's path and confirms it stays
  under the destination directory before writing (a "zip-slip" guard).

Mod loader version strings from Forge/Fabric/Quilt/NeoForge's own official Maven metadata are
*not* separately sanitized before path use — exploiting that would require compromising one of
those upstream hosts, which the app already implicitly trusts to serve the installer jar it's
about to execute, so it isn't a materially different trust boundary.

CurseForge profile import (`CurseForgeImportService`) copies from a local, user-selected folder
(detected via the Windows registry or manually browsed), not a remote source — a different trust
boundary from the download paths above.

## Rate limiting

`RateLimiter` (in `net/`) enforces a sliding-window call cap, persisted to a small state file so
the window survives app restarts. Used to cap CurseForge API usage (50 calls/hour by default)
independent of whatever limit CurseForge or the proxy itself enforces, so a bug or accidental loop
in the app can't hammer either into throttling everyone else.

It's safe across both processes and threads:
- **Across processes** — an exclusive `FileLock` spans the whole read-modify-write, matching the
  PHP proxy's own `flock()` use for the same file-based coordination.
- **Within a process** — `FileLock` isn't re-entrant-safe (two channels in the same JVM racing for
  an overlapping lock throw `OverlappingFileLockException` instead of blocking), so a static,
  path-keyed lock map serializes same-JVM access before any thread attempts `channel.lock()`.

## Checksums

Every downloaded file that has a published hash gets verified before being trusted:

- Vanilla server jars — SHA-1 against Mojang's manifest.
- Modrinth files — SHA-1 against Modrinth's published hash.
- CurseForge files — SHA-1 (preferred) or MD5 (fallback) against CurseForge's published hash.
- Mod loader installers — checksum-verified before being executed.

A mismatch deletes the downloaded file and fails the operation with a clear error, rather than
silently keeping a possibly-corrupted or tampered file.

## The CurseForge "no third-party downloads" setting

Not a security issue in this app, but worth stating plainly since it comes up: when a mod author
disables third-party downloads on CurseForge, the API returns no download URL for that mod's
files. StellarServerForge does not work around this by reconstructing CurseForge's CDN URL pattern
from the file ID — doing so would deliberately defeat the author's own opt-out choice. See
[ZeroG Mods Catalog](ZeroG-Mods-Catalog.md) for what the app does instead.
