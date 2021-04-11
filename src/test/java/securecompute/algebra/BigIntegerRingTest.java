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
        System.out.println(divMod(24, 10));
        System.out.println(divMod(24, -10));
        System.out.println(divMod(-24, 10));
        System.out.println(divMod(-24, -10));
        System.out.println(divMod(24, 12));
        System.out.println(divMod(24, -12));
        System.out.println(divMod(-24, 12));
        System.out.println(divMod(-24, -12));
    }

    @Test
    void testGcdExt() {
        System.out.println(gcdExt(0, 0));
        System.out.println(gcdExt(0, 1));
        System.out.println(gcdExt(1, 0));
        System.out.println(gcdExt(1, 1));
        System.out.println(gcdExt(1, 2));
        System.out.println(gcdExt(2, 1));
        System.out.println(gcdExt(10, 24));
        System.out.println(gcdExt(24, 10));
        System.out.println(gcdExt(10, 25));
        System.out.println(gcdExt(25, 10));
        System.out.println();
        System.out.println(gcdExt(-1, 0));
        System.out.println(gcdExt(-1, 1));
        System.out.println(gcdExt(-1, 2));
        System.out.println(gcdExt(-2, 1));
        System.out.println(gcdExt(-10, 24));
        System.out.println(gcdExt(-24, 10));
        System.out.println(gcdExt(-10, 25));
        System.out.println(gcdExt(-25, 10));
        System.out.println();
        System.out.println(gcdExt(0, -1));
        System.out.println(gcdExt(1, -1));
        System.out.println(gcdExt(1, -2));
        System.out.println(gcdExt(2, -1));
        System.out.println(gcdExt(10, -24));
        System.out.println(gcdExt(24, -10));
        System.out.println(gcdExt(10, -25));
        System.out.println(gcdExt(25, -10));
        System.out.println();
        System.out.println(gcdExt(-1, -1));
        System.out.println(gcdExt(-1, -2));
        System.out.println(gcdExt(-2, -1));
        System.out.println(gcdExt(-10, -24));
        System.out.println(gcdExt(-24, -10));
        System.out.println(gcdExt(-10, -25));
        System.out.println(gcdExt(-25, -10));
        System.out.println();
    }

    private DivModResult<BigInteger> divMod(long dividend, long divisor) {
        DivModResult<BigInteger> result = divMod(valueOf(dividend), valueOf(divisor));
        assertEquals(result.getQuotient(), div(valueOf(dividend), valueOf(divisor)));
        assertEquals(result.getRemainder(), mod(valueOf(dividend), valueOf(divisor)));
        return result;
    }

    private GcdExtResult<BigInteger> gcdExt(long left, long right) {
        return gcdExt(valueOf(left), valueOf(right));
    }
}
