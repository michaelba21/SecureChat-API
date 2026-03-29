# SecureChat API – Fix Explanation & Demo (Criterium 3.1)

**Datum**: 21 maart 2026  
**Betreft**: Herstel van 401/403 fouten door UUID-mismatch

---

## 1. Wat was het probleem?
De feedback van docent constateerde het volgende:
- Nieuwe gebruikers aanmaken via de eigen `/api/auth/register` endpoint werkte, maar het gegenereerde token gaf daarna altijd **401 Unauthorized**.
- Gebruikers aanmaken via Keycloak en daarna authenticeren werkte wél voor tokens, maar de UUID die Keycloak gebruikte verschilde van de UUID die lokaal was opgeslagen.
- Hierdoor was de gebruiker "niet erkend als lid" van een chatroom → **403 Forbidden** op vrijwel alle gerelateerde endpoints.

### Technische oorzaak
Het systeem had **twee parallelle authenticatiemethoden** die allebei een eigen UUID genereerden voor dezelfde gebruiker:

| Methode | UUID-bron | Resultaat |
|---------|-----------|-----------|
| Lokale registratie (`POST /api/auth/register`) | `UUID.randomUUID()` | Willekeurige, lokale UUID |
| Keycloak authenticatie | `jwt.getSubject()` (Keycloak's eigen UUID) | Vaste Keycloak UUID |

Wanneer een gebruiker via Keycloak inlogde, had zijn Keycloak-token een **andere UUID** dan wat er in de lokale database stond. Alle queries die de gebruiker opzochten aan de hand van zijn UUID mislukten daardoor.

---

## 2. Wat is er opgelost?

### Fix 1 – Lokale authenticatie uitgeschakeld
**Bestand**: `java-backend/src/main/java/com/securechat/controller/AuthController.java`

De lokale `login`, `register` en `refresh` endpoints retourneren nu **501 Not Implemented** met de boodschap dat authenticatie via Keycloak moet plaatsvinden. Zo bestaat er nog maar één manier om een gebruiker aan te maken: via Keycloak.

### Fix 2 – UUID altijd van Keycloak
**Bestand**: `java-backend/src/main/java/com/securechat/service/UserSyncService.java`

Bij elke geauthenticeerde request wordt de UUID uitgelezen uit de `sub` claim van het Keycloak JWT-token (`jwt.getSubject()`). Als de gebruiker nog niet in de lokale database bestaat, wordt hij aangemaakt met **exact deze UUID**. Bestaat hij al, dan wordt zijn profiel bijgewerkt. Dit garandeert dat de UUID in de database altijd gelijk is aan die in Keycloak.

### Fix 3 – Bij Spring Security wordt alleen Keycloak tokens geaccepteerd
**Bestand**: `java-backend/src/main/java/com/securechat/config/SecurityConfig.java`

De applicatie is geconfigureerd als OAuth2 Resource Server met Keycloak als enige issuer. Lokaal gegenereerde JWT tokens bestaan niet meer en worden dus ook niet geaccepteerd.

---

## 3. Demo Voorbereiding (10 Minuten)

Eerst database schoonmaken van oude data:

### Stap 1: Clean Database 
Voer het volgende uit in PostgreSQL om oude (conflicterende) data te verwijderen:
```sql
DELETE FROM chat_room_members WHERE user_id IN (SELECT id FROM users WHERE keycloak_id IS NULL);
DELETE FROM messages WHERE sender_id IN (SELECT id FROM users WHERE keycloak_id IS NULL);
DELETE FROM chat_rooms WHERE NOT EXISTS (SELECT 1 FROM chat_room_members WHERE chat_room_id = chat_rooms.id);
DELETE FROM users WHERE keycloak_id IS NULL;
ALTER TABLE users ALTER COLUMN keycloak_id SET NOT NULL;
```
**Resultaat:** Er zijn geen oude/overbodige UUIDs meer achtergebleven.

### Stap 2: Create Keycloak User 
1. Open: http://localhost:9090/admin
2. Login: `admin` / `admin`
3. Maak een gebruiker aan:
   - Username: `demo_examiner`
   - Password: `Exam2026!`
   - Keycloak genereert een UUID → `UserSyncService` synchroniseert deze hierna met de database.

---

## 4. Live Verificatie – zo werkt het nu (5 Minuten)

Tijdens de demo kunt u de volgende 4 tests uitvoeren:

### Test 1 — Lokale registratie is geblokkeerd
```bash
curl -X POST http://localhost:8080/api/auth/register
```
**Verwacht:** `501 Not Implemented`

### Test 2 — Token ophalen en ook de Gebruiker wordt gesynchroniseerd
```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/SecureChat/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=securechat-client" \
  -d "username=demo_examiner" \
  -d "password=Exam2026!" | jq -r .access_token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/auth/whoami
```
**Verwacht:** `200 OK`. De getoonde `userId` in het JSON-antwoord is identiek aan de UUID in de Keycloak admin console.

### Test 3 — Chatroom aanmaken
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"name":"Test Room"}' \
http://localhost:8080/api/chatrooms
```
**Verwacht:** `201 Created`. Bewaar de geretourneerde Room ID (bijv. 1).

### Test 4 — Member toevoegen (403 Fixed)
Gebruik de Room ID (bijv. 1) uit Test 3 en de UUID uit Test 2:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"userId":"<UUID-from-Test-2-response>"}' \
http://localhost:8080/api/chatrooms/1/members
```
**Verwacht:** `200 OK`  
*(Voorheen gaf dit altijd 403, maar doordat de UUID van Keycloak en de app nu exact matchen is autorisatie succesvol).*

---

## 5. Overzicht gewijzigde bestanden

| Bestand | Wijziging |
|---------|-----------|
| `AuthController.java` | Lokale login/register/refresh → 501 Not Implemented |
| `UserSyncService.java` | UUID altijd van Keycloak via `jwt.getSubject()` |
| `SecurityConfig.java` | Keycloak-only JWT validatie ingesteld |
| `AuthStatusController.java` | Nieuwe endpoint `/api/auth/whoami` om UUID te verifiëren |
| `UserController.java` | `/api/users/me` aangescherpt voor synchronisatie |
| `HealthController.java` & `DemoStatusController.java` | Nieuwe status checks tbv de demo |
| `OpenAPIConfig.java` | Swagger UI mapping op `/swagger-ui.html` |
| `database/CLEANUP_UUID_CONFLICTS.sql` | SQL script om oude conflicterende UUID-data te verwijderen |

---

## 6. Samenvatting voor de beoordeling

**Instructies:**

> Het initiële probleem was dat er twee authenticatiesystemen waren die beide een eigen UUID produceerden. Het Keycloak-token had een andere UUID dan we in onze database opsloegen, wat leidde tot 401 en 403 fouten bij het autoriseren van de gebruiker (bijv. als lid van een chatroom). 
> 
> Er wordt lokale authenticatie uitgeschakeld en gebruiken nu Keycloak als de enige 'Source of Truth'. De database is een gesynchroniseerde spiegel die altijd exact de Keycloak UUID gebruikt. De vier getoonde API-tests bewijzen dat gebruikers synchroniseren en de autorisatie (geen 403's meer) nu waterdicht werkt.