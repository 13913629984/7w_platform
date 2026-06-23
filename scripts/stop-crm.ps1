$conns = Get-NetTCPConnection -LocalPort 8092 -State Listen -ErrorAction SilentlyContinue
if (-not $conns) { Write-Host "No process listening on 8092"; exit 0 }
foreach ($x in $conns) {
    $p = Get-CimInstance Win32_Process -Filter "ProcessId=$($x.OwningProcess)" -ErrorAction SilentlyContinue
    if ($p -and $p.CommandLine -like '*qws-crm*') {
        Stop-Process -Id $x.OwningProcess -Force
        Write-Host "Stopped qws-crm PID $($x.OwningProcess)"
    } else {
        Write-Host "Port 8092 held by non-crm PID $($x.OwningProcess): $($p.CommandLine)"
    }
}
