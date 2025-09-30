@echo off
echo Testing direct JAR execution...

set "ENROLLABLE_ID=22"
set "ENROLLABLE_TYPE=App\Models\Inmate"
set "API_TOKEN=1|kbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084"
set "API_URL=http://127.0.0.1:8000/api"

cd /d "C:\Users\abdia\code\GP360\BiometricService"

java -cp "dist\BiometricService.jar;lib\*" ^
     -DenrollableId=%ENROLLABLE_ID% ^
     -DenrollableType="%ENROLLABLE_TYPE%" ^
     -Dapi.token="%API_TOKEN%" ^
     -Dapi.url="%API_URL%" ^
     com.gp360.biometric.BiometricApplication

pause