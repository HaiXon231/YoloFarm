param(
    [string]$EnvFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendScript = Join-Path $repoRoot "backend\api\scripts\run-local.ps1"

if (-not (Test-Path $backendScript)) {
    throw "Cannot find backend run script at $backendScript"
}

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    & $backendScript
} else {
    & $backendScript -EnvFile $EnvFile
}
