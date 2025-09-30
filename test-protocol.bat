@echo off
echo ========================================
echo Testing Protocol URL Parsing
echo ========================================
echo.

set "TEST_URL=gp360://enroll?id=22&type=inmate&token=1%%7CkbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084&api=http%%3A%%2F%%2F127.0.0.1%%3A8000%%2Fapi"
echo Test URL: %TEST_URL%
echo.

REM Create a temporary PowerShell script for parsing
set "PS_SCRIPT=%TEMP%\parse_gp360_url.ps1"

echo $url = '%TEST_URL%' > "%PS_SCRIPT%"
echo $url = $url -replace 'gp360://', '' >> "%PS_SCRIPT%"
echo $parts = $url -split '\?' >> "%PS_SCRIPT%"
echo if ($parts.Count -gt 1) { >> "%PS_SCRIPT%"
echo     $query = $parts[1] >> "%PS_SCRIPT%"
echo     $params = @{} >> "%PS_SCRIPT%"
echo     $query -split '&' ^| ForEach-Object { >> "%PS_SCRIPT%"
echo         $kv = $_ -split '=', 2 >> "%PS_SCRIPT%"
echo         if ($kv.Count -eq 2) { >> "%PS_SCRIPT%"
echo             $params[$kv[0]] = [System.Web.HttpUtility]::UrlDecode($kv[1]) >> "%PS_SCRIPT%"
echo         } >> "%PS_SCRIPT%"
echo     } >> "%PS_SCRIPT%"
echo     Write-Output "ID:$($params['id'])" >> "%PS_SCRIPT%"
echo     Write-Output "TYPE:$($params['type'])" >> "%PS_SCRIPT%"
echo     Write-Output "TOKEN:$($params['token'])" >> "%PS_SCRIPT%"
echo     Write-Output "API:$($params['api'])" >> "%PS_SCRIPT%"
echo } >> "%PS_SCRIPT%"

echo Parsing URL parameters...
echo.

for /f "tokens=1,* delims=:" %%a in ('powershell -ExecutionPolicy Bypass -File "%PS_SCRIPT%"') do (
    echo Found: %%a = %%b
    if "%%a"=="ID" set "ENROLLABLE_ID=%%b"
    if "%%a"=="TYPE" set "ENROLLABLE_TYPE=%%b"
    if "%%a"=="TOKEN" set "API_TOKEN=%%b"
    if "%%a"=="API" set "API_URL=%%b"
)

del "%PS_SCRIPT%" 2>nul

echo.
echo Parsed Values:
echo - ID: %ENROLLABLE_ID%
echo - Type: %ENROLLABLE_TYPE%
echo - Token: %API_TOKEN:~0,20%...
echo - API: %API_URL%
echo.

pause