package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.constraint.LinearCode;

import java.util.Collections;
import java.util.List;

public class ShortenedLinearCode<V, E> implements LinearCode<V, E> {

    private final LinearCode<V, E> baseCode;
    private final int shortenNumber;

    public ShortenedLinearCode(LinearCode<V, E> baseCode, int shortenNumber) {
        if (!baseCode.isSystematic()) {
            throw new IllegalArgumentException("Code to be shortened must be systematic");
        }
        if (shortenNumber < 0 || shortenNumber > baseCode.dimension()) {
            throw new IllegalArgumentException("Shorten number must be between 0 and the code dimension inclusively");
        }
        this.baseCode = baseCode;
        this.shortenNumber = shortenNumber;
    }

    @Override
    public FiniteVectorSpace<V, E> symbolSpace() {
        return baseCode.symbolSpace();
    }

    @Override
    public int length() {
        return baseCode.length() - shortenNumber;
    }

    @Override
    public int dimension() {
        return baseCode.dimension() - shortenNumber;
    }

    @Override
    public int distance() {
        return baseCode.distance();
    }

    @Override
    public int codistance() {
        return Math.max(baseCode.codistance() - shortenNumber, 1);
    }

    @Override
    public List<V> parityCheck(List<V> vector) {
        return baseCode.parityCheck(zeroExtend(vector));
    }

    @Override
    public List<V> encode(List<V> message) {
        return baseCode.encode(zeroExtend(message)).subList(0, length());
    }

    @Override
    public List<V> decode(List<V> codeword) {
        return baseCode.decode(zeroExtend(codeword)).subList(0, dimension());
    }

    private List<V> zeroExtend(List<V> vector) {
        return ImmutableList.<V>builder()
                .addAll(vector)
                .addAll(Collections.nCopies(shortenNumber, symbolSpace().zero()))
                .build();
    }
}
