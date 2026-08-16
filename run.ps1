<#
.SYNOPSIS
    Runs Taxi Racer in Docker and opens it in your browser.

.DESCRIPTION
    The one-command way to play. Needs Docker Desktop and nothing else - no Java, no
    JavaFX, no build tools. The game runs inside the container on its own display and is
    served to your browser over noVNC.

    Press Ctrl+C to stop the game and shut the container down.

.EXAMPLE
    .\run.ps1
    .\run.ps1 -Rebuild
#>
[CmdletBinding()]
param(
    # Force the image to be rebuilt even if it already exists.
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$Url = 'http://localhost:8080/'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed. Install Docker Desktop from https://docker.com, or run natively with .\play.ps1 (needs a JDK)."
}

try {
    docker info --format '{{.ServerVersion}}' 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
} catch {
    throw "Docker is installed but the engine is not running. Start Docker Desktop and try again."
}

$buildArgs = @('compose', 'up', '-d')
if ($Rebuild) { $buildArgs += '--build' }

Write-Host "Building and starting Taxi Racer (the first run downloads and compiles, so give it a minute)..." -ForegroundColor Cyan
& docker @buildArgs
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed." }

Write-Host "Waiting for the game to come up..." -ForegroundColor Cyan
$ready = $false
foreach ($attempt in 1..60) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $ready) {
    Write-Warning "The game did not answer on $Url in time. Recent container output:"
    & docker compose logs --tail 40
    throw "Startup timed out."
}

Write-Host ""
Write-Host "  Taxi Racer is running at $Url" -ForegroundColor Green
Write-Host "  Saves are kept in .\saves" -ForegroundColor DarkGray
Write-Host "  Press Ctrl+C to stop." -ForegroundColor DarkGray
Write-Host ""

Start-Process $Url

try {
    & docker compose logs -f
} finally {
    Write-Host "`nStopping Taxi Racer..." -ForegroundColor Cyan
    & docker compose down | Out-Null
}
