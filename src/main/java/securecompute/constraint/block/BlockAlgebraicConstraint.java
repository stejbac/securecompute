package securecompute.constraint.block;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.algebra.module.block.BlockFiniteVectorSpace;
import securecompute.constraint.AlgebraicConstraint;

import java.util.List;
import java.util.function.Function;

public class BlockAlgebraicConstraint<V, E> extends BlockConstraint<V> implements AlgebraicConstraint<List<V>, E> {

    private final FiniteVectorSpace<List<V>, E> symbolSpace;
    private final int degree, redundancy;

    public BlockAlgebraicConstraint(AlgebraicConstraint<V, E> columnConstraint, int rowLength) {
        super(columnConstraint, rowLength);
        symbolSpace = new BlockFiniteVectorSpace<>(columnConstraint.symbolSpace(), rowLength);
        degree = columnConstraint.degree();
        redundancy = columnConstraint.redundancy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AlgebraicConstraint<V, E> columnConstraint() {
        return (AlgebraicConstraint<V, E>) super.columnConstraint();
    }

    @Override
    public FiniteVectorSpace<List<V>, E> symbolSpace() {
        return symbolSpace;
    }

    @Override
    public int degree() {
        return degree;
    }

    @Override
    public int redundancy() {
        return redundancy;
    }

    @Override
    public List<List<V>> parityCheck(List<List<V>> vector) {
        return mapColumns(vector, columnConstraint()::parityCheck);
    }

    final List<List<V>> mapColumns(List<List<V>> rows, Function<List<V>, List<V>> columnMapping) {
        List<List<V>> mappedColumns = streamLayers(rows, rowLength())
                .map(columnMapping)
                .collect(ImmutableList.toImmutableList());

        int size = mappedColumns.isEmpty() ? 0 : mappedColumns.get(0).size();
        return transpose(mappedColumns, size);
    }
}
