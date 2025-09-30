# Create test data
$testData = @{
    id = 23
    type = 'inmate'
    token = '1|kbS5uIs8njq55xIrxwWAM2u1XodTn2PgZVXUelD35f793084'
    api = 'http://127.0.0.1:8000/api'
}

# Convert to JSON
$json = $testData | ConvertTo-Json -Compress

Write-Host "JSON data: $json"

# Encode to Base64
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
$base64 = [Convert]::ToBase64String($bytes)

Write-Host "Base64 encoded: $base64"

# Create URL
$url = "gp360://enroll?data=$base64"

Write-Host "Full URL: $url"
Write-Host ""

# Call the handler
& "$PSScriptRoot\protocol-handler.ps1" $url