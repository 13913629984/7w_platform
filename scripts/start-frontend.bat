@echo off
chcp 65001 >nul
setlocal
set "PLATFORM_ROOT=%~dp0.."
set "FRONTEND_ROOT=%PLATFORM_ROOT%\frontend"
title QWS Frontend 3000
cd /d "%FRONTEND_ROOT%"
if not exist "node_modules" (
  echo [SETUP] node_modules not found, running npm install...
  npm install
  if errorlevel 1 pause & exit /b 1
)
echo [START] frontend on port 3000: http://localhost:3000
npm run dev
pause
