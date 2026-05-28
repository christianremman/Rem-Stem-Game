package com.remstem.game.config;

import com.remstem.game.model.Room;
import com.remstem.game.model.WsEvent;
import com.remstem.game.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomService roomService;
    private final SimpMessagingTemplate messaging;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Optional<Room> roomOpt = roomService.findBySessionId(sessionId);
        roomOpt.ifPresent(room ->
            roomService.findPlayerBySessionId(sessionId).ifPresent(player -> {
                player.setConnected(false);
                messaging.convertAndSend("/topic/rooms/" + room.getCode(),
                        WsEvent.of("PLAYER_DISCONNECTED", Map.of("playerId", player.getId())));
            })
        );
    }
}
