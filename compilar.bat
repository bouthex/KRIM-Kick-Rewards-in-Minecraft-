@echo off
cd /d "%~dp0"
call gradlew.bat build
echo.
echo Si dice BUILD SUCCESSFUL, el mod esta en build\libs\kickcreeper-1.0.0.jar
pause
