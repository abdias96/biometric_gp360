@echo off
echo ========================================
echo GP360 Biometric Service Launcher
echo ========================================
echo.

REM Parse command line arguments
set ENROLLABLE_ID=%1
set ENROLLABLE_TYPE=%2
set API_TOKEN=%3
set API_URL=%4

REM Set default values if not provided
if "%ENROLLABLE_TYPE%"=="" set ENROLLABLE_TYPE=App\Models\Inmate
if "%API_URL%"=="" set API_URL=http://localhost:8000/api

REM Check if JAR exists
if exist "dist\BiometricService.jar" (
    set JAR_PATH=dist\BiometricService.jar
    set LIB_PATH=lib\*
) else if exist "target\biometric-service-1.0.0-jar-with-dependencies.jar" (
    set JAR_PATH=target\biometric-service-1.0.0-jar-with-dependencies.jar
    set LIB_PATH=
) else (
    echo ERROR: JAR file not found. Please build the project first.
    echo Run: build.bat
    pause
    exit /b 1
)

REM Copy DigitalPersona DLLs if needed
if exist "..\Enrollment1\dpfpdd.dll" (
    echo Copying DigitalPersona DLLs...
    xcopy /Y "..\Enrollment1\*.dll" "."
)

echo Starting Biometric Service...
echo.
echo Configuration:
echo - Enrollable ID: %ENROLLABLE_ID%
echo - Enrollable Type: %ENROLLABLE_TYPE%
echo - API URL: %API_URL%
echo.

REM Run the application with parameters
if "%LIB_PATH%"=="" (
    java -DenrollableId=%ENROLLABLE_ID% ^
         -DenrollableType="%ENROLLABLE_TYPE%" ^
         -Dapi.token="%API_TOKEN%" ^
         -Dapi.url="%API_URL%" ^
         -jar "%JAR_PATH%"
) else (
    java -cp "%JAR_PATH%;%LIB_PATH%" ^
         -DenrollableId=%ENROLLABLE_ID% ^
         -DenrollableType="%ENROLLABLE_TYPE%" ^
         -Dapi.token="%API_TOKEN%" ^
         -Dapi.url="%API_URL%" ^
         com.gp360.biometric.BiometricApplication
)

echo.
echo Application closed.
pause