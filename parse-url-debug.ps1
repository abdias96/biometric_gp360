param([string]$url)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "URL Parser Debug" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Show the raw URL received
Write-Host "Raw URL received:" -ForegroundColor Yellow
Write-Host $url
Write-Host ""

# Remove protocol prefix
$url = $url -replace '^gp360://', ''
Write-Host "After removing protocol:" -ForegroundColor Yellow
Write-Host $url
Write-Host ""

# Split at the question mark
$parts = $url -split '\?', 2
Write-Host "URL parts count: $($parts.Count)" -ForegroundColor Yellow

if ($parts.Count -lt 2) {
    Write-Host "ERROR: No query string found!" -ForegroundColor Red
    Write-Host "ID="
    Write-Host "TYPE="
    Write-Host "TOKEN="
    Write-Host "API="
    exit
}

Write-Host "Query string:" -ForegroundColor Yellow
Write-Host $parts[1]
Write-Host ""

# Parse query string manually
$queryString = $parts[1]
$params = @{}

Write-Host "Parsing parameters..." -ForegroundColor Yellow
foreach ($param in $queryString.Split('&')) {
    Write-Host "  Processing: $param" -ForegroundColor Gray

    $keyValue = $param.Split('=', 2)
    if ($keyValue.Count -eq 2) {
        $key = $keyValue[0]
        $value = $keyValue[1]

        Write-Host "    Key: $key" -ForegroundColor Gray
        Write-Host "    Raw Value: $value" -ForegroundColor Gray

        # Full URL decode using .NET
        Add-Type -AssemblyName System.Web
        $decodedValue = [System.Web.HttpUtility]::UrlDecode($value)

        Write-Host "    Decoded Value: $decodedValue" -ForegroundColor Green

        $params[$key] = $decodedValue
    }
}

Write-Host ""
Write-Host "Final parsed parameters:" -ForegroundColor Yellow
Write-Host "ID=$($params['id'])"
Write-Host "TYPE=$($params['type'])"
Write-Host "TOKEN=$($params['token'])"
Write-Host "API=$($params['api'])"

Write-Host ""
Write-Host "Token details:" -ForegroundColor Yellow
if ($params['token']) {
    Write-Host "  Length: $($params['token'].Length)" -ForegroundColor Gray
    Write-Host "  First 20 chars: $($params['token'].Substring(0, [Math]::Min(20, $params['token'].Length)))" -ForegroundColor Gray
} else {
    Write-Host "  TOKEN NOT FOUND!" -ForegroundColor Red
}