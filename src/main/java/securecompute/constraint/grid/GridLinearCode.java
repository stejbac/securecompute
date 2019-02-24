package securecompute.constraint.grid;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import securecompute.constraint.Constraint;
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
    public SimpleLocalTest<V> localTest() {
        return new SimpleLocalTest<>(rowConstraint(), columnConstraint(), this);
    }

    @Override
    public RepeatedLocalTest<V, SimpleGridEvidence<V>> localTest(double maxFalsePositiveProbability) {
        return new RepeatedLocalTest<>(localTest(), maxFalsePositiveProbability);
    }

    public static class SimpleLocalTest<V> implements LocallyTestableCode.LocalTest<V, SimpleGridEvidence<V>> {

        private final Constraint<V> rowConstraint, columnConstraint;
        private final GridLinearCode<?, ?> code;
        private final int distance;
        private final double falsePositiveProbability;
        private final double rowSelectionProbability;

        SimpleLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code) {
            this(rowConstraint, columnConstraint, code, optimalRowSelectionProbability(code));
        }

        private SimpleLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code,
                                double rowSelectionProbability) {
            this.rowConstraint = rowConstraint;
            this.columnConstraint = columnConstraint;
            this.code = code;
            int rowDistance = code.rowConstraint().distance();
            int colDistance = code.columnConstraint().distance();
            int excludedColCount = rowConstraint.length() - code.rowConstraint().length();
            int excludedRowCount = columnConstraint.length() - code.columnConstraint().length();
            distance = (rowDistance + excludedColCount - 1) * (colDistance + excludedRowCount - 1);

            // TODO: Make sure rounding is handled correctly here...
            this.rowSelectionProbability = rowSelectionProbability;
            double colSelectionProbability = 1 - rowSelectionProbability;

            falsePositiveProbability = Math.max(
                    1 - colSelectionProbability * rowDistance / code.rowConstraint().length(),
                    1 - rowSelectionProbability * colDistance / code.columnConstraint().length()
            );
        }

        private static double optimalRowSelectionProbability(GridLinearCode<?, ?> code) {
            double x = (double) code.rowConstraint().distance() * code.columnConstraint().length();
            double y = (double) code.columnConstraint().distance() * code.rowConstraint().length();
            return x / (x + y);
        }

        @Override
        public int distance() {
            return distance;
        }

        @Override
        public double falsePositiveProbability() {
            return falsePositiveProbability;
        }

        public double rowSelectionProbability() {
            return rowSelectionProbability;
        }

        @Override
        public SimpleGridEvidence<V> query(List<V> vector, Random random) {
            return random.nextDouble() <= rowSelectionProbability
                    ? query(vector, -1, random.nextInt(code.columnConstraint().length()))
                    : query(vector, random.nextInt(code.rowConstraint().length()), -1);
        }

        private SimpleGridEvidence<V> query(List<V> vector, int x, int y) {
            List<List<V>> rows = Lists.partition(vector, rowConstraint.length());

            // Don't use 'toImmutableList', in order to support null elements (erasures):
            List<V> line = y >= 0
                    ? rows.get(y)
                    : rows.stream().map(r -> r.get(x)).collect(Collectors.toList());

            return evidence(x, y, line);
        }

        SimpleGridEvidence<V> evidence(int x, int y, List<V> line) {
            return new SimpleGridEvidence<V>(x, y, line) {
                @Override
                public boolean isValid() {
                    return y >= 0 ? rowConstraint.isValid(line) : columnConstraint.isValid(line);
                }
            };
        }
    }

    public static abstract class SimpleGridEvidence<V> implements LocalTest.Evidence {

        // TODO: Make private:
        final int x, y;
        final List<V> line;

        SimpleGridEvidence(int x, int y, List<V> line) {
            if (x < 0 && y < 0 || x != -1 && y != -1) {
                throw new IllegalArgumentException("Exactly one index must be non-negative and the other must equal -1");
            }
            this.x = x;
            this.y = y;
            this.line = line;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("x", x)
                    .add("y", y)
                    .add("line", line)
                    .toString();
        }
    }
}
