param(
    [string]$EnvFile = ".env"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    if (Test-Path $EnvFile) {
        $EnvFile = (Resolve-Path $EnvFile).Path
    } else {
        $EnvFile = Join-Path $projectRoot $EnvFile
    }
}

if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing $EnvFile. Copy .env.example to .env and fill values first."
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }

    $parts = $line.Split('=', 2)
    if ($parts.Length -ne 2) { return }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()

    if (-not [string]::IsNullOrWhiteSpace($key)) {
        [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

$required = @(
    "DB_USERNAME",
    "DB_PASSWORD",
    "JWT_SECRET_KEY",
    "ADAFRUIT_USERNAME",
    "ADAFRUIT_IO_KEY"
)

$missing = @()
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "Process"))) {
        $missing += $name
    }
}

if ($missing.Count -gt 0) {
    Write-Error ("Missing required env vars: " + ($missing -join ", "))
}

Write-Host "Loaded env from $EnvFile. Starting Spring Boot..."
Push-Location $projectRoot
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
