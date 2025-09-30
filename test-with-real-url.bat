@echo off
echo ========================================
echo Testing with Real Protocol URL
echo ========================================
echo.

REM This simulates what the browser would send
set "TEST_URL=gp360://enroll?id=23&type=inmate&token=1%%7CkbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084&api=http%%3A%%2F%%2F127.0.0.1%%3A8000%%2Fapi"

echo Testing URL: %TEST_URL%
echo.
echo Calling protocol handler...
echo.

call protocol-handler.bat "%TEST_URL%"

pause