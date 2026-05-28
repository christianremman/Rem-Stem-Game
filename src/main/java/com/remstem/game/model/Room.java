package com.remstem.game.model;

import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    private final String code;
    private final String hostToken;
    private GameState state = GameState.LOBBY;
    private GameConfig config = new GameConfig();

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<SpinResult> history = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeRules = Collections.synchronizedList(new ArrayList<>());

    private int spinCount = 0;

    public Room(String code, String hostToken) {
        this.code = code;
        this.hostToken = hostToken;
    }

    public List<Player> getPlayerList() {
        return new ArrayList<>(players.values());
    }
}
