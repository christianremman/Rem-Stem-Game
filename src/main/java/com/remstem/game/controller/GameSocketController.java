package com.remstem.game.controller;

import com.remstem.game.model.*;
import com.remstem.game.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Random;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rooms/{code}/vote")
    public void vote(@DestinationVariable String code, @Payload Map<String, String> body) {
        Room room = roomService.find(code).orElse(null);
        if (room == null || room.getHistory().isEmpty()) return;

        SpinResult last = room.getHistory().get(room.getHistory().size() - 1);
        String vote = body.get("vote");
        String playerId = body.get("playerId");

        if (playerId.equals(last.getPlayer().getId())) return;
        if (!last.getVoters().add(playerId)) return;
        if (last.isResolved()) return;

        if ("BOO".equals(vote)) {
            messaging.convertAndSend("/topic/rooms/" + code,
                    WsEvent.of("BOO", Map.of("playerId", playerId)));
            return;
        }

        int done, refused;
        if ("REFUSED".equals(vote)) {
            refused = last.getVotesRefused().incrementAndGet();
            done = last.getVotesDone().get();
        } else {
            done = last.getVotesDone().incrementAndGet();
            refused = last.getVotesRefused().get();
        }

        int playerCount = room.getPlayers().size() - 1; // exclude selected player
        boolean majorityRefused = refused > playerCount / 2.0;
        boolean majorityDone = done > playerCount / 2.0;

        if ((majorityRefused || majorityDone) && last.markResolved()) {
            Player selected = last.getPlayer();
            if (refused > done) {
                selected.setRefused(selected.getRefused() + 1);
                selected.setEstimatedSips(selected.getEstimatedSips() + 2);
                messaging.convertAndSend("/topic/rooms/" + code,
                        WsEvent.of("VOTE_RESULT", Map.of("completed", false, "extraDrinks", 2)));
            } else {
                selected.setCompleted(selected.getCompleted() + 1);
                int sips = last.getChallenge().getSips() != null ? last.getChallenge().getSips() : 0;
                selected.setEstimatedSips(selected.getEstimatedSips() + sips);
                messaging.convertAndSend("/topic/rooms/" + code,
                        WsEvent.of("VOTE_RESULT", Map.of("completed", true, "extraDrinks", 0)));
            }
        }
    }

    @MessageMapping("/rooms/{code}/powerup")
    public void powerUp(@DestinationVariable String code, @Payload Map<String, Object> body) {
        Room room = roomService.find(code).orElse(null);
        if (room == null) return;

        String playerId = (String) body.get("playerId");
        PowerUpType type;
        try {
            type = PowerUpType.valueOf((String) body.get("type"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return;
        }
        String targetId = (String) body.getOrDefault("targetId", null);

        Player player = room.getPlayers().get(playerId);
        if (player == null || !player.getPowerUps().contains(type)) return;
        player.getPowerUps().remove(type);

        Map<String, Object> payload = targetId != null
                ? Map.of("playerId", playerId, "type", type, "targetId", targetId)
                : Map.of("playerId", playerId, "type", type);

        messaging.convertAndSend("/topic/rooms/" + code, WsEvent.of("POWERUP_USED", payload));

        if (type == PowerUpType.DOUBLE_DOWN) {
            boolean win = new Random().nextBoolean();
            messaging.convertAndSend("/topic/rooms/" + code,
                    WsEvent.of("DOUBLE_DOWN_RESULT", Map.of("win", win, "playerId", playerId)));
        } else if (type == PowerUpType.REVENGE && targetId != null) {
            if (!room.getHistory().isEmpty()) {
                SpinResult last = room.getHistory().get(room.getHistory().size() - 1);
                if (!last.isResolved() && last.getPlayer().getId().equals(playerId)) {
                    Player target = room.getPlayers().get(targetId);
                    if (target != null) {
                        last.setPlayer(target);
                        target.setTimesSelected(target.getTimesSelected() + 1);
                        messaging.convertAndSend("/topic/rooms/" + code,
                                WsEvent.of("SPIN_RESULT", last));
                    }
                }
            }
        }
    }

    @MessageMapping("/rooms/{code}/rule")
    public void addRule(@DestinationVariable String code, @Payload Map<String, String> body) {
        Room room = roomService.find(code).orElse(null);
        if (room == null) return;

        String rule = body.get("rule");
        String addedBy = body.get("addedBy");
        String playerId = body.get("playerId");

        if (rule == null || rule.isBlank()) return;
        if (addedBy == null) addedBy = "Unknown";

        room.getActiveRules().add(rule);
        Player rulePlayer = room.getPlayers().get(playerId);
        if (rulePlayer != null) rulePlayer.setRulesSet(rulePlayer.getRulesSet() + 1);

        messaging.convertAndSend("/topic/rooms/" + code,
                WsEvent.of("RULE_ADDED", Map.of("rule", rule, "addedBy", addedBy)));
    }

    @MessageMapping("/rooms/{code}/register")
    public void register(@DestinationVariable String code, @Payload Map<String, String> body,
                         @Header("simpSessionId") String sessionId) {
        String playerId = body.get("playerId");
        roomService.find(code).ifPresent(room -> {
            Player p = room.getPlayers().get(playerId);
            if (p != null) {
                p.setSessionId(sessionId);
                p.setConnected(true);
            }
        });
    }
}
