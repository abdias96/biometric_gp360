@echo off
echo Testing Base64 Protocol Handler
echo.

REM Create JSON with parameters
set "JSON={\"id\":23,\"type\":\"inmate\",\"token\":\"1|kbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084\",\"api\":\"http://127.0.0.1:8000/api\"}"

echo JSON to encode: %JSON%
echo.

REM Encode to Base64 using PowerShell
for /f "delims=" %%a in ('powershell -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('%JSON%'))"') do set "BASE64=%%a"

echo Base64 encoded: %BASE64%
echo.

REM Create URL with Base64 data
set "URL=gp360://enroll?data=%BASE64%"

echo Full URL: %URL%
echo.

echo Calling protocol handler...
powershell.exe -ExecutionPolicy Bypass -File protocol-handler.ps1 "%URL%"

pause