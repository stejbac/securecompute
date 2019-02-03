package securecompute.algebra;

public interface AbelianGroupElement<E> {

    AbelianGroup<E> getAbelianGroup();

    E cast();

    default E add(E other) {
        return getAbelianGroup().sum(cast(), other);
    }

    default E subtract(E other) {
        return getAbelianGroup().difference(cast(), other);
    }

    default E negate() {
        return getAbelianGroup().negative(cast());
    }
}
