/**
 * ZeroG Network CurseForge proxy.
 *
 * Holds the real CurseForge API key as a Worker secret, server-side only — it never ships to
 * StellarServerForge users. The app calls this Worker instead of api.curseforge.com directly, so
 * CurseForge mod installs "just work" with zero setup for anyone using the app, without a shared
 * secret ever being embedded in the distributed jar.
 *
 * Only forwards the metadata lookup (GET /v1/mods/{modId}/files). The actual mod jar download
 * (the "downloadUrl" in CurseForge's response) is a public CDN link that does NOT require the API
 * key, so the app downloads the file directly from CurseForge's CDN — this Worker never proxies
 * large binary downloads, keeping it cheap and fast on Cloudflare's free tier.
 *
 * Deploy: see README.md in this folder.
 */

const CURSEFORGE_API_BASE = "https://api.curseforge.com";

// Only these query params are ever forwarded — never anything else a caller might try to smuggle
// through (e.g. no arbitrary path/host injection).
const ALLOWED_PARAMS = new Set(["gameVersion", "modLoaderType", "pageSize", "index"]);

export default {
  async fetch(request, env) {
    if (request.method !== "GET") {
      return json({ error: "Method not allowed" }, 405);
    }

    const url = new URL(request.url);
    const match = url.pathname.match(/^\/v1\/mods\/(\d+)\/files$/);
    if (!match) {
      return json({ error: "Not found" }, 404);
    }
    const modId = match[1];

    if (!env.CF_API_KEY) {
      return json({ error: "Proxy is not configured (missing CF_API_KEY secret)" }, 500);
    }

    const upstream = new URL(`${CURSEFORGE_API_BASE}/v1/mods/${modId}/files`);
    for (const [key, value] of url.searchParams) {
      if (ALLOWED_PARAMS.has(key)) {
        upstream.searchParams.set(key, value);
      }
    }

    const upstreamResponse = await fetch(upstream.toString(), {
      headers: {
        "x-api-key": env.CF_API_KEY,
        "Accept": "application/json",
      },
    });

    const body = await upstreamResponse.text();
    return new Response(body, {
      status: upstreamResponse.status,
      headers: {
        "content-type": "application/json",
        // Same-origin desktop app has no browser CORS concerns, but keep this open in case a
        // future web client wants to hit the proxy too.
        "access-control-allow-origin": "*",
        "cache-control": "public, max-age=300",
      },
    });
  },
};

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json" },
  });
}
