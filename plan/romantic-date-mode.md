# Romantic Date Mode — Implementation Plan

## Context

Add a "Date Night" mode to Wheel Unfortunate aimed at two people on a date. The mode runs as a fully separate experience from the party game: single wheel (no WHO wheel), turns alternate automatically between the two players, auto-spins on a short timer (configurable, default 3 min), and the UI switches to a full romantic visual theme. Content covers four wheel segments: personal questions, physical touch, romantic dares, and drink.

---

## Decisions

- **GameMode** enum added to `GameConfig`: `PARTY` (default) | `ROMANTIC`
- **4 new wheel segments** for romantic mode: `PERSONAL_QUESTION`, `PHYSICAL_TOUCH`, `ROMANTIC_DARE`, and reuse `DRINK`
- **No intensity selector** in romantic mode; content is pre-curated across light → bold intensity internally, skewing toward medium/bold
- **Turn alternation**: server tracks `currentTurnIndex` (0 or 1) in `Room`; increments each spin; sent in `SPIN_RESULT`
- **Timer**: `GameConfig.spinIntervalMinutes` configurable; romantic mode defaults to `3.0` on the HomeView UI
- **New view**: `DateView.vue` — separate from `HostView.vue` to avoid coupling; routes to `/date/:code`
- **Visual theme**: rose/candlelight palette applied via scoped CSS in `DateView`

---

## Backend Changes

### 1. `GameMode.java` (new enum)
```
src/main/java/com/remstem/game/model/GameMode.java
```
```java
public enum GameMode { PARTY, ROMANTIC }
```

### 2. `GameConfig.java`
Add field:
```java
private GameMode gameMode = GameMode.PARTY;
```

### 3. `ChallengeType.java`
Add three values:
```java
PERSONAL_QUESTION, PHYSICAL_TOUCH, ROMANTIC_DARE
```
Existing `DRINK` reused as-is.

### 4. `SpinService.java`
If `gameMode == ROMANTIC`, call `spinRomantic(room)`:
- Equal weight over `[PERSONAL_QUESTION, PHYSICAL_TOUCH, ROMANTIC_DARE, DRINK]`
- Increment `room.currentTurnIndex`, set `playerId` to player at that index in `SPIN_RESULT`

### 5. `Room.java`
Add field:
```java
private int currentTurnIndex = 0;
```

### 6. `ChallengeService.java`
Load 3 new content files at `@PostConstruct`:
```java
pool.put(ChallengeType.PERSONAL_QUESTION, loadFile("content/romantic_questions.json", ...));
pool.put(ChallengeType.PHYSICAL_TOUCH,    loadFile("content/physical_touch.json", ...));
pool.put(ChallengeType.ROMANTIC_DARE,     loadFile("content/romantic_dares.json", ...));
```
DRINK in romantic mode: same generation logic, copy says "take a sip together".
Intensity compatibility: for ROMANTIC mode, accept all content regardless of intensity field.

---

## Content Files (`src/main/resources/content/`)

All files follow existing schema: `[{ "text": "...", "intensity": "NORMAL", "subtype": "..." }]`

### `romantic_questions.json` (~20 questions, light → deep)

**Light:**
- What's your idea of a perfect date?
- What song always puts you in a good mood?
- What's something small that always makes you smile?
- What's your favorite memory from the past year?
- What's one thing you've always wanted to try but haven't?

**Medium:**
- What do you find most attractive about the person across from you?
- What's something about yourself you think I'd be surprised to know?
- What does a great relationship look like to you?
- What's the most romantic thing someone has ever done for you?
- What's your love language?
- When did you last feel truly understood by someone?
- What's a dream you've never told most people about?

**Deep:**
- What's your biggest fear when it comes to relationships?
- What does love mean to you — right now, at this point in your life?
- Is there something you've been wanting to say but haven't found the right moment?
- What's one thing you wish people knew about who you really are?
- What would make this night memorable for you?

### `physical_touch.json` (~16 items, light → bold)

**Light:**
- Hold hands for the rest of this round.
- Give them a hug that lasts at least 10 seconds.
- Kiss them on the cheek.
- Hold eye contact without speaking for 30 seconds.
- Gently fix something small about how they look.

**Medium:**
- Give them a 60-second shoulder or neck massage.
- Hold their face in your hands for 10 seconds and say nothing.
- Dance together for one full song — you pick the song.
- Kiss their hand or forehead.
- Lean against each other in silence for one full minute.

**Bold:**
- Kiss them — at least 5 seconds, no laughing.
- Whisper something in their ear and let them react.
- Kiss them somewhere unexpected — cheek, nose, hand, shoulder — their choice of where.
- Hold them close and slow dance, even if there's no music.

### `romantic_dares.json` (~16 items)

**Light:**
- Tell them one thing you genuinely appreciate about them — something specific.
- Write a 3-sentence note about why tonight matters and read it out loud.
- Describe what you notice most when you look at them right now.
- Give them a compliment they've probably never heard before.
- Tell them your favorite thing about the way they laugh.

**Medium:**
- Describe your ideal future — 5 years from now — in 60 seconds.
- Tell them the moment tonight when you felt most comfortable.
- Say something you've been meaning to tell them but haven't yet.
- Look them in the eyes and tell them one thing you find beautiful about them.
- Tell them what you were thinking about on the way here.

**Bold:**
- Tell them one thing you want more of in this relationship (or in what this is becoming).
- Tell them what you'd want your first morning together to look like.
- Describe in detail what you'd plan for a perfect weekend away, just the two of you.

---

## Frontend Changes

### 1. `HomeView.vue`
- Add mode selector above create-room form:
  ```
  [🎉 Party Game]   [🌹 Date Night]
  ```
  Sets `createConfig.gameMode` to `PARTY` or `ROMANTIC`.
- When `ROMANTIC` selected: hide intensity selector, set default `spinIntervalMinutes` to `3`, show description ("Just the two of you.").
- After room creation: navigate to `/date/{code}` if ROMANTIC, `/host/{code}` if PARTY.

### 2. New route
```
/date/:code    DateView    romantic main screen
```
Add to `frontend/src/router/index.ts`.

### 3. `DateView.vue` (new)

Layout (works portrait on a phone or tablet propped between two people):
```
┌─────────────────────────────┐
│  🌹 Date Night   ABC123     │
│  ─────────────────────────  │
│                             │
│      [Person A's turn]      │  ← alternates each spin
│                             │
│         ╔═══════╗           │
│         ║ WHEEL ║           │  ← single SpinWheel, 4 segments
│         ╚═══════╝           │
│                             │
│      Next spin in  2:47     │
│                             │
│ ┌─────────────────────────┐ │
│ │  💋 PHYSICAL TOUCH       │ │  ← ChallengeCard
│ │  Kiss them for 5 seconds │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

- Reuses `SpinWheel.vue`, `CountdownBar.vue`, `ChallengeCard.vue`
- Reuses `useWebSocket` composable — subscribes to same STOMP topics
- Self-ping `GET /api/rooms/{code}` every 10 min (same pattern as HostView)
- No player panel, no voting, no power-ups, no active rules

### 4. Romantic color palette (scoped CSS in `DateView.vue`)

| Token | Value | Use |
|-------|-------|-----|
| Background | `#1A0A0A` | Page bg |
| Wheel segment 1 | `#DC143C` crimson | QUESTION |
| Wheel segment 2 | `#FFB6C1` soft pink | TOUCH |
| Wheel segment 3 | `#C4977A` rose gold | DARE |
| Wheel segment 4 | `#FFF0EC` cream | DRINK |
| Card bg | `#2D0C1E` | ChallengeCard |
| Card text | `#FFE4E1` | Challenge text |
| Turn indicator | `#FF85A1` | "Person A's turn" label |

```typescript
const ROMANTIC_COLORS = ['#DC143C', '#FFB6C1', '#C4977A', '#FFF0EC']
const ROMANTIC_LABELS = ['QUESTION', 'TOUCH', 'DARE', 'DRINK']
```

---

## Files to Create / Modify

| Action | File |
|--------|------|
| Create | `src/main/java/com/remstem/game/model/GameMode.java` |
| Modify | `src/main/java/com/remstem/game/model/GameConfig.java` |
| Modify | `src/main/java/com/remstem/game/model/ChallengeType.java` |
| Modify | `src/main/java/com/remstem/game/model/Room.java` |
| Modify | `src/main/java/com/remstem/game/service/SpinService.java` |
| Modify | `src/main/java/com/remstem/game/service/ChallengeService.java` |
| Create | `src/main/resources/content/romantic_questions.json` |
| Create | `src/main/resources/content/physical_touch.json` |
| Create | `src/main/resources/content/romantic_dares.json` |
| Modify | `frontend/src/views/HomeView.vue` |
| Create | `frontend/src/views/DateView.vue` |
| Modify | `frontend/src/router/index.ts` |

---

## Verification

1. `mvn spring-boot:run` + `npm run dev`
2. HomeView → select Date Night → create room → verify navigates to `/date/{code}`
3. Verify wheel shows 4 segments with romantic colors
4. Let timer fire → verify SPIN_RESULT contains the correct alternating player
5. Spin multiple times → verify turns alternate A → B → A → B
6. Verify all 3 romantic challenge types appear over several spins
7. Verify DRINK still works with "take a sip together" copy
8. Verify self-ping fires (network tab — request every 10 min)
9. End game → stats screen works (no regression in party mode)
