$ErrorActionPreference = 'Stop'
$PlatformRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FrontendRoot = Join-Path $PlatformRoot 'frontend'

Set-Location $FrontendRoot
if (-not (Test-Path 'node_modules')) {
  Write-Host '[SETUP] node_modules not found, running npm install...'
  npm install
}
Write-Host '[START] frontend on port 4000: http://localhost:4000'
npm run dev
