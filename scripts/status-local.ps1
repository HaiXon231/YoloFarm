Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "=== Backend (port 8080) ===" -ForegroundColor Cyan
$backend = netstat -ano | Select-String ":8080"
if ($backend) {
    $backend | ForEach-Object { $_.ToString() }
} else {
    Write-Host "No process listening on port 8080." -ForegroundColor Yellow
}

Write-Host "`n=== Simulator (python main.py) ===" -ForegroundColor Cyan
$simProcs = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -match "python" -and
        $_.CommandLine -match "digital-twin\\main.py"
    }

if ($simProcs) {
    $simProcs | Select-Object ProcessId, Name, CommandLine | Format-List
} else {
    Write-Host "No digital-twin simulator process found." -ForegroundColor Yellow
}
