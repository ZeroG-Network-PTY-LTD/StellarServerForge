# CurseForge proxy (PHP / cPanel) — hardened

A small PHP script that lets StellarServerForge install CurseForge-sourced ZeroG Network mods with
**zero setup for end users** — no CurseForge API key ever ships inside the app.

**Honest caveat first**: nothing running on shared hosting can make a secret *literally*
unobtainable — anyone with control of the hosting account (you, or an attacker who compromises it)
can always read it, same as any server-side secret anywhere. What the setup below *does* eliminate
is every remote/accidental leak path: the key can't be fetched over HTTP, can't leak via a
misconfigured `.htaccess`, can't leak via a stray backup file, and isn't in git.

(There's also a Cloudflare Workers version in `../proxy/curseforge-proxy/` if you'd rather run it
there — the two are independent; only deploy the one you're using, and point the app at it.)

## Where the key lives — pick one, ranked strongest first

`curseforge.php` (via `lib.php`) checks these in order and uses the first one it finds:

1. **`CF_API_KEY` environment variable** — strongest option, because the key never exists as a file
   at all. If your cPanel host offers "Setup Node.js/PHP App" environment variables, or you can add
   `SetEnv CF_API_KEY "..."` to the Apache vhost config (not `.htaccess` — that would put it back in
   a file), use this.
2. **A config file OUTSIDE the web root**, at `curseforge-proxy-secret/config.php`, two directories
   above wherever `curseforge.php` lives. On a typical cPanel layout that's outside `public_html`
   entirely — a path Apache/LiteSpeed will never serve over HTTP no matter what, regardless of
   `.htaccess`. **This is the recommended option** if environment variables aren't available.
3. **`config.php` in the same folder as `curseforge.php`** — last resort. Still blocked by
   `.htaccess`, but that's one server misconfiguration away from being servable. The script logs a
   warning (to your PHP error log) every time this fallback is used, as a nudge to move it.

## Requirements

Any standard shared cPanel host, with PHP 8.1+ and the `curl` extension (on by default almost
everywhere). If your host is on PHP 7.4, drop `declare(strict_types=1)` and the `: never` return
type hints — everything else is compatible.

## One-time setup

1. In cPanel → **Subdomains**, create `sfs` pointing at `zerognetwork.co.za`, with a document root
   like `public_html/sfs`.

2. Upload every file in this folder to that document root: `index.php`, `curseforge.php`,
   `lib.php`, `config.example.php`, `.htaccess`.

3. Set the key using **option 2 above** (recommended): on the server, one level above
   `public_html` (i.e. your account's home directory, NOT inside anything web-served), create:
   ```
   curseforge-proxy-secret/config.php
   ```
   containing:
   ```php
   <?php
   return ['CF_API_KEY' => 'your-real-key-here'];
   ```
   Get a key from <https://console.curseforge.com/>. Set its file permissions to `600` (owner
   read/write only) via File Manager or your SFTP client's "Permissions" dialog.

   If you'd rather use an environment variable (option 1), set `CF_API_KEY` there instead and skip
   this step. Either way, **never** put the real key in `config.php` inside `public_html/sfs/` if
   you can avoid it — that's the weakest of the three options.

4. Visit `https://sfs.zerognetwork.co.za/` in a browser — a status card should show "configured"
   with a green dot. If it says "NOT configured," double check the key is in one of the three
   locations above with a non-empty `CF_API_KEY`.

5. Tell the app where to find it: update `DEFAULT_PROXY_BASE_URL` in
   `src/main/java/com/zerog/stellarserverforge/zerogmods/CurseForgeInstallService.java` to
   `https://sfs.zerognetwork.co.za`, then rebuild the jar.

## What's hardened, and why

- **Key never reachable over HTTP** when using option 1 or 2 above — not a permissions question,
  the file/value simply isn't inside anything a web request can traverse to.
- **`.htaccess`** blocks direct access to any `config*.php` file (and backup variants like
  `.bak`/`.old`/`~`) as a backstop, disables directory listing, and blocks every `.php` file in the
  folder except the two real entry points (`index.php`, `curseforge.php`) — so a stray editor
  backup or an accidentally-uploaded extra file can't be hit directly either.
- **No error leakage**: `curseforge.php` forces `display_errors` off and never echoes upstream
  cURL error text (which could carry internal paths) — errors go to the server's PHP error log
  instead, out of any HTTP response.
- **Per-IP rate limiting** (120 requests/hour/IP by default, in `curseforge.php`) — a real backstop
  independent of who or what is calling the endpoint, on top of the app's own 50/hour client-side
  self-throttle. Limits blast radius even if the URL is ever discovered/scraped.
- **Config never in git**: `config.php` and the outside-webroot folder are both outside version
  control; `config.example.php` (a placeholder with no real value) is the only one committed.

## Operating it

- **Rotate the key**: edit wherever you put it (env var or the outside-webroot config file) — no
  app rebuild needed, since the app never sees the key.
- **If the proxy is ever down**: users can still add their own personal CurseForge API key in the
  ZeroG Network Mods dialog, bypassing the proxy entirely.
- **Logs**: check your cPanel PHP error log if `curseforge.php` returns 500s — most common causes
  are the key not being found in any of the three locations, or the `curl` extension being off.
