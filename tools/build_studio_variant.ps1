param(
    [int]$VersionCode = 1008,
    [string]$VersionName = '1.0.8',
    [string]$Bash = (Join-Path $env:ProgramFiles 'Git/bin/bash.exe')
)
$ErrorActionPreference = 'Stop'
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & $Bash tools/build_all_wff.sh --studio-navy $VersionCode $VersionName
    if ($LASTEXITCODE -ne 0) { throw "Studio variant build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
