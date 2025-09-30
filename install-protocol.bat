@echo off
echo ========================================
echo GP360 Protocol Handler Installer
echo ========================================
echo.
echo This will register gp360:// protocol to launch BiometricService
echo.

REM Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: Administrator privileges required.
    echo Please run this script as Administrator.
    pause
    exit /b 1
)

set "CURRENT_DIR=%~dp0"
set "JAR_PATH=%CURRENT_DIR%dist\BiometricService.jar"

REM Create registry entries for gp360:// protocol
echo Creating registry entries...

REM Create protocol handler
reg add "HKCR\gp360" /ve /d "GP360 Biometric Protocol" /f
reg add "HKCR\gp360" /v "URL Protocol" /d "" /f
reg add "HKCR\gp360\DefaultIcon" /ve /d "%JAR_PATH%,0" /f
reg add "HKCR\gp360\shell" /ve /f
reg add "HKCR\gp360\shell\open" /ve /f
reg add "HKCR\gp360\shell\open\command" /ve /d "\"%CURRENT_DIR%protocol-handler.bat\" \"%%1\"" /f

echo.
echo Protocol handler registered successfully!
echo You can now use gp360:// URLs to launch the biometric service
echo.
pause