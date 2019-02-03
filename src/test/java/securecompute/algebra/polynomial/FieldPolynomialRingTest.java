package securecompute.algebra.polynomial;

import org.junit.jupiter.api.Test;
import securecompute.algebra.BooleanField;
import securecompute.helper.WithDefaultFieldPolynomialRing;

import static org.junit.jupiter.api.Assertions.*;

class FieldPolynomialRingTest implements WithDefaultFieldPolynomialRing<Boolean> {

    private final FieldPolynomialRing<Boolean> ring = new FieldPolynomialRing<>(BooleanField.INSTANCE);

    @Override
    public IFieldPolynomialRing<Boolean> getDefaultStructure() {
        return ring;
    }

    @Test
    void testDivMod() {
        assertAll(
                () -> assertThrows(ArithmeticException.class, () -> zero().div(zero())),
                () -> assertThrows(ArithmeticException.class, () -> zero().mod(zero())),
                () -> assertEquals(zero(), zero().div(one())),
                () -> assertEquals(zero(), zero().mod(one())),
                () -> assertEquals(one(), one().div(one())),
                () -> assertEquals(zero(), one().mod(one())),
                () -> assertEquals(polynomial(true, true, true),
                        polynomial(true, false, false, true).div(polynomial(true, true))),
                () -> assertEquals(zero(),
                        polynomial(true, false, false, true).mod(polynomial(true, true)))
        );
    }
}
