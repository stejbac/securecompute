package securecompute.algebra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public enum BooleanField implements FiniteField<Boolean> {
    INSTANCE;

    private static final BigInteger SIZE = BigInteger.valueOf(2);

    @Override
    public BigInteger size() {
        return SIZE;
    }

    @Override
    public Boolean getPrimitiveElement() {
        return true;
    }

    @Override
    public Boolean sampleUniformly(Random random) {
        return random.nextBoolean();
    }

    @Override
    public Boolean fromBigInteger(BigInteger n) {
        return n.testBit(0);
    }

    @Override
    public Boolean sum(Boolean left, Boolean right) {
        return left ^ right;
    }

    @Override
    public Boolean product(Boolean left, Boolean right) {
        return left & right;
    }

    @Override
    public Boolean negative(Boolean elt) {
        return elt;
    }

    @Override
    public Boolean reciprocalOrZero(Boolean elt) {
        return elt;
    }

    public static Polynomial<Boolean> fromBinary(long value) {
        List<Boolean> coefficients = IntStream.range(0, 64 - Long.numberOfLeadingZeros(value))
                .mapToObj(n -> (value & 1L << n) != 0)
                .collect(ImmutableList.toImmutableList());

        return new FieldPolynomialRing<>(INSTANCE).polynomial(coefficients);
    }

    public static long toBinary(Polynomial<Boolean> polynomial) {
        return Streams.mapWithIndex(polynomial.getCoefficients().stream(), (b, n) -> b ? 1L << n : 0)
                .mapToLong(n -> n)
                .sum();
    }
}
