@echo off
chcp 65001 >nul
setlocal
title QWS Platform - Start All
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
  echo [ERROR] One or more services failed to start. Review the output above.
) else (
  echo [DONE] QWS Platform services are running.
  echo Frontend: http://localhost:4000
)
pause
exit /b %RC%
