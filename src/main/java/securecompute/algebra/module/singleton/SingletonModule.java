package securecompute.algebra.module.singleton;

import securecompute.algebra.Ring;
import securecompute.algebra.module.Module;

import java.util.Objects;

public class SingletonModule<E> implements Module<E, E> {

    private final Ring<E> baseRing;

    public SingletonModule(Ring<E> baseRing) {
        this.baseRing = Objects.requireNonNull(baseRing);
    }

    @Override
    public Ring<E> getBaseRing() {
        return baseRing;
    }

    @Override
    public E scalarProduct(E left, E right) {
        return baseRing.product(left, right);
    }

    @Override
    public E zero() {
        return baseRing.zero();
    }

    @Override
    public E sum(E left, E right) {
        return baseRing.sum(left, right);
    }

    @Override
    public E sum(Iterable<E> elements) {
        return baseRing.sum(elements);
    }

    @Override
    public E negative(E elt) {
        return baseRing.negative(elt);
    }

    @Override
    public E difference(E left, E right) {
        return baseRing.difference(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj != null && getClass() == obj.getClass() && baseRing.equals(((SingletonModule<?>) obj).baseRing);
    }

    @Override
    public int hashCode() {
        return baseRing.hashCode();
    }
}
