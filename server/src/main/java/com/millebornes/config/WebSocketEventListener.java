package com.millebornes.config;

import com.millebornes.model.GameRoom;
import com.millebornes.service.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(LobbyService lobbyService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("New WebSocket connection: sessionId={}", headerAccessor.getSessionId());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket disconnected: sessionId={}", sessionId);

        // Remove spectator from any room they were watching
        for (GameRoom room : lobbyService.getAllRooms()) {
            if (room.getSpectators().containsKey(sessionId)) {
                room.removeSpectator(sessionId);
                messagingTemplate.convertAndSend("/topic/lobby/" + room.getRoomCode(),
                        lobbyService.toLobbyDTO(room, false));
                break;
            }
        }
    }
}
