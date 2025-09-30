@echo off
echo ========================================
echo Testing URL Parsing
echo ========================================
echo.

REM Test URL with all parameters including token
set "TEST_URL=gp360://enroll?id=25&type=App%%5CModels%%5CInmate%%5CInmate&token=1%%7CabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ&api=http%%3A%%2F%%2Flocalhost%%3A8000%%2Fapi"

echo Test URL: %TEST_URL%
echo.
echo Parsing with PowerShell:
echo.

powershell -ExecutionPolicy Bypass -File parse-url.ps1 "%TEST_URL%"

echo.
echo ========================================
pause