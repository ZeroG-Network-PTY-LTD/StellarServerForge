# Mod Loaders

StellarServerForge supports Forge, NeoForge, Fabric, Quilt, and Vanilla servers, chosen in the
setup wizard and changeable later from Settings ("Change modloader version").

## Version metadata sources

`ModLoaderMetadataService` (in `modloader/`) fetches version listings per loader:

- **Forge** — `promotions_slim.json` (Forge's official promotions feed).
- **NeoForge** — Maven metadata XML.
- **Fabric** / **Quilt** — Maven metadata XML.

`MavenMetadata` parses the shared XML format for the three Maven-based loaders.

## Version resolution

`ModLoaderVersionResolver` picks the right loader version for the selected Minecraft version.
NeoForge gets special handling: it has switched numbering schemes over its lifetime (a legacy
`<mc-version>-<build>` style versus a newer hotfix-style scheme), so the resolver has to
distinguish which regime a given Minecraft version falls under rather than assuming one format.

## Install process

- **Forge / NeoForge** (`ForgeNeoForgeInstaller`) — downloads the loader's installer jar and runs
  it against the server directory, verifying checksums before executing.
- **Fabric / Quilt** (`FabricQuiltInstaller`) — downloads and runs the equivalent installer for
  those loaders.

Both installers verify checksums before trusting/running a downloaded installer jar.

## Launch line construction

`ModLoaderLaunchLine` builds the correct server launch command per loader, including handling
`win_args.txt`/`user_jvm_args.txt`-style argument files that newer Forge/NeoForge versions use
instead of (or alongside) a plain classpath invocation. `LaunchArgsBuilder` then merges this with
the JVM/RAM arguments (see [Settings Reference](Settings-Reference.md)) before
`ServerProcessRunner` actually starts the process.

## Vanilla

Vanilla servers skip all of the above — `VanillaInstallService` (in `mojang/`) downloads the
official server jar directly from Mojang's manifest and SHA1-verifies it.
