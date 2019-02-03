package securecompute.algebra;

import org.junit.jupiter.api.Test;
import securecompute.helper.WithDefaultField;

class QuotientFieldTest implements WithDefaultField<QuotientField<Integer>.Coset> {

    private final QuotientField<Integer> integersMod127 = new QuotientField<>(IntegerRing.INSTANCE, 127);

    @Override
    public QuotientField<Integer> getDefaultStructure() {
        return integersMod127;
    }

    private QuotientField<Integer>.Coset c(int n) {
        return getDefaultStructure().coset(n);
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
}
