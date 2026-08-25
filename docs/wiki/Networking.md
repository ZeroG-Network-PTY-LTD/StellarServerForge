# Networking

Networking tools live under `net_port/` and are reached from the dashboard/Settings.

## Port conflict detection

`PortConflictService` checks whether the configured server port is actually free before launch,
using a real `ServerSocket` bind-test rather than shelling out to `netstat`/`tasklist`. If the
port's in use, it identifies the owning process (via `ProcessHandle`/OS-specific lookup) and offers
to kill it or pick a different port — a real, working version of a path the legacy batch tool's
equivalent menu never actually reached.

## UPnP port forwarding

`UpnpService` uses a native Java UPnP library (`weupnp`) to forward the configured port on
UPnP-capable routers, replacing the legacy tool's dependency on an external
`Portforwarded.Server.exe`/.NET helper. Reached from Settings → "Manage UPnP".

## Firewall check

`FirewallCheckService` checks whether the configured port is allowed through the OS firewall.
This is Windows-only (it shells out to `netsh advfirewall` behind a platform check) — on other
operating systems it reports that the check isn't available rather than silently doing nothing.

## Network preflight

`NetworkPreflightService` (in `net/`) does basic DNS/reachability checks before operations that
need network access, so failures surface as a clear message rather than a confusing timeout deep
inside an HTTP call.
