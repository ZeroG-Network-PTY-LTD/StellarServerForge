<?php
declare(strict_types=1);

/**
 * Shared key-resolution logic for curseforge.php and index.php, so the status page can never
 * drift out of sync with what the proxy endpoint actually does.
 *
 * Resolution order (first found wins) — see README.md "Where the key lives":
 *   1. CF_API_KEY environment variable (best: never touches disk as a file at all)
 *   2. A config file OUTSIDE the web root, at ../../curseforge-proxy-secret/config.php
 *   3. config.php in this same folder (last resort — logged as a warning when used)
 */
function zerog_resolve_curseforge_key(): string {
    $fromEnv = getenv('CF_API_KEY');
    if (is_string($fromEnv) && trim($fromEnv) !== '') {
        return trim($fromEnv);
    }

    $outsideWebRoot = dirname(__DIR__, 2) . '/curseforge-proxy-secret/config.php';
    if (is_file($outsideWebRoot)) {
        $config = require $outsideWebRoot;
        $key = trim((string) ($config['CF_API_KEY'] ?? ''));
        if ($key !== '') {
            return $key;
        }
    }

    $inWebRoot = __DIR__ . '/config.php';
    if (is_file($inWebRoot)) {
        error_log('curseforge-proxy: using config.php inside the web root — move it outside the '
            . 'document root (see README.md) for stronger protection.');
        $config = require $inWebRoot;
        return trim((string) ($config['CF_API_KEY'] ?? ''));
    }

    return '';
}
