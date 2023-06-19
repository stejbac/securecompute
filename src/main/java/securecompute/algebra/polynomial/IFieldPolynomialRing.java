package securecompute.algebra.polynomial;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.EuclideanDomain;
import securecompute.algebra.Field;
import securecompute.algebra.module.VectorSpace;

public interface IFieldPolynomialRing<E> extends IPolynomialRing<E>, EuclideanDomain<Polynomial<E>>, VectorSpace<Polynomial<E>, E> {

    @Override
    Field<E> getBaseRing();

    @Override
    default int size(Polynomial<E> elt) {
        return elt.getDegree() + 1;
    }

    @Override
    default Polynomial<E> invSignum(Polynomial<E> elt) {
        E msc = elt.getDegree() >= 0 ? elt.getCoefficients().get(elt.getDegree()) : getBaseRing().zero();
        return polynomial(ImmutableList.of(getBaseRing().reciprocalOrZero(msc)));
    }

    @Override
    default DivModResult<Polynomial<E>> divMod(Polynomial<E> dividend, Polynomial<E> divisor) {
        if (divisor.getDegree() < 0) {
            throw new ArithmeticException("Division by zero");
        }

        int divisorDeg = divisor.getDegree();
        E divisorMsc = divisor.getCoefficients().get(divisorDeg);

        Polynomial<E> quotient = zero();
        for (int dividendDeg; (dividendDeg = dividend.getDegree()) >= divisorDeg; ) {
            E dividendMsc = dividend.getCoefficients().get(dividendDeg);
//            Polynomial<E> scalar = polynomial(ImmutableList.of(getBaseRing().quotient(dividendMsc, divisorMsc)));
            E scalar = getBaseRing().quotient(dividendMsc, divisorMsc);
            Polynomial<E> shiftedScalar = polynomial(ImmutableList.of(scalar)).shift(dividendDeg - divisorDeg);

            // FIXME: This could potentially result in a never-ending loop if there are floating-point rounding errors:
//            dividend = dividend.subtract(divisor.multiply(shiftedScalar));
            dividend = dividend.subtract(divisor.scalarMultiply(scalar).shift(dividendDeg - divisorDeg));
            // TODO: Consider replacing this O(n^2) construction of the quotient with a builder:
            quotient = quotient.add(shiftedScalar);
        }
        return DivModResult.of(quotient, dividend);
    }
}
