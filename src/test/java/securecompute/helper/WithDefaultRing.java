package securecompute.helper;

import securecompute.algebra.Ring;

import java.math.BigInteger;

public interface WithDefaultRing<E> extends Ring<E> {

    Ring<E> getDefaultStructure();

    @Override
    default E fromBigInteger(BigInteger n) {
        return getDefaultStructure().fromBigInteger(n);
    }

    @Override
    default E sum(E left, E right) {
        return getDefaultStructure().sum(left, right);
    }

    @Override
    default E product(E left, E right) {
        return getDefaultStructure().product(left, right);
    }

    @Override
    default E negative(E elt) {
        return getDefaultStructure().negative(elt);
    }

    @Override
    default E zero() {
        return getDefaultStructure().zero();
    }

    @Override
    default E one() {
        return getDefaultStructure().one();
    }

    @Override
    default E difference(E left, E right) {
        return getDefaultStructure().difference(left, right);
    }

    @Override
    default E power(E elt, BigInteger exponent) {
        return getDefaultStructure().power(elt, exponent);
    }
}
