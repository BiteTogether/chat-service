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

## Project Structure

```text
chat-service/
  src/main/java/com/bitetogether/chat_service/
    configuration/
      crypto/            # encryption configuration
      kafka/             # kafka consumer/producer/container configuration
      mongodb/           # mongo auditing/config
      reactive/          # reactive request context filters (user headers -> context)
      redis/             # redis configuration
      security/          # webflux security chain
      webclient/         # downstream webclient config
      websocket/         # websocket mapping and auth extraction
    controller/          # REST APIs (ConversationController, MessageController)
    dto/                 # request/response/event DTOs
    enums/               # domain enums and websocket actions
    event/               # domain event publisher interfaces/impl
    exception/           # error code and processing exceptions
    mapper/              # mappers between model and DTO
    model/               # Mongo documents (Conversation, Message, Participant, ChatUserSnapshot)
    repository/          # reactive repositories
    service/             # business logic + kafka listeners
    util/                # pagination and helper utilities
    websocket/           # websocket handler, subscriber, room registry
  src/main/resources/
    application.yml
    application-*.yml
```

## API Details

### Base paths

- Conversation APIs: `/api/v1/conversations`
- Message APIs: `/api/v1/messages`
- WebSocket endpoint: `/ws/chat`

### Authentication/user context headers

This service resolves caller context from forwarded headers (typically from API Gateway):

- `X-User-Id` (required for user-scoped operations)
- `X-User-Role` (optional, defaults to `USER`)
- `X-User-Email` (optional)
- `X-Username` (optional)

For WebSocket, user context is read from headers first, then query params fallback (`userId`, optional `role`, `email`, `username`).

### Response envelope

REST handlers return `ApiResponseDTO<T>` with common fields such as status, message, and data.

### Conversation APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/conversations` | Create conversation (`DIRECT` or `GROUP`) |
| `GET` | `/api/v1/conversations/{conversationId}` | Get conversation details |
| `GET` | `/api/v1/conversations?cursor=&limit=` | Get current user's conversations (cursor by ISO datetime) |
| `PUT` | `/api/v1/conversations/{conversationId}` | Update name/avatar (admin only) |
| `POST` | `/api/v1/conversations/{conversationId}/participants/{userId}` | Add participant (admin only) |
| `DELETE` | `/api/v1/conversations/{conversationId}/participants/{userId}` | Remove participant (self or admin) |
| `PATCH` | `/api/v1/conversations/{conversationId}/participants/{userId}/role?role=ADMIN` | Update role (admin only) |
| `DELETE` | `/api/v1/conversations/{conversationId}` | Delete conversation (admin only) |

`CreateConversationRequest`:
- `type` (`DIRECT` or `GROUP`)
- `name` (required for group)
- `avatarUrl` (optional)
- `participantIds` (set of user IDs)

`UpdateConversationRequest`:
- `name` (optional, max 100)
- `avatarUrl` (optional)

### Message APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/messages` | Send message |
| `GET` | `/api/v1/messages/conversation/{conversationId}?cursor=&limit=` | Get messages in conversation (cursor by sequence) |
| `GET` | `/api/v1/messages/{messageId}` | Get message by ID |
| `PUT` | `/api/v1/messages/{messageId}` | Update message content (sender only) |
| `DELETE` | `/api/v1/messages/{messageId}` | Delete message (sender only) |

`ChatInboundMessage` (REST send + WebSocket send):
- `conversationId` (required)
- `action` (required; typically `SEND` for create-message flow)
- `messageType` (`TEXT`, `IMAGE`, `FILE`, `EMOJI`)
- `content` (plaintext; service encrypts before persistence)

`UpdateMessageRequest`:
- `content` (required, non-blank)

### WebSocket API (`/ws/chat`)

Supported inbound actions (`WebSocketAction`):

- `SUBSCRIBE`: subscribe current session to a conversation room.
- `UNSUBSCRIBE`: unsubscribe current session from a room.
- `SEND`: send a message to a room (persists and broadcasts).
- `TYPING`: accepted but currently no-op placeholder.
- `READ`: accepted but currently no-op placeholder.

Inbound payload example:

```json
{
  "conversationId": "conv_abc123",
  "action": "SEND",
  "messageType": "TEXT",
  "content": "Hello team"
}
```

Outbound event examples:

- Created message: `{"action":"SEND","conversationId":"...","message":{...}}`
- Updated message: `{"action":"SEND","eventType":"MESSAGE_UPDATED","conversationId":"...","message":{...}}`
- Deleted message: `{"action":"SEND","eventType":"MESSAGE_DELETED","conversationId":"...","messageId":"..."}`

If parsing/auth fails, the socket emits:

```json
{"type":"ERROR","message":"..."}
```

## Pagination

- Conversations: cursor is an ISO datetime string (`YYYY-MM-DDTHH:mm:ss`), newest-first.
- Messages: cursor is message sequence number, newest-first.
- `limit` is normalized in service utilities (default and max safeguards are applied).

## Configuration Notes

Profiles included from `application.yml`:

- `security`
- `openapi`
- `database`
- `webclient`
- `kafka`
- `redis`
- `crypto`

Key environment variables depend on each profile file, and include at least:

- `SERVER_PORT`
- Mongo/Kafka/Redis/crypto settings referenced in `application-*.yml`
- OpenAPI metadata variables (`API_TITLE`, `API_DESCRIPTION`, `API_VERSION`, etc.)

## Running Locally

```bash
cd /home/kienle/Coding/BiteTogether/chat-service
./mvnw spring-boot:run
```

## Test

```bash
cd /home/kienle/Coding/BiteTogether/chat-service
./mvnw test
```
