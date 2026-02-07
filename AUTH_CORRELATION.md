# SecureChat Authenticatie-overzicht (OAuth 2.0, Keycloak & Postman)

Dit document biedt een technisch overzicht van hoe authenticatie en autorisatie zijn geïmplementeerd in SecureChat, met details over de interactie tussen Keycloak (Provider), de Node.js Gateway, de Java Backend en Postman.

## 1. OAuth 2.0 Architectuur & Flow

SecureChat maakt gebruik van de **OAuth 2.0 Authorization Code Flow** (met PKCE voor frontends) en **Direct Access Grants** (voor testen/scripts).

### Componenten
- **Identity Provider (IdP):** Keycloak (draait op poort `9090`).
- **Authorization Server:** Keycloak realm `SecureChat`.
- **Resource Server (Java Backend):** Poort `8081`. Valideert JWT's uitgegeven door Keycloak.
- **API Gateway (Node.js):** Poort `3000`. Fungeert als proxy en verifieert access tokens voordat verzoeken worden doorgestuurd naar de backend.
- **Client (Postman/Frontend):** Vraagt tokens aan bij Keycloak en voegt deze toe aan de `Authorization` header.

### Authenticatie Flow (Direct Access)
1. **Verzoek:** De Client stuurt een POST-verzoek met inloggegevens naar het token-endpoint van Keycloak.
2. **Token Uitgifte:** Keycloak valideert de gegevens en retourneert een **Access Token** (JWT), **Refresh Token** en **ID Token**.
3. **Gateway Verificatie:** De Client roept de Node.js Gateway (`:3000`) aan met het Bearer Token. De Gateway verifieert de JWT.
4. **Backend Validatie:** De Gateway stuurt het verzoek door naar de Java Backend (`:8081`). De Java Backend valideert onafhankelijk de handtekening van de JWT (via OIDC Discovery), de audience (`aud`) en de issuer (`iss`).
5. **Autorisatie:** De Backend haalt rollen (bijv. `ROLE_ADMIN`) op uit de `resource_access` claim van het token om beveiliging op methodeniveau af te dwingen.

---

## 2. Keycloak Configuratie Details

| Parameter | Waarde |
| :--- | :--- |
| **Auth Server URL** | `localhost:9090` |
| **Realm** | `SecureChat` |
| **Client ID** | `securechat-backend` |
| **Client Secret** | `b90M2LWNz5H0rUx9JTmre1JXdrxm98b5` |
| **Token Endpoint** | `realms/SecureChat/protocol/openid-connect/token` |
| **Issuer (iss)** | `localhost:9090/realms/SecureChat` |
| **Audience (aud)** | `securechat-backend` |

---

## 3. Postman Integratie

Postman is geconfigureerd om als een OAuth 2.0 client te fungeren. U kunt de meegeleverde `postman-config.json` gebruiken om uw omgeving in te stellen.

### Handmatige Installatiestappen
1. **Access Token Ophalen:**
   - Methode: `POST`
   - URL: `localhost:9090/realms/SecureChat/protocol/openid-connect/token`
   - Body (`x-www-form-urlencoded`):
     - `grant_type`: `password`
     - `client_id`: `securechat-backend`
     - `client_secret`: `b90M2LWNz5H0rUx9JTmre1JXdrxm98b5`
     - `username`: `novi@test.nl`
     - `password`: `novi123`
     - `scope`: `openid profile email`

2. **Het Token Gebruiken:**
   - Ga in uw API-verzoek naar het tabblad **Authorization**.
   - Selecteer **Type**: `Bearer Token`.
   - Plak de `access_token` die u in de vorige stap hebt ontvangen.

### Correlatie Logica
Het Access Token is een **JWT (JSON Web Token)**. Zowel de Node.js Gateway als de Java Backend zijn geconfigureerd om:
- De JWT te decoderen.
- Te verifiëren of deze is ondertekend door Keycloak.
- De claim `resource_access.securechat-backend.roles` te controleren om te bepalen of de gebruiker `ROLE_ADMIN` of andere permissies heeft.

---

## 4. Verificatie & Testscripts
U kunt het ingebouwde PowerShell-script gebruiken om de hele flow snel te testen:
`d:\backend\code9.6 -2 feb\java-backend\scripts\SecureChat-Auth.ps1`

Dit script haalt een token op, decodeert het om de claims te tonen en kopieert het Access Token naar uw klembord voor direct gebruik in Postman.
