# GP360 Protocol Handler PowerShell Script
param(
    [string]$url
)

Write-Host "========================================"
Write-Host "GP360 Biometric Client"
Write-Host "========================================"
Write-Host ""

Write-Host "Received URL: $url"
Write-Host ""

# Parse the URL
$url = $url -replace '^gp360://', ''
$parts = $url -split '\?', 2

if ($parts.Count -lt 2) {
    Write-Host "ERROR: Invalid URL format"
    Read-Host "Press Enter to exit"
    exit 1
}

$queryString = $parts[1]

# Check if we have Base64 encoded data
if ($queryString -match '^data=(.+)$') {
    $encodedData = $Matches[1]

    # URL decode if needed
    $encodedData = $encodedData -replace '%3D', '='
    $encodedData = $encodedData -replace '%2B', '+'
    $encodedData = $encodedData -replace '%2F', '/'

    Write-Host "Decoding Base64 parameters..."

    try {
        # Decode Base64
        $jsonBytes = [System.Convert]::FromBase64String($encodedData)
        $jsonString = [System.Text.Encoding]::UTF8.GetString($jsonBytes)

        # Parse JSON
        $params = ConvertFrom-Json $jsonString

        $enrollableId = $params.id
        $enrollableType = if ($params.type) { $params.type } else { 'inmate' }
        $apiToken = $params.token
        $apiUrl = if ($params.api) { $params.api } else { 'http://localhost:8000/api' }

        # Convert type format
        if ($enrollableType -eq 'inmate') {
            $enrollableType = 'App\Models\Inmate'
        }
    }
    catch {
        Write-Host "ERROR: Failed to decode parameters: $_"

        # Fall back to traditional parsing
        $params = @{}
        foreach ($param in $queryString.Split('&')) {
            $keyValue = $param.Split('=', 2)
            if ($keyValue.Count -eq 2) {
                $key = $keyValue[0]
                $value = $keyValue[1]

                # URL decode
                $value = $value -replace '%7C', '|'
                $value = $value -replace '%3A', ':'
                $value = $value -replace '%2F', '/'
                $value = $value -replace '%20', ' '
                $value = $value -replace '\+', ' '

                $params[$key] = $value
            }
        }

        $enrollableId = $params['id']
        $enrollableType = if ($params['type']) { $params['type'] } else { 'App\Models\Inmate' }
        $apiToken = $params['token']
        $apiUrl = if ($params['api']) { $params['api'] } else { 'http://localhost:8000/api' }
    }
}
else {
    # Traditional parameter parsing
    $params = @{}
    foreach ($param in $queryString.Split('&')) {
        $keyValue = $param.Split('=', 2)
        if ($keyValue.Count -eq 2) {
            $key = $keyValue[0]
            $value = $keyValue[1]

            # URL decode
            $value = $value -replace '%7C', '|'
            $value = $value -replace '%3A', ':'
            $value = $value -replace '%2F', '/'
            $value = $value -replace '%20', ' '
            $value = $value -replace '\+', ' '

            $params[$key] = $value
        }
    }

    $enrollableId = $params['id']
    $enrollableType = if ($params['type']) { $params['type'] } else { 'App\Models\Inmate' }
    $apiToken = $params['token']
    $apiUrl = if ($params['api']) { $params['api'] } else { 'http://localhost:8000/api' }
}

Write-Host "Parsed Parameters:"
Write-Host "- ID: $enrollableId"
Write-Host "- Type: $enrollableType"
if ($apiToken) {
    $tokenDisplay = if ($apiToken.Length -gt 20) { $apiToken.Substring(0,20) + "..." } else { $apiToken }
    Write-Host "- Token: $tokenDisplay"
} else {
    Write-Host "- Token: [NOT PROVIDED]"
}
Write-Host "- API: $apiUrl"
Write-Host ""

# Check for required parameters
if (-not $enrollableId) {
    Write-Host "ERROR: No ID provided"
    Write-Host "The biometric client requires an ID parameter."
    Read-Host "Press Enter to exit"
    exit 1
}

# Change to script directory
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# Check if JAR exists
$jarPath = Join-Path $scriptPath "dist\BiometricService.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: BiometricService.jar not found"
    Write-Host "Expected location: $jarPath"
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Starting BiometricService.jar..."
Write-Host ""

# Build Java command
$javaArgs = @(
    "-cp", "dist\BiometricService.jar;lib\*",
    "-DenrollableId=$enrollableId",
    "-DenrollableType=`"$enrollableType`""
)

if ($apiToken) {
    $javaArgs += "-Dapi.token=`"$apiToken`""
}

$javaArgs += "-Dapi.url=`"$apiUrl`""
$javaArgs += "com.gp360.biometric.BiometricApplication"

# Execute Java
& java $javaArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Failed to start BiometricService"
    Write-Host "Error code: $LASTEXITCODE"
    Write-Host ""
    Write-Host "Possible causes:"
    Write-Host "- Java is not installed or not in PATH"
    Write-Host "- JAR file is corrupted"
    Write-Host "- Missing dependencies"
    Read-Host "Press Enter to exit"
}