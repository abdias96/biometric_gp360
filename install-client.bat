@echo off
echo ========================================
echo GP360 BIOMETRIC CLIENT INSTALLER
echo ========================================
echo.
echo Este instalador configurará el Cliente Biométrico GP360 en su máquina
echo.

REM Check for admin rights
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: Se requieren permisos de Administrador.
    echo Por favor ejecute este instalador como Administrador.
    pause
    exit /b 1
)

REM Set installation directory
set "INSTALL_DIR=%ProgramFiles%\GP360\BiometricClient"
set "CURRENT_DIR=%~dp0"

echo Instalando en: %INSTALL_DIR%
echo.

REM Create installation directory
if not exist "%INSTALL_DIR%" (
    echo Creando directorio de instalación...
    mkdir "%INSTALL_DIR%"
)

REM Copy files
echo Copiando archivos del sistema...
xcopy /Y /E /I "%CURRENT_DIR%dist" "%INSTALL_DIR%\dist"
xcopy /Y /E /I "%CURRENT_DIR%lib" "%INSTALL_DIR%\lib"
copy /Y "%CURRENT_DIR%*.dll" "%INSTALL_DIR%\"
copy /Y "%CURRENT_DIR%protocol-handler.bat" "%INSTALL_DIR%\"

REM Create protocol handler with correct path
echo Registrando protocolo gp360://...
reg add "HKCR\gp360" /ve /d "GP360 Biometric Protocol" /f
reg add "HKCR\gp360" /v "URL Protocol" /d "" /f
reg add "HKCR\gp360\DefaultIcon" /ve /d "%INSTALL_DIR%\dist\BiometricService.jar,0" /f
reg add "HKCR\gp360\shell" /ve /f
reg add "HKCR\gp360\shell\open" /ve /f
reg add "HKCR\gp360\shell\open\command" /ve /d "\"%INSTALL_DIR%\protocol-handler.bat\" \"%%1\"" /f

REM Create Start Menu shortcut
echo Creando acceso directo en el menú Inicio...
set "SHORTCUT=%ProgramData%\Microsoft\Windows\Start Menu\Programs\GP360 Biometric Client.lnk"
powershell -Command "$WshShell = New-Object -comObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%SHORTCUT%'); $Shortcut.TargetPath = '%INSTALL_DIR%\protocol-handler.bat'; $Shortcut.WorkingDirectory = '%INSTALL_DIR%'; $Shortcut.IconLocation = '%INSTALL_DIR%\dist\BiometricService.jar, 0'; $Shortcut.Description = 'GP360 Biometric Client'; $Shortcut.Save()"

REM Check Java installation
echo.
echo Verificando instalación de Java...
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo ADVERTENCIA: Java no está instalado o no está en el PATH.
    echo Por favor instale Java 11 o superior para usar el Cliente Biométrico.
    echo Descarga: https://adoptium.net/
    echo.
) else (
    echo Java detectado correctamente.
)

REM Add to PATH (optional)
echo.
echo ¿Desea agregar el Cliente Biométrico al PATH del sistema? (S/N)
set /p ADD_PATH=
if /i "%ADD_PATH%"=="S" (
    setx PATH "%PATH%;%INSTALL_DIR%" /M
    echo Cliente agregado al PATH del sistema.
)

echo.
echo ========================================
echo INSTALACIÓN COMPLETADA
echo ========================================
echo.
echo El Cliente Biométrico GP360 ha sido instalado exitosamente.
echo.
echo Características instaladas:
echo ✓ Cliente Biométrico en: %INSTALL_DIR%
echo ✓ Protocolo gp360:// registrado
echo ✓ Acceso directo en el menú Inicio
echo.
echo IMPORTANTE:
echo - Conecte su lector DigitalPersona 4500/5000
echo - El sistema está listo para capturar huellas desde el navegador
echo - No necesita ejecutar ninguna aplicación manualmente
echo.
pause