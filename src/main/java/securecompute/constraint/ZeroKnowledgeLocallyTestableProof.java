package securecompute.constraint;

import java.util.Random;

public interface ZeroKnowledgeLocallyTestableProof<V> extends LocallyTestableProof<V> {

    Random getRandom();

    double minimumAllowedFalseNegativeProbability();

    @Override
    ZeroKnowledgeLocalTest<V> localTest();

    @Override
    ZeroKnowledgeLocalTest<V> localTest(double maxFalseNegativeProbability);

    default ZeroKnowledgeLocalTest<V> localTestOfMaximalPower() {
        return localTest(minimumAllowedFalseNegativeProbability());
    }

    // TODO: Consider adding an evidence type parameter to LocalTest, to ensure the same return types for 'simulate' & 'query'.
    interface ZeroKnowledgeLocalTest<V> extends LocalTest<V> {

        Evidence simulate(Random random);
    }

    abstract class ZeroKnowledgeRepeatedLocalTest<V> extends RepeatedLocalTest<V> implements ZeroKnowledgeLocalTest<V> {

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V> singleTest, int repetitionCount) {
            super(singleTest, repetitionCount);
        }

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V> singleTest, double maxFalseNegativeProbability, int maxAllowedRepetitions) {
            super(singleTest, maxFalseNegativeProbability);
            if (repetitionCount() > maxAllowedRepetitions) {
                throw new IllegalArgumentException("Test repetition count exceeds maximum guaranteeing zero knowledge");
            }
        }

        public static double minimumAllowedFalseNegativeProbability(LocalTest<?> singleTest, int maxAllowedRepetitions) {
            double prob = Math.pow(singleTest.falseNegativeProbability(), maxAllowedRepetitions);
            // Handle rounding errors - must round probability up for safety:
            while (repetitionsRequiredForDesiredPower(singleTest, prob) > maxAllowedRepetitions) {
                prob = Math.nextUp(prob);
            }
            return prob;
        }

        @Override
        public abstract RepeatedLocalTest.Evidence simulate(Random random);
    }
}
