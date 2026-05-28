package com.remstem.game.controller;

import com.remstem.game.model.*;
import com.remstem.game.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameSocketControllerTest {

    private GameSocketController controller;
    private RoomService roomService;
    private SimpMessagingTemplate messaging;
    private Room room;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        messaging = mock(SimpMessagingTemplate.class);
        controller = new GameSocketController(roomService, messaging);

        room = new Room("ABC123", "token");
        alice = new Player("p1", "Alice");
        bob = new Player("p2", "Bob");
        room.getPlayers().put("p1", alice);
        room.getPlayers().put("p2", bob);

        when(roomService.find("ABC123")).thenReturn(Optional.of(room));
        when(roomService.find("MISS")).thenReturn(Optional.empty());
    }

    // --- powerUp ---

    @Test
    void powerUp_removesTypeFromPlayer() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "REVENGE"));
        assertThat(alice.getPowerUps()).doesNotContain(PowerUpType.REVENGE);
    }

    @Test
    void powerUp_broadcasts_powerupUsed() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "SAFE"));

        var captor = ArgumentCaptor.forClass(WsEvent.class);
        verify(messaging).convertAndSend(eq("/topic/rooms/ABC123"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("POWERUP_USED");
    }

    @Test
    void powerUp_withTargetId_includesTargetInPayload() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "REVENGE", "targetId", "p2"));

        var captor = ArgumentCaptor.forClass(WsEvent.class);
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/rooms/ABC123"), captor.capture());
        var powerUpEvent = captor.getAllValues().stream()
                .filter(e -> "POWERUP_USED".equals(e.getType())).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) powerUpEvent.getPayload();
        assertThat(payload).containsKey("targetId");
    }

    @Test
    void powerUp_ignored_whenPlayerLacksIt() {
        alice.getPowerUps().remove(PowerUpType.REVENGE);
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "REVENGE"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void powerUp_ignored_whenPlayerNotFound() {
        controller.powerUp("ABC123", Map.of("playerId", "ghost", "type", "SAFE"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void powerUp_ignored_whenRoomNotFound() {
        controller.powerUp("MISS", Map.of("playerId", "p1", "type", "SAFE"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void powerUp_ignored_whenTypeInvalid() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "INVALID"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void powerUp_doubleDown_broadcastsDoubleDownResult() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "DOUBLE_DOWN"));

        var captor = ArgumentCaptor.forClass(WsEvent.class);
        verify(messaging, atLeast(2)).convertAndSend(eq("/topic/rooms/ABC123"), captor.capture());
        boolean hasDoubleDownResult = captor.getAllValues().stream()
                .anyMatch(e -> "DOUBLE_DOWN_RESULT".equals(e.getType()));
        assertThat(hasDoubleDownResult).isTrue();
    }

    @Test
    void powerUp_canOnlyUseEachTypeOnce() {
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "SAFE"));
        reset(messaging);
        controller.powerUp("ABC123", Map.of("playerId", "p1", "type", "SAFE"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    // --- addRule ---

    @Test
    void addRule_addsRuleToRoom() {
        controller.addRule("ABC123", Map.of("rule", "no swearing", "addedBy", "Alice", "playerId", "p1"));
        assertThat(room.getActiveRules()).contains("no swearing");
    }

    @Test
    void addRule_broadcastsRuleAdded() {
        controller.addRule("ABC123", Map.of("rule", "left hand only", "addedBy", "Alice", "playerId", "p1"));

        var captor = ArgumentCaptor.forClass(WsEvent.class);
        verify(messaging).convertAndSend(eq("/topic/rooms/ABC123"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("RULE_ADDED");
    }

    @Test
    void addRule_incrementsPlayerRulesSet() {
        controller.addRule("ABC123", Map.of("rule", "drink left", "addedBy", "Alice", "playerId", "p1"));
        assertThat(alice.getRulesSet()).isEqualTo(1);
    }

    @Test
    void addRule_multipleRules_allStored() {
        controller.addRule("ABC123", Map.of("rule", "no swearing", "addedBy", "Alice", "playerId", "p1"));
        controller.addRule("ABC123", Map.of("rule", "left hand", "addedBy", "Bob", "playerId", "p2"));
        assertThat(room.getActiveRules()).hasSize(2);
    }

    @Test
    void addRule_ignored_whenRoomNotFound() {
        controller.addRule("MISS", Map.of("rule", "test", "addedBy", "Alice", "playerId", "p1"));
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    // --- register ---

    @Test
    void register_setsSessionIdOnPlayer() {
        controller.register("ABC123", Map.of("playerId", "p1"), "sess-abc");
        assertThat(alice.getSessionId()).isEqualTo("sess-abc");
    }

    @Test
    void register_marksPlayerConnected() {
        alice.setConnected(false);
        controller.register("ABC123", Map.of("playerId", "p1"), "sess-abc");
        assertThat(alice.isConnected()).isTrue();
    }

    @Test
    void register_ignored_whenPlayerNotFound() {
        assertThatCode(() ->
                controller.register("ABC123", Map.of("playerId", "ghost"), "sess-abc"))
                .doesNotThrowAnyException();
    }

    @Test
    void register_ignored_whenRoomNotFound() {
        assertThatCode(() ->
                controller.register("MISS", Map.of("playerId", "p1"), "sess-abc"))
                .doesNotThrowAnyException();
    }
}
