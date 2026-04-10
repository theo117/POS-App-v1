@echo off
setlocal

cd /d "%~dp0"

set "LOG_FILE=%CD%\modern-ui-server.log"

if exist "%LOG_FILE%" del "%LOG_FILE%"
if not exist build\classes mkdir build\classes

echo Compiling modern UI backend...
javac -d build\classes src\*.java
if errorlevel 1 (
    echo.
    echo Compile failed.
    pause
    goto :end
)

echo Starting JavaPOS web launcher...
java --enable-native-access=ALL-UNNAMED -cp "build\classes;lib\sqlite-jdbc-3.49.1.0.jar" ModernPosLauncherMain > "%LOG_FILE%" 2>&1

:end
endlocal
