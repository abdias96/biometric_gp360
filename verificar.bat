@echo off
echo ========================================
echo BiometricService - Modo Verificacion 1:N
echo ========================================
echo.

REM Verificar que Java esta instalado
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java no esta instalado o no esta en el PATH
    pause
    exit /b 1
)

REM Verificar que el JAR existe
if not exist "dist\BiometricVerification.jar" (
    echo ERROR: No se encuentra dist\BiometricVerification.jar
    echo Compile primero con: build.bat
    pause
    exit /b 1
)

echo Iniciando verificacion biometrica 1:N...
echo.

java -cp "dist\BiometricVerification.jar;lib\*" com.gp360.biometric.verification.VerificationApp

pause