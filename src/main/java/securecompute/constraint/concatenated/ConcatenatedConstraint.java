package securecompute.constraint.concatenated;

import com.google.common.collect.Lists;
import securecompute.constraint.Constraint;

import java.util.List;

public class ConcatenatedConstraint<V> implements Constraint<V> {

    private final Constraint<V> rowConstraint;
    private final Constraint<List<V>> outerConstraint;
    private final int length;

    public ConcatenatedConstraint(Constraint<V> rowConstraint, Constraint<List<V>> outerConstraint) {
        this.rowConstraint = rowConstraint;
        this.outerConstraint = outerConstraint;
        length = rowConstraint.length() * outerConstraint.length();
    }

    public Constraint<V> rowConstraint() {
        return rowConstraint;
    }

    public Constraint<List<V>> outerConstraint() {
        return outerConstraint;
    }

    @Override
    public boolean isValid(List<V> vector) {
        List<List<V>> rows = Lists.partition(vector, rowConstraint.length());
        return rows.stream().allMatch(rowConstraint::isValid) && outerConstraint.isValid(rows);
    }

    @Override
    public int length() {
        return length;
    }
}
