package securecompute.constraint.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.constraint.Constraint;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BlockConstraint<V> implements Constraint<List<V>> {

    private final Constraint<V> columnConstraint;
    private final int rowLength;
    private final int length;

    public BlockConstraint(Constraint<V> columnConstraint, int rowLength) {
        this.columnConstraint = columnConstraint;
        this.rowLength = rowLength;
        length = columnConstraint.length();
    }

    public Constraint<V> columnConstraint() {
        return columnConstraint;
    }

    public int rowLength() {
        return rowLength;
    }

    @Override
    public boolean isValid(List<List<V>> rows) {
        return streamLayers(rows, rowLength).allMatch(columnConstraint::isValid);
    }

    @Override
    public int length() {
        return length;
    }

    //    @SuppressWarnings("ConstantConditions") // IDEA is simply wrong about "row != null" always being true here...
    public static <V> Stream<List<V>> streamLayers(List<List<V>> rows, int rowLength) {
        return IntStream.range(0, rowLength).mapToObj(i ->
                Lists.transform(rows, row -> row != null ? row.get(i) : null));
    }

    // TODO: Should we infer the row length here (& maybe above as well)?
    public static <V> List<List<V>> transpose(List<List<V>> rows, int rowLength) {
        return streamLayers(rows, rowLength)
                .map(ImmutableList::copyOf)
                .collect(ImmutableList.toImmutableList());
    }
}
