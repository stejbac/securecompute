package securecompute.constraint.grid;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import securecompute.constraint.LinearCode;
import securecompute.constraint.LocallyTestableCode;
import securecompute.constraint.block.BlockLinearCode;
import securecompute.constraint.concatenated.ConcatenatedLinearCode;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GridLinearCode<V, E> extends ConcatenatedLinearCode<V, E> implements LocallyTestableCode<V> {

    private final LinearCode<V, E> columnConstraint;

    public GridLinearCode(LinearCode<V, E> rowCode, LinearCode<V, E> columnCode) {
        super(rowCode, new BlockLinearCode<>(columnCode, rowCode.length()));
        this.columnConstraint = columnCode;
    }

    public LinearCode<V, E> columnConstraint() {
        return columnConstraint;
    }

    @Override
    public BlockLinearCode<V, E> outerConstraint() {
        return (BlockLinearCode<V, E>) super.outerConstraint();
    }

    @Override
    public SimpleLocalTest<V, E> localTest() {
        return new SimpleLocalTest<>(this);
    }

    @Override
    public RepeatedLocalTest<V, SimpleGridEvidence<V>> localTest(double maxFalsePositiveProbability) {
        return new RepeatedLocalTest<>(localTest(), maxFalsePositiveProbability);
    }

    public static class SimpleLocalTest<V, E> implements LocallyTestableCode.LocalTest<V, SimpleGridEvidence<V>> {

        private final GridLinearCode<V, E> code;
        private final int distance;
        private final double falsePositiveProbability;

        SimpleLocalTest(GridLinearCode<V, E> code) {
            this(code, 0, 0);
        }

        SimpleLocalTest(GridLinearCode<V, E> code, int excludedRowCount, int excludedColumnCount) {
            this.code = code;
            int rowDistance = code.rowConstraint().distance() - 1;
            int colDistance = code.columnConstraint().distance() - 1;
            distance = rowDistance * colDistance;
            // TODO: Make sure rounding is handled correctly here:
            double rowLen = code.rowConstraint().length() - excludedRowCount;
            double colLen = code.columnConstraint().length() - excludedColumnCount;
//            double prob1 = (1 - (rowDistance + 1) / rowLen) * (1 - colDistance / colLen);
//            double prob2 = (1 - rowDistance / rowLen) * (1 - (colDistance + 1) / colLen);
//            double prob1 = (1 - (rowDistance + 1) / rowLen) * (1 - 1 / colLen);
//            double prob2 = (1 - 1 / rowLen) * (1 - (colDistance + 1) / colLen);
            falsePositiveProbability = Math.max(
                    1 - (rowDistance + 1 - excludedRowCount) / rowLen,
                    1 - (colDistance + 1 - excludedColumnCount) / colLen
            );
        }

        public GridLinearCode<V, E> code() {
            return code;
        }

        @Override
        public int distance() {
            return distance;
        }

        @Override
        public double falsePositiveProbability() {
            return falsePositiveProbability;
        }

        @Override
        public SimpleGridEvidence<V> query(List<V> vector, Random random) {
            return query(vector,
                    random.nextInt(code.rowConstraint().length()),
                    random.nextInt(code.columnConstraint().length()));
        }

        final SimpleGridEvidence<V> query(List<V> vector, int x, int y) {
            List<List<V>> rows = Lists.partition(vector, code.rowConstraint().length());

            List<V> row = rows.get(y);
            List<V> column = rows.stream()
                    .map(r -> r.get(x))
                    .collect(Collectors.toList()); // Don't use 'toImmutableList', in order to support null elements (erasures)

            return evidence(x, y, column, row);
        }

        protected SimpleGridEvidence<V> evidence(int x, int y, List<V> column, List<V> row) {
            return new SimpleGridEvidence<V>(x, y, column, row) {
                @Override
                public boolean isValid() {
                    return code.rowConstraint().isValid(row) && code.columnConstraint().isValid(column);
                }
            };
        }
    }

    public static abstract class SimpleGridEvidence<V> implements LocalTest.Evidence {

        // TODO: Make private:
        final int x, y;
        final List<V> column, row;

        SimpleGridEvidence(int x, int y, List<V> column, List<V> row) {
            this.x = x;
            this.y = y;
            this.column = column;
            this.row = row;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("x", x)
                    .add("y", y)
                    .add("column", column)
                    .add("row", row)
                    .toString();
        }
    }
}
