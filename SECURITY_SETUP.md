# 🚀 Stellar Server Forge - Security & Setup Guide

## 🔐 Security Implementation

### API Key Management
- **Secure Storage**: API keys are stored in external configuration files (`config/api-keys.properties`)
- **Git Safety**: All sensitive configuration files are automatically excluded from version control
- **Runtime Validation**: The application validates API key configuration at startup
- **Error Handling**: Clear error messages guide users to configure API keys properly

### Configuration Files
```
config/
├── api-keys.properties          # 🔒 SENSITIVE - Your API keys (never commit!)
└── stellar-forge.properties     # ✅ Safe - Application settings
```

## 🛠️ Setup Instructions

### 1. Initial Setup
```bash
# Clone the repository
git clone <repository-url>
cd stellar-server-forge

# Build the application
mvn clean compile
```

### 2. First Run (Generates Configuration)
```bash
# Run to generate configuration template
mvn exec:java -Dexec.mainClass="com.zerog.network.stellarforge.Main"
```

### 3. Configure API Keys
1. **Open the generated file**: `config/api-keys.properties`
2. **Get your CurseForge API key**: 
   - Visit [CurseForge Console](https://console.curseforge.com/)
   - Create account or log in
   - Generate API key
3. **Update the configuration**:
   ```properties
   curseforge.api.key=YOUR_ACTUAL_API_KEY_HERE
   modrinth.api.key=optional_modrinth_key
   ```

### 4. Run the Application
```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.zerog.network.stellarforge.Main"

# Or build and run JAR
mvn clean package
java -jar target/stellar-server-forge-1.0.0.jar

# Or use the convenience scripts
build.bat
run.bat
```

## 🔒 Security Best Practices

### What's Protected
✅ **API Keys**: Stored in external files, never in source code  
✅ **Configuration**: Separated from application code  
✅ **Version Control**: Sensitive files automatically excluded  
✅ **Validation**: Runtime checks for missing configuration  

### What You Should Do
1. **Never commit** `config/api-keys.properties` to version control
2. **Keep API keys private** - don't share them
3. **Use environment variables** in production deployments
4. **Regularly rotate** API keys for security

### What's Already Done
- `.gitignore` configured to exclude sensitive files
- Configuration validation at startup
- Clear error messages for missing configuration
- Secure fallback behavior when APIs are unavailable

## 📁 Project Structure

```
stellar-server-forge/
├── src/main/java/com/zerog/network/stellarforge/
│   ├── Main.java                           # Application entry point
│   ├── config/
│   │   └── SecureConfig.java              # 🔐 Secure configuration manager
│   ├── api/
│   │   ├── CurseForgeClient.java          # CurseForge API integration
│   │   └── ModrinthClient.java            # Modrinth API integration
│   ├── gui/
│   │   └── MainWindow.java                # Main application window
│   ├── model/
│   │   ├── ServerConfig.java              # Server configuration model
│   │   └── ModInfo.java                   # Mod information model
│   └── utils/
│       ├── FileUtil.java                  # File operations
│       └── ServerManager.java             # Server management
├── config/                                # 🔒 Configuration directory
│   ├── api-keys.properties               # SENSITIVE - Never commit!
│   └── stellar-forge.properties          # Application settings
├── pom.xml                                # Maven configuration
├── build.bat                              # Build script
├── run.bat                                # Run script
├── .gitignore                             # Git exclusions (includes security)
└── README.md                              # Project documentation
```

## 🚨 Important Notes

1. **API Key Required**: CurseForge API key is required for mod installation features
2. **Configuration Template**: The application creates a template on first run
3. **Security First**: All sensitive data is externalized from the codebase
4. **ZeroG Network**: Customized for space-themed gaming community

## 🎯 Ready to Use

The application is now ready with:
- ✅ **Secure API key management**
- ✅ **Space-themed branding** for ZeroG Network
- ✅ **Git-safe configuration** 
- ✅ **Professional error handling**
- ✅ **Modern Java architecture**

Just configure your CurseForge API key and you're ready to create Minecraft servers!
