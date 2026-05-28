package com.remstem.game.service;

import com.remstem.game.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertThat(room.getSpinCount()).isEqualTo(1);
    }

    @Test
    void spin_incrementsSpinCountPerSpin() {
        Room room = new Room("TEST01", "token");
        room.setConfig(new GameConfig());
        room.getPlayers().put("p1", new Player("p1", "Alice"));

        spinService.spin(room);
        spinService.spin(room);

        assertThat(room.getSpinCount()).isEqualTo(2);
    }
}
