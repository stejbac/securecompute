package securecompute.algebra;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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

    default E quotient(E dividend, long divisor) {
        return product(dividend, reciprocal(fromLong(divisor)));
    }

    default E quotient(E dividend, BigInteger divisor) {
        return product(dividend, reciprocal(fromBigInteger(divisor)));
    }

    @Override
    default E power(E elt, BigInteger exponent) {
        if (exponent.signum() < 0) {
            elt = reciprocal(elt);
            exponent = exponent.negate();
        }
        return Ring.super.power(elt, exponent);
    }

    // uses Montgomery's trick to perform a batch inversion from a single inversion op
    static <E> List<E> reciprocals(Field<E> field, List<E> elements) {
        List<E> result = new ArrayList<>(elements.size());
        E one = field.one(), prod = one;
        for (E elt : elements) {
            result.add(prod);
            prod = elt == one ? prod : field.product(prod, elt);
        }
        prod = field.reciprocal(prod);
        for (int i = elements.size(); i-- > 0; ) {
            E elt = elements.get(i);
            result.set(i, elt == one ? one : field.product(result.get(i), prod));
            if (i > 0) {
                prod = elt == one ? prod : field.product(prod, elt);
            }
        }
        return result;
    }
}
