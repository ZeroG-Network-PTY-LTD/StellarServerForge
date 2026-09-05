# Changelog

All notable changes to StellarServerForge are recorded here, newest first. This file ships inside
the app (see the "Changelog" link on the dashboard) and its latest entry is also used as the
GitHub Release notes for each CI build — see `docs/wiki/Building-and-CI.md`.

## Unreleased

## 2026-09-05

- Added a "Restart server" button next to Stop: gracefully stops the running server, then
  relaunches it with the current settings.
- Reworked the Utilities tab: consistent card layout with properly wrapped descriptions, a live
  server-icon preview, a single icon designer dialog (replacing the old three-dialog color-chooser
  chain), Select all/Select none for the server-pack ZIP file list, and a Clear button for the
  output log.
- Added a Changelog dialog and a "found a bug?" link on the dashboard, pointing at the GitHub
  issue tracker.
