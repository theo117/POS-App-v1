@echo off
setlocal

REM Build and package JavaPOS as a Windows installer (.exe)
REM Run this script from the project folder: POSApp-main\POSApp-main

set "APP_NAME=JavaPOS"
set "APP_VERSION=1.0.0"
set "VENDOR=Your Name or Company"
set "MAIN_CLASS=JavaPOS"
set "MAIN_JAR=JavaPOS.jar"
set "DIST_DIR=dist"
set "OUT_DIR=installer"

where ant >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Ant not found in PATH. Install Ant or run from NetBeans with Ant configured.
  exit /b 1
)

where jpackage >nul 2>&1
if errorlevel 1 (
  echo [ERROR] jpackage not found in PATH. Use JDK 14+ and ensure it is on PATH.
  exit /b 1
)

echo [1/2] Building JAR with Ant...
call ant clean jar
if errorlevel 1 (
  echo [ERROR] Ant build failed.
  exit /b 1
)

if not exist "%DIST_DIR%\%MAIN_JAR%" (
  echo [ERROR] Expected JAR not found: %DIST_DIR%\%MAIN_JAR%
  exit /b 1
)

echo [2/2] Creating EXE installer with jpackage...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --input "%DIST_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest "%OUT_DIR%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --win-shortcut ^
  --win-menu ^
  --win-per-user-install

if errorlevel 1 (
  echo [ERROR] jpackage failed.
  echo If you see a WiX-related error, install WiX Toolset and rerun.
  exit /b 1
)

echo [SUCCESS] Installer created in: %OUT_DIR%
endlocal
