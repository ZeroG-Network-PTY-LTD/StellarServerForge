@echo off
echo ========================================
echo  Stellar Server Forge - Build Script
echo  ZeroG Network
echo ========================================
echo.
echo Building project with Maven...
echo.
cd /d "%~dp0"
call mvn clean compile
echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo  BUILD SUCCESSFUL
    echo ========================================
    echo.
    echo The project has been compiled successfully.
    echo To run the application, use run.bat
    echo To create a JAR file, use: mvn package
) else (
    echo ========================================
    echo  BUILD FAILED
    echo ========================================
    echo.
    echo Please check the error messages above.
)
echo.
pause

