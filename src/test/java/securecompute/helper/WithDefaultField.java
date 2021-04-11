package securecompute.helper;

import securecompute.algebra.Field;

import java.math.BigInteger;

public interface WithDefaultField<E> extends WithDefaultRing<E>, Field<E> {

    @Override
    Field<E> getDefaultStructure();

    @Override
    default E reciprocalOrZero(E elt) {
        return getDefaultStructure().reciprocalOrZero(elt);
    }

    @Override
    default E reciprocal(E elt) {
        return getDefaultStructure().reciprocal(elt);
    }

    @Override
    default E quotient(E dividend, E divisor) {
        return getDefaultStructure().quotient(dividend, divisor);
    }

    @Override
    default E power(E elt, BigInteger exponent) {
        return getDefaultStructure().power(elt, exponent);
    }
}
