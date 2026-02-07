# =============================================
# ChatRoom CRUD API Test with OAuth2 Token
# =============================================

# ---------- CONFIGURATION ----------
$KeycloakUrl   = "http://localhost:9090"
$realm         = "SecureChat"
$clientId      = "securechat-backend"
$clientSecret  = "b90M2LWNz5H0rUx9JTmre1JXdrxm98b5"
$username      = "novi@test.nl"
$password      = "novi123"
$apiBaseUrl    = "http://localhost:8080/api/chatrooms"

# ---------- FETCH KEYCLOAK TOKEN ----------
Write-Host "`nRequesting Keycloak token..." -ForegroundColor Cyan

$tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$realm/protocol/openid-connect/token" `
    -Method Post -Body @{
        grant_type    = "password"
        client_id     = $clientId
        client_secret = $clientSecret
        username      = $username
        password      = $password
    } -ContentType "application/x-www-form-urlencoded"

$token = $tokenResponse.access_token

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

Write-Host "Access token fetched successfully!" -ForegroundColor Green

# ---------- CRUD TEST ----------
Write-Host "`n=== CHATROOM CRUD TEST START ===" -ForegroundColor Cyan

try {
    # 1 CREATE
    $newRoom = @{
        name            = "Test ChatRoom $(Get-Date -Format 'HH:mm:ss')"
        description     = "Created via PowerShell CRUD test"
        isPrivate       = $false
        maxParticipants = 10
    } | ConvertTo-Json

    Write-Host "`nCreating a new chat room..."
    $createResponse = Invoke-RestMethod -Uri $apiBaseUrl -Method Post -Body $newRoom -Headers $headers
    $roomId = $createResponse.id
    Write-Host "  Created chat room with ID: $roomId" -ForegroundColor Green

    # 2 READ
    Write-Host "`nReading the chat room..."
    $readResponse = Invoke-RestMethod -Uri "$apiBaseUrl/$roomId" -Method Get -Headers $headers
    Write-Host "  Read chat room:" ($readResponse | ConvertTo-Json -Depth 5) -ForegroundColor Green

    # 3 UPDATE
    $updateRoom = @{
        name        = "Updated ChatRoom $(Get-Date -Format 'HH:mm:ss')"
        description = "Updated via PowerShell"
    } | ConvertTo-Json

    Write-Host "`nUpdating the chat room..."
    $updateResponse = Invoke-RestMethod -Uri "$apiBaseUrl/$roomId" -Method Put -Body $updateRoom -Headers $headers     
    Write-Host "  Updated chat room:" ($updateResponse | ConvertTo-Json -Depth 5) -ForegroundColor Green

    # 4 DELETE
    Write-Host "`nDeleting the chat room..."
    Invoke-RestMethod -Uri "$apiBaseUrl/$roomId" -Method Delete -Headers $headers
    Write-Host "  Deleted chat room with ID: $roomId" -ForegroundColor Green

} catch {
    Write-Host "`n Error during CRUD operations:" $_.Exception.Message -ForegroundColor Red
}

Write-Host "`n=== CHATROOM CRUD TEST FINISHED ===" -ForegroundColor Cyan

