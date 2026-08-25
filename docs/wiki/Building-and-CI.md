# Building and CI

## Gradle tasks

| Task | Command | Notes |
|---|---|---|
| Compile | `./gradlew compileJava` | |
| Unit tests | `./gradlew test` | Excludes tests tagged `live`. |
| Live network tests | `./gradlew testLive` | Hits real services (Mojang, Modrinth, etc.) — not run by default or in CI. |
| Fat jar | `./gradlew shadowJar` | Produces `build/libs/StellarServerForge-<version>-all.jar`, main class `com.zerog.stellarserverforge.Main`. |
| Windows native app | `./gradlew jpackage` | Runs `jpackage --type app-image` against the shaded jar; output at `build/jpackage/StellarServerForge/`. Windows-only (uses `jpackage.exe`). |
| Full build | `./gradlew clean build` | Compile + test + package. |

Build tool: Gradle with the Kotlin DSL (`build.gradle.kts`). Java toolchain: 21.

Key dependencies: `jackson-databind` (JSON), `weupnp` (UPnP), `night-config-toml` (Forge/NeoForge
`mods.toml` parsing), `jna-platform` (Windows registry access for CurseForge import).

## CI: build, release, and Discord notification

`.github/workflows/discord-release-notify.yml` runs on every push to `main`:

1. Checks out the repo, sets up JDK 21 (Temurin).
2. Builds the shaded jar (`./gradlew shadowJar --no-daemon`).
3. Packages it as `StellarServerForge_v<short-sha>.jar`.
4. Creates a GitHub Release tagged `build-<short-sha>` with the **real jar attached directly**
   (not zipped).
5. Posts a rich embed to a Discord webhook announcing the new build, with a direct download link.

Skip a push's CI run by including `[skip ci]` anywhere in the commit message — useful for
metadata-only or non-code changes (catalog tweaks, docs) that don't warrant a full rebuild.

### Separate: raw GitHub activity in Discord

Commit/PR/issue activity is mirrored into Discord separately, via a **native GitHub repo webhook**
pointed directly at Discord's `/github`-formatted webhook endpoint — Discord renders these events
natively, so there's no Actions workflow involved for that feed. This is independent of the
release-announcement embed above (different webhook, different purpose).

## Packaging notes

- `shadowJar` uses `com.gradleup.shadow`, producing a single runnable jar with all dependencies
  bundled — no separate classpath setup needed to run it.
- `jpackage` bundles a Java runtime image, so the resulting `.exe` doesn't require end users to
  have Java installed separately. It's an `app-image` (a folder you can zip/distribute), not an
  installer/MSI.
