param([string]$url)

# Remove protocol prefix
$url = $url -replace '^gp360://', ''

# Split at the question mark
$parts = $url -split '\?', 2
if ($parts.Count -lt 2) {
    Write-Host "ID="
    Write-Host "TYPE="
    Write-Host "TOKEN="
    Write-Host "API="
    exit
}

# Parse query string manually
$queryString = $parts[1]
$params = @{}

foreach ($param in $queryString.Split('&')) {
    $keyValue = $param.Split('=', 2)
    if ($keyValue.Count -eq 2) {
        $key = $keyValue[0]
        $value = $keyValue[1]

        # Full URL decode using .NET
        Add-Type -AssemblyName System.Web
        $value = [System.Web.HttpUtility]::UrlDecode($value)

        $params[$key] = $value
    }
}

# Output parameters
Write-Host "ID=$($params['id'])"
Write-Host "TYPE=$($params['type'])"
Write-Host "TOKEN=$($params['token'])"
Write-Host "SESSION=$($params['session'])"
Write-Host "API=$($params['api'])"