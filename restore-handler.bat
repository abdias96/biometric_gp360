@echo off
echo Restoring normal handler for gp360:// protocol
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
reg add "HKCR\gp360\shell\open\command" /ve /d "\"%CURRENT_DIR%protocol-handler.bat\" \"%%1\"" /f

echo.
echo Normal handler restored.
pause