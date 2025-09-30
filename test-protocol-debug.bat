@echo off
echo ========================================
echo Protocol Handler Debug Test
echo ========================================
echo.

REM Test with a simple URL first
echo TEST 1: Simple URL without token
echo --------------------------------
set "URL1=gp360://enroll?id=25&type=inmate&api=http://localhost:8000/api"
echo URL: %URL1%
powershell -ExecutionPolicy Bypass -File parse-url-debug.ps1 "%URL1%"
echo.
echo ========================================
echo.

REM Test with token
echo TEST 2: URL with token
echo --------------------------------
set "URL2=gp360://enroll?id=25&type=inmate&token=testtoken123&api=http://localhost:8000/api"
echo URL: %URL2%
powershell -ExecutionPolicy Bypass -File parse-url-debug.ps1 "%URL2%"
echo.
echo ========================================
echo.

REM Test with complex token (like Laravel Sanctum)
echo TEST 3: URL with complex token
echo --------------------------------
set "URL3=gp360://enroll?id=25&type=App\Models\Inmate\Inmate&token=1^|abcdefghijklmnopqrstuvwxyz&api=http://localhost:8000/api"
echo URL: %URL3%
powershell -ExecutionPolicy Bypass -File parse-url-debug.ps1 "%URL3%"
echo.
echo ========================================
echo.

pause