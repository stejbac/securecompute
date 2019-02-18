package securecompute.constraint;

import java.util.Random;

public interface ZeroKnowledgeLocallyTestableProof<V> extends LocallyTestableProof<V> {

    Random getRandom();

    double minimumAllowedFalseNegativeProbability();

    @Override
    ZeroKnowledgeLocalTest<V, ?> localTest();

    @Override
    ZeroKnowledgeLocalTest<V, ?> localTest(double maxFalseNegativeProbability);

    ZeroKnowledgeLocalTest<V, ?> localTestOfMaximalPower();

    interface ZeroKnowledgeLocalTest<V, S extends LocalTest.Evidence> extends LocalTest<V, S> {

        S simulate(Random random);
    }

    abstract class ZeroKnowledgeRepeatedLocalTest<V, S extends LocalTest.Evidence> extends RepeatedLocalTest<V, S>
            implements ZeroKnowledgeLocalTest<V, RepeatedLocalTest.RepeatedEvidence<S>> {

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V, S> singleTest, int repetitionCount) {
            super(singleTest, repetitionCount);
        }

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V, S> singleTest, double maxFalseNegativeProbability, int maxAllowedRepetitions) {
            super(singleTest, maxFalseNegativeProbability);
            if (repetitionCount() > maxAllowedRepetitions) {
                throw new IllegalArgumentException("Test repetition count exceeds maximum guaranteeing zero knowledge");
            }
        }

        public static double minimumAllowedFalseNegativeProbability(LocalTest<?, ?> singleTest, int maxAllowedRepetitions) {
            double prob = Math.pow(singleTest.falseNegativeProbability(), maxAllowedRepetitions);
            // Handle rounding errors - must round probability up for safety:
            while (repetitionsRequiredForDesiredPower(singleTest, prob) > maxAllowedRepetitions) {
                prob = Math.nextUp(prob);
            }
            return prob;
        }

        @Override
        public abstract RepeatedEvidence<S> simulate(Random random);
    }
}
