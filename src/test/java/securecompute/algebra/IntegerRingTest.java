package securecompute.algebra;

import org.junit.jupiter.api.Test;
import securecompute.helper.WithDefaultEuclideanDomain;

class IntegerRingTest implements WithDefaultEuclideanDomain<Integer> {

    @Override
    public EuclideanDomain<Integer> getDefaultStructure() {
        return IntegerRing.INSTANCE;
    }

    @Test
    void testDivMod() {
        System.out.println(divMod(24, 10));
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
    }
}
