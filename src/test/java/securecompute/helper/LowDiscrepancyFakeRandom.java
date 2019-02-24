package securecompute.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.DoubleStream;

public class LowDiscrepancyFakeRandom extends Random {

    private static final double[] MULTIPLIERS = DoubleStream.of(2, 3, 5, 7, 11).map(Math::sqrt).toArray();

    private final double[] vector;
    private final Map<Integer, Integer> BOUND_TO_INDEX_MAP = new HashMap<>();

    public LowDiscrepancyFakeRandom(long seed) {
        super(seed);
        vector = doubles().limit(MULTIPLIERS.length).toArray();
        BOUND_TO_INDEX_MAP.put(-1, 0);
    }

    @Override
    public double nextDouble() {
        if (BOUND_TO_INDEX_MAP.isEmpty()) {
            return super.nextDouble();
        }
        return vector[iterateAt(-1)];
    }

    private int iterateAt(int bound) {
        int i = BOUND_TO_INDEX_MAP.computeIfAbsent(bound, b -> BOUND_TO_INDEX_MAP.size());
        vector[i] = vector[i] + MULTIPLIERS[i];
        vector[i] = vector[i] - Math.floor(vector[i]);
        return i;
    }

    @Override
    public int nextInt(int bound) {
        if (bound < 0) {
            throw new IllegalArgumentException();
        }
        return (int) (vector[iterateAt(bound)] * bound);
    }
}
