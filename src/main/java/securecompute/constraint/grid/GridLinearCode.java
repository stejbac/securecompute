package securecompute.constraint.grid;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.constraint.Constraint;
import securecompute.constraint.LinearCode;
import securecompute.constraint.LocallyTestableCode;
import securecompute.constraint.LocallyTestableCode.LocalTest.Evidence;
import securecompute.constraint.block.BlockLinearCode;
import securecompute.constraint.concatenated.ConcatenatedLinearCode;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public CompoundLocalTest<V> localTest(double maxFalsePositiveProbability) {
        return new CompoundLocalTest<>(rowConstraint(), columnConstraint(), this, maxFalsePositiveProbability);
    }

    static abstract class BaseLocalTest<V, S extends Evidence> implements LocallyTestableCode.LocalTest<V, S> {

        private final Constraint<V> rowConstraint, columnConstraint;
        private final GridLinearCode<?, ?> code;
        private final int distance;

        BaseLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code) {
            this.rowConstraint = rowConstraint;
            this.columnConstraint = columnConstraint;
            this.code = code;
            int excludedColCount = rowConstraint.length() - code.rowConstraint().length();
            int excludedRowCount = columnConstraint.length() - code.columnConstraint().length();
            distance = (code.rowConstraint().distance() + excludedColCount - 1) * (code.columnConstraint().distance() + excludedRowCount - 1);
        }

        GridLinearCode<?, ?> code() {
            return code;
        }

        @Override
        public int distance() {
            return distance;
        }

        SimpleGridEvidence<V> query(List<V> vector, int x, int y) {
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

    public static abstract class SimpleGridEvidence<V> implements Evidence {

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

    public static class SimpleLocalTest<V> extends BaseLocalTest<V, SimpleGridEvidence<V>> {

        private final double falsePositiveProbability;
        private final double rowSelectionProbability;

        SimpleLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code) {
            this(rowConstraint, columnConstraint, code, optimalRowSelectionProbability(code));
        }

        private SimpleLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code,
                                double rowSelectionProbability) {
            super(rowConstraint, columnConstraint, code);
            int rowDistance = code.rowConstraint().distance();
            int colDistance = code.columnConstraint().distance();

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
        public double falsePositiveProbability() {
            return falsePositiveProbability;
        }

        public double rowSelectionProbability() {
            return rowSelectionProbability;
        }

        @Override
        public SimpleGridEvidence<V> query(List<V> vector, Random random) {
            return random.nextDouble() <= rowSelectionProbability
                    ? query(vector, -1, random.nextInt(code().columnConstraint().length()))
                    : query(vector, random.nextInt(code().rowConstraint().length()), -1);
        }
    }

    public static class CompoundLocalTest<V> extends BaseLocalTest<V, RepeatedEvidence<SimpleGridEvidence<V>>> {

        private final int rowSampleCount, columnSampleCount;
        private final double falsePositiveProbability;

        public CompoundLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code,
                                 double maxFalsePositiveProbability) {
            this(rowConstraint, columnConstraint, code,
                    samplesRequiredForDesiredConfidence(code.columnConstraint(), maxFalsePositiveProbability),
                    samplesRequiredForDesiredConfidence(code.rowConstraint(), maxFalsePositiveProbability)
            );
        }

        public CompoundLocalTest(Constraint<V> rowConstraint, Constraint<V> columnConstraint, GridLinearCode<?, ?> code,
                                 int rowSampleCount, int columnSampleCount) {

            super(rowConstraint, columnConstraint, code);
            this.rowSampleCount = rowSampleCount;
            this.columnSampleCount = columnSampleCount;
            falsePositiveProbability = falsePositiveProbability(code, rowSampleCount, columnSampleCount);
        }

        // guaranteed to give a probability such that the proceeding method gives required samples <= [row|column]SampleCount:
        static double falsePositiveProbability(GridLinearCode<?, ?> code, int rowSampleCount, int columnSampleCount) {
            int width = code.rowConstraint().length();
            int height = code.columnConstraint().length();
            int goodColCount = width - code.rowConstraint().distance();
            int goodRowCount = height - code.columnConstraint().distance();

            // minimal log-probability (for bad vector) that we sample only good rows/columns, missing all the bad ones:
            double logRowErrorProb = BinomialUtils.logBinomialCoefficientRatio(goodRowCount, height, rowSampleCount);
            double logColErrorProb = BinomialUtils.logBinomialCoefficientRatio(goodColCount, width, columnSampleCount);

            return Math.exp(Math.max(logRowErrorProb, logColErrorProb));
        }

        private static int samplesRequiredForDesiredConfidence(LinearCode<?, ?> lineCode, double maxFalsePositiveProbability) {
            // TODO: Is there a more efficient way of calculating this?
            int len = lineCode.length();
            int goodSymbolCount = len - lineCode.distance();
            double threshold = Math.log(maxFalsePositiveProbability);
            while (Math.exp(threshold) > maxFalsePositiveProbability) {
                threshold = Math.nextDown(threshold);
            }
            for (int k = 0; ; k++) {
                if (BinomialUtils.logBinomialCoefficientRatio(goodSymbolCount, len, k) <= threshold) {
                    return k;
                }
            }
        }

        public int rowSampleCount() {
            return rowSampleCount;
        }

        public int columnSampleCount() {
            return columnSampleCount;
        }

        @Override
        public double falsePositiveProbability() {
            return falsePositiveProbability;
        }

        @Override
        public RepeatedEvidence<SimpleGridEvidence<V>> query(List<V> vector, Random random) {
            int[] colChoice = BinomialUtils.sortedRandomChoice(code().rowConstraint().length(), columnSampleCount, random);
            int[] rowChoice = BinomialUtils.sortedRandomChoice(code().columnConstraint().length(), rowSampleCount, random);

            List<SimpleGridEvidence<V>> evidenceList = Stream.concat(
                    Arrays.stream(colChoice).mapToObj(x -> query(vector, x, -1)),
                    Arrays.stream(rowChoice).mapToObj(y -> query(vector, -1, y))
            ).collect(ImmutableList.toImmutableList());

            return new RepeatedEvidence<>(evidenceList);
        }
    }
}
