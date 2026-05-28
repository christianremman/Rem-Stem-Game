package com.remstem.game.service;

import com.remstem.game.model.*;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    @PreDestroy
    public void shutdown() { cleaner.shutdownNow(); }

    public Room create() {
        String code = generateCode();
        String hostToken = UUID.randomUUID().toString();
        Room room = new Room(code, hostToken);
        rooms.put(code, room);
        return room;
    }

    public Optional<Room> find(String code) {
        return Optional.ofNullable(rooms.get(code.toUpperCase()));
    }

    public Player join(String code, String name) {
        Room room = find(code).orElseThrow(() -> new NoSuchElementException("Room not found"));
        if (room.getState() != GameState.LOBBY) {
            throw new IllegalStateException("Cannot join room that is not in lobby state");
        }
        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, name);
        player.setJoinIndex(room.getJoinCounter().getAndIncrement());
        room.getPlayers().put(playerId, player);
        return player;
    }

    public void start(String code) {
        Room room = find(code).orElseThrow();
        if (room.getState() == GameState.ENDED) {
            throw new IllegalStateException("Cannot start a game that has already ended");
        }
        room.setState(GameState.ACTIVE);
    }

    public Map<String, Object> end(String code) {
        Room room = find(code).orElseThrow();
        room.setState(GameState.ENDED);
        cleaner.schedule(() -> rooms.remove(code), 1, TimeUnit.HOURS);
        return buildStats(room);
    }

    public void updateConfig(String code, GameConfig config) {
        find(code).ifPresent(r -> r.setConfig(config));
    }

    public void removePlayer(String code, String playerId) {
        find(code).ifPresent(r -> r.getPlayers().remove(playerId));
    }

    public void setConnected(String code, String playerId, boolean connected) {
        find(code).flatMap(r -> Optional.ofNullable(r.getPlayers().get(playerId)))
                  .ifPresent(p -> p.setConnected(connected));
    }

    public Optional<Room> findBySessionId(String sessionId) {
        return rooms.values().stream()
                .filter(r -> r.getPlayers().values().stream()
                        .anyMatch(p -> sessionId.equals(p.getSessionId())))
                .findFirst();
    }

    public Optional<Player> findPlayerBySessionId(String sessionId) {
        return rooms.values().stream()
                .flatMap(r -> r.getPlayers().values().stream())
                .filter(p -> sessionId.equals(p.getSessionId()))
                .findFirst();
    }

    public Map<String, Object> buildStats(Room room) {
        List<Player> all = room.getPlayerList();
        int maxSelected = all.stream().mapToInt(Player::getTimesSelected).max().orElse(0);
        int maxRefused  = all.stream().mapToInt(Player::getRefused).max().orElse(0);
        int maxSips     = all.stream().mapToInt(Player::getEstimatedSips).max().orElse(0);
        int maxRules    = all.stream().mapToInt(Player::getRulesSet).max().orElse(0);

        List<Map<String, Object>> playerStats = all.stream().map(p -> {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", p.getId());
            s.put("name", p.getName());
            s.put("timesSelected", p.getTimesSelected());
            s.put("completed", p.getCompleted());
            s.put("refused", p.getRefused());
            s.put("estimatedSips", p.getEstimatedSips());
            s.put("title", assignTitle(p, maxSelected, maxRefused, maxSips, maxRules));
            return s;
        }).toList();
        return Map.of("players", playerStats, "totalSpins", room.getSpinCount().get());
    }

    private String assignTitle(Player p, int maxSelected, int maxRefused, int maxSips, int maxRules) {
        if (p.getTimesSelected() == maxSelected && maxSelected > 0) return "The Unlucky One";
        if (p.getRefused() == maxRefused && maxRefused > 0) return "Biggest Coward";
        if (p.getEstimatedSips() == maxSips && maxSips > 0) return "Iron Stomach";
        if (p.getRulesSet() == maxRules && maxRules > 0) return "Rule Setter";
        return "Party Animal";
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }
}
