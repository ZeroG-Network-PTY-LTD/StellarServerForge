@echo off
title My First Server
echo ========================================
echo  My First Server
echo  Minecraft 1.21.1
echo  Mod Loader: NeoForge
echo ========================================
echo.
echo Starting server...
echo.

java -Xms4G -Xmx8G -XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -jar server.jar nogui

pause
