# 🎡 Wheel Unfortunate

A real-time multiplayer drinking game. One shared host screen runs the show — two wheels spin simultaneously to pick a player and a punishment. Players join from their phones via QR code and interact live.

---

## How It Works

1. Host opens the app and creates a room → gets a 6-character code + QR code
2. Players scan the QR or type the code on their phones and enter their name
3. Host starts the game → a countdown begins (15 or 30 min intervals)
4. Timer fires → both wheels spin on the host screen
5. **Wheel 1** lands on a player. **Wheel 2** lands on a challenge type
6. Challenge is shown on the host screen and on the selected player's phone
7. 30-second voting window opens — all other players vote DONE / REFUSED / BOO
8. Majority REFUSED → +2 extra sips for the selected player
9. Repeat until host ends the game → stats screen with titles and sip counts

### Challenge Types

| Type | What happens |
|------|-------------|
| DRINK | Drink 1–5 sips |
| SOCIAL | Everyone drinks |
| GIVE/TAKE | Give or take sips from a player you choose |
| TRUTH | Random truth question |
| DARE | Random dare |
| CHALLENGE | Mini game (physical, verbal, memory, creative, head-to-head) |
| HOT SEAT | Group interrogates the selected player for 3 questions |
| MOST LIKELY TO | Selected player reads a prompt; group votes by pointing; most votes = drink |

Intensity levels (Chill / Normal / Savage) shift the weights and the content pool.

### Power-Ups

Each player starts with one of each:

| Power-Up | Effect |
|----------|--------|
| ⚔️ Revenge | Redirect a spin you were selected for to another player |
| 🛡️ Safe Card | Skip a spin entirely |
| 🎲 Double Down | 50/50 gamble — double the punishment or get off free |

---

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.3 (Java 21) |
| Frontend | Vue 3 + TypeScript, Pinia, Vue Router |
| Realtime | WebSocket (STOMP over SockJS) |
| State | In-memory — no database |
| Deployment | Single Render Web Service (Docker) |

Vue's `dist/` is copied into Spring Boot's static resources at build time. One service, one port, no CORS.

---

## Running Locally

**Prerequisites:** Java 21, Maven 3.9+, Node 20+

```bash
# Terminal 1 — backend
mvn spring-boot:run

# Terminal 2 — frontend (proxies /api and /ws to :8080)
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in a browser for the host screen.  
Open `http://localhost:5173` on a phone (same network) and join with the room code.

### Sound files

Place these in `frontend/public/sounds/` — the game runs without them, just silently:

- `drumroll.mp3` — plays at the 5-second spin warning
- `airhorn.mp3` — plays when the result is revealed
- `tick.mp3` — wheel tick during deceleration

---

## Building for Production

```bash
docker build -t wheel-unfortunate .
docker run -p 8080:8080 wheel-unfortunate
```

The Dockerfile runs a 3-stage build: Node builds the Vue frontend, Maven copies the dist into `src/main/resources/static/` and packages the JAR, the final image is a slim JRE + JAR only.

---

## Deployment (Render)

The repo includes a `render.yaml` configured for a Docker web service.

1. Push to GitHub
2. Create a new Render Web Service → connect the repo → select "Docker" environment
3. Set the `PORT` environment variable (Render injects this automatically)
4. Deploy

**Cold start note:** Render's free tier sleeps after 15 minutes of no HTTP traffic. WebSocket connections alone don't reset this timer. The host screen pings `GET /api/rooms/{code}` every 10 minutes while the game is active to keep the instance warm. No external service needed.

---

## Project Structure

```
wheel-unfortunate/
├── Dockerfile
├── render.yaml
├── pom.xml
├── frontend/                        # Vue 3 + TypeScript (Vite)
│   ├── src/
│   │   ├── views/
│   │   │   ├── HomeView.vue         # Create or join
│   │   │   ├── HostView.vue         # Big screen — wheels, countdown, rules
│   │   │   ├── PlayerView.vue       # Mobile — challenge card, power-ups, vote
│   │   │   └── StatsView.vue        # End-of-night results
│   │   ├── components/
│   │   │   ├── SpinWheel.vue        # SVG wheel with CSS animation
│   │   │   ├── ChallengeCard.vue
│   │   │   ├── CountdownBar.vue
│   │   │   ├── PowerUpBar.vue
│   │   │   ├── VotePanel.vue
│   │   │   ├── ActiveRules.vue
│   │   │   └── QRCodeDisplay.vue
│   │   ├── stores/gameStore.ts      # Pinia — all game state + WS event handler
│   │   └── composables/
│   │       ├── useWebSocket.ts      # STOMP client
│   │       └── useSound.ts          # Audio preload + play
│   └── public/sounds/               # drumroll.mp3, airhorn.mp3, tick.mp3
└── src/main/java/com/remstem/game/
    ├── config/WebSocketConfig.java  # STOMP broker, disconnect listener
    ├── controller/
    │   ├── RoomController.java      # REST endpoints
    │   ├── GameSocketController.java # STOMP: vote, power-up, rule, register
    │   └── SpaController.java       # Vue Router fallback
    ├── model/                        # Room, Player, Challenge, SpinResult, …
    ├── service/
    │   ├── RoomService.java
    │   ├── SpinService.java         # Weighted random + wheel angle calculation
    │   └── ChallengeService.java    # Loads content JSON at startup
    └── scheduler/SpinScheduler.java # Per-room ScheduledFuture
```

---

## API Reference

### REST

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/rooms` | Create room → `{roomCode, hostToken}` |
| GET | `/api/rooms/{code}` | Room state (reconnection) |
| POST | `/api/rooms/{code}/players` | Join room, body: `{name}` |
| PUT | `/api/rooms/{code}/config` | Update interval/intensity |
| POST | `/api/rooms/{code}/start` | Start game |
| POST | `/api/rooms/{code}/end` | End game → stats |
| DELETE | `/api/rooms/{code}/players/{id}` | Remove player (host) |

### WebSocket (STOMP)

Subscribe to `/topic/rooms/{code}` for all room broadcasts.  
Subscribe to `/user/queue/private` for private events.

**Send:**
- `/app/rooms/{code}/vote` — `{vote: "DONE"|"REFUSED"|"BOO", playerId}`
- `/app/rooms/{code}/powerup` — `{playerId, type, targetId?}`
- `/app/rooms/{code}/rule` — `{rule, addedBy, playerId}`

**Receive event types:** `PLAYER_JOINED`, `PLAYER_DISCONNECTED`, `GAME_STARTED`, `COUNTDOWN`, `SPIN_WARNING`, `SPIN_STARTING`, `SPIN_RESULT`, `VOTE_WINDOW_OPEN`, `VOTE_RESULT`, `POWERUP_USED`, `RULE_ADDED`, `GAME_ENDED`
