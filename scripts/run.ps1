$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Test-Path '.tools/java-home') -or -not (Test-Path 'dist/movie-recommender.jar') -or -not (Test-Path 'artifacts/model.json')) { & "$PSScriptRoot/setup.ps1" }
$java = Join-Path (Get-Content '.tools/java-home' -Raw).Trim() 'bin/java.exe'
if (-not (Test-Path $java)) { & "$PSScriptRoot/setup.ps1"; $java = Join-Path (Get-Content '.tools/java-home' -Raw).Trim() 'bin/java.exe' }
& $java '-jar' 'dist/movie-recommender.jar' 'serve' @args
exit $LASTEXITCODE
