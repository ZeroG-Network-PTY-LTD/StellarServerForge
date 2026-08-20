# Configuration Options - Now Available! ✅

## What Was Fixed

The configuration options are now **fully accessible** with **multiple access methods** and **enhanced error handling**.

---

## 🆕 NEW FEATURES ADDED

### 1. Menu Bar Access (**NEW!**)

Added a complete menu bar to the application with easy access to all features:

**File Menu:**
- ⚙️ Configure Server (Ctrl+C)
- Exit (Alt+F4)

**Tools Menu:**
- 📦 Install Mods
- 🚀 Launch Server

**Help Menu:**
- ℹ️ About

### 2. Enhanced Error Handling

- **Debug Output**: Console messages show exactly what's happening
- **Full Stack Trace**: If an error occurs, you see the complete error details
- **Better Error Messages**: Clear, actionable error information

### 3. Keyboard Shortcuts

- **Ctrl+C** - Open configuration dialog
- **Alt+F4** - Exit application

---

## 📋 HOW TO ACCESS CONFIGURATION

### Method 1: Button (Original)
1. Launch Stellar Server Forge
2. Click the **"⚙️ Configure Server"** button in the center window

### Method 2: Menu Bar (NEW!)
1. Launch Stellar Server Forge
2. Click **File → Configure Server** in the menu bar
3. Or press **Ctrl+C** on your keyboard

---

## ⚙️ AVAILABLE CONFIGURATION OPTIONS

The Server Configuration Dialog includes all these settings:

### 📁 Basic Settings
- **Server Name**: Display name for your server (e.g., "ZeroG Server")
- **Server Path**: Where server files will be stored (default: "server")
  - Includes "Browse..." button for easy folder selection

### 🎮 Game Version
- **Minecraft Version**: Drop-down with popular versions
  - 1.20.6, 1.20.4, 1.20.2, 1.20.1
  - 1.19.4, 1.19.3, 1.19.2
  - 1.18.2, 1.18.1
  - 1.17.1
  - 1.16.5
  - 1.12.2
  - Or type a custom version
  
- **Mod Loader**: Choose your mod loader
  - Forge (most popular, ~65%)
  - Fabric (modern, ~30%)
  - Quilt (Fabric fork, ~3%)
  - NeoForge (modern Forge, ~2%)
  - Vanilla (no mods)

### ⚡ Performance
- **Max RAM (GB)**: Memory allocation slider (1-32 GB)
  - Default: 4 GB
  - Minimum RAM will be half of maximum
  
- **Server Port**: Network port number (1-65535)
  - Default: 25565 (Minecraft standard)

### ☕ Java Configuration
- **Java Installation**: Auto-detected Java versions
  - "Detect Automatically" (recommended)
  - "Use System Java"
  - Specific detected installations (e.g., "Java 17 - C:\Program Files\Java...")
  - "Custom Path..." (manual selection)
  
- **Custom Java Path**: Manual Java location
  - Only enabled when "Custom Path..." is selected
  - Includes "Browse..." button

### 🔧 Advanced Options
- **JVM Arguments**: Custom Java Virtual Machine arguments
  - Text area for typing custom args
  - "Restore Default" button for optimal settings
  
- **Auto-restart on crash**: Checkbox to enable automatic server restart
- **Enable UPnP**: Automatic port forwarding (experimental, coming soon)

### 💾 Dialog Buttons
- **💾 Save Configuration**: Saves all settings
- **Cancel**: Closes without saving
- **↺ Reset to Defaults**: Restores all default values

---

## 🔍 TROUBLESHOOTING

### If You Still Can't See Configuration Options:

**1. Check if Application Launched:**
```bash
# Run from command line to see output
java -jar stellar-server-forge-1.0.0.jar
```

**2. Try All Access Methods:**
- Click the button in the center window
- Use File → Configure Server menu
- Press Ctrl+C keyboard shortcut

**3. Look for Error Messages:**
- Check the console/terminal window
- Look for error dialogs (might be behind main window)
- Error dialogs now show full details

**4. Verify JAR File:**
```bash
# Check file size (should be ~6 MB)
dir stellar-server-forge-1.0.0.jar

# Expected output shows file around 6,000,000 bytes
```

**5. Check Java Version:**
```bash
# Must be Java 11 or higher
java -version
```

### Common Issues Resolved:

✅ **"Button does nothing"** → Use menu bar (File → Configure Server)
✅ **"Can't find configuration"** → Look in File menu or press Ctrl+C
✅ **"Dialog won't open"** → Check console for error messages
✅ **"Options seem limited"** → See full list above - all options are there!

---

## 📝 CONFIGURATION FILE

After first configuration, settings are saved to:
```
config/server-config.json
```

You can also manually edit this file if needed (advanced users).

---

## ✅ VERIFICATION

To verify configuration is working:

1. **Launch application**: `java -jar stellar-server-forge-1.0.0.jar`
2. **Open configuration**: File → Configure Server (or Ctrl+C)
3. **Dialog should appear** with all sections visible:
   - Basic Settings
   - Game Version
   - Performance
   - Java Configuration
   - Advanced Options
4. **Buttons at bottom**: Save Configuration, Cancel, Reset to Defaults

If you see all of these, **configuration is working perfectly!** ✅

---

## 🎯 WHAT'S INCLUDED

**Updated Files:**
- `MainWindow.java` - Added menu bar and enhanced error handling
- `stellar-server-forge-1.0.0.jar` - Rebuilt with fixes

**New Files:**
- `CONFIGURATION_HELP.md` - This help document

**All Features Working:**
- ✅ Configuration dialog with all options
- ✅ Multiple access methods (button, menu, keyboard)
- ✅ Enhanced error reporting
- ✅ Full debugging output
- ✅ All 12+ configuration options available

---

## 📞 STILL NEED HELP?

If configuration still doesn't work:

1. Run with output capture:
   ```bash
   java -jar stellar-server-forge-1.0.0.jar > debug.log 2>&1
   ```

2. Check `debug.log` for error messages

3. Copy any errors and report them with:
   - Your operating system
   - Java version (`java -version`)
   - What you tried
   - Error messages from debug.log

---

## 🎉 READY TO USE!

Your Stellar Server Forge now has:
- ✅ **Full configuration access**
- ✅ **Multiple ways to open it**
- ✅ **All settings available**
- ✅ **Better error handling**
- ✅ **Help documentation**

**Start configuring your server now!** 🚀

---

**Stellar Server Forge v1.0.0**  
**ZeroG Network**  
**May 11, 2026**  

*Configuration options are now fully available and enhanced!*

