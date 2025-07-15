@echo off
echo Running Stellar Server Forge - ZeroG Network...
echo.

REM Check if JAR file exists
if not exist "target\stellar-server-forge-1.0.0.jar" (
    echo JAR file not found. Building project first...
    call build.bat
    if %errorlevel% neq 0 (
        echo Build failed. Cannot run application.
        pause
        exit /b 1
    )
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JRE 11 or higher
    pause
    exit /b 1
)

REM Run the application
echo Starting Stellar Server Forge...
java -jar target\stellar-server-forge-1.0.0.jar

if %errorlevel% neq 0 (
    echo Application exited with error code %errorlevel%
    pause
)
