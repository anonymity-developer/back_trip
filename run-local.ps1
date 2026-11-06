param(
    [string]$TomcatHome = 'C:\tools\app\apache-tomcat-11.0.26',
    [ValidateRange(1, 65535)][int]$Port = 8080,
    [string]$EnvFile = '.env'
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$envFile = if ([System.IO.Path]::IsPathRooted($EnvFile)) { $EnvFile } else { Join-Path $projectRoot $EnvFile }
$warFile = Join-Path $projectRoot 'target\enjoytrip.war'
$tomcatBase = Join-Path $projectRoot "target\local-tomcat-$Port"

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing .env file: $envFile"
}
if (-not (Test-Path -LiteralPath (Join-Path $TomcatHome 'bin\catalina.bat'))) {
    throw "Tomcat 11 was not found at $TomcatHome"
}
$portProbe = [System.Net.Sockets.TcpClient]::new()
try {
    $portProbe.Connect('127.0.0.1', $Port)
    throw "Port $Port is already in use. Stop the existing server or choose another port."
} catch [System.Net.Sockets.SocketException] {
    # No server is listening yet.
} finally {
    $portProbe.Dispose()
}

foreach ($line in Get-Content -LiteralPath $envFile -Encoding utf8) {
    $entry = $line.Trim()
    if ($entry.Length -eq 0 -or $entry.StartsWith('#')) { continue }
    if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        throw 'Invalid .env line. Expected NAME=value.'
    }
    $name = $Matches[1]
    $value = $Matches[2].Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

foreach ($name in @('ENJOYTRIP_DB_URL', 'ENJOYTRIP_DB_USER', 'ENJOYTRIP_DB_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        throw "Missing required setting in .env: $name"
    }
}
foreach ($name in @('ENJOYTRIP_DB_PASSWORD', 'ENJOYTRIP_ADMIN_PASSWORD')) {
    if ([Environment]::GetEnvironmentVariable($name, 'Process') -eq 'replace-me') {
        throw "Replace the example value for $name in .env with a real password."
    }
}

$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = "-Duser.home=$env:USERPROFILE"
    $mavenRepo = Join-Path $env:USERPROFILE '.m2\repository'
    & (Join-Path $projectRoot 'mvnw.cmd') "-Dmaven.repo.local=$mavenRepo" package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
}
if (-not (Test-Path -LiteralPath $warFile)) {
    throw 'Maven completed without producing target\enjoytrip.war.'
}

foreach ($directory in @($tomcatBase, (Join-Path $tomcatBase 'webapps'), (Join-Path $tomcatBase 'logs'), (Join-Path $tomcatBase 'temp'), (Join-Path $tomcatBase 'work'))) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}
Copy-Item -Recurse -Force -LiteralPath (Join-Path $TomcatHome 'conf') -Destination $tomcatBase
$serverXml = Join-Path $tomcatBase 'conf\server.xml'
$serverConfig = [System.IO.File]::ReadAllText($serverXml)
$serverConfig = $serverConfig.Replace('<Server port="8005"', '<Server port="-1"')
$serverConfig = $serverConfig.Replace('<Connector port="8080"', "<Connector port=`"$Port`"")
[System.IO.File]::WriteAllText($serverXml, $serverConfig, [System.Text.UTF8Encoding]::new($false))
$expandedApp = [System.IO.Path]::GetFullPath((Join-Path $tomcatBase 'webapps\enjoytrip'))
$expectedApp = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "target\local-tomcat-$Port\webapps\enjoytrip"))
if ($expandedApp -ne $expectedApp) {
    throw 'Unexpected Tomcat deployment directory.'
}
if (Test-Path -LiteralPath $expandedApp) {
    Remove-Item -LiteralPath $expandedApp -Recurse -Force
}
Copy-Item -Force -LiteralPath $warFile -Destination (Join-Path $tomcatBase 'webapps\enjoytrip.war')

$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $tomcatBase
Write-Host "Starting EnjoyTrip at http://localhost:$Port/enjoytrip/"
Write-Host 'Press Ctrl+C to stop Tomcat.'
& (Join-Path $TomcatHome 'bin\catalina.bat') run
