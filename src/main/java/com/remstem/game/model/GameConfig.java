package com.remstem.game.model;

import lombok.Data;

@Data
public class GameConfig {
    private int spinIntervalMinutes = 15;
    private Intensity intensity = Intensity.NORMAL;
}
