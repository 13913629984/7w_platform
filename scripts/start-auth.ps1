param(
    [string]$BackendRoot = (Join-Path (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)) 'backend'),
    [int]$Port = 8090
)

$ErrorActionPreference = 'Stop'
$logRoot = Join-Path (Split-Path -Parent $BackendRoot) 'logs'
if (-not (Test-Path $logRoot)) { New-Item -ItemType Directory -Path $logRoot | Out-Null }

$service = 'qws-auth'
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

# Wait until the service reports healthy.
# 只探端口不够：Tomcat 会先绑定端口、再完成 Spring 上下文初始化，
# 这中间前端打过来的请求会 502/500。改用 /actuator/health 才算真正就绪。
# 注意（踩过的坑）：PowerShell 5.1 对 actuator 返回的
# application/vnd.spring-boot.actuator.v3+json 这类"未知 +json"类型不会按文本解码，
# $resp.Content 会退化成 byte[]，于是 $resp.Content -match '"UP"' 恒为假，
# 服务其实 9s 就 Ready 了，脚本却白等满 90s 才放行 —— 这是"一个服务要 1 分多钟"的真正原因。
# 现在统一先转成字符串再判断，并把轮询间隔降到 500ms，就绪即放行。
$up = $false
$sw = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt 180; $i++) {
    Start-Sleep -Milliseconds 500
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        $body = $resp.Content
        if ($body -is [byte[]]) { $body = [System.Text.Encoding]::UTF8.GetString($body) }
        if ($resp.StatusCode -eq 200 -and $body -match '"UP"') {
            $up = $true
            Write-Host ("[READY] {0} is healthy (after ~{1}s, PID={2})" -f $service, [math]::Round($sw.Elapsed.TotalSeconds, 1), $proc.Id) -ForegroundColor Green
            break
        }
    } catch {
        # 还没起来，继续等
    }
    # 进程已退出就没必要继续等
    if ($proc.HasExited) {
        Write-Host "[ERROR] $service exited early (exit code $($proc.ExitCode)). Tail of ${logOut}:" -ForegroundColor Red
        Get-Content $logOut -Tail 30 -ErrorAction SilentlyContinue
        break
    }
}
if (-not $up) {
    Write-Host "[WARN] $service did not become healthy on port $Port in 90s. Tail of ${logOut}:" -ForegroundColor Red
    Get-Content $logOut -Tail 30
}
