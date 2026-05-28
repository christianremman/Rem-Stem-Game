# Wheel Unfortunate — Game Design & Implementation Plan

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
| CHALLENGE | Mini game from preset list (physical, verbal, memory, creative, head-to-head) |
| HOT_SEAT | Group asks 3 live questions back to back — no preset list, pure social pressure |
| MOST_LIKELY_TO | Selected player reads a "most likely to..." prompt; group votes by pointing; most votes = drink |

**Intensity levels** affect weights:
- Chill: more SOCIAL/GIVE_TAKE/HOT_SEAT, low sip counts, tame truths and dares
- Normal: balanced mix across all types
- Savage: more DARE/CHALLENGE/MOST_LIKELY_TO, higher sip counts, hard truths, embarrassing dares

---

### Content Library

Content lives in `src/main/resources/content/` as JSON. Each item has an `intensity` field (`CHILL` | `NORMAL` | `SAVAGE`) and a `subtype` field. `ChallengeService` loads all files at startup and filters by intensity on spin.

#### TRUTH — Sub-types
| Sub-type | What it asks |
|----------|-------------|
| PERSONAL_HISTORY | "Have you ever…" — past events |
| THIS_ROOM | Questions about relationships with people present |
| CONFESSION | Something you did / thought / felt |
| RANKING | Rank people in the room on something |

#### DARE — Sub-types
| Sub-type | What it asks |
|----------|-------------|
| PERFORMANCE | Sing, dance, act, impersonate |
| PHONE_SOCIAL | Text, call, post, or show something on your phone |
| PHYSICAL | Push-ups, balance, cartwheels, plank |
| ROLEPLAY | Accent, character, persona to maintain |

#### CHALLENGE — Sub-types
| Sub-type | Format |
|----------|--------|
| SPEED | Complete X in Y seconds |
| SKILL_PHYSICAL | Balance, coordination, or strength feat |
| MEMORY | Memorize and recall under pressure |
| VERBAL | Tongue twisters, rhyming, backwards alphabet |
| HEAD_TO_HEAD | Competitive — arm wrestle, staring contest |
| ENDURANCE | Hold a position or composure for duration |
| CREATIVE | Draw, build, or narrate something |

---

#### TRUTH Content

**Chill (10)**
1. What's the most embarrassing thing you've ever done in public?
2. Have you ever pretended to be sick to get out of something?
3. What's the most childish thing you still do?
4. Have you ever sent a message to the completely wrong person? What did it say?
5. What's a weird habit you have that nobody in this room knows about?
6. What's the most embarrassing photo currently on your phone?
7. Have you ever laughed so hard something embarrassing happened?
8. What's the worst lie you've told your parents?
9. What's something you hate doing but have to pretend you enjoy?
10. What's the strangest dream you've had in the last month?

**Normal (10)**
1. What's something you've done that no one in this room knows about?
2. Have you ever ghosted someone? What happened?
3. Who in this room would you trust least with a secret?
4. What's your most embarrassing drunk story?
5. Have you ever liked someone romantically who is in this room right now?
6. Have you ever talked badly about someone who is in this room?
7. What's your biggest insecurity?
8. What's the pettiest reason you've ended a friendship or stopped talking to someone?
9. What's the most desperate thing you've done to get someone's attention?
10. What's the worst date you've ever been on?

**Savage (10)**
1. Who in this room would you choose if you absolutely had to kiss someone here?
2. What's the most embarrassing thing you've ever been caught doing?
3. What's something you've done that you're genuinely ashamed of?
4. Have you ever cheated — in a relationship, on a test, or at a game?
5. What's the biggest lie you've told someone you were with romantically?
6. Have you ever stolen anything? What was it?
7. What's the most NSFW thought you've had about someone in this room?
8. Who here is most likely to get arrested in the next 5 years, and why?
9. What's your most controversial opinion about one of the people here — say it directly to them?
10. Have you ever done something illegal? Give us at least one detail.

---

#### DARE Content

**Chill (10)**
1. Do your best impression of someone in this room for 30 seconds — group votes on accuracy.
2. Speak in an accent chosen by the group for the next 2 rounds.
3. Give a sincere compliment to every single person in the room, one by one.
4. Do 10 jumping jacks while singing the chorus of a song chosen by the group.
5. Talk in slow motion for the next 3 minutes — 1 sip each time you forget.
6. Let the person to your left write any caption on your last Instagram post (they show you before posting).
7. Show the group the most embarrassing photo on your phone.
8. Do your best model runway walk across the room — full commitment required.
9. Let the group give you a nickname you must use for the rest of the game.
10. Call a friend, say "I have news — I'll explain tomorrow," and immediately hang up.

**Normal (10)**
1. Let each player draw one mark on your face with a washable marker.
2. Speak only in questions for 5 minutes — 1 sip per statement.
3. Let the group look through your camera roll for 30 seconds.
4. Do your best stand-up comedy bit — minimum 1 minute; group claps to pass.
5. Call someone and speak only in a foreign accent for 30 seconds.
6. Read the last text message from 3 apps on your phone — group picks which apps.
7. Hold a plank for 60 seconds — 1 sip per 10 seconds short.
8. Let the group post one Instagram story on your behalf — they write it, you approve before sending.
9. Impersonate a celebrity chosen by the group for 60 seconds without breaking character.
10. Deliver a 1-minute fake movie trailer narration for a film title the group invents.

**Savage (10)**
1. Text your most recent ex "I've been thinking about you" — show the group before sending.
2. Let the group change your phone wallpaper to anything they choose for the rest of the night.
3. Let the group compose and send one message from your phone to a contact they choose (content must stay SFW).
4. Share your last 5 Google searches with the group.
5. Let the group scroll your dating app for 30 seconds — no dating app = drink 3 sips.
6. Call a family member and sing Happy Birthday to them regardless of the date — full song, no stopping.
7. Post a photo on your main social media with a caption written entirely by the group — keep it up for 10 minutes.
8. Do a 30-second seductive dance directed at whoever the group picks.
9. Tell the group your most embarrassing story involving a romantic interest — minimum 2 minutes.
10. Demonstrate what you look like when you're flirting, live, targeted at whoever the group chooses.

---

#### CHALLENGE Content

**Chill (10)**
1. *(VERBAL)* Name 10 animals in 15 seconds — 1 sip per miss.
2. *(VERBAL)* Say "toy boat" 5 times fast — stumble once = 1 sip.
3. *(SKILL_PHYSICAL)* Balance a coin on your nose for 10 seconds — fail = 1 sip.
4. *(SKILL_PHYSICAL)* Do 15 push-ups without stopping — 1 sip per rep short.
5. *(MEMORY)* Memorize 5 random items shown to you, look away, recite them — 1 sip per miss.
6. *(VERBAL)* Say the alphabet backwards in 30 seconds — fail = 2 sips.
7. *(CREATIVE)* Whistle a recognizable song — group guesses in 30 seconds or you drink 1 sip.
8. *(ENDURANCE)* Keep a completely straight face for 30 seconds while the group tries to make you laugh — laugh = 2 sips.
9. *(MEMORY)* Name every player currently in the room in under 5 seconds — 1 sip per miss.
10. *(SKILL_PHYSICAL)* Attempt 3 cartwheels — refuse entirely = drink 3 sips.

**Normal (10)**
1. *(CREATIVE)* Draw a portrait of the person to your right in 60 seconds — group rates 1–10; below 5 = drink 2 sips.
2. *(HEAD_TO_HEAD)* Arm wrestle the person the group picks — loser drinks 2 sips.
3. *(SPEED)* Do 20 burpees in 45 seconds — 1 sip per rep short, max 5.
4. *(VERBAL)* Speak only in rhyme for 2 minutes — 1 sip per failure, each subsequent failure +1 extra sip.
5. *(MEMORY)* Name 5 films starring an actor the group picks in 20 seconds — fail = 2 sips.
6. *(ENDURANCE)* Hold a tree pose (yoga) for 45 seconds — 1 sip per stumble.
7. *(CREATIVE)* Build the tallest tower possible with 10 cups in 30 seconds — must stand for 5 seconds unassisted.
8. *(CREATIVE)* Tell a story incorporating 5 random words the group picks — minimum 60 seconds, group votes pass/fail.
9. *(PERFORMANCE)* Sing the full chorus of a song the group picks — 1 sip per line missed.
10. *(SKILL_PHYSICAL)* Mirror the movements of the person next to you for 90 seconds — they lead, you follow — diverge = 1 sip.

**Savage (10)**
1. *(ENDURANCE)* Finish the rest of your current drink in under 10 seconds — fail = group adds to it.
2. *(ENDURANCE)* Let the group blindfold you and feed you a mystery item from the table — refuse = 5 sips.
3. *(SPEED)* Do 25 squats counting out loud — miscount or stop = 1 sip and restart from zero.
4. *(VERBAL)* Group picks a word you must use naturally in every sentence for 5 minutes — each miss = 1 sip.
5. *(MEMORY)* Speed trivia: group asks 5 questions, 8 seconds each — 1 sip per wrong answer.
6. *(HEAD_TO_HEAD)* Staring contest with whoever the group picks — first to blink drinks 3 sips immediately.
7. *(SKILL_PHYSICAL)* Hold a handstand against the wall for 15 seconds — fail = 3 sips.
8. *(VERBAL)* Recite the chorus of 3 songs in 90 seconds — group picks all 3 — fail any = 1 sip each.
9. *(ENDURANCE)* Stand on one leg for 60 seconds while someone reads you a distracting story — fall = 3 sips.
10. *(MEMORY)* Group invents a secret handshake with you in 45 seconds — you must perform it flawlessly 5 minutes later — fail = 3 sips.

---

#### HOT_SEAT Prompts (10)
These appear on the host screen as instructions to the group, not as text for the selected player.

1. Ask the player about their most recent relationship status — 3 questions minimum.
2. The group votes on the player's most likely personality flaw — then each player defends their vote.
3. Each player asks one question the selected player must answer truthfully — no passing.
4. Group agrees on one word to describe the player and says it simultaneously — player has to react.
5. Ask the player to rank everyone in the room from most to least trustworthy — out loud.
6. Group asks the player about their love life — 3 questions, any topic.
7. Ask the player what they genuinely think of each person in the room — one sentence per person.
8. Group interrogates the player about the last time they did something they shouldn't have.
9. Ask the player 3 "what would you do if..." scenarios — group rates the answers.
10. Group chooses a life decision the player has made and questions them about it for 60 seconds.

---

#### MOST_LIKELY_TO Prompts (10)
Selected player reads the prompt; group votes by pointing; most votes = drink 2 sips.

1. Most likely to accidentally text their boss something inappropriate.
2. Most likely to end up on a reality TV show.
3. Most likely to cry at a commercial.
4. Most likely to get married first.
5. Most likely to wake up in a foreign country with no memory of getting there.
6. Most likely to ghost someone after a first date.
7. Most likely to be famous for completely the wrong reason.
8. Most likely to start a fight over a board game.
9. Most likely to have a secret second life nobody knows about.
10. Most likely to still be doing this exact thing in 10 years.

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

## Project Layout

Single Maven project. The Vue frontend lives in `frontend/` at the project root. Docker builds both in separate stages and copies Vue's `dist/` into `src/main/resources/static/` before compiling the JAR. Spring Boot serves everything from that static path.

```
wheel-unfortunate/
├── pom.xml
├── Dockerfile
├── render.yaml
├── frontend/                              # Vue 3 + TypeScript (Vite)
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── public/
│   │   └── sounds/
│   │       ├── drumroll.mp3
│   │       ├── airhorn.mp3
│   │       └── tick.mp3
│   └── src/                               # (see Frontend Structure below)
└── src/                                   # Spring Boot
    ├── main/
    │   ├── java/com/remstem/game/         # (see Backend Structure below)
    │   └── resources/
    │       ├── application.yml
    │       ├── static/                    # Vue dist copied here at build time
    │       └── content/
    │           ├── truths.json
    │           ├── dares.json
    │           ├── challenges.json
    │           ├── hotseat.json
    │           └── mostlikelyto.json
    └── test/
        └── java/com/remstem/game/
```

### application.yml (key entries)

```yaml
server:
  port: ${PORT:8080}

spring:
  web:
    resources:
      static-locations: classpath:/static/

game:
  spin-warning-seconds: 5
```

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
│  🎡 Wheel Unfortunate   ⏱ 14:32   ABC123 📷 │  ← top bar
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
│  Wheel Unfortunate 🎡  │
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

### Build strategy (single service, Docker)

Multi-stage Dockerfile. No Maven frontend plugin needed — Node and Maven run in separate build stages; the final image is a slim JRE + JAR only.

```dockerfile
# Stage 1 — build Vue frontend
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2 — build Spring Boot JAR
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -q
COPY src/ ./src/
COPY --from=frontend /app/frontend/dist ./src/main/resources/static/
RUN mvn package -DskipTests -q

# Stage 3 — runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Render config (`render.yaml`)

```yaml
services:
  - type: web
    name: wheel-unfortunate
    env: docker
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
- Sound assets: `drumroll.mp3`, `airhorn.mp3`, `tick.mp3` placed in `frontend/public/sounds/`
- `Dockerfile` (multi-stage: node → maven → jre-alpine)
- `render.yaml` (`env: docker`)

### Phase 2 — Core backend
- Room + Player models
- RoomService (create, join, start, end)
- SpinService (weighted random challenge selection — 8 types including HOT_SEAT and MOST_LIKELY_TO)
- ChallengeService (loads truth/dare/challenge/hotseat/mostlikelyto content from `src/main/resources/content/*.json` at startup; filters by intensity on spin)
- Content JSON files: `truths.json`, `dares.json`, `challenges.json`, `hotseat.json`, `mostlikelyto.json`
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
