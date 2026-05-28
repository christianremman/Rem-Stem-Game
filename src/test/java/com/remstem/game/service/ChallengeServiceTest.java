package com.remstem.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remstem.game.model.Challenge;
import com.remstem.game.model.ChallengeType;
import com.remstem.game.model.Intensity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class ChallengeServiceTest {

    private ChallengeService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ChallengeService(new ObjectMapper(), new DefaultResourceLoader());
        service.load();
    }

    // --- content loading ---

    @Test
    void load_truthsPool_notEmpty() {
        Challenge c = service.random(ChallengeType.TRUTH, Intensity.CHILL);
        assertThat(c).isNotNull();
        assertThat(c.getType()).isEqualTo(ChallengeType.TRUTH);
        assertThat(c.getText()).isNotBlank();
    }

    @Test
    void load_daresPool_notEmpty() {
        Challenge c = service.random(ChallengeType.DARE, Intensity.NORMAL);
        assertThat(c.getType()).isEqualTo(ChallengeType.DARE);
        assertThat(c.getText()).isNotBlank();
    }

    @Test
    void load_challengesPool_notEmpty() {
        Challenge c = service.random(ChallengeType.CHALLENGE, Intensity.SAVAGE);
        assertThat(c.getType()).isEqualTo(ChallengeType.CHALLENGE);
        assertThat(c.getText()).isNotBlank();
    }

    @Test
    void load_hotSeatPool_notEmpty() {
        Challenge c = service.random(ChallengeType.HOT_SEAT, Intensity.NORMAL);
        assertThat(c.getType()).isEqualTo(ChallengeType.HOT_SEAT);
        assertThat(c.getText()).isNotBlank();
    }

    @Test
    void load_mostLikelyToPool_notEmpty() {
        Challenge c = service.random(ChallengeType.MOST_LIKELY_TO, Intensity.NORMAL);
        assertThat(c.getType()).isEqualTo(ChallengeType.MOST_LIKELY_TO);
        assertThat(c.getText()).isNotBlank();
    }

    @Test
    void load_allTruthsHaveIntensity() {
        List<Challenge> sampled = sampleDistinct(ChallengeType.TRUTH, Intensity.SAVAGE, 30);
        assertThat(sampled).allSatisfy(c -> assertThat(c.getIntensity()).isNotNull());
    }

    // --- intensity compatibility ---

    @Test
    void random_chillGame_returnsOnlyChillContent() {
        List<Challenge> sampled = sampleDistinct(ChallengeType.TRUTH, Intensity.CHILL, 30);
        assertThat(sampled).allSatisfy(c ->
                assertThat(c.getIntensity()).isEqualTo(Intensity.CHILL));
    }

    @Test
    void random_normalGame_returnsChillOrNormal() {
        List<Challenge> sampled = sampleDistinct(ChallengeType.TRUTH, Intensity.NORMAL, 30);
        assertThat(sampled).allSatisfy(c ->
                assertThat(c.getIntensity()).isIn(Intensity.CHILL, Intensity.NORMAL));
    }

    @Test
    void random_savageGame_returnsAnyIntensity() {
        List<Challenge> sampled = sampleDistinct(ChallengeType.TRUTH, Intensity.SAVAGE, 100);
        boolean hasSavage = sampled.stream().anyMatch(c -> c.getIntensity() == Intensity.SAVAGE);
        boolean hasChill = sampled.stream().anyMatch(c -> c.getIntensity() == Intensity.CHILL);
        assertThat(hasSavage).isTrue();
        assertThat(hasChill).isTrue();
    }

    @Test
    void random_fallback_whenTypeHasNoPool_returnsTwoSipsFallback() {
        // DRINK / SOCIAL / GIVE_TAKE not in pool — service returns fallback
        Challenge fallback = service.random(ChallengeType.DRINK, Intensity.NORMAL);
        assertThat(fallback).isNotNull();
        assertThat(fallback.getText()).isNotBlank();
    }

    @Test
    void random_eachCall_canReturnDifferentContent() {
        // draw enough samples to confirm pool isn't always same item
        List<Challenge> samples = IntStream.range(0, 40)
                .mapToObj(i -> service.random(ChallengeType.DARE, Intensity.SAVAGE))
                .toList();
        long distinct = samples.stream().map(Challenge::getText).distinct().count();
        assertThat(distinct).isGreaterThan(1);
    }

    private List<Challenge> sampleDistinct(ChallengeType type, Intensity intensity, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> service.random(type, intensity))
                .toList();
    }
}
