$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Test-Path 'target/dependency') -or -not (Test-Path '.tools/java-home')) { & "$PSScriptRoot/setup.ps1" -SkipTraining }
$java = Join-Path (Get-Content '.tools/java-home' -Raw).Trim() 'bin/java.exe'
$opens = @('java.lang','java.lang.invoke','java.lang.reflect','java.io','java.net','java.nio','java.util','java.util.concurrent','java.util.concurrent.atomic','sun.nio.ch','sun.nio.cs','sun.security.action','sun.util.calendar') | ForEach-Object { "--add-opens=java.base/$_=ALL-UNNAMED" }
& $java '-Xmx2g' @opens '-cp' 'target/classes;target/dependency/*' 'college.bda.Main' 'train' @args
if ($LASTEXITCODE -ne 0) { throw 'Spark training failed. See the error above.' }
