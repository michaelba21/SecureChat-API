
# System Design & Architecture Flows

This document details the core data flows, class structures, and technical architecture for the SecureChat application.

## 7.2 Klassendiagram (Class Diagram)

```mermaid
erDiagram
    User {
        UUID id PK
        string username
        string email
        string password_hash
        datetime created_at
        datetime last_login
        boolean is_active
    }

    ChatRoom {
        UUID id PK
        UUID created_by FK
        string name
        string description
        boolean is_private
        datetime created_at
    }

    Message {
        UUID id PK
        UUID user_id FK
        UUID chat_room_id FK
        string content
        string message_type
        datetime timestamp
        boolean is_edited
    }

    ChatRoomMember {
        UUID id PK
        UUID user_id FK
        UUID chat_room_id FK
        datetime joined_at
        datetime left_at
    }

    File {
        UUID id PK
        UUID user_id FK
        string filename
        string file_path
        int file_size
        string mime_type
        datetime uploaded_at
    }

    User ||--o{ ChatRoom : creates
    User ||--o{ Message : sends
    User ||--o{ ChatRoomMember : is_member
    User ||--o{ File : uploads
    ChatRoom ||--o{ Message : contains
    ChatRoom ||--o{ ChatRoomMember : has_members
    Message ||--|| File : "may have (0..1)"
```

## 8. Sequentiediagrammen (Sequence Diagrams)

### 8.1 User Login & JWT Token Issuance

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant A as Auth Controller
    participant S as Auth Service
    participant D as Database

    C->>G: POST /auth/login
    Note over C,G: {email, password}
    
    G->>A: Forward login request
    A->>S: authenticate(email, password)
    S->>D: SELECT * FROM users WHERE email = ?
    D-->>S: Return user data
    S->>S: Verify password with bcrypt
    S->>S: Generate JWT tokens
    S->>D: UPDATE last_login timestamp
    S-->>A: Return tokens + user info
    A-->>G: Login response
    G-->>C: 200 OK with tokens
```

### 8.2 Bericht Versturen (Message Sending)

```mermaid
sequenceDiagram
    participant Client
    participant MessageController
    participant MessageService
    participant MessageRepository
    participant DB
    Client->>MessageController: POST /api/chatrooms/{id}/messages
    MessageController->>MessageService: createMessage(userId, roomId, messageDto)
    
    MessageService->>MessageService: validateMembership(userId, roomId)
    alt Not member
        MessageService-->>MessageController: throw ForbiddenException()
        MessageController-->>Client: 403 Forbidden
    else Is member
        MessageService->>MessageService: createMessageEntity()
        MessageService->>MessageRepository: save(message)
        MessageRepository->>DB: INSERT INTO messages
        DB-->>MessageRepository: success
        MessageRepository-->>MessageService: savedMessage
        MessageService-->>MessageController: MessageResponseDto
        MessageController-->>Client: 201 Created
    end
```



### 8.3 Polling Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB as Database
    Client->>Controller: HTTP Request (with JWT)
    Note over Controller: Spring Security validates JWT
    Controller->>Service: Process business logic
    Service->>Repository: Perform data operation
    Repository->>DB: Execute SQL query
    DB-->>Repository: Return data
    Repository-->>Service: Return entity/result
    Service-->>Controller: Return DTO/response
    Controller-->>Client: HTTP Response
```

## 9. Technische Architectuur

### 9.1 Architectuuroverzicht

```mermaid
graph TD
    A[Client<br/>Postman/Browser] --> B[Node.js Gateway<br/>Port 3000]
    B --> C[Java Spring Boot<br/>Port 8080]
    C --> D[PostgreSQL<br/>Port 5432]
    C --> E[File Storage<br/>/uploads/]
    
    B --> F[JWT Auth]
    B --> G[Rate Limiting]
    B --> H[CORS]

    C --> I[Controllers]
    C --> J[Services]
    C --> K[Repositories]
    
    I --> L[Auth]
    I --> M[Users]
    I --> N[ChatRooms]
    I --> O[Messages]
    I --> P[Files]
```

## 13.3 Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    User ||--o{ ChatRoom : "creates (1:N)"
    User ||--o{ Message : "sends (1:N)"
    User ||--o{ ChatRoomMember : "participates (1:N)"
    User ||--o{ File : "uploads (1:N)"
    User ||--o{ RefreshToken : "has (1:N)"
    User ||--o{ MessageAuditLog : "performs (1:N)"
    User ||--o{ AdminAction : "performs as admin (1:N)"
    User ||--o{ AdminAction : "receives as target (1:N)"
    
    ChatRoom ||--o{ Message : "contains (1:N)"
    ChatRoom ||--o{ ChatRoomMember : "has members (1:N)"
    
    Message ||--|| File : "attaches (1:0..1)"
    Message ||--o{ MessageAuditLog : "audited by (1:N)"
    
    User {
        UUID id PK
        String username UK
        String email UK
        String password_hash
        String bio
        String avatar_url
        Status status
        UserRole role
        Boolean is_active
        Instant created_at
        Instant last_login
        UUID deactivated_by
        Instant deactivated_at
        String deactivation_reason
    }
    
    ChatRoom {
        UUID id PK
        UUID created_by FK
        String name
        String description
        Boolean is_private
        Integer max_participants
        Instant created_at
        Instant deleted_at
        UUID deleted_by
    }
    
    Message {
        UUID id PK
        UUID sender_id FK
        UUID chat_room_id FK
        UUID file_id FK
        String content
        MessageType message_type
        Boolean is_edited
        Instant edited_at
        Instant timestamp
        Instant deleted_at
        UUID deleted_by
    }
    
    ChatRoomMember {
        UUID id PK
        UUID user_id FK
        UUID chat_room_id FK
        MemberRole role
        Instant joined_at
        Instant left_at
        Instant last_activity
        UUID removed_by
    }
    
    File {
        UUID id PK
        UUID uploader_id FK
        String filename
        String file_path
        Long file_size
        String mime_type
        Boolean is_public
        Instant uploaded_at
        Instant deleted_at
        UUID deleted_by
    }
    
    RefreshToken {
        UUID id PK
        UUID user_id FK
        String token UK
        Instant expiry_date
        Instant created_at
        Instant last_used_at
        String ip_address
        String user_agent
    }
    
    MessageAuditLog {
        UUID id PK
        UUID message_id FK
        UUID performer_id FK
        String content
        AuditAction action
        Instant performed_at
        String ip_address
        String user_agent
        String reason
        String old_content
    }
    
    AdminAction {
        UUID id PK
        UUID admin_id FK
        UUID target_user_id FK
        AdminActionType action_type
        String reason
        Instant performed_at
        String ip_address
        String metadata
    }
```

## Testing Strategy

```mermaid
flowchart LR
    A[ Unit<br/>70%] --> B[ Integration<br/>25%] --> C[ E2E<br/>5%]
    
    subgraph Details [Test Distribution]
        A1[50-60 tests<br/>Fast execution]
        B1[10-15 tests<br/>Medium speed]
        C1[2-3 tests<br/>Slow but comprehensive]
    end
```

## 14. Implementation Details & Code Maps

### 14.1 Real-time Messaging (Server-Sent Events)
**Implementatie**:
*   **Controller**: `MessageController.java` (`streamMessages` endpoint, ~line 189) handles the SSE connection.
*   **Publishing**: `MessageController.java` (`sendMessage` method, ~line 102) publishes "new-message" events via `MessageStreamService`.
*   **Rationale**: SSE chosen over WebSockets for simpler HTTP-based protocol and automatic browser reconnection, sufficient for server-to-client updates.

### 14.2 Database: ChatRoomMember Join-Table
**Implementatie**:
*   **Entity**: `ChatRoomMember.java` explicitly maps the N:M relationship between `User` and `ChatRoom`.
*   **Structure**: Includes metadata fields like `joined_at`, `role` (ADMIN/MEMBER), and `last_read_at`.
*   **Benefits**: Enables efficient membership queries and role-based access control compared to simple ID arrays.

### 14.3 Bestandsdownloads (Streaming)
**Implementatie**:
*   **Controller**: `FileController.java` returns `ResponseEntity<Resource>` to stream data.
*   **Storage**: `LocalFileStorageService.java` uses `UrlResource` (line 71) to reference files on disk without loading them entirely into memory.
*   **Benefit**: Prevents `OutOfMemoryError` when handling large file downloads (>500MB).

### 14.4 Soft Delete Audit Trail
**Implementatie**:
*   **Entity**: `Message.java` uses an `isDeleted` boolean flag and `deletedAt` timestamp.
*   **Service**: `MessageService.deleteMessage` (lines 229-236) performs a logical delete update rather than a physical `DELETE` SQL command.
*   **Benefit**: Preserves history for audit purposes and data recovery.

### 14.5 Architecture & Code References

| Categorie | Onderwerp | Keuze | Code Reference (Verified) | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Architectuur** | JWT-verificatie | Node.js Gateway | `nodejs-gateway/server.js` | Lines 40-70 (Proxy logic) |
| **Code** | DTO-conversie | Handmatige mappers | `MessageController.java` | Uses `MessageDtoMapper` |
| **Database** | UUID Keys | `java.util.UUID` | `User.java`, `RefreshToken.java` | `@GeneratedValue(strategy=UUID)` |
| **Opslag** | Bestanden | Lokaal (`/uploads/`) | `LocalFileStorageService.java` | Generates UUID filenames |
| **Verbetering** | Real-time | SSE (SseEmitter) | `MessageController.java` | `streamMessages` & `publish` |
| **Verbetering** | Membership | Join-tabel | `ChatRoomMember.java` | Full Entity Implementation |
| **Verbetering** | Downloads | Streaming Resource | `FileController.java` | Returns `ResponseEntity<Resource>` |
| **Verbetering** | Compliance | Soft Delete | `MessageService.java` | `softDelete` method implemented |

