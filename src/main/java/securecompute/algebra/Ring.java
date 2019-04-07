package securecompute.algebra;

import com.google.common.collect.Streams;

import java.util.stream.Stream;

public interface Ring<E> extends AbelianGroup<E> {

    E fromInt(int n);

    @Override
    default E zero() {
        return fromInt(0);
    }

    default E one() {
        return fromInt(1);
    }

    E product(E left, E right);

    default E product(Iterable<E> elements) {
        return product(Streams.stream(elements));
    }

    default E product(Stream<E> elements) {
        return elements.reduce(one(), this::product);
    }

    default E power(E elt, int exponent) {
        if (exponent < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        E acc = null;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                acc = acc != null ? product(acc, elt) : elt;
            }
            if ((exponent /= 2) > 0) {
                elt = product(elt, elt);
            }
        }
        return acc != null ? acc : one();
    }
}
