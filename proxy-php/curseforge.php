<?php
declare(strict_types=1);

/**
 * ZeroG Network CurseForge proxy (PHP/cPanel version) — hardened.
 *
 * Holds the real CurseForge API key server-side so StellarServerForge users can install
 * CurseForge-sourced mods with zero setup; the key never ships inside the distributed jar.
 *
 * Key resolution order (first one found wins) — see README.md "Where the key lives":
 *   1. CF_API_KEY environment variable (best: never touches disk as a file at all)
 *   2. A config file OUTSIDE the web root, at ../../curseforge-proxy-secret/config.php
 *      (i.e. not reachable by any HTTP request regardless of .htaccess/server config)
 *   3. config.php in this same folder (last resort — still .htaccess-blocked, but only a
 *      config mistake away from being servable; logged as a warning when used)
 *
 * Only forwards the metadata lookup. The mod jar's actual download URL is a public CurseForge
 * CDN link that doesn't need the key, so this script never proxies large binary downloads.
 *
 * Endpoint: GET /curseforge.php?modId=<numeric id>&gameVersion=<mc version>&modLoaderType=<1-6>
 */

// Never leak paths/stack traces to the response, no matter what the host's php.ini defaults are.
error_reporting(0);
ini_set('display_errors', '0');

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Cache-Control: public, max-age=300');
header('X-Content-Type-Options: nosniff');

require __DIR__ . '/lib.php';

function fail(int $status, string $message): never {
    http_response_code($status);
    echo json_encode(['error' => $message]);
    exit;
}

/** Simple per-IP sliding-window limiter, independent of the app's own client-side self-throttle —
 * this is the real backstop, since it applies no matter who/what is calling this endpoint. */
function enforceIpRateLimit(int $maxPerHour = 120): void {
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
    $stateDir = sys_get_temp_dir() . '/curseforge-proxy-ratelimit';
    if (!is_dir($stateDir)) {
        @mkdir($stateDir, 0700, true);
    }
    $stateFile = $stateDir . '/' . hash('sha256', $ip) . '.txt';

    $fh = @fopen($stateFile, 'c+');
    if ($fh === false) {
        return; // Don't fail the request just because the rate-limit state couldn't be opened.
    }
    flock($fh, LOCK_EX);

    $now = time();
    $cutoff = $now - 3600;
    $timestamps = [];
    rewind($fh);
    while (($line = fgets($fh)) !== false) {
        $t = (int) trim($line);
        if ($t >= $cutoff) {
            $timestamps[] = $t;
        }
    }

    if (count($timestamps) >= $maxPerHour) {
        flock($fh, LOCK_UN);
        fclose($fh);
        fail(429, 'Rate limit reached for this IP. Try again later.');
    }

    $timestamps[] = $now;
    ftruncate($fh, 0);
    rewind($fh);
    fwrite($fh, implode("\n", $timestamps) . "\n");
    flock($fh, LOCK_UN);
    fclose($fh);
}

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    fail(405, 'Method not allowed');
}

enforceIpRateLimit();

$apiKey = zerog_resolve_curseforge_key();
if ($apiKey === '') {
    fail(500, 'Proxy is not configured — see README.md "Where the key lives"');
}

$modId = $_GET['modId'] ?? '';
if (!preg_match('/^\d+$/', (string) $modId)) {
    fail(400, 'modId must be a numeric CurseForge mod ID');
}

// Allowlist forwarded params — never let a caller inject arbitrary query params upstream.
$allowedParams = ['gameVersion', 'modLoaderType', 'pageSize', 'index'];
$query = [];
foreach ($allowedParams as $param) {
    if (isset($_GET[$param]) && $_GET[$param] !== '') {
        $query[$param] = $_GET[$param];
    }
}
if (!isset($query['pageSize'])) {
    $query['pageSize'] = '50';
}

$url = 'https://api.curseforge.com/v1/mods/' . rawurlencode((string) $modId) . '/files';
if ($query) {
    $url .= '?' . http_build_query($query);
}

$ch = curl_init($url);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'x-api-key: ' . $apiKey,
        'Accept: application/json',
    ],
    CURLOPT_TIMEOUT => 20,
    CURLOPT_SSL_VERIFYPEER => true,
]);
$response = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

if ($response === false) {
    error_log('curseforge-proxy: upstream request failed: ' . $curlError);
    fail(502, 'Upstream request to CurseForge failed.');
}

http_response_code($statusCode ?: 502);
echo $response;
