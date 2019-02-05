package securecompute.constraint.block;

import securecompute.constraint.LinearCode;

import java.util.List;

public class BlockLinearCode<V, E> extends BlockAlgebraicConstraint<V, E> implements LinearCode<List<V>, E> {

    private final int dimension;
    private final int distance;
    private final int redundancy;

    public BlockLinearCode(LinearCode<V, E> columnCode, int rowLength) {
        super(columnCode, rowLength);
        dimension = columnCode.dimension();
        distance = columnCode.distance();
        redundancy = columnCode.redundancy();
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
    public List<List<V>> encode(List<List<V>> message) {
        return mapColumns(message, columnConstraint()::encode);
    }

    @Override
    public List<List<V>> decode(List<List<V>> codeword) {
        return mapColumns(codeword, columnConstraint()::decode);
    }
}
