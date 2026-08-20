Based on the batch script in `Universalator-2.54.txt` , the Java version should be designed as a full-featured **Minecraft Server Manager / Installer / Launcher** application.

Here’s a structured breakdown of what the Java program should consist of:

---

# Core Architecture

## 1. Main Application Controller

Responsible for:

* Starting the application
* Loading configs
* Initial system checks
* Opening menus/UI
* Routing commands
* Managing application state

Suggested classes:

```java
Main.java
ApplicationManager.java
MenuController.java
```

---

# Configuration System

## 2. Settings / Config Management

The BAT script heavily relies on variables and a settings text file.

Java equivalent:

* Config file handling
* Read/write settings
* Default settings generation
* Validation

Should support:

* Minecraft version
* Modloader type
* Modloader version
* Java version
* RAM allocation
* JVM args
* Ports
* UPNP settings
* Restart settings
* Override settings

Suggested:

```java
ConfigManager.java
Settings.java
```

Possible formats:

* JSON
* YAML
* TOML
* Properties

(JSON is easiest)

---

# User Interface

## 3. UI System

The BAT file uses text menus.

In Java you could use:

### Option A — Console UI

Simpler.

Use:

```java
Scanner
JLine
Lanterna
```

### Option B — GUI

Recommended.

Use:

* JavaFX
* Swing

GUI should contain:

* Dashboard
* Settings panels
* Logs viewer
* Install progress
* Server controls
* Mod scanner
* Status indicators

---

# Minecraft Version Handling

## 4. Minecraft Version Management

The BAT validates versions using Mojang manifests.

Java should:

* Download version manifests
* Parse JSON
* Validate versions
* Extract:

    * major
    * minor
    * hotfix

Should support:

* old versions
* new yearly versioning

Suggested:

```java
MinecraftVersionService.java
VersionParser.java
MojangManifestService.java
```

---

# Modloader Support

## 5. Modloader Management

Support:

* Forge
* NeoForge
* Fabric
* Quilt
* Vanilla

Program must:

* Download metadata
* Parse Maven XML
* Detect latest versions
* Validate compatibility
* Install loaders

Suggested classes:

```java
ModLoaderManager.java
ForgeInstaller.java
NeoForgeInstaller.java
FabricInstaller.java
QuiltInstaller.java
VanillaInstaller.java
```

---

# Internet & Download System

## 6. Download Manager

The BAT script downloads:

* metadata files
* manifests
* Java runtimes
* server jars

Java program should include:

* async downloads
* retry logic
* checksum validation
* progress bars
* mirror fallback

Suggested:

```java
DownloadManager.java
ChecksumValidator.java
HttpService.java
```

Libraries:

* OkHttp
* Apache HttpClient

---

# XML / JSON Parsing

## 7. Metadata Parsing

Needed for:

* Maven metadata XML
* Mojang JSON manifests
* Forge promotions JSON

Use:

```java
Jackson
Gson
javax.xml
```

---

# Java Runtime Management

## 8. Java Detection & Installation

One of the biggest systems in the BAT.

Program must:

### Detect installed Java versions

Search:

* PATH
* Program Files
* Registry
* Custom folders

### Determine compatibility

Example logic:

| Minecraft   | Required Java |
| ----------- | ------------- |
| 1.16.5      | 8             |
| 1.17        | 16            |
| 1.18–1.20.4 | 17            |
| 1.20.6+     | 21            |
| 1.21+       | 21/25         |

### Install Java automatically

Should:

* Download Adoptium JRE/JDK
* Extract archives
* Store local runtimes

Suggested classes:

```java
JavaManager.java
JavaDetector.java
JavaDownloader.java
JavaCompatibilityService.java
```

---

# Server Launch System

## 9. Minecraft Server Launcher

Core feature.

Must:

* Build launch command
* Apply JVM args
* Apply RAM limits
* Launch correct jar
* Capture console output
* Detect crashes
* Restart if enabled

Suggested:

```java
ServerLauncher.java
ProcessManager.java
ConsoleBridge.java
```

Use:

```java
ProcessBuilder
```

---

# Log Management

## 10. Logs Viewer

Features:

* Live console
* Saved logs
* Crash log parsing
* Error highlighting

Suggested:

```java
LogManager.java
CrashAnalyzer.java
```

---

# Mod Scanner System

## 11. Mod Analysis

The BAT script scans mods.

Java program should:

* Scan `/mods`
* Read `.jar` metadata
* Detect:

    * client-only mods
    * MCreator mods
    * duplicates
    * incompatible mods
    * missing dependencies

Suggested:

```java
ModScanner.java
JarMetadataReader.java
DependencyAnalyzer.java
```

Use:

```java
java.util.jar.JarFile
```

---

# Networking Features

## 12. Port & Network Management

Should include:

* Port editing
* UPNP support
* Firewall checks
* DNS checks
* Ping tests

Suggested:

```java
NetworkManager.java
UPNPManager.java
FirewallChecker.java
DNSChecker.java
```

Libraries:

* Cling (UPNP)
* Java networking APIs

---

# CurseForge Integration

## 13. CurseForge Profile Importer

BAT imports CurseForge profiles.

Java version should:

* Detect CurseForge installation
* Read profile manifests
* Import:

    * mods
    * configs
    * versions

Suggested:

```java
CurseForgeImporter.java
ProfileScanner.java
```

---

# File Management

## 14. File Utilities

Features needed:

* ZIP creation
* Folder cleanup
* Icon generation
* Script generation
* Config editing

Suggested:

```java
ZipManager.java
FileUtilities.java
IconGenerator.java
```

---

# Validation Systems

## 15. Validation Layer

The BAT script validates almost everything.

Java version should validate:

* version numbers
* RAM values
* Java compatibility
* file existence
* URLs
* checksums
* ports

Suggested:

```java
ValidationService.java
```

---

# Error Handling

## 16. Error Recovery System

Must include:

* retry mechanisms
* user-friendly errors
* fallback downloads
* corrupted file recovery

Suggested:

```java
ErrorHandler.java
RecoveryManager.java
```

---

# Auto Update System

## 17. Self-Updater (Optional)

Could include:

* app updates
* metadata cache updates
* loader updates

---

# Recommended Project Structure

```text
src/
 ├── app/
 ├── config/
 ├── ui/
 ├── minecraft/
 ├── modloader/
 ├── java/
 ├── network/
 ├── downloads/
 ├── logs/
 ├── mods/
 ├── server/
 ├── utils/
 └── validation/
```

---

# Recommended Technologies

## Best Stack

### Language

* Java 21

### UI

* JavaFX

### Build Tool

* Gradle

### JSON

* Jackson

### HTTP

* OkHttp

### Logging

* Logback

### XML

* JAXB / DOM / SAX

---

# Recommended Extra Features

Things you can improve over the BAT script:

## Better GUI

* Real-time graphs
* RAM monitor
* CPU monitor
* Download progress
* Dark mode

## Multi-server support

Manage multiple Minecraft servers.

## Plugin system

Allow custom installers/extensions.

## Backup system

Automatic world backups.

## Modpack templates

One-click installs.

## Docker support

Containerized servers.

---

# Biggest Systems To Build First

Priority order:

1. Config system
2. Minecraft version parser
3. Java detection
4. Downloader
5. Modloader installers
6. Server launcher
7. GUI
8. Mod scanner
9. Networking tools
10. Auto-update systems

---

# Overall

This BAT script is essentially:

> A full Minecraft server management platform disguised as a batch file.

The Java rewrite is large enough to be considered a real desktop application, not just a launcher.
