param(
    [string]$BackendRoot = (Join-Path (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)) 'backend'),
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$logRoot = Join-Path (Split-Path -Parent $BackendRoot) 'logs'
if (-not (Test-Path $logRoot)) { New-Item -ItemType Directory -Path $logRoot | Out-Null }

$service = 'qws-gateway'
$jar = Join-Path $BackendRoot "$service\target\$service-1.0.0-SNAPSHOT.jar"
$logOut = Join-Path $logRoot "$service.out.log"
$logErr = Join-Path $logRoot "$service.err.log"

# Stop only this QWS service if it is already bound to the target port.
$existing = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    foreach ($connection in $existing) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine -like "*$service*") {
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-Host "[CLEAN] Stopped previous $service process $($connection.OwningProcess) on port $Port" -ForegroundColor Yellow
        } else {
            throw "Port $Port is occupied by another process $($connection.OwningProcess). Not stopping it for safety."
        }
    }
    Start-Sleep -Seconds 1
}

# Build the fat jar if missing
if (-not (Test-Path $jar)) {
    Write-Host "[BUILD] mvn -pl $service -am package -DskipTests" -ForegroundColor Cyan
    Set-Location $BackendRoot
    & mvn -pl $service -am package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed for $service" }
}

Write-Host "[START] $service on port $Port -> http://localhost:$Port" -ForegroundColor Green
Write-Host "        log: $logOut"

$startArgs = @{
    FilePath               = 'java'
    ArgumentList           = @('-jar', $jar)
    PassThru               = $true
    WindowStyle            = 'Hidden'
    RedirectStandardOutput = $logOut
    RedirectStandardError  = $logErr
}
$proc = Start-Process @startArgs

# Wait for the port to open
$up = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if (Test-NetConnection -ComputerName 'localhost' -Port $Port -InformationLevel Quiet 2>$null) {
        $up = $true
        Write-Host "[READY] $service is up (after ~${i}s, PID=$($proc.Id))" -ForegroundColor Green
        break
    }
}
if (-not $up) {
    Write-Host "[WARN] $service did not open port $Port in 60s. Tail of ${logOut}:" -ForegroundColor Red
    Get-Content $logOut -Tail 30
}
