package com.remstem.game.model;

import lombok.Data;

@Data
public class GameConfig {
    private double spinIntervalMinutes = 15;
    private Intensity intensity = Intensity.NORMAL;
}
