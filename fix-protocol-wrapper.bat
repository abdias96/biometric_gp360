@echo off
echo ========================================
echo Fixing GP360 Protocol Registration (Wrapper)
echo ========================================
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

echo Removing old registry entries...
reg delete "HKCR\gp360" /f 2>nul

echo.
echo Registering protocol handler...

REM Create main protocol key
reg add "HKCR\gp360" /ve /d "GP360 Biometric Protocol" /f
reg add "HKCR\gp360" /v "URL Protocol" /d "" /f

REM Set icon (optional)
reg add "HKCR\gp360\DefaultIcon" /ve /d "%CURRENT_DIR%dist\BiometricService.jar,0" /f

REM Create shell structure
reg add "HKCR\gp360\shell" /ve /d "" /f
reg add "HKCR\gp360\shell\open" /ve /d "" /f

REM Use the wrapper batch file
reg add "HKCR\gp360\shell\open\command" /ve /d "\"%CURRENT_DIR%protocol-wrapper.bat\" %%1" /f

echo.
echo Testing registry...
reg query "HKCR\gp360\shell\open\command"

echo.
echo ========================================
echo Protocol handler fixed!
echo ========================================
echo.
echo Admin: Please run this as Administrator to register the protocol.
echo Then test in browser with the biometric capture button.
echo.
pause