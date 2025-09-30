@echo off
REM This wrapper handles the URL properly and calls PowerShell
REM Windows Registry will call this with the full URL

REM Get all arguments as one string
setlocal enabledelayedexpansion
set "URL=%*"

REM Replace quotes if any
set "URL=!URL:"=!"

REM Call PowerShell with the URL
powershell.exe -WindowStyle Normal -ExecutionPolicy Bypass -File "%~dp0protocol-handler.ps1" "!URL!"