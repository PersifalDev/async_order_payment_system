package dev.haritonenko.utils;

import java.util.concurrent.ThreadLocalRandom;

public class StubUtils {

    public static boolean chance(double chance) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static void randomSafeSleepMs(
            int min,
            int max
    ) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("Min and max values must be not negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("Min value must be less or equal to max");
        }
        try {
            var sleepMs = ThreadLocalRandom.current().nextInt(min, max + 1);
            if (sleepMs <= 0) {
                return;
            }
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static int getRandomBetween(
            int min,
            int max
    ) {
        if (min > max) {
            throw new IllegalArgumentException("Min value must be less or equal to max");
        }
        return ThreadLocalRandom.current()
                .nextInt(min, max + 1);
    }
}
