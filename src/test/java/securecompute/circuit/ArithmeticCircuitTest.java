package securecompute.circuit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import securecompute.algebra.Gf256;
import securecompute.circuit.ArithmeticCircuit.Gate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static securecompute.circuit.cryptography.Aes.AES_FIELD;

public class ArithmeticCircuitTest {

    @Test
    void testTopologicalSortOfGates() {
        AlgebraicFunction<Gf256.Element> idFn = AlgebraicFunction.sumFn(AES_FIELD, 1);
        Gate<Gf256.Element> g0, g1, g2, g3, g4;

        ArithmeticCircuit<?> circuit = ArithmeticCircuit.builder(AES_FIELD)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(AES_FIELD, 1))
                .addGate(g3 = new Gate<>(idFn, "C"))
                .addGate(g2 = new Gate<>(idFn, "B"))
                .addGate(g1 = new Gate<>(idFn, "A"))
                .addGate(g4 = new ArithmeticCircuit.OutputPort<>(AES_FIELD, 1))
                //
                .addWire(g0, 0, g1, 0)
                .addWire(g1, 0, g2, 0)
                .addWire(g1, 0, g3, 0)
                .addWire(g2, 0, g3, 0)
                .addWire(g3, 0, g4, 0)
                //
                .build();

        assertDoesNotThrow(circuit::gatesInTopologicalOrder);
        assertEquals(ImmutableList.of(g0, g1, g2, g3, g4), circuit.gatesInTopologicalOrder());
    }

    // TODO: Consider adding a separate 'AlgebraicFunctionTest' class, containing this utility function:

    public static <E> void checkAlgebraicFunction(AlgebraicFunction<E> function) {
        List<List<E>> allTuples = Lists.cartesianProduct(Collections.nCopies(function.inputLength(),
                function.field().getElements().collect(ImmutableList.toImmutableList())));

        List<E> zeroSyndrome = Collections.nCopies(function.parityCheckTerms().size(), function.field().zero());

        for (List<E> inputVector : allTuples) {
            List<E> allSymbols = function.baseFn().apply(inputVector);

            assertEquals(inputVector, allSymbols.subList(0, function.inputLength()));
            assertEquals(zeroSyndrome, function.parityCheck(allSymbols), inputVector.toString());
        }
    }
}
