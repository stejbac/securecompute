package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;
import securecompute.algebra.Gf256;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.constraint.AlgebraicConstraint;
import securecompute.constraint.LocallyTestableCode.LocalTest;
import securecompute.constraint.LocallyTestableCode.RepeatedLocalTest.RepeatedEvidence;
import securecompute.constraint.ZeroKnowledgeLocallyTestableProof.ZeroKnowledgeLocalTest;
import securecompute.constraint.cyclic.ReedSolomonCode;
import securecompute.constraint.grid.GridLinearCode.SimpleGridEvidence;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static securecompute.constraint.grid.GridProofTest.algebraicConstraint;

class ZeroKnowledgeGridProofTest {

    private static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);
    private static final SingletonVectorSpace<Gf256.Element> BLOCK_SPACE = new SingletonVectorSpace<>(AES_FIELD);

    private static final ReedSolomonCode<Gf256.Element> ROW_CODE = new ReedSolomonCode<>(128, 49, AES_FIELD);
    private static final ReedSolomonCode<Gf256.Element> COL_CODE = new ReedSolomonCode<>(255, 49, AES_FIELD);

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

    @Test
    void localTestsHaveExpectedProperties() {
        double minErrorRate = GRID_PROOF.minimumAllowedFalsePositiveProbability();
        System.out.println(minErrorRate);

        assertEquals(minErrorRate, GRID_PROOF.localTestOfMaximalConfidence().falsePositiveProbability(), 1e-15);
        assertEquals(48, GRID_PROOF.localTestOfMaximalConfidence().repetitionCount());
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

        LocalTest.Evidence evidence = maxConfidenceTest.query(ENCODED_VALID_WITNESS, new Random(2468));
        assertTrue(evidence.isValid());
    }

    @Test
    void simulatedEvidenceHasExpectedProperties() {
        GRID_PROOF.getRandom().setSeed(3579);
        ZeroKnowledgeGridProof<Gf256.Element, ?>.RepeatedLocalTest maxConfidenceTest = GRID_PROOF.localTestOfMaximalConfidence();

        RepeatedEvidence<? extends SimpleGridEvidence<?>> realEvidence = maxConfidenceTest.query(ENCODED_VALID_WITNESS, new Random(2468));
        RepeatedEvidence<? extends SimpleGridEvidence<?>> simulatedEvidence = maxConfidenceTest.simulate(new Random(2468));

        List<? extends SimpleGridEvidence<?>> realEvidenceList = realEvidence.evidenceList();
        List<? extends SimpleGridEvidence<?>> simulatedEvidenceList = simulatedEvidence.evidenceList();

        // Real vs. simulated sampled rows & columns are the same, given the same random seed...
        assertEquals(realEvidenceList.size(), simulatedEvidenceList.size());
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
                    assertTrue(elt1.equals(elt2));
                    return elt1;
                }));

        assertFalse(sampledElements.isEmpty());
    }

    private Stream<Map.Entry<Long, ?>> sampledElements(SimpleGridEvidence<?> e) {

        Stream<Map.Entry<Long, ?>> columnElements = Streams.mapWithIndex(e.column.stream(), (elt, y) ->
                Collections.singletonMap(e.x + y * 128L, elt).entrySet().iterator().next());
        Stream<Map.Entry<Long, ?>> rowElements = Streams.mapWithIndex(e.row.stream(), (elt, x) ->
                Collections.singletonMap(x + e.y * 128L, elt).entrySet().iterator().next());

        return Stream.concat(columnElements, rowElements);
    }
}
