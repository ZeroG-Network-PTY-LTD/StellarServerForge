# Configuration Access Guide

## How to Access Configuration Options

There are **two ways** to access the server configuration in Stellar Server Forge:

### Method 1: Main Window Button
1. Launch the application
2. Click the **"⚙️ Configure Server"** button in the center of the window

### Method 2: Menu Bar (**NEW!**)
1. Launch the application
2. Go to **File → Configure Server** in the menu bar
3. Or use keyboard shortcut: **Ctrl+C**

## What Configuration Options Are Available

The configuration dialog includes:

### Basic Settings
- **Server Name**: Display name for your server
- **Server Path**: Directory where server files will be stored

### Game Version
- **Minecraft Version**: Select from popular versions or type custom version
- **Mod Loader**: Choose from Forge, Fabric, Quilt, NeoForge, or Vanilla

### Performance
- **Max RAM (GB)**: Memory allocation (1-32 GB)
- **Server Port**: Network port (default: 25565)

### Java Configuration
- **Java Installation**: Auto-detect or select specific Java version
- **Custom Java Path**: Manually specify Java location

### Advanced Options
- **JVM Arguments**: Custom Java Virtual Machine arguments
- **Auto-restart on crash**: Automatically restart if server crashes
- **Enable UPnP**: Automatic port forwarding (experimental)

## Troubleshooting

### If Configuration Dialog Won't Open:

**1. Check Console Output**
- The application prints debug messages when opening the dialog
- Look for error messages in the console

**2. Try the Menu Bar**
- If the button doesn't work, try: **File → Configure Server**
- Or keyboard shortcut: **Ctrl+C**

**3. Check Java Version**
- Ensure you're running Java 11 or higher
- Run: `java -version` in command prompt

**4. View Detailed Error**
- If an error occurs, a dialog will show the full stack trace
- Copy the error message for troubleshooting

**5. Check Dependencies**
- Ensure the JAR file is complete (~6 MB)
- Try rebuilding: `mvn clean package`

### Common Issues:

**"Nothing happens when I click Configure Server"**
- Try the menu bar instead
- Check if an error dialog appeared (might be behind other windows)
- Look at console output for error messages

**"Dialog appears but is empty/broken"**
- This might be a display issue
- Try resizing the dialog window
- Check if Swing is properly initialized

**"Can't find the configuration button"**
- It's in the center of the main window
- Icon: ⚙️
- Text: "Configure Server"
- Alternative: File menu → Configure Server

## Getting Help

If configuration still doesn't work:

1. **Run with verbose output:**
   ```bash
   java -jar stellar-server-forge-1.0.0.jar > output.log 2>&1
   ```

2. **Check the output.log file** for error messages

3. **Copy any error messages** and report them

4. **Include:**
   - Operating System
   - Java version (`java -version`)
   - Steps you tried
   - Any error messages

## Manual Configuration

If the GUI isn't working, you can manually edit the configuration:

**File:** `config/server-config.json` (created after first setup attempt)

Example configuration:
```json
{
  "serverName": "My Server",
  "serverPath": "server",
  "minecraftVersion": "1.20.1",
  "modLoader": "FORGE",
  "maxRamGb": 4,
  "port": 25565
}
```

After editing, restart the application.

---

**Stellar Server Forge v1.0.0**  
**ZeroG Network**  
**For more help, see TROUBLESHOOTING.md**

