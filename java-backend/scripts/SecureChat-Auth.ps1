# ============================
# SecureChat Keycloak OAuth2 Token Fetch & Role Check (PowerShell)
# ============================

# --------- CONFIGURATION ---------
$realm        = "SecureChat"
$clientId     = "securechat-backend"
$clientSecret = "b90M2LWNz5H0rUx9JTmre1JXdrxm98b5"
$username     = "novi@test.nl"
$password     = "novi123"
$clientName   = "securechat-backend"  # for resource_access

$keycloakUrl  = "http://localhost:9090/realms/$realm/protocol/openid-connect/token"

# --------- REQUEST TOKENS ---------
Write-Host "Requesting tokens from Keycloak..."
$response = curl.exe -s -X POST $keycloakUrl `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=password" `
    -d "client_id=$clientId" `
    -d "client_secret=$clientSecret" `
    -d "username=$username" `
    -d "password=$password" `
    -d "scope=openid profile email" | ConvertFrom-Json

$accessToken  = $response.access_token
$refreshToken = $response.refresh_token
$idToken      = $response.id_token

Write-Host "`n--- TOKENS ---"
Write-Host "Access Token : $accessToken"
Write-Host "Refresh Token: $refreshToken"
Write-Host "ID Token     : $idToken"

# --------- FUNCTION TO DECODE JWT ---------
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

# --------- DECODE TOKENS ---------
$decodedAccessToken = Decode-Jwt $accessToken
$decodedIdToken     = Decode-Jwt $idToken

Write-Host "`n--- DECODED ACCESS TOKEN ---"
$decodedAccessToken | ConvertTo-Json -Depth 10

if ($decodedIdToken) {
    Write-Host "`n--- DECODED ID TOKEN ---"
    $decodedIdToken | ConvertTo-Json -Depth 10
} else {
    Write-Host "`nID Token not issued. Check Keycloak client settings (Standard Flow + Direct Access Grants enabled)."
}

# --------- CHECK ROLE IN ACCESS TOKEN ---------
$clientRoles = $decodedAccessToken.resource_access.$clientName.roles
if ($clientRoles -contains "ROLE_ADMIN") {
    Write-Host "`nUser has ROLE_ADMIN "
} else {
    Write-Host "`nUser does NOT have ROLE_ADMIN "
}

# --------- POSTMAN READY ---------
Write-Host "`n--- POSTMAN USAGE ---"
Write-Host "Set Authorization -> Bearer Token -> $accessToken"

# Optional: automatically copy access token to clipboard
$accessToken | Set-Clipboard
Write-Host "`nAccess token copied to clipboard! You can paste it in Postman."
