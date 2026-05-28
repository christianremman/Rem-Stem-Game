package com.remstem.game.config;

import com.remstem.game.model.Room;
import com.remstem.game.model.WsEvent;
import com.remstem.game.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RoomService roomService;
    private final SimpMessagingTemplate messaging;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Optional<Room> roomOpt = roomService.findBySessionId(sessionId);
        roomOpt.ifPresent(room -> {
            roomService.findPlayerBySessionId(sessionId).ifPresent(player -> {
                player.setConnected(false);
                messaging.convertAndSend("/topic/rooms/" + room.getCode(),
                        WsEvent.of("PLAYER_DISCONNECTED", Map.of("playerId", player.getId())));
            });
        });
    }
}
