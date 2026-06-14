# Clash Royale MVP

Free-to-play Clash Royale-inspired Android game. 1v1 real-time matches, no pay-to-win, one fixed deck of 8 cards for MVP.

## Tech Stack

- **Game client**: LibGDX (Kotlin) — targets Android + Desktop (Desktop for dev testing)
- **Server**: Ktor (Kotlin) + WebSockets — authoritative game simulation
- **Serialization**: `kotlinx-serialization` JSON throughout
- **Deployment**: Railway free tier (git-push, reads `PORT` env var)

## Module Structure

```
:server          — Ktor JVM application (authoritative server)
:game:core       — LibGDX game logic, shared across platforms
:game:android    — Android launcher + native libs
:game:desktop    — Desktop launcher for development/testing
```

## How to Run

**Server (local):**
```
./gradlew :server:run
```

**Desktop game (connects to local server):**
```
./gradlew :game:desktop:run
```

**Android:** Build and install via IntelliJ IDEA's Android run configuration. Requires Android SDK installed and a physical device or emulator.

## Game Design

**Arena:** 1000 × 1800 world units. Y=0 = Player 1 side, Y=1800 = Player 2 side.

Tower positions:
- P1: Princess(200,200), King(500,100), Princess(800,200)
- P2: Princess(200,1600), King(500,1700), Princess(800,1600)

**Elixir:** starts 5, max 10, regenerates at 2.8/sec.

**Win:** Destroy enemy King Tower, or most towers after 3-minute timer.

**8 Cards (fixed deck, no selection in MVP):**

| Card | Elixir | HP | DMG | Speed | Range | Targets | Notes |
|---|---|---|---|---|---|---|---|
| Knight | 3 | 1200 | 120 | 60 | 1.2 | Ground | Tanky melee |
| Archer | 3 | 340 | 60 | 60 | 5.0 | All | Ranged, spawns 2 |
| Giant | 5 | 3000 | 140 | 40 | 1.2 | Buildings | Ignores troops |
| Fireball | 4 | — | 600 | — | AoE r=3.5 | All | Instant spell |
| Minions | 3 | 240 | 55 | 80 | 2.0 | All | Flying, spawns 3 |
| Barbarians | 5 | 900 | 100 | 60 | 1.2 | Ground | Spawns 4 |
| Musketeer | 4 | 580 | 180 | 60 | 6.0 | All | Long range |
| Mini PEKKA | 4 | 1200 | 380 | 60 | 1.2 | Ground | Slow, high DMG |

## Server Architecture

**Authoritative model:** server runs all simulation. Clients send actions only; render what server says.

**Game loop:** 20 ticks/sec (50ms/tick) via Ktor coroutine in `GameRoom`.

**Tick order:** advance elixir → decrement timer → move/attack units → tower auto-attack → remove dead → check win → broadcast `GAME_STATE_UPDATE`

**Key classes to build:**
- `GameRoom` — holds two `WebSocketServerSession`s, owns `GameState`, runs the loop
- `GameLogic` — pure functions: `tick()`, `applyCardPlay()`, movement, combat
- `CardValidator` — rejects `NOT_ENOUGH_ELIXIR`, `INVALID_PLACEMENT`, `INVALID_CARD`
- `MatchmakingService` — `ConcurrentLinkedQueue`; when size ≥ 2, creates `GameRoom`
- `Messages.kt` — sealed classes for all WebSocket message types

## Message Protocol

All messages: JSON with `"type"` discriminator field. Use `kotlinx.serialization` sealed classes with `classDiscriminator = "type"`.

**Client → Server:**
```json
{ "type": "JOIN_QUEUE", "playerId": "device-uuid" }
{ "type": "PLAY_CARD", "card": "KNIGHT", "x": 512.0, "y": 620.0 }
{ "type": "LEAVE_MATCH" }
```

**Server → Client:**
```json
{ "type": "MATCH_FOUND", "roomId": "abc123", "playerIndex": 0 }
{ "type": "GAME_STATE_UPDATE", "tick": 142, "timeRemainingMs": 163000, "myElixir": 7.4, "opponentElixir": 5.1, "units": [...], "towers": [...] }
{ "type": "GAME_ENDED", "winner": 0, "reason": "KING_DESTROYED" }
{ "type": "CARD_PLAY_REJECTED", "reason": "NOT_ENOUGH_ELIXIR" }
```

## Client Architecture

**Thread safety:** WebSocket runs on coroutine dispatcher; LibGDX renders on its own thread. Use `AtomicReference<ClientGameState>` — network writes swap atomically, render reads with `.get()`.

**Coordinate mapping:** `scale = screenHeight / 1800f`. All server world coords × scale before drawing.

**Screen flow:** MenuScreen → MatchmakingScreen → GameScreen → ResultScreen

**Card placement UX:** tap card → SELECTING mode → tap arena (own half) → send PLAY_CARD

**Rendering (MVP):** LibGDX `ShapeRenderer` with colored shapes. Replace with sprites in polish pass.

**Key classes to build:**
- `GameWebSocketClient` — singleton, manages connection + reconnect
- `MessageHandler` — dispatches inbound JSON to typed handlers
- `ArenaRenderer` — draws towers, units, HP bars via `ShapeRenderer`
- `CardHandUI` — bottom tray, grays out cards you can't afford
- `CardPlacementController` — handles tap-select-place flow

## Implementation Milestones

1. Gradle skeleton compiles, desktop window opens
2. Server WebSocket — accepts connections, echoes messages
3. Message protocol — sealed classes serialize/deserialize (unit tests)
4. Matchmaking — two clients connect, both receive MATCH_FOUND
5. Server game loop — ticks at 20/sec, elixir increments, timer counts down
6. Unit simulation — spawn, move, attack, die; tower combat; win detection
7. Card validation — CardValidator + Fireball spell; unit tests
8. **Client network — connects to server, receives state, logs to console ← FIRST E2E TEST**
9. Arena rendering — ShapeRenderer draws units, towers, HP bars, elixir bar, timer
10. Card hand UI + input — select card, tap to place, send PLAY_CARD
11. Full screen flow — all screen transitions working
12. Android build — runs on physical device, connects to local server over Wi-Fi
13. Cloud deploy — server on Railway, two phones on different networks play a match
14. Polish — sprites, sounds, stat tuning

## Deployment (Railway)

- Add `com.github.johnrengelman.shadow` plugin for fat JAR
- `Procfile`: `web: java -jar server/build/libs/server-all.jar`
- Port: `System.getenv("PORT")?.toInt() ?: 8080` in `Application.kt`
- Health check: `GET /health → 200 OK`

## Developer Notes

- User has strong Java/Kotlin backend background, limited mobile/game experience
- LibGDX was chosen over Jetpack Compose (Compose has no game loop or sprite batching)
- LibGDX `ShapeRenderer` and `SpriteBatch` cannot be used in the same `begin()`/`end()` block
- Android `minSdk = 26` (Android 8.0), `compileSdk = 35`
- The `copyAndroidNatives` Gradle task unpacks LibGDX `.so` files into `game/android/libs/<abi>/`
