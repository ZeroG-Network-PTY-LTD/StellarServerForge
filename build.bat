@echo off
echo Building Stellar Server Forge - ZeroG Network...
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 11 or higher
    pause
    exit /b 1
)

REM Clean and compile
echo Cleaning previous build...
call mvn clean

echo Compiling project...
call mvn compile

if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo Building JAR file...
call mvn package

if %errorlevel% neq 0 (
    echo ERROR: JAR creation failed
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo JAR file location: target\stellar-server-forge-1.0.0.jar
echo.
echo To run the application:
echo   java -jar target\stellar-server-forge-1.0.0.jar
echo.
echo IMPORTANT: Before running, configure your API keys in config\api-keys.properties
echo Get your CurseForge API key from: https://console.curseforge.com/
echo.
pause
