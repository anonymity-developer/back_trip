$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Project .env file is missing.' }

$key = $null
foreach ($line in Get-Content -LiteralPath $envFile -Encoding utf8) {
    if ($line -match '^ENJOYTRIP_TOUR_API_KEY=(.*)$') {
        $key = $Matches[1].Trim().Trim('"').Trim("'")
        break
    }
}
if ([string]::IsNullOrWhiteSpace($key)) { throw 'ENJOYTRIP_TOUR_API_KEY is empty in .env.' }

if ($key -match '%[0-9A-Fa-f]{2}') {
    $key = [uri]::UnescapeDataString($key)
}
$encodedKey = [uri]::EscapeDataString($key)
$baseUri = 'https://apis.data.go.kr/B551011/KorService2/areaBasedList2?serviceKey=' +
    $encodedKey + '&MobileOS=ETC&MobileApp=EnjoyTrip&pageNo=1&arrange=C'
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(20)
try {
    foreach ($probe in @(
        @{ Name = 'all regions, 1 row'; Suffix = '&numOfRows=1' },
        @{ Name = 'Seoul, 1 row'; Suffix = '&areaCode=1&numOfRows=1' },
        @{ Name = 'Seoul, 1000 rows'; Suffix = '&areaCode=1&numOfRows=1000' }
    )) {
        Write-Output ("Probe: {0}" -f $probe.Name)
        try {
            $response = $client.GetAsync($baseUri + $probe.Suffix).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $resultCode = [regex]::Match($body, '<resultCode>([^<]*)</resultCode>').Groups[1].Value
            $resultMessage = [regex]::Match($body, '<resultMsg>([^<]*)</resultMsg>').Groups[1].Value
            if (-not $resultCode) {
                $resultCode = [regex]::Match($body, '<returnReasonCode>([^<]*)</returnReasonCode>').Groups[1].Value
            }
            $totalCount = [regex]::Match($body, '<totalCount>([^<]*)</totalCount>').Groups[1].Value
            $itemCount = [regex]::Matches($body, '<item>').Count
            $firstItem = [regex]::Match($body, '<item>(.*?)</item>', 'Singleline').Groups[1].Value
            $fields = @([regex]::Matches($firstItem, '<([A-Za-z][A-Za-z0-9]*)>') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
            Write-Output ("HTTP status: {0}" -f [int]$response.StatusCode)
            Write-Output ("API result code: {0}" -f $(if ($resultCode) { $resultCode } else { '(none)' }))
            Write-Output ("API result message: {0}" -f $(if ($resultMessage) { $resultMessage } else { '(none)' }))
            Write-Output ("Total count: {0}" -f $(if ($totalCount) { $totalCount } else { '(none)' }))
            Write-Output ("Items on page: {0}" -f $itemCount)
            Write-Output ("First item fields: {0}" -f $(if ($fields.Count) { $fields -join ',' } else { '(none)' }))
        } catch {
            $exception = $_.Exception.GetBaseException()
            Write-Output ("Request failed: {0}" -f $exception.GetType().Name)
        }
    }
} finally {
    $client.Dispose()
}
