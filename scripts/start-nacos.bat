@echo off
chcp 65001 >nul
setlocal
if "%NACOS_HOME%"=="" set "NACOS_HOME=D:\01_tool\dev\nacos-server-2.5.2\nacos"
set "STARTUP=%NACOS_HOME%\bin\startup.cmd"
if not exist "%STARTUP%" (
  echo [ERROR] Nacos startup.cmd not found: %STARTUP%
  echo Set NACOS_HOME or edit this script.
  pause
  exit /b 1
)
title QWS Nacos 8848
cd /d "%NACOS_HOME%\bin"
echo [START] Nacos standalone: http://localhost:8848/nacos
call startup.cmd -m standalone
pause
