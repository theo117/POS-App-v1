@echo off
setlocal EnableExtensions

REM Build and package JavaPOS as a Windows EXE installer without Ant.

cd /d "%~dp0"

set "APP_NAME=JavaPOS"
set "APP_VERSION=1.0.0"
set "VENDOR=JavaPOS"
set "MAIN_CLASS=ModernPosLauncherMain"
set "MAIN_JAR=JavaPOS.jar"
set "DIST_DIR=dist"
set "BUILD_DIR=build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "OUT_DIR=installer"
set "SQLITE_JAR=lib\sqlite-jdbc-3.49.1.0.jar"

where jpackage >nul 2>&1
if errorlevel 1 (
  echo [ERROR] jpackage not found in PATH. Use JDK 14+ and ensure it is on PATH.
  pause
  exit /b 1
)

where javac >nul 2>&1
if errorlevel 1 (
  echo [ERROR] javac not found in PATH.
  pause
  exit /b 1
)

where jar >nul 2>&1
if errorlevel 1 (
  echo [ERROR] jar not found in PATH.
  pause
  exit /b 1
)

if not exist "%SQLITE_JAR%" (
  echo [ERROR] Missing dependency: %SQLITE_JAR%
  pause
  exit /b 1
)

echo [1/4] Cleaning build folders...
if exist "%CLASSES_DIR%" rmdir /s /q "%CLASSES_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%CLASSES_DIR%"
mkdir "%DIST_DIR%"

echo [2/4] Compiling sources...
javac -d "%CLASSES_DIR%" src\*.java
if errorlevel 1 (
  echo [ERROR] javac compilation failed.
  pause
  exit /b 1
)

echo [3/4] Copying UI assets...
xcopy /E /I /Y modern-ui "%CLASSES_DIR%\modern-ui" >nul
if errorlevel 1 (
  echo [ERROR] Failed to copy modern-ui assets.
  pause
  exit /b 1
)

echo Bundling SQLite driver classes...
pushd "%CLASSES_DIR%"
jar xf "..\..\%SQLITE_JAR%"
if errorlevel 1 (
  popd
  echo [ERROR] Failed to unpack SQLite dependency.
  pause
  exit /b 1
)
popd

(
  echo Manifest-Version: 1.0
  echo Main-Class: %MAIN_CLASS%
  echo.
)> "%BUILD_DIR%\installer-manifest.mf"

if errorlevel 1 (
  echo [ERROR] Failed to prepare manifest.
  pause
  exit /b 1
)

echo [4/4] Building fat JAR...
jar --create --file "%DIST_DIR%\%MAIN_JAR%" --manifest "%BUILD_DIR%\installer-manifest.mf" -C "%CLASSES_DIR%" .
if errorlevel 1 (
  echo [ERROR] Failed to create application JAR.
  pause
  exit /b 1
)

echo Creating EXE installer...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
if exist "%OUT_DIR%\%APP_NAME%" rmdir /s /q "%OUT_DIR%\%APP_NAME%"

jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --input "%DIST_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --dest "%OUT_DIR%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --win-menu ^
  --win-shortcut ^
  --win-per-user-install

if errorlevel 1 (
  echo [ERROR] jpackage failed.
  echo If the error mentions WiX, use the EXE build or install WiX Toolset.
  pause
  exit /b 1
)

echo.
echo [SUCCESS] EXE installer created in: %OUT_DIR%
pause
endlocal
