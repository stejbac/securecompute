package securecompute.algebra.polynomial;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import securecompute.algebra.BooleanField;
import securecompute.algebra.IntegerRing;
import securecompute.algebra.QuotientField;
import securecompute.helper.WithDefaultFieldPolynomialRing;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FieldPolynomialRingTest {
    @Nested
    class BooleanPolynomialsTest implements WithDefaultFieldPolynomialRing<Boolean> {
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

    @Nested
    class Gf5PolynomialsTest implements WithDefaultFieldPolynomialRing<QuotientField<Integer>.Coset> {
        private final QuotientField<Integer> integersMod5 = new QuotientField<>(IntegerRing.INSTANCE, 5);
        private final FieldPolynomialRing<QuotientField<Integer>.Coset> ring = new FieldPolynomialRing<>(integersMod5);

        @Override
        public IFieldPolynomialRing<QuotientField<Integer>.Coset> getDefaultStructure() {
            return ring;
        }

        @Test
        void testGcdExt() {
            QuotientField<Polynomial<QuotientField<Integer>.Coset>> field = new QuotientField<>(ring, polynomial(2, 1));
            GcdExtResult<?> expectedGcdExtResult = GcdExtResult.of(
                    zero(), polynomial(3), one(), polynomial(2, 1), polynomial(2)
            );
            assertAll(
                    () -> assertEquals(one(), gcd(polynomial(2, 1), polynomial(2))),
                    () -> assertEquals(expectedGcdExtResult, gcdExt(polynomial(2, 1), polynomial(2))),
                    () -> assertDoesNotThrow(() -> field.reciprocalOrZero(field.coset(polynomial(2))))
            );
        }

        private Polynomial<QuotientField<Integer>.Coset> polynomial(int... coefficients) {
            return Arrays.stream(coefficients)
                    .mapToObj(integersMod5::coset)
                    .collect(Collectors.collectingAndThen(ImmutableList.toImmutableList(), this::polynomial));
        }
    }
}
