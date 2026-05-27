# Wheel of Bad Decisions — Game Design & Implementation Plan

## Context

Greenfield drinking game app. Core loop: a shared host screen shows a dual spinning wheel that fires every 15 or 30 minutes. One wheel picks a player, the other picks a punishment. Players join from their own phones via QR code / room code and can interact in real-time (use power-ups, vote on challenge completion, react). End of night shows a full stats screen. Deployed as a single service on Render.

---

## Game Design

### Core Loop

1. Host creates a room → gets a 6-char room code + QR code
2. Players join on phones → enter name → see their personal "player view"
3. Host starts the game → countdown timer begins (15 or 30 min)
4. Timer fires → both wheels spin simultaneously on the host screen
5. Wheel 1 (names) → selects a player
6. Wheel 2 (punishment) → selects challenge type
7. Challenge displayed on host screen AND on selected player's phone
8. Players interact: vote, use power-ups, react
9. Repeat until host ends the game → stats screen

### Wheel 2 — Challenge Types (weighted, configurable by intensity)

| Type | Description |
|------|-------------|
| DRINK | Drink 1–5 sips (randomized) |
| SOCIAL | Everyone drinks 1–2 sips |
| GIVE_TAKE | Give or take sips from a player you choose |
| TRUTH | Random question from preset list |
| DARE | Random dare from preset list |
| CHALLENGE | Mini physical challenge (preset list) |

**Intensity levels** affect weights:
- Chill: more SOCIAL/GIVE_TAKE, low sip counts
- Normal: balanced mix
- Savage: more DARE/CHALLENGE, higher sip counts, harder truths

### Player Power-Ups (each player gets 1 of each at game start)

| Power-Up | Effect |
|----------|--------|
| Revenge Token | Redirect a spin you were selected for to another player |
| Safe Card | Skip a spin entirely — one-time use |
| Double Down | 50/50 gamble: double the punishment OR get off free |

### Voting / Boo System

After a challenge is displayed, a 30-second voting window opens. Players vote DONE or REFUSED on their phone. If majority votes REFUSED, player drinks an extra 2 sips. Audience can also send a "BOO" emoji reaction during voting.

### Active Rules

When DARE or CHALLENGE lands, selected player may optionally impose a rule on the group for the rest of the night (e.g., "no real names", "drink with left hand"). Rules are shown persistently on the host screen. Breaking a rule = drink.

### End-of-Night Stats

- Times selected per player
- Challenges completed vs. refused
- Total sips (estimated)
- Titles awarded: "Iron Stomach", "Biggest Coward", "Party Animal", "The Unlucky One", "Rule Setter"

---

## Architecture

### Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.x (Java 21) |
| Frontend | Vue 3 + TypeScript, Pinia, Vue Router |
| Realtime | WebSocket (STOMP over SockJS) |
| State | In-memory (no DB — game state lives one night) |
| Deployment | Single Render Web Service (Vue built into Spring Boot static resources) |

### Why single service on Render

Avoids CORS complexity, one deployment to manage, Vue dist folder served from Spring Boot's `static/` resources. Simpler for a party-night app with no persistence requirement.

---

## Backend Structure

```
com.remstem.game
├── config/
│   └── WebSocketConfig.java          # STOMP broker, SockJS endpoint
├── controller/
│   ├── RoomController.java           # REST: create/join/start/end room
│   └── GameSocketController.java     # @MessageMapping: power-ups, votes
├── model/
│   ├── Room.java                     # roomCode, players, state, config, history
│   ├── Player.java                   # id, name, powerUps, stats
│   ├── Challenge.java                # type enum + payload
│   ├── SpinResult.java               # playerId + challenge
│   └── GameState.java                # LOBBY | ACTIVE | PAUSED | ENDED
├── service/
│   ├── RoomService.java              # room lifecycle, player management
│   ├── SpinService.java              # wheel randomization logic
│   └── ChallengeService.java         # challenge pool, truth/dare lists
└── scheduler/
    └── SpinScheduler.java            # per-room ScheduledFuture, fires spin
```

### REST Endpoints

```
POST   /api/rooms                     → create room, returns {roomCode, hostToken}
GET    /api/rooms/{code}              → room state (for reconnection)
POST   /api/rooms/{code}/players      → join room, body: {name}
PUT    /api/rooms/{code}/config       → update interval/intensity (host only)
POST   /api/rooms/{code}/start        → start game, begin scheduler
POST   /api/rooms/{code}/end          → end game, return stats
DELETE /api/rooms/{code}/players/{id} → remove player (host only)
```

### WebSocket Channels (STOMP)

```
Subscribe /topic/rooms/{code}         → broadcast to all in room
Subscribe /user/queue/private         → private events (you were selected, power-up result)

Send /app/rooms/{code}/vote           → submit DONE | REFUSED | BOO
Send /app/rooms/{code}/powerup        → use power-up {type, targetId?}
Send /app/rooms/{code}/rule           → add a rule string (post-challenge)
```

### Server-Sent Events (broadcast types)

```
PLAYER_JOINED      {player}
PLAYER_DISCONNECTED {playerId}           # player stays on wheel; host shows greyed name
GAME_STARTED       {config}
COUNTDOWN          {secondsUntilSpin}
SPIN_WARNING       {}                    # fired 5s before spin — alarm + visual flash
SPIN_STARTING      {}
SPIN_RESULT        {spinResult: {player, challenge}}
VOTE_WINDOW_OPEN   {durationSeconds: 30}
VOTE_RESULT        {completed: boolean, extraDrinks: number}
POWERUP_USED       {playerId, type, targetId?}
RULE_ADDED         {rule: string, addedBy: string}
GAME_ENDED         {stats}
```

---

## Frontend Structure

```
src/
├── router/index.ts
├── stores/
│   └── gameStore.ts                  # Pinia: room, player, spin state
├── composables/
│   └── useWebSocket.ts               # STOMP client, subscribe/send helpers
├── views/
│   ├── HomeView.vue                  # Create or join game
│   ├── HostView.vue                  # Big screen: wheels + countdown + rules
│   ├── PlayerView.vue                # Mobile: challenge card + power-up buttons + vote
│   └── StatsView.vue                 # End-of-night results
└── components/
    ├── SpinWheel.vue                 # Animated wheel (CSS rotate + easing)
    ├── ChallengeCard.vue             # Displays active challenge
    ├── CountdownBar.vue              # Time until next spin
    ├── PowerUpBar.vue                # Player's available power-ups
    ├── VotePanel.vue                 # DONE / REFUSED / BOO buttons
    ├── ActiveRules.vue               # Persistent rules list
    └── QRCodeDisplay.vue             # Room join QR code
```

### Routes

```
/                  HomeView       create or join
/host/:code        HostView       full-screen display
/play/:code        PlayerView     mobile player interface
/stats/:code       StatsView      end-of-night stats
```

---

## Wheel Animation

- CSS `transform: rotate()` with `transition: transform 3s cubic-bezier(0.17, 0.67, 0.12, 0.99)`
- Spin duration ~3–4 seconds
- Result determined server-side before animation starts; animation is cosmetic
- Both wheels spin simultaneously; results revealed after animation completes
- Sound: optional spin sound + result sting (can be muted per device)

---

## Deployment — Render

### Build strategy (single service)

Maven `frontend-maven-plugin` builds Vue during `mvn package` and copies `dist/` into `src/main/resources/static/`. Spring Boot serves everything.

### Render config (`render.yaml`)

```yaml
services:
  - type: web
    name: wheel-of-bad-decisions
    env: java
    buildCommand: mvn package -DskipTests
    startCommand: java -jar target/game-*.jar
    envVars:
      - key: PORT
        sync: false
```

### Cold start mitigation

Render free tier sleeps after 15 min idle. UptimeRobot pings `/api/health` every 14 min — this runs 24/7 regardless of whether anyone is using the site. Pause/resume the monitor manually before and after a party, or upgrade to Render paid ($7/mo) to skip this entirely.

### Disconnection handling

Players who disconnect are **not removed from the wheel** — their name stays and they're still a valid spin target. The host screen shows disconnected players in a greyed state. On reconnect, the player re-opens their URL: `GET /api/rooms/{code}` returns current game state and the WebSocket re-subscribes. Power-ups and voting require an active connection; everything else (being selected, having challenges read aloud by the group) works without it. The host screen is the primary game surface — the game is never blocked by player disconnections.

---

## Implementation Phases

### Phase 1 — Project scaffold
- Spring Boot project init (Spring Web, WebSocket, Lombok)
- Vue 3 + TypeScript project init (Vite, Pinia, Vue Router, SockJS + STOMP)
- Maven frontend plugin wiring
- `render.yaml`

### Phase 2 — Core backend
- Room + Player models
- RoomService (create, join, start, end)
- SpinService (weighted random challenge selection)
- ChallengeService (preset truth/dare/challenge pools)
- SpinScheduler (per-room ScheduledFuture; fires SPIN_WARNING at T-5s, then SPIN_STARTING)
- REST endpoints
- WebSocket config + GameSocketController
- Disconnect detection via WebSocket session listener → PLAYER_DISCONNECTED broadcast

### Phase 3 — Host view (big screen)
- HomeView (create game, show QR code)
- HostView: SpinWheel ×2, CountdownBar, ActiveRules, PlayerList
- WebSocket integration in gameStore

### Phase 4 — Player view (mobile)
- PlayerView: join flow, ChallengeCard, PowerUpBar, VotePanel
- Private WebSocket channel for selected-player notifications
- Power-up use flows (revenge redirect, safe skip, double down)

### Phase 5 — Stats + polish
- StatsView with titles/badges
- Sound effects (mutable)
- Intensity selector in lobby
- End-to-end test on mobile browser

---

## Verification

1. Run backend: `mvn spring-boot:run`
2. Run frontend dev: `npm run dev` (proxy `/api` and `/ws` to backend)
3. Open host screen at `http://localhost:5173/`
4. Open two browser tabs simulating player phones
5. Create room → both players join → start game → verify spin fires, both wheels animate, challenge appears on player view
6. Test each power-up type
7. Vote REFUSED → verify extra drinks shown
8. End game → verify stats screen
9. `mvn package` → verify Vue is bundled into JAR → run JAR → verify full flow on single port
