<?php
declare(strict_types=1);

/**
 * Shared key-resolution logic for curseforge.php and index.php, so the status page can never
 * drift out of sync with what the proxy endpoint actually does.
 *
 * Resolution order (first found wins) — see README.md "Where the key lives":
 *   1. CF_API_KEY environment variable (best: never touches disk as a file at all)
 *   2. A config file OUTSIDE the web root, at curseforge-proxy-secret/config.php, tried at a few
 *      plausible depths since cPanel hosts differ in whether a subdomain's document root is
 *      nested inside public_html or is a sibling of it
 *   3. config.php in this same folder (last resort — logged as a warning when used)
 */
function zerog_resolve_curseforge_key(): string {
    $fromEnv = getenv('CF_API_KEY');
    if (is_string($fromEnv) && trim($fromEnv) !== '') {
        return trim($fromEnv);
    }

    // Try a few plausible depths above this folder for the outside-webroot secret, since cPanel
    // layouts vary: nested (public_html/sfs -> 2 levels to account home) vs. sibling
    // (public_html and sfs both directly under account home -> 1 level) vs. a deeper nesting some
    // hosts use for addon domains (3 levels).
    for ($depth = 1; $depth <= 3; $depth++) {
        $candidate = dirname(__DIR__, $depth) . '/curseforge-proxy-secret/config.php';
        if (is_file($candidate)) {
            $config = require $candidate;
            $key = trim((string) ($config['CF_API_KEY'] ?? ''));
            if ($key !== '') {
                return $key;
            }
            error_log("curseforge-proxy: found $candidate but CF_API_KEY in it is empty — checked further candidates.");
        }
    }

    $inWebRoot = __DIR__ . '/config.php';
    if (is_file($inWebRoot)) {
        error_log('curseforge-proxy: using config.php inside the web root — move it outside the '
            . 'document root (see README.md "Where the key lives") for stronger protection. '
            . 'None of the outside-webroot candidate paths (1-3 directories above ' . __DIR__
            . ') had a usable curseforge-proxy-secret/config.php.');
        $config = require $inWebRoot;
        return trim((string) ($config['CF_API_KEY'] ?? ''));
    }

    return '';
}
