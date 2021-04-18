package securecompute.algebra.elliptic;

import org.junit.jupiter.api.Test;
import securecompute.algebra.IntegerRing;
import securecompute.algebra.LargePrimeField;
import securecompute.algebra.QuotientField;
import securecompute.algebra.elliptic.ProjectiveWeierstrassCurve.Point;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectiveWeierstrassCurveTest {
    private static final QuotientField<Integer> Z_223 = new QuotientField<>(IntegerRing.INSTANCE, 223);
    private static final ProjectiveWeierstrassCurve<QuotientField<Integer>.Coset> CURVE = new ProjectiveWeierstrassCurve<>(Z_223, Z_223.zero(), Z_223.fromLong(7));

    private static final BigInteger P = new BigInteger("0fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16);
    private static final BigInteger N = new BigInteger("0fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
    private static final BigInteger G_X = new BigInteger("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", 16);
    private static final BigInteger G_Y = new BigInteger("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8", 16);

    private static final LargePrimeField Z_P = new LargePrimeField(P);
    private static final ProjectiveWeierstrassCurve<LargePrimeField.Coset> SEC_P256K1 = new ProjectiveWeierstrassCurve<>(Z_P, Z_P.zero(), Z_P.fromLong(7));

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
        assertTrue(SEC_P256K1.isCurvePoint(Z_P.coset(G_X), Z_P.coset(G_Y), Z_P.one()));
        Point<LargePrimeField.Coset> G = point(SEC_P256K1, G_X, G_Y);
//        for (int i = 0; i < 10000; i++) {
//            G.multiply(N);
//        }
        assertEquals(SEC_P256K1.zero(), G.multiply(N));
    }

    private static <E> Point<QuotientField<E>.Coset> point(ProjectiveWeierstrassCurve<QuotientField<E>.Coset> curve, E x, E y) {
        QuotientField<E> baseField = (QuotientField<E>) curve.getField();
        return point(curve, x, y, baseField.getBaseRing().one());
    }

    private static <E> Point<QuotientField<E>.Coset> point(ProjectiveWeierstrassCurve<QuotientField<E>.Coset> curve, E x, E y, E z) {
        QuotientField<E> baseField = (QuotientField<E>) curve.getField();
        return curve.point(baseField.coset(x), baseField.coset(y), baseField.coset(z));
    }
}
