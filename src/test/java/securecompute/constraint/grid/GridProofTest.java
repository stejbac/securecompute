package securecompute.constraint.grid;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import securecompute.constraint.AlgebraicConstraint;
import securecompute.constraint.LocallyTestableCode;
import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.algebra.Gf256;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.constraint.cyclic.ReedSolomonCode;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GridProofTest {

    private static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);
    private static final SingletonVectorSpace<Gf256.Element> BLOCK_SPACE = new SingletonVectorSpace<>(AES_FIELD);

    private static final ReedSolomonCode<Gf256.Element> ROW_CODE = new ReedSolomonCode<>(255, 85, AES_FIELD);
    private static final ReedSolomonCode<Gf256.Element> COL_CODE = new ReedSolomonCode<>(200, 50, AES_FIELD);

    private static final AlgebraicConstraint<Gf256.Element, Gf256.Element> ROW_MESSAGE_CONSTRAINT = algebraicConstraint(2, 85,
            BLOCK_SPACE,
            v -> ImmutableList.of(v.get(0).multiply(v.get(1)).subtract(AES_FIELD.one()))
//            v.get(0).multiply(v.get(1)).equals(AES_FIELD.one())
    );

    private static final AlgebraicConstraint<Gf256.Element, Gf256.Element> COL_MESSAGE_CONSTRAINT = algebraicConstraint(1, 50,
            BLOCK_SPACE,
            v -> ImmutableList.of(v.get(0).add(v.get(1)).add(v.get(2)).subtract(AES_FIELD.one()))
//            v.get(0).add(v.get(1)).add(v.get(2)).equals(AES_FIELD.one())
    );

    private static final GridProof<Gf256.Element, Gf256.Element> GRID_PROOF = new GridProof<>(
            new GridLinearCode<>(ROW_CODE, COL_CODE),
            new TripleLayerConstraint<>(ROW_MESSAGE_CONSTRAINT, COL_MESSAGE_CONSTRAINT)
    );

    private static final List<Gf256.Element> RANDOM_MESSAGE_COLUMN = Stream
            .generate(Suppliers.compose(AES_FIELD::sampleUniformly, Suppliers.ofInstance(new Random(3456))))
            .limit(50)
            .collect(ImmutableList.toImmutableList());

    private static final List<List<Gf256.Element>> VALID_MESSAGE = IntStream.range(0, 85 * 50)
            .mapToObj(i -> validMessageAt(i % 85, i / 85))
            .collect(ImmutableList.toImmutableList());

    private static final List<List<Gf256.Element>> INVALID_MESSAGE = Collections.nCopies(85 * 50,
            ImmutableList.of(AES_FIELD.zero(), AES_FIELD.zero(), AES_FIELD.zero()));

    private static final List<List<Gf256.Element>> ENCODED_VALID_MESSAGE = GRID_PROOF.encode(VALID_MESSAGE);

    private static List<Gf256.Element> validMessageAt(int i, int j) {
        Gf256.Element x = RANDOM_MESSAGE_COLUMN.get(j);

        Gf256.Element y = x.equals(AES_FIELD.zero()) || j < 3
                ? AES_FIELD.one()
                : i == 0 ? x : i == 1 ? x.recip() : AES_FIELD.zero();

        return ImmutableList.of(y, AES_FIELD.zero(), AES_FIELD.zero());
    }

    // TODO: Consider adding 'GeneralAlgebraicConstraint' (along with a builder) to the production code, in place of this:
    private static <V, E> AlgebraicConstraint<V, E> algebraicConstraint(int degree, int length,
                                                                        FiniteVectorSpace<V, E> symbolSpace,
                                                                        Function<List<V>, List<V>> parityCheckFn) {
        return new AlgebraicConstraint<V, E>() {
            @Override
            public FiniteVectorSpace<V, E> symbolSpace() {
                return symbolSpace;
            }

            @Override
            public int degree() {
                return degree;
            }

            @Override
            public int length() {
                return length;
            }

            @Override
            public int redundancy() {
                return 1;
            }

            @Override
            public List<V> parityCheck(List<V> vector) {
                return parityCheckFn.apply(vector);
            }
        };
    }

    @Test
    void gridProofHasCorrectLocalTestProperties() {
//        assertEquals(171 * 151, GRID_PROOF.asLinearCode().distance());
        assertEquals(170 * 101, GRID_PROOF.localTest().distance());
        assertEquals(98.0 / 200, GRID_PROOF.localTest().falseNegativeProbability());
    }

    @Test
    void invalidMessageThrowsOnEncode() {
        assertThrows(IllegalArgumentException.class, () -> GRID_PROOF.encode(INVALID_MESSAGE));
    }

    @Test
    void validMessageExpandsToValidGridProof() {
        assumeTrue(GRID_PROOF.witnessConstraint().isValid(VALID_MESSAGE));
        assertTrue(GRID_PROOF.isValid(ENCODED_VALID_MESSAGE));
    }

    @Test
    void decodeIsLeftInverseOfEncode() {
        assertEquals(VALID_MESSAGE, GRID_PROOF.decode(ENCODED_VALID_MESSAGE));
    }

    @Test
    void validMessagePassesLocalTest() {
        assumeTrue(GRID_PROOF.witnessConstraint().isValid(VALID_MESSAGE));

        LocallyTestableCode.LocalTest.Evidence evidence = GRID_PROOF.localTest(0x1.0p-256)
                .query(ENCODED_VALID_MESSAGE, new Random(4567));

        assertFalse(evidence.isFailure());
    }
}
