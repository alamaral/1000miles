# Mille Bornes — Architecture

## Overview

A multiplayer web implementation of the classic French card game Mille Bornes. Players race to reach 1000 miles (or 1250 with an extension) by playing distance cards while attacking opponents with hazards and defending with remedies and safeties.

The app is split into two parts: a **Java Spring Boot server** that owns all game logic and state, and a **React client** that renders the UI and relays player actions.

```
┌──────────────────────────────────────────────────────────┐
│  Browser (React Client)                                  │
│                                                          │
│  LobbyPage ──REST──▶ /api/lobby/*  (create/join/start)  │
│  GamePage  ──STOMP──▶ /app/game/*  (draw/play/discard)  │
│            ◀─STOMP── /topic/game/* (state/hand/scores)   │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│  Spring Boot Server (port 8080)                          │
│                                                          │
│  LobbyController ─▶ LobbyService    (rooms, players)    │
│  GameController  ─▶ GameService     (turns, broadcasts)  │
│  SessionController  (WebSocket registration)             │
│                      GameEngine     (rules, validation)  │
│                      ScoreCalculator (end-of-hand scores)│
│                      TurnManager    (turn advancement)   │
└──────────────────────────────────────────────────────────┘
```

## Server (`server/`)

Java 11, Spring Boot 2.7.18, Gradle 7.6.4.

All game state lives in server memory (no database). Loss of the server process loses all active games.

### Package layout

```
com.millebornes/
├── MilleBornesApplication.java       Entry point
├── config/
│   ├── WebConfig.java                CORS, SPA fallback for /game/* routes
│   ├── WebSocketConfig.java          STOMP over SockJS at /ws endpoint
│   └── WebSocketEventListener.java   Connection lifecycle logging
├── controller/
│   ├── LobbyController.java          REST: create, join, spectate, start, new-hand
│   ├── GameController.java           STOMP: draw, play, discard, coup-fourre, extension
│   └── SessionController.java        STOMP: register WebSocket session with player ID
├── service/
│   ├── LobbyService.java             Room creation/joining, room code generation
│   └── GameService.java              Game lifecycle, move handling, state broadcasting
├── game/
│   ├── GameEngine.java               Pure validation & application of moves (static methods)
│   ├── TurnManager.java              Turn phase transitions, player advancement
│   ├── ScoreCalculator.java          End-of-hand scoring (safeties, shut-out, trip complete, etc.)
│   └── Deck.java                     Shuffle, draw from the 106-card deck
├── model/
│   ├── Card.java                     Enum of all 106 cards (distances, hazards, remedies, safeties)
│   ├── CardType.java                 DISTANCE, HAZARD, REMEDY, SAFETY
│   ├── HazardType.java               ACCIDENT, OUT_OF_GAS, FLAT_TIRE, STOP, SPEED_LIMIT
│   ├── GamePhase.java                WAITING, PLAYING, HAND_OVER, GAME_OVER
│   ├── TurnPhase.java                DRAW, PLAY, COUP_FOURRE_WINDOW
│   ├── Player.java                   Hand, battle/speed/distance piles, safeties, miles
│   ├── GameRoom.java                 Pre-game lobby (players, host, teams, max players)
│   ├── GameState.java                Full mutable game state (deck, players, phase, CF metadata, last-action tracking)
│   ├── Team.java                     Team structure for team-play variant
│   └── ScoreSheet.java               Per-player score breakdown for one hand
└── dto/
    ├── GameStateDTO.java             Public game state (hides player hands, shows hand size only, includes last-action for animation)
    ├── LobbyDTO.java                 Room info for lobby display
    ├── PlayCardRequest.java          Inbound play-card message
    ├── DiscardRequest.java           Inbound discard message
    └── CoupFourreResponse.java       Inbound coup-fourre declaration
```

### REST API

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/lobby/create` | Create a room. Body: `{ playerName }`. Returns `{ roomCode, playerId, lobby }` |
| POST | `/api/lobby/join/{code}` | Join a room. Body: `{ playerName }`. Returns `{ roomCode, playerId, lobby }` |
| GET | `/api/lobby/spectate/{code}` | Validate room exists for spectating. Returns `{ roomCode, lobby, gameInProgress }` |
| GET | `/api/lobby/rooms` | List all rooms |
| POST | `/api/lobby/{code}/start` | Host starts the game |
| POST | `/api/lobby/{code}/new-hand` | Start next hand (after HAND_OVER) |
| POST | `/api/lobby/{code}/teams` | Configure team play. Body: `{ useTeams }` |

### WebSocket (STOMP) Messages

**Client → Server** (sent to `/app/game/{code}/...`):

| Destination | Payload | Purpose |
|-------------|---------|---------|
| `/register` | `{ playerId }` | Associate WebSocket session with player; triggers state push for reconnects |
| `/draw` | `{ playerId }` | Draw a card from the deck |
| `/play` | `{ playerId, cardIndex, targetPlayerId }` | Play a card (distance/remedy on self, hazard on targetPlayerId) |
| `/discard` | `{ playerId, cardIndex }` | Discard a card |
| `/coup-fourre` | `{ playerId, cardIndex }` | Declare coup fourre with the safety at cardIndex |
| `/coup-fourre-pass` | `{ playerId }` | Decline coup fourre opportunity |
| `/extension` | `{ playerId, declare }` | Declare or decline extension to 1250 miles |

**Server → Client** (broadcast to `/topic/game/{code}/...`):

| Topic | Payload | Audience |
|-------|---------|----------|
| `/topic/game/{code}` | `GameStateDTO` | All players + spectators (hands hidden) |
| `/topic/game/{code}/hand/{playerId}` | `{ hand, playerId }` | One specific player (their actual cards) |
| `/topic/game/{code}/error/{playerId}` | `{ message }` | One specific player |
| `/topic/game/{code}/score` | `{ scores, cumulativeScores, handComplete, gameComplete }` | All players + spectators |
| `/topic/lobby/{code}` | `LobbyDTO` | All players in the room |

### Key design decisions

- **GameEngine is stateless.** All methods are static: `validatePlay(state, player, card, target)` returns an error string or null, `applyPlay(...)` mutates the state. This makes the rules easy to test in isolation.
- **GameService orchestrates.** It calls GameEngine for validation, mutates state, then broadcasts via `SimpMessagingTemplate`.
- **DTOs filter private info.** `GameStateDTO` converts hands into just a `handSize` integer. Each player's actual hand is sent separately to their private topic.
- **Reconnection support.** When a player registers (or re-registers after a refresh), the server pushes the current game state and their hand immediately.
- **Last-action tracking for animation.** `GameState` tracks `lastPlayedCard`, `lastPlayedByPlayerId`, `lastPlayedTargetPlayerId`, and `lastActionType` ("PLAY"/"DISCARD"). These are set in `handlePlay`/`handleDiscard` (card is captured before the engine removes it from hand) and cleared in `handleDraw` so stale data isn't re-broadcast. The DTO includes these fields so all clients can trigger animations.

## Client (`client/`)

React 18, Vite 5, SockJS + STOMP.js. No CSS framework — all inline styles.

### File layout

```
client/src/
├── main.jsx                          ReactDOM render
├── App.jsx                           Router: / → LobbyPage, /game/:code → GamePage
├── context/
│   └── GameContext.jsx                All shared state, REST calls, WebSocket subscriptions,
│                                      session persistence (sessionStorage)
├── services/
│   └── WebSocketService.js            SockJS + STOMP client singleton (connect, subscribe, send)
├── pages/
│   ├── LobbyPage.jsx                 Create/join/watch room, player list, start button
│   └── GamePage.jsx                  Main game view: board, hand, draw/discard, touch drag,
│                                      responsive scaling, spectator mode, card play animation
└── components/
    ├── GameBoard.jsx                 Arranges PlayerAreas (opponents top, self bottom)
    ├── PlayerArea.jsx                One player's tableau: battle, speed, safeties, distance bundles
    ├── PlayerHand.jsx                Row of cards in hand, sized to fit 7 across
    ├── CardComponent.jsx             Single card rendering (colors, icons, borders by card type)
    ├── DrawPile.jsx                  Clickable face-down deck with count
    ├── DiscardPile.jsx               Drop target showing top discard
    ├── TurnIndicator.jsx             Whose turn, phase, hand number, mile target
    ├── CoupFourrePrompt.jsx          Modal prompt when coup fourre opportunity arises
    └── ScoreBoard.jsx                End-of-hand/game score overlay
```

### Data flow

1. **Lobby:** User enters name → REST `POST /api/lobby/create` or `/join/{code}` → receives `playerId` + `roomCode` → navigates to `/game/{code}` → WebSocket connects and registers.
2. **Gameplay:** Server broadcasts `GameStateDTO` to all subscribers on every state change. Each player also receives their private hand. The client renders whatever the server sends.
3. **Player actions:** Draw (click), play (drag card to player area), discard (drag card to discard pile) → client sends STOMP message → server validates, applies, broadcasts.
4. **Spectators:** Connect to the same public topics but don't subscribe to any hand/error topic. See the full board without any private information.

### Responsive scaling

The game UI is designed for a 1200px-wide layout. On narrower screens, the entire game container is shrunk with `CSS transform: scale(viewportWidth / 1200)`. This is applied in `GamePage.jsx` — no child component needs to know about it.

Modals (coup fourre prompt, scoreboard, error toast) render outside the scaled container at native viewport resolution so they remain readable and tappable on small screens.

### Touch drag-and-drop

HTML5 drag-and-drop doesn't work on mobile browsers. `GamePage.jsx` adds native touch event listeners (`touchstart`/`touchmove`/`touchend` with `passive: false`) that:
1. Detect touch on a `[data-drag-index]` card element
2. Create a floating DOM clone that follows the finger
3. Use `document.elementFromPoint()` to find `[data-drop-target]` elements under the finger
4. Highlight drop targets with an orange outline
5. On release, dispatch to `handleDropOnPlayer` or `handleDropOnDiscard` based on the target's data attributes

### Card play animation

When a card is played or discarded, all clients see an animated card flying from the source to the destination along a curved path. The animation system lives in `GamePage.jsx`.

**Server side:** Every broadcast after a play/discard includes `lastPlayedCard`, `lastPlayedByPlayerId`, `lastPlayedTargetPlayerId`, and `lastActionType`. These are cleared on draw so stale data isn't re-sent.

**Client side:**

- A `useEffect` watches for new last-action data in the incoming `gameState`. A ref-based key (`lastAnimKeyRef`) prevents re-triggering the same animation on reconnect.
- **Source element:** For the local player, the card flies from their hand (`[data-hand-area]`). For other players, it flies from the player's board area (`[data-player-id]`).
- **Destination element:** For plays, the target player's board area. For discards, the discard pile (`[data-drop-target="discard"]`).
- **Bezier curves:** All animations use `requestAnimationFrame` loops tracing quadratic bezier paths with ease-in-out timing. Straight flights use a gentle perpendicular arc. Self-play (same source/dest, viewed by others) uses a "swoop" — starting above the battle pile, curving down-left via a control point, then arcing to the destination.
- **Buffered game state:** A `displayedGameState` lags behind the live `gameState` during animations. The board and piles render from `displayedGameState`, so the card doesn't appear at its destination until the flying animation completes. When the animation finishes, `displayedGameState` is flushed to the latest state.
- The `CardPlayAnimation` component renders a `position: fixed` overlay with a small `CardComponent` at the animated position, with `pointerEvents: none` and high `zIndex`.

### Session persistence

`GameContext` saves `{ playerId, playerName, roomCode, spectating }` to `sessionStorage` after create/join/spectate. On page load, if a saved session exists, it reconnects the WebSocket automatically. The server's register handler detects the returning player and pushes the current game state and hand.

## Build & Run

```bash
# Server
cd server
./gradlew bootRun          # runs on port 8080

# Client (dev)
cd client
npm install
npx vite                   # runs on port 3000, proxies /api and /ws to 8080

# Client (production build)
npx vite build             # outputs to client/dist/
```

### Environment notes

- Java 11 (not 17+) — Spring Boot 2.7.18
- Gradle 7.6.4 via wrapper (system gradle may be too old)
- Node.js 18+ required for Vite
