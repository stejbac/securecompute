package securecompute.algebra;

import java.math.BigInteger;

public interface Field<E> extends Ring<E> {

    E reciprocalOrZero(E elt);

    default E reciprocal(E elt) {
        if (elt.equals(zero())) {
            throw new ArithmeticException("Division by zero");
        }
        return reciprocalOrZero(elt);
    }

    default E quotient(E dividend, E divisor) {
        return product(dividend, reciprocal(divisor));
    }

    @Override
    default E power(E elt, BigInteger exponent) {
        if (exponent.signum() < 0) {
            elt = reciprocal(elt);
            exponent = exponent.negate();
        }
        return Ring.super.power(elt, exponent);
    }
}
