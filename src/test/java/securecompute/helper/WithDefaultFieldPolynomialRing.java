package securecompute.helper;

import securecompute.algebra.Field;
import securecompute.algebra.polynomial.IFieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;

public interface WithDefaultFieldPolynomialRing<E> extends WithDefaultPolynomialRing<E>, WithDefaultEuclideanDomain<Polynomial<E>>, IFieldPolynomialRing<E> {

    @Override
    IFieldPolynomialRing<E> getDefaultStructure();

    @Override
    default Field<E> getBaseRing() {
        return getDefaultStructure().getBaseRing();
    }

    @Override
    default int size(Polynomial<E> elt) {
        return getDefaultStructure().size(elt);
    }

    @Override
    default DivModResult<Polynomial<E>> divMod(Polynomial<E> dividend, Polynomial<E> divisor) {
        return getDefaultStructure().divMod(dividend, divisor);
    }
}
