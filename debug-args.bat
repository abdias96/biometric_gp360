@echo off
echo ========================================
echo DEBUG: Raw Arguments
echo ========================================
echo.
echo Number of args: %0 %1 %2 %3 %4 %5 %6 %7 %8 %9
echo.
echo All args with star: %*
echo.
echo Arg 1 raw: %1
echo Arg 1 unquoted: %~1
echo.

REM Save to file for inspection
echo %date% %time% >> debug-args.log
echo Raw: %* >> debug-args.log
echo Arg1: %1 >> debug-args.log
echo Unquoted: %~1 >> debug-args.log
echo ------------------------ >> debug-args.log

pause