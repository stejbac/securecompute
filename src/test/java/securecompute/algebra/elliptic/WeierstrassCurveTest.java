package securecompute.algebra.elliptic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;
import securecompute.algebra.*;
import securecompute.algebra.elliptic.WeierstrassCurve.Point;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WeierstrassCurveTest {
    private static final QuotientField<Integer> Z_223 = new QuotientField<>(IntegerRing.INSTANCE, 223);
    private static final WeierstrassCurve<QuotientField<Integer>.Coset> CURVE = new WeierstrassCurve<>(Z_223, Z_223.zero(), Z_223.fromLong(7));

    private static final BigInteger P = new BigInteger("0fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16);
    private static final BigInteger N = new BigInteger("0fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
    private static final BigInteger G_X = new BigInteger("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", 16);
    private static final BigInteger G_Y = new BigInteger("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8", 16);

    private static final QuotientField<BigInteger> Z_P = new QuotientField<>(BigIntegerRing.INSTANCE, P);
    private static final WeierstrassCurve<QuotientField<BigInteger>.Coset> SEC_P256K1 = new WeierstrassCurve<>(Z_P, Z_P.zero(), Z_P.fromLong(7));

    private static final List<BigInteger> N_MINUS_1_PRIME_FACTORS = Stream.of(
            "2", "2", "2", "2", "2", "2", "3", "149", "631", "107361793816595537", "174723607534414371449", "341948486974166000522343609283189"
    ).map(BigInteger::new).collect(ImmutableList.toImmutableList());

    @Test
    void testToString() {
        assertEquals("Infinity", CURVE.zero().toString());
        assertEquals("(47 (mod 223), 71 (mod 223))", point(CURVE, 47, 71).toString());
    }

    @Test
    void testIsCurvePoint() {
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(192), Z_223.fromLong(105)));
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(17), Z_223.fromLong(56)));
        assertFalse(CURVE.isCurvePoint(Z_223.fromLong(200), Z_223.fromLong(119)));
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(1), Z_223.fromLong(193)));
        assertFalse(CURVE.isCurvePoint(Z_223.fromLong(42), Z_223.fromLong(99)));

        assertThrows(IllegalArgumentException.class, () -> point(CURVE, 200, 119));
    }

    @Test
    void testSum() {
        assertEquals(point(CURVE, 220, 181), point(CURVE, 170, 142).add(point(CURVE, 60, 139)));
        assertEquals(point(CURVE, 215, 68), point(CURVE, 47, 71).add(point(CURVE, 17, 56)));
        assertEquals(point(CURVE, 47, 71), point(CURVE, 143, 98).add(point(CURVE, 76, 66)));
    }

    @Test
    void testMultiply() {
        assertEquals(point(CURVE, 49, 71), point(CURVE, 192, 105).multiply(2));
        assertEquals(point(CURVE, 64, 168), point(CURVE, 143, 98).multiply(2));
        assertEquals(point(CURVE, 36, 111), point(CURVE, 47, 71).multiply(2));
        assertEquals(point(CURVE, 194, 51), point(CURVE, 47, 71).multiply(4));
        assertEquals(point(CURVE, 116, 55), point(CURVE, 47, 71).multiply(8));
        assertEquals(point(CURVE, 69, 137), point(CURVE, 47, 71).multiply(12));
        assertEquals(point(CURVE, 194, 172), point(CURVE, 47, 71).multiply(17));
        assertEquals(CURVE.zero(), point(CURVE, 47, 71).multiply(21));
        assertNotEquals(CURVE.zero(), point(CURVE, 47, 71).multiply(7));
        assertNotEquals(CURVE.zero(), point(CURVE, 47, 71).multiply(3));
    }

    @Test
    void testBigCurve() {
        assertTrue(P.isProbablePrime(20));
        assertTrue(N.isProbablePrime(20));
        assertTrue(SEC_P256K1.isCurvePoint(Z_P.coset(G_X), Z_P.coset(G_Y)));
        Point<QuotientField<BigInteger>.Coset> G = point(SEC_P256K1, G_X, G_Y);
//        for (int i = 0; i < 10000; i++) {
//            G.multiply(N);
//        }
        assertEquals(SEC_P256K1.zero(), G.multiply(N));

        LargePrimeField baseField = new LargePrimeField(P);
        LargePrimeField scalarField = new LargePrimeField(N, N_MINUS_1_PRIME_FACTORS);

        assertEquals(baseField.coset(BigInteger.valueOf(3)), baseField.getPrimitiveElement());
        assertEquals(scalarField.coset(BigInteger.valueOf(7)), scalarField.getPrimitiveElement());
    }

    private static <E> Point<QuotientField<E>.Coset> point(WeierstrassCurve<QuotientField<E>.Coset> curve, E x, E y) {
        QuotientField<E> baseField = (QuotientField<E>) curve.getField();
        return curve.point(baseField.coset(x), baseField.coset(y));
    }

    @Test
    void testLenstraFactorization() {
        Random rnd = new Random(12345);
        BigInteger p = BigInteger.probablePrime(20, rnd);
        BigInteger q = BigInteger.probablePrime(20, rnd);
        LargePrimeField field = new LargePrimeField(p.multiply(q), false);

        assertTrue(Lists.partition(Ints.asList(IntStream.range(0, 1000000).toArray()), 200).stream()
                .anyMatch(list -> list.parallelStream().anyMatch(i -> {
                    LargePrimeField.Coset sqrtB = field.sampleUniformly(rnd);
                    WeierstrassCurve<LargePrimeField.Coset> curve = new WeierstrassCurve<>(field, field.sampleUniformly(rnd), sqrtB.pow(2));
                    Point<LargePrimeField.Coset> point = curve.point(field.zero(), sqrtB);
                    try {
                        for (int j = 2; j < IntMath.sqrt(i, RoundingMode.DOWN); j++) {
                            if (IntMath.isPrime(j)) {
                                point = curve.sum(Collections.nCopies(j, point));
                            }
                        }
                        return false;
                    } catch (ReducibleGeneratorException e) {
                        BigInteger r = (BigInteger) e.getFirstFactor(), s = (BigInteger) e.getSecondFactor();
                        assertEquals(p.min(q), r.min(s));
                        assertEquals(p.max(q), r.max(s));
                        return true;
                    }
                })));
    }
}
