package securecompute.algebra.module;

import java.util.List;

public interface FiniteVectorSpace<V, E> extends VectorSpace<V, E> {

    int getDimension();

    V pack(Iterable<E> elements);

    List<E> unpack(V vector);
}
