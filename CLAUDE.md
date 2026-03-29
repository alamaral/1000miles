# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multiplayer Mille Bornes card game — Java Spring Boot backend + React frontend communicating via REST (lobby) and STOMP over WebSocket (gameplay). All game state is in-memory (no database).

## Build & Development Commands

```bash
# Full build
make all          # Builds server + client
make server       # ./gradlew build (server only)
make client       # vite build → client/dist/ (client only)
make test         # Run server tests (./gradlew test)
make clean        # Clean build artifacts

# Local development (two terminals)
cd server && ./gradlew bootRun      # Backend on port 8080
cd client && npm install && npx vite # Frontend on port 3000 (proxies to 8080)

# Run a single test class
cd server && ./gradlew test --tests "com.millebornes.game.GameEngineTest"
```

## Architecture

**Server** (Java 11, Spring Boot 2.7, Gradle):
- `controller/` — REST endpoints (`LobbyController`) and STOMP message handlers (`GameController`, `SessionController`)
- `game/` — Stateless `GameEngine` (static validation/apply methods), `TurnManager`, `ScoreCalculator`, `Deck`
- `service/` — `LobbyService` (room management), `GameService` (game lifecycle, state broadcasting)
- `model/` — `Card` enum (106 cards), `GameState`, `Player`, `Team`, game phase enums
- `dto/` — `GameStateDTO` hides other players' hands; each player gets their hand via a private STOMP topic

**Client** (React 18, Vite 5, no CSS framework — all inline styles):
- `context/GameContext.jsx` — Central state management: REST calls, WebSocket subscriptions, session persistence via sessionStorage
- `services/WebSocketService.js` — STOMP client singleton; `api.js` — API base URL from `VITE_API_URL` env var
- `pages/LobbyPage.jsx` — Create/join/spectate rooms; `GamePage.jsx` — Main game board with drag-drop and card animations
- `components/` — `GameBoard`, `PlayerArea`, `PlayerHand`, `CardComponent`, `DrawPile`, `DiscardPile`, `CoupFourrePrompt`, `ScoreBoard`

**Communication**:
- REST: `/api/lobby/*` for room creation, joining, starting games
- STOMP: `/app/game/{code}/*` for draw/play/discard/coup-fourré; server broadcasts to `/topic/game/{code}/*`

## Key Design Patterns

- **GameEngine is stateless**: All game rule validation uses static methods — easy to test in isolation
- **DTO privacy**: Server sends `GameStateDTO` (public, hands hidden) to all players, plus private hand updates per-player via separate STOMP topics
- **Card animation**: Server includes `lastPlayedCard`/`lastPlayedByPlayerId`/`lastActionType` in state broadcasts; client animates from source to destination
- **Reconnection**: Client persists session to `sessionStorage`; server re-broadcasts full state on WebSocket register

## Deployment

Configured for Render.com via `render.yaml` — server as Docker container, client as static site. Server Dockerfile uses multi-stage Java 11 build.
