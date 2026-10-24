$ErrorActionPreference = 'Stop'
$PlatformRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FrontendRoot = Join-Path $PlatformRoot 'frontend'

Set-Location $FrontendRoot

if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
  Write-Host '[ERROR] npm.cmd not found in PATH' -ForegroundColor Red
  exit 1
}

if (-not (Test-Path 'node_modules')) {
  Write-Host '[SETUP] node_modules not found, running npm install...'
  npm install
  if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] npm install failed' -ForegroundColor Red
    exit 1
  }
}

# 端口被占用时 vite 默认会静默漂移（4000 -> 4001 ...），导致“看似启动成功”
# 但访问 4000 失败。这里先清理占用，再用 --strictPort 强制锁定端口。
$existing = Get-NetTCPConnection -LocalPort 4000 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
  foreach ($conn in $existing) {
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($conn.OwningProcess)" -ErrorAction SilentlyContinue
    if ($proc -and ($proc.CommandLine -like '*vite*' -or $proc.CommandLine -like '*npm*run dev*')) {
      Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
      Write-Host ("[CLEAN] Stopped previous dev server PID={0} on port 4000" -f $conn.OwningProcess) -ForegroundColor Yellow
    } else {
      Write-Host ("[ERROR] Port 4000 is occupied by PID={0}, not a vite process. Refusing to kill it." -f $conn.OwningProcess) -ForegroundColor Red
      exit 1
    }
  }
  Start-Sleep -Seconds 1
}

Write-Host '[START] frontend on port 4000: http://localhost:4000'
Write-Host '[HINT] Use --strictPort so vite fails loudly instead of drifting to 4001'

& npm.cmd run dev -- --strictPort
