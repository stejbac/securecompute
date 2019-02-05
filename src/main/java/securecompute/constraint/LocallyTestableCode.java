package securecompute.constraint;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public interface LocallyTestableCode<V> extends Code<V> {

    /**
     * @return a simple local test of maximal distance and power (i.e. minimal false negative probability)
     */
    LocalTest<V> localTest();

    /**
     * @param maxFalseNegativeProbability the maximum allowed false negative probability
     * @return a compound local test of maximal distance within the provided false negative probability bound
     */
    default LocalTest<V> localTest(double maxFalseNegativeProbability) {
        return new RepeatedLocalTest<>(localTest(), maxFalseNegativeProbability);
    }

    /**
     * A local test of fixed distance and false negative probability. Local tests are probabilistic tests of closeness
     * of a given vector to some codeword. They only sample a small subset of the provided test vector, where each
     * sample is of ad hoc form implementing {@link Evidence}.
     * <p>
     * We say that a local test has distance <tt>d</tt> and false negative probability <tt>p</tt> if the following
     * holds:
     * <p>
     * For an arbitrary test vector <tt>v</tt>, possibly containing nulls (taken to be erasures), such that the test
     * passes with strictly greater probability than <tt>p</tt>, there is a unique codeword of Hamming distance less
     * than or equal to <tt>d</tt> from <tt>v</tt>.
     *
     * @param <V> the code alphabet (symbol type)
     */
    interface LocalTest<V> {

        int distance();

        double falseNegativeProbability();

        Evidence query(List<V> vector, Random random);

        interface Evidence {

            boolean isFailure();
        }
    }

    class RepeatedLocalTest<V> implements LocalTest<V> {

        private final LocalTest<V> singleTest;
        private final int repetitionCount;

        RepeatedLocalTest(LocalTest<V> singleTest, double maxFalseNegativeProbability) {
            this(singleTest, repetitionsRequiredForDesiredPower(singleTest, maxFalseNegativeProbability));
        }

        RepeatedLocalTest(LocalTest<V> singleTest, int repetitionCount) {
            this.singleTest = singleTest;
            this.repetitionCount = repetitionCount;
        }

        static int repetitionsRequiredForDesiredPower(LocalTest<?> singleTest, double maxFalseNegativeProbability) {
            // TODO: Make sure rounding is handled correctly here:
            return (int) Math.ceil(
                    Math.log(maxFalseNegativeProbability) / Math.log(singleTest.falseNegativeProbability()));
        }

        public LocalTest<V> singleTest() {
            return singleTest;
        }

        public int repetitionCount() {
            return repetitionCount;
        }

        @Override
        public int distance() {
            return singleTest.distance();
        }

        @Override
        public double falseNegativeProbability() {
            // TODO: Make sure rounding is handled correctly here:
            return Math.pow(singleTest.falseNegativeProbability(), repetitionCount);
        }

        @Override
        public Evidence query(List<V> vector, Random random) {
            List<LocalTest.Evidence> evidenceList = IntStream.range(0, repetitionCount)
                    .mapToObj(i -> singleTest.query(vector, random))
                    .collect(ImmutableList.toImmutableList());

            return new Evidence(evidenceList);
        }

        public static class Evidence implements LocalTest.Evidence {

            private final List<LocalTest.Evidence> evidenceList;

            public Evidence(List<LocalTest.Evidence> evidenceList) {
                this.evidenceList = ImmutableList.copyOf(evidenceList);
            }

            public List<LocalTest.Evidence> evidenceList() {
                return evidenceList;
            }

            @Override
            public boolean isFailure() {
                return evidenceList.stream().anyMatch(LocalTest.Evidence::isFailure);
            }
        }
    }
}
