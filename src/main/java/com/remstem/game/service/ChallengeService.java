package com.remstem.game.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remstem.game.model.Challenge;
import com.remstem.game.model.ChallengeType;
import com.remstem.game.model.Intensity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private final Map<ChallengeType, List<Challenge>> pool = new EnumMap<>(ChallengeType.class);

    @PostConstruct
    void load() throws Exception {
        pool.put(ChallengeType.TRUTH, loadFile("content/truths.json", ChallengeType.TRUTH));
        pool.put(ChallengeType.DARE, loadFile("content/dares.json", ChallengeType.DARE));
        pool.put(ChallengeType.CHALLENGE, loadFile("content/challenges.json", ChallengeType.CHALLENGE));
        pool.put(ChallengeType.HOT_SEAT, loadFile("content/hotseat.json", ChallengeType.HOT_SEAT));
        pool.put(ChallengeType.MOST_LIKELY_TO, loadFile("content/mostlikelyto.json", ChallengeType.MOST_LIKELY_TO));
    }

    private List<Challenge> loadFile(String path, ChallengeType type) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        List<Map<String, Object>> raw = objectMapper.readValue(resource.getInputStream(),
                new TypeReference<>() {});
        List<Challenge> result = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            Challenge c = new Challenge();
            c.setType(type);
            c.setText((String) item.get("text"));
            c.setIntensity(Intensity.valueOf((String) item.get("intensity")));
            if (item.containsKey("subtype")) c.setSubtype((String) item.get("subtype"));
            result.add(c);
        }
        return result;
    }

    public Challenge random(ChallengeType type, Intensity intensity) {
        List<Challenge> candidates = pool.getOrDefault(type, List.of()).stream()
                .filter(c -> isCompatible(c.getIntensity(), intensity))
                .toList();
        if (candidates.isEmpty()) {
            candidates = pool.getOrDefault(type, List.of());
        }
        if (candidates.isEmpty()) {
            Challenge fallback = new Challenge();
            fallback.setType(type);
            fallback.setText("Take 2 sips.");
            fallback.setSips(2);
            fallback.setIntensity(intensity);
            return fallback;
        }
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private boolean isCompatible(Intensity content, Intensity game) {
        return switch (game) {
            case CHILL -> content == Intensity.CHILL;
            case NORMAL -> content == Intensity.CHILL || content == Intensity.NORMAL;
            case SAVAGE -> true;
        };
    }
}
