package securecompute.algebra.module;

import securecompute.algebra.AbelianGroup;
import securecompute.algebra.AbelianGroupElement;

public interface ModuleElement<V, E> extends AbelianGroupElement<V> {

    @Override
    default AbelianGroup<V> getAbelianGroup() {
        return getModule();
    }

    Module<V, E> getModule();

    default V scalarMultiply(E scalar) {
        return getModule().scalarProduct(cast(), scalar);
    }
}
