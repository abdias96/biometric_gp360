@echo off
echo ========================================
echo Cerrando todos los procesos Java
echo ========================================

REM Cerrar todos los procesos java.exe y javaw.exe
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul

echo.
echo Procesos Java cerrados.
echo El lector biometrico ahora esta disponible.
echo.
pause