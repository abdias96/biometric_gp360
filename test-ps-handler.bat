@echo off
echo Testing PowerShell Protocol Handler
echo.

set "TEST_URL=gp360://enroll?id=23&type=inmate&token=1|kbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084&api=http://127.0.0.1:8000/api"

echo Test URL: %TEST_URL%
echo.

powershell.exe -ExecutionPolicy Bypass -File protocol-handler.ps1 "%TEST_URL%"

pause