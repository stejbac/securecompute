package securecompute.constraint.grid;

import securecompute.constraint.Constraint;
import securecompute.constraint.block.BlockConstraint;
import securecompute.constraint.concatenated.ConcatenatedConstraint;

public class GridConstraint<V> extends ConcatenatedConstraint<V> {

    private final Constraint<V> columnConstraint;

    public GridConstraint(Constraint<V> rowConstraint, Constraint<V> columnConstraint) {
        super(rowConstraint, new BlockConstraint<>(columnConstraint, rowConstraint.length()));
        this.columnConstraint = columnConstraint;
    }

    public Constraint<V> columnConstraint() {
        return columnConstraint;
    }

    @Override
    public BlockConstraint<V> outerConstraint() {
        return (BlockConstraint<V>) super.outerConstraint();
    }
}
