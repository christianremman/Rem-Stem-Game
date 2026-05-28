package com.remstem.game.controller;

import com.remstem.game.model.*;
import com.remstem.game.scheduler.SpinScheduler;
import com.remstem.game.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final SpinScheduler spinScheduler;
    private final SimpMessagingTemplate messaging;

    @PostMapping
    public ResponseEntity<Map<String, String>> create() {
        Room room = roomService.create();
        return ResponseEntity.ok(Map.of("roomCode", room.getCode(), "hostToken", room.getHostToken()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> get(@PathVariable String code) {
        return roomService.find(code)
                .map(r -> ResponseEntity.ok(roomView(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{code}/players")
    public ResponseEntity<?> join(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            Player player = roomService.join(code, body.get("name"));
            Room room = roomService.find(code).orElseThrow();
            messaging.convertAndSend("/topic/rooms/" + code,
                    WsEvent.of("PLAYER_JOINED", Map.of("player", playerView(player))));
            return ResponseEntity.ok(Map.of("playerId", player.getId(), "playerName", player.getName()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{code}/config")
    public ResponseEntity<?> config(@PathVariable String code, @RequestBody GameConfig config) {
        roomService.updateConfig(code, config);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<?> start(@PathVariable String code) {
        Room room = roomService.start(code);
        messaging.convertAndSend("/topic/rooms/" + code,
                WsEvent.of("GAME_STARTED", Map.of("config", room.getConfig())));
        spinScheduler.start(code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/end")
    public ResponseEntity<?> end(@PathVariable String code) {
        Map<String, Object> stats = roomService.end(code);
        spinScheduler.stop(code);
        messaging.convertAndSend("/topic/rooms/" + code, WsEvent.of("GAME_ENDED", stats));
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{code}/players/{playerId}")
    public ResponseEntity<?> removePlayer(@PathVariable String code, @PathVariable String playerId) {
        roomService.removePlayer(code, playerId);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> roomView(Room room) {
        return Map.of(
                "code", room.getCode(),
                "state", room.getState(),
                "config", room.getConfig(),
                "players", room.getPlayerList().stream().map(this::playerView).toList(),
                "activeRules", room.getActiveRules(),
                "spinCount", room.getSpinCount()
        );
    }

    private Map<String, Object> playerView(Player player) {
        return Map.of(
                "id", player.getId(),
                "name", player.getName(),
                "connected", player.isConnected(),
                "powerUps", player.getPowerUps()
        );
    }
}
