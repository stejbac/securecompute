package securecompute.algebra;

import org.junit.jupiter.api.Test;
import securecompute.helper.WithDefaultEuclideanDomain;

import java.math.BigInteger;

import static java.math.BigInteger.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BigIntegerRingTest implements WithDefaultEuclideanDomain<BigInteger> {
    @Override
    public EuclideanDomain<BigInteger> getDefaultStructure() {
        return BigIntegerRing.INSTANCE;
    }

    @Test
    void testDivMod() {
        assertEquals(divModResultOf(2, 4), divMod(24, 10));
        assertEquals(divModResultOf(-3, -6), divMod(24, -10));
        assertEquals(divModResultOf(-3, 6), divMod(-24, 10));
        assertEquals(divModResultOf(2, -4), divMod(-24, -10));
        assertEquals(divModResultOf(2, 0), divMod(24, 12));
        assertEquals(divModResultOf(-2, 0), divMod(24, -12));
        assertEquals(divModResultOf(-2, 0), divMod(-24, 12));
        assertEquals(divModResultOf(2, 0), divMod(-24, -12));
    }

    @Test
    void testGcdExt() {
        assertEquals(gcdExtResultOf(0, 0, 0, 0, 0), gcdExt(0, 0));
        assertEquals(gcdExtResultOf(0, 1, 1, 0, 1), gcdExt(0, 1));
        assertEquals(gcdExtResultOf(1, 0, 1, 1, 0), gcdExt(1, 0));
        assertEquals(gcdExtResultOf(0, 1, 1, 1, 1), gcdExt(1, 1));
        assertEquals(gcdExtResultOf(1, 0, 1, 1, 2), gcdExt(1, 2));
        assertEquals(gcdExtResultOf(0, 1, 1, 2, 1), gcdExt(2, 1));
        assertEquals(gcdExtResultOf(5, -2, 2, 5, 12), gcdExt(10, 24));
        assertEquals(gcdExtResultOf(3, -7, 2, 12, 5), gcdExt(24, 10));
        assertEquals(gcdExtResultOf(3, -1, 5, 2, 5), gcdExt(10, 25));
        assertEquals(gcdExtResultOf(1, -2, 5, 5, 2), gcdExt(25, 10));

        assertEquals(gcdExtResultOf(-1, 0, 1, -1, 0), gcdExt(-1, 0));
        assertEquals(gcdExtResultOf(0, 1, 1, -1, 1), gcdExt(-1, 1));
        assertEquals(gcdExtResultOf(1, 1, 1, -1, 2), gcdExt(-1, 2));
        assertEquals(gcdExtResultOf(0, 1, 1, -2, 1), gcdExt(-2, 1));
        assertEquals(gcdExtResultOf(7, 3, 2, -5, 12), gcdExt(-10, 24));
        assertEquals(gcdExtResultOf(2, 5, 2, -12, 5), gcdExt(-24, 10));
        assertEquals(gcdExtResultOf(2, 1, 5, -2, 5), gcdExt(-10, 25));
        assertEquals(gcdExtResultOf(1, 3, 5, -5, 2), gcdExt(-25, 10));

        assertEquals(gcdExtResultOf(0, -1, 1, 0, -1), gcdExt(0, -1));
        assertEquals(gcdExtResultOf(0, -1, 1, 1, -1), gcdExt(1, -1));
        assertEquals(gcdExtResultOf(-1, -1, 1, 1, -2), gcdExt(1, -2));
        assertEquals(gcdExtResultOf(0, -1, 1, 2, -1), gcdExt(2, -1));
        assertEquals(gcdExtResultOf(-7, -3, 2, 5, -12), gcdExt(10, -24));
        assertEquals(gcdExtResultOf(-2, -5, 2, 12, -5), gcdExt(24, -10));
        assertEquals(gcdExtResultOf(-2, -1, 5, 2, -5), gcdExt(10, -25));
        assertEquals(gcdExtResultOf(-1, -3, 5, 5, -2), gcdExt(25, -10));

        assertEquals(gcdExtResultOf(0, -1, 1, -1, -1), gcdExt(-1, -1));
        assertEquals(gcdExtResultOf(-1, 0, 1, -1, -2), gcdExt(-1, -2));
        assertEquals(gcdExtResultOf(0, -1, 1, -2, -1), gcdExt(-2, -1));
        assertEquals(gcdExtResultOf(-5, 2, 2, -5, -12), gcdExt(-10, -24));
        assertEquals(gcdExtResultOf(-3, 7, 2, -12, -5), gcdExt(-24, -10));
        assertEquals(gcdExtResultOf(-3, 1, 5, -2, -5), gcdExt(-10, -25));
        assertEquals(gcdExtResultOf(-1, 2, 5, -5, -2), gcdExt(-25, -10));
    }

    private DivModResult<BigInteger> divModResultOf(long quotient, long remainder) {
        return DivModResult.of(valueOf(quotient), valueOf(remainder));
    }

    private DivModResult<BigInteger> divMod(long dividend, long divisor) {
        DivModResult<BigInteger> result = divMod(valueOf(dividend), valueOf(divisor));
        assertEquals(result.getQuotient(), div(valueOf(dividend), valueOf(divisor)));
        assertEquals(result.getRemainder(), mod(valueOf(dividend), valueOf(divisor)));
        return result;
    }

    private GcdExtResult<BigInteger> gcdExtResultOf(long x, long y, long gcd, long leftDivGcd, long rightDivGcd) {
        return GcdExtResult.of(valueOf(x), valueOf(y), valueOf(gcd), valueOf(leftDivGcd), valueOf(rightDivGcd));
    }

    private GcdExtResult<BigInteger> gcdExt(long left, long right) {
        GcdExtResult<BigInteger> result = gcdExt(valueOf(left), valueOf(right));
        assertEquals(result.getGcd(), gcd(valueOf(left), valueOf(right)));
        return result;
    }
}
