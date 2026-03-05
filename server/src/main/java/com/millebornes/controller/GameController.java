package com.millebornes.controller;

import com.millebornes.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/{roomCode}/draw")
    public void draw(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        gameService.handleDraw(roomCode, playerId);
    }

    @MessageMapping("/game/{roomCode}/play")
    public void play(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        int cardIndex = (Integer) body.get("cardIndex");
        String targetPlayerId = (String) body.get("targetPlayerId");
        gameService.handlePlay(roomCode, playerId, cardIndex, targetPlayerId);
    }

    @MessageMapping("/game/{roomCode}/discard")
    public void discard(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        int cardIndex = (Integer) body.get("cardIndex");
        gameService.handleDiscard(roomCode, playerId, cardIndex);
    }

    @MessageMapping("/game/{roomCode}/coup-fourre")
    public void coupFourre(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        int cardIndex = (Integer) body.get("cardIndex");
        gameService.handleCoupFourre(roomCode, playerId, cardIndex);
    }

    @MessageMapping("/game/{roomCode}/coup-fourre-pass")
    public void coupFourrePass(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        gameService.handleCoupFourrePass(roomCode, playerId);
    }

    @MessageMapping("/game/{roomCode}/extension")
    public void extension(@DestinationVariable String roomCode, Map<String, Object> body) {
        String playerId = (String) body.get("playerId");
        boolean declare = Boolean.TRUE.equals(body.get("declare"));
        gameService.handleExtension(roomCode, playerId, declare);
    }
}
