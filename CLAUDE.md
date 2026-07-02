# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SoulMate AI — an emotional companion app backend. Users create AI companions with customizable personalities, relationships, and speaking styles, then chat with them via streaming (SSE) or synchronous REST endpoints. The AI engine is mimo-v2.5-pro (Xiaomi), accessed through Spring AI's OpenAI-compatible client.

## Build & Run

```bash
# Build all modules
mvn clean package -DskipTests

# Run the application (soulmate-app is the entry point)
mvn spring-boot:run -pl soulmate-app

# Run a single test class
mvn test -pl <module-name> -Dtest=ClassName

# Run a single test method
mvn test -pl <module-name> -Dtest=ClassName#methodName
```

Requires: Java 21, PostgreSQL 16, Redis. Initialize DB with `sql/init.sql`.

## Module Architecture

Six Maven modules with strict dependency direction (no circular deps):

```
soulmate-app        → Spring Boot entry point, application.yml, SoulMateApplication.java
soulmate-web        → Controllers, security config, WebSocket config, CORS, MyBatis-Plus config
soulmate-service    → Business logic (interfaces + impl/), PromptBuilder for LLM prompts
soulmate-ai         → AI capability module: Spring AI ChatClient integration (currently thin, delegates to service layer)
soulmate-mapper     → MyBatis-Plus mapper interfaces (one per entity, extends BaseMapper<Entity>)
soulmate-domain     → Entities, DTOs, enums — pure data classes, no business logic
soulmate-common     → Cross-cutting: R<T> response wrapper, ResultCode enums, BizException, JwtUtil, config properties, constants
```

Dependency chain: `web → service → mapper → domain`, `web → common`, `service → ai → common`, all modules depend on `common`.

## Key Patterns

**Unified response**: All REST endpoints return `R<T>` (code=0 for success, non-zero for errors). Error codes defined in `ResultCode` enum. Throw `BizException` for business errors; `GlobalExceptionHandler` catches and wraps them.

**Authentication**: JWT-based stateless auth. `JwtAuthFilter` (in SecurityConfig) extracts `Bearer` token from `Authorization` header, resolves userId, sets it as `request.setAttribute("currentUserId", userId)`. Controllers inject via `@RequestAttribute("currentUserId")`. White-listed paths: `/api/auth/**`, `/actuator/**`, `/ws/**`.

**AI chat flow**: `ConversationController` → `ConversationService` → `ChatService` → `PromptBuilder` → Spring AI `ChatClient`. PromptBuilder assembles system prompt (companion personality, relationship type, speaking style, background story) + Redis-cached conversation history + user message. History stored in Redis lists as `"role:content"` strings with a sliding context window.

**Streaming**: SSE via `Flux<ChatResponse>` at `POST /api/chat/stream`. The `ChatResponse` includes `conversationId`, `content` chunk, and `done` flag. Synchronous fallback at `POST /api/chat/send`.

**Database conventions**: All tables prefixed with `t_`. All entities use logical delete (`deleted` field, 0=active, 1=deleted). IDs are snowflake-style (`id-type: assign_id` in MyBatis-Plus config). Underscore-to-camelCase mapping enabled. Mapper XML files go in `classpath*:mapper/**/*.xml`.

**WebSocket**: STOMP over SockJS at `/ws` endpoint. Client sends to `/app/*`, server pushes to `/topic/*`. Used for real-time message delivery and typing indicators.

## Configuration

Application config is in `soulmate-app/src/main/resources/application.yml`. Custom config prefix: `soulmate.*` (JWT settings, free-tier limits, AI model settings). Properties classes: `AiProperties`, `JwtProperties`, `LimitProperties` (all in `soulmate-common`).

The AI model defaults to `mimo-v2.5-pro` via OpenAI-compatible API at `https://api.xiaomimimo.com/v1`. Spring AI is configured under `spring.ai.openai.*`.

## Conventions

- Chinese comments and log messages throughout the codebase — maintain this style.
- Lombok everywhere: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`.
- MapStruct for DTO conversions (processor configured in maven-compiler-plugin).
- Enums use `UPPERCASE` values stored as `VARCHAR` in DB (not ordinal).
- Entity fields: `createTime`, `updateTime`, `deleted` are standard on all tables.
