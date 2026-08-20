# Architecture Documentation

**Stellar Server Forge v1.0.0**  
**Technical Architecture Overview**

---

## 📋 Table of Contents

- [System Overview](#system-overview)
- [Architecture Layers](#architecture-layers)
- [Core Components](#core-components)
- [Data Flow](#data-flow)
- [Technology Stack](#technology-stack)
- [Design Patterns](#design-patterns)
- [Security Architecture](#security-architecture)
- [Extension Points](#extension-points)

---

## 🏗️ System Overview

Stellar Server Forge is a **desktop GUI application** built with Java Swing that automates the creation and management of Minecraft modded servers. The architecture follows a **layered design** with clear separation of concerns.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│              (Swing GUI + FlatLaf Theme)                │
│  MainWindow | ServerConfigDialog | ModInstallerDialog  │
│           ServerLauncherDialog                          │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                   Business Logic Layer                   │
│  ServerManager | JavaManager | Installer Classes        │
│        MojangManifestService                            │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                   Data Access Layer                      │
│   API Clients (CurseForge, Modrinth, Mojang)           │
│   File Operations | Configuration Management            │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                   Infrastructure Layer                   │
│   HTTP (OkHttp) | JSON (Gson) | Logging (SLF4J)        │
│   File System | Process Management                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Architecture Layers

### 1. Presentation Layer (GUI)

**Purpose:** User interaction and display

**Components:**
- `MainWindow.java` - Main application window with 4 action buttons
- `ServerConfigDialog.java` - Server configuration form (450 lines)
- `ServerLauncherDialog.java` - Live server control interface (540 lines)
- `ModInstallerDialog.java` - Mod browsing and installation (650 lines)

**Key Features:**
- FlatLaf dark theme for modern appearance
- SwingWorker for async operations (prevents UI freezing)
- Event-driven architecture
- Real-time progress indicators
- Responsive design

**Design Principles:**
- Separation from business logic
- No direct file/network operations
- All long-running tasks in background threads
- User feedback on all operations

### 2. Business Logic Layer

**Purpose:** Core application functionality

**Components:**

**Server Management:**
- `ServerManager.java` (~350 lines)
  - Creates server directory structure
  - Generates start scripts
  - Manages server.properties
  - EULA acceptance
  - Server lifecycle control

**Java Management:**
- `JavaManager.java` (~380 lines)
  - Cross-platform Java detection
  - Version parsing and compatibility checking
  - MC version → Java version mapping
  - Best Java selection algorithm

**Mod Loader Installers:**
- `ForgeInstaller.java` (~650 lines) - Forge automation
- `FabricInstaller.java` (~350 lines) - Fabric automation
- `QuiltInstaller.java` (~350 lines) - Quilt automation
- `NeoForgeInstaller.java` (~400 lines) - NeoForge automation

**Version Management:**
- `MojangManifestService.java` (~350 lines)
  - Fetches Mojang version manifest
  - Downloads official server.jar
  - SHA1 verification
  - Version validation

### 3. Data Access Layer

**Purpose:** External system interaction

**API Clients:**
- `CurseForgeClient.java` - CurseForge API integration
- `ModrinthClient.java` - Modrinth API integration

**Configuration:**
- `SecureConfig.java` - Configuration management
- `KeyVault.java` - Encrypted API key storage

**Responsibilities:**
- HTTP communication (via OkHttp)
- JSON parsing (via Gson)
- API authentication
- Rate limiting (future)
- Error handling

### 4. Infrastructure Layer

**Purpose:** Low-level services

**Technologies:**
- **OkHttp** - HTTP client
- **Gson** - JSON serialization/deserialization
- **SLF4J + Logback** - Logging framework
- **Java NIO** - File operations
- **ProcessBuilder** - Server execution

---

## 🔧 Core Components

### Entry Point

```java
Main.java
├─ Initializes logging
├─ Sets look and feel (FlatLaf)
├─ Creates MainWindow
└─ Starts event dispatch thread
```

### Configuration System

```
SecureConfig
├─ Loads application properties
├─ Manages default values
├─ Creates config directory
└─ Provides getters for settings

KeyVault
├─ Encrypts/decrypts API keys
├─ Supports external + embedded keys
├─ Handles key generation
└─ Secure storage
```

### Server Lifecycle

```
Server Creation Flow:
1. User Configuration (ServerConfigDialog)
   └─ Collects: name, version, loader, RAM, port
   
2. Server Setup (ServerManager)
   ├─ Create directory structure
   ├─ Accept EULA
   ├─ Download server.jar (MojangManifestService)
   ├─ Verify SHA1
   ├─ Generate server.properties
   └─ Create start script
   
3. Mod Loader Installation (Installer classes)
   ├─ Detect version compatibility
   ├─ Download installer/server JAR
   ├─ Execute installation
   └─ Verify installation
   
4. Mod Installation (ModInstallerDialog + API clients)
   ├─ Search mods (CurseForge/Modrinth)
   ├─ Download mod JARs
   └─ Place in mods/ directory
   
5. Server Launch (ServerLauncherDialog)
   ├─ Build Java command
   ├─ Start process
   ├─ Capture console output
   └─ Monitor status
```

---

## 📊 Data Flow

### Configuration Flow

```
User Input (GUI)
    ↓
ServerConfig Model
    ↓
SecureConfig (Persistence)
    ↓
Properties File (config/stellar-forge.properties)
```

### Mod Installation Flow

```
User Search (ModInstallerDialog)
    ↓
API Client (CurseForge/Modrinth)
    ↓
ModInfo Models
    ↓
Display in GUI
    ↓
User Click "Install"
    ↓
Download Manager (SwingWorker)
    ↓
Save to mods/ directory
```

### Server Launch Flow

```
User Click "Start Server" (ServerLauncherDialog)
    ↓
ServerManager.buildLaunchCommand()
    ↓
JavaManager (find appropriate Java)
    ↓
ProcessBuilder (execute command)
    ↓
Output Stream Reader (console)
    ↓
Display in GUI (real-time)
```

---

## 💻 Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 11 | Core language |
| **Maven** | 3.6+ | Build system |
| **Swing** | JDK | GUI framework |
| **FlatLaf** | 3.2+ | Look and feel |
| **OkHttp** | 4.x | HTTP client |
| **Gson** | 2.10+ | JSON parsing |
| **SLF4J** | 2.0+ | Logging API |
| **Logback** | 1.4+ | Logging implementation |

### Why These Choices?

**Java 11:**
- Wide compatibility (most servers have Java 11+)
- LTS release
- Module system support

**Maven vs Gradle:**
- More mature and stable
- Better IDE integration
- Simpler dependency management
- Easier for contributors

**Swing vs JavaFX:**
- No additional runtime required
- Better cross-platform compatibility
- Mature and stable
- FlatLaf provides modern appearance

**OkHttp:**
- Industry-standard HTTP client
- Async support
- Connection pooling
- Built-in retry mechanisms

**Gson:**
- Simple API
- Fast performance
- No annotations required
- Type-safe

---

## 🎨 Design Patterns

### 1. Model-View-Controller (MVC)

**Model:**
- `ServerConfig` - Server configuration data
- `ModInfo` - Mod information data

**View:**
- All dialog classes (GUI components)

**Controller:**
- Manager classes (ServerManager, JavaManager)
- Installer classes

### 2. Singleton Pattern

**Used in:**
- `SecureConfig` - Single configuration instance
- `KeyVault` - Single key storage instance

```java
public class SecureConfig {
    private static SecureConfig instance;
    
    public static SecureConfig getInstance() {
        if (instance == null) {
            instance = new SecureConfig();
        }
        return instance;
    }
}
```

### 3. Factory Pattern

**Used in:**
- Mod loader installer selection based on type

```java
public static Installer getInstaller(ModLoader loader) {
    switch (loader) {
        case FORGE: return new ForgeInstaller();
        case FABRIC: return new FabricInstaller();
        case QUILT: return new QuiltInstaller();
        case NEOFORGE: return new NeoForgeInstaller();
        default: throw new IllegalArgumentException();
    }
}
```

### 4. Observer Pattern

**Used in:**
- Progress tracking (PropertyChangeListener)
- UI updates from background tasks

```java
SwingWorker<Void, Integer> worker = new SwingWorker<>() {
    @Override
    protected Void doInBackground() {
        // Long-running task
        publish(50); // Update progress
        return null;
    }
    
    @Override
    protected void process(List<Integer> chunks) {
        progressBar.setValue(chunks.get(chunks.size() - 1));
    }
};
```

### 5. Strategy Pattern

**Used in:**
- Different mod loader installation strategies
- API client selection (CurseForge vs Modrinth)

---

## 🔐 Security Architecture

### API Key Management

```
KeyVault
├─ AES Encryption for stored keys
├─ External file support (config/api-keys.properties)
├─ Embedded fallback keys
└─ Secure key generation
```

**Security Measures:**
1. **Encryption at Rest** - Keys encrypted with AES
2. **External Storage** - Keys in separate file (.gitignore)
3. **No Hardcoding** - No keys in source code
4. **Validation** - Keys validated at startup

### Download Security

**Integrity Verification:**
```
Download Flow:
1. Request file from official API
2. Download over HTTPS
3. Calculate SHA1 hash
4. Compare with expected hash
5. Accept or reject
```

**Official Sources Only:**
- Mojang API for server.jar
- CurseForge API for mods
- Modrinth API for mods
- Official mod loader sites

### Process Security

**Server Execution:**
```
ProcessBuilder Security:
├─ Controlled working directory
├─ No shell execution
├─ Argument validation
├─ Output sanitization
└─ Proper cleanup on exit
```

---

## 🔌 Extension Points

### Adding New Mod Loaders

**Interface to implement:**
```java
public interface ModLoaderInstaller {
    boolean install(String mcVersion, Path serverPath);
    boolean isInstalled(Path serverPath);
    String getLatestVersion(String mcVersion);
}
```

**Steps:**
1. Create new class in `installer/` package
2. Implement ModLoaderInstaller interface
3. Add to ModLoader enum
4. Update factory method
5. Add to UI dropdown

### Adding New Mod Platforms

**Interface to implement:**
```java
public interface ModPlatformClient {
    List<ModInfo> searchMods(String query, String mcVersion);
    String getDownloadUrl(String modId, String versionId);
    List<ModInfo> getSuggestedMods(String mcVersion, ModLoader loader);
}
```

**Steps:**
1. Create new class in `api/` package
2. Implement ModPlatformClient interface
3. Add API configuration to SecureConfig
4. Update ModInstallerDialog to include platform
5. Add UI toggle/tab

### Adding New Features

**Recommended approach:**
1. Create feature branch
2. Add business logic in `manager/` package
3. Create GUI in `gui/` package
4. Add configuration if needed
5. Update documentation
6. Submit pull request

---

## 📦 Build Architecture

### Maven Build Phases

```
mvn clean          # Clean target directory
mvn compile        # Compile Java sources
mvn test           # Run tests (future)
mvn package        # Create JAR
mvn install        # Install to local repo
```

### JAR Structure

```
stellar-server-forge-1.0.0.jar
├─ META-INF/
│  └─ MANIFEST.MF (main class defined)
├─ com/zerog/network/stellarforge/ (application classes)
├─ dependencies/ (all shaded dependencies)
└─ resources/ (logback.xml, etc.)
```

**Fat JAR Benefits:**
- Single file distribution
- All dependencies included
- No classpath issues
- Easy to run

---

## 🔄 Async Architecture

### Background Tasks

**All long-running operations use SwingWorker:**

```java
public class DownloadTask extends SwingWorker<File, Integer> {
    @Override
    protected File doInBackground() throws Exception {
        // Download file
        // Update progress via publish()
        return downloadedFile;
    }
    
    @Override
    protected void process(List<Integer> chunks) {
        // Update UI progress bar
    }
    
    @Override
    protected void done() {
        try {
            File result = get();
            // Update UI with result
        } catch (Exception e) {
            // Handle error in UI
        }
    }
}
```

**Benefits:**
- UI never freezes
- Progress tracking
- Easy cancellation
- Exception handling

---

## 📈 Performance Considerations

### Optimization Strategies

**1. Lazy Loading:**
- Configuration loaded on demand
- API clients initialized when needed
- Java installations discovered once

**2. Caching:**
- Version manifests cached
- Java installations cached
- API responses cached (future)

**3. Efficient I/O:**
- Buffered streams for file operations
- NIO for large files
- Proper resource cleanup (try-with-resources)

**4. Thread Management:**
- SwingWorker for CPU tasks
- Background threads for I/O
- Proper thread cleanup

---

## 🧪 Testing Strategy (Future)

### Planned Test Architecture

```
src/test/java/
├─ unit/            # Unit tests (JUnit)
├─ integration/     # Integration tests
├─ ui/              # UI tests (AssertJ Swing)
└─ fixtures/        # Test data
```

**Test Coverage Goals:**
- Unit tests for business logic (80%+)
- Integration tests for API clients
- UI tests for critical workflows
- Performance tests for large operations

---

## 📝 Logging Architecture

### Logging Levels

```
ERROR   - Critical errors requiring attention
WARN    - Important warnings
INFO    - General information (default)
DEBUG   - Detailed debugging info
TRACE   - Very detailed tracing
```

### Logging Strategy

```java
private static final Logger logger = 
    LoggerFactory.getLogger(ClassName.class);

// Error logging
logger.error("Failed to download file", exception);

// Info logging
logger.info("Server started successfully on port {}", port);

// Debug logging
logger.debug("Processing mod: {}", modInfo);
```

**Configuration:** `src/main/resources/logback.xml`

---

## 🎯 Future Architecture Enhancements

### Planned Improvements

**1. Plugin System:**
- Define plugin interface
- Dynamic plugin loading
- Plugin marketplace

**2. Database Layer:**
- SQLite for server history
- Mod cache database
- Configuration history

**3. Network Layer:**
- WebSocket for real-time updates
- REST API for external control
- Remote server management

**4. Event System:**
- Application-wide event bus
- Plugin event hooks
- Async event processing

---

## 📚 References

**Design Inspiration:**
- Java Swing Best Practices
- Maven Standard Directory Layout
- Clean Code principles
- SOLID principles

**External Documentation:**
- [Java SE Documentation](https://docs.oracle.com/javase/11/)
- [FlatLaf Documentation](https://www.formdev.com/flatlaf/)
- [OkHttp Documentation](https://square.github.io/okhttp/)

---

## 🤝 Contributing to Architecture

When proposing architectural changes:

1. **Discuss first** - Open an issue for major changes
2. **Document** - Update this file
3. **Test** - Ensure no regressions
4. **Review** - Get maintainer approval

---

**Stellar Server Forge** - *Well-architected for maintainability and extensibility*  
**ZeroG Network** | **Version 1.0.0** | **May 11, 2026**

