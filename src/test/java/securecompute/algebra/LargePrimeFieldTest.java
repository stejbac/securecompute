package securecompute.algebra;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class LargePrimeFieldTest {
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 7, 11, 13, 127, 193, 257, 3329})
    void testSqrt(int p) {
        LargePrimeField zP = new LargePrimeField(BigInteger.valueOf(p));

        long squareCount = zP.getElements().map(zP::sqrt).map(PlusMinus::getWitness).filter(Objects::nonNull).count();
        long invSquareCount = zP.getElements().map(zP::invSqrt).map(PlusMinus::getWitness).filter(Objects::nonNull).count();
        assertEquals(p / 2 + 1, squareCount);
        assertEquals(p / 2, invSquareCount);

        assertEquals(zP.zero().plusMinus(), zP.sqrt(zP.zero()));
        assertEquals(PlusMinus.ofMissing(zP), zP.invSqrt(zP.zero()));

        assertAll(zP.getElements().map(x -> () -> {
            LargePrimeField.Coset y = zP.invSqrt(x).getWitness(), z = zP.sqrt(x).getWitness();
            if (y != null) {
                assertEquals(zP.one(), y.multiply(y).multiply(x));
                assertEquals(zP.one().plusMinus(), zP.product(zP.sqrt(x), zP.invSqrt(x)));
                if (p > 2) {
                    assertFalse(y.getWitness().testBit(0));
                    assertFalse(z.getWitness().testBit(0));
                }
            }
        }));
    }
}
