param([switch]$SkipTraining)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Find-Jdk17 {
    $candidates = @($env:JAVA_HOME, 'C:\Program Files\Java\jdk-17')
    $candidates += @(Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path "$candidate\bin\javac.exe")) {
            $version = & "$candidate\bin\javac.exe" -version 2>&1
            if ("$version" -match '^javac 17\.') { return $candidate }
        }
    }
    $command = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($command -and ((& $command.Source -version 2>&1) -match '^javac 17\.')) { return (Split-Path (Split-Path $command.Source -Parent) -Parent) }
    return $null
}
$jdk = Find-Jdk17
if (-not $jdk) {
    if (-not (Get-Command winget.exe -ErrorAction SilentlyContinue)) { throw 'JDK 17 is missing. Install it, set JAVA_HOME and rerun setup.cmd.' }
    & winget install --id EclipseAdoptium.Temurin.17.JDK --exact --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { throw 'JDK installation failed.' }
    $jdk = Find-Jdk17
}
if (-not $jdk) { throw 'Could not locate JDK 17 after installation. Set JAVA_HOME and rerun.' }
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
New-Item -ItemType Directory -Force '.tools' | Out-Null
Set-Content '.tools\java-home' $jdk
$maven = Join-Path (Get-Location) '.tools\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path $maven)) {
    Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile '.tools\maven.zip'
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead((Join-Path (Get-Location) '.tools\maven.zip'))
    try { $hash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() } finally { $stream.Dispose(); $sha.Dispose() }
    if ($hash -ne '0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb') { throw 'Maven checksum mismatch.' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory((Join-Path (Get-Location) '.tools\maven.zip'), (Join-Path (Get-Location) '.tools'))
}
& $maven -B -ntp package dependency:copy-dependencies '-DincludeScope=compile' '-DoutputDirectory=target/dependency'
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
New-Item -ItemType Directory -Force 'dist' | Out-Null
Copy-Item 'target\movie-recommender.jar' 'dist\movie-recommender.jar' -Force
if (-not (Test-Path 'data\ml-latest-small\ratings.csv')) {
    # PowerShell uses the Windows certificate store for the official HTTPS download.
    New-Item -ItemType Directory -Force 'data' | Out-Null
    Invoke-WebRequest 'https://files.grouplens.org/datasets/movielens/ml-latest-small.zip' -OutFile 'data\movielens-download.zip'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory((Join-Path (Get-Location) 'data\movielens-download.zip'), (Join-Path (Get-Location) 'data'))
    Remove-Item -LiteralPath 'data\movielens-download.zip'
}
if (-not $SkipTraining -and -not (Test-Path 'artifacts\model.json')) { & "$PSScriptRoot\train.ps1" }
Write-Host 'Setup complete. Run run.cmd and open http://127.0.0.1:8080'
