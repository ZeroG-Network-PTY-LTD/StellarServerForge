# The Universalator — Functional Specification

Source: `Universalator-2.58.bat` (4496 lines), a Windows CMD batch script that installs and launches modded Minecraft Java Edition servers (Forge, NeoForge, Fabric, Quilt, Vanilla). This document is the complete behavioral spec for reimplementing the tool as a Java desktop application (GUI framework unspecified — Swing or JavaFX). No source code is included here, only behavior.

---

## 1. Purpose & Overall Flow

The Universalator is a single-file, self-contained installer/launcher for modded Minecraft servers. It is placed into a server folder (alongside an optional `mods` folder and other modpack files) and run directly (not as Administrator). It:

1. Detects/installs the correct Java runtime for the chosen Minecraft version.
2. Detects/installs the chosen modloader (Forge/NeoForge/Fabric/Quilt) or the vanilla server jar.
3. Manages `server.properties`, EULA acceptance, RAM/JVM args, port settings.
4. Offers optional UPnP automatic port forwarding via a bundled helper executable.
5. Provides utility features: client-mod scanning, MCreator-mod detection, zipping a server pack, icon generation, CurseForge profile import, log-crash diagnostics, run.sh/run.bat generation, and a "purge caches" reset.
6. Launches the server process and, optionally, auto-restarts it on unplanned shutdown.

### First run vs subsequent runs
- **Startup sequence (always, every run), in order:**
  1. `initialization_settings` — working directory pinning, version string from filename, console colors, `HERE`/`HEREPOWERSHELL` path variables, Windows-version gate (<10 unsupported and exits), seasonal banner (June = rainbow), creates `univ-utils` folder, sets `MONTHS_OLD=12`.
  2. `check_system_n_location` — validates the run folder path (special chars, spaces, exclamation marks, bad system folders like Desktop/OneDrive/Program Files, launcher-app folders), checks `_JAVA_OPTIONS`/`JDK_JAVA_OPTIONS`/`JAVA_TOOL_OPTIONS` env vars for conflicting `-Xmx`/`-Xmn`, ensures core Windows PATH entries exist for the session, verifies FINDSTR/CERTUTIL/NETSTAT/PING/CURL/TAR and PowerShell are available, checks TAR is the Windows built-in (not a GNU tar shadow), checks free/used disk space, checks the Windows `hosts` file for Mojang domain redirects.
  3. `get_license_check_license` — downloads `LICENSE` from GitHub as a canary file to confirm downloads persist (detects overly-aggressive AV deleting files).
  4. `check_ips` — detects public IP (via ip-api.com, falling back to api.ipify.org) and local LAN IPv4 (via `ipconfig`).
  5. `get_license_check_license` again (verifies the canary file still exists).
  6. `check_server_properties` — creates a default `server.properties` if missing; otherwise validates/repairs a few fields.
  7. `check_port_settings` — establishes `PORT`/`PORTUDP` from settings file or defaults, syncs `server.properties`'s `server-port`, warns if port < 10000, checks if port already in use via `NETSTAT` and offers to `TASKKILL` the owning process.
  8. If no `settings-universalator.txt` exists but `settings-linux-universalator.txt` does, converts Linux settings → Windows settings file.
  9. Looks up a CurseForge `minecraft_root` registry key (`HKCU\Software\Overwolf\Curseforge`). If found and no settings file exists yet, offers Import (CurseForge profile) vs Manual entry. Otherwise falls to manual full settings entry (`settingsentry`).
- **First run** (no `settings-universalator.txt`): goes directly into full settings entry — Minecraft version → modloader type → modloader version → Java version → RAM — then stamps a new settings file, and shows the main menu.
- **Subsequent runs**: reads the settings file into variables, optionally offers a client-mod scan if never done and a non-empty `mods` folder exists, checks UPnP helper existence, and shows the main menu.

Every menu-driven screen is entered via `CLS` + ASCII/ANSI-styled header + prompt, single-line free-text entry (via `SET /P`), validated in a loop until acceptable, no arrow-key navigation — this is the interaction model to replicate as GUI equivalents (text fields, buttons, dropdowns).

---

## 2. Settings / Config File Formats

### 2.1 `settings-universalator.txt` (Windows)
A generated batch-executable text file: comment lines start with `::`; data lines are literally `SET <KEY>=<value>` and get executed via `!TEMP!` at read time (i.e., the original script literally interprets this file as batch commands). In the Java rewrite this should become a plain key=value (e.g., properties/YAML/JSON) file — do **not** replicate "executable settings file" behavior.

Fields written by `stampsettingsfile` (in this order), with meaning:

| Key | Meaning |
|---|---|
| `MINECRAFT` | Minecraft version string, e.g. `1.20.1` |
| `MODLOADER` | One of `FORGE`, `NEOFORGE`, `FABRIC`, `QUILT`, `VANILLA` |
| `MODLOADERVERSION` | Version string of the modloader (blank if VANILLA) |
| `JAVAVERSION` | Java major version to use (8, 16, 17, 21, 25) — script-managed, "do not edit" |
| `MAXRAMGIGS` | Integer GB for `-Xmx` |
| `ARGS` | JVM args string (user-editable; default is a G1GC tuning string) |
| `ASKMODSCHECK` | `Y`/`N` — whether to still prompt for a client-mod scan on next main-menu load |
| `PORT` | TCP port (Minecraft game protocol) |
| `PORTUDP` | UDP port (used by voice-chat mods, e.g. Simple Voice Chat) |
| `PROTOCOL` | `TCP` / `UDP` / `BOTH` — which protocol(s) UPnP forwards |
| `USEPORTFORWARDED` | `Y`/`N` — whether UPnP forwarding is active |
| `OVERRIDE` | `A` (Automatic detect/fetch), `J` (force system PATH `java`), `F` (Force Adoptium-managed Java) |

Notes:
- `OTHERARGS` (always-applied JVM args, e.g. log4j mitigation) is a script constant, not stored in the settings file.
- Defaults applied when unset before stamping: `ASKMODSCHECK=Y`, `PROTOCOL=TCP`, `USEPORTFORWARDED=N`, `OVERRIDE=A`.
- Reading the file (`read_settings_file`) parses non-comment lines with a trim, executes each as `SET`, then derives `MAXRAM=-Xmx<GB>G`. If `OVERRIDE=J`, it also captures the output of `java -version` into `CUSTOMJAVA` for display.
- Editing a single key uses `univ_settings_edit <KEY> <value>` — rewrites the file line-by-line, replacing only the matching `SET <KEY>=` line. **Java equivalent**: load into a map, update key, persist — trivial with any key/value format.

### 2.2 `settings-linux-universalator.txt` (Linux variant, read-only, one-way converted)
Format: `KEY=value` lines (with optional `#`-comment lines ignored), no `SET` prefix, values sometimes quoted. Fields consumed by `convert_linux_settings`: `MINECRAFT`, `MODLOADER`, `MODLOADERVERSION`, `JAVAVERSION`, `MAXRAMGIGS`, `ARGS`, `PORT`, `PORTUDP`, `PROTOCOL`, `USEPORTFORWARDED`. No `OVERRIDE` in Linux settings — Windows conversion always sets `OVERRIDE=A`. Conversion only proceeds (calls `stampsettingsfile`) if **all** required fields were successfully parsed; otherwise the user falls through to manual entry.

### 2.3 `server.properties`
Standard Minecraft server properties file, `key=value` per line, `#` for comments.
- **If missing**, script creates one with defaults: `allow-flight=true`, `online-mode=true`, `server-port=25565`, `server-ip=` (blank), `level-name=world`, `motd=A Minecraft Server`, `view-distance=10`, `max-build-height=256`, `spawn-npcs=true`, `spawn-animals=true`, `difficulty=normal`.
- **If present**, on every startup the script: reads `server-port` into `SERVERPROPSPORT`; forces `allow-flight` and `online-mode` back to `true` if found `false`; captures `server-ip=` value — if non-blank, warns the user (custom domain use case) and offers to blank it (`CORRECT`) or leave it (`IGNORE`); validates `difficulty` is one of peaceful/easy/normal/hard, resetting to `normal` if not.
- Port sync: if `server-port` in the file differs from the Universalator `PORT` setting, the file is rewritten to match `PORT` (Universalator settings win). If no `server-port=` line exists at all, one is appended.
- Editing logic (`serverpropsedit <key> [<value>]`, called with no value to blank a value): reads the whole file into parallel arrays of property/value split on `=`, updates the matching property's stored value only if different, then rewrites the entire file preserving order — blank properties preserve comment lines (lines starting with `#`) verbatim, and blank non-comment properties are dropped entirely from output. **Implementation note**: this is effectively an ordered key-preserving properties-file rewrite; a Java `LinkedHashMap<String,String>` plus manual serialization (to keep comments) is the natural analog.
- **server.properties menu** (`PROPS` command, function `serverpropsedit_function`): displays a fixed whitelist of properties (alphabetically iterated): `difficulty, enable-command-block, enforce-whitelist, function-permission-level, level-name, level-seed, level-type, max-players, max-tick-time, max-world-size, motd, region-file-compression, server-port, simulation-distance, spawn-protection, view-distance, white-list` — only entries actually present in the file are shown, numbered, column-aligned. Selecting a number:
  - If the current value is `true`/`false` → toggles it.
  - `difficulty` → cycles peaceful→easy→normal→hard→peaceful.
  - `function-permission-level` → prompts for new int, clamps to [1,4], non-numeric input resets to 2.
  - `region-file-compression` → toggles `deflate`⇄`lz4` (with a note about lz4 being faster but larger); any other value defaults to `deflate`.
  - `max-players` → prompts for int, non-numeric → 20, clamps min 1.
  - `max-tick-time` → prompts for int, non-numeric → 60000, min -1.
  - `max-world-size` → prompts for int, non-numeric → 29999984, min 1.
  - `simulation-distance` / `view-distance` → prompts for int, non-numeric → 10, clamp [1,32].
  - All other whitelisted keys → free-text prompt, trimmed.
  - Editing `server-port` also updates the in-memory `PORT` variable (but does **not** call `univ_settings_edit` here — implementation note: this is a minor inconsistency vs. the main Port menu which does sync the settings file).

### 2.4 `eula.txt`
Standard `eula=true`/`eula=false` file. Handled by `:eula` (see §12).

---

## 3. Menu Systems

### 3.1 Main Menu (`:mainmenu`)
Displayed every time the user returns from any action. Shows: current Minecraft version, modloader, modloader version (label varies by loader), Java version (or "CUSTOM OVERRIDE" + path if `OVERRIDE=J`), Max RAM, current port(s) (format varies based on `USEPORTFORWARDED`/`PROTOCOL`), and UPnP forwarding enabled/disabled indicator (only shown if the Portforwarded helper exe exists).

Visible hotkeys on the main screen: `L` (launch), `S` (re-enter all settings), `R` (RAM setting), `UPNP` (UPnP menu), `SCAN` (client mod scan), `A` (list all commands).

Full set of recognized main-menu commands (case-insensitive), each dispatches to a function/GOTO:
| Command | Action |
|---|---|
| `Q` | Reset console color, clear screen, exit program |
| `M` | Redisplay main menu |
| `UPNP` | UPnP port forwarding submenu |
| `V` | Re-fetch modloader metadata, prompt for new modloader version, persist to settings |
| `J` | Set Java version |
| `R` | Set RAM |
| `S` | Full settings re-entry (MC version → modloader → modloader version → Java → RAM → stamp) |
| `L` | Go to launch sequence |
| `SCAN` | Client-mod scan |
| `OVERRIDE` | Toggle Java override mode (A/J/F cycle) |
| `MCREATOR` | Scan mods folder for MCreator-made mods (only if `mods` folder exists) |
| `A` | Show "all commands" alternate menu |
| `ZIP` | Server pack ZIP creation menu |
| `IMPORT` | CurseForge profile import |
| `PORT` | Port number edit menu |
| `PROPS` | server.properties edit menu |
| `FIREWALL` | Windows Firewall rule check for Java |
| `RESTART` | Toggle auto-restart-on-crash |
| `LOG` / `LOGS` | View last log file |
| `MODS` | View mods folder contents (unsorted) |
| `SMOD` | View mods folder contents (sorted, jar files only) |
| `ICON` | Server icon (server-icon.png) generator |
| `GENRUN` | Generate run.sh / run.bat scripts |
| `PURGE` | Purge cached/downloaded modloader & utility files (undocumented/hidden command) |

Unrecognized input redisplays the current menu (main or all-commands) rather than erroring.

### 3.2 All-Commands Menu (`A`)
Same dispatcher/input loop as main menu (`allcommandsentry`), just a different static listing of every command above (including ones not shown on the main screen: `V`, `PORT`, `PROPS`, `RESTART`, `FIREWALL`, `GENRUN`, `LOG`, `MODS`/`SMOD`, `ICON`, `MCREATOR`, `OVERRIDE`, `ZIP`, `IMPORT`). Selecting `M` returns to main menu display.

### 3.3 UPnP Port Forwarding Menu (`:upnpmenu_funciton` / `:upnpmenu`)
Gate: if the detected public IP starts with `100.64.` (CGNAT range), immediately warns UPnP cannot work and exits back.

**If the Portforwarded.Server.exe helper is not yet downloaded**, the menu shows only informational text about port forwarding and offers:
- `DOWNLOAD` — downloads the helper (see §14).
- `M` — back to main menu.

**If the helper exists**, checks `dotnet` is on PATH (warns/exits with a link if missing) and displays: helper installed status, dotnet version, current `PROTOCOL`, UPnP active/inactive status with forwarded port(s), optionally shows Local/Public IP (toggle), and offers:
- `CHECK` — validate router UPnP support (`upnp_validate`).
- `TOGGLE` — cycle `PROTOCOL` TCP→BOTH→UDP→TCP.
- `PORT` — change TCP/UDP port numbers (`upnpport_function`).
- `SHOW`/`HIDE` — toggle IP display.
- `A` — activate UPnP forwarding (`upnp_activate_function`).
- `D` — deactivate UPnP forwarding (`upnp_deactivate_function`).
- `M` — stamps settings file and returns to main menu.
Unrecognized input loops back to the UPnP menu.

### 3.4 Server-Pack ZIP Menu (`:zipit_function` → `:zipit2`)
Confirms intent (`Y`/`M`), pre-populates a candidate file/folder list by scanning the current directory for a fixed whitelist (`config`, `defaultconfigs`, `kubejs`, `mods`, `scripts`, `server-icon.png`, `server.properties`, `settings-universalator.txt`, and anything matching `Universalator*`). Interactive loop accepts:
- `ADD <name>` — add an existing file/folder (rejects names containing `univ-utils`, `.fabric`, `libraries`, `versions`, `logs`, or `.jar`, since those are installer-managed/generated).
- `REM <name>` — remove a previously listed entry (marks as `deletedentry` rather than physically removing from array).
- `ZIPIT <name>` — builds `<name>.zip` via PowerShell `Compress-Archive` for each remaining entry, generates and bundles a `univ-utils/readme-server.txt` explaining Windows/Linux usage, downloads and bundles `Universalator-linux.sh` from GitHub if not already cached.
- `M` — exit to main menu.

### 3.5 Icon Generation Menu (`:icon_make` → `:icon_choice_loop`)
State machine with in-memory selections (`COLOR`, `TEXTCOLOR`, `CUSTOMTEXT`). Options:
1. Generate default blue 64×64 icon with yellow "Univ" text (fixed layout, Consolas 24 bold) → saved as `server-icon.png` immediately.
2. Pick background color — free-text HTML color name, `L` lists all `System.Drawing.Color` names via PowerShell, validated via `[System.Drawing.Color]::FromName().IsKnownColor`.
3. Enter custom diagonal text (max 10 chars) — auto-shrinks font size via PowerShell `TextRenderer.MeasureText` binary-decrement loop (from 30pt down to 6pt min) to fit within 85px width.
4. Pick text color (same validation as background).
5. Generate — combines choices: colored 64×64 bitmap, if custom text present draws it centered, rotated -45°, else draws a plain color swatch. If a `server-icon.png` already exists, renames the old one to `server-iconN.png` (finds first free integer N) before writing the new one.
`M` returns to main menu. Loop continues after each generation.

### 3.6 CurseForge Import / Manual Choice (`:query_import_or_manual`)
Only invoked on first run when a CurseForge registry key is found and no settings file exists yet. Simple `I` (import) / `M` (manual) choice.

### 3.7 CurseForge Profile Import Menu (`:import_curseforge_profile`, also reachable via `IMPORT` command)
Reads `HKCU\Software\Overwolf\Curseforge` value `minecraft_root` from the registry. Lists subfolders of `<root>\Instances\` containing a `minecraftinstance.json`, numbered. User selects by number or `M`. See §11 for the copy/import logic.

### 3.8 Server-Properties Edit Menu — see §2.3.

### 3.9 Port Edit Menu (`:portedit_function`)
Prompts for a new port, accepts `default` (→25565) or `M` (cancel), rejects non-numeric input and values < 10000, then updates `PORT`, `server.properties`, and settings file.

### 3.10 Purge Menu (`:purge_function`)
Confirmation prompt requiring literal `PURGE` (or `M` to cancel). On confirm, deletes: all `*.jar` in root, `libraries\`, `.fabric\`, `univ-utils\installers\`, `univ-utils\java\`, `univ-utils\Portforwarded\`, `univ-utils\versions\`, all `univ-utils\*.json`, all `univ-utils\*.xml`. Explicitly does not touch user's custom server files (mods, config, world saves, settings).

---

## 4. Function/Routine Catalog

Below, every `:label` invoked via `CALL` (i.e., a "function") plus significant `GOTO`-only sections, with purpose, inputs, outputs, and external calls. (Section numbers cross-reference detailed behavior described elsewhere in this document rather than repeating it verbatim.)

| Function | Purpose | Reads | Produces/Side-effects | External calls |
|---|---|---|---|---|
| `initialization_settings` | Bootstraps environment: cwd pin, version string, colors, header text, folder creation | script filename, current date (month) | `HERE`, `HEREPOWERSHELL`, `UNIV_VERSION`, `UNIV_HEADER`, ANSI color vars, `univ-utils\` folder | `powershell -Command "Get-Date -Format MM"` |
| `check_system_n_location` | Validates run environment sanity | cwd, PATH, `_JAVA_OPTIONS` etc, hosts file | May exit with error screens | `powershell` (path/exclamation check, disk space via `Win32_LogicalDisk`/`Get-PSDrive`), `WHERE` on FINDSTR/CERTUTIL/NETSTAT/PING/CURL/TAR/powershell, `tar --version` |
| `get_license_check_license` | Canary-tests that downloaded files persist | `univ-utils\license.txt` existence | Downloads `LICENSE` | `powershell` WebClient to `raw.githubusercontent.com/nanonestor/universalator/main/LICENSE` |
| `check_ips` | Detects public & LAN IPv4 | none | `PUBLICIP`, `LOCALIP` | `powershell` HTTP GET to `ip-api.com/json` (fallback `api.ipify.org`), `ipconfig` |
| `check_server_properties` | Ensures valid `server.properties` | `server.properties` | Creates/edits file; may prompt for `server-ip` correction | none |
| `check_port_settings` | Establishes port vars & clears conflicts | settings file, `server.properties`, `NETSTAT`, `TASKLIST` | `PORT`, `PORTUDP`; may `TASKKILL` | `NETSTAT -aon`, `TASKLIST`, `TASKKILL` |
| `convert_linux_settings` | One-way migrates Linux settings → Windows settings | `settings-linux-universalator.txt` | `settings-universalator.txt` (via `stampsettingsfile`) | none |
| `settingsentry` | Orchestrates full settings re-entry | user input | Calls sub-functions below, writes settings file | (delegates) |
| `enter_mcversion` | Prompts & validates Minecraft version | Mojang manifest | `MINECRAFT`, `MCMAJOR`/`MCMINOR`/`MCHOTFIX` | `powershell` parses `version_manifest_v2.json` |
| `get_mcmajorminor` | Parses `MINECRAFT` into major/minor/hotfix ints | `MINECRAFT` | `MCMAJOR`, `MCMINOR`, `MCHOTFIX` | none |
| `enter_modloader_type` | Prompts & validates modloader choice | user input | `MODLOADER` (uppercased) | none |
| `get_modloader_metadatafile` | Downloads/refreshes maven-metadata.xml (or Forge's promotions_slim.json) if stale (>6h) | `MODLOADER`, `MINECRAFT` | `univ-utils\maven-*-metadata.xml`, `univ-utils\promotions_slim.json` | `powershell Test-Path -OlderThan`, `curl`, `powershell WebClient.DownloadFile` from maven repos |
| `resolve_n_ping` | DNS + ping preflight for the modloader's maven host and Mojang hosts | `MODLOADER` | May halt with DNS/ping troubleshooting screens | `powershell Resolve-DnsName`, `ping` |
| `enter_fabric_quilt_version` | Prompts newest-vs-custom Fabric/Quilt loader version, validates against maven XML | metadata XML | `MODLOADERVERSION` | `powershell` XML parse |
| `enter_forge_neoforge_version` | Prompts newest-vs-custom Forge/NeoForge version, validates against JSON/XML | `promotions_slim.json` or neoforge maven XML | `MODLOADERVERSION` | `powershell` JSON/XML parse |
| `setjava` | Determines/prompts required Java major version | `MCMAJOR`/`MCMINOR` | `JAVAVERSION` | none |
| `enter_ram` | Prompts max RAM GB, validates integer | System RAM via PowerShell | `MAXRAMGIGS` | `powershell Get-CimInstance Win32_OperatingSystem` |
| `stampsettingsfile` | Writes full settings file from current vars | in-memory vars | `settings-universalator.txt` | none |
| `read_settings_file` | Loads settings file into vars | `settings-universalator.txt` | all setting vars, `MAXRAM`, `CUSTOMJAVA` (if OVERRIDE=J) | `java -version` (if OVERRIDE=J) |
| `checkformodsfolder` | Warns if launching without a `mods` folder | `mods` folder existence | `CONTINUE` Y/N | none |
| `java_checks` | Finds or installs correct Java (system or Adoptium) | `JAVAVERSION`, `OVERRIDE`, filesystem, Adoptium API | `JAVAFILE`, `JAVATYPE`, `JAVANUM`, downloads/extracts JDK | `powershell` (dir scan by CreationTime/LastWriteTime, Adoptium `api.adoptium.net` JSON), `certutil -hashfile`, `tar -xf` |
| `check_for_forge_neoforge` | Detects/installs Forge or NeoForge server files | `MODLOADER`, `MINECRAFT`, `MODLOADERVERSION` | Downloads installer jar, runs it, moves it to `univ-utils\installers\` | `ping`, `powershell WebClient`, `curl`, `certutil -hashfile`, java installer execution |
| `check_for_fabric_quilt` | Detects/installs Fabric or Quilt server files | as above | Downloads installer jar, runs it | `ping`, `powershell` (maven-metadata XML + WebClient), `curl` (checksum), `certutil`, java installer execution |
| `check_for_vanilla` | Detects/downloads vanilla server jar | `MINECRAFT`, Mojang manifest | `minecraft_server.<ver>.jar` | `powershell` (version json fetch + WebClient download), `certutil -hashfile` |
| `checkmojmanifest` | Refreshes Mojang `version_manifest_v2.json` if >1 day old | file age | `univ-utils\version_manifest_v2.json` | `powershell Test-Path -OlderThan`, `powershell WebClient` |
| `eula` | Ensures `eula.txt` says true, else prompts `AGREE` | `eula.txt` | writes `eula.txt` | none |
| `launchserver`* | Builds final launch command line and runs Java (with/without UPnP wrapper); handles auto-restart | all settings + `JAVAFILE` | Runs server process; may loop on crash | `Portforwarded.Server.exe` (if UPnP active), `dotnet --version` |
| `clientmodsscan` | Orchestrates client-only mod detection | `mods` folder | dispatches to loader-specific scanners | none directly |
| `scanforgeneoforge` | Detects client-only mods for Forge/NeoForge via TOML/mcmod.info parsing | jar contents, `clientonlymods.txt` | Optionally moves files to `CLIENTMODS\` | `powershell` (tar extraction of TOML/JSON inside jars, PSToml module optional), `powershell WebClient` (downloads master list from GitHub) |
| `scanfabricquilt` | Detects client-only mods for Fabric/Quilt via `fabric.mod.json`/`quilt.mod.json` + dependency cross-check | jar contents | Optionally moves files to `CLIENTMODS\` | `powershell` (tar extraction + JSON parse) |
| `upnpmenu_funciton` / `upnpmenu` | UPnP menu loop | see §3.3 | dispatches | `dotnet --version`, `WHERE dotnet` |
| `toggle` | Cycles `PROTOCOL` | `PROTOCOL` | `PROTOCOL` | none |
| `upnpport_function` | Prompts TCP/UDP ports, syncs voicechat mod config & server.properties | user input, `config\voicechat\voicechat-server.properties` | `PORT`, `PORTUDP`, files | none |
| `upnp_validate` | Live-tests router UPnP support using a throwaway MC 1.4.2 jar | `PORT`, Java | `VALIDATEDUPNP` | `Portforwarded.Server.exe testmode="true"`, may call `java_checks`/`check_for_vanilla` |
| `upnp_activate_function` | Confirms & activates UPnP forwarding (validates first) | user input | `USEPORTFORWARDED=Y`, settings file | (delegates to `upnp_validate`) |
| `upnp_deactivate_function` | Deactivates UPnP forwarding | none | `USEPORTFORWARDED=N`, settings file | none |
| `upnpdownload` | Downloads and extracts the Portforwarded helper, checksum-verifies | none | `univ-utils\Portforwarded\Portforwarded.Server.exe` | `powershell WebClient` (GitHub release ZIP), `tar -xf`, `certutil -hashfile` |
| `override` | Cycles `OVERRIDE` A→J→F→A, shows description | `OVERRIDE` | settings file | `java -version` (if now J) |
| `mcreatorscan` | Searches jar contents for MCreator signature strings | `mods\*.jar` | `mcreator-mods.txt` | `findstr /m`, `SORT` |
| `zipit_function` / `zipit2` | Server pack ZIP builder menu | see §3.4 | `<name>.zip`, `univ-utils\readme-server.txt` | `powershell Compress-Archive`, `powershell WebClient` (linux script) |
| `logsscan` | Pattern-matches `logs\latest.log` for known crash signatures, prints guidance | `logs\latest.log` | none (display only) | `FINDSTR` |
| `portedit_function` | Port edit menu | see §3.9 | `PORT`, `server.properties`, settings file | none |
| `serverpropsedit_function` | server.properties interactive editor | see §2.3 | `server.properties` | none |
| `firewallcheck` | Checks for a Windows Firewall allow-rule for the resolved java.exe on the current port | `JAVAVERSION`, `univ-utils\java\`, `PORT` | display only | `powershell Get-NetFirewallProfile`/`Get-NetFirewallRule`/`Get-NetFirewallPortFilter`/`Get-NetFirewallApplicationFilter` |
| `restarttoggle` | Toggles `RESTART` Y/N (session-only, not persisted to settings file) | `RESTART` | `RESTART` | none |
| `check_upnp_program_exists` | Resets UPnP-related settings if helper exe missing | filesystem | `PROTOCOL=TCP`, `USEPORTFORWARDED=N` if needed | none |
| `purge_function` | Deletes cached/downloaded files | see §3.10 | filesystem deletes | none |
| `logs_view` | Displays `logs\latest.log`, then runs `logsscan` | `logs\latest.log` | none | `TYPE` |
| `mods_view` | Lists mods folder contents (unsorted for `MODS`, sorted jar-only for `SMOD`), writes `modslist.txt` | `mods\` | `modslist.txt` | `DIR`, `SORT` |
| `gen_run_scripts` | Generates standalone `run.sh`/`run.bat` and (for MC>16) `user_jvm_args.txt` — Forge/NeoForge only | `MODLOADER`, `MCMAJOR`, installed files | `run.sh`, `run.bat`, `user_jvm_args.txt` | none |
| `icon_make` / `icon_choice_loop` | Icon generator menu | see §3.5 | `server-icon.png` | `powershell` (`System.Drawing`) |
| `import_curseforge_profile` | CurseForge profile import | see §11 | copies files, `settings-universalator.txt` | `REG QUERY`, `powershell` (JSON parse of `minecraftinstance.json`), `XCOPY`/`COPY` |
| `query_import_or_manual` | First-run I/M choice | user input | `IMPORTCHOICE` | none |
| `serverpropsedit` (utility, 2-arg) | Rewrites one `server.properties` key=value | `server.properties`, `%1`,`%2` | `server.properties` | none |
| `GetMaxStringLength` | Computes max display length for column alignment | string | out var | none |
| `l_replace` | Generic string-replace utility (used rarely; string-substitution is normally done via `!VAR:from=to!` instead) | strings | out var | none |
| `univ_settings_edit` | Rewrites one key in `settings-universalator.txt` | key, value | file | none |
| `trim` | Trims trailing spaces from a string | string | out var | none |
| `StringLength` | Computes length of an arbitrary string (binary-search algorithm) | string | out var | none |

\* `:launchserver` is technically a GOTO target reached from `:launch_sequence`, not a `CALL`ed function, but is documented here as it's the core orchestration point.

**Total distinct callable label/functions: ~55** (including small utility labels and sub-loops like `:javaselect`, `:badramentry`, `:redoenterforge`, etc., which are internal retry loops within the functions above rather than separately catalogued).

---

## 5. Minecraft Version Handling

### 5.1 Manifest source
`checkmojmanifest`: downloads `https://launchermeta.mojang.com/mc/game/version_manifest_v2.json` to `univ-utils\version_manifest_v2.json` if the file doesn't exist or is older than 1 day (via `Test-Path -OlderThan (Get-Date).AddDays(-1)`). If a download attempt fails and a settings file already exists (i.e., not first run), user is prompted to retry; on very first run it fails silently/gracefully (script does not hard-block first run on manifest fetch failure — though later steps requiring it will still need it).

### 5.2 Version entry & validation (`enter_mcversion`)
- Free-text prompt, examples shown: `1.7.10`, `1.16.5`, `1.19.2`.
- Trims trailing spaces.
- Validates entry against the manifest: filters manifest entries where `type == "release"`, checks exact string match against `MINECRAFT`. Only `release`-typed versions are accepted — snapshots, betas, alphas are rejected. On failure, error message and loop back to re-enter.

### 5.3 Major/Minor/Hotfix parsing (`get_mcmajorminor`)
Splits `MINECRAFT` on `.` into up to 3 tokens.
- **Legacy scheme** (first token == `1`): `MCMAJOR` = 2nd token, `MCMINOR` = 3rd token (may be blank → defaults 0). No hotfix concept.
- **New scheme** (first token != `1`, e.g. future year-based versioning like `26.1`): `MCMAJOR` = 1st token, `MCMINOR` = 2nd token, `MCHOTFIX` = 3rd token if present else `0`.
- If `MCMINOR` ends up undefined, it's forced to 0.

**Implementation note**: this is explicitly designed to future-proof for a hypothetical post-1.x Mojang versioning scheme (referenced as "as of 2026 new MC versions will start with the year prefix"); a Java reimplementation should keep both parsing paths since real-world version strings may take either shape going forward.

---

## 6. Modloader Handling

All four loaders (plus Vanilla) share a common lifecycle: (1) determine/enter version, (2) detect if already installed under this MC+loader+version, (3) if not, download an installer/artifact and run it, (4) verify installation succeeded, (5) build a modloader-specific launch command line.

### 6.1 Metadata sources (`get_modloader_metadatafile`)
| Loader | Metadata file | Metadata URL |
|---|---|---|
| Fabric | `maven-fabric-metadata.xml` | `https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml` |
| Quilt | `maven-quilt-metadata.xml` | `https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml` |
| Forge | `maven-forge-metadata.xml` | `https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml` |
| NeoForge (MC 1.20.1) | `maven-neoforge-1.20.1-metadata.xml` | `https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml` |
| NeoForge (other) | `maven-neoforge-metadata.xml` | `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml` |

Forge additionally downloads `https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json` (a "recommended/latest per MC version" index), refreshed if the cached copy is >6 hours old, same refresh cadence as the maven XMLs. If the modloader is unreachable (offline maven), the stale/existing metadata file is preserved rather than deleted, so the tool degrades gracefully offline using cached data. DNS resolve + ping preflight (`resolve_n_ping`) is performed before attempting the download (see §14 for exact hosts checked).

**Implementation note**: SHA256 checksum verification of the downloaded metadata files is present in the script but entirely commented out (dead code) — not currently enforced. Worth considering for the Java rewrite since it's clearly an intended-but-abandoned feature.

### 6.2 Fabric / Quilt version selection (`enter_fabric_quilt_version`)
1. Reads `<metadata>.versioning.release` from the XML → "recommended newest" (`FQLOADER`).
2. Prompts Y (use newest) / N (enter custom).
3. If custom, validates the entered version exists in `<metadata>.versioning.versions.version` list; loops with an error screen if not found.

### 6.3 Forge / NeoForge version selection (`enter_forge_neoforge_version`)
- **Forge**: newest version comes from `promotions_slim.json`'s `promos.'<MC>-latest'` key (falls back to a "Not detected — enter a version number" placeholder text if absent).
- **NeoForge @ 1.20.1**: hardcoded newest = `47.1.106` (NeoForge stopped updating this branch).
- **NeoForge @ other versions**: newest is computed by iterating every `<version>` entry in the maven XML, splitting each on `.`/`-` into up to 5 tokens, matching major/minor against `MCMAJOR`/`MCMINOR`:
  - **Legacy MC (MCMAJOR < 26)**: among matches, tracks the highest 3rd token (patch/build number) as "newest."
  - **New-scheme MC (MCMAJOR ≥ 26)**: additionally requires the 3rd token equal `MCHOTFIX`, then tracks the highest 4th token as "newest" (accounts for hotfix-qualified NeoForge version strings). This dual logic is designed for a not-yet-real future Mojang versioning scheme and should be preserved as documented, even though real-world data may never hit the `≥26` branch by the time of reimplementation.
  - If nothing matched at all, flags `MAVENISSUE=Y` and shows a "no versions found for this MC version" recovery screen offering **S** (start settings over) or **T** (retry fetching a fresh metadata file, deleting the stale one first).
- User prompted Y (use detected newest) or free-text entry.
  - Forge-only extra validation: rejects entries containing any a–z/A–Z letters (Forge versions are purely numeric — NeoForge is exempted from this because it has `-beta` suffixed versions).
  - Validates the entered `<MC>-<version>` (Forge, and NeoForge@1.20.1 which shares Forge's naming) or bare `<version>` (NeoForge other) actually exists in the metadata XML before accepting.

### 6.4 Detection of an existing install (`check_for_forge_neoforge`)
- NeoForge: checks for `libraries/net/neoforged/forge/<MC>-<ver>/` (1.20.1) or `libraries/net/neoforged/neoforge/<ver>/` (other) as a directory.
- Forge: file-naming has changed across MC history:
  - MC ≤ 12: wildcard-matches any `*<MC>-<ver>*.jar` in root.
  - MC 13–16: requires both `forge-<MC>-<ver>.jar` (or `forge-<MC>-<ver>-<MC>.jar` for MC 7–9, an even older triple-barreled naming scheme) present in root **and** `libraries\net\minecraftforge\forge\<MC>-<ver>\` present.
  - MC ≥ 17: only checks `libraries\net\minecraftforge\forge\<MC>-<ver>\` directory.
- If not found: deletes any existing root `*.jar`, `libraries\`, `.fabric\` (cleanup to prevent version mashups), downloads the vanilla server jar first for MC ≤ 16 (old Forge installers sometimes point to dead vanilla-jar URLs), then downloads the installer jar (`forge-<MC>-<ver>-installer.jar` or `neoforge-<ver>-installer.jar` / neoforge-1.20.1's own `forge-<MC>-<ver>-installer.jar` naming) via PowerShell WebClient with `curl` fallback, SHA256-checksum-verifies against `<url>.sha256`, then executes: `java -Djava.net.preferIPv4Stack=true -XX:+UseG1GC -jar <installer>.jar --installServer`. Deletes installer logs, `run.*`, `user_jvm_args.txt` (Universalator replaces these). Moves the (now-used) installer jar into `univ-utils\installers\` for reuse; if a later install attempt still fails to find the expected result, the cached installer is deleted and redownloaded.
- Caches the installer jar under `univ-utils\installers\<mod_loader>-<ver>-installer.jar` for reuse across launches without re-downloading.

### 6.5 Detection/install of Fabric/Quilt (`check_for_fabric_quilt`)
- **Quilt-specific guard**: if the run path contains any space character, Quilt refuses to proceed (Quilt's installer cannot handle paths with spaces) — hard error with instructions to move the folder.
- Detects existing install by checking both `<loader>-server-launch-<MC>-<ver>.jar` in root AND `libraries\net\fabricmc\...` (Fabric) or `libraries\org\quiltmc\...` (Quilt) for the loader jar.
- If not found: cleans `.fabric\`, `libraries\`, root `*.jar`; pings the loader's maven host; downloads the loader-agnostic **installer** (not the loader itself) from its own maven-metadata `release` version (`fabric-installer`/`quilt-installer`); checksum-verifies; runs:
  - Fabric: `java -XX:+UseG1GC -jar fabric-installer.jar server -loader <ver> -mcversion <MC> -downloadMinecraft`
  - Quilt: `java -XX:+UseG1GC -jar quilt-installer.jar install server <MC> <ver> --download-server --install-dir=<HERE>`
  - Both installers download the vanilla server jar themselves as part of the process.
  - Renames the resulting `<loader>-server-launch.jar` to `<loader>-server-launch-<MC>-<ver>.jar` for consistent tracking across versions.

### 6.6 Vanilla (`check_for_vanilla`)
- Checks for `minecraft_server.<MC>.jar` in root; if absent, ensures the Mojang manifest is present, downloads that specific version's per-version JSON (`univ-utils\versions\<MC>.json`, cached indefinitely once fetched, unlike the top-level manifest which refreshes daily) if not already cached, extracts `downloads.server.url` and `downloads.server.sha1`, downloads the jar, and SHA1-checksum-verifies. Retries on failure.
- Also used by the UPnP validator (`upnp_validate`) to fetch a throwaway MC 1.4.2 jar for router-test purposes, gated by a module-level `UPNPGETMCJAR=Y` flag that short-circuits all the normal UI messaging.

### 6.7 Launch line construction (per loader, in `launchserver`)
- **Forge, MC ≤ 16**: locates the launcher jar in root via wildcard match on `*<MC>-<ver>*.jar`; `LAUNCHLINE = <MAXRAM> <USEARGS> -jar <foundjar> nogui`.
- **Forge, MC ≥ 17**: uses the modern args-file launch: `LAUNCHLINE = <MAXRAM> <USEARGS> @libraries/net/minecraftforge/forge/<MC>-<ver>/win_args.txt nogui %*`.
- **NeoForge @ 1.20.1**: `@libraries/net/neoforged/forge/<MC>-<ver>/win_args.txt nogui %*`.
- **NeoForge (other)**: `@libraries/net/neoforged/neoforge/<ver>/win_args.txt nogui %*`.
- **Fabric**: `-jar fabric-server-launch-<MC>-<ver>.jar nogui`.
- **Quilt**: `-jar quilt-server-launch-<MC>-<ver>.jar nogui`.
- **Vanilla**: `-jar minecraft_server.<MC>.jar nogui`.

`win_args.txt` is a Forge/NeoForge-generated file that already contains the modloader's own required JVM/classpath args — the Universalator injects `<MAXRAM>` and `<USEARGS>` (its own RAM + extra JVM args) ahead of the `@file` reference, then appends `nogui` and forwards any extra script args (`%*`).

---

## 7. Java Version Management

### 7.1 Selection by Minecraft version (`setjava`)
| MC condition | JAVAVERSION | User choice offered? |
|---|---|---|
| MCMAJOR ≤ 15 | 8 | No (only option) |
| MCMAJOR ≤ 16 and MCMINOR ≤ 4 | 8 | No |
| MCMAJOR ≤ 16 and MCMINOR ≥ 5 (i.e. 1.16.5) | 8 (default) | Yes — 8 or 11 |
| MCMAJOR == 17 | 16 | No |
| MCMAJOR ≥ 18 | 17 (default, may be overridden below) | — |
| MCMAJOR ∈ [18,19] | 17 (default) | Yes — 17, 21, or 25 |
| MCMAJOR == 20, MCMINOR ≤ 5 | 17 (default) | Yes — 17, 21, or 25 |
| MCMAJOR == 20, MCMINOR ≥ 6 | 21 (default) | Yes — 21 or 25 |
| MCMAJOR == 21 | 21 (default) | Yes — 21 or 25 |
| MCMAJOR > 21 | 25 | No (only option) |

If only one possible Java version exists for the MC version, the prompt is skipped entirely (an `ONLY=Y` flag short-circuits); if reached via the main-menu `J` command in that state, a one-line "only version possible" notice flashes instead of a prompt. Entered choice is re-validated against the same rule table (loops back on invalid choice). If changed via main-menu `J`, persists immediately to settings file.

### 7.2 Java acquisition/override (`java_checks`, `:override`)
`OVERRIDE` has 3 modes, cycled by the `OVERRIDE` command:
- **`A` (Automatic)**: default behavior — checks system-installed Java first (see below), falls back to Universalator-managed Adoptium download.
- **`J` (system PATH java)**: skips all detection, uses the literal command `java` (whatever resolves on PATH at launch time) — settings screen displays `CUSTOMJAVA` (captured `java -version` output) for the user's information.
- **`F` (Force Adoptium)**: skips system-Java detection entirely (`GOTO :skipsystemjavacheck`), always uses/installs the Universalator-managed Adoptium copy.

**System Java detection (mode A)**: Uses PowerShell to scan `C:\Program Files`, `C:\Program Files\Java`, `C:\Program Files\Eclipse Adoptium`, `C:\Program Files\Eclipse Foundation`, `C:\Program Files\Amazon Corretto`, `C:\Program Files\Zulu` for directories matching a regex against common JDK folder naming conventions (`jdk-N`, `temurin-N`, `jre-N...`, `zulu-N`, `jdk1.N`, `java-N`, `openjdk-N`) for the target major version, tagging each as "new" or "old" based on `CreationTime` vs. `MONTHS_OLD` (12 months) cutoff. First "new" match with a `bin\java.exe` present wins; reads the `release` file inside for `IMPLEMENTOR` and `JAVA_VERSION` display strings.

**Adoptium-managed Java (fallback / mode F)**: Checks `univ-utils\java\` for a folder matching `jdk8u` (Java 8) or `jdk-<N>` (else), tests its `bin\java.exe` LastWriteTime against a 6-month (`A_MONTHS`) cutoff.
- If found and fresh (<6 months) → use it.
- If found and stale → queries Adoptium's Features Releases API (`https://api.adoptium.net/v3/assets/feature_releases/<ver>/ga?...&image_type=<jdk|jre>&...`) for the current release name/version, compares against the folder name string; if it matches, keeps using the (still functionally current) old folder; if it doesn't match, deletes the folder and re-downloads.
- If not found at all → downloads fresh: queries the same Adoptium API for `binaries.package.link` and `binaries.package.checksum` (SHA256), downloads the ZIP via PowerShell WebClient, verifies checksum via `certutil -hashfile`, extracts with `tar -xf`, deletes the ZIP, loops back to re-verify.
- Java 16 uses `image_type=jdk` (no JRE was ever published for it, since it's not an LTS release); all other versions use `image_type=jre`.

### 7.3 Custom Java path
When `OVERRIDE=J`, no path is separately configured — the literal string `java` is used, relying entirely on the OS `PATH`. There is no facility to point at an arbitrary custom `javaw.exe`/`java.exe` file path beyond what's already on the system `PATH`. **Implementation note for Java rewrite**: consider whether to keep this PATH-only behavior or add an explicit custom-executable-path field — the .bat only supports the PATH-resolution flavor of "custom java."

---

## 8. RAM / JVM Arguments

- `ARGS` (default): `-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M` — user-editable via the raw settings file only (no menu editor for this specific field in the reviewed script), persisted per-server.
- `OTHERARGS` (script constant, always applied, never persisted/edited): `-XX:+IgnoreUnrecognizedVMOptions -Dlog4j2.formatMsgNoLookups=true`.
- `MAXRAMGIGS`: user-entered integer GB (`enter_ram`), validated: no decimals, strips stray `+`/`-`, must parse as an integer. Displays total & free system RAM (via PowerShell `Win32_OperatingSystem`) for reference; typical suggested range 4–10 GB.
- `MAXRAM` derived string: `-Xmx<MAXRAMGIGS>G` (computed at settings-read time, not stored).
- **At launch time**, a special rule: if `JAVAVERSION ≥ 17` **and** `ARGS` still equals the exact unmodified default string, the tool drops the G1GC tuning args entirely (`USEARGS` becomes empty) — reasoning given in comments: newer JVMs self-tune GC better than the manually-tuned flags. If the user has customized `ARGS` at all (even trivially), their custom args are used as-is regardless of Java version. Final combined args = `USEARGS + " " + OTHERARGS` (OTHERARGS always appended).
- Same default-args-suppression logic is duplicated in `gen_run_scripts` (§13) for generating standalone run scripts.
- **`run.sh`/`run.bat` variant**: `MAXRAM` is folded into `USEARGS` and (for MC>16) dumped into `user_jvm_args.txt`, since modern Forge/NeoForge natively read that file via `@user_jvm_args.txt`; for MC≤16, the ram/args are embedded directly in the generated script's launch line instead, and any stale `user_jvm_args.txt` is deleted to avoid confusion.

---

## 9. Port / Networking

### 9.1 Port settings
- `PORT` (TCP, the actual Minecraft game protocol port) default `25565`.
- `PORTUDP` (used by voice-chat-type mods) default `24454`.
- `PROTOCOL`: `TCP` / `UDP` / `BOTH` — controls which port(s) get UPnP-forwarded, cycled via the UPnP menu `TOGGLE` command.
- Port edit flows (main-menu `PORT` and UPnP-menu `PORT`) validate: numeric only, ≥10000, syncs `server.properties`'s `server-port` and the settings file. The UPnP-menu variant additionally offers to sync a detected `config\voicechat\voicechat-server.properties`'s `port=` line to match the chosen UDP port (rewrites that file preserving comments, via a temp-file + rename pattern since the file must be edited in its own directory).
- Startup always checks `NETSTAT -aon | FINDSTR <port>` for a conflicting listener; if found, resolves the owning PID via `TASKLIST`, and (unless the owning session is `SYSTEM`) offers `KILL` (via `TASKKILL /F /PID`) or `Q` (quit to resolve manually). **Implementation note**: the actual "skip if SYSTEM session" `GOTO :skipportclear` unconditionally follows right after the SYSTEM check regardless of match — meaning the `:portwarning` prompt block is effectively unreachable dead code in this script version (a latent bug/regression worth deciding whether to fix or intentionally drop in the reimplementation).

### 9.2 UPnP port forwarding
Handled by a bundled external helper executable, **not** reimplemented in batch itself:
- Binary: `univ-utils\Portforwarded\Portforwarded.Server.exe`, sourced from GitHub release `https://github.com/itssimple/Portforwarded.Server/releases/download/3.0.0-alpha/Portforwarder.Server-3.0.0-alpha-win-x64.zip`. Requires Microsoft .NET (`dotnet`) runtime on the host; the script checks `dotnet --version` and nudges the user toward newer .NET if <8.
- Downloaded ZIP is extracted (only `Portforwarded.Server.exe` is pulled out via `tar -xf ... Portforwarded.Server.exe`), and SHA256-verified against a hardcoded expected hash `163cdd5f32764bf9c5b70f74eecfca47a71038979ca63bd67fe340a477fd5144`.
- **Validation** (`upnp_validate`): runs the helper in `testmode="true"` against a throwaway vanilla MC 1.4.2 jar with a single TCP mapping on the configured port; looks for the string `"Created map for IP"` in its output to decide pass/fail. Uses whatever `java` is on PATH if present, else provisions Java via `java_checks`.
- **Actual launch wrapping** (in `launchserver`, if `USEPORTFORWARDED=Y` and helper exists): instead of invoking `java` directly, invokes:
  `Portforwarded.Server.exe executable:file="<javaexe>" executable:workingdirectory="<HERE>" executable:parameters="<launchline>" upnp:0:Protocol="Tcp" upnp:0:LocalPort=<PORT> upnp:0:PublicPort=<PORT> [upnp:1:Protocol="Udp" upnp:1:LocalPort=<PORTUDP> upnp:1:PublicPort=<PORTUDP>]` — the arg set varies by `PROTOCOL` (TCP-only / BOTH / UDP-only). The helper process launches Java as its child and manages the router UPnP mapping around its lifetime.
- CGNAT networks (public IP prefix `100.64.`) are pre-detected and UPnP menu access is blocked outright with guidance toward alternatives (e.g. playit.gg).

### 9.3 Firewall
`firewallcheck`: resolves the currently-configured Java executable's folder path (based on `JAVAVERSION` matching a folder name inside `univ-utils\java\`), then uses PowerShell `Get-NetFirewallProfile`/`Get-NetFirewallRule`/`Get-NetFirewallPortFilter`/`Get-NetFirewallApplicationFilter` to determine whether: (a) the Private firewall profile is even enabled (if not, treat as pass automatically), or (b) an enabled Inbound Allow rule exists whose port filter includes the current `PORT` on `TCP` and whose application filter's `Program` matches the resolved java.exe path exactly. Displays a pass/fail message with remediation guidance (delete conflicting rules, relaunch, click "Allow" on the Windows prompt). Only actionable after at least one successful launch has provisioned a Java folder (early-exits with an explanatory message otherwise).

---

## 10. Mod Handling

### 10.1 Client-mod scanning (`clientmodsscan` → `scanforgeneoforge` / `scanfabricquilt`)
Purpose: detect mods that are client-only (would crash a dedicated server) and offer to relocate them to a `CLIENTMODS\` folder.

**Forge/NeoForge path** (`scanforgeneoforge`):
- Downloads/refreshes (1-hour cache) a community-curated master list `clientonlymods.txt` from `https://raw.githubusercontent.com/nanonestor/utilities/main/clientonlymods.txt` (one modID per line, presumably).
- For MC > 12 (i.e. modern `mods.toml`/`neoforge.mods.toml` era): uses PowerShell to open every jar in `mods\`, extract `META-INF/mods.toml` or `META-INF/neoforge.mods.toml` via `tar xOf` (stdout extraction, no unzip to disk), and parse out `modId` and `clientSideOnly`.
  - **Preferred path**: if the PowerShell module `PSToml` is installed, uses proper TOML parsing (`ConvertFrom-Toml`) for accuracy.
  - **Fallback path**: if `PSToml` is not installed, uses a regex-based best-effort TOML scrape instead (less robust, explicitly acknowledged in comments as harder to reason about).
- For MC ≤ 12 (legacy `mcmod.info` JSON-in-jar format): extracts `mcmod.info`, parses as JSON, reads `modid` (or `[0].modid` if it's an array-rooted file).
- Independently moves any `*essential*.jar` whose contents contain `essential-loader.properties` straight to `CLIENTMODS\` (Essential is a well-known always-client-only mod, special-cased outside the TOML/JSON scan so it's caught even without valid metadata).
- Cross-references each collected mod's ID against `clientonlymods.txt` via `FINDSTR`, plus checks the parsed `clientSideOnly` flag directly. Either signal marks a mod as a client mod.
- Displays a column-aligned modID/filename report; offers to move matched files into `CLIENTMODS\` (created if needed).
- MC ≤ 12 shows an extra caveat that detection accuracy may be lower for mods without a proper `mcmod.info`.

**Fabric/Quilt path** (`scanfabricquilt`):
- For each jar, extracts `fabric.mod.json` (Fabric) or `quilt.mod.json` falling back to `fabric.mod.json` (Quilt), parses JSON for `id`, `environment` (`client`/`server`/`*`), and `depends` (dependency modIDs).
- A curated hardcoded override list of known-mismarked mod IDs is force-flagged as client regardless of their declared environment: `e4mc_minecraft moremcmeta_emissive_plugin mainhandswitch mobility notifyme removewardeneffect sparkle vs-wakes-compat wakes zoomify`.
- Cross-checks each `environment=client` mod's ID against the aggregated `depends` lists of **all** other scanned mods (written to a temp `fabricdeps.txt` for fast `FINDSTR` lookups rather than looping all in-memory) — a client mod that's a required dependency of another (non-client) mod is presumably excluded from "safe to remove" flagged results... **Implementation note**: actually reading the logic, mods found as a dependency of something else are simply *not* added to the removable list (the `INCLUDE=Y` flag only sets if the ID is *not* found in any dependency list) — i.e., mods needed by other mods are treated as not safely removable and are silently excluded from the report, which the user is not explicitly told; worth flagging as a UX gap to potentially improve in the Java version.
- Reports and offers move-to-`CLIENTMODS` exactly as the Forge path does.

### 10.2 MCreator mod detection (`mcreatorscan`)
Searches every jar in `mods\` for the literal byte strings `net/mcreator` or `/procedures/` (class package signatures MCreator-generated mods always contain) using `findstr /i /m` (list-matching-files mode). Writes a sorted list to `mcreator-mods.txt` in the root folder and displays it with a warning that MCreator mods are commonly poorly coded and cause server issues. No removal action offered — informational only.

### 10.3 Mods folder viewing (`mods_view`, `MODS`/`SMOD` commands)
`MODS`: lists **all** files/subfolders in `mods\` (unsorted, `DIR /B`). `SMOD`: lists only `*.jar` files, reverse-sorted (`SORT /R`). Both write `mods\*.jar` listing to `modslist.txt` and no-op with a friendly message if `mods\` has no jars.

---

## 11. CurseForge Integration

### 11.1 Registry lookup
Both the first-run auto-detect and the explicit `IMPORT` command query `HKCU\Software\Overwolf\Curseforge` value `minecraft_root` via `REG QUERY`. If absent, informs the user no CurseForge folder was found.

### 11.2 Profile discovery
Enumerates subdirectories of `<root>\Instances\` that contain a `minecraftinstance.json` file — each such subdirectory is a candidate "profile." Presents a numbered list (folder names relative to the Instances root) for selection.

### 11.3 Profile parsing
For the selected instance folder, parses `minecraftinstance.json` via PowerShell (one field at a time, separate PowerShell invocations per field — not batched):
- `baseModLoader.forgeVersion` → modloader version (field name is legacy but used regardless of actual loader).
- `gameVersion` → Minecraft version.
- `baseModLoader.name` → modloader type string, truncated to the substring before the first `-` (CurseForge encodes something like `forge-47.1.0`; only the prefix before `-` is kept as the loader name).
- `name` → the human-readable profile display name (for confirmation screen only).

Note: the dead-code existence check (`IF EXIST "!TEMP_MODLOADERVERSION!" ...`) is present but bypassed via an unconditional `goto :skip_cf_json_oops` immediately before it — meaning **no actual validation currently occurs** that all four fields were successfully parsed before proceeding; this is effectively dead/disabled validation code, an implementation note worth flagging for the Java rewrite (should probably re-enable proper null/empty checks).

### 11.4 Import/copy process
On confirmation:
1. Deletes (from the current server folder) a fixed list of "typical modpack" items if present, to avoid stale leftovers: `blueprints config crash-reports datapacks defaultconfigs kubejs local logs modernfix mods patchouli_books resourcepacks schematics scripts screenshots shaderpacks xaero usercache.json usernamecache.json server-icon.png`.
2. Copies everything from the CurseForge instance folder to the server folder **except**: `.curseclient`, `minecraftinstance.json`, `crash-reports`, `logs`, `fancymenu_data`, `natives`, `saves`, `screenshots`, `shaderpacks` (folders/files filtered by substring match on the top-level entry name) — folders copied recursively (`XCOPY /E /H /I /Y /Q`), files copied directly (`COPY /Y`).
3. Sets `MINECRAFT`, `MODLOADER` (normalized to uppercase canonical name), `MODLOADERVERSION` from the parsed values.
4. Runs `get_mcmajorminor`, `setjava`, `enter_ram` (still interactive for RAM — not imported from CurseForge), `stampsettingsfile`, then immediately offers `clientmodsscan`.

---

## 12. Server Lifecycle

### 12.1 EULA handling (`eula`)
- If `eula.txt` doesn't exist → must agree.
- If it exists → reads it line by line, uses PowerShell to normalize `=` to `#` per-line for safer string containment checks, flags "must agree" if a line contains `eula#` but not also `eula#true` (case-sensitivity note: this correctly handles the vanilla file's default comment line, which uses `#Between…` and `eula=false`, without misfiring).
- If must-agree: shows Mojang EULA URL (`https://account.mojang.com/documents/minecraft_eula`), requires the exact literal text `AGREE` (case-insensitive), then writes `eula.txt` containing exactly `eula=true`. Any other input loops the prompt indefinitely (no way to decline and proceed — declining exits only by closing the program).

### 12.2 Launch sequence (`launch_sequence` → dispatches to `launchserver`)
Order: `checkformodsfolder` (warn if no mods dir & non-vanilla, offer continue-anyway) → `java_checks` → loader-specific install check (Forge/NeoForge/Fabric/Quilt/Vanilla, whichever applies) → `eula` → `launchserver`.

### 12.3 Launch screen & command build (`launchserver`)
Displays a "ready to launch" summary (MC version, loader+version, Java source/version, public IP:port, LAN IP:port, `localhost` for same-machine testing). Prompts `M` (abort to main menu) or any other key to proceed.

Pre-launch housekeeping performed unconditionally on every launch attempt (not just first install): moves any `*essential*.jar` (verified via presence of `essential-loader.properties` inside) out of `mods\` into `CLIENTMODS\` — this nuisance-mod cleanup runs every single launch, independent of whether the user ever ran a full client-mod scan.

Builds `USEARGS`/final JVM args per §8, builds `LAUNCHLINE` per §6.7, sets console title to include MC version + loader, then launches either directly (`"<javafile>" <launchline>`) or wrapped through the UPnP helper (§9.2), depending on `USEPORTFORWARDED` and helper availability.

### 12.4 Auto-restart on crash (`RESTART` toggle, checked after the launch command returns)
- `RESTART` is a **session-only** variable — toggled via the `RESTART` command in the all-commands menu but **not persisted** to the settings file (unlike almost every other setting) — it resets to undefined every fresh script run. **Implementation note**: this looks like it may be an intentional safety choice (don't silently keep restarting across sessions) but should be a deliberate design decision in the Java port, not an accident.
- After the java process exits, if `RESTART=Y` and `logs\latest.log` exists: checks whether the log contains the string `"Stopping the server"` (a graceful, intentional `/stop`). If that string is **absent** (crash/unplanned exit) **and** `RESTARTCOUNT ≤ 5`, increments `RESTARTCOUNT` and loops back to relaunch the exact same command. `RESTARTCOUNT` resets to 0 whenever the main menu is freshly displayed (i.e., resets per session/menu-visit, not per individual launch attempt within the auto-restart loop).
- After restart-looping ends (either graceful stop detected, or the 5-attempt ceiling reached), calls `logsscan` for crash diagnostics, then `PAUSE`s and returns to main menu.

### 12.5 Log viewing / crash diagnostics (`logsscan`, also driven from `LOG`/`LOGS` command)
Scans `logs\latest.log` for known signature strings and prints tailored guidance blocks (does not modify anything, informational only). Skips entirely if the log shows `"Stopping the server"` (intentional shutdown, nothing to diagnose). Recognized signatures and guidance:
- `Unsupported class file major version` → Java/Forge version mismatch note.
- `invalid dist DEDICATED_SERVER` + `Loading errors encountered` + `Missing or unsupported mandatory dependencies:` (actually two separate blocks — see below) — client-side mod crash detection: extracts and lists every mod name found via `has failed to load correctly` lines.
- `FAILED TO BIND TO PORT` → port-in-use guidance.
- `Missing or unsupported mandatory dependencies:` → dependency/version mismatch guidance, lists every `Mod ID:` line found in the log.
- `Tried to read NBT tag with too high complexity, depth > 512` → NBT depth overflow guidance (suggests the "Long NBT Killer" mod).
- Final generic fallback message: "server may have crashed/stopped, check log files."

### 12.6 Backups
No backup functionality exists in this script. (Confirmed absent — not merely undocumented. The Java rewrite may choose to add this as a new feature, but it has no .bat precedent to replicate.)

---

## 13. Utility Features

### 13.1 Icon creation — see §3.5.

### 13.2 ZIP / Server-pack creation — see §3.4.

### 13.3 run.sh / run.bat generation (`gen_run_scripts`, `GENRUN` command)
Forge/NeoForge-only (explicitly refuses for Fabric/Quilt/Vanilla with an error message). Requires the modloader files to already be installed (checks for the presence of `unix_args.txt` alongside the already-detected `win_args.txt`, or a matched root jar for MC≤16) — if not installed yet, tells the user to `LAUNCH` first.
- Writes `run.sh` (`#!/usr/bin/env sh` + a `java ...` line) and `run.bat` (`@echo off` + a `java ...` line + `PAUSE`), always fully overwriting any prior versions.
- For MC ≤ 16: embeds `MAXRAM`/`ARGS`/`OTHERARGS` and the direct jar filename inline in the generated script.
- For MC > 16: uses the `@user_jvm_args.txt @libraries/.../unix_args.txt` (sh) / `win_args.txt` (bat) pattern, and separately writes the computed JVM args (RAM + ARGS/OTHERARGS, same Java-17+ default-suppression rule as launch) to `user_jvm_args.txt` in the root — this is the file Forge/NeoForge natively reads for user-supplied JVM args when launched via these generated scripts (or externally, without Universalator at all).

### 13.4 Purge function — see §3.10.

### 13.5 Linux settings conversion — see §2.2.

---

## 14. External Dependencies

### 14.1 Bundled/managed local files under `univ-utils\`
| Path | Purpose |
|---|---|
| `univ-utils\license.txt` | Download canary (GPL license text) |
| `univ-utils\version_manifest_v2.json` | Cached Mojang version manifest (refresh: 1 day) |
| `univ-utils\versions\<MC>.json` | Cached per-version Mojang metadata (never auto-refreshed once fetched) |
| `univ-utils\maven-fabric-metadata.xml` / `maven-quilt-metadata.xml` / `maven-forge-metadata.xml` / `maven-neoforge-metadata.xml` / `maven-neoforge-1.20.1-metadata.xml` | Cached loader maven-metadata (refresh: 6 hours) |
| `univ-utils\promotions_slim.json` | Cached Forge "latest per MC version" index (refresh: 6 hours) |
| `univ-utils\clientonlymods.txt` | Cached community client-mod-ID list (refresh: 1 hour) |
| `univ-utils\installers\` | Cached Forge/NeoForge/Fabric/Quilt installer jars (persist until purge or reinstall) |
| `univ-utils\java\<jdk-folder>\` | Adoptium-managed JDK installs (per major version, refresh check: 6 months) |
| `univ-utils\Portforwarded\Portforwarded.Server.exe` | UPnP helper binary (downloaded once, checksum-pinned) |
| `univ-utils\readme-server.txt` | Generated readme bundled into server-pack ZIPs |
| `univ-utils\Universalator-linux.sh` | Cached copy of the Linux sibling script, bundled into server-pack ZIPs |

### 14.2 External URLs / APIs contacted
| URL/Host | Purpose |
|---|---|
| `https://launchermeta.mojang.com/mc/game/version_manifest_v2.json` | Master MC version list |
| Per-version URL from the manifest (`piston-meta.mojang.com` typically) | Per-version metadata (server jar URL + SHA1) |
| `https://maven.minecraftforge.net/...` | Forge maven-metadata + installer jars |
| `https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json` | Forge "latest" index |
| `https://maven.neoforged.net/releases/...` | NeoForge maven-metadata + installer jars |
| `https://maven.fabricmc.net/...` | Fabric maven-metadata + installer jar |
| `https://maven.quiltmc.org/repository/release/...` | Quilt maven-metadata + installer jar |
| `https://api.adoptium.net/v3/assets/feature_releases/...` | Adoptium (Eclipse Temurin) JDK/JRE release lookup + binary download link + checksum |
| `https://raw.githubusercontent.com/nanonestor/universalator/main/LICENSE` | Download-canary file |
| `https://raw.githubusercontent.com/nanonestor/utilities/main/clientonlymods.txt` | Community curated client-only mod ID list |
| `https://raw.githubusercontent.com/nanonestor/universalator/latest-linux/Universalator-linux.sh` | Linux sibling script (bundled in server-pack ZIP) |
| `https://github.com/itssimple/Portforwarded.Server/releases/download/3.0.0-alpha/Portforwarder.Server-3.0.0-alpha-win-x64.zip` | UPnP helper binary release |
| `http://ip-api.com/json/?fields=query` | Public IP lookup (primary) |
| `https://api.ipify.org?format=json` | Public IP lookup (fallback) |
| DNS resolve targets checked for connectivity preflight | `maven.minecraftforge.net`, `maven.fabricmc.net`, `maven.quiltmc.org`, `maven.neoforged.net`, `launchermeta.mojang.com`, `piston-meta.mojang.com` |
| Ping targets (server reachability) | Same maven hosts as above, per active modloader |

### 14.3 External processes/tools invoked
| Tool | Usage |
|---|---|
| `powershell` / `powershell -Command` | The workhorse — JSON/XML parsing, WebClient downloads, `Get-CimInstance`, `Resolve-DnsName`, `Get-NetFirewall*` cmdlets, `System.Drawing` image generation, `Compress-Archive`, `Test-Path -OlderThan`, module checks (`PSToml`) |
| `curl` | Primary/fallback file download, `.sha256` checksum-string fetches |
| `certutil -hashfile <file> SHA256` / `SHA1` | Local file checksum computation (no native CMD hashing tool exists) |
| `tar` (Windows built-in bsdtar, explicitly must **not** be a GNU-tar shadow) | ZIP/tar extraction, and cleverly repurposed for reading a single file's contents *out of* a jar/zip to stdout (`tar xOf <jar> <entry>`) without full extraction — used extensively for scanning mod metadata inside jars |
| `ping` | Connectivity checks, and also abused as a cross-platform-in-CMD sleep/delay mechanism (`ping -n 2 127.0.0.1 >nul` ≈ ~1 second delay) |
| `reg query` | CurseForge registry key lookup (`HKCU\Software\Overwolf\Curseforge`) |
| `netstat` / `tasklist` / `taskkill` | Port-in-use detection and process termination |
| `dotnet --version` / `where dotnet` | .NET runtime presence check (required by the UPnP helper) |
| `ipconfig` | LAN IPv4 detection |
| `findstr`, `sort`, `dir`, `xcopy`, `copy`, `move`, `rd`, `del`, `ren` | Standard file/text operations throughout |
| `Portforwarded.Server.exe` | Third-party .NET UPnP client — wraps and launches the actual `java` process when UPnP forwarding is active, and independently used in a `testmode` for router-capability validation |

### 14.4 Windows-specific behavior needing cross-platform/Java-native equivalents
- **Registry access** (CurseForge folder detection) — Java has no direct registry API; would need JNA/JNI, a registry-reading library, or fall back to well-known CurseForge install paths / manual folder browsing on non-Windows.
- **Windows Firewall rule inspection** (`Get-NetFirewallRule` etc.) — Windows-only concept; on Java this either needs a native bridge or should become a no-op/platform-conditional feature.
- **`certutil`/PowerShell checksum computation** — trivially replaced by `java.security.MessageDigest` (SHA-1/SHA-256) — no external process needed at all in Java.
- **`tar xOf <jar> <entry>` (stream a single entry from inside an archive without full extraction)** — trivially replaced by `java.util.zip.ZipFile`/`ZipInputStream` (jars are zips) reading a specific entry — Java is actually *better suited* here than the batch script's process-shelling approach.
- **TOML parsing (`mods.toml`, `neoforge.mods.toml`)** — needs a Java TOML library (e.g., toml4j, night-config); the .bat's dependency on the optional `PSToml` PowerShell module vs. a regex fallback should be replaced by a proper Java TOML parser used unconditionally (removing the accuracy trade-off entirely).
- **JSON parsing** (Mojang manifests, per-version files, `fabric.mod.json`/`quilt.mod.json`, `mcmod.info`, `minecraftinstance.json`, Adoptium API, promotions_slim.json) — replace with a standard Java JSON library (Jackson/Gson).
- **XML parsing** (maven-metadata.xml files) — replace with standard Java XML (JAXB/DOM/StAX) or a small XPath-based reader.
- **`System.Drawing` icon generation** — replace with `java.awt.Graphics2D`/`BufferedImage`/`ImageIO` (very close 1:1 mapping: draw string, rotate transform, save PNG).
- **ZIP creation (`Compress-Archive`)** — replace with `java.util.zip.ZipOutputStream`.
- **DNS resolution checks (`Resolve-DnsName`)** — replace with `java.net.InetAddress.getByName()` / `getAllByName()` wrapped in try/catch for UnknownHostException.
- **Ping** — Java's `InetAddress.isReachable()` is a rough analog but is notoriously unreliable across networks/firewalls (often requires raw ICMP privileges); may be preferable to instead attempt an actual small HTTP/TLS handshake to the target host as a "is it reachable" substitute, or shell out to the OS `ping` command cross-platform if true ICMP semantics matter.
- **UPnP port forwarding** — the .bat entirely delegates this to the external `Portforwarded.Server.exe` (.NET, Windows-only-ish though the repo suggests it may support other platforms via .NET's cross-platform runtime — verify at implementation time). Java has native UPnP libraries (e.g., `weupnp`, Cling) that could replace this dependency entirely rather than shelling out to an external binary — recommended architectural change for the reimplementation, removing the Windows/.NET dependency.
- **Console ANSI colors (`color 1E`, VT100-style `[34;103m` sequences)** — irrelevant in a GUI; simply becomes standard UI styling/theming.
- **PATH/environment variable manipulation, `_JAVA_OPTIONS` conflict detection** — `System.getenv()` is a direct read-only equivalent; no session-scoped PATH mutation is needed in a GUI app since it's not shelling to further CMD commands the same way.
- **`SET /P` free-text prompts with strict validate-loop UX** — becomes standard GUI form fields/dialogs with inline validation; the loop-until-valid pattern maps directly to form validation with error messages instead of re-prompting a terminal.
- **File-name-encoded version number** (`Universalator-2.58.bat` → `UNIV_VERSION=2.58`) — Java app should get its version from a manifest/build property instead of the executable's filename.

---

## 15. Edge Cases & Validation Logic Worth Preserving

- **Only `release`-type Minecraft versions are accepted** — snapshots/betas/alphas rejected outright at entry time (§5.2).
- **Trimming**: every free-text entry point trims exactly one trailing space repeatedly via the `:trim` utility (handles multiple trailing spaces via a loop) — should be a standard `.trim()`/`.strip()` in Java, but note it only strips **trailing** spaces in the original, never leading (an asymmetry to consciously decide whether to keep or fix).
- **DNS/ping failure messaging**: distinct, itemized failure screens naming exactly which hosts failed to resolve (modloader maven + both Mojang metadata hosts checked together, once per session via `DNSANDPINGPASSEDBEFORE`/individual `DNSFAIL*` flags) with actionable guidance (switch to 1.1.1.1/8.8.8.8 public DNS). Ping failures get a distinct "poor connection or file server offline" retry loop, separate from DNS failures.
- **Checksum verification** is implemented and actively enforced for: Forge/NeoForge installer jars (SHA256 via `.sha256` sidecar), Fabric/Quilt installer jars (same), vanilla server jar (SHA1, from Mojang's per-version JSON), Adoptium JDK ZIP (SHA256, from Adoptium API), Portforwarded.Server.exe (SHA256, hardcoded expected value). It is **commented out / dead code** for: the loader maven-metadata XML files themselves (§6.1 note) — worth reconsidering whether to add for completeness in the Java version, since metadata corruption could otherwise silently cause bad version-resolution results.
- **Folder path validation** is extensive and blocking (not just warnings) for: trailing special characters, embedded exclamation marks, embedded spaces, embedded square brackets, running from Desktop/OneDrive/Documents/Downloads/`.minecraft`/XboxGames/Program Files/`C:\`, or inside known launcher-app folders (CurseForge, ATLauncher, GDLauncher, PrismLauncher, ModrinthApp). Quilt additionally hard-blocks on **any** space anywhere in the full path (stricter than the general check, which only blocks embedded spaces in `!HERE!` broadly — actually the general space-check already blocks any space in the path, so the Quilt-specific check is redundant but present as a defensive belt-and-suspenders check). **Implementation note**: in a Java GUI, most of these become soft warnings with an "I understand, continue anyway" affordance rather than hard `PAUSE & EXIT`s, since a GUI installer typically lets the user pick any folder via a file chooser — worth a deliberate product decision on how strict to remain.
- **Disk space / environment sanity checks** (free space <20GB or >95% used, conflicting `_JAVA_OPTIONS`-style env vars with `-Xmx`/`-Xmn`, hosts-file redirects for Mojang domains, GNU-tar-shadowing-Windows-tar, missing core CMD tools) are all **soft/bypassable** warnings (`PAUSE` then continues) except the GNU-tar and missing-core-tools/PowerShell checks, which are **hard blocks** (the script cannot function without real functioning tar/PowerShell).
- **NETSTAT port-conflict resolution has a confirmed dead-code path** (§9.1) — the interactive kill-or-quit prompt is unreachable due to an unconditional `GOTO` immediately preceding it; currently the script silently does nothing if the port appears in use (aside from computing `FOUNDOPENPORT`/`PIDNUM`/`IMAGENAME` that are then discarded). Decide explicitly whether the Java version should implement the *intended* behavior (prompt to kill) or the *actual current* behavion (silently proceed) — recommend implementing the intended behavior since it's clearly more useful and the omission looks like an unintentional regression.
- **CurseForge-import field validation is dead code** (§11.3) — an unconditional `goto` skips the intended "were all four fields non-empty" check. Recommend fixing this in the Java rewrite (guard against a botched/partial `minecraftinstance.json`).
- **Auto-restart state is not persisted** across script runs (§12.4) — a deliberate-or-accidental safety behavior to make an explicit product decision about when porting.
- **`server.properties` PROPS-menu port edits don't sync the Universalator settings file** (§2.3) — inconsistent with the dedicated Port menu, which does sync both directions; likely an oversight to fix in the Java version (should always keep `server.properties.server-port` and the app's own port setting mutually consistent regardless of which UI path triggered the change).
- **Fabric/Quilt client-mod dependency exclusion is silent** (§10.1) — mods excluded from the "safe to remove" list because another mod depends on them are not flagged to the user as "found but kept because required" — purely omitted from the report. Consider surfacing this distinction explicitly in the Java UI (e.g., a separate "client mods kept because required as a dependency" section) for better transparency.
- **Windows version gate**: hard-blocks any Windows version internally numbered ≤9 (i.e., older than Windows 10) — not really relevant to a cross-platform Java rewrite, but documents that the original tool's minimum supported OS was Windows 10.
- **Seasonal Easter egg**: displays a rainbow-colored ASCII banner header specifically during the month of June, otherwise a plain yellow banner — purely cosmetic, optional to replicate.
- **`RESTARTCOUNT` reset semantics**: resets to 0 every time the main menu is freshly (re)displayed, not per individual restart-loop pass — meaning if a user manually returns to the main menu after some crashes and then relaunches, they get a fresh budget of 5 auto-restarts, rather than a lifetime cap.
