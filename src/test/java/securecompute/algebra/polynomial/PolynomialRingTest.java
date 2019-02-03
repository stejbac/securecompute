package securecompute.algebra.polynomial;

import org.junit.jupiter.api.Test;
import securecompute.algebra.IntegerRing;
import securecompute.helper.WithDefaultPolynomialRing;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PolynomialRingTest implements WithDefaultPolynomialRing<Integer> {

    private final PolynomialRing<Integer> ring = new PolynomialRing<>(IntegerRing.INSTANCE);

    @Override
    public PolynomialRing<Integer> getDefaultStructure() {
        return ring;
    }

    @Test
    void testToString() {
        assertAll(
                () -> assertEquals("0", zero().toString()),
                () -> assertEquals("1", one().toString()),
                () -> assertEquals("-1", fromInt(-1).toString()),
                () -> assertEquals("0 + 1*X", polynomial(0, 1).toString()),
                () -> assertEquals("1 + 0*X + 1*X^2", polynomial(1, 0, 1).toString()),
                () -> assertEquals("(-1) + (-3)*X + 0*X^2 + 10*X^3", polynomial(-1, -3, 0, 10).toString())
        );
    }

    @Test
    void testAdd() {
        assertAll(
                () -> assertEquals(zero(), fromInt(-1).add(one())),
                () -> assertEquals(zero(), polynomial(0, -1).add(polynomial(0, 1))),
                () -> assertEquals(polynomial(1, 0, 2),
                        polynomial(1, 1).add(polynomial(0, -1, 2)))
        );
    }

    @Test
    void testMultiply() {
        assertAll(
                () -> assertEquals(zero(), one().multiply(zero())),
                () -> assertEquals(zero(), zero().multiply(one())),
                () -> assertEquals(one(), one().multiply(one())),
                () -> assertEquals(polynomial(0, 0, 1),
                        polynomial(0, 1).multiply(polynomial(0, 1))),
                () -> assertEquals(polynomial(1, -3, 3, -1),
                        polynomial(1, -2, 1).multiply(polynomial(1, -1)))
        );
    }

    @Test
    void testPow() {
        assertAll(
                () -> assertEquals(one(), zero().pow(0)),
                () -> assertEquals(zero(), zero().pow(1)),
                () -> assertEquals(fromInt(64), fromInt(2).pow(6)),
                () -> assertEquals(polynomial(1), polynomial(1, 1).pow(0)),
                () -> assertEquals(polynomial(1, 1), polynomial(1, 1).pow(1)),
                () -> assertEquals(polynomial(1, 2, 1), polynomial(1, 1).pow(2)),
                () -> assertEquals(polynomial(1, 3, 3, 1), polynomial(1, 1).pow(3)),
                () -> assertEquals(polynomial(1, 4, 6, 4, 1), polynomial(1, 1).pow(4)),
                () -> assertEquals(polynomial(1, 5, 10, 10, 5, 1), polynomial(1, 1).pow(5))
        );
    }

    @Test
    void testShift() {
        assertAll(
                () -> assertEquals(zero(), zero().shift(Integer.MAX_VALUE)),
                () -> assertEquals(zero(), zero().shift(1)),
                () -> assertEquals(zero(), zero().shift(0)),
                () -> assertEquals(zero(), zero().shift(-1)),
                () -> assertEquals(zero(), zero().shift(Integer.MIN_VALUE)),
                () -> assertEquals(zero(), one().shift(Integer.MIN_VALUE)),
                () -> assertEquals(zero(), one().shift(-2)),
                () -> assertEquals(zero(), one().shift(-1)),
                () -> assertEquals(one(), one().shift(0)),
                () -> assertEquals(polynomial(0, 1), one().shift(1)),
                () -> assertEquals(polynomial(0, 0, 1), one().shift(2))
        );
    }
}
