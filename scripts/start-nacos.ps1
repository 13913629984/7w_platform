param(
  [string]$NacosHome = $env:NACOS_HOME
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($NacosHome)) {
  $NacosHome = 'D:\01_tool\dev\nacos-server-2.5.2\nacos'
}

$startup = Join-Path $NacosHome 'bin\startup.cmd'
if (-not (Test-Path $startup)) {
  Write-Host "[ERROR] Nacos startup.cmd not found: $startup" -ForegroundColor Red
  Write-Host 'Set NACOS_HOME or edit this script.'
  exit 1
}

Set-Location (Join-Path $NacosHome 'bin')
Write-Host '[START] Nacos standalone: http://localhost:8848/nacos'
& $startup -m standalone
