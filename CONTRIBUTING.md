# Contributing to Stellar Server Forge

First off, thank you for considering contributing to Stellar Server Forge! 🎉

It's people like you that make Stellar Server Forge such a great tool for the Minecraft community.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)

---

## 📜 Code of Conduct

This project and everyone participating in it is governed by respect, professionalism, and collaboration. By participating, you are expected to uphold these values. Please report unacceptable behavior to the project maintainers.

---

## 🤝 How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**When reporting bugs, include:**
- **Clear title** - Descriptive summary
- **Steps to reproduce** - Detailed steps
- **Expected behavior** - What should happen
- **Actual behavior** - What actually happens
- **Environment** - OS, Java version, etc.
- **Logs** - Console output or error messages
- **Screenshots** - If applicable

### Suggesting Enhancements

Enhancement suggestions are welcome! Please provide:
- **Clear description** - What and why
- **Use cases** - When would this be useful
- **Alternatives** - Other solutions considered
- **Mockups** - If UI changes involved

### Your First Code Contribution

Unsure where to begin? Look for issues labeled:
- `good first issue` - Perfect for newcomers
- `help wanted` - Extra attention needed
- `documentation` - Documentation improvements

### Priority Contributions

**High Priority:**
1. **Automatic Java Installation** - Complete the Java auto-install feature
2. **Mod Dependency Analyzer** - Parse mod dependencies
3. **Server Backup System** - Automated backup functionality
4. **Test Coverage** - Add unit and integration tests

**Medium Priority:**
1. Multi-server profile management
2. Performance monitoring
3. Network tools (UPNP support)
4. UI/UX improvements

---

## 🛠️ Development Setup

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher
- Git
- IDE (IntelliJ IDEA recommended)

### Clone and Build

```bash
# Clone the repository
git clone [repository-url]
cd StellarServerForge

# Build the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="com.zerog.network.stellarforge.Main"

# Package as JAR
mvn clean package
```

### IDE Setup

**IntelliJ IDEA:**
1. Open project folder
2. Maven will auto-import dependencies
3. Set JDK to 11 or higher
4. Run `Main.java`

**Eclipse:**
1. Import as Maven project
2. Update project configuration
3. Set compiler compliance to Java 11
4. Run `Main.java`

---

## 📁 Project Structure

```
src/main/java/com/zerog/network/stellarforge/
├── Main.java                          # Application entry point
├── config/                            # Configuration management
│   ├── SecureConfig.java             # Configuration loader
│   └── KeyVault.java                 # Encrypted key storage
├── gui/                              # User interface
│   ├── MainWindow.java               # Main dashboard
│   ├── ServerConfigDialog.java       # Settings editor
│   ├── ServerLauncherDialog.java     # Server control
│   └── ModInstallerDialog.java       # Mod browser
├── api/                              # External API clients
│   ├── CurseForgeClient.java         # CurseForge integration
│   └── ModrinthClient.java           # Modrinth integration
├── installer/                        # Mod loader installers
│   ├── ForgeInstaller.java           # Forge automation
│   ├── FabricInstaller.java          # Fabric automation
│   ├── QuiltInstaller.java           # Quilt automation
│   └── NeoForgeInstaller.java        # NeoForge automation
├── manager/                          # Business logic
│   ├── JavaManager.java              # Java detection
│   ├── ServerManager.java            # Server operations
│   └── MojangManifestService.java    # Official downloads
└── model/                            # Data models
    ├── ServerConfig.java             # Configuration model
    └── ModInfo.java                  # Mod data model
```

---

## 💻 Coding Standards

### Java Style Guide

**Follow these conventions:**

```java
// Package names: lowercase
package com.zerog.network.stellarforge.utils;

// Class names: PascalCase
public class ServerManager {

    // Constants: UPPER_SNAKE_CASE
    private static final int DEFAULT_PORT = 25565;
    
    // Fields: camelCase with descriptive names
    private String serverPath;
    private int maxRamGb;
    
    // Methods: camelCase, verb-based
    public boolean startServer() {
        // Method implementation
    }
    
    // Private methods: camelCase with descriptive names
    private void validateConfiguration() {
        // Implementation
    }
}
```

**Code Quality:**
- ✅ Use meaningful variable names
- ✅ Write self-documenting code
- ✅ Add comments for complex logic
- ✅ Use try-with-resources for I/O
- ✅ Handle exceptions appropriately
- ✅ Log important operations
- ✅ Avoid code duplication

**Error Handling:**
```java
try {
    // Operation that might fail
    processFile(filePath);
} catch (IOException e) {
    logger.error("Failed to process file: {}", filePath, e);
    showErrorDialog("Unable to process file: " + e.getMessage());
    return false;
}
```

### Documentation

**Use JavaDoc for public APIs:**
```java
/**
 * Downloads and installs the Forge mod loader.
 * 
 * @param minecraftVersion The Minecraft version (e.g., "1.20.1")
 * @param serverPath The server installation directory
 * @return true if installation successful, false otherwise
 * @throws IOException if download or installation fails
 */
public boolean installForge(String minecraftVersion, Path serverPath) 
        throws IOException {
    // Implementation
}
```

---

## 📝 Commit Guidelines

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `style` - Code style changes (formatting)
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance tasks

**Examples:**
```
feat(installer): add NeoForge automatic installation

Implements automatic NeoForge installer with Maven API integration.
Includes version detection and progress tracking.

Closes #42
```

```
fix(gui): resolve server console scrolling issue

Fixed auto-scroll not working when server produces rapid output.
Now properly scrolls to bottom on each new line.

Fixes #38
```

---

## 🔄 Pull Request Process

### Before Submitting

1. **Create a branch** - Use descriptive names
   ```bash
   git checkout -b feature/java-auto-install
   git checkout -b fix/console-scroll-bug
   ```

2. **Follow coding standards** - Consistent with project style

3. **Test your changes** - Ensure everything works

4. **Update documentation** - If adding features

5. **Commit properly** - Follow commit guidelines

### Submitting

1. **Push your branch**
   ```bash
   git push origin feature/your-feature
   ```

2. **Create Pull Request**
   - Clear title and description
   - Reference related issues
   - Describe changes made
   - Include screenshots if UI changes

3. **PR Template:**
   ```markdown
   ## Description
   Brief description of changes
   
   ## Motivation
   Why is this change needed?
   
   ## Changes Made
   - Change 1
   - Change 2
   
   ## Testing
   How was this tested?
   
   ## Screenshots
   (If applicable)
   
   ## Related Issues
   Closes #123
   ```

### Review Process

- Maintainers will review your PR
- Address feedback promptly
- Be open to suggestions
- CI checks must pass
- At least one approval required

---

## 🧪 Testing

### Manual Testing

**Before submitting, test:**
1. Application starts without errors
2. Your feature works as intended
3. Existing features still work
4. No console errors or warnings
5. UI is responsive and clean

### Testing Checklist

- [ ] Build succeeds (`mvn clean compile`)
- [ ] Application runs (`mvn exec:java`)
- [ ] JAR packages correctly (`mvn package`)
- [ ] Feature works as documented
- [ ] No regressions in existing features
- [ ] Error handling works
- [ ] Logs are meaningful

### Future: Automated Tests

We're working on adding:
- Unit tests (JUnit)
- Integration tests
- UI tests
- CI/CD pipeline

Contributions to testing infrastructure are highly valued!

---

## 📚 Documentation

### When to Update Documentation

**Always update docs when:**
- Adding new features
- Changing existing behavior
- Adding configuration options
- Fixing important bugs
- Adding API methods

### Documentation Files

- **README.md** - User-facing feature overview
- **QUICKSTART.md** - Getting started guide
- **IMPLEMENTATION_STATUS.md** - Technical status
- **SESSION_X_COMPLETE.md** - Development logs
- **JavaDoc** - Code documentation

### Documentation Style

- Clear and concise
- Step-by-step instructions
- Include examples
- Use screenshots when helpful
- Proofread before submitting

---

## 🎯 Areas Needing Contribution

### High Priority

**1. Automatic Java Installation**
- Download JDK from official sources
- Install on Windows/Linux/macOS
- Verify installation
- Update configuration

**2. Mod Dependency Analyzer**
- Parse mod JAR files
- Extract dependency information
- Check compatibility
- Suggest required mods

**3. Server Backup System**
- Automated backups
- Backup scheduling
- Restore functionality
- Compression support

### Medium Priority

**4. Multi-Server Profiles**
- Manage multiple servers
- Switch between configurations
- Profile import/export

**5. Performance Monitoring**
- Server resource usage
- Player count tracking
- TPS monitoring
- Alert system

**6. Network Tools**
- UPNP port forwarding
- DNS checking
- Firewall detection
- Connectivity testing

### Documentation

**7. Video Tutorials**
- Getting started video
- Feature demonstrations
- Troubleshooting guides

**8. Translations**
- User interface localization
- Documentation translations

---

## 💡 Tips for Contributors

### Best Practices

1. **Start small** - Begin with small, focused changes
2. **Ask questions** - Don't hesitate to ask for clarification
3. **Be patient** - Reviews may take time
4. **Stay focused** - One feature per PR
5. **Test thoroughly** - Prevent regressions
6. **Document well** - Help others understand your code

### Getting Help

- Check existing documentation
- Review similar code in the project
- Ask in issue discussions
- Reach out to maintainers

### Recognition

Contributors will be:
- Listed in project credits
- Mentioned in release notes
- Acknowledged in documentation

---

## 📞 Contact

**Project:** Stellar Server Forge  
**Organization:** ZeroG Network  
**Version:** 1.0.0  
**License:** GNU GPL v3.0  

---

## 🙏 Thank You!

Your contributions make Stellar Server Forge better for everyone. Whether it's code, documentation, bug reports, or feature suggestions - every contribution matters!

**Happy coding!** 🚀

---

**Stellar Server Forge** - *Powered by community contributions*  
**ZeroG Network** | **May 11, 2026**

