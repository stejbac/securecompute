package securecompute.algebra;

import securecompute.Nullable;

import java.util.Objects;

final class WitnessedPlusMinus<E> implements PlusMinus<E> {
    private final AbelianGroup<E> group;
    @Nullable
    private final E witness;
    private final boolean isHalfZero;

    WitnessedPlusMinus(@Nullable E witness, AbelianGroup<E> group) {
        this.group = group;
        this.witness = witness;
        isHalfZero = witness != null && witness.equals(group.negative(witness));
    }

    @Override
    public boolean isHalfZero() {
        return isHalfZero;
    }

    @Override
    @Nullable
    public E getWitness() {
        return witness;
    }

    @Override
    public AbelianGroup<E> getAbelianGroup() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WitnessedPlusMinus)) {
            return o != null && o.equals(this);
        }
        WitnessedPlusMinus<?> other = (WitnessedPlusMinus<?>) o;
        return group.equals(other.group) && (Objects.equals(witness, other.witness) || group.negative(witness).equals(other.witness));
    }

    @Override
    public int hashCode() {
        return 31 * group.hashCode() + (witness != null ? witness.hashCode() + group.negative(witness).hashCode() : 0);
    }

    @Override
    public String toString() {
        return witness != null ? (isHalfZero ? "" : "\u00b1") + witness : "<missing>";
    }
}
