package securecompute.algebra.module;

import securecompute.algebra.Field;

public interface VectorSpace<V, E> extends Module<V, E> {

    @Override
    Field<E> getBaseRing();
}
