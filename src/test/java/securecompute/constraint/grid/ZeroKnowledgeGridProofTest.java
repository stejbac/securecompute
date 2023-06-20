package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;
import securecompute.algebra.Gf256;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.constraint.AlgebraicConstraint;
import securecompute.constraint.LocallyTestableCode.LocalTest;
import securecompute.constraint.LocallyTestableCode.LocalTest.Evidence;
import securecompute.constraint.LocallyTestableCode.RepeatedEvidence;
import securecompute.constraint.ZeroKnowledgeLocallyTestableProof.ZeroKnowledgeLocalTest;
import securecompute.constraint.cyclic.ReedSolomonCode;
import securecompute.constraint.grid.GridLinearCode.SimpleGridEvidence;
import securecompute.helper.LowDiscrepancyFakeRandom;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static securecompute.constraint.grid.GridProofTest.algebraicConstraint;

class ZeroKnowledgeGridProofTest {

    private static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);
    private static final SingletonVectorSpace<Gf256.Element> BLOCK_SPACE = new SingletonVectorSpace<>(AES_FIELD);

    // These choices of k are optimal (minimising ZK test significance, followed by nonce size), for the given grid & witness dimensions:
    private static final ReedSolomonCode<Gf256.Element> ROW_CODE = new ReedSolomonCode<>(128, 24, AES_FIELD);
    private static final ReedSolomonCode<Gf256.Element> COL_CODE = new ReedSolomonCode<>(255, 52, AES_FIELD);

    private static final AlgebraicConstraint<Gf256.Element, Gf256.Element> ROW_MESSAGE_CONSTRAINT = algebraicConstraint(2, 1, 1,
            BLOCK_SPACE,
            v -> ImmutableList.of(v.get(0).pow(2).subtract(AES_FIELD.exp(1)))
    );

    private static final AlgebraicConstraint<Gf256.Element, Gf256.Element> COL_MESSAGE_CONSTRAINT = algebraicConstraint(1, 1, 0,
            BLOCK_SPACE,
            v -> ImmutableList.of()
    );

    private static final ZeroKnowledgeGridProof<Gf256.Element, Gf256.Element> GRID_PROOF = new ZeroKnowledgeGridProof<>(
            new GridLinearCode<>(ROW_CODE, COL_CODE),
            new TripleLayerConstraint<>(ROW_MESSAGE_CONSTRAINT, COL_MESSAGE_CONSTRAINT),
            new Random(1357)
    );

    private static final List<List<Gf256.Element>> VALID_WITNESS = ImmutableList.of(ImmutableList.of(
            AES_FIELD.exp(128), AES_FIELD.zero(), AES_FIELD.zero()));

    private static final List<List<Gf256.Element>> INVALID_WITNESS = ImmutableList.of(ImmutableList.of(
            AES_FIELD.exp(129), AES_FIELD.zero(), AES_FIELD.zero()));

    private static final List<List<Gf256.Element>> ENCODED_VALID_WITNESS = GRID_PROOF.encode(VALID_WITNESS);

    private static final List<Gf256.Element> MINIMAL_WEIGHT_INVALID_COL = COL_CODE.pow(2).encode(
            IntStream.range(0, 103)
                    .mapToObj(i -> i == 102 ? AES_FIELD.one() : AES_FIELD.zero())
                    .collect(ImmutableList.toImmutableList())
    );

    // Modify the last parity column, so that all the columns are good & a minimal number of (non-witness-intersecting) rows are bad:
    @SuppressWarnings("ConstantConditions")
    private static final List<List<Gf256.Element>> MINIMAL_BAD_VECTOR_OF_ERRORS = Streams.mapWithIndex(ENCODED_VALID_WITNESS.stream(), (x, i) ->
            i % 128 < 127 ? x : ImmutableList.of(x.get(0), x.get(1), MINIMAL_WEIGHT_INVALID_COL.get((int) i / 128))
    ).collect(ImmutableList.toImmutableList());

    @Test
    void localTestsHaveExpectedProperties() {
        double minErrorRate = GRID_PROOF.minimumAllowedFalsePositiveProbability();
        System.out.println(minErrorRate);

        assertEquals(minErrorRate, GRID_PROOF.localTestOfMaximalConfidence().falsePositiveProbability(), 1e-15);
        assertEquals(51, GRID_PROOF.localTestOfMaximalConfidence().rowSampleCount());
        assertEquals(23, GRID_PROOF.localTestOfMaximalConfidence().columnSampleCount());
        assertEquals((128 - 24) * (255 - 103), GRID_PROOF.localTest().distance());
    }

    @Test
    void simpleLocalTestHasCorrectFalsePositiveRate() {
        Random rnd = new LowDiscrepancyFakeRandom(12345);

        long passCount = Stream.generate(() -> GRID_PROOF.localTest().query(MINIMAL_BAD_VECTOR_OF_ERRORS, rnd))
                .limit(1000)
                .filter(Evidence::isValid)
                .count();
        double passRate = passCount / 1000.0;

        assertEquals(passRate, GRID_PROOF.localTest().falsePositiveProbability(), 0.002);
    }

    @Test
    void compoundLocalTestHasCorrectFalsePositiveRate() {
        Random rnd = new LowDiscrepancyFakeRandom(12345);
        LocalTest<List<Gf256.Element>, ?> localTest = GRID_PROOF.localTest(0.5);

        long passCount = Stream.generate(() -> localTest.query(MINIMAL_BAD_VECTOR_OF_ERRORS, rnd))
                .limit(500)
                .filter(Evidence::isValid)
                .count();
        double passRate = passCount / 500.0;

        assertEquals(passRate, localTest.falsePositiveProbability(), 0.002);
        assertTrue(localTest.falsePositiveProbability() < 0.5);
    }

    @Test
    void invalidWitnessThrowsOnEncode() {
        GRID_PROOF.getRandom().setSeed(1357);
        assertThrows(IllegalArgumentException.class, () -> GRID_PROOF.encode(INVALID_WITNESS));
    }

    @Test
    void encodedValidWitnessIsValid() {
        assertTrue(GRID_PROOF.isValid(ENCODED_VALID_WITNESS));
    }

    @Test
    void decodeIsLeftInverseOfEncode() {
        assertEquals(VALID_WITNESS, GRID_PROOF.decode(ENCODED_VALID_WITNESS));
    }

    @Test
    void encodedValidWitnessPassesMaxConfidenceTest() {
        ZeroKnowledgeLocalTest<List<Gf256.Element>, ?> maxConfidenceTest = GRID_PROOF.localTestOfMaximalConfidence();

        Evidence evidence = maxConfidenceTest.query(ENCODED_VALID_WITNESS, new Random(2468));
        assertTrue(evidence.isValid());
    }

    @Test
    void simulatedEvidenceHasExpectedProperties() {
        GRID_PROOF.getRandom().setSeed(3579);
        ZeroKnowledgeGridProof<Gf256.Element, ?>.CompoundLocalTest maxConfidenceTest = GRID_PROOF.localTestOfMaximalConfidence();

        RepeatedEvidence<? extends SimpleGridEvidence<?>> realEvidence = maxConfidenceTest.query(ENCODED_VALID_WITNESS, new Random(2468));
        RepeatedEvidence<? extends SimpleGridEvidence<?>> simulatedEvidence = maxConfidenceTest.simulate(new Random(2468));

        List<? extends SimpleGridEvidence<?>> realEvidenceList = realEvidence.evidenceList();
        List<? extends SimpleGridEvidence<?>> simulatedEvidenceList = simulatedEvidence.evidenceList();

        // Real vs. simulated sampled rows & columns are the same, given the same random seed...
        assertEquals(realEvidenceList.size(), simulatedEvidenceList.size());
        //noinspection UnstableApiUsage
        assertAll(Streams.zip(realEvidenceList.stream(), simulatedEvidenceList.stream(), (e, f) -> () -> {
            assertEquals(e.getClass(), f.getClass());
            assertEquals(e.x, f.x);
            assertEquals(e.y, f.y);
        }));

        // Simulated evidence is valid.
        assertTrue(simulatedEvidence.isValid());

        // Simulated evidence is consistent (i.e. the rows & columns agree where they overlap or repeat).
        Map<Long, ?> sampledElements = simulatedEvidenceList.stream()
                .flatMap(this::sampledElements)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (elt1, elt2) -> {
                    assertEquals(elt1, elt2);
                    return elt1;
                }));

        assertFalse(sampledElements.isEmpty());
    }

    @Test
    void singletonSimulatedEvidenceHasExpectedProperties() {
        GRID_PROOF.getRandom().setSeed(3579);
        ZeroKnowledgeGridProof<Gf256.Element, ?>.SimpleLocalTest simpleTest = GRID_PROOF.localTest();

        SimpleGridEvidence<?> realEvidence = simpleTest.query(ENCODED_VALID_WITNESS, new Random(2468));
        SimpleGridEvidence<?> simulatedEvidence = simpleTest.simulate(new Random(2468));

        // Real vs. simulated sampled row/column is the same, given the same random seed...
        assertAll(() -> {
            assertEquals(realEvidence.getClass(), simulatedEvidence.getClass());
            assertEquals(realEvidence.x, simulatedEvidence.x);
            assertEquals(realEvidence.y, simulatedEvidence.y);
        });

        // Simulated evidence is valid.
        assertTrue(simulatedEvidence.isValid());
    }

    private Stream<Map.Entry<Long, ?>> sampledElements(SimpleGridEvidence<?> e) {
        return Streams.mapWithIndex(e.line.stream(), (elt, i) ->
                Collections.singletonMap(e.y >= 0 ? i + e.y * 128L : e.x + i * 128L, elt).entrySet().iterator().next());
    }
}
