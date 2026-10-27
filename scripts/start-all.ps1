[CmdletBinding()]
param(
    [switch]$SkipNacos,
    [switch]$SkipFrontend,
    # 默认并行启动：每个服务自身约 7~10s，串行 8 个就是 80s+，并行只受最慢的那个约束。
    # 需要看清晰顺序日志时用 -Serial。
    [switch]$Serial,
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$PlatformRoot = Split-Path -Parent $ScriptRoot
$LogRoot = Join-Path $PlatformRoot 'logs'
if (-not (Test-Path -LiteralPath $LogRoot)) {
    New-Item -ItemType Directory -Path $LogRoot | Out-Null
}

function Test-QwsPort {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-QwsPort {
    param([int]$Port, [int]$Timeout = $TimeoutSeconds)
    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        if (Test-QwsPort -Port $Port) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return (Test-QwsPort -Port $Port)
}

function Start-QwsBackgroundProcess {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [string]$StdOut,
        [string]$StdErr
    )

    $startArgs = @{
        FilePath = $FilePath
        ArgumentList = $Arguments
        WorkingDirectory = $WorkingDirectory
        WindowStyle = 'Hidden'
        RedirectStandardOutput = $StdOut
        RedirectStandardError = $StdErr
        PassThru = $true
    }
    return Start-Process @startArgs
}

function Start-QwsBundledService {
    param([string]$Name, [int]$Port, [string]$ScriptFile)

    if (Test-QwsPort -Port $Port) {
        Write-Host ("[SKIP] {0} already listening on port {1}" -f $Name, $Port) -ForegroundColor Yellow
        return $true
    }

    $scriptPath = Join-Path $ScriptRoot $ScriptFile
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        Write-Host ("[ERROR] Missing script: {0}" -f $scriptPath) -ForegroundColor Red
        return $false
    }

    Write-Host ("[START] {0} on port {1}" -f $Name, $Port) -ForegroundColor Cyan
    try {
        & $scriptPath
    } catch {
        Write-Host ("[ERROR] {0} failed: {1}" -f $Name, $_.Exception.Message) -ForegroundColor Red
        return $false
    }

    if (Wait-QwsPort -Port $Port -Timeout $TimeoutSeconds) {
        Write-Host ("[READY] {0} is listening on port {1}" -f $Name, $Port) -ForegroundColor Green
        return $true
    }

    Write-Host ("[ERROR] {0} did not listen on port {1} within {2}s" -f $Name, $Port, $TimeoutSeconds) -ForegroundColor Red
    return $false
}

$results = [ordered]@{}

Write-Host ''
Write-Host 'QWS Platform - start all services' -ForegroundColor White
Write-Host '---------------------------------' -ForegroundColor DarkGray

# Nacos first: gateway and discovery clients need it.
if ($SkipNacos) {
    Write-Host '[SKIP] Nacos (requested)' -ForegroundColor Yellow
    $results['nacos'] = $true
} elseif (Test-QwsPort -Port 8848) {
    Write-Host '[SKIP] Nacos already listening on port 8848' -ForegroundColor Yellow
    $results['nacos'] = $true
} else {
    $nacosScript = Join-Path $ScriptRoot 'start-nacos.ps1'
    if (-not (Test-Path -LiteralPath $nacosScript)) {
        Write-Host ("[ERROR] Missing script: {0}" -f $nacosScript) -ForegroundColor Red
        $results['nacos'] = $false
    } else {
        Write-Host '[START] Nacos on port 8848' -ForegroundColor Cyan
        $null = Start-QwsBackgroundProcess -FilePath 'powershell.exe' -Arguments @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $nacosScript) -WorkingDirectory $PlatformRoot -StdOut (Join-Path $LogRoot 'start-nacos.ps1.out.log') -StdErr (Join-Path $LogRoot 'start-nacos.ps1.err.log')
        if (Wait-QwsPort -Port 8848 -Timeout $TimeoutSeconds) {
            Write-Host '[READY] Nacos is listening on port 8848' -ForegroundColor Green
            $results['nacos'] = $true
        } else {
            Write-Host ("[ERROR] Nacos did not listen on port 8848 within {0}s" -f $TimeoutSeconds) -ForegroundColor Red
            $results['nacos'] = $false
        }
    }
}

# Gateway is started last so its routes can resolve the backend services.
$services = @(
    @{ Name = 'qws-auth';    Port = 8090; Script = 'start-auth.ps1' },
    @{ Name = 'qws-system';  Port = 8091; Script = 'start-system.ps1' },
    @{ Name = 'qws-crm';     Port = 8092; Script = 'start-crm.ps1' },
    @{ Name = 'qws-wms';     Port = 8093; Script = 'start-wms.ps1' },
    @{ Name = 'qws-fin';     Port = 8094; Script = 'start-fin.ps1' },
    @{ Name = 'qws-maint';   Port = 8095; Script = 'start-maint.ps1' },
    @{ Name = 'qws-ets';     Port = 8096; Script = 'start-ets.ps1' },
    @{ Name = 'qws-gateway'; Port = 8080; Script = 'start-gateway.ps1' }
)

# /actuator/health 才是真就绪（端口开 ≠ Spring ready）。
# 注意：PowerShell 5.1 对 application/vnd.spring-boot.actuator.v3+json 不解码成文本，
# $resp.Content 会是 byte[]，必须先转字符串再匹配，否则永远判不健康（历史 bug）。
function Test-QwsHealthy {
    param([int]$Port)
    try {
        $resp = Invoke-WebRequest -Uri ("http://localhost:{0}/actuator/health" -f $Port) -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        $body = $resp.Content
        if ($body -is [byte[]]) { $body = [System.Text.Encoding]::UTF8.GetString($body) }
        return ($resp.StatusCode -eq 200 -and $body -match '"UP"')
    } catch {
        return $false
    }
}

function Start-QwsServicesParallel {
    param($ServiceList)

    $pending = @()
    foreach ($s in $ServiceList) {
        if (Test-QwsPort -Port $s.Port) {
            Write-Host ("[SKIP] {0} already listening on port {1}" -f $s.Name, $s.Port) -ForegroundColor Yellow
            $results[$s.Name] = $true
            continue
        }
        $scriptPath = Join-Path $ScriptRoot $s.Script
        if (-not (Test-Path -LiteralPath $scriptPath)) {
            Write-Host ("[ERROR] Missing script: {0}" -f $scriptPath) -ForegroundColor Red
            $results[$s.Name] = $false
            continue
        }
        $outLog = Join-Path $LogRoot ("start-{0}.out.log" -f $s.Name)
        $errLog = Join-Path $LogRoot ("start-{0}.err.log" -f $s.Name)
        $null = Start-QwsBackgroundProcess -FilePath 'powershell.exe' -Arguments @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $scriptPath) -WorkingDirectory $PlatformRoot -StdOut $outLog -StdErr $errLog
        Write-Host ("[START] {0} on port {1}" -f $s.Name, $s.Port) -ForegroundColor Cyan
        $pending += [pscustomobject]@{ Name = $s.Name; Port = $s.Port; Log = $outLog }
    }

    if (@($pending).Count -eq 0) { return }

    Write-Host ("[WAIT] {0} service(s) starting in parallel" -f @($pending).Count) -ForegroundColor DarkGray
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while (@($pending).Count -gt 0 -and (Get-Date) -lt $deadline) {
        foreach ($p in @($pending)) {
            if (Test-QwsHealthy -Port $p.Port) {
                Write-Host ("[READY] {0} is healthy on port {1} (t+{2}s)" -f $p.Name, $p.Port, [math]::Round($sw.Elapsed.TotalSeconds, 1)) -ForegroundColor Green
                $results[$p.Name] = $true
                $pending = @($pending | Where-Object { $_.Name -ne $p.Name })
            }
        }
        if (@($pending).Count -gt 0) { Start-Sleep -Milliseconds 500 }
    }

    foreach ($p in @($pending)) {
        Write-Host ("[ERROR] {0} not healthy on port {1} within {2}s" -f $p.Name, $p.Port, $TimeoutSeconds) -ForegroundColor Red
        if (Test-Path -LiteralPath $p.Log) {
            Get-Content -LiteralPath $p.Log -Tail 15 | ForEach-Object { Write-Host ("        " + $_) -ForegroundColor DarkGray }
        }
        $results[$p.Name] = $false
    }
}

if ($Serial) {
    foreach ($service in $services) {
        $results[$service.Name] = Start-QwsBundledService -Name $service.Name -Port $service.Port -ScriptFile $service.Script
    }
} else {
    Start-QwsServicesParallel -ServiceList $services
}

# Vite dev server on port 4000.
function Test-QwsFrontendHealthy {
    try {
        $resp = Invoke-WebRequest -Uri 'http://localhost:4000/' -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        return ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500)
    } catch {
        return $false
    }
}

function Clear-QwsFrontendPort {
    # 4000 端口若被上次残留的 vite 占用，vite 默认会静默漂移到 4001（访问 4000 就连不上）
    $conns = Get-NetTCPConnection -LocalPort 4000 -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return $true }
    $allCleared = $true
    foreach ($conn in $conns) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($conn.OwningProcess)" -ErrorAction SilentlyContinue
        if ($proc -and ($proc.CommandLine -like '*vite*' -or $proc.CommandLine -like '*npm*run dev*')) {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-Host ("[CLEAN] Stopped stale dev server PID={0} on port 4000" -f $conn.OwningProcess) -ForegroundColor Yellow
        } else {
            $name = if ($proc) { $proc.Name } else { 'unknown' }
            Write-Host ("[ERROR] Port 4000 is held by PID={0} ({1}), not a vite process. Free it manually." -f $conn.OwningProcess, $name) -ForegroundColor Red
            $allCleared = $false
        }
    }
    if ($allCleared) { Start-Sleep -Seconds 1 }
    return $allCleared
}

function Start-QwsFrontend {
    $frontendRoot = Join-Path $PlatformRoot 'frontend'
    $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue

    if (-not (Test-Path -LiteralPath $frontendRoot)) {
        Write-Host ("[ERROR] Frontend directory not found: {0}" -f $frontendRoot) -ForegroundColor Red
        return $false
    }
    if (-not $npm) {
        Write-Host '[ERROR] npm.cmd was not found in PATH' -ForegroundColor Red
        return $false
    }
    if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules'))) {
        Write-Host '[SETUP] frontend/node_modules not found, running npm install...' -ForegroundColor Cyan
        Push-Location $frontendRoot
        try {
            & $npm.Source install
            if ($LASTEXITCODE -ne 0) { throw ("npm install exited with code {0}" -f $LASTEXITCODE) }
        } catch {
            Write-Host ("[ERROR] npm install failed: {0}" -f $_.Exception.Message) -ForegroundColor Red
            return $false
        } finally {
            Pop-Location
        }
    }

    if (-not (Clear-QwsFrontendPort)) { return $false }

    Write-Host '[START] frontend on port 4000' -ForegroundColor Cyan
    # --strictPort：端口不可用时立刻失败，避免静默换到 4001
    $null = Start-QwsBackgroundProcess -FilePath $npm.Source -Arguments @('run', 'dev', '--', '--strictPort') -WorkingDirectory $frontendRoot -StdOut (Join-Path $LogRoot 'frontend.out.log') -StdErr (Join-Path $LogRoot 'frontend.err.log')

    if (-not (Wait-QwsPort -Port 4000 -Timeout $TimeoutSeconds)) {
        Write-Host ("[ERROR] frontend did not listen on port 4000 within {0}s" -f $TimeoutSeconds) -ForegroundColor Red
        $tail = Join-Path $LogRoot 'frontend.out.log'
        if (Test-Path -LiteralPath $tail) {
            Write-Host '        --- frontend.out.log (tail) ---' -ForegroundColor DarkGray
            Get-Content -LiteralPath $tail -Tail 15 | ForEach-Object { Write-Host ("        " + $_) -ForegroundColor DarkGray }
        }
        return $false
    }

    # 端口开着不代表页面能访问，再确认一次 HTTP
    for ($i = 0; $i -lt 20; $i++) {
        if (Test-QwsFrontendHealthy) {
            Write-Host '[READY] frontend is listening on port 4000' -ForegroundColor Green
            return $true
        }
        Start-Sleep -Milliseconds 500
    }

    Write-Host '[ERROR] port 4000 is open but HTTP request failed. Check frontend.out.log' -ForegroundColor Red
    return $false
}

if ($SkipFrontend) {
    Write-Host '[SKIP] frontend (requested)' -ForegroundColor Yellow
    $results['frontend'] = $true
} elseif (Test-QwsPort -Port 4000) {
    if (Test-QwsFrontendHealthy) {
        Write-Host '[SKIP] frontend already healthy on port 4000' -ForegroundColor Yellow
        $results['frontend'] = $true
    } else {
        Write-Host '[WARN] port 4000 is open but not serving HTTP; restarting it' -ForegroundColor Yellow
        $results['frontend'] = Start-QwsFrontend
    }
} else {
    $results['frontend'] = Start-QwsFrontend
}

Write-Host ''
Write-Host 'Startup summary' -ForegroundColor White
Write-Host '---------------' -ForegroundColor DarkGray
foreach ($item in $results.GetEnumerator()) {
    $status = if ($item.Value) { 'OK' } else { 'FAILED' }
    $color = if ($item.Value) { 'Green' } else { 'Red' }
    Write-Host ("{0,-12} {1}" -f $item.Key, $status) -ForegroundColor $color
}

$failed = @($results.GetEnumerator() | Where-Object { -not $_.Value })
if ($failed.Count -gt 0) {
    throw ("{0} service(s) failed to start. Check logs in {1}" -f $failed.Count, $LogRoot)
}

Write-Host ''
Write-Host 'Frontend:  http://localhost:4000' -ForegroundColor Green
Write-Host 'Gateway:   http://localhost:8080' -ForegroundColor Green
Write-Host 'Nacos:     http://localhost:8848/nacos' -ForegroundColor Green
Write-Host ("Logs:      {0}" -f $LogRoot) -ForegroundColor DarkGray
