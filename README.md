# Stellar Server Forge - ZeroG Network

A modern Java GUI application for creating and managing Minecraft modded servers with integrated mod installation from CurseForge and Modrinth. Built for the ZeroG Network space-themed gaming community.

## 🚀 Features

- **Server Configuration**: Easy setup of Minecraft version, mod loader (Forge, Fabric, Quilt, NeoForge), RAM allocation, and more
- **Mod Installation**: Browse and install mods from CurseForge and Modrinth APIs
- **Modpack Import**: Import complete modpacks from ZIP files with automatic mod detection and installation
- **Suggested Mods**: Get popular mod recommendations based on your server configuration
- **Server Management**: Start, stop, and monitor your server directly from the GUI
- **Modern UI**: Clean, dark theme interface using FlatLaf
- **Configuration Persistence**: Automatically saves and loads your server settings
- **Embedded Security**: API keys are now securely embedded with AES encryption

## 📦 Modpack Import Feature

The application now supports importing modpacks from ZIP files:

- **Automatic Detection**: Detects CurseForge `manifest.json` files and HTML mod lists
- **User Configuration**: Interactive dialogs for Minecraft version and mod loader selection
- **Server-Side Filtering**: Automatically filters out client-only mods
- **Progress Tracking**: Real-time progress updates during import and installation
- **Fallback Support**: Handles various modpack formats automatically

### Using Modpack Import

1. Click "Import Modpack" in the main menu
2. Select a ZIP file containing your modpack
3. Configure import settings (Minecraft version, mod loader, filtering options)
4. Click "Import" to begin automatic installation

## 🔐 Security Features

- **Embedded API Keys**: API keys are now securely embedded with AES encryption
- **KeyVault System**: Secure storage and retrieval of sensitive data
- **No External Configuration**: No need to configure API keys manually
- **Runtime Validation**: Validates API keys at startup with clear error messages

## 🛠️ Setup and Configuration

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher
- CurseForge API key (required for CurseForge mod installation)

### Initial Setup

1. **Clone/Download the project**
2. **Navigate to the project directory**
3. **Build the project**:
   ```bash
   mvn clean compile
   ```

### API Key Configuration

**IMPORTANT**: Before running the application, you must configure your API keys.

1. **Run the application once** to generate the configuration template:
   ```bash
   mvn exec:java -Dexec.mainClass="com.zerog.network.stellarforge.Main"
   ```

2. **Configure your API keys**:
   - Open `config/api-keys.properties`
   - Replace `YOUR_CURSEFORGE_API_KEY_HERE` with your actual CurseForge API key
   - Optionally set your Modrinth API key (not required for basic operations)

3. **Get your CurseForge API key**:
   - Go to [CurseForge Console](https://console.curseforge.com/)
   - Create an account or log in
   - Generate an API key for your application

### Configuration Files

The application creates several configuration files:

- `config/api-keys.properties` - **SENSITIVE**: Contains your API keys (never commit this!)
- `config/stellar-forge.properties` - Application settings
- `server-config.json` - Server configuration
- `.gitignore` - Automatically excludes sensitive files from version control

## Supported Mod Loaders

- **Forge**: The most popular mod loader
- **Fabric**: Lightweight and fast mod loader
- **Quilt**: Fork of Fabric with additional features
- **NeoForge**: Modern fork of Forge

## Supported Platforms

- **CurseForge**: Largest mod repository
- **Modrinth**: Modern, fast mod platform

## System Requirements

- Java 11 or higher
- Windows, Linux, or macOS
- Internet connection for mod downloads

## Building and Running

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher

### Build Instructions

1. Clone or download the project
2. Navigate to the project directory
3. Run Maven to build the project:

```bash
mvn clean compile
```

### Running the Application

To run the application:

```bash
mvn exec:java -Dexec.mainClass="com.zerog.network.stellarforge.Main"
```

Or build a JAR file:

```bash
mvn clean package
java -jar target/stellar-server-forge-1.0.0.jar
```

### Using the Build Scripts

For easier building and running:

```bash
# Build the project
build.bat

# Run the application
run.bat
```

## Usage

### Getting Started

1. **Launch the Application**: Run the JAR file or use Maven
2. **Configure Server**: 
   - Enter server name and select installation directory
   - Choose Minecraft version and mod loader
   - Set RAM allocation and port
   - Configure Java path if needed
3. **Install Mods** (Optional):
   - Click "Install Mods" to browse and install mods
   - Use the search function or browse suggested mods
   - Install individual mods or all suggested mods
4. **Launch Server**: Click "Launch Server" to start your server

### Server Configuration

The application provides three main tabs:

#### Server Configuration
- **Server Name**: Display name for your server
- **Server Path**: Directory where server files will be stored
- **Minecraft Version**: Target Minecraft version
- **Mod Loader**: Choose between Forge, Fabric, Quilt, or NeoForge
- **Max RAM**: Memory allocation for the server
- **Server Port**: Network port for the server
- **Java Path**: Custom Java installation path (optional)
- **JVM Arguments**: Advanced JVM startup parameters

#### Mod Management
- **Search Mods**: Search for mods on CurseForge and Modrinth
- **Suggested Mods**: View popular mods for your configuration
- **Install Mods**: Download and install mods automatically
- **Manage Installed**: View and remove installed mods

#### Server Control
- **Launch/Stop**: Start and stop your server
- **Server Log**: View real-time server output
- **Auto-restart**: Configure automatic server restart on crash

### Mod Installation

The mod installer provides two ways to find mods:

1. **Search**: Enter keywords to search across CurseForge and Modrinth
2. **Suggested**: View curated lists of popular mods for your server type

Features:
- Automatic compatibility filtering based on Minecraft version and mod loader
- Detailed mod information including descriptions and file sizes
- Batch installation of multiple mods
- Automatic dependency resolution (planned feature)

## Configuration Files

The application creates several configuration files:

- `server-config.json`: Main server configuration
- `server.properties`: Minecraft server properties
- `eula.txt`: Minecraft EULA acceptance
- `start.bat`: Server startup script

## Troubleshooting

### Common Issues

1. **Java Not Found**: Ensure Java is installed and in your PATH, or specify custom Java path
2. **Port Already in Use**: Change the server port in configuration
3. **Insufficient RAM**: Increase RAM allocation or ensure system has enough memory
4. **Mod Compatibility**: Check mod descriptions for compatibility information

### Logs

Application logs are available in the console output. Server logs are displayed in the Server Control tab.

## License

This project is licensed under the GNU General Public License v3.0 - see the original Universalator batch file for details.

## Contributing

This is a GUI reimplementation of the original Universalator batch script. Contributions are welcome for:

- Additional mod platform support
- UI improvements
- Bug fixes
- Performance optimizations

## Original Credits

Based on "The Universalator - Modded Minecraft Server Installation / Launching Program" by Kerry Sherwin.

## API Usage

This application uses the following APIs:
- CurseForge API for mod information and downloads
- Modrinth API for mod information and downloads
- Various mod loader APIs for version information

Please respect the terms of service of these platforms when using this application.
