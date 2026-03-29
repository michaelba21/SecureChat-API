# =============================================
# SecureChat File API Test with Keycloak OAuth2
# =============================================

# ---------- CONFIGURATION ----------
$realm        = "SecureChat"
$clientId     = "securechat-backend"
$clientSecret = "b90M2LWNz5H0rUx9JTmre1JXdrxm98b5"
$username     = "novi@test.nl"
$password     = "novi123"

$KeycloakUrl  = "http://localhost:9090/realms/$realm/protocol/openid-connect/token"
$ApiBaseUrl   = "http://localhost:8080/api/files"

$TestFilePath       = "D:\backend\code9.6 -2 feb\java-backend\test.txt"
$DownloadedFilePath = "D:\backend\code9.6 -2 feb\java-backend\downloaded_test.txt"

# Ensure test file exists
if (-not (Test-Path $TestFilePath)) {
    Write-Host "Creating test file: $TestFilePath" -ForegroundColor Yellow
    "This is a test file for upload/download verification." | Out-File -FilePath $TestFilePath -Encoding utf8
}

# ---------- FETCH KEYCLOAK TOKEN ----------
Write-Host "`nRequesting Keycloak token..." -ForegroundColor Cyan

$tokenResponse = Invoke-RestMethod -Uri $KeycloakUrl `
    -Method Post -Body @{
        grant_type    = "password"
        client_id     = $clientId
        client_secret = $clientSecret
        username      = $username
        password      = $password
    } -ContentType "application/x-www-form-urlencoded"

$accessToken = $tokenResponse.access_token
$headers = @{
    "Authorization" = "Bearer $accessToken"
}

Write-Host "Access token fetched successfully!" -ForegroundColor Green

# ---------- HELPER FUNCTION ----------
function Decode-Jwt($token) {
    $parts = $token.Split('.')
    if ($parts.Count -lt 2) { return $null }
    $payload = $parts[1].Replace('-', '+').Replace('_', '/')
    switch ($payload.Length % 4) {
        2 { $payload += '==' }
        3 { $payload += '=' }
    }
    $bytes = [System.Convert]::FromBase64String($payload)
    $json  = [System.Text.Encoding]::UTF8.GetString($bytes)
    return $json | ConvertFrom-Json
}

# ---------- DECODE ACCESS TOKEN ----------
$decodedAccessToken = Decode-Jwt $accessToken
Write-Host "`n--- DECODED ACCESS TOKEN ---"
$decodedAccessToken | ConvertTo-Json -Depth 10

# ---------- FILE UPLOAD ----------
Write-Host "`nUploading test file..."
try {
    # Using curl.exe for compatibility with Windows PowerShell 5.1
    $uploadResponseJson = curl.exe -s -X POST "$ApiBaseUrl/upload" `
        -H "Authorization: Bearer $accessToken" `
        -F "file=@$TestFilePath" 
    
    $uploadResponse = $uploadResponseJson | ConvertFrom-Json
    $FileId = $uploadResponse.id
    if (-not $FileId) { throw "No File ID returned: $uploadResponseJson" }
    Write-Host "Upload successful! File ID: $FileId" -ForegroundColor Green
} catch {
    Write-Host "Error uploading file: $_" -ForegroundColor Red
    exit
}

# ---------- FILE DOWNLOAD ----------
Write-Host "`nDownloading test file..."
try {
    Invoke-RestMethod -Uri "$ApiBaseUrl/download/$FileId" `
        -Method Get -Headers $headers -OutFile $DownloadedFilePath
    Write-Host "File downloaded to $DownloadedFilePath" -ForegroundColor Green
} catch {
    Write-Host "Error downloading file: $_" -ForegroundColor Red
    exit
}

# ---------- FILE INTEGRITY CHECK ----------
if ((Get-FileHash $TestFilePath).Hash -eq (Get-FileHash $DownloadedFilePath).Hash) {
    Write-Host "`nFile integrity check passed! " -ForegroundColor Green
} else {
    Write-Host "`nFile integrity check failed! " -ForegroundColor Red
}
