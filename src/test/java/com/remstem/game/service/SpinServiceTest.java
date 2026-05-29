package com.remstem.game.service;

import com.remstem.game.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpinServiceTest {

    private SpinService spinService;
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        challengeService = mock(ChallengeService.class);
        Challenge fallback = new Challenge();
        fallback.setType(ChallengeType.TRUTH);
        fallback.setText("test");
        fallback.setIntensity(Intensity.NORMAL);
        when(challengeService.random(any(), any())).thenReturn(fallback);
        spinService = new SpinService(challengeService);
    }

    @Test
    void spin_withEmptyPlayerList_throwsIllegalState() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        // RED: SpinService.spin calls new Random().nextInt(0) → IllegalArgumentException
        assertThatThrownBy(() -> spinService.spin(room))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no players");
    }

    @Test
    void spin_selectsAPlayerFromTheRoom() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        Player p = new Player("p1", "Alice");
        room.getPlayers().put("p1", p);

        SpinResult result = spinService.spin(room);

        assertThat(result.getPlayer()).isEqualTo(p);
        assertThat(result.getChallenge()).isNotNull();
        assertThat(p.getTimesSelected()).isEqualTo(1);
        assertThat(room.getSpinCount().get()).isEqualTo(1);
    }

    @Test
    void spin_incrementsSpinCountPerSpin() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        spinService.spin(room);
        spinService.spin(room);

        assertThat(room.getSpinCount().get()).isEqualTo(2);
    }

    @Test
    void spin_resultAddedToHistory() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        spinService.spin(room);

        assertThat(room.getHistory()).hasSize(1);
        assertThat(room.getHistory().get(0).getChallenge()).isNotNull();
    }

    @Test
    void spin_spinNumberMatchesRoomSpinCount() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        SpinResult result = spinService.spin(room);

        assertThat(result.getSpinNumber()).isEqualTo(room.getSpinCount().get());
    }

    @Test
    void spin_playerWheelAngle_inValidRange() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        SpinResult result = spinService.spin(room);

        assertThat(result.getPlayerWheelAngle()).isBetween(0.0, 360.0);
        assertThat(result.getChallengeWheelAngle()).isBetween(0.0, 360.0);
    }

    @Test
    void spin_drinkChallenge_chillIntensity_sipsInRange1To2() {
        Room room = new Room("TEST01", "token");
        GameConfig config = new GameConfig();
        config.setIntensity(Intensity.CHILL);
        room.setConfig(config);
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        // Run enough times to hit a DRINK challenge
        for (int i = 0; i < 100; i++) {
            SpinResult result = spinService.spin(room);
            if (result.getChallenge().getType() == ChallengeType.DRINK) {
                assertThat(result.getChallenge().getSips()).isBetween(1, 2);
                return;
            }
        }
        // If no DRINK in 100 spins, skip (extremely unlikely but possible)
    }

    @Test
    void spin_drinkChallenge_savageIntensity_sipsInRange3To5() {
        Room room = new Room("TEST01", "token");
        GameConfig config = new GameConfig();
        config.setIntensity(Intensity.SAVAGE);
        room.setConfig(config);
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        for (int i = 0; i < 100; i++) {
            SpinResult result = spinService.spin(room);
            if (result.getChallenge().getType() == ChallengeType.DRINK) {
                assertThat(result.getChallenge().getSips()).isBetween(3, 5);
                return;
            }
        }
    }

    @Test
    void spin_socialChallenge_textMentionsEveryone() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        for (int i = 0; i < 100; i++) {
            SpinResult result = spinService.spin(room);
            if (result.getChallenge().getType() == ChallengeType.SOCIAL) {
                assertThat(result.getChallenge().getText()).containsIgnoringCase("everyone");
                return;
            }
        }
    }

    @Test
    void spin_romanticMode_onlyUsesRomanticChallengeTypes() {
        when(challengeService.random(any(), any())).thenAnswer(inv -> {
            Challenge c = new Challenge();
            c.setType(inv.getArgument(0, ChallengeType.class));
            c.setText("test");
            c.setIntensity(Intensity.NORMAL);
            return c;
        });
        Room room = new Room("TEST01", "token");
        GameConfig config = new GameConfig();
        config.setGameMode(GameMode.ROMANTIC);
        room.setConfig(config);
        Player alice = new Player("p1", "Alice"); alice.setJoinIndex(0);
        Player bob   = new Player("p2", "Bob");   bob.setJoinIndex(1);
        room.getPlayers().put("p1", alice);
        room.getPlayers().put("p2", bob);

        Set<ChallengeType> expected = Set.of(
                ChallengeType.PERSONAL_QUESTION, ChallengeType.PHYSICAL_TOUCH,
                ChallengeType.ROMANTIC_DARE, ChallengeType.DRINK);
        for (int i = 0; i < 50; i++) {
            assertThat(spinService.spin(room).getChallenge().getType()).isIn(expected);
        }
    }

    @Test
    void spin_romanticMode_alternatesTurns() {
        when(challengeService.random(any(), any())).thenAnswer(inv -> {
            Challenge c = new Challenge();
            c.setType(inv.getArgument(0, ChallengeType.class));
            c.setText("test");
            c.setIntensity(Intensity.NORMAL);
            return c;
        });
        Room room = new Room("TEST01", "token");
        GameConfig config = new GameConfig();
        config.setGameMode(GameMode.ROMANTIC);
        room.setConfig(config);
        Player alice = new Player("p1", "Alice"); alice.setJoinIndex(0);
        Player bob   = new Player("p2", "Bob");   bob.setJoinIndex(1);
        room.getPlayers().put("p1", alice);
        room.getPlayers().put("p2", bob);

        SpinResult r1 = spinService.spin(room);
        SpinResult r2 = spinService.spin(room);
        SpinResult r3 = spinService.spin(room);

        assertThat(r1.getPlayer()).isNotEqualTo(r2.getPlayer());
        assertThat(r1.getPlayer()).isEqualTo(r3.getPlayer());
    }

    @Test
    void spin_multiplePlayersInRoom_allCanBeSelected() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));
        room.getPlayers().put("p2", new Player("p2", "Bob"));
        room.getPlayers().put("p3", new Player("p3", "Charlie"));

        java.util.Set<String> selected = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            selected.add(spinService.spin(room).getPlayer().getId());
        }
        assertThat(selected).containsExactlyInAnyOrder("p1", "p2", "p3");
    }
}
