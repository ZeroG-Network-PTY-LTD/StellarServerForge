# Changelog

All notable changes to StellarServerForge are recorded here, newest first. This file ships inside
the app (see the Changelog icon button, bottom-right of the dashboard) and its latest entry is
also used as the GitHub Release notes for each CI build — see `docs/wiki/Building-and-CI.md`.

## Unreleased

## 2026-09-05

- Added a "Restart server" button next to Stop: gracefully stops the running server, then
  relaunches it with the current settings.
- Reworked the Utilities tab: consistent card layout with properly wrapped descriptions, a live
  server-icon preview, a single icon designer dialog (replacing the old three-dialog color-chooser
  chain), Select all/Select none for the server-pack ZIP file list, and a Clear button for the
  output log.
- Added a Changelog icon button and a bug-shaped "report a bug" icon button, both bottom-right of
  the dashboard's link bar; the separate "Issue tracker" icon was removed since the new bug icon
  covers the same purpose.
- Utilities tab: added "Import from file..." to use an existing image as the server icon, an
  "Open server folder" button, a disabled/explained Purge button when there's nothing to purge, a
  disabled/explained run-scripts button for non-Forge/NeoForge modloaders, and a confirm prompt
  before overwriting an existing server-pack ZIP.
- Settings tab: the RAM slider now shows tick marks and warns if the allocation exceeds detected
  system RAM; added a "Check availability" button for the port field; added "Reset to default"
  buttons for the ZeroG catalog/proxy URL fields.
- Mods tab: the screen now loads the mods/ folder listing immediately instead of starting blank;
  added a live filter box, a "View CLIENTMODS" toggle so mods can be moved back out again (not just
  in), and an "Open folder" button. Selection is now tracked by file name so filtering no longer
  silently clears which mods were selected.
