package com.remstem.game.model;

import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
public class Room {
    private final String code;
    private final String hostToken;
    private GameState state = GameState.LOBBY;
    private GameConfig config = new GameConfig();

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<SpinResult> history = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeRules = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger spinCount = new AtomicInteger(0);
    private final AtomicInteger joinCounter = new AtomicInteger(0);
    private final AtomicInteger currentTurnIndex = new AtomicInteger(0);

    public Room(String code, String hostToken) {
        this.code = code;
        this.hostToken = hostToken;
    }

    public List<Player> getPlayerList() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(Player::getJoinIndex))
                .collect(Collectors.toList());
    }
}
