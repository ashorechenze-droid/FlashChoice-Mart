@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-project.ps1" %*
if errorlevel 1 (
    echo.
    echo Startup failed. See the error above.
    pause
    exit /b 1
)
echo.
echo Startup finished.
pause
