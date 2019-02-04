package securecompute.constraint;

import securecompute.algebra.FiniteField;
import securecompute.algebra.module.FiniteVectorSpace;

import java.util.List;

public interface AlgebraicConstraint<V, E> extends Constraint<V> {

    FiniteVectorSpace<V, E> symbolSpace();

    int degree();

    /**
     * @return the size of the parity check vectors (syndromes)
     */
    int redundancy();

    List<V> parityCheck(List<V> vector);

    default FiniteField<E> field() {
        return (FiniteField<E>) symbolSpace().getBaseRing();
    }

    @Override
    default boolean isValid(List<V> vector) {
        return !vector.contains(null) && parityCheck(vector).stream().allMatch(symbolSpace().zero()::equals);
    }
}
