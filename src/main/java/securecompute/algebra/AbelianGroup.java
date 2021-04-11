package securecompute.algebra;

import com.google.common.collect.Streams;

import java.math.BigInteger;
import java.util.stream.Stream;

public interface AbelianGroup<E> {

    E zero();

    E sum(E left, E right);

    default E sum(Iterable<E> elements) {
        return sum(Streams.stream(elements));
    }

    default E sum(Stream<E> elements) {
        return elements.reduce(zero(), this::sum);
    }

    E negative(E elt);

    default E difference(E left, E right) {
        return sum(left, negative(right));
    }

    default E product(E elt, long k) {
        return product(elt, BigInteger.valueOf(k));
    }

    default E product(E elt, BigInteger k) {
        if (k.signum() < 0) {
            return product(elt, k.negate());
        }
        E acc = null;
        for (int i = 0, len = k.bitLength(); i < len; i++) {
            if (k.testBit(i)) {
                acc = acc != null ? sum(acc, elt) : elt;
            }
            if (i < len - 1) {
                elt = sum(elt, elt);
            }
        }
        return acc != null ? acc : zero();
    }
}
