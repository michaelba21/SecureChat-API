# SecureChat API - Hybride Backend Chat Applicatie

## Inhoudsopgave

- [A. Over deze app](#a-over-deze-app)
- [B. Belangrijkste Functionaliteiten](#b-belangrijkste-functionaliteiten)
- [C. Technische specificaties & vereisten](#c-technische-specificaties--vereisten)
  - [a. Technische Stack](#a-technische-stack)
  - [b. Systeemvereisten](#b-systeemvereisten)
  - [c. Pre-requisites Checklist](#c-pre-requisites-checklist)
- [D. Architectuur](#d-architectuur)
- [E. Authenticatie en beveiliging (OAuth2 + Keycloak)](#e-authenticatie-en-beveiliging-oauth2--keycloak)
- [F. API Endpoints](#f-api-endpoints)
  - [a. Overzicht](#a-overzicht)
  - [b. Voorbeelden](#b-voorbeelden)
- [G. API Documentatie & Testing](#g-api-documentatie--testing)
  - [a. Testing Tools](#a-testing-tools)
  - [b. API Health Endpoints](#b-api-health-endpoints)
  - [c. Beschikbare Scripts](#c-beschikbare-scripts)
- [H. Installatie en opstarten](#h-installatie-en-opstarten)
  - [a. Stap-voor-stap installatie](#a-stap-voor-stap-installatie)
  - [b. Environment configuratie](#b-environment-configuratie)
  - [c. Keycloak Realm Import](#c-keycloak-realm-import)
  - [d. Opstartopties](#d-opstartopties)
- [I. Automatisch Testen](#i-automatisch-testen)
- [J. Foutafhandeling en Troubleshooting](#j-foutafhandeling-en-troubleshooting)
- [K. Bijdragen aan het project](#k-bijdragen-aan-het-project)
- [L. Licentie](#l-licentie)

---

## A. Over deze app
De SecureChat API vormt het digitale hart van een moderne chat-app. We gebruiken een hybride architectuur met Keycloak voor geavanceerde authenticatie en autorisatie. Het resultaat is een veilig platform voor gesprekken, bestanden en gebruikers, waarvan elke handeling netjes wordt bijgehouden.

De applicatie brengt geavanceerde concepten tot leven, zoals OAuth2/OpenID Connect authenticatie, Role-Based Access Control (RBAC), en real-time communicatie tussen losse services.

## B. Belangrijkste Functionaliteiten (ref.1)
| Categorie | Functionaliteit | Beschrijving |
| :--- | :--- | :--- |
| **Authenticatie** | OAuth2 + OpenID Connect | Geïntegreerd met Keycloak voor enterprise-grade authenticatie |
| **Autorisatie** | RBAC met Keycloak | Role-Based Access Control beheerd via Keycloak realms en clients |
| **Chat** | Chatroom Beheer | Aanmaken, beheren en beveiligen van publieke en privé chatrooms |
| **Real-time** | HTTP Polling | Near-realtime updates voor berichten via efficiënte polling mechanismen |
| **Bestanden** | Secure File Sharing | Uploaden en delen van bestanden met strikte toegangscontrole |
| **Audit** | Soft-delete & Logging | Uitgebreide audit trails voor verwijderde items en admin-acties ("blame") |
| **Beveiliging** | Rate Limiting & CORS | Bescherming tegen misbruik |
| **Moderatie** | Admin Tools | Functionaliteit voor het bannen/kicken van gebruikers en moderatie-logs |

## C. Technische specificaties & vereisten

### a. Technische Stack (ref.2,3,4,5 en 6)
| Categorie | Technologie | Versie | Doel |
| :--- | :--- | :--- | :--- |
| **Backend** | Java (Spring Boot) | 17+ | Business Logic, Validatie |
| **Identity Provider** | Keycloak | 26.5+ | OAuth2/OpenID Connect SSO |
| **Database** | PostgreSQL | 15+ | Data opslag & Relaties |
| **Build Tool** | Maven | 3.6+ | Java dependency management |
| **Scripting** | PowerShell | 5.1+ | Automatisatie & testing |
| **API Testing** | cURL/Invoke-RestMethod | - | API tests en integratie |
| **Integration Testing** | Testcontainers | - | Geïsoleerde database tests |

### b. Systeemvereisten
| Type | Vereiste | Details |
| :--- | :--- | :--- |
| **Software** | Java JDK | v17 of hoger (Eclipse Adoptium/Oracle/OpenJDK) |
| **Software** | Keycloak | v26.5 of hoger (local standalone) |
| **Software** | PostgreSQL | v15 of hoger (of Docker container) |
| **Software** | Maven | v3.6 of hoger |
| **Software** | PowerShell | v5.1 of hoger (Windows) |
| **Software** | Docker | v20+ (vereist voor integratietests) |
| **Netwerk** | Poorten | 8080 (Backend), 9090 (Keycloak), 5432 (DB) vrij |

### c. Pre-requisites Checklist

**Voor je begint:**
- [ ] Docker geïnstalleerd en running (voor integratietests)
- [ ] PostgreSQL 15+ geïnstalleerd (of gebruik Docker container)
- [ ] Java JDK 17+ geïnstalleerd
- [ ] Maven 3.6+ geïnstalleerd
- [ ] Keycloak 26.5+ gedownload en uitgepakt
- [ ] Poorten 8080, 9090, 5432 beschikbaar

## D. Architectuur
De applicatie hanteert een gedecentraliseerde architectuur met Keycloak als identity provider:

| Component | Poort | Verantwoordelijkheid |
| :--- | :--- | :--- |
| **Java Backend** | 8080 | Core logic, Database interacties, Validatie, File I/O. |
| **Keycloak Server** | 9090 | OAuth2/OpenID Connect authenticatie, gebruikersbeheer. |
| **PostgreSQL** | 5432 | Persistente data opslag. |

**Request Flow:**
1. Client haalt OAuth2 token op via Keycloak (9090).
2. Client stuurt request naar Java Backend (8080) with Bearer token.
3. Java Backend valideert token via Keycloak en verwerkt de logica.
4. Java Backend spreekt de Database aan voor persistente opslag.

## E. Authenticatie en beveiliging (OAuth2 + Keycloak)
De beveiliging is geïmplementeerd met Keycloak als centrale identity provider:

- **Authenticatie**: OAuth2 Password Grant flow (voor testen) + OpenID Connect.
- **Tokens**:
    - **Access Token**: JWT tokens uitgegeven door Keycloak met ingebouwde roles.
    - **Refresh Token**: Voor sessie verlenging.
    - **ID Token**: Voor gebruikersinformatie (indien geconfigureerd).
- **Roles**: Beheerd in Keycloak (bijv. `ROLE_ADMIN`, `ROLE_USER`).
- **Clients**: `securechat-backend` client geconfigureerd met client secret.


> **Security Note:** Het client secret `b90M2LWNz5H0rUx9JTmre1JXdrxm98b5` is alleen voor ontwikkeling.
> Voor productie:
> 1. Genereer een nieuw random secret in Keycloak Admin Console.
> 2. Update zowel Keycloak client configuratie als je applicatie configuratie.
> 3. Gebruik environment variables of secrets management.

**Java Backend Configuratie (`application.properties`):**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=localhost:9090/realms/SecureChat
```

**JSON Configuratiebestanden:**
De applicatie bevat JSON configuratiebestanden in `resources/json/`:
- `master-realm.json`: Master realm configuratie voor Keycloak.
- `SecureChat-realm.json`: SecureChat realm configuratie.
- `SecureChat API - JPA Relations.properties`: JPA entity relaties documentatie.

**Logboekconfiguratie:**
- `logback-spring.xml`: Configureert log levels en output voor development en productie.

## F. API Endpoints
### a. Overzicht (ref.7)
Base URL: `localhost:8080/api`

| Categorie | Method | Endpoint | Beschrijving | Auth |
| :--- | :--- | :--- | :--- | :--- |
| **Chat Rooms** | GET | `/chatrooms` | Alle rooms ophalen | Bearer |
| | POST | `/chatrooms` | Nieuwe room maken | Bearer |
| | GET | `/chatrooms/:id` | Specifieke room | Bearer |
| | PUT | `/chatrooms/:id` | Room updaten | Bearer |
| | DELETE | `/chatrooms/:id` | Room verwijderen | Bearer |

### b. Voorbeelden (ref.8,9 en 10)
**OAuth2 Token ophalen (PowerShell):**
```powershell
$tokenResponse = Invoke-RestMethod -Uri "localhost:9090/realms/SecureChat/protocol/openid-connect/token" `
    -Method Post -Body @{
        grant_type    = "password"
        client_id    = "securechat-backend"
        client_secret = "b90M2LWNz5H0rUx9JTmre1JXdrxm98b5"
        username      = "novi@test.nl"
        password      = "novi123"
    } -ContentType "application/x-www-form-urlencoded"
```

**Chatroom aanmaken:**
```bash
curl -X POST localhost:8080/api/chatrooms \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Room",
    "description": "Mijn test chatroom",
    "isPrivate": false,
    "maxParticipants": 10
  }'
```

## G. API Documentatie & Testing
### a. Testing Tools
- **Postman/Insomnia**: Voor GUI-based API testing met OAuth2 support.
- **PowerShell**: Voor geautomatiseerde testing (zie beschikbare scripts).
- **Keycloak Admin Console**: Voor gebruikers- en role management (op localhost:9090).

### b. API Health Endpoints
- **Backend Status**: `GET localhost:8080/actuator/health`
- **Database Status**: `GET localhost:8080/actuator/health/db`

### c. Beschikbare Scripts
- `SecureChat-Auth.ps1`: Haalt OAuth2 tokens op en decodeert JWT.
- `chatroom-crud-test.ps1`: Volledige CRUD test voor chatrooms.
- `build.ps1`: Build script voor Java backend.
- `run.ps1`: Start script voor Java backend.
- `start-keycloak.bat`: Start script voor Keycloak server.

## H. Installatie en opstarten (ref.11)
### a. Stap-voor-stap installatie
| Stap | Actie | Commando / Details |
| :--- | :--- | :--- |
| 1 | Repository klonen | `git clone https://github.com/michaelba21/SecureChat-API.git` |
| 2 | Database aanmaken | `psql -U postgres -c "CREATE DATABASE securechat_db;"` |
| 3 | Keycloak downloaden | Download van keycloak.org/downloads |
| 4 | Keycloak extracten | Unzip naar bijv. `D:\backend\keycloak-26.5.1\` |
| 5 | Keycloak configureren | Importeer realm uit `config/keycloak-realm-export.json` of `resources/json/SecureChat-realm.json` |
| 6 | Java Backend Builden | `cd java-backend && mvn clean package` of gebruik `build.ps1` |

### b. Environment configuratie
**Java Backend** (`java-backend/src/main/resources/application.properties`):
```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/securechat_db
spring.datasource.username=postgres
spring.datasource.password=jouw-wachtwoord
spring.security.oauth2.resourceserver.jwt.issuer-uri=localhost:9090/realms/SecureChat
```

### c. Keycloak Realm Import
Importeer de realm configuratie via Keycloak Admin Console:
1. Start Keycloak: `.\start-keycloak.bat`
2. Open op localhost:9090
3. Login als admin user.
4. Ga naar **Realm Settings** → **Partial Import**.
5. Selecteer `config/keycloak-realm-export.json` of `resources/json/SecureChat-realm.json`.
6. Enable **Users**, **Clients**, **Roles**, en **Groups**.
7. Klik **Import**.

### d. Opstartopties
**Optie 1: PowerShell Scripts (Windows)**
```powershell
# Start Keycloak (aparte terminal)
.\start-keycloak.bat

# Start Java Backend (aparte terminal)
.\run.ps1
```

**Optie 2: Handmatig**
Terminal 1 (Keycloak):
```bash
cd D:\backend\keycloak-26.5.1\bin
kc.bat start-dev --http-port=9090
```
Terminal 2 (Java):
```bash
cd java-backend
mvn spring-boot:run
```

## I. Automatisch Testen
### PowerShell Test Scripts
**Token Fetch & Decode:**
```powershell
.\SecureChat-Auth.ps1
```
Test OAuth2 flow en decodeert JWT tokens voor role verificatie. Kopieert automatisch het access token naar het clipboard voor gebruik in Postman.
*Output:* Toont gedecodeerde JWT tokens met roles en gebruikersinformatie.

**ChatRoom CRUD Test:**
```powershell
.\chatroom-crud-test.ps1
```
Voert volledige CRUD operaties uit op chatrooms:
1. Haalt OAuth2 token op via Keycloak
2. Maakt een nieuwe chatroom aan
3. Leest de chatroom details
4. Werkt de chatroom bij
5. Verwijdert de chatroom
*Output:* Toont alle CRUD operaties in kleurgecodeerde console output met success/failure indicatoren.

### Build en Run Scripts
- **Build Script (`.\build.ps1`)**: Configureert `JAVA_HOME` en voert `Maven clean package` uit.
- **Run Script (`.\run.ps1`)**: Start de Java Spring Boot applicatie met geconfigureerde JDK.

### Integratietests (Testcontainers)
De Java backend bevat geavanceerde integratietests die gebruik maken van **Testcontainers** met PostgreSQL. Deze tests starten een echte PostgreSQL Docker container voor realistische testing.

```bash
# Run een enkele integratietest
mvn -Dtest=com.securechat.integration.ChatRoomControllerIT test

# Run alle integratietests
mvn test -P integrationtest

# Run alle tests (unit + integratie)
mvn test
```

**Wat wordt getest:**
-  **Autorisatie**: Alleen de creator van een chatroom kan deze updaten.
-  **Veiligheid**: Non-creators krijgen 403 Forbidden bij update pogingen.
-  **Robuste JSON parsing**: Correcte afhandeling van edge cases.
-  **Database filtering**: GET endpoints retourneren alleen relevante data per gebruiker.

**Vereisten voor integratietests:**
- Docker moet draaien (`docker --version` om te controleren).
- Genoeg RAM beschikbaar voor PostgreSQL container.
- Testcontainers gebruikt PostgreSQL 15 Alpine image.

## J. Foutafhandeling en Troubleshooting
| Probleem | Oorzaak | Oplossing |
| :--- | :--- | :--- |
| **Keycloak niet bereikbaar** | Poort 9090 bezet of service niet gestart | Start Keycloak via `start-keycloak.bat` |
| **Invalid Token** | Token expired of secret mismatch | Haal nieuw token op via `SecureChat-Auth.ps1` |
| **Role niet gevonden** | Keycloak role niet geconfigureerd | Voeg role toe in Keycloak Admin Console |
| **Database Connection** | PostgreSQL niet gestart | Start PostgreSQL service |
| **Port Conflict** | Poort 8080 bezet | Wijzig `server.port` in `application.properties` |
| **JAVA_HOME niet gevonden** | JDK niet geïnstalleerd of PATH niet correct | Pas `build.ps1` en `run.ps1` aan met correct JDK pad |
| **Docker niet gevonden** | Docker niet geïnstalleerd of niet running | Installeer Docker en start de Docker Desktop service |
| **JSON configuratie fout** | JSON bestanden corrupt of incomplete | Gebruik `config/keycloak-realm-export.json` als backup |

**Veelvoorkomende Keycloak Issues:**
- Client niet geconfigureerd met correcte redirect URIs.
- Client secret mismatch tussen configuraties.
- User heeft geen roles toegewezen in Keycloak.
- Direct Access Grants niet ingeschakeld voor client.

**Script-specifieke problemen:**
- PowerShell Execution Policy kan scripts blokkeren - voer uit als admin: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
- Integratietests falen zonder Docker - installeer Docker Desktop en start de service.

## K. Bijdragen aan het project
| Stap | Actie |
| :--- | :--- |
| 1 | Fork deze repository |
| 2 | Maak een feature branch: `git checkout -b feature/naam` |
| 3 | Commit wijzigingen: `git commit -m "Beschrijving"` |
| 4 | Push naar fork en open Pull Request |

**Belangrijke zaken voor contributors:**
- Test alle wijzigingen met de beschikbare PowerShell scripts.
- Zorg dat Keycloak configuratie up-to-date blijft in JSON bestanden.
- Documenteer nieuwe endpoints of wijzigingen in authenticatie flow.
- Zorg dat integratietests blijven werken (vereist Docker).
- Behoud compatibiliteit met bestaande JSON configuraties.

## L. Licentie
Dit project is ontwikkeld voor educatieve doeleinden. Zie het `LICENSE` bestand (MIT) voor meer informatie.