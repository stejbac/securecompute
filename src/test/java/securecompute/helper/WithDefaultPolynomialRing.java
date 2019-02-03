package securecompute.helper;

import securecompute.algebra.Ring;
import securecompute.algebra.polynomial.IPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;

import java.util.List;

public interface WithDefaultPolynomialRing<E> extends WithDefaultRing<Polynomial<E>>, IPolynomialRing<E> {

    @Override
    IPolynomialRing<E> getDefaultStructure();

    @Override
    default Ring<E> getBaseRing() {
        return getDefaultStructure().getBaseRing();
    }

    @Override
    default Polynomial<E> scalarProduct(Polynomial<E> left, E right) {
        return getDefaultStructure().scalarProduct(left, right);
    }

    @Override
    default Polynomial<E> polynomial(List<E> coefficients) {
        return getDefaultStructure().polynomial(coefficients);
    }

    @Override
    @SuppressWarnings("unchecked")
    default Polynomial<E> polynomial(E... coefficients) {
        return getDefaultStructure().polynomial(coefficients);
    }

    @Override
    default Polynomial<E> shift(Polynomial<E> elt, int n) {
        return getDefaultStructure().shift(elt, n);
    }
}
