package securecompute.algebra.elliptic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import securecompute.StreamUtils;
import securecompute.algebra.*;
import securecompute.algebra.EuclideanDomain.DivModResult;
import securecompute.algebra.elliptic.ProjectiveWeierstrassCurve.Point;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectiveWeierstrassCurveTest {
    private static final QuotientField<Integer> Z_223 = new QuotientField<>(IntegerRing.INSTANCE, 223);
    private static final ProjectiveWeierstrassCurve<QuotientField<Integer>.Coset> CURVE = new ProjectiveWeierstrassCurve<>(
            Z_223, Z_223.zero(), Z_223.fromLong(7));

    private static final BigInteger P = new BigInteger("0fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16);
    private static final BigInteger N = new BigInteger("0fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
    private static final BigInteger G_X = new BigInteger("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", 16);
    private static final BigInteger G_Y = new BigInteger("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8", 16);

    private static final List<BigInteger> P_MINUS_1_PRIME_FACTORS = Stream.of(
            "2", "3", "7", "13441", "205115282021455665897114700593932402728804164701536103180137503955397371"
    ).map(BigInteger::new).collect(ImmutableList.toImmutableList());

    private static final List<BigInteger> N_MINUS_1_PRIME_FACTORS = Stream.of(
            "2", "2", "2", "2", "2", "2", "3", "149", "631", "107361793816595537", "174723607534414371449", "341948486974166000522343609283189"
    ).map(BigInteger::new).collect(ImmutableList.toImmutableList());

    private static final LargePrimeField Z_P = new LargePrimeField(P, P_MINUS_1_PRIME_FACTORS);
    private static final LargePrimeField Z_N = new LargePrimeField(N, N_MINUS_1_PRIME_FACTORS);
    private static final ProjectiveWeierstrassCurve<LargePrimeField.Coset> SEC_P256K1 = new ProjectiveWeierstrassCurve<>(
            Z_P, Z_P.zero(), Z_P.fromLong(7));

    @Test
    void testToString() {
        assertEquals("(0 (mod 223) : 1 (mod 223) : 0 (mod 223))", CURVE.zero().toString());
        assertEquals("(0 (mod 223) : 1 (mod 223) : 0 (mod 223))", point(CURVE, 0, -2, 0).toString());
        assertEquals("(47 (mod 223) : 71 (mod 223) : 1 (mod 223))", point(CURVE, 47, 71).toString());
        assertEquals("(47 (mod 223) : 71 (mod 223) : 1 (mod 223))", point(CURVE, 470, 710, 10).toString());
    }

    @Test
    void testEquals() {
        assertEquals(point(CURVE, 47, 71), point(CURVE, 470, 710, 10));
        assertNotEquals(point(CURVE, 47, 71), point(CURVE, 470, -710, 10));
    }

    @Test
    void testIsCurvePoint() {
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(192), Z_223.fromLong(105), Z_223.one()));
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(17), Z_223.fromLong(56), Z_223.one()));
        assertFalse(CURVE.isCurvePoint(Z_223.fromLong(200), Z_223.fromLong(119), Z_223.one()));
        assertTrue(CURVE.isCurvePoint(Z_223.fromLong(1), Z_223.fromLong(193), Z_223.one()));
        assertFalse(CURVE.isCurvePoint(Z_223.fromLong(42), Z_223.fromLong(99), Z_223.one()));
        assertFalse(CURVE.isCurvePoint(Z_223.zero(), Z_223.zero(), Z_223.zero()));

        assertThrows(IllegalArgumentException.class, () -> point(CURVE, 200, 119));
    }

    @Test
    void testSum() {
        assertEquals(point(CURVE, 220, 181), point(CURVE, 170, 142).add(point(CURVE, 60, 139)));
        assertEquals(point(CURVE, 215, 68), point(CURVE, 47, 71).add(point(CURVE, 17, 56)));
        assertEquals(point(CURVE, 47, 71), point(CURVE, 143, 98).add(point(CURVE, 76, 66)));
        assertEquals(point(CURVE, 36, 111), point(CURVE, 47, 71).add(point(CURVE, 47, 71)));
        assertEquals(point(CURVE, 47, 71), point(CURVE, 0, 1, 0).add(point(CURVE, 47, 71)));
        assertEquals(point(CURVE, 47, 71), CURVE.zero().add(point(CURVE, 47, 71)));
        assertEquals(point(CURVE, 47, 71), point(CURVE, 47, 71).add(CURVE.zero()));
        assertEquals(CURVE.zero(), CURVE.zero().add(CURVE.zero()));
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
        assertEquals(Z_P.zero(), SEC_P256K1.getA());
        assertEquals(Z_P.fromLong(7), SEC_P256K1.getB());

        assertTrue(SEC_P256K1.isCurvePoint(Z_P.coset(G_X), Z_P.coset(G_Y), Z_P.one()));
        Point<LargePrimeField.Coset> G = point(SEC_P256K1, G_X, G_Y);
//        for (int i = 0; i < 10000; i++) {
//            G.multiply(N);
//        }
        assertEquals(SEC_P256K1.zero(), G.multiply(N));
    }

    @Test
    void testPlusMinusPoint() {
        assertEquals(SEC_P256K1.zero(), SEC_P256K1.plusMinusPoint(Z_P.one(), Z_P.zero()).getWitness());
        assertTrue(SEC_P256K1.plusMinusPoint(Z_P.one(), Z_P.zero()).isHalfZero());
        assertNull(SEC_P256K1.plusMinusPoint(Z_P.zero(), Z_P.zero()).getWitness());
        assertNull(SEC_P256K1.plusMinusPoint(Z_P.zero(), Z_P.one()).getWitness());
        assertNotNull(SEC_P256K1.plusMinusPoint(Z_P.one(), Z_P.one()).getWitness());
    }

    @Test
    void testCompressDecompress() {
        Point<LargePrimeField.Coset> G = point(SEC_P256K1, G_X, G_Y);
        for (int i = 1; i <= 10; i++) {
            Point<LargePrimeField.Coset> p = G.multiply(i);
            PlusMinus<Point<LargePrimeField.Coset>> plusMinus = checkCompressDecompress(p);
            assertFalse(plusMinus.isHalfZero());
        }
    }

    private static PlusMinus<Point<LargePrimeField.Coset>> checkCompressDecompress(Point<LargePrimeField.Coset> p) {
        P1PointCoordinates<LargePrimeField.Coset> p1Coordinates = SEC_P256K1.plusMinus(p).coordinates();
        PlusMinus<Point<LargePrimeField.Coset>> plusMinus = SEC_P256K1.plusMinusPoint(p1Coordinates);
        Point<LargePrimeField.Coset> decoded = plusMinus.getWitness();

        assertNotNull(decoded);
        assertEquals(ImmutableSet.of(p, p.negate()), ImmutableSet.of(decoded, decoded.negate()));
        assertFalse(decoded.normalCoordinates().get().y().getWitness().testBit(0));
        return plusMinus;
    }

    @Test
    void testBigCurveEndomorphism() {
        LargePrimeField.Coset cubeRootOfUnityModP = Z_P.getPrimitiveElement().pow(P.divide(BigInteger.valueOf(3)));
        LargePrimeField.Coset cubeRootOfUnityModN = Z_N.getPrimitiveElement().pow(N.divide(BigInteger.valueOf(3)));

        assertEquals(Z_P.one(), cubeRootOfUnityModP.pow(3));
        assertEquals(Z_N.one(), cubeRootOfUnityModN.pow(3));

        Point<LargePrimeField.Coset> G = point(SEC_P256K1, G_X, G_Y), G_mapped;
        assertEquals(multiplyX(G, cubeRootOfUnityModP), G_mapped = G.multiply(cubeRootOfUnityModN.getWitness()));
        assertEquals(multiplyX(G, cubeRootOfUnityModP.pow(2)), G.multiply(cubeRootOfUnityModN.pow(2).getWitness()));

        BigIntegerRing ring = BigIntegerRing.INSTANCE;
        List<GlvPair> glvPairs = StreamUtils.takeWhile(
                EuclideanDomain.partialGcdExtResults(ring, Z_N.size(), cubeRootOfUnityModN.getWitness()),
                r -> ring.size(r.left()) >= ring.size(r.y())
        ).map(r -> new GlvPair(r.left(), r.y())).collect(ImmutableList.toImmutableList());

        assertEquals(72, glvPairs.size());

        Random rnd = new Random(6543);
        for (int i = 0; i < 10; i++) {
            LargePrimeField.Coset k = Z_N.sampleUniformly(rnd);
            GlvPair glvPair = toGlvForm(glvPairs, k.getWitness());
            assertEquals(k, Z_N.fromBigInteger(glvPair.k).add(cubeRootOfUnityModN.multiply(glvPair.m)));
            assertEquals(G.multiply(k.getWitness()), G.multiply(glvPair.k).add(G_mapped.multiply(glvPair.m)));
        }
//        for (int i = 0; i < 1000000; i++) {
//            toGlvForm(glvPairs, Z_N.sampleUniformly(rnd).getWitness());
//        }
    }

    private static GlvPair toGlvForm(List<GlvPair> glvPairs, BigInteger k) {
        BigIntegerRing ring = BigIntegerRing.INSTANCE;
        BigInteger m = ring.zero();
        for (GlvPair glvPair : glvPairs) {
            DivModResult<BigInteger> divModResult = ring.divMod(k, glvPair.k);
            m = ring.sum(m, ring.product(glvPair.m, divModResult.getQuotient()));
            k = divModResult.getRemainder();
        }
        return new GlvPair(k, m);
    }

    private static Point<LargePrimeField.Coset> multiplyX(Point<LargePrimeField.Coset> point, LargePrimeField.Coset u) {
        P2PointCoordinates<LargePrimeField.Coset> coordinates = point.normalCoordinates().get();
        return SEC_P256K1.point(coordinates.x().multiply(u), coordinates.y(), coordinates.z());
    }

    private static <E> Point<QuotientField<E>.Coset> point(ProjectiveWeierstrassCurve<QuotientField<E>.Coset> curve, E x, E y) {
        QuotientField<E> baseField = (QuotientField<E>) curve.getField();
        return point(curve, x, y, baseField.getBaseRing().one());
    }

    private static <E> Point<QuotientField<E>.Coset> point(ProjectiveWeierstrassCurve<QuotientField<E>.Coset> curve, E x, E y, E z) {
        QuotientField<E> baseField = (QuotientField<E>) curve.getField();
        return curve.point(baseField.coset(x), baseField.coset(y), baseField.coset(z));
    }

    private static class GlvPair {
        final BigInteger k, m;

        GlvPair(BigInteger k, BigInteger m) {
            this.k = k;
            this.m = m;
        }
    }
}
