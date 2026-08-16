<#
.SYNOPSIS
    Builds and runs Taxi Racer natively on Windows.

.DESCRIPTION
    Needs a JDK 17 or newer on the PATH and nothing else. The JavaFX SDK is downloaded
    once into .javafx\ and reused afterwards, so there is no Maven, Gradle or manual
    JavaFX setup involved.

    Set JAVAFX_HOME to point at an existing JavaFX SDK to skip the download.

.EXAMPLE
    .\play.ps1
    .\play.ps1 -Clean
#>
[CmdletBinding()]
param(
    # Recompile from scratch.
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$JavaFxVersion = '21.0.5'
$ClassesDir    = 'build\classes'
# Cached outside the project: the SDK is ~120 MB and this repository may well live in a
# synced folder, where a dependency cache has no business being uploaded to the cloud.
$SdkRoot       = Join-Path $env:LOCALAPPDATA 'TaxiRacer\javafx'

function Assert-Jdk {
    $javac = Get-Command javac -ErrorAction SilentlyContinue
    if (-not $javac) {
        throw "No JDK found. Install one (Temurin 21 from https://adoptium.net) or use Docker instead: docker compose up"
    }
    $versionText = (& javac -version 2>&1) -join ' '
    if ($versionText -notmatch '(\d+)') {
        throw "Could not read the javac version from: $versionText"
    }
    $major = [int]$Matches[1]
    if ($major -lt 17) {
        throw "JDK 17 or newer is required; found $versionText"
    }
    Write-Host "Using $versionText" -ForegroundColor DarkGray
}

function Get-JavaFxLib {
    if ($env:JAVAFX_HOME) {
        $lib = Join-Path $env:JAVAFX_HOME 'lib'
        if (Test-Path $lib) {
            Write-Host "Using JAVAFX_HOME at $env:JAVAFX_HOME" -ForegroundColor DarkGray
            return (Resolve-Path $lib).Path
        }
        Write-Warning "JAVAFX_HOME is set but $lib does not exist; falling back to the local SDK."
    }

    foreach ($root in @($SdkRoot, '.javafx')) {
        $existing = Get-ChildItem -Path $root -Directory -Filter 'javafx-sdk-*' -ErrorAction SilentlyContinue |
                    Select-Object -First 1
        if ($existing) {
            return (Join-Path $existing.FullName 'lib')
        }
    }

    Write-Host "Downloading the JavaFX $JavaFxVersion SDK (about 50 MB, once) to $SdkRoot..." -ForegroundColor Cyan
    $url = "https://download2.gluonhq.com/openjfx/$JavaFxVersion/openjfx-${JavaFxVersion}_windows-x64_bin-sdk.zip"
    $zip = Join-Path $env:TEMP "openjfx-$JavaFxVersion.zip"

    $previousProgress = $ProgressPreference
    $ProgressPreference = 'SilentlyContinue'
    try {
        Invoke-WebRequest -Uri $url -OutFile $zip
    } catch {
        throw "Could not download JavaFX from $url. Download it by hand, then set JAVAFX_HOME to the extracted folder. ($_)"
    } finally {
        $ProgressPreference = $previousProgress
    }

    New-Item -ItemType Directory -Force -Path $SdkRoot | Out-Null
    Expand-Archive -Path $zip -DestinationPath $SdkRoot -Force
    Remove-Item $zip -Force -ErrorAction SilentlyContinue

    $sdk = Get-ChildItem -Path $SdkRoot -Directory -Filter 'javafx-sdk-*' | Select-Object -First 1
    if (-not $sdk) { throw "The JavaFX archive did not contain the expected javafx-sdk-* folder." }
    return (Join-Path $sdk.FullName 'lib')
}

function Build-Game([string]$FxLib) {
    if ($Clean -and (Test-Path $ClassesDir)) {
        Remove-Item -Recurse -Force $ClassesDir
    }

    $sources = Get-ChildItem -Recurse -Path 'src' -Filter '*.java'
    $newestSource = ($sources | Measure-Object LastWriteTimeUtc -Maximum).Maximum
    $marker = Join-Path $ClassesDir 'Launcher.class'
    if ((Test-Path $marker) -and (Get-Item $marker).LastWriteTimeUtc -ge $newestSource) {
        Write-Host "Sources unchanged; skipping compile." -ForegroundColor DarkGray
        return
    }

    Write-Host "Compiling..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null
    & javac -d $ClassesDir --module-path $FxLib --add-modules javafx.controls,javafx.media ($sources | ForEach-Object FullName)
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }

    # Assets travel on the classpath so they resolve the same way they do from the jar.
    Copy-Item -Recurse -Force img, dat, res $ClassesDir
}

Assert-Jdk
$fxLib = Get-JavaFxLib
Build-Game -FxLib $fxLib

Write-Host "Starting Taxi Racer..." -ForegroundColor Green
& java --module-path $fxLib --add-modules javafx.controls,javafx.media -cp $ClassesDir Launcher
