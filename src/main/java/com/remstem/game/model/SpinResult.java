package com.remstem.game.model;

import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class SpinResult {
    private Player player;
    private Challenge challenge;
    private double playerWheelAngle;
    private double challengeWheelAngle;
    private int spinNumber;
    private final Set<String> voters = ConcurrentHashMap.newKeySet();

    public SpinResult(Player player, Challenge challenge, double playerWheelAngle,
                      double challengeWheelAngle, int spinNumber) {
        this.player = player;
        this.challenge = challenge;
        this.playerWheelAngle = playerWheelAngle;
        this.challengeWheelAngle = challengeWheelAngle;
        this.spinNumber = spinNumber;
    }
}
