# Chat Service

The Chat Service powers 1:1 and group messaging for BiteTogether. It exposes reactive REST APIs for conversation/message lifecycle and a WebSocket channel for real-time delivery.

## Application Business

### What this service owns

- Conversation lifecycle: create, read, update, delete.
- Participant lifecycle: add/remove members and update participant roles.
- Message lifecycle: send, fetch (cursor pagination), update, delete.
- Real-time fanout of message created/updated/deleted events via WebSocket.
- Per-user unread state using participant `lastReadMessageSequence`.
- Message encryption at rest (message content is encrypted before persistence).

### Key business rules

- Conversation types:
    - `DIRECT`: must contain exactly 2 unique participants.
    - `GROUP`: requires a non-empty `name`.
- Conversation creator is automatically added as `ADMIN` (for REST-created conversations).
- Duplicate direct conversations are prevented when one already exists between 2 users.
- Authorization rules:
    - Only participants can read conversation/messages.
    - Only sender can update/delete a message.
    - Only admins can update conversation metadata, manage other participants, or delete conversation.
    - The last admin cannot be removed/demoted.
- Message retrieval auto-updates read progress for the requesting user.

### Integration behavior

- Kafka consumer: user events sync `ChatUserSnapshot` (`UserCreated`, `UserUpdated`, `UserDeleted`).
- Kafka consumer: conversation events can auto-create direct conversations between two users.
- Domain event publisher + WebSocket subscriber broadcast message changes to connected room sessions.

## Tech Stack

- Java 21, Spring Boot 3 (WebFlux)
- Reactive MongoDB (`spring-boot-starter-data-mongodb-reactive`)
- Reactive Redis (`spring-boot-starter-data-redis-reactive`)
- Kafka (`spring-kafka`)
- OpenAPI/Swagger (`springdoc-openapi-starter-webflux-ui`)
- Shared contracts/utilities from `common-service`

### Endpoint
```
ws://gateway-url/chat-service/ws/chat
```

**⚠️ Important:** WebSocket connections MUST go through API Gateway (Kong).

### Authentication
Gateway validates JWT token and injects headers:
- `X-User-Id` - User ID (required)
- `X-User-Role` - User role
- `X-User-Email` - User email  
- `X-Username` - Username

Chat service reads these headers to identify the user. Direct connections without Gateway headers will be rejected.

### Client Connection (via Gateway)
```javascript
// Client sends JWT in Authorization header
const ws = new WebSocket('ws://gateway-url/chat-service/ws/chat', {
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  }
});
```

**Note:** JavaScript WebSocket API doesn't support custom headers. Use Gateway's WebSocket proxy with JWT in URL or upgrade handshake.

### Message Format
```json
{
  "conversationId": "conv_123",
  "action": "SEND",
  "messageType": "TEXT",
  "content": "Hello, world!"
}
```

**Actions**: `SEND`, `SUBSCRIBE`, `UNSUBSCRIBE`, `TYPING`, `READ`  
**Message Types**: `TEXT`, `IMAGE`, `FILE`, `EMOJI`

### Testing
Xem các công cụ test trong thư mục [`test/`](test/)

---

## Configuration

- **Server Port:** `8083` (environment variable: `SERVER_PORT`)
- **WebSocket Path:** `/ws/chat`

## Running the Service

### Using Docker Compose
```bash
docker-compose up -d
```

Service sẽ chạy trên port `8083`.

### Checking Service Status
```bash
# Check if service is running
curl http://localhost:8083/actuator/health

# View logs
docker-compose logs -f chat-service
```

## Environment Variables

Xem file `docker-compose.yml` để biết danh sách đầy đủ các biến môi trường cần thiết.
