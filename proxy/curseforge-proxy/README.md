# CurseForge proxy

A tiny Cloudflare Worker that lets StellarServerForge install CurseForge-sourced ZeroG Network
mods with **zero setup for end users** — no CurseForge API key ever ships inside the app.

## Why this exists

CurseForge's API requires a personal API key on every request. Baking that key into a publicly
distributed jar isn't real protection — anyone can extract it from the binary, and CurseForge's
terms expect keys to be used server-side, not embedded in redistributed client software.

This Worker holds the real key as a secret on Cloudflare's servers, never sent to any client. The
app calls this Worker's `/v1/mods/{modId}/files` endpoint instead of `api.curseforge.com` directly;
the Worker adds the key server-side and returns the same JSON shape CurseForge's own API returns.

The actual mod jar download still goes straight from the user's machine to CurseForge's CDN — that
`downloadUrl` is a public link that doesn't need the API key, so this Worker never proxies large
binary downloads (keeps it fast and free-tier friendly).

## One-time setup

1. Install [wrangler](https://developers.cloudflare.com/workers/wrangler/install-and-update/) and
   log in to your Cloudflare account (needs to be one with access to `zerognetwork.co.za`'s DNS, or
   any account if you're fine using the default `*.workers.dev` subdomain instead):
   ```
   npm install -g wrangler
   wrangler login
   ```

2. From this folder, set your real CurseForge API key as a Worker secret (never written to any
   file, never committed):
   ```
   wrangler secret put CF_API_KEY
   ```
   Get a key from <https://console.curseforge.com/> if you don't have one yet.

3. Deploy:
   ```
   wrangler deploy
   ```
   This prints the Worker's URL (something like `curseforge-proxy.<your-subdomain>.workers.dev`).

4. (Recommended) Attach a custom route on your own domain instead of the workers.dev subdomain —
   see the comment in `wrangler.toml` — so the URL is stable and on brand
   (`curseforge-proxy.zerognetwork.co.za`).

5. Tell the app where to find it: update `DEFAULT_PROXY_BASE_URL` in
   `src/main/java/com/zerog/stellarserverforge/zerogmods/CurseForgeInstallService.java` to your
   deployed Worker URL, then rebuild the jar. Every copy of the app built after that point will use
   the proxy automatically — no per-user configuration needed.

## Operating it

- **Rotate the key** any time with `wrangler secret put CF_API_KEY` again — no redeploy of the app
  needed, since the app never sees the key.
- **Rate limiting**: the app self-throttles to 50 CurseForge calls/hour per install of the app (see
  `RateLimiter` in the Java source), but that's a courtesy limit, not a security boundary — add a
  Cloudflare Rate Limiting rule on this Worker's route (dashboard → your domain → Security → WAF →
  Rate limiting rules) to cap requests per IP as the real backstop.
- **Cost**: metadata-only JSON responses are tiny and cached for 5 minutes at the edge
  (`cache-control: public, max-age=300` in the Worker response), so this comfortably fits Cloudflare
  Workers' free tier for any realistic StellarServerForge user base.
- **If the proxy is ever down**: users can still add their own personal CurseForge API key in the
  ZeroG Network Mods dialog, which bypasses the proxy entirely and talks to CurseForge directly with
  their own key — that override path still exists in the app for exactly this situation.
