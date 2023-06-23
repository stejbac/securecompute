package securecompute.algebra;

import securecompute.Nullable;

import java.math.BigInteger;

public interface PlusMinus<E> {
    AbelianGroup<E> getAbelianGroup();

    boolean isHalfZero();

    @Nullable
    E getWitness();

    default PlusMinus<E> multiply(long k) {
        return getAbelianGroup().product(this, k);
    }

    default PlusMinus<E> multiply(BigInteger k) {
        return getAbelianGroup().product(this, k);
    }

    static <E> PlusMinus<E> ofMissing(AbelianGroup<E> group) {
        return new WitnessedPlusMinus<>(null, group);
    }
}
