@echo off
echo ========================================
echo Final Protocol Handler Test
echo ========================================
echo.
echo This will open the biometric client with test data.
echo.
echo Press any key to launch the protocol handler...
pause > nul

start gp360://enroll?id=22^&type=inmate^&token=1^|kbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084^&api=http://127.0.0.1:8000/api

echo.
echo Protocol handler launched!
echo Check if the biometric application opened with ID=22
echo.
pause