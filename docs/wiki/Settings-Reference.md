# Settings Reference

Persisted to `settings.json` in the server folder (`ServerSettings`, via `SettingsService`). Most
fields are edited through the setup wizard or the Settings screen rather than by hand, but this is
the full field list for reference.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `minecraftVersion` | string | — (required) | The target Minecraft version, e.g. `"1.21.1"`. |
| `modLoader` | enum | `VANILLA` | `FORGE`, `NEOFORGE`, `FABRIC`, `QUILT`, or `VANILLA`. |
| `modLoaderVersion` | string | `""` | The resolved loader version to install (blank = auto-resolve latest matching). |
| `javaVersion` | int | `0` | The Java major version in use/required, per [Java Provisioning](Java-Provisioning.md). |
| `javaOverrideMode` | enum | `AUTOMATIC` | `AUTOMATIC`, `SYSTEM_PATH`, or `FORCE_MANAGED` — see [Java Provisioning](Java-Provisioning.md). |
| `maxRamGigs` | int | `4` | RAM (GB) given to the server JVM (`-Xmx`). |
| `args` | string | see below | Extra JVM args, editable; defaults to a G1GC-tuned flag set. |
| `askModsCheck` | boolean | `true` | Whether to prompt for a mod scan on relevant actions. |
| `port` | int | `25565` | The Minecraft server TCP port. |
| `portUdp` | int | `24454` | UDP port (voice/plugin-dependent use). |
| `protocol` | string | `"TCP"` | Protocol used for port-forwarding/firewall operations. |
| `usePortForwarded` | boolean | `false` | Whether UPnP port forwarding is enabled. |
| `zeroGCatalogUrl` | string | ZeroG's hosted catalog URL | Which [ZeroG mods catalog](ZeroG-Mods-Catalog.md) JSON to fetch. |
| `zeroGProxyBaseUrl` | string | `""` (= use the built-in default) | CurseForge proxy endpoint override. |
| `curseForgeApiKey` | string, **encrypted at rest** | `""` | Optional personal CurseForge API key — see [Security](Security.md). |

## Default JVM args (`DEFAULT_ARGS`)

```
-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions
-XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M
```

## Always-appended args (`OTHER_ARGS`, not user-editable)

```
-XX:+IgnoreUnrecognizedVMOptions -Dlog4j2.formatMsgNoLookups=true
```

Always appended regardless of the `args` field — the `log4j2.formatMsgNoLookups` flag in
particular is a baseline mitigation, not something a user should be able to accidentally strip out.

## Default ZeroG catalog URL

```
https://raw.githubusercontent.com/ZeroG-Network-PTY-LTD/StellarServerForge/main/zerog-mods-catalog.json
```
