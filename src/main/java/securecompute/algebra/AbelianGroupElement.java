package securecompute.algebra;

import java.math.BigInteger;

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

    default E multiply(long k) {
        return getAbelianGroup().product(cast(), k);
    }

    default E multiply(BigInteger k) {
        return getAbelianGroup().product(cast(), k);
    }

    default PlusMinus<E> plusMinus() {
        return getAbelianGroup().plusMinus(cast());
    }
}
