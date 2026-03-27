Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Stopping backend on port 8080 (if any)..." -ForegroundColor Cyan
$lines = netstat -ano | Select-String ":8080" | ForEach-Object { $_.ToString() }
$pids = @()
foreach ($line in $lines) {
    $parts = ($line -split "\s+") | Where-Object { $_ -ne "" }
    if ($parts.Length -ge 5) {
        $pid = $parts[$parts.Length - 1]
        if ($pid -match "^\d+$") { $pids += [int]$pid }
    }
}
$pids = $pids | Select-Object -Unique
foreach ($pid in $pids) {
    try {
        Stop-Process -Id $pid -Force -ErrorAction Stop
        Write-Host "Stopped PID $pid (port 8080)." -ForegroundColor Green
    } catch {
        Write-Host "Cannot stop PID $pid: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "Stopping digital-twin python processes (if any)..." -ForegroundColor Cyan
$simProcs = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -match "python" -and
        $_.CommandLine -match "digital-twin\\main.py"
    }

foreach ($proc in $simProcs) {
    try {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
        Write-Host "Stopped simulator PID $($proc.ProcessId)." -ForegroundColor Green
    } catch {
        Write-Host "Cannot stop simulator PID $($proc.ProcessId): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "Done." -ForegroundColor Cyan
