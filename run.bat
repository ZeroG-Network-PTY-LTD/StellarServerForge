@echo off
echo ========================================
echo  Stellar Server Forge - ZeroG Network
echo  Version 1.0.0
echo ========================================
echo.
echo Starting application...
echo.
cd /d "%~dp0"
call mvn exec:java "-Dexec.mainClass=com.zerog.network.stellarforge.Main"
echo.
echo Application closed.
pause
