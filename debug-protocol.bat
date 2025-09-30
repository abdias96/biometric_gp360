@echo off
echo ========================================
echo DEBUG: Protocol Handler Arguments
echo ========================================
echo.
echo Percent 0 (script): %0
echo Percent 1 (first arg): %1
echo Percent star (all args): %*
echo Percent tilde 1 (unquoted): %~1
echo.
echo Writing to debug file...
echo %date% %time% >> protocol-debug.log
echo Args: %* >> protocol-debug.log
echo Arg1: %1 >> protocol-debug.log
echo Unquoted: %~1 >> protocol-debug.log
echo ---------------------------------------- >> protocol-debug.log
echo.
echo Check protocol-debug.log for details
pause