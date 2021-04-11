package securecompute.helper;

import securecompute.algebra.EuclideanDomain;

public interface WithDefaultEuclideanDomain<E> extends WithDefaultRing<E>, EuclideanDomain<E> {

    @Override
    EuclideanDomain<E> getDefaultStructure();

    @Override
    default int size(E elt) {
        return getDefaultStructure().size(elt);
    }

    @Override
    default E invSignum(E elt) {
        return getDefaultStructure().invSignum(elt);
    }

    @Override
    default E div(E dividend, E divisor) {
        return getDefaultStructure().div(dividend, divisor);
    }

    @Override
    default E mod(E dividend, E divisor) {
        return getDefaultStructure().mod(dividend, divisor);
    }

    @Override
    default DivModResult<E> divMod(E dividend, E divisor) {
        return getDefaultStructure().divMod(dividend, divisor);
    }

    @Override
    default E gcd(E left, E right) {
        return getDefaultStructure().gcd(left, right);
    }

    @Override
    default E lcm(E left, E right) {
        return getDefaultStructure().lcm(left, right);
    }

    @Override
    default GcdExtResult<E> gcdExt(E left, E right) {
        return getDefaultStructure().gcdExt(left, right);
    }
}
