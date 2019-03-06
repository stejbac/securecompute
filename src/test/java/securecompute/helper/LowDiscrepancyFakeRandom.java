package securecompute.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.DoubleStream;

public class LowDiscrepancyFakeRandom extends Random {

    private static final double[] MULTIPLIERS = DoubleStream.of(2, 3, 5, 7, 11, 13, 17, 19, 23).map(Math::sqrt).toArray();

    private final int fieldsPerBound;
    private final double[] vector;
    private final Map<Integer, Integer> boundToIndexMap = new HashMap<>();

    public LowDiscrepancyFakeRandom(long seed) {
        this(seed, 1);
    }

    public LowDiscrepancyFakeRandom(long seed, int fieldsPerBound) {
        super(seed);
        this.fieldsPerBound = fieldsPerBound;
        vector = doubles().limit(MULTIPLIERS.length).toArray();
        boundToIndexMap.put(-1, 0);
    }

    @Override
    public double nextDouble() {
        if (boundToIndexMap.isEmpty()) {
            return super.nextDouble();
        }
        return vector[iterateAt(-1)];
    }

    private int iterateAt(int bound) {
        int i = boundToIndexMap.compute(bound, (b, j) -> j == null
                ? boundToIndexMap.size() * fieldsPerBound
                : j + 1 - ((j + 1) % fieldsPerBound == 0 ? fieldsPerBound : 0)
        );
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
