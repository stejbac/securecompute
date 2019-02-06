package securecompute.constraint.block;

import securecompute.constraint.MultiplicativeLinearCode;

import java.util.List;

public class BlockMultiplicativeLinearCode<V, E> extends BlockLinearCode<V, E> implements MultiplicativeLinearCode<List<V>, E> {

    public BlockMultiplicativeLinearCode(MultiplicativeLinearCode<V, E> columnCode, int rowLength) {
        super(columnCode, rowLength);
    }

    @Override
    public MultiplicativeLinearCode<V, E> columnConstraint() {
        return (MultiplicativeLinearCode<V, E>) super.columnConstraint();
    }

    @Override
    public MultiplicativeLinearCode<List<V>, E> pow(int exponent) {
        return new BlockMultiplicativeLinearCode<>(columnConstraint().pow(exponent), rowLength());
    }
}
