package com.remstem.game.model;

import lombok.Data;
import java.util.EnumSet;
import java.util.Set;

@Data
public class Player {
    private final String id;
    private final String name;
    private int joinIndex;
    private boolean connected = true;
    private String sessionId;

    private Set<PowerUpType> powerUps = EnumSet.allOf(PowerUpType.class);

    // stats
    private int timesSelected = 0;
    private int completed = 0;
    private int refused = 0;
    private int estimatedSips = 0;
    private int rulesSet = 0;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
