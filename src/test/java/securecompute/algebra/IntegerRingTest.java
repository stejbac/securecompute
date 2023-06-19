package securecompute.algebra;

import org.junit.jupiter.api.Test;
import securecompute.helper.WithDefaultEuclideanDomain;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegerRingTest implements WithDefaultEuclideanDomain<Integer> {
    @Override
    public EuclideanDomain<Integer> getDefaultStructure() {
        return IntegerRing.INSTANCE;
    }

    @Test
    void testDivMod() {
        assertEquals(DivModResult.of(2, 4), divMod(24, 10));
        assertEquals(DivModResult.of(-3, -6), divMod(24, -10));
        assertEquals(DivModResult.of(-3, 6), divMod(-24, 10));
        assertEquals(DivModResult.of(2, -4), divMod(-24, -10));
        assertEquals(DivModResult.of(2, 0), divMod(24, 12));
        assertEquals(DivModResult.of(-2, 0), divMod(24, -12));
        assertEquals(DivModResult.of(-2, 0), divMod(-24, 12));
        assertEquals(DivModResult.of(2, 0), divMod(-24, -12));
    }

    @Test
    void testGcdExt() {
        assertEquals(GcdExtResult.of(0, 0, 0, 0, 0), gcdExt(0, 0));
        assertEquals(GcdExtResult.of(0, 1, 1, 0, 1), gcdExt(0, 1));
        assertEquals(GcdExtResult.of(1, 0, 1, 1, 0), gcdExt(1, 0));
        assertEquals(GcdExtResult.of(0, 1, 1, 1, 1), gcdExt(1, 1));
        assertEquals(GcdExtResult.of(1, 0, 1, 1, 2), gcdExt(1, 2));
        assertEquals(GcdExtResult.of(0, 1, 1, 2, 1), gcdExt(2, 1));
        assertEquals(GcdExtResult.of(5, -2, 2, 5, 12), gcdExt(10, 24));
        assertEquals(GcdExtResult.of(3, -7, 2, 12, 5), gcdExt(24, 10));
        assertEquals(GcdExtResult.of(3, -1, 5, 2, 5), gcdExt(10, 25));
        assertEquals(GcdExtResult.of(1, -2, 5, 5, 2), gcdExt(25, 10));

        assertEquals(GcdExtResult.of(-1, 0, 1, -1, 0), gcdExt(-1, 0));
        assertEquals(GcdExtResult.of(0, 1, 1, -1, 1), gcdExt(-1, 1));
        assertEquals(GcdExtResult.of(1, 1, 1, -1, 2), gcdExt(-1, 2));
        assertEquals(GcdExtResult.of(0, 1, 1, -2, 1), gcdExt(-2, 1));
        assertEquals(GcdExtResult.of(7, 3, 2, -5, 12), gcdExt(-10, 24));
        assertEquals(GcdExtResult.of(2, 5, 2, -12, 5), gcdExt(-24, 10));
        assertEquals(GcdExtResult.of(2, 1, 5, -2, 5), gcdExt(-10, 25));
        assertEquals(GcdExtResult.of(1, 3, 5, -5, 2), gcdExt(-25, 10));

        assertEquals(GcdExtResult.of(0, -1, 1, 0, -1), gcdExt(0, -1));
        assertEquals(GcdExtResult.of(0, -1, 1, 1, -1), gcdExt(1, -1));
        assertEquals(GcdExtResult.of(-1, -1, 1, 1, -2), gcdExt(1, -2));
        assertEquals(GcdExtResult.of(0, -1, 1, 2, -1), gcdExt(2, -1));
        assertEquals(GcdExtResult.of(-7, -3, 2, 5, -12), gcdExt(10, -24));
        assertEquals(GcdExtResult.of(-2, -5, 2, 12, -5), gcdExt(24, -10));
        assertEquals(GcdExtResult.of(-2, -1, 5, 2, -5), gcdExt(10, -25));
        assertEquals(GcdExtResult.of(-1, -3, 5, 5, -2), gcdExt(25, -10));

        assertEquals(GcdExtResult.of(0, -1, 1, -1, -1), gcdExt(-1, -1));
        assertEquals(GcdExtResult.of(-1, 0, 1, -1, -2), gcdExt(-1, -2));
        assertEquals(GcdExtResult.of(0, -1, 1, -2, -1), gcdExt(-2, -1));
        assertEquals(GcdExtResult.of(-5, 2, 2, -5, -12), gcdExt(-10, -24));
        assertEquals(GcdExtResult.of(-3, 7, 2, -12, -5), gcdExt(-24, -10));
        assertEquals(GcdExtResult.of(-3, 1, 5, -2, -5), gcdExt(-10, -25));
        assertEquals(GcdExtResult.of(-1, 2, 5, -5, -2), gcdExt(-25, -10));
    }
}
