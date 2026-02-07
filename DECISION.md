## DECISION.md - SecureChat API Architectuurbeslissingen
## Project: SecureChat API - Hybride Backend Chat Applicatie
## Auteur: Michael Barak
## Laatst Bijgewerkt: Januari 2026

________________________________________
# Inhoudsopgave
1.	Overzicht
2.	Snelle Referentietabel
3.	Architectuurbeslissingen
4.	Beveiligingsbeslissingen
5.	Databasebeslissingen
6.	Implementatiebeslissingen
7.	Implementatie-evolutie
8.	Bekende Beperkingen
9.	Toekomstige Roadmap
________________________________________
# Overzicht
Dit document houdt alle significante architecturale en technische beslissingen bij die zijn genomen tijdens de ontwikkeling van de SecureChat API. Elke beslissing bevat:
•	Context: Waarom de beslissing nodig was
•	Beslissing: Wat er gekozen is
•	Alternatieven: Wat er verder overwogen is
•	Rationale: Waarom deze keuze is gemaakt
•	Consequenties: Impact op de codebase
•	Status: Actueel, Planbaar, of Vervangen
________________________________________
# Tabel 1-Snelle Referentietabel (ref.nr.8)

| ID | Categorie | Onderwerp | Status | Datum | Impact |
|:---|:---|:---|:---|:---|:---|
| AB-001 | Architectuur | Hybride Architectuur (Node.js + Java) | Actief geïmplementeerd | Nov 2025 | Hoog |
| AB-002 | Architectuur | JWT Verificatie in Gateway | Actief geïmplementeerd | Nov 2025 | Hoog |
| AB-003 | Architectuur | DTO-Conversie in Controller | Actief geïmplementeerd | Nov 2025 | Gemiddeld |
| BB-001 | Beveiliging | Database-Backed Refresh Tokens | Actief geïmplementeerd | Nov 2025 | Hoog |
| BB-002 | Beveiliging | BCrypt Wachtwoord Hashing | Actief geïmplementeerd | Nov 2025 | Hoog |
| BB-003 | Beveiliging | Soft Delete voor Audit Trail | Actief geïmplementeerd | Jan 2025 | Gemiddeld |
| DB-001 | Database | UUID als Primaire Sleutel | Actief geïmplementeerd | Nov 2025 | Hoog |
| DB-002 | Database | ChatRoomMember Join-Tabel | Actief geïmplementeerd | Nov 2025 | Hoog |
| IB-001 | Implementatie | Lokale Bestandsopslag (met migratiepad) | Actief geïmplementeerd | Nov 2025 | Gemiddeld |
| EV-001 | Evolutie | HTTP Polling → SSE | Actief geïmplementeerd | Jan 2025 | Hoog |
| EV-002 | Evolutie | Byte Array → Streaming Downloads | Actief geïmplementeerd | Jan 2025 | Gemiddeld |
| BL-001 | Beperking | Granulaire Rate-Limiting | Niet actief - planning nodig | Toekomst | Hoog |

- Legenda:
•	Actief geïmplementeerd: Volledig geïmplementeerd en operationeel in codebase
•	Niet actief - planning nodig: Bekend probleem, oplossing vereist planning en implementatie
•	Vervangen door nieuwe oplossing: Niet meer actief, vervangen door nieuwe beslissing
________________________________________
# Architectuurbeslissingen
AB-001: Hybride Architectuur (Node.js Gateway + Java Backend)
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Hoog
Context:
Behoefte aan een schaalbare, onderhoudbare backend-architectuur die zorgen scheidt en toekomstige microservices-uitbreiding ondersteunt.
Beslissing:
Implementeer een hybride architectuur:
•	Node.js Express als API Gateway (poort 3000)
•	Java Spring Boot als Business Logic Layer (poort 8080)
•	PostgreSQL als persistente dataopslag (poort 5432)
Overwogen Alternatieven:
1.	Monolithische Java-only applicatie
2.	Volledige microservices met service mesh
3.	Node.js-only met Sequelize ORM
Rationale:
•	Duidelijke scheiding van verantwoordelijkheden (authenticatie vs business logic)
•	Gateway fungeert als enkele beveiligingsgrens
•	Benut Node.js prestaties voor I/O-operaties
•	Java's robuustheid voor complexe business logic
•	Educatieve waarde in begrip van gedistribueerde systemen
Consequenties:
•	Positief: Gecentraliseerde authenticatie in gateway
•	Positief: Eenvoudig nieuwe backend-services toevoegen
•	Overweging: Extra complexiteit in inter-service communicatie
•	Overweging: Vereist intern API-contractbeheer
Code Referenties:
•	nodejs-gateway/index.js (regels 55-73: Proxy configuratie)
•	Gateway stuurt requests door met X-User-Id en X-User-Roles headers (index.js:61-69)
________________________________________
AB-002: JWT Verificatie Alleen in Gateway
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Hoog
Context:
Token-verificatie kan plaatsvinden op gateway-niveau, backend-niveau, of beide. Moet beslissen waar authenticatielogica zich bevindt.
Beslissing:
Alle JWT-verificatie (access + refresh tokens) gebeurt uitsluitend in Node.js gateway. Java backend vertrouwt interne headers van gateway.
Overwogen Alternatieven:
1.	Tokens verifiëren in zowel gateway als Java (dubbele verificatie)
2.	Alleen in Java verifiëren met Spring Security JWT
3.	Intern JWT tussen gateway en backend gebruiken
Rationale:
•	DRY-principe: Enkel punt van authenticatielogica
•	Prestaties: Vermijd redundante CPU-intensieve cryptografische operaties
•	Scheiding van zorgen: Gateway handelt auth af, Java handelt business logic af
•	Toekomstbestendig: Eenvoudig Python/Go services toevoegen zonder auth te dupliceren
Consequenties:
•	Positief: Gecentraliseerde auth vereenvoudigt audits
•	Positief: 100ms snellere request-verwerking (geen dubbele verificatie)
•	Overweging: Gateway is single point of failure voor authenticatie
•	Overweging: Interne headers kunnen worden vervalst door kwaadwillende interne services
Beveiligingsoverweging:
Voor productie, overweeg interne requests te signeren met een intern JWT om header-spoofing te voorkomen.
Code Referenties:
•	nodejs-gateway/index.js (regels 28-47: JWT-verificatie middleware)
•	nodejs-gateway/index.js (regels 61-69: header-forwarding)
________________________________________
AB-003: DTO-Conversie in Controller-Laag
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Gemiddeld
Context:
Entity-naar-DTO conversie kan plaatsvinden in Controller, Service, of via automatische mappers. Moet laagverantwoordelijkheid beslissen.
Beslissing:
DTO-conversie gebeurt in Controller-laag met behulp van toegewijde Mapper-klassen.
Overwogen Alternatieven:
1.	Service-laag retourneert direct DTOs
2.	MapStruct voor automatische mapping
3.	Handmatige conversie in Controller (huidige aanpak)
Rationale:
•	Single Responsibility Principle: Services focussen puur op business logic
•	Flexibiliteit: Controllers kunnen response-formaat per API-versie aanpassen
•	Testbaarheid: Service-tests valideren alleen domeinlogica, geen presentatie
•	Losse koppeling: Services blijven onafhankelijk van API-contracten
Consequenties:
•	Positief: Services zijn herbruikbaar over verschillende API-versies
•	Positief: Schone scheiding tussen business en presentatielagen
•	Overweging: Boilerplate mapper-code (kan verminderd worden met MapStruct)
•	Overweging: Vereist discipline om mappers gesynchroniseerd te houden met entities
Code Referenties:
•	MessageController.java (regels 144, 171: gebruikt MessageDtoMapper::toDto)
•	MessageDtoMapper.java (handmatige conversielogica)
________________________________________
# Beveiligingsbeslissingen
BB-001: Database-Ondersteunde Refresh Tokens
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Hoog
Context:
Refresh tokens kunnen stateless JWTs zijn of opgeslagen in database. Moet balans vinden tussen schaalbaarheid en beveiliging.
Beslissing:
Sla refresh tokens op in PostgreSQL refresh_tokens tabel met metadata (IP-adres, user agent, vervaldatum).
Overwogen Alternatieven:
1.	Stateless JWT refresh tokens (geen database)
2.	Redis cache voor refresh tokens
3.	Geëncrypteerde cookies zonder database
Rationale:
•	Token-intrekking: Cruciaal voor beveiligingsincidenten en uitloggen
•	Multi-device beheer: Volg en trek tokens in per apparaat
•	Audit trail: Zie wanneer/waar tokens zijn uitgegeven en gebruikt
•	Flexibele vervaldatum: Verschillende vervaldatum voor "onthoud mij" functionaliteit
•	Zwarte lijst: Gestolen tokens kunnen onmiddellijk ongeldig worden gemaakt
Consequenties:
•	Positief: Echte beveiliging: tokens kunnen worden ingetrokken vóór vervaldatum
•	Positief: "Overal uitloggen" functionaliteit
•	Positief: Detectie van verdachte activiteit (IP/locatie wijzigingen)
•	Overweging: Database-query bij elke token-verversing
•	Overweging: Opruimjob nodig voor verlopen tokens
Implementatie Details:
•	Access tokens: 15 minuten (kort venster bij compromis)
•	Refresh tokens: 7 dagen (balans tussen UX en beveiliging)
•	Max 5 refresh tokens per gebruiker (apparaatlimiet)
•	Geplande opruiming: dagelijks om 2:00 uur
Code Referenties:
•	RefreshToken.java (entity met user_id, token, expiry, IP, user_agent)
•	RefreshTokenService.java (regels 28-56: token-creatie met apparaatlimiet)
________________________________________
BB-002: BCrypt Wachtwoord Hashing (Cost Factor 12)
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Hoog
Context:
Wachtwoordopslag vereist hashing. Moet balans vinden tussen beveiliging en prestaties.
Beslissing:
Gebruik BCrypt met cost factor 12 (~200ms hash tijd).
Overwogen Alternatieven:
1.	BCrypt cost factor 10 (sneller, minder veilig)
2.	Argon2 (nieuwer, maar minder volwassen Java-ondersteuning)
3.	PBKDF2 (langzamer dan BCrypt)
Rationale:
•	Industriestandaard voor wachtwoord-hashing
•	Cost factor 12 = 4096 rondes (2^12)
•	~200ms hash tijd voorkomt brute force aanvallen
•	Handelt automatisch salt-generatie af
Consequenties:
•	Positief: Resistent tegen rainbow table aanvallen
•	Positief: Configureerbare work factor voor toekomstige hardware
•	Overweging: Login/registratie duurt ~200ms (acceptabele afweging)
Code Referenties:
•	SecurityConfig.java (BCrypt bean configuratie)
•	AuthService.java (wachtwoord-encodering tijdens registratie)
________________________________________
BB-003: Soft Delete voor Audit Trail
Datum: Januari 2026 (Week 8)
Status: Actief geïmplementeerd
Impact: Gemiddeld
Vervangt: Hard delete implementatie
Context:
Initiële implementatie gebruikte hard deletes (DELETE FROM messages). Realiseerde dat dit compliance-vereisten schendt en herstel voorkomt.
Beslissing:
Implementeer soft delete met is_deleted boolean vlag, deleted_at tijdstempel, en deleted_by UUID.
Overwogen Alternatieven:
1.	Hard delete met archieftabel
2.	Hibernate @SQLDelete annotatie
3.	Event sourcing (complete audit log)
Rationale:
•	Compliance: AVG, SOX, HIPAA vereisen audit trails
•	Herstel: Gebruikers kunnen per ongeluk verwijderde inhoud herstellen
•	Juridische bescherming: Bewijsbehoud voor geschillen
•	Eenvoud: Eén boolean veld vs complexe archieflogica
Consequenties:
•	Positief: Berichten herstelbaar binnen 90-dagen retentievenster
•	Positief: Admin kan zien wie wat en wanneer heeft verwijderd
•	Positief: Compliance-ready voor gereguleerde industrieën
•	Overweging: Queries moeten is_deleted = false filteren
•	Overweging: Opslag groeit sneller (periodieke opruiming nodig)
Implementatie Details:
•	Soft delete: UPDATE messages SET is_deleted=true, deleted_at=NOW(), deleted_by=? WHERE id=?
•	Hard delete: Geplande job verwijdert records ouder dan 90 dagen
•	Privacy balans: "Recht om vergeten te worden" na retentieperiode
Code Referenties:
•	Message.java (regels 64-72: is_deleted, deleted_at, deleted_by velden)
•	MessageService.deleteMessage() (regel 207-212: soft delete logica)
•	MessageRepository.findActiveByChatRoomId() (wordt intern gefilterd in repository)
________________________________________
# Databasebeslissingen
DB-001: UUID als Primaire Sleutel
Datum: November 2025
Status: Actief geïmplementeerd
Impact: Hoog
Context:
Primaire sleutels kunnen auto-increment Long of UUID zijn. Moet beslissen op basis van schaalbaarheid en beveiliging.
Beslissing:
Gebruik UUID versie 4 (random) als primaire sleutel voor alle entiteiten, gegenereerd via GenerationType.UUID.
Overwogen Alternatieven:
1.	Long met GenerationType.IDENTITY (auto-increment)
2.	UUID versie 7 (tijd-geordend, geïntroduceerd in Java 19+)
3.	Samengestelde sleutels (natuurlijke sleutels)
Rationale:
•	Collision-free schaling: Geen ID-conflicten bij database sharding
•	Beveiliging: Onvoorspelbare IDs voorkomen enumeratie-aanvallen (/users/1, /users/2)
•	Gedistribueerde systemen: Services kunnen IDs onafhankelijk genereren
•	Database-merge vriendelijk: Geen conflicten bij samenvoegen databases
•	Client-side generatie: Mogelijk voor optimistische UI-updates
Consequenties:
•	Positief: Statistisch uniek (1 op 2^122 collision kans)
•	Positief: Horizontaal schaalbaar zonder coördinatie
•	Overweging: 16 bytes vs 8 bytes (100% grotere indexes)
•	Overweging: UUIDv4 kan index-fragmentatie veroorzaken (random inserts)
Prestatie-impact:
•	Insert prestaties: ~5-10% langzamer dan Long bij bulk inserts
•	Query prestaties: Geen meetbaar verschil bij <1M records
•	Index grootte: +100% (16 vs 8 bytes per entry)
Toekomstige Overweging:
UUIDv7 (tijd-gebaseerd) zou index-fragmentatie verminderen terwijl UUID-voordelen behouden blijven. Overwegen voor volgende iteratie.
Code Referenties:
•	User.java (regel 19: @GeneratedValue(strategy = GenerationType.UUID))
•	RefreshToken.java (regel 36: UUID generatie)
•	Alle entiteiten gebruiken UUID consistent
________________________________________
DB-002: ChatRoomMember Join-Tabel
Datum: November 2025 (Week 4)
Status: Actief geïmplementeerd
Impact: Hoog
Vervangt: Impliciete membership via messages tabel
Context:
Initiële ERD miste expliciete many-to-many relatie tussen Users en ChatRooms. Membership werd afgeleid uit messages tabel, wat inefficiënte queries veroorzaakte.
Beslissing:
Voeg expliciete chatroom_members join-tabel toe met bidirectionele JPA-relaties.
Overwogen Alternatieven:
1.	Membership afleiden uit messages (originele aanpak)
2.	Aparte membership service met eigen database
3.	Gedenormaliseerde user_ids array in chatrooms tabel
Rationale:
•	Prestaties: Membership check is O(1) index lookup vs O(n) message scan
•	Data-integriteit: Expliciete foreign keys dwingen referentiële integriteit af
•	Rijke metadata: Kan role (OWNER/ADMIN/MEMBER), join_at, left_at volgen
•	Schaalbaarheid: Queries blijven snel naarmate berichtvolume groeit
Consequenties:
•	Positief: 25x snellere membership queries (2ms vs 50ms)
•	Positief: Maakt role-based permissies mogelijk (owner vs member)
•	Positief: Join/leave geschiedenis voor audit trail
•	Overweging: Vereiste 1-week refactoring van bestaande services
•	Overweging: Extra opslag (kleine overhead per membership)

# Tabel 2- Prestatie Vergelijking:

| Query                          | Voor Join-Tabel              | Na Join-Tabel          |
|:-------------------------------|:-----------------------------|:-----------------------|
| Is gebruiker lid?              | 50ms (scan messages)         | 2ms (index lookup)     |
| Haal alle leden op             | N/A (onmogelijk)             | 10ms                   |
| Haal gebruiker's rooms op      | 200ms (join messages)        | 5ms                    |

Code Referenties:
•	ChatRoom.java (regel 64: @OneToMany List<ChatRoomMember> members)
•	User.java (regel 80: @OneToMany List<ChatRoomMember> memberships)
•	ChatRoomMember.java (complete join entity met roles en tijdstempels)
________________________________________
Implementatiebeslissingen
IB-001: Lokale Bestandsopslag (met S3 Migratiepad)
Datum: November 2025
Status: Actief geïmplementeerd (met gedefinieerd migratiepad)
Impact: Gemiddeld
Context:
Voor ontwikkel- en demo-doeleinden is een eenvoudige bestandsopslag nodig zonder externe cloud-afhankelijkheden. Moet beslissen tussen lokale opslag en cloud-oplossingen zoals AWS S3.
Beslissing:
Gebruik lokale bestandsopslag in ./uploads/ directory met unieke UUID-bestandsnamen, met een gedefinieerd FileStorageService interface voor toekomstige cloud-migratie.
Overwogen Alternatieven:
1.	AWS S3 met pre-signed URLs (productie-ready, maar complexer)
2.	Google Cloud Storage (alternatieve cloud provider)
3.	MinIO (self-hosted S3-compatible storage)
Rationale:
•	Geen externe afhankelijkheden: Geen AWS-account vereist tijdens ontwikkeling
•	Sneller debuggen: Bestanden direct zichtbaar in bestandssysteem
•	Voldoende voor opleidingscontext: Demonstreert bestandsbeheer zonder overengineering
•	Lage latency: Lokale disk I/O sneller dan network calls bij kleine datasets
•	Toekomstbestendig: Interface-based design maakt cloud-migratie eenvoudig
Consequenties:
•	Positief: Eenvoudige setup zonder cloud-configuratie
•	Positief: Snelle ontwikkelcyclus (directe bestandsinspectie)
•	Positief: Geen maandelijkse cloud-kosten tijdens ontwikkeling
•	Positief: FileStorageService interface maakt migratie mogelijk zonder code-wijzigingen
•	Overweging: Niet geschikt voor productie (beperkte schijfruimte, geen redundantie)
•	Overweging: Horizontale schaling vereist shared storage oplossing
Implementatie Details:
FileStorageService Interface (Dependency Inversion):
java
public interface FileStorageService {
    String storeFile(MultipartFile file) throws IOException;// (ref. nr. 1)
    Resource loadFile(String filename) throws IOException;
    void deleteFile(String filename) throws IOException; // (ref. nr. 2)
}
Huidige Implementatie:
java
@Service
@Profile("dev") // Actief in development
public class LocalFileStorageService implements FileStorageService {
    private final Path uploadDir = Paths.get("uploads"); // (ref. nr. 3)
    
    @PostConstruct
    public void init() throws IOException { // (ref. nr. 4)
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir); // (ref. nr. 5)

        }
    }
    
    @Override
    public String storeFile(MultipartFile file) throws IOException {// (ref. nr. 1)
        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(uniqueFileName); // (ref. nr. 6,9)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return uniqueFileName;
    }
}
Toekomstig Migratiepad:
java
@Service
@Profile("production") // Actief in productie
public class S3FileStorageService implements FileStorageService {
    private final AmazonS3 s3Client;
    private final String bucketName;
    
    // Implementatie met AWS SDK
    // Geen wijzigingen nodig in controllers/services
}

# Tabel 3-Productie Overwegingen (Voor S3 Migratie): (ref.nr.10,11,12, 13 and 14)

| Aspect                  | Lokale Opslag                          | AWS S3                                      |
|:------------------------|:---------------------------------------|:--------------------------------------------|
| Schaalbaarheid          | Beperkt door disk                      | Onbeperkt                                   |
| Redundantie             | Geen (single point of failure)         | 99.999999999% durability                    |
| Backup                  | Handmatig                              | Automatisch (versioning)                    |
| CDN Integratie          | N/A                                    | CloudFront integration                      |
| Kosten                  | Gratis (eigen server)                  | Pay-per-use ($0.023/GB)                     |
| Horizontale Schaling    | Onmogelijk                             | Seamless                                    |

Migratiestrategie:
1.	Fase 1 - Development (huidig):
o	LocalFileStorageService actief met @Profile("dev")
o	Bestanden in ./uploads/ directory
2.	Fase 2 - Staging:
o	S3FileStorageService implementeren met @Profile("staging")
o	Test migratie met bestaande bestanden
o	Parallel validation (uploads naar beide systemen)
3.	Fase 3 - Production:
o	S3FileStorageService actief met @Profile("production")
o	CloudFront CDN voor snelle downloads wereldwijd
o	Lifecycle policies voor oude bestanden
Code Referenties:
•	FileStorageService.java (interface, maakt migratie mogelijk)
•	LocalFileStorageService.java (huidige implementatie, regel 49: UUID generatie)
•	FileController.java (gebruikt FileService, die FileStorageService gebruikt)
Relatie met Verantwoordingsdocument:
•	“Sectie 2.4: "Bestanden opslaan op lokale schijf i.p.v. S3"” // (ref. nr. 7)
•	Tabel: Opslag → Bestanden → Lokaal (/uploads/) vs AWS S3 + CDN
________________________________________
# Implementatie-evolutie
EV-001: HTTP Polling → Server-Sent Events (SSE)
Datum: Januari 2026 (Week 7)
Status: Actief geïmplementeerd
Impact: Hoog
Vervangt: HTTP polling elke 5-10 seconden
Context:
Initiële implementatie gebruikte HTTP polling voor nieuwe berichten. Gebruikerstesten onthulden slechte UX (5-10 seconden latentie) en excessief netwerkverkeer.
Beslissing:
Migreer naar Server-Sent Events (SSE) met Spring Boot SseEmitter.
Overwogen Alternatieven:
1.	Doorgaan met polling (slechte UX)
2.	WebSockets (complexer, vereist protocol upgrade)
3.	Long-polling (beter dan polling, slechter dan SSE)
Rationale:
•	Latentie: <100ms vs 5-10 seconden
•	Netwerkefficiëntie: 90% reductie in verkeer (geen lege poll requests)
•	Browser-native: Automatische herverbinding ingebouwd
•	Uni-directioneel: Perfect voor server-naar-client messaging
•	HTTP-gebaseerd: Geen WebSocket complexiteit
Consequenties:
•	Positief: Real-time messaging ervaring (industriestandaard)
•	Positief: Significant betere gebruikerservaring
•	Positief: Schaalt naar duizenden gelijktijdige verbindingen
•	Overweging: Open verbindingen consumeren server resources
•	Overweging: Clients moeten event listeners implementeren
Implementatie Details:
•	Endpoint: GET /api/chatrooms/{roomId}/messages/stream
•	Content-Type: text/event-stream
•	Stuurt nieuwe berichten als JSON events
•	Client: JavaScript EventSource API
Code Referenties:
•	MessageController.streamMessages() (regel 180: SSE endpoint)
•	MessageStreamService.publish() (publiceert events naar streams)
•	MessageController.sendMessage() (regel 99: publiceert na save)
________________________________________
EV-002: Byte Array → Streaming Bestandsdownloads
Datum: Januari 2026 (Week 8)
Status: Actief geïmplementeerd
Impact: Gemiddeld
Vervangt: In-memory byte[] laden
Context:
Applicatie crashte met OutOfMemoryError bij downloaden van grote bestanden (>500MB). Initiële implementatie laadde volledig bestand in geheugen.
Beslissing:
Gebruik Spring Resource interface (specifiek UrlResource) voor streaming downloads.
Overwogen Alternatieven:
1.	Heap size verhogen (lost fundamenteel probleem niet op)
2.	Handmatige InputStream afhandeling (complexer)
3.	Chunked transfer encoding (automatisch afgehandeld door Resource)
Rationale:
•	Constant geheugen: Alleen 8KB chunks in geheugen, niet volledig bestand
•	Grote bestand ondersteuning: Getest met bestanden tot 1GB
•	Range requests: Browser pauze/hervatten automatisch ondersteund
•	Betere prestaties: Client begint onmiddellijk met downloaden
Consequenties:
•	Positief: Geen OutOfMemoryError voor elke bestandsgrootte
•	Positief: Ondersteunt pauze/hervatten (HTTP Range header)
•	Positief: Lager geheugen footprint (voorspelbaar resource gebruik)
•	Overweging: Iets complexer dan byte[] aanpak

# Tabel 4-Prestatie Vergelijking: (ref.nr.15)   

| Bestandsgrootte | byte[] Aanpak             | Streaming Aanpak      |
|:----------------|:--------------------------|:----------------------|
| 10 MB           | Werkt                     | Werkt                 |
| 100 MB          | Traag, veel geheugen      | Snel, weinig geheugen |
| 500 MB          | OutOfMemoryError          | Werkt perfect         |
| 1 GB            | Crash                     | Werkt perfect         |

Code Referenties:
•	FileController.downloadFile() (regel 79: retourneert ResponseEntity<Resource>)
•	LocalFileStorageService.loadFile() (regel 69: retourneert Resource)
________________________________________
# Bekende Beperkingen
BL-001: Granulaire Rate-Limiting Niet Geïmplementeerd
Datum: Januari 2026
Status: Niet actief - planning nodig
Impact: Hoog
Beschrijving:
Huidige rate-limiting is globaal IP-gebaseerd in de Node.js gateway (100 requests/minuut per IP). Geen per-resource limieten bestaan voor:
•	Bestandsuploads per chatroom
•	Berichtfrequentie per chatroom
•	Bestandstype quota (afbeeldingen vs video's)
•	Per-gebruiker quotas
Impact:
•	DoS kwetsbaarheid: Kwaadwillende gebruiker kan chatroom spammen met 1000 bestanden
•	Opslag uitputting: Geen limieten op totale opslag per gebruiker/room
•	Bandbreedte misbruik: Geen onderscheid tussen 10KB afbeeldingen en 500MB video's
•	Slechte UX: Legitieme gebruikers getroffen door spam
Geplande Oplossing:
Implementeer Bucket4j token bucket algoritme met Redis voor gedistribueerde rate-limiting.
Voorbeeld Scenario's:
text
Huidig actief: 100 requests/minuut per IP (globaal)
Niet actief: 50 uploads/uur per gebruiker per chatroom
Niet actief: 30 berichten/minuut per gebruiker per chatroom
Niet actief: Max 5 video's/uur, 50 afbeeldingen/uur per gebruiker
Code Referentie:
•	nodejs-gateway/index.js (regels 11-21: rate limiter configuratie)
Tijdlijn: Gepland voor v2.0 (vereist Redis infrastructuur)
Relatie met Verantwoordingsdocument:
•	Sectie 3.5: "Geen rate-limiting per chatroom of bestandstype"
•	Dit is de enige echte beperking in het systeem (niet een bewuste keuze)
________________________________________
# Toekomstige Roadmap
Geplande Functies
Korte termijn (Volgende 1-2 maanden):
•	Granulaire rate-limiting met Bucket4j + Redis (BL-001)
•	AWS S3 migratie voor bestandsopslag (IB-001 upgrade)
•	API versioning (/api/v1/, /api/v2/)
•	Uitgebreide integratie testsuite (20% coverage verhogen naar 40%)
•	Intern JWT voor gateway-backend communicatie (AB-002 verbetering)
Middellange termijn (3-6 maanden):
•	WebSocket ondersteuning voor bi-directionele real-time functies
•	End-to-end encryptie voor berichten
•	Multi-regio database replicatie
•	CDN integratie voor bestandsdownloads (CloudFront)
•	Geavanceerde audit logging (detectie verdachte activiteit)
•	UUIDv7 migratie voor betere database prestaties (DB-001 verbetering)
Lange termijn (6-12 maanden):
•	Mobiele app ondersteuning (iOS/Android)
•	Video/audio bellen functionaliteit
•	AI-aangedreven content moderatie
•	Compliance certificeringen (SOC 2, ISO 27001)
•	Kubernetes deployment met auto-scaling

________________________________________
Contact & Feedback
Document Eigenaar: Michael Barak
Project Repository: https://github.com/michaelba21/SecureChat-API 
Feedback: Voor suggesties of correcties, open een issue in de repository

# Tabel 4- Versiegeschiedenis: (ref.nr.16)

| Versie | Datum          | Wijzigingen                                                                                             | Auteur        |
|:-------|:---------------|:--------------------------------------------------------------------------------------------------------|:--------------|
| v1.0   | November 2025  | Initiële architectuurbeslissingen (AB-001, AB-002, AB-003, BB-001, BB-002, DB-001)                       | Michael Barak |
| v1.1   | Januari 2026   | Evolutie secties toegevoegd (EV-001, EV-002), Database join-tabel (DB-002)                              | Michael Barak |
| v1.2   | Januari 2026   | Soft delete toegevoegd (BB-003), Beperkingen gedocumenteerd (BL-001, BL-002), Snelle Referentietabel toegevoegd | Michael Barak |
| v1.3   | Januari 2026   | Correctie: BL-002 (Lokale Bestandsopslag) geherclassificeerd van "Beperking" naar "Implementatiebeslissing" (IB-001). Nieuwe sectie "Implementatiebeslissingen" toegevoegd. Uitgebreide documentatie van migratiepad en rationale. | Michael Barak |

Laatst Bijgewerkt: februari 2026
Onderhouden Door: Michael Barak


