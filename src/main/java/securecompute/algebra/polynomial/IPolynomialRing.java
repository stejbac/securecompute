package securecompute.algebra.polynomial;

import securecompute.algebra.Ring;
import securecompute.algebra.module.Module;

import java.util.List;

public interface IPolynomialRing<E> extends Ring<Polynomial<E>>, Module<Polynomial<E>, E> {

    Polynomial<E> polynomial(List<E> coefficients);

    @SuppressWarnings("unchecked")
    Polynomial<E> polynomial(E... coefficients);

    Polynomial<E> shift(Polynomial<E> elt, int n);
}
