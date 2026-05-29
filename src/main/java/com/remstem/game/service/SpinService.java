package com.remstem.game.service;

import com.remstem.game.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SpinService {

    private final ChallengeService challengeService;

    // weights per intensity [DRINK, SOCIAL, GIVE_TAKE, TRUTH, DARE, CHALLENGE, HOT_SEAT, MOST_LIKELY_TO]
    private static final Map<Intensity, int[]> WEIGHTS = Map.of(
        Intensity.CHILL,  new int[]{15, 20, 20, 15, 10, 10, 5, 5},
        Intensity.NORMAL, new int[]{15, 10, 10, 15, 15, 15, 10, 10},
        Intensity.SAVAGE, new int[]{15, 5,  5,  10, 20, 20, 10, 15}
    );

    private static final ChallengeType[] TYPES = {
        ChallengeType.DRINK, ChallengeType.SOCIAL, ChallengeType.GIVE_TAKE,
        ChallengeType.TRUTH, ChallengeType.DARE, ChallengeType.CHALLENGE,
        ChallengeType.HOT_SEAT, ChallengeType.MOST_LIKELY_TO
    };

    private static final ChallengeType[] ROMANTIC_TYPES = {
        ChallengeType.PERSONAL_QUESTION, ChallengeType.PHYSICAL_TOUCH,
        ChallengeType.ROMANTIC_DARE, ChallengeType.DRINK
    };

    public SpinResult spin(Room room) {
        if (room.getConfig().getGameMode() == GameMode.ROMANTIC) {
            return spinRomantic(room);
        }
        List<Player> players = room.getPlayerList();
        if (players.isEmpty()) throw new IllegalStateException("Cannot spin with no players in room");
        int selectedIdx = ThreadLocalRandom.current().nextInt(players.size());
        Player selected = players.get(selectedIdx);

        Intensity intensity = room.getConfig().getIntensity();
        ChallengeType type = weightedRandom(WEIGHTS.get(intensity));
        Challenge challenge = buildChallenge(type, intensity);

        int playerCount = players.size();
        double playerAngle = angleForIndex(selectedIdx, playerCount);
        double challengeAngle = angleForIndex(indexOf(TYPES, type), TYPES.length);

        challenge.setWheelIndex(indexOf(TYPES, type));

        int spinNumber = room.getSpinCount().incrementAndGet();
        selected.setTimesSelected(selected.getTimesSelected() + 1);

        SpinResult result = new SpinResult(selected, challenge, playerAngle, challengeAngle, spinNumber);
        room.getHistory().add(result);
        return result;
    }

    private SpinResult spinRomantic(Room room) {
        List<Player> players = room.getPlayerList();
        if (players.isEmpty()) throw new IllegalStateException("Cannot spin with no players in room");

        int idx = room.getCurrentTurnIndex().getAndIncrement() % players.size();
        Player selected = players.get(idx);

        ChallengeType type = ROMANTIC_TYPES[ThreadLocalRandom.current().nextInt(ROMANTIC_TYPES.length)];
        Challenge challenge = buildRomanticChallenge(type);

        double challengeAngle = angleForIndex(indexOf(ROMANTIC_TYPES, type), ROMANTIC_TYPES.length);
        challenge.setWheelIndex(indexOf(ROMANTIC_TYPES, type));

        int spinNumber = room.getSpinCount().incrementAndGet();
        selected.setTimesSelected(selected.getTimesSelected() + 1);

        SpinResult result = new SpinResult(selected, challenge, 0.0, challengeAngle, spinNumber);
        room.getHistory().add(result);
        return result;
    }

    private Challenge buildRomanticChallenge(ChallengeType type) {
        if (type == ChallengeType.DRINK) {
            Challenge c = new Challenge();
            c.setType(ChallengeType.DRINK);
            c.setSips(1);
            c.setText("Take a sip together.");
            c.setIntensity(Intensity.NORMAL);
            return c;
        }
        return challengeService.random(type, Intensity.SAVAGE);
    }

    private Challenge buildChallenge(ChallengeType type, Intensity intensity) {
        return switch (type) {
            case DRINK -> {
                int sips = intensity == Intensity.SAVAGE ? (ThreadLocalRandom.current().nextInt(3) + 3)
                         : intensity == Intensity.CHILL  ? (ThreadLocalRandom.current().nextInt(2) + 1)
                         : (ThreadLocalRandom.current().nextInt(3) + 2);
                Challenge c = new Challenge();
                c.setType(ChallengeType.DRINK);
                c.setSips(sips);
                c.setText("Drink " + sips + " sip" + (sips > 1 ? "s" : "") + ".");
                c.setIntensity(intensity);
                yield c;
            }
            case SOCIAL -> {
                int sips = intensity == Intensity.SAVAGE ? 2 : 1;
                Challenge c = new Challenge();
                c.setType(ChallengeType.SOCIAL);
                c.setSips(sips);
                c.setText("Everyone drinks " + sips + " sip" + (sips > 1 ? "s" : "") + ".");
                c.setIntensity(intensity);
                yield c;
            }
            case GIVE_TAKE -> {
                Challenge c = new Challenge();
                c.setType(ChallengeType.GIVE_TAKE);
                c.setSips(2);
                c.setText("Give or take 2 sips from a player of your choice.");
                c.setIntensity(intensity);
                yield c;
            }
            default -> challengeService.random(type, intensity);
        };
    }

    private ChallengeType weightedRandom(int[] weights) {
        int total = Arrays.stream(weights).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) return TYPES[i];
        }
        return TYPES[TYPES.length - 1];
    }

    private double angleForIndex(int index, int total) {
        double segmentDeg = 360.0 / total;
        double randomOffset = (ThreadLocalRandom.current().nextDouble() - 0.5) * segmentDeg * 0.8;
        return (index * segmentDeg + segmentDeg / 2.0 + randomOffset) % 360.0;
    }

    private int indexOf(ChallengeType[] arr, ChallengeType t) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == t) return i;
        return 0;
    }
}
