package securecompute.algebra;

import com.google.common.collect.Streams;

public interface AbelianGroup<E> {

    E zero();

    E sum(E left, E right);

    default E sum(Iterable<E> elements) {
        return Streams.stream(elements).reduce(zero(), this::sum);
    }

    E negative(E elt);

    default E difference(E left, E right) {
        return sum(left, negative(right));
    }
}
