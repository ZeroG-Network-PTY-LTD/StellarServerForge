@echo off
echo Starting Stellar Server Forge...
echo.
cd /d "%~dp0"
mvn exec:java
pause
