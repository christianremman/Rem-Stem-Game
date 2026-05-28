package com.remstem.game.controller;

import com.remstem.game.model.*;
import com.remstem.game.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteHandlingTest {

    private GameSocketController controller;
    private RoomService roomService;
    private SimpMessagingTemplate messaging;
    private Room room;
    private Player selectedPlayer;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        messaging = mock(SimpMessagingTemplate.class);
        controller = new GameSocketController(roomService, messaging);

        room = new Room("ABC123", "token");
        selectedPlayer = new Player("p1", "Alice");
        room.getPlayers().put("p1", selectedPlayer);
        room.getPlayers().put("p2", new Player("p2", "Bob"));
        room.getPlayers().put("p3", new Player("p3", "Charlie"));

        Challenge challenge = new Challenge();
        challenge.setType(ChallengeType.DRINK);
        challenge.setSips(3);
        challenge.setIntensity(Intensity.NORMAL);

        SpinResult spin = new SpinResult(selectedPlayer, challenge, 90.0, 45.0, 1);
        room.getHistory().add(spin);

        when(roomService.find("ABC123")).thenReturn(java.util.Optional.of(room));
    }

    @Test
    void vote_doneTwice_completedCountIsOne_notTwo() {
        // RED: currently completed++ per DONE vote → completed=2 after two voters
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p2"));
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p3"));

        assertThat(selectedPlayer.getCompleted()).isEqualTo(1);
    }

    @Test
    void vote_doneThreeTimes_sipsAddedOnce_notThreeTimes() {
        // RED: currently estimatedSips += sips per DONE vote → 9 after three voters
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p1"));
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p2"));
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p3"));

        assertThat(selectedPlayer.getEstimatedSips()).isEqualTo(3); // sips once, not 9
    }

    @Test
    void vote_majorityRefused_extraDrinksAppliedOnce() {
        // RED: currently refused++ per REFUSED vote → refused=2, estimatedSips=4 after two voters
        controller.vote("ABC123", Map.of("vote", "REFUSED", "playerId", "p2"));
        controller.vote("ABC123", Map.of("vote", "REFUSED", "playerId", "p3"));

        assertThat(selectedPlayer.getRefused()).isEqualTo(1); // one refusal event, not per-voter
        assertThat(selectedPlayer.getEstimatedSips()).isEqualTo(2); // +2 extra, applied once
    }

    @Test
    void vote_samePlayerTwice_ignoredSecondTime() {
        // reach majority first, then same player tries to vote again
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p2"));
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p3")); // majority → resolve
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p2")); // duplicate, already resolved

        assertThat(selectedPlayer.getCompleted()).isEqualTo(1); // resolved exactly once
    }

    @Test
    void vote_boo_doesNotAffectStats() {
        controller.vote("ABC123", Map.of("vote", "BOO", "playerId", "p2"));

        assertThat(selectedPlayer.getCompleted()).isEqualTo(0);
        assertThat(selectedPlayer.getRefused()).isEqualTo(0);
    }

    @Test
    void vote_minorityRefused_doesNotApplyExtraDrinks() {
        // 1 refused out of 3 players = minority, no extra penalty
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p2"));
        controller.vote("ABC123", Map.of("vote", "DONE", "playerId", "p3"));
        controller.vote("ABC123", Map.of("vote", "REFUSED", "playerId", "p1"));

        assertThat(selectedPlayer.getRefused()).isEqualTo(0); // majority said DONE
        assertThat(selectedPlayer.getCompleted()).isEqualTo(1);
    }
}
