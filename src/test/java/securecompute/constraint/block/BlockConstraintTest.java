package securecompute.constraint.block;

import com.google.common.collect.ImmutableList;
import com.google.common.math.IntMath;
import org.junit.jupiter.api.Test;
import securecompute.constraint.Constraint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockConstraintTest {

    private static final Constraint<Integer> COLUMN_CONSTRAINT = new Constraint<Integer>() {
        @Override
        public boolean isValid(List<Integer> vector) {
            return !vector.contains(null) &&
                    vector.get(0) + IntMath.pow(vector.get(1), 2) + IntMath.pow(vector.get(2), 3) == 0;
        }

        @Override
        public int length() {
            return 3;
        }
    };

    private static final BlockConstraint<Integer> BLOCK_CONSTRAINT = new BlockConstraint<>(COLUMN_CONSTRAINT, 2);

    private static final List<List<Integer>> TEST_VECTOR_WITH_ERASURES = Arrays.asList(
            ImmutableList.of(1, 2),
            null,
            ImmutableList.of(3, 4)
    );

    @Test
    void streamLayersCanHandleVectorsWithErasures() {
        assertDoesNotThrow(() -> BlockConstraint.streamLayers(TEST_VECTOR_WITH_ERASURES, 2));

        List<List<Integer>> layers = BlockConstraint.streamLayers(TEST_VECTOR_WITH_ERASURES, 2)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(1, null, 3), layers.get(0));
        assertEquals(Arrays.asList(2, null, 4), layers.get(1));
    }

    @Test
    void isValidCanHandleVectorsWithErasures() {
        assertDoesNotThrow(() -> BLOCK_CONSTRAINT.isValid(TEST_VECTOR_WITH_ERASURES));
    }
}
