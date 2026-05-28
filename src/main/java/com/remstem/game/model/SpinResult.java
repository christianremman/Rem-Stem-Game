package com.remstem.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class SpinResult {
    private Player player;
    private Challenge challenge;
    private double playerWheelAngle;
    private double challengeWheelAngle;
    private int spinNumber;

    @JsonIgnore private final Set<String> voters = ConcurrentHashMap.newKeySet();
    @JsonIgnore private final AtomicInteger votesDone = new AtomicInteger(0);
    @JsonIgnore private final AtomicInteger votesRefused = new AtomicInteger(0);
    @JsonIgnore private final AtomicBoolean resolved = new AtomicBoolean(false);

    public boolean markResolved() {
        return resolved.compareAndSet(false, true);
    }

    public boolean isResolved() {
        return resolved.get();
    }

    public SpinResult(Player player, Challenge challenge, double playerWheelAngle,
                      double challengeWheelAngle, int spinNumber) {
        this.player = player;
        this.challenge = challenge;
        this.playerWheelAngle = playerWheelAngle;
        this.challengeWheelAngle = challengeWheelAngle;
        this.spinNumber = spinNumber;
    }
}
