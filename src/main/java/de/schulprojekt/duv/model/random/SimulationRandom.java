package de.schulprojekt.duv.model.random;

import java.util.concurrent.ThreadLocalRandom;

public class SimulationRandom {
    // Hilfsklasse für schnellen Thread-Safe Zufall
    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static double nextDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}