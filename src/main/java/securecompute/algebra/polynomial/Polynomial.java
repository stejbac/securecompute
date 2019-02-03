package securecompute.algebra.polynomial;

import com.google.common.collect.Lists;
import securecompute.algebra.AbelianGroup;
import securecompute.algebra.EuclideanDomainElement;
import securecompute.algebra.Field;
import securecompute.algebra.module.ModuleElement;

import java.util.List;

public interface Polynomial<E> extends EuclideanDomainElement<Polynomial<E>>, ModuleElement<Polynomial<E>, E> {

    @Override
    default AbelianGroup<Polynomial<E>> getAbelianGroup() {
        return getRing();
    }

    @Override
    default IPolynomialRing<E> getRing() {
        return getModule();
    }

    @Override
    IPolynomialRing<E> getModule();

    @Override
    default IFieldPolynomialRing<E> getEuclideanDomain() {
        IPolynomialRing<E> ring = getRing();
        if (!(ring.getBaseRing() instanceof Field)) {
            throw new UnsupportedOperationException("Not a euclidean domain element: base ring is not a field");
        }
        return (IFieldPolynomialRing<E>) ring;
    }

    List<E> getCoefficients();

    // TODO: Should degree(0), order(0) := Integer.MIN_VALUE to allow generalisation to Laurent polynomials? (Note -Integer.MIN_VALUE == Integer.MIN_VALUE.)

    default int getDegree() {
        return getCoefficients().size() - 1;
    }

    default int getOrder() {
        return Lists.transform(getCoefficients(), getRing().getBaseRing().zero()::equals).indexOf(false);
    }

    default Polynomial<E> shift(int n) {
        return getRing().shift(cast(), n);
    }
}
