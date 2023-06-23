package securecompute.algebra.elliptic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.junit.jupiter.api.Test;
import securecompute.algebra.LargePrimeField;
import securecompute.algebra.PlusMinus;
import securecompute.algebra.elliptic.ProjectiveTwistedEdwardsCurve.Point;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectiveTwistedEdwardsCurveTest {
    private static final BigInteger P = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed", 16);
    private static final BigInteger L = new BigInteger("1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed", 16);
    private static final BigInteger B_X = new BigInteger("216936d3cd6e53fec0a4e231fdd6dc5c692cc7609525a7b2c9562d608f25d51a", 16);
    private static final BigInteger B_Y = new BigInteger("6666666666666666666666666666666666666666666666666666666666666658", 16);

    private static final List<BigInteger> P_MINUS_1_PRIME_FACTORS = Stream.of(
            "2", "2", "3", "65147", "74058212732561358302231226437062788676166966415465897661863160754340907"
    ).map(BigInteger::new).collect(ImmutableList.toImmutableList());

    private static final List<BigInteger> L_MINUS_1_PRIME_FACTORS = Stream.of(
            "2", "2", "3", "11", "198211423230930754013084525763697", "276602624281642239937218680557139826668747"
    ).map(BigInteger::new).collect(ImmutableList.toImmutableList());

    private static final LargePrimeField.Coset MINUS_ONE;

    private static final LargePrimeField Z_P = new LargePrimeField(P, P_MINUS_1_PRIME_FACTORS) {
        // a hack to speed up point doubling in ED_25519 by avoiding expensive multiplication by a = P - 1 (mod P):
        @Override
        public Coset product(Coset left, Coset right) {
            return right == MINUS_ONE ? left.negate() : super.product(left, right);
        }
    };
    private static final LargePrimeField Z_L = new LargePrimeField(L, L_MINUS_1_PRIME_FACTORS);

    private static final ProjectiveTwistedEdwardsCurve<LargePrimeField.Coset> ED_25519 = new ProjectiveTwistedEdwardsCurve<>(
            Z_P, MINUS_ONE = Z_P.one().negate(), Z_P.fromLong(-121665).divide(121666)
    );

    @Test
    void testIsCurvePoint() {
        assertTrue(ED_25519.isCurvePoint(Z_P.coset(B_X), Z_P.coset(B_Y), Z_P.one()));
        assertFalse(ED_25519.isCurvePoint(Z_P.coset(B_Y), Z_P.coset(B_X), Z_P.one()));
        assertTrue(ED_25519.isCurvePoint(Z_P.zero(), Z_P.one(), Z_P.one()));
        assertFalse(ED_25519.isCurvePoint(Z_P.zero(), Z_P.one(), Z_P.zero()));
        assertFalse(ED_25519.isCurvePoint(Z_P.one(), Z_P.zero(), Z_P.zero()));
        assertFalse(ED_25519.isCurvePoint(Z_P.zero(), Z_P.zero(), Z_P.zero()));

        assertThrows(IllegalArgumentException.class, () -> ED_25519.point(Z_P.zero(), Z_P.one(), Z_P.zero()));
    }

    @Test
    void testBigCurve() {
        assertNotNull(Z_P.sqrt(ED_25519.getA()).getWitness());
        assertNull(Z_P.sqrt(ED_25519.getD()).getWitness());

        assertEquals(Z_P.fromLong(2), Z_P.getPrimitiveElement());
        assertEquals(Z_L.fromLong(2), Z_L.getPrimitiveElement());

        Point<LargePrimeField.Coset> B = ED_25519.point(Z_P.coset(B_X), Z_P.coset(B_Y), Z_P.one());
//        for (int i = 0; i < 10000; i++) {
//            B.multiply(L);
//        }
        assertEquals(ED_25519.zero(), B.multiply(L));
    }

    @Test
    void testPlusMinusPoint() {
        assertEquals(ED_25519.zero(), ED_25519.plusMinusPoint(Z_P.one(), Z_P.one()).getWitness());
        assertNotNull(ED_25519.plusMinusPoint(MINUS_ONE, Z_P.one()).getWitness());
        assertNotNull(ED_25519.plusMinusPoint(Z_P.zero(), Z_P.one()).getWitness());
        assertNull(ED_25519.plusMinusPoint(Z_P.one(), Z_P.zero()).getWitness());
        assertNull(ED_25519.plusMinusPoint(Z_P.zero(), Z_P.zero()).getWitness());
    }

    @Test
    void testCompressDecompress() {
        Point<LargePrimeField.Coset> B = ED_25519.point(Z_P.coset(B_X), Z_P.coset(B_Y), Z_P.one());
        for (int i = 1; i <= 10; i++) {
            Point<LargePrimeField.Coset> p = B.multiply(i);
            PlusMinus<Point<LargePrimeField.Coset>> plusMinus = checkCompressDecompress(p);
            assertFalse(plusMinus.isHalfZero());
        }
    }

    @Test
    void testSmallSubgroup() {
        Random rnd = new Random(1234567);
        Multiset<Point<LargePrimeField.Coset>> pointMultiset = LinkedHashMultiset.create();
        for (int i = 0; i < 100; i++) {
            LargePrimeField.Coset y = Z_P.sampleUniformly(rnd);
            Point<LargePrimeField.Coset> point = ED_25519.plusMinusPoint(y, Z_P.one()).getWitness();
            if (point != null) {
                point = point.multiply(L);
                checkCompressDecompress(point);
                pointMultiset.add(point);
            }
        }
        for (Point<LargePrimeField.Coset> point : pointMultiset.elementSet()) {
            System.out.println("Got " + pointMultiset.count(point) + " occurrences of:");
            System.out.println(point);
            int i = 1;
            while (!point.multiply(i).equals(ED_25519.zero())) {
                assertTrue(i++ < 8);
            }
            System.out.println("Order = " + i +
                    ", isNegative = " + ED_25519.isNegative(point) +
                    ", isHalfZero = " + point.plusMinus().isHalfZero());
            System.out.println();
        }
        System.out.println("Got " + (100 - pointMultiset.size()) + " missing points.");
    }

    private static PlusMinus<Point<LargePrimeField.Coset>> checkCompressDecompress(Point<LargePrimeField.Coset> p) {
        P1PointCoordinates<LargePrimeField.Coset> p1Coordinates = ED_25519.plusMinus(p).coordinates();
        PlusMinus<Point<LargePrimeField.Coset>> plusMinus = ED_25519.plusMinusPoint(p1Coordinates);
        Point<LargePrimeField.Coset> decoded = plusMinus.getWitness();

        assertNotNull(decoded);
        assertEquals(ImmutableSet.of(p, p.negate()), ImmutableSet.of(decoded, decoded.negate()));
        assertFalse(decoded.normalCoordinates().get().x().getWitness().testBit(0));
        return plusMinus;
    }
}
