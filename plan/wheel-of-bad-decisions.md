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

## UI/UX Design

### Visual Language

**Theme:** Loud & colorful — carnival/game-show energy. Bright colors, bold black outlines, thick borders, drop shadows. Think Wheel of Fortune meets a beer pong table.

**CSS Framework:** Tailwind CSS

**Color palette:**

| Token | Value | Use |
|-------|-------|-----|
| Background | `#FFF9E6` warm cream | Page background |
| Wheel Red | `#EF4444` | Segment |
| Wheel Blue | `#3B82F6` | Segment |
| Wheel Green | `#22C55E` | Segment |
| Wheel Orange | `#F97316` | Segment |
| Wheel Purple | `#A855F7` | Segment |
| Wheel Pink | `#EC4899` | Segment |
| Challenge card | `#FBBF24` yellow | ChallengeCard bg |
| Text | `#1F2937` | Dark body text |
| Border | `#000000` 3px | All cards/wheels |

**Typography:** Fredoka One (Google Fonts) for all headings and challenge text — rounded, bold, playful. Nunito for supporting body text. Max 3 font sizes per screen; prefer weight over size for hierarchy.

---

### Host Screen (landscape — TV / laptop)

```
┌─────────────────────────────────────────────────┐
│  🎡 Wheel of Bad Decisions   ⏱ 14:32   ABC123 📷 │  ← top bar
├────────────────────┬────────────────────────────┤
│                    │                            │
│   WHO WHEEL        │   WHAT WHEEL               │  ← main area
│   (player names)   │   (challenge type)         │
│                    │                            │
│    ▼ pointer       │    ▼ pointer               │
│   /~~~~~\          │   /~~~~~\                  │
│  / R | B \         │  /🍺 | 😈\                 │
│ | G  +  Y |        │ |DRINK+DARE|               │
│  \ P | O /         │  \🌍 | 🎯/                 │
│   \~~~~~/          │   \~~~~~/                  │
│                    │                            │
├────────────────────┴────────────────────────────┤
│  📋 Active rules: no swearing · left hand only  │  ← rules ticker
└─────────────────────────────────────────────────┘
         ↕ toggleable right sidebar
┌─────────────────────────┐
│ Players                 │
│ 🟢 Christian   🍺🍺🍺  │
│ 🟡 Alex (away) 🍺       │
│ 🟢 Jo          🍺🍺    │
└─────────────────────────┘
```

- Result card pops in **center overlay** between both wheels on spin result (scale + fade in, z-index over both wheels)
- Player panel toggled by host with a button — hidden by default to keep screen clean
- Countdown bar runs across the top below the header; color transitions green → yellow → red

---

### Player Screen (portrait — mobile)

**Idle state:**
```
┌───────────────────┐
│  Wheel of Bad 🎡  │
│  Hi, Christian!   │
│                   │
│   Next spin in    │
│   ┌───────────┐   │
│   │  14 : 32  │   │
│   └───────────┘   │
│                   │
│  Your power-ups:  │
│ [🗡 Revenge][🛡 Safe][🎲 x2] │
└───────────────────┘
```

**Selected state (full-screen takeover, triggered by SPIN_RESULT with your playerId):**
```
┌───────────────────┐
│  🔥 IT'S YOU! 🔥  │  ← flash animation entry
│                   │
│  ┌─────────────┐  │
│  │  🍺 DRINK   │  │  ← ChallengeCard — large, centered
│  │  3 SIPS     │  │
│  └─────────────┘  │
│                   │
│  ┌───────┐ ┌────┐ │
│  │ DONE ✓│ │ ✗  │ │  ← min 44px tap target
│  └───────┘ └────┘ │
│  [🗡 Revenge][🛡 Safe][🎲 x2] │
└───────────────────┘
```

**Voting state (you're not selected — watching):**
```
┌───────────────────┐
│  Christian got:   │
│  DRINK 3 SIPS     │
│                   │
│  Did they do it?  │
│  [✅ DONE] [❌ NO] │
│  [👎 BOO]         │
└───────────────────┘
```

---

### Wheel Component (SpinWheel.vue)

- **SVG-based** (not Canvas) — scales cleanly to any screen size, accessible
- Segments drawn as SVG `<path>` elements with `clipPath`
- Bold 3px black stroke between segments
- Center hub: white circle, game logo or emoji
- Pointer: fixed SVG triangle at 12 o'clock, outside the wheel, bold black with drop shadow
- Segment text: Fredoka One, white, radially oriented, truncated at 12 chars

---

### Animation Sequence (high drama)

**At SPIN_WARNING (T-5s):**
1. Screen darkens slightly (overlay `bg-black/20`)
2. Drumroll audio starts
3. Large 5→1 countdown flashes center screen

**Spin phase:**
1. Both wheels begin rotating simultaneously
2. Fast initial velocity (easing: `cubic-bezier(0.2, 0, 0.8, 1)`)
3. Deceleration phase: `cubic-bezier(0, 0, 0.2, 1)` over final 1.5s
4. Total duration: ~4s
5. Lands on server-determined segment

**At result:**
1. White screen flash (200ms, `opacity-100 → 0`)
2. Airhorn / result sound sting
3. Result card scales in from center (`scale-0 → scale-100`, 300ms spring)
4. Confetti burst (`canvas-confetti` library)
5. Player name and challenge text animate in with stagger

**Sound controls:**
- Mute button always visible on host screen (top-right)
- Each player can mute on their device independently
- Sounds: `drumroll.mp3`, `airhorn.mp3`, `tick.mp3` (wheel ticks during deceleration)

---

### Stats Screen

Full-width card grid, one card per player. Each card shows:
- Player name (large, Fredoka One)
- Times selected, completed, refused
- Estimated sips
- Title badge (e.g. "🏆 Party Animal", "🐔 Biggest Coward")

Confetti plays on page load. "Play again" button creates a new room.

---

## Wheel Animation

- SVG wheel; rotation via CSS `transform: rotate()` applied to the SVG group
- Two-phase easing: fast start `cubic-bezier(0.2, 0, 0.8, 1)` → deceleration `cubic-bezier(0, 0, 0.2, 1)` over final 1.5s, total ~4s
- Result angle computed server-side and sent in SPIN_RESULT; animation is purely cosmetic
- Both wheels spin simultaneously
- Sound files loaded at game start to avoid latency: `drumroll.mp3`, `airhorn.mp3`, `tick.mp3`
- Per-device mute button; host mute button on HostView

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

### Cold start / spin-down mitigation

Render free tier sleeps after 15 min with no HTTP requests. WebSocket connections (even active ones) do not reset this timer. At a 30-min spin interval, the server can spin down mid-game and lose all in-memory state.

**Solution: host screen self-ping.** `HostView.vue` runs a `setInterval` every 10 minutes that calls `GET /api/rooms/{code}`. This keeps Render warm only while the party is active, needs no external service, and acts as a connectivity check (redirect to error if the room is gone). Interval started in `onMounted`, cleared in `onUnmounted`.

### Disconnection handling

Players who disconnect are **not removed from the wheel** — their name stays and they're still a valid spin target. The host screen shows disconnected players in a greyed state. On reconnect, the player re-opens their URL: `GET /api/rooms/{code}` returns current game state and the WebSocket re-subscribes. Power-ups and voting require an active connection; everything else (being selected, having challenges read aloud by the group) works without it. The host screen is the primary game surface — the game is never blocked by player disconnections.

---

## Implementation Phases

### Phase 1 — Project scaffold
- Spring Boot project init (Spring Web, WebSocket, Lombok)
- Vue 3 + TypeScript project init (Vite, Pinia, Vue Router, SockJS + STOMP)
- Tailwind CSS configured in Vite
- Google Font: Fredoka One + Nunito loaded via `index.html`
- `canvas-confetti` npm package added
- Sound assets: `drumroll.mp3`, `airhorn.mp3`, `tick.mp3` placed in `public/sounds/`
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
- Self-ping: `setInterval` in `onMounted` calling `GET /api/rooms/{code}` every 10 min, cleared in `onUnmounted`
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
