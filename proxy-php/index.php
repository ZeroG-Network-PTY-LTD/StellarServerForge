<?php
declare(strict_types=1);

require __DIR__ . '/lib.php';

$configured = zerog_resolve_curseforge_key() !== '';
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>ZeroG Network — CurseForge proxy</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body {
    margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
    background: #161826; color: #e9e9ed;
    font-family: "Inter", "Segoe UI", system-ui, -apple-system, sans-serif;
    padding: 24px;
  }
  .card {
    max-width: 480px; width: 100%; padding: 28px; border-radius: 14px;
    background: #232532; border: 1px solid #3f424d;
  }
  h1 { font-size: 18px; font-weight: 500; margin: 0 0 14px; display: flex; align-items: center; gap: 10px; }
  p { font-size: 13px; line-height: 1.6; color: #b2b6ca; margin: 0 0 8px; }
  .dot { display: inline-block; width: 9px; height: 9px; border-radius: 50%; flex: none; }
  .ok { background: #84d9a0; }
  .bad { background: #d98a84; }
  code {
    background: #1c1e2c; padding: 2px 6px; border-radius: 4px; font-size: 12px;
    font-family: "JetBrains Mono", Consolas, monospace; color: #d2cefd;
  }
  .kicker {
    font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; color: #93979b;
    margin: 18px 0 4px;
  }
</style>
</head>
<body>
  <div class="card">
    <h1><span class="dot <?= $configured ? 'ok' : 'bad' ?>"></span>ZeroG Network CurseForge proxy</h1>
    <p>Status: <strong><?= $configured ? 'configured' : 'NOT configured' ?></strong><?php if (!$configured): ?> — copy <code>config.example.php</code> to <code>config.php</code> and add your key.<?php endif; ?></p>
    <p>Used by StellarServerForge to install CurseForge-sourced ZeroG Network mods without shipping an API key inside the app.</p>
    <div class="kicker">Endpoint</div>
    <p><code>/curseforge.php?modId=&lt;id&gt;&amp;gameVersion=&lt;mc version&gt;&amp;modLoaderType=&lt;1-6&gt;</code></p>
  </div>
</body>
</html>
