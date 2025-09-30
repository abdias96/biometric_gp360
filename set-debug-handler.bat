@echo off
echo Setting debug handler for gp360:// protocol
echo.
echo This requires Administrator privileges.
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
reg add "HKCR\gp360\shell\open\command" /ve /d "\"%CURRENT_DIR%debug-protocol.bat\" \"%%1\"" /f

echo.
echo Debug handler set. Try clicking the biometric button in the browser.
echo Check protocol-debug.log for captured arguments.
pause