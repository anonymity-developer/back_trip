param([switch]$Integration)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Import-ProjectEnv.ps1')
Import-ProjectEnv -Path (Join-Path $projectRoot '.env')

$originalUrl = $env:ENJOYTRIP_DB_URL
$originalDbTest = $env:ENJOYTRIP_DB_TEST
$originalJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    if ($Integration) {
        if ($originalUrl -notmatch '^(jdbc:mysql://[^/]+/)[A-Za-z0-9_]+(\?.*)?$') {
            throw 'Integration tests require a single-database MySQL JDBC URL.'
        }
        # Reuse the shared credentials/host/options; only destructive test data uses a separate DB.
        $env:ENJOYTRIP_DB_URL = $Matches[1] + 'enjoytrip_test' + $Matches[2]
        $env:ENJOYTRIP_DB_TEST = 'true'
    } else {
        Remove-Item Env:ENJOYTRIP_DB_TEST -ErrorAction SilentlyContinue
    }
    $env:JAVA_TOOL_OPTIONS = "-Duser.home=$env:USERPROFILE"
    & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" test
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
} finally {
    $env:ENJOYTRIP_DB_URL = $originalUrl
    $env:ENJOYTRIP_DB_TEST = $originalDbTest
    $env:JAVA_TOOL_OPTIONS = $originalJavaOptions
}
