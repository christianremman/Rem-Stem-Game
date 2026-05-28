package com.remstem.game.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class SpinResult {
    private Player player;
    private Challenge challenge;
    private double playerWheelAngle;
    private double challengeWheelAngle;
    private int spinNumber;
}
