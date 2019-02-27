package securecompute.constraint;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public interface LocallyTestableCode<V> extends Code<V> {

    /**
     * @return a simple local test of maximal distance and confidence (i.e. minimal false positive probability)
     */
    LocalTest<V, ?> localTest();

    /**
     * @param maxFalsePositiveProbability the maximum allowed false positive probability
     * @return a compound local test of maximal distance within the provided false positive probability bound
     */
    LocalTest<V, ?> localTest(double maxFalsePositiveProbability);

    /**
     * A local test of fixed distance and false positive probability. Local tests are probabilistic tests of closeness
     * of a given vector to some codeword. They only sample a small subset of the provided test vector, where each
     * sample is of ad hoc form implementing {@link Evidence}.
     * <p>
     * We say that a local test has distance <tt>d</tt> and false positive probability <tt>p</tt> if the following
     * holds:
     * <p>
     * For an arbitrary test vector <tt>v</tt>, possibly containing nulls (taken to be erasures), such that the test
     * passes with strictly greater probability than <tt>p</tt>, there is a unique codeword of Hamming distance less
     * than or equal to <tt>d</tt> from <tt>v</tt>.
     *
     * @param <V> the code alphabet (symbol type)
     * @param <S> the local test evidence type
     */
    interface LocalTest<V, S extends LocalTest.Evidence> {

        int distance();

        double falsePositiveProbability();

        S query(List<V> vector, Random random);

        interface Evidence {

            boolean isValid();
        }
    }

    class RepeatedLocalTest<V, S extends LocalTest.Evidence> implements LocalTest<V, RepeatedEvidence<S>> {

        private final LocalTest<V, S> singleTest;
        private final int repetitionCount;

        RepeatedLocalTest(LocalTest<V, S> singleTest, double maxFalsePositiveProbability) {
            this(singleTest, repetitionsRequiredForDesiredConfidence(singleTest, maxFalsePositiveProbability));
        }

        RepeatedLocalTest(LocalTest<V, S> singleTest, int repetitionCount) {
            this.singleTest = singleTest;
            this.repetitionCount = repetitionCount;
        }

        static int repetitionsRequiredForDesiredConfidence(LocalTest<?, ?> singleTest, double maxFalsePositiveProbability) {
            // TODO: Make sure rounding is handled correctly here:
            return (int) Math.ceil(
                    Math.log(maxFalsePositiveProbability) / Math.log(singleTest.falsePositiveProbability()));
        }

        public LocalTest<V, S> singleTest() {
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
        public double falsePositiveProbability() {
            // TODO: Make sure rounding is handled correctly here:
            return Math.pow(singleTest.falsePositiveProbability(), repetitionCount);
        }

        @Override
        public RepeatedEvidence<S> query(List<V> vector, Random random) {
            List<S> evidenceList = IntStream.range(0, repetitionCount)
                    .mapToObj(i -> singleTest.query(vector, random))
                    .collect(ImmutableList.toImmutableList());

            return new RepeatedEvidence<>(evidenceList);
        }

    }

    class RepeatedEvidence<S extends LocalTest.Evidence> implements LocalTest.Evidence {

        private final List<S> evidenceList;

        public RepeatedEvidence(List<S> evidenceList) {
            this.evidenceList = ImmutableList.copyOf(evidenceList);
        }

        public List<S> evidenceList() {
            return evidenceList;
        }

        @Override
        public boolean isValid() {
            return evidenceList.stream().allMatch(LocalTest.Evidence::isValid);
        }
    }
}
