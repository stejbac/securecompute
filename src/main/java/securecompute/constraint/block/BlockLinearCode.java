package securecompute.constraint.block;

import securecompute.constraint.LinearCode;

import java.util.List;
import java.util.SortedSet;
import java.util.function.Function;

public class BlockLinearCode<V, E> extends BlockAlgebraicConstraint<V, E> implements LinearCode<List<V>, E> {

    private final int dimension;
    private final int distance;
    private final int redundancy;
    private final int codistance;

    public BlockLinearCode(LinearCode<V, E> columnCode, int rowLength) {
        super(columnCode, rowLength);
        dimension = columnCode.dimension();
        distance = columnCode.distance();
        redundancy = columnCode.redundancy();
        codistance = columnCode.codistance();
    }

    @Override
    public LinearCode<V, E> columnConstraint() {
        return (LinearCode<V, E>) super.columnConstraint();
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public int redundancy() {
        return redundancy;
    }

    @Override
    public int codistance() {
        return codistance;
    }

    @Override
    public List<List<V>> encode(List<List<V>> message) {
        return mapColumns(message, columnConstraint()::encode);
    }

    @Override
    public List<List<V>> decode(List<List<V>> codeword) {
        return mapColumns(codeword, columnConstraint()::decode);
    }

    @Override
    public boolean isSystematic() {
        return columnConstraint().isSystematic();
    }

    @Override
    public Function<List<List<V>>, List<List<V>>> interpolationFn(SortedSet<Integer> knownSymbolIndices) {
        Function<List<V>, List<V>> columnMapping = columnConstraint().interpolationFn(knownSymbolIndices);
        return rows -> mapColumns(rows, columnMapping);
    }
}
