param(
    [switch]$InitOnly,
    [switch]$ApplyFeedKeys,
    [switch]$RewriteAllFeedKeys
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

function Write-Step([string]$text) {
    Write-Host "`n==> $text" -ForegroundColor Cyan
}

function Ensure-File([string]$target, [string]$source) {
    if (-not (Test-Path $target)) {
        Copy-Item $source $target
        Write-Host "Created $(Split-Path -Leaf $target) from example template." -ForegroundColor Yellow
    }
}

Push-Location $projectRoot
try {
    Write-Step "Checking Python"
    $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
    if ($null -eq $pythonCmd) {
        throw "Python is not installed or not found in PATH."
    }

    Write-Step "Preparing virtual environment"
    if (-not (Test-Path ".venv\Scripts\python.exe")) {
        python -m venv .venv
    }

    $venvPython = Join-Path $projectRoot ".venv\Scripts\python.exe"

    Write-Step "Installing dependencies"
    & $venvPython -m pip install --upgrade pip
    & $venvPython -m pip install -r requirements.txt

    Write-Step "Ensuring local config files"
    Ensure-File ".env" ".env.example"
    Ensure-File "profiles.json" "profiles.example.json"

    Write-Step "Validating Python scripts"
    & $venvPython -m py_compile main.py tools\feed_key_manager.py

    Write-Step "Running feed key planner"
    & $venvPython tools\feed_key_manager.py

    if ($ApplyFeedKeys) {
        Write-Step "Applying generated feed keys to database"
        if ($RewriteAllFeedKeys) {
            & $venvPython tools\feed_key_manager.py --rewrite-all --apply
        } else {
            & $venvPython tools\feed_key_manager.py --apply
        }
    }

    if ($InitOnly) {
        Write-Host "Initialization complete. Start simulator later with: .\scripts\run-simulator.ps1" -ForegroundColor Green
        exit 0
    }

    Write-Step "Starting digital twin simulator"
    & $venvPython main.py
}
finally {
    Pop-Location
}
