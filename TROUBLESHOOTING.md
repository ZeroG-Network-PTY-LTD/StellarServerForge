# Troubleshooting Guide

**Stellar Server Forge v1.0.0**

This guide helps you resolve common issues when using Stellar Server Forge.

---

## 📋 Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [Installation Issues](#installation-issues)
- [Configuration Problems](#configuration-problems)
- [Mod Loader Issues](#mod-loader-issues)
- [Mod Installation Problems](#mod-installation-problems)
- [Server Launch Issues](#server-launch-issues)
- [Java-Related Problems](#java-related-problems)
- [Network and API Issues](#network-and-api-issues)
- [Performance Problems](#performance-problems)
- [Getting Further Help](#getting-further-help)

---

## 🔍 Quick Diagnostics

### Before You Start

**Check these first:**
1. ✅ Java 11+ is installed: `java -version`
2. ✅ Internet connection is working
3. ✅ Sufficient disk space (at least 2 GB free)
4. ✅ No antivirus blocking the application
5. ✅ API keys are configured (if using mod features)

### Common Quick Fixes

**Application won't start:**
```bash
# Try running with verbose output
java -jar stellar-server-forge-1.0.0.jar --debug
```

**UI looks broken:**
```bash
# Reset FlatLaf theme
java -Dflatlaf.theme=light -jar stellar-server-forge-1.0.0.jar
```

---

## 🚀 Installation Issues

### Problem: "Java not found" error

**Symptoms:**
- Error: "java is not recognized as an internal or external command"
- Application won't start

**Solutions:**

**Windows:**
```powershell
# Check if Java is installed
java -version

# If not installed, download from:
# https://adoptium.net/ (Recommended)
# Or: https://www.oracle.com/java/technologies/downloads/
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk

# Fedora
sudo dnf install java-11-openjdk-devel

# Arch
sudo pacman -S jdk11-openjdk
```

**macOS:**
```bash
# Using Homebrew
brew install openjdk@11

# Or download from Adoptium
```

### Problem: "Unable to extract JAR"

**Symptoms:**
- Error when running JAR file
- Corrupted file messages

**Solutions:**
1. **Re-download the JAR** - May be corrupted
2. **Check file size** - Should be ~6 MB
3. **Verify download** - Use a different browser
4. **Try different location** - Some folders have restrictions

---

## ⚙️ Configuration Problems

### Problem: "API key not configured" warning

**Symptoms:**
- Warning dialog on startup
- Mod features don't work
- CurseForge search fails

**Solutions:**

1. **Get a CurseForge API key:**
   - Go to https://console.curseforge.com/
   - Create account or log in
   - Navigate to "API Keys"
   - Click "Generate API Key"
   - Copy the key

2. **Configure the key:**
   ```
   Open: config/api-keys.properties
   Replace: YOUR_CURSEFORGE_API_KEY_HERE
   With: your-actual-key-here
   Save and restart application
   ```

3. **Verify configuration:**
   ```properties
   # Should look like this:
   curseforge.api.key=$2a$10$abcd...xyz
   curseforge.enabled=true
   ```

### Problem: "Failed to decrypt API key"

**Symptoms:**
- Error message about encryption
- Can't access mod features

**Solutions:**
1. **Clear and reconfigure:**
   ```bash
   # Delete the config file
   rm config/api-keys.properties
   
   # Restart application
   # It will create a new template
   
   # Add your API key again
   ```

2. **Check file permissions:**
   ```bash
   # Ensure file is readable
   chmod 644 config/api-keys.properties
   ```

### Problem: Settings don't save

**Symptoms:**
- Configuration resets after restart
- Changes don't persist

**Solutions:**
1. **Check file permissions:**
   - Application needs write access to config/ folder
   - Try running as administrator (Windows)

2. **Check disk space:**
   - Ensure sufficient space (at least 100 MB)

3. **Manually edit:**
   ```
   Edit: config/stellar-forge.properties
   Save your changes
   Restart application
   ```

---

## 🔧 Mod Loader Issues

### Problem: Forge installation fails

**Symptoms:**
- "Forge installation failed" error
- Installer hangs or crashes
- Libraries not downloading

**Solutions:**

1. **Check Minecraft version:**
   - Ensure Forge supports your MC version
   - Try a different MC version (e.g., 1.20.1)

2. **Manual verification:**
   ```bash
   # Check if server.jar exists
   ls server/
   
   # Look for forge files
   ls server/libraries/
   ```

3. **Clean and retry:**
   ```bash
   # Remove partial installation
   rm -rf server/libraries/
   rm server/forge-*.jar
   
   # Try installation again
   ```

4. **Check internet:**
   - Forge downloads many libraries
   - Ensure stable internet connection
   - Try again with better connection

### Problem: Fabric/Quilt installation fails

**Symptoms:**
- Installation error
- Server launcher not created

**Solutions:**

1. **Verify Minecraft version:**
   - Check fabric/quilt supports your version
   - Visit https://fabricmc.net/versions/

2. **Check downloads:**
   ```bash
   # Fabric server launcher should exist
   ls server/fabric-server-launch.jar
   # or for Quilt
   ls server/quilt-server-launch.jar
   ```

3. **Retry installation:**
   - Click "Install Mod Loader" again
   - Sometimes first attempt fails

### Problem: NeoForge installation fails

**Symptoms:**
- "MC version not supported" error
- Installation fails immediately

**Solutions:**

1. **Check MC version:**
   - NeoForge requires MC 1.20.1 or higher
   - For older versions, use Forge instead

2. **Version mapping:**
   ```
   MC 1.20.1 → NeoForge 20.1.x
   MC 1.20.2 → NeoForge 20.2.x
   MC 1.20.4 → NeoForge 20.4.x
   ```

3. **Try Forge instead:**
   - If NeoForge fails, use Forge
   - Forge supports more versions

---

## 📦 Mod Installation Problems

### Problem: "Mod not found" when searching

**Symptoms:**
- Search returns no results
- Can't find specific mod

**Solutions:**

1. **Check spelling:**
   - Try different keywords
   - Use mod's full name
   - Try partial names

2. **Check platform:**
   - Some mods only on CurseForge
   - Some only on Modrinth
   - Try both platforms

3. **Verify compatibility:**
   - Ensure mod supports your MC version
   - Check mod loader compatibility

### Problem: Mod download fails

**Symptoms:**
- Download starts but never completes
- "Download failed" error

**Solutions:**

1. **Check internet connection:**
   ```bash
   # Test connectivity
   ping curseforge.com
   ping modrinth.com
   ```

2. **Check disk space:**
   - Ensure enough space for mods
   - Mods can be large (10-100 MB each)

3. **Manual download:**
   ```bash
   # If auto-download fails:
   # 1. Download mod manually from website
   # 2. Place in server/mods/ folder
   ```

4. **Check API rate limits:**
   - Too many requests may be blocked
   - Wait a few minutes and retry

### Problem: Mod causes server crash

**Symptoms:**
- Server won't start after installing mod
- Crash reports mention specific mod

**Solutions:**

1. **Check mod compatibility:**
   - Ensure mod matches MC version
   - Ensure mod matches loader (Forge/Fabric)
   - Check for required dependencies

2. **Remove problematic mod:**
   ```bash
   # Remove from mods folder
   cd server/mods/
   rm problematic-mod.jar
   ```

3. **Check dependencies:**
   - Some mods require other mods
   - Read mod description carefully
   - Install required dependencies

4. **Check logs:**
   ```bash
   # View crash log
   cat server/logs/latest.log
   
   # Look for error messages
   grep -i "error" server/logs/latest.log
   ```

---

## 🎮 Server Launch Issues

### Problem: Server won't start

**Symptoms:**
- "Server failed to start" error
- Server process exits immediately
- No console output

**Solutions:**

1. **Check Java installation:**
   ```bash
   # Verify Java is working
   java -version
   
   # Check JDK (not just JRE)
   javac -version
   ```

2. **Check server files:**
   ```bash
   # Verify server.jar exists
   ls server/server.jar
   
   # Check EULA
   cat server/eula.txt
   # Should say "eula=true"
   ```

3. **Check RAM allocation:**
   - Ensure system has enough RAM
   - Try reducing RAM in settings
   - Minimum 2 GB recommended

4. **Manual start test:**
   ```bash
   cd server/
   java -Xmx4G -Xms1G -jar server.jar nogui
   # See actual error messages
   ```

### Problem: Port already in use

**Symptoms:**
- Error: "Address already in use"
- Server fails to bind to port

**Solutions:**

1. **Check what's using the port:**
   ```bash
   # Windows
   netstat -ano | findstr :25565
   
   # Linux/Mac
   lsof -i :25565
   ```

2. **Kill conflicting process:**
   ```bash
   # Windows (replace PID)
   taskkill /PID <process_id> /F
   
   # Linux/Mac
   kill -9 <process_id>
   ```

3. **Change server port:**
   - Open Server Config dialog
   - Change port from 25565 to something else
   - Save and restart

### Problem: Server runs but can't connect

**Symptoms:**
- Server appears to be running
- Players can't connect
- "Connection refused" errors

**Solutions:**

1. **Check server is actually running:**
   - Look at console output
   - Should say "Done! For help, type help"

2. **Check connection:**
   ```bash
   # Test local connection
   telnet localhost 25565
   
   # Or using nc
   nc -zv localhost 25565
   ```

3. **Check firewall:**
   ```bash
   # Windows: Add firewall rule
   netsh advfirewall firewall add rule name="Minecraft" dir=in action=allow protocol=TCP localport=25565
   
   # Linux: Open port
   sudo ufw allow 25565/tcp
   ```

4. **Connect locally first:**
   - Try connecting from same computer
   - Use `localhost` or `127.0.0.1`
   - If works, it's a network issue

---

## ☕ Java-Related Problems

### Problem: "Wrong Java version" error

**Symptoms:**
- Error about Java version
- Server won't start with installed Java

**Solutions:**

1. **Check version requirements:**
   ```
   MC 1.16.5 and earlier → Java 8+
   MC 1.17 → Java 16+
   MC 1.18-1.20.4 → Java 17+
   MC 1.20.5+ → Java 21+
   ```

2. **Install correct Java version:**
   - Download from https://adoptium.net/
   - Install appropriate version
   - Restart application

3. **Manually specify Java:**
   - Open Server Config dialog
   - Click "Detect Java Installations"
   - Select appropriate version
   - Save configuration

### Problem: Multiple Java versions conflicting

**Symptoms:**
- Wrong Java being used
- Version detection fails

**Solutions:**

1. **Check PATH:**
   ```bash
   # Windows
   echo %PATH%
   where java
   
   # Linux/Mac
   echo $PATH
   which java
   ```

2. **Set JAVA_HOME:**
   ```bash
   # Windows
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   
   # Linux/Mac (add to ~/.bashrc)
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
   ```

3. **Use custom Java path:**
   - In Server Config dialog
   - Manually enter Java path
   - Application will use that path

---

## 🌐 Network and API Issues

### Problem: "API request failed" errors

**Symptoms:**
- Can't search for mods
- API timeouts
- Connection errors

**Solutions:**

1. **Check internet connection:**
   ```bash
   # Test connectivity
   ping 8.8.8.8
   curl https://api.curseforge.com
   ```

2. **Check API status:**
   - Visit https://status.curseforge.com/
   - Check if APIs are operational

3. **Check proxy settings:**
   - If behind corporate proxy
   - Configure Java proxy settings:
   ```bash
   java -Dhttp.proxyHost=proxy.example.com \
        -Dhttp.proxyPort=8080 \
        -jar stellar-server-forge-1.0.0.jar
   ```

4. **Wait and retry:**
   - APIs may be temporarily down
   - Rate limits may have been hit
   - Try again in a few minutes

### Problem: Downloads are very slow

**Symptoms:**
- Downloads take forever
- Timeouts during installation

**Solutions:**

1. **Check bandwidth:**
   - Ensure no other downloads running
   - Close bandwidth-heavy applications

2. **Try different time:**
   - Peak times may be slower
   - Try during off-peak hours

3. **Check CDN access:**
   - Some CDNs may be blocked
   - Try different network if possible

---

## ⚡ Performance Problems

### Problem: Application is slow/laggy

**Symptoms:**
- UI freezes
- Slow response
- High CPU usage

**Solutions:**

1. **Increase heap size:**
   ```bash
   java -Xmx2G -jar stellar-server-forge-1.0.0.jar
   ```

2. **Close other applications:**
   - Free up system resources
   - Close unnecessary programs

3. **Check system requirements:**
   - Minimum: 1 GB RAM
   - Recommended: 2 GB RAM
   - Modern CPU

### Problem: Server uses too much RAM

**Symptoms:**
- System slowdown
- Out of memory errors

**Solutions:**

1. **Adjust RAM allocation:**
   - Open Server Config
   - Reduce max RAM setting
   - Save and restart server

2. **Add JVM arguments:**
   ```
   -XX:+UseG1GC
   -XX:+ParallelRefProcEnabled
   -XX:MaxGCPauseMillis=200
   ```

3. **Remove unused mods:**
   - Some mods are RAM-heavy
   - Keep only necessary mods

---

## 🆘 Getting Further Help

### Gather Information

Before asking for help, collect:

1. **System information:**
   ```bash
   java -version
   echo $OS
   ```

2. **Application logs:**
   - Check console output
   - Check logs/ directory

3. **Error messages:**
   - Copy full error text
   - Take screenshots

4. **Configuration:**
   - Your server configuration
   - Mod list if applicable

### Where to Get Help

1. **Check documentation:**
   - README.md
   - This troubleshooting guide
   - QUICKSTART.md

2. **Search existing issues:**
   - Check if someone had same problem
   - Look for solutions in closed issues

3. **Ask the community:**
   - Create new issue with details
   - Provide all gathered information
   - Be patient and respectful

### Creating a Good Bug Report

**Include:**
- **Title:** Clear, descriptive
- **Description:** What you were trying to do
- **Steps to reproduce:** Exact steps
- **Expected:** What should happen
- **Actual:** What actually happened
- **Environment:** OS, Java version, app version
- **Logs:** Error messages or logs
- **Screenshots:** If UI-related

**Example:**
```markdown
## Bug: Server fails to start after Forge installation

**Description:**
After clicking "Install Mod Loader" for Forge, the installation
completes successfully, but clicking "Start Server" fails immediately.

**Steps to Reproduce:**
1. Configure server with MC 1.20.1 and Forge
2. Run server setup
3. Install Forge (succeeds)
4. Click "Start Server" (fails)

**Expected:**
Server should start normally

**Actual:**
Error: "Failed to start server - server.jar not found"

**Environment:**
- OS: Windows 11
- Java: version 17.0.2
- App: Stellar Server Forge 1.0.0

**Logs:**
[Attach logs here]
```

---

## 📝 Common Error Messages

### "EULA not accepted"
**Solution:** Delete server directory and run setup again

### "Failed to verify checksum"
**Solution:** Delete server.jar and re-download

### "Java executable not found"
**Solution:** Install JDK and configure Java path

### "Permission denied"
**Solution:** Run with appropriate permissions or change directory

### "OutOfMemoryError"
**Solution:** Increase RAM allocation or reduce server load

### "ClassNotFoundException"
**Solution:** Rebuild JAR or re-download application

---

## 🔄 Reset Everything

**If all else fails:**

```bash
# Backup your world
cp -r server/world ~/world-backup

# Delete everything except your world
rm -rf server/
rm -rf config/
rm -rf logs/

# Restart application
# It will create fresh configuration

# Restore your world
cp -r ~/world-backup server/world
```

---

## ✅ Preventive Measures

**To avoid issues:**

1. ✅ Keep Java updated
2. ✅ Use stable Minecraft versions
3. ✅ Read mod descriptions carefully
4. ✅ Backup worlds regularly
5. ✅ Keep API keys secure
6. ✅ Monitor disk space
7. ✅ Use recommended settings
8. ✅ Update application when new versions release

---

**Still having issues?** Don't hesitate to reach out to the community or open an issue with detailed information!

---

**Stellar Server Forge** - *We're here to help!*  
**ZeroG Network** | **Version 1.0.0** | **May 11, 2026**

