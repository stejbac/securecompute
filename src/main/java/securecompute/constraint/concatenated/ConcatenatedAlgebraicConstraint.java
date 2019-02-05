package securecompute.constraint.concatenated;

import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.algebra.module.block.BlockFiniteVectorSpace;
import securecompute.constraint.AlgebraicConstraint;

import java.util.List;

public abstract class ConcatenatedAlgebraicConstraint<V, E> extends ConcatenatedConstraint<V> implements AlgebraicConstraint<V, E> {

    private final FiniteVectorSpace<V, E> symbolSpace;
    private final int degree;

    public ConcatenatedAlgebraicConstraint(AlgebraicConstraint<V, E> rowConstraint, AlgebraicConstraint<List<V>, E> outerConstraint) {
        super(rowConstraint, outerConstraint);
        symbolSpace = rowConstraint.symbolSpace();
        FiniteVectorSpace<List<V>, E> outerSymbolSpace = outerConstraint.symbolSpace();
        if (!(outerSymbolSpace instanceof BlockFiniteVectorSpace)) {
            throw new IllegalArgumentException("Outer constraint symbol space must be a BlockFiniteVectorSpace");
        }
        if (((BlockFiniteVectorSpace<V, E>) outerSymbolSpace).getBlockSize() != rowConstraint.length()) {
            throw new IllegalArgumentException("Outer constraint block size != row length");
        }
        if (!symbolSpace.equals(((BlockFiniteVectorSpace<V, E>) outerSymbolSpace).getBaseModule())) {
            throw new IllegalArgumentException("Unequal row and column symbol spaces");
        }
        degree = Math.max(rowConstraint.degree(), outerConstraint.degree());
    }

    @Override
    public AlgebraicConstraint<V, E> rowConstraint() {
        return (AlgebraicConstraint<V, E>) super.rowConstraint();
    }

    @Override
    public AlgebraicConstraint<List<V>, E> outerConstraint() {
        return (AlgebraicConstraint<List<V>, E>) super.outerConstraint();
    }

    @Override
    public FiniteVectorSpace<V, E> symbolSpace() {
        return symbolSpace;
    }

    @Override
    public int degree() {
        return degree;
    }
}
