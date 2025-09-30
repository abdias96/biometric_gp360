@echo off
REM Protocol handler for gp360:// URLs
REM Receives URL like: gp360://enroll?id=123&type=inmate&token=xxx&api=http://localhost:8000/api

echo ========================================
echo GP360 Biometric Client
echo ========================================
echo.

REM Enable delayed expansion for better variable handling
setlocal enabledelayedexpansion

REM Capture URL - %1 will have the entire URL when called from registry
set "FULL_URL=%~1"

REM Debug: Show what we received
echo Received URL: %FULL_URL%
echo.

REM Initialize variables with defaults
set "ENROLLABLE_ID="
set "ENROLLABLE_TYPE=App\Models\Inmate"
set "API_TOKEN="
set "SESSION_ID="
set "API_URL=http://localhost:8000/api"

REM Change to script directory
cd /d "%~dp0"

REM Parse URL using PowerShell helper script
echo Parsing URL parameters...
for /f "tokens=1,* delims==" %%a in ('powershell -ExecutionPolicy Bypass -File parse-url.ps1 "!FULL_URL!"') do (
    if "%%a"=="ID" set "ENROLLABLE_ID=%%b"
    if "%%a"=="TYPE" set "ENROLLABLE_TYPE=%%b"
    if "%%a"=="TOKEN" set "API_TOKEN=%%b"
    if "%%a"=="SESSION" set "SESSION_ID=%%b"
    if "%%a"=="API" set "API_URL=%%b"
)

echo.
echo Parsed Parameters:
echo - ID: %ENROLLABLE_ID%
echo - Type: %ENROLLABLE_TYPE%
if defined API_TOKEN (
    echo - Token: !API_TOKEN:~0,20!...
) else if defined SESSION_ID (
    echo - Session: %SESSION_ID%
) else (
    echo - Token: [NOT PROVIDED]
)
echo - API: %API_URL%
echo.

REM Check if we have required parameters
if "%ENROLLABLE_ID%"=="" (
    echo ERROR: No ID provided
    echo The biometric client requires an ID parameter.
    pause
    exit /b 1
)

REM Check if JAR exists
if not exist "dist\BiometricService.jar" (
    echo ERROR: BiometricService.jar not found in %~dp0dist\
    echo.
    echo Please ensure the BiometricService.jar is built and placed in:
    echo %~dp0dist\
    echo.
    pause
    exit /b 1
)

echo Starting BiometricService.jar...
echo.

REM Run the JAR with parameters
if defined API_TOKEN (
    REM Direct token provided
    java -cp "dist\BiometricService.jar;lib\*" ^
         -DenrollableId=%ENROLLABLE_ID% ^
         -DenrollableType="%ENROLLABLE_TYPE%" ^
         -Dapi.token="%API_TOKEN%" ^
         -Dapi.url="%API_URL%" ^
         com.gp360.biometric.BiometricApplication
) else if defined SESSION_ID (
    REM Session ID provided - BiometricService will exchange it for token
    echo Using session ID to retrieve token...
    java -cp "dist\BiometricService.jar;lib\*" ^
         -DenrollableId=%ENROLLABLE_ID% ^
         -DenrollableType="%ENROLLABLE_TYPE%" ^
         -Dapi.session="%SESSION_ID%" ^
         -Dapi.url="%API_URL%" ^
         com.gp360.biometric.BiometricApplication
) else (
    echo WARNING: No authentication credentials provided
    echo The biometric client will not be able to save data.
    echo.
    java -cp "dist\BiometricService.jar;lib\*" ^
         -DenrollableId=%ENROLLABLE_ID% ^
         -DenrollableType="%ENROLLABLE_TYPE%" ^
         -Dapi.url="%API_URL%" ^
         com.gp360.biometric.BiometricApplication
)

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Failed to start BiometricService
    echo Error code: %ERRORLEVEL%
    echo.
    echo Possible causes:
    echo - Java is not installed or not in PATH
    echo - JAR file is corrupted
    echo - Missing dependencies
    pause
)