package com.millebornes.controller;

import com.millebornes.dto.GameStateDTO;
import com.millebornes.model.GameRoom;
import com.millebornes.model.GameState;
import com.millebornes.model.Player;
import com.millebornes.service.GameService;
import com.millebornes.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SessionController {

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public SessionController(LobbyService lobbyService, GameService gameService,
                             SimpMessagingTemplate messagingTemplate) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Called by the client right after connecting to associate a WebSocket session
     * with a player in a room. Also pushes current game state for reconnecting
     * players and spectators.
     */
    @MessageMapping("/game/{roomCode}/register")
    public void register(@DestinationVariable String roomCode, Map<String, String> body,
                         SimpMessageHeaderAccessor headerAccessor) {
        String playerId = body.get("playerId");
        String sessionId = headerAccessor.getSessionId();

        GameRoom room = lobbyService.getRoom(roomCode);
        if (room == null) return;

        Player player = null;
        if (playerId != null) {
            player = room.getPlayerById(playerId);
            if (player != null) {
                player.setSessionId(sessionId);
            }
        } else {
            // Spectator — track by session with their display name
            String spectatorName = body.getOrDefault("spectatorName", "Spectator");
            room.addSpectator(sessionId, spectatorName);
        }

        // Broadcast updated lobby state
        messagingTemplate.convertAndSend("/topic/lobby/" + roomCode,
                lobbyService.toLobbyDTO(room, false));

        // If game in progress, push current state (for reconnecting players and spectators)
        GameState game = gameService.getGame(roomCode);
        if (game != null) {
            messagingTemplate.convertAndSend("/topic/game/" + roomCode,
                    GameStateDTO.fromGameState(game));

            // Send hand to reconnecting player
            if (player != null) {
                Player gamePlayer = game.getPlayerById(playerId);
                if (gamePlayer != null) {
                    Map<String, Object> handMsg = new HashMap<>();
                    handMsg.put("hand", gamePlayer.getHand());
                    handMsg.put("playerId", playerId);
                    messagingTemplate.convertAndSend(
                            "/topic/game/" + roomCode + "/hand/" + playerId, handMsg);
                }
            }
        }
    }
}
