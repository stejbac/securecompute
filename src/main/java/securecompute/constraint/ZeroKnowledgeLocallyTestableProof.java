package securecompute.constraint;

import java.util.Random;

public interface ZeroKnowledgeLocallyTestableProof<V> extends LocallyTestableProof<V> {

    Random getRandom();

    double minimumAllowedFalsePositiveProbability();

    @Override
    ZeroKnowledgeLocalTest<V, ?> localTest();

    @Override
    ZeroKnowledgeLocalTest<V, ?> localTest(double maxFalsePositiveProbability);

    ZeroKnowledgeLocalTest<V, ?> localTestOfMaximalConfidence();

    interface ZeroKnowledgeLocalTest<V, S extends LocalTest.Evidence> extends LocalTest<V, S> {

        S simulate(Random random);
    }

    abstract class ZeroKnowledgeRepeatedLocalTest<V, S extends LocalTest.Evidence> extends RepeatedLocalTest<V, S>
            implements ZeroKnowledgeLocalTest<V, RepeatedEvidence<S>> {

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V, S> singleTest, int repetitionCount) {
            super(singleTest, repetitionCount);
        }

        protected ZeroKnowledgeRepeatedLocalTest(LocalTest<V, S> singleTest, double maxFalsePositiveProbability, int maxAllowedRepetitions) {
            super(singleTest, maxFalsePositiveProbability);
            if (repetitionCount() > maxAllowedRepetitions) {
                throw new IllegalArgumentException("Test repetition count exceeds maximum guaranteeing zero knowledge");
            }
        }

        public static double minimumAllowedFalsePositiveProbability(LocalTest<?, ?> singleTest, int maxAllowedRepetitions) {
            double prob = Math.pow(singleTest.falsePositiveProbability(), maxAllowedRepetitions);
            // Handle rounding errors - must round probability up for safety:
            while (repetitionsRequiredForDesiredConfidence(singleTest, prob) > maxAllowedRepetitions) {
                prob = Math.nextUp(prob);
            }
            return prob;
        }

        @Override
        public abstract RepeatedEvidence<S> simulate(Random random);
    }
}
