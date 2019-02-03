package securecompute.algebra;

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
    default E power(E elt, int exponent) {
        if (exponent < 0) {
            elt = reciprocal(elt);
            exponent = 0 - exponent;
        }
        return Ring.super.power(elt, exponent);
    }
}
