package securecompute.algebra.module;

import securecompute.algebra.AbelianGroup;
import securecompute.algebra.Ring;

public interface Module<V, E> extends AbelianGroup<V> {

    Ring<E> getBaseRing();

    V scalarProduct(V left, E right);
}
