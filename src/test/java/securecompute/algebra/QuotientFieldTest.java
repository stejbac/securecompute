package securecompute.algebra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import securecompute.helper.WithDefaultField;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class QuotientFieldTest implements WithDefaultField<QuotientField<Integer>.Coset> {

    private final QuotientField<Integer> integersMod127 = new QuotientField<>(IntegerRing.INSTANCE, 127);

    @Override
    public QuotientField<Integer> getDefaultStructure() {
        return integersMod127;
    }

    private QuotientField<Integer>.Coset c(int n) {
        return n == 1 ? one() : getDefaultStructure().coset(n);
    }

    @Test
    void testAddSubtract() {
        System.out.println(c(-1));
        System.out.println(c(10).add(c(10)));
        System.out.println(c(90).add(c(90)));
        System.out.println(c(10).multiply(c(10)));
        System.out.println(c(10).multiply(c(90)));
        System.out.println(c(-1).multiply(c(-1)));
        System.out.println(c(10).recip());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 1000})
    void testReciprocals(int n) {
        Random rnd = new Random(5678 + n);
        List<QuotientField<Integer>.Coset> elements = rnd.ints(n, 1, 127)
                .mapToObj(this::c)
                .collect(ImmutableList.toImmutableList());

        List<?> expected = Lists.transform(elements, FieldElement::recip);
        Assertions.assertEquals(expected, Field.reciprocals(integersMod127, elements));

        if (n > 0) {
            List<QuotientField<Integer>.Coset> newElements = new ArrayList<>(elements);
            newElements.set(rnd.nextInt(n), zero());
            Assertions.assertThrows(ArithmeticException.class, () -> Field.reciprocals(integersMod127, newElements));
        }
    }
}
