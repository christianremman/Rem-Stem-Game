package com.remstem.game.scheduler;

import com.remstem.game.model.*;
import com.remstem.game.service.RoomService;
import com.remstem.game.service.SpinService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpinScheduler {

    private final RoomService roomService;
    private final SpinService spinService;
    private final SimpMessagingTemplate messaging;

    @Value("${game.spin-warning-seconds:5}")
    private int warningSeconds;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> revealFutures = new ConcurrentHashMap<>();
    private final Map<String, List<ScheduledFuture<?>>> countdownFutures = new ConcurrentHashMap<>();

    public void start(String roomCode) {
        stop(roomCode);
        Room room = roomService.find(roomCode).orElseThrow();
        long intervalMs = (long) room.getConfig().getSpinIntervalMinutes() * 60 * 1000;
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> fireSpin(roomCode), intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        futures.put(roomCode, future);
        scheduleCountdowns(roomCode, intervalMs);
    }

    public void stop(String roomCode) {
        ScheduledFuture<?> f = futures.remove(roomCode);
        if (f != null) f.cancel(false);
        ScheduledFuture<?> reveal = revealFutures.remove(roomCode);
        if (reveal != null) reveal.cancel(false);
        cancelCountdowns(roomCode);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void cancelCountdowns(String roomCode) {
        List<ScheduledFuture<?>> old = countdownFutures.remove(roomCode);
        if (old != null) old.forEach(cf -> cf.cancel(false));
    }

    private void fireSpin(String roomCode) {
        try {
            Room room = roomService.find(roomCode).orElse(null);
            if (room == null || room.getState() != GameState.ACTIVE) {
                stop(roomCode);
                return;
            }
            if (room.getPlayerList().isEmpty()) return;

            SpinResult result = spinService.spin(room);
            broadcast(roomCode, WsEvent.of("SPIN_STARTING", Map.of(
                    "playerWheelAngle", result.getPlayerWheelAngle(),
                    "challengeWheelAngle", result.getChallengeWheelAngle()
            )));

            ScheduledFuture<?> reveal = executor.schedule(() -> {
                broadcast(roomCode, WsEvent.of("SPIN_RESULT", result));
                broadcast(roomCode, WsEvent.of("VOTE_WINDOW_OPEN", Map.of("durationSeconds", 30)));
            }, 4, TimeUnit.SECONDS);
            revealFutures.put(roomCode, reveal);

            long intervalMs = (long) room.getConfig().getSpinIntervalMinutes() * 60 * 1000;
            scheduleCountdowns(roomCode, intervalMs);
        } catch (Exception e) {
            log.error("Spin error for room {}", roomCode, e);
        }
    }

    private void scheduleCountdowns(String roomCode, long intervalMs) {
        cancelCountdowns(roomCode);
        List<ScheduledFuture<?>> list = new ArrayList<>();

        long warningMs = intervalMs - (warningSeconds * 1000L);
        if (warningMs > 0) {
            list.add(executor.schedule(() -> {
                Room room = roomService.find(roomCode).orElse(null);
                if (room != null && room.getState() == GameState.ACTIVE) {
                    broadcast(roomCode, WsEvent.of("SPIN_WARNING",
                            Map.of("secondsUntilSpin", warningSeconds)));
                }
            }, warningMs, TimeUnit.MILLISECONDS));
        }

        long countdownInterval = 10_000L;
        for (long t = intervalMs - countdownInterval; t > 0; t -= countdownInterval) {
            final long remaining = t;
            list.add(executor.schedule(() -> {
                Room room = roomService.find(roomCode).orElse(null);
                if (room != null && room.getState() == GameState.ACTIVE) {
                    broadcast(roomCode, WsEvent.of("COUNTDOWN",
                            Map.of("secondsUntilSpin", remaining / 1000)));
                }
            }, intervalMs - remaining, TimeUnit.MILLISECONDS));
        }

        countdownFutures.put(roomCode, list);
    }

    private void broadcast(String code, WsEvent event) {
        messaging.convertAndSend("/topic/rooms/" + code, event);
    }
}
