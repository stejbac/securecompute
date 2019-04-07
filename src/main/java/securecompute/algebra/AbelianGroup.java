package securecompute.algebra;

import com.google.common.collect.Streams;

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
}
