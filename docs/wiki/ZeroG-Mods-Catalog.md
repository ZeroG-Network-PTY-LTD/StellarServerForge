# ZeroG Mods Catalog

The "ZeroG Network mods" screen is a curated, **zero-setup** mod install flow — no CurseForge
account or API key required for any user, unlike installing CurseForge mods any other way.

## How it works

1. `ZeroGModCatalogService` fetches a catalog JSON file — by default,
   [`zerog-mods-catalog.json`](../../zerog-mods-catalog.json) in this repo, served raw from
   GitHub. Each entry validates independently (name/source/projectId checked); a malformed entry
   is skipped with a logged reason rather than failing the whole catalog load, and the last
   successfully fetched catalog is cached so a transient network failure doesn't leave the screen
   empty.
2. Each catalog entry names a `source` (`MODRINTH` or `CURSEFORGE`) and a `projectId`.
3. Selecting an entry and clicking **Install selected** resolves and downloads the right file for
   your configured Minecraft version and mod loader:
   - **Modrinth** (`ModrinthInstallService`) — calls Modrinth's public API directly; no
     authentication needed at all.
   - **CurseForge** (`CurseForgeInstallService`) — goes through ZeroG Network's hosted proxy by
     default (`https://sfs.zerognetwork.co.za`, see [`proxy-php/`](../../proxy-php/README.md)),
     which holds the real CurseForge API key server-side. The app-side rate limiter caps usage at
     50 calls/hour independent of whatever the proxy or CurseForge itself enforces.
4. The resolved file's SHA-1 (or MD5 fallback, for CurseForge) is verified against the published
   hash before the file is kept; a mismatch deletes the download and fails the install rather than
   leaving a possibly-corrupt jar in `mods/`.
5. The reported file name from either API is sanitized to its bare file name (`Path.getFileName()`,
   rejecting blank/`.`/`..`) before being resolved against `mods/` — see [Security](Security.md).

## When CurseForge can't provide a direct download

Some mod authors disable "Allow Third Party" downloads on CurseForge — when that's set, the
CurseForge API returns no `downloadUrl` for any file, and the install fails with an explanatory
message. This is intentional on the author's part, not a bug or a gap in the app: StellarServerForge
does not attempt to reconstruct CurseForge's CDN URL from the file ID to route around it, since
doing so would defeat the author's explicit choice.

When this happens, the app offers a one-click prompt to open the mod's page in your browser so you
can download it manually instead. If a Modrinth listing for the same mod exists in the catalog
(with no such restriction), that's the more reliable path — see the catalog for examples.

## Configuring your own catalog or proxy

Both are configurable from Settings → "ZeroG mods connection":

- **Catalog URL** — point at a different catalog JSON if you've forked the app for a different
  mod set/org.
- **CurseForge proxy endpoint** — point at your own deployed proxy (see
  [`proxy-php/`](../../proxy-php/README.md) or the Cloudflare Workers version in
  [`proxy/`](../../proxy/README.md)) instead of ZeroG's.
- **Personal CurseForge API key** (optional) — bypasses any proxy and talks to CurseForge
  directly with your own key. Stored encrypted (see [Security](Security.md)).
