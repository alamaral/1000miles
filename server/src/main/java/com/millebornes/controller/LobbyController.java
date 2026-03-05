package com.millebornes.controller;

import com.millebornes.dto.LobbyDTO;
import com.millebornes.model.GameRoom;
import com.millebornes.model.Player;
import com.millebornes.service.GameService;
import com.millebornes.service.LobbyService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyController(LobbyService lobbyService, GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> body) {
        String playerName = body.get("playerName");
        if (playerName == null || playerName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Player name is required"));
        }

        // SessionId will be set when they connect via WebSocket
        GameRoom room = lobbyService.createRoom(playerName.trim(), "pending");
        Player host = room.getPlayers().get(0);

        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", room.getRoomCode());
        response.put("playerId", host.getId());
        response.put("lobby", lobbyService.toLobbyDTO(room, false));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomCode, @RequestBody Map<String, String> body) {
        String playerName = body.get("playerName");
        if (playerName == null || playerName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Player name is required"));
        }

        try {
            // Block new players from joining after game has started
            if (gameService.getGame(roomCode.toUpperCase()) != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Game already in progress. Use Watch to spectate."));
            }

            GameRoom room = lobbyService.joinRoom(roomCode.toUpperCase(), playerName.trim(), "pending");
            Player joined = room.getPlayers().get(room.getPlayers().size() - 1);

            Map<String, Object> response = new HashMap<>();
            response.put("roomCode", room.getRoomCode());
            response.put("playerId", joined.getId());
            response.put("lobby", lobbyService.toLobbyDTO(room, false));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<LobbyDTO>> listRooms() {
        return ResponseEntity.ok(lobbyService.listRooms());
    }

    @PostMapping("/{roomCode}/partner")
    public ResponseEntity<?> choosePartner(@PathVariable String roomCode, @RequestBody Map<String, String> body) {
        try {
            String playerId = body.get("playerId");
            String targetPlayerId = body.get("targetPlayerId");
            String teamName = body.get("teamName");
            if (playerId == null || targetPlayerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "playerId and targetPlayerId are required"));
            }
            String code = roomCode.toUpperCase();
            LobbyDTO lobbyDTO = lobbyService.choosePartner(code, playerId, targetPlayerId, teamName);

            // Auto-start when 6 players are fully partnered
            if (lobbyDTO.getPlayers().size() == 6 && lobbyDTO.getPlayers().stream().allMatch(p -> p.getTeamIndex() != -1)) {
                gameService.startGame(code, true);
                lobbyDTO.setGameStarted(true);
            }

            messagingTemplate.convertAndSend("/topic/lobby/" + code, lobbyDTO);
            return ResponseEntity.ok(lobbyDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomCode, @RequestBody Map<String, String> body) {
        try {
            String playerId = body.get("playerId");
            if (playerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "playerId is required"));
            }
            LobbyDTO lobbyDTO = lobbyService.leaveRoom(roomCode.toUpperCase(), playerId);
            messagingTemplate.convertAndSend("/topic/lobby/" + roomCode.toUpperCase(), lobbyDTO);
            return ResponseEntity.ok(lobbyDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomCode}/start")
    public ResponseEntity<?> startGame(@PathVariable String roomCode) {
        try {
            gameService.startGame(roomCode.toUpperCase());
            return ResponseEntity.ok(Map.of("started", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomCode}/teams")
    public ResponseEntity<?> configureTeams(@PathVariable String roomCode, @RequestBody Map<String, Boolean> body) {
        try {
            boolean useTeams = body.getOrDefault("useTeams", false);
            lobbyService.configureTeams(roomCode.toUpperCase(), useTeams);
            GameRoom room = lobbyService.getRoom(roomCode.toUpperCase());
            return ResponseEntity.ok(lobbyService.toLobbyDTO(room, false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/spectate/{roomCode}")
    public ResponseEntity<?> spectateRoom(@PathVariable String roomCode) {
        GameRoom room = lobbyService.getRoom(roomCode.toUpperCase());
        if (room == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room not found"));
        }

        boolean gameInProgress = gameService.getGame(roomCode.toUpperCase()) != null;

        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", room.getRoomCode());
        response.put("lobby", lobbyService.toLobbyDTO(room, gameInProgress));
        response.put("gameInProgress", gameInProgress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomCode}/reset")
    public ResponseEntity<?> resetRoom(@PathVariable String roomCode) {
        GameRoom room = lobbyService.getRoom(roomCode.toUpperCase());
        if (room == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Room not found"));
        }
        gameService.resetGame(roomCode.toUpperCase());
        return ResponseEntity.ok(Map.of("reset", true));
    }

    @PostMapping("/{roomCode}/new-hand")
    public ResponseEntity<?> startNewHand(@PathVariable String roomCode) {
        try {
            gameService.startNewHand(roomCode.toUpperCase());
            return ResponseEntity.ok(Map.of("started", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
