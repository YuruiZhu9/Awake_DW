# Sync Robolectric android-all-instrumented jars from the local Maven cache
# into the flat offline directory referenced by robolectric.dependency.dir
# (see build.gradle.kts testOptions in each module).
# Run once per machine after the first successful test run, and again whenever
# a test pins a new @Config sdk version.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$dest = Join-Path $root '.robolectric/offline'
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$source = Join-Path $env:USERPROFILE '.m2/repository/org/robolectric/android-all-instrumented'
if (-not (Test-Path -LiteralPath $source)) { throw "No local cache found at $source; run the tests once with the mirror resolver first." }
Get-ChildItem -LiteralPath $source -Recurse -Filter '*.jar' | ForEach-Object {
    Copy-Item $_.FullName -Destination (Join-Path $dest $_.Name) -Force
    Write-Output ("synced " + $_.Name)
}
