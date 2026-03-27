param(
    [switch]$InitOnly,
    [switch]$ApplyFeedKeys,
    [switch]$RewriteAllFeedKeys
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$simScript = Join-Path $repoRoot "simulator\digital-twin\scripts\run-simulator.ps1"

if (-not (Test-Path $simScript)) {
    throw "Cannot find simulator run script at $simScript"
}

$args = @()
if ($InitOnly) { $args += "-InitOnly" }
if ($ApplyFeedKeys) { $args += "-ApplyFeedKeys" }
if ($RewriteAllFeedKeys) { $args += "-RewriteAllFeedKeys" }

& $simScript @args
