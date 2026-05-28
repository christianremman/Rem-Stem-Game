package com.remstem.game.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Challenge {
    private ChallengeType type;
    private String text;
    private Integer sips;
    private String subtype;
    private Intensity intensity;
    private int wheelIndex;
}
