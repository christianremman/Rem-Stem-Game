package com.remstem.game.service;

import com.remstem.game.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

class RoomServiceTest {

    private RoomService service;

    @BeforeEach
    void setUp() {
        service = new RoomService();
    }

    // --- create ---

    @Test
    void create_returnsRoomWithSixCharCode() {
        Room room = service.create();
        assertThat(room.getCode()).hasSize(6);
        assertThat(room.getCode()).matches("[A-Z2-9]{6}");
    }

    @Test
    void create_returnsRoomWithHostToken() {
        Room room = service.create();
        assertThat(room.getHostToken()).isNotBlank();
    }

    @Test
    void create_roomStartsInLobbyState() {
        Room room = service.create();
        assertThat(room.getState()).isEqualTo(GameState.LOBBY);
    }

    @Test
    void create_twoRooms_haveDifferentCodes() {
        String code1 = service.create().getCode();
        String code2 = service.create().getCode();
        assertThat(code1).isNotEqualTo(code2);
    }

    // --- find ---

    @Test
    void find_returnsRoom_whenExists() {
        Room created = service.create();
        assertThat(service.find(created.getCode())).isPresent();
    }

    @Test
    void find_returnsEmpty_whenMissing() {
        assertThat(service.find("XXXXXX")).isEmpty();
    }

    @Test
    void find_isCaseInsensitive() {
        Room room = service.create();
        assertThat(service.find(room.getCode().toLowerCase())).isPresent();
    }

    // --- join ---

    @Test
    void join_addsPlayerToRoom() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        assertThat(room.getPlayers()).containsKey(player.getId());
        assertThat(player.getName()).isEqualTo("Alice");
    }

    @Test
    void join_throwsNoSuchElement_whenRoomNotFound() {
        assertThatThrownBy(() -> service.join("XXXXXX", "Alice"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void join_throwsIllegalState_whenRoomNotInLobby() {
        // Bug: no state guard — players can join started/ended games
        Room room = service.create();
        service.start(room.getCode());
        assertThatThrownBy(() -> service.join(room.getCode(), "LatePlayer"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void join_assignsUniquePlayerIds() {
        Room room = service.create();
        Player p1 = service.join(room.getCode(), "Alice");
        Player p2 = service.join(room.getCode(), "Bob");
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }

    @Test
    void join_playerStartsWithAllPowerUps() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        assertThat(player.getPowerUps()).containsExactlyInAnyOrder(
                PowerUpType.REVENGE, PowerUpType.SAFE, PowerUpType.DOUBLE_DOWN);
    }

    @Test
    void join_playerStartsConnected() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        assertThat(player.isConnected()).isTrue();
    }

    // --- start ---

    @Test
    void start_setsStateToActive() {
        Room room = service.create();
        service.start(room.getCode());
        assertThat(room.getState()).isEqualTo(GameState.ACTIVE);
    }

    @Test
    void start_throwsNoSuchElement_whenRoomNotFound() {
        assertThatThrownBy(() -> service.start("XXXXXX"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void start_throwsIllegalState_whenRoomAlreadyEnded() {
        // Bug: no guard — ended room can be revived to ACTIVE
        Room room = service.create();
        service.start(room.getCode());
        service.end(room.getCode());
        assertThatThrownBy(() -> service.start(room.getCode()))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- end ---

    @Test
    void end_setsStateToEnded() {
        Room room = service.create();
        service.start(room.getCode());
        service.end(room.getCode());
        assertThat(room.getState()).isEqualTo(GameState.ENDED);
    }

    @Test
    void end_returnsStatsMap() {
        Room room = service.create();
        service.join(room.getCode(), "Alice");
        service.start(room.getCode());
        var stats = service.end(room.getCode());
        assertThat(stats).containsKey("players");
        assertThat(stats).containsKey("totalSpins");
    }

    // --- updateConfig ---

    @Test
    void updateConfig_updatesSpinInterval() {
        Room room = service.create();
        GameConfig config = new GameConfig();
        config.setSpinIntervalMinutes(0.5);
        config.setIntensity(Intensity.SAVAGE);
        service.updateConfig(room.getCode(), config);
        assertThat(room.getConfig().getSpinIntervalMinutes()).isEqualTo(0.5);
        assertThat(room.getConfig().getIntensity()).isEqualTo(Intensity.SAVAGE);
    }

    @Test
    void updateConfig_silentlyIgnores_unknownCode() {
        assertThatCode(() -> service.updateConfig("XXXXXX", new GameConfig()))
                .doesNotThrowAnyException();
    }

    // --- removePlayer ---

    @Test
    void removePlayer_removesFromRoom() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        service.removePlayer(room.getCode(), player.getId());
        assertThat(room.getPlayers()).doesNotContainKey(player.getId());
    }

    // --- setConnected ---

    @Test
    void setConnected_false_marksPlayerDisconnected() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        service.setConnected(room.getCode(), player.getId(), false);
        assertThat(player.isConnected()).isFalse();
    }

    @Test
    void setConnected_true_marksPlayerReconnected() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        player.setConnected(false);
        service.setConnected(room.getCode(), player.getId(), true);
        assertThat(player.isConnected()).isTrue();
    }

    // --- findBySessionId / findPlayerBySessionId ---

    @Test
    void findBySessionId_returnsRoom_whenPlayerHasSession() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        player.setSessionId("sess-001");
        assertThat(service.findBySessionId("sess-001")).contains(room);
    }

    @Test
    void findBySessionId_returnsEmpty_whenNoMatch() {
        assertThat(service.findBySessionId("unknown-session")).isEmpty();
    }

    @Test
    void findPlayerBySessionId_returnsPlayer() {
        Room room = service.create();
        Player player = service.join(room.getCode(), "Alice");
        player.setSessionId("sess-002");
        assertThat(service.findPlayerBySessionId("sess-002")).contains(player);
    }

    // --- buildStats / assignTitle ---

    @Test
    void buildStats_assignsUnluckyOne_toMostSelected() {
        Room room = buildRoomWithHistory();
        Player alice = room.getPlayers().get("p1");
        alice.setTimesSelected(5);
        var stats = service.buildStats(room);
        var aliceStats = findPlayerStats(stats, "p1");
        assertThat(aliceStats.get("title")).isEqualTo("The Unlucky One");
    }

    @Test
    void buildStats_assignsBiggestCoward_toMostRefused() {
        Room room = buildRoomWithHistory();
        Player alice = room.getPlayers().get("p1");
        Player bob = room.getPlayers().get("p2");
        bob.setRefused(3);   // bob most refused
        alice.setTimesSelected(0);  // alice not most selected
        var stats = service.buildStats(room);
        var bobStats = findPlayerStats(stats, "p2");
        assertThat(bobStats.get("title")).isEqualTo("Biggest Coward");
    }

    @Test
    void buildStats_assignsIronStomach_toMostSips() {
        Room room = buildRoomWithHistory();
        room.getPlayers().get("p2").setEstimatedSips(10);
        var stats = service.buildStats(room);
        var bobStats = findPlayerStats(stats, "p2");
        assertThat(bobStats.get("title")).isEqualTo("Iron Stomach");
    }

    @Test
    void buildStats_assignsRuleSetter_toMostRules() {
        Room room = buildRoomWithHistory();
        room.getPlayers().get("p2").setRulesSet(2);
        var stats = service.buildStats(room);
        var bobStats = findPlayerStats(stats, "p2");
        assertThat(bobStats.get("title")).isEqualTo("Rule Setter");
    }

    @Test
    void buildStats_defaultsToPartyAnimal_whenNoMaxReached() {
        Room room = service.create();
        service.join(room.getCode(), "Alice");
        var stats = service.buildStats(room);
        @SuppressWarnings("unchecked")
        var players = (java.util.List<java.util.Map<String, Object>>) stats.get("players");
        assertThat(players).allSatisfy(p -> assertThat(p.get("title")).isEqualTo("Party Animal"));
    }

    @Test
    void buildStats_totalSpins_matchesRoomSpinCount() {
        Room room = service.create();
        room.getSpinCount().set(7);
        var stats = service.buildStats(room);
        assertThat(stats.get("totalSpins")).isEqualTo(7);
    }

    // --- helpers ---

    private Room buildRoomWithHistory() {
        Room room = service.create();
        Player alice = new Player("p1", "Alice");
        Player bob = new Player("p2", "Bob");
        room.getPlayers().put("p1", alice);
        room.getPlayers().put("p2", bob);
        return room;
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> findPlayerStats(java.util.Map<String, Object> stats, String id) {
        var players = (java.util.List<java.util.Map<String, Object>>) stats.get("players");
        return players.stream().filter(p -> id.equals(p.get("id"))).findFirst().orElseThrow();
    }
}
