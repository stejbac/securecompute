package securecompute.circuit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;
import securecompute.algebra.Gf256;
import securecompute.algebra.polynomial.BasePolynomialExpression;
import securecompute.algebra.polynomial.BasePolynomialExpression.Constant;
import securecompute.algebra.polynomial.BasePolynomialExpression.Variable;
import securecompute.algebra.polynomial.PolynomialExpression;
import securecompute.circuit.ArithmeticCircuit.Gate;
import securecompute.circuit.ArithmeticCircuit.InputPort;
import securecompute.circuit.ArithmeticCircuit.OutputPort;
import securecompute.helper.WithDefaultField;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static securecompute.algebra.polynomial.BasePolynomialExpression.constant;
import static securecompute.algebra.polynomial.BasePolynomialExpression.variable;

class ArithmeticCircuitTest implements WithDefaultField<Gf256.Element> {

    private static final Variable<Gf256.Element>
            X0 = variable(0), X1 = variable(1), X2 = variable(2), X3 = variable(3),
            X4 = variable(4), X5 = variable(5), X6 = variable(6), X7 = variable(7);

    private static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);

    private static final List<Gf256.Element> AFFINE_TRANSFORM_MATRIX_COLUMNS = LongStream.of(
            0b00011111,
            0b00111110,
            0b01111100,
            0b11111000,
            0b11110001,
            0b11100011,
            0b11000111,
            0b10001111
    ).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList());

    private static final Gf256.Element AFFINE_TRANSFORM_OFFSET = AES_FIELD.element(0b01100011); // 0x63

    private static final List<Gf256.Element> FROBENIUS_MAP_MULTIPLIERS = LongStream.of(
            0b00101001, // 0x29
            0b10110000, // 0xb0
            0b01011000, // 0x58
            0b00000101, // 0x05
            0b10100110, // 0xa6
            0b01010011, // 0x53
            0b10100100, // 0xa4
            0b01010010  // 0x52
    ).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList());

    private static final List<Gf256.Element> ALL_FIELD_ELEMENTS = LongStream.range(0, 256)
            .mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList());

    @Override
    public Gf256 getDefaultStructure() {
        return AES_FIELD;
    }

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

    @Test
    void testCircuit() {
        AlgebraicFunction<Gf256.Element> reciprocalOrZeroFn = AlgebraicFunction.builder(AES_FIELD)
                .degree(2)
                .inputLength(1)
                .auxiliaryLength(1)
                .outputLength(1)
                .baseFn(v -> ImmutableList.of(
                        v.get(0),
                        v.get(0).equals(zero()) ? one() : zero(),
                        v.get(0).recipOrZero()
                ))
                .parityCheckTerms(ImmutableList.of(
                        X0.multiply(X2).add(X1).add(constant(one())),
                        X1.multiply(X1.add(constant(one()))),
                        X0.add(X2).multiply(X1)
                ))
                .build();

        AlgebraicFunction<Gf256.Element> affineTransformFn = AlgebraicFunction.builder(AES_FIELD)
                .degree(2)
                .inputLength(1)
                .auxiliaryLength(6)
                .outputLength(1)
                .baseFn(v -> ImmutableList.of(
                        v.get(0),
                        v.get(0).pow(2),
                        v.get(0).pow(4),
                        v.get(0).pow(8),
                        v.get(0).pow(16),
                        v.get(0).pow(32),
                        v.get(0).pow(64),
                        affineTransform(v.get(0))
                ))
                .parityCheckTerms(ImmutableList.of(
                        X0.multiply(X0).add(X1),
                        X1.multiply(X1).add(X2),
                        X2.multiply(X2).add(X3),
                        X3.multiply(X3).add(X4),
                        X4.multiply(X4).add(X5),
                        X5.multiply(X5).add(X6),
                        c(0x63).add(
                                scale(0x05, X0)).add(
                                scale(0x09, X1)).add(
                                scale(0xf9, X2)).add(
                                scale(0x25, X3)).add(
                                scale(0xf4, X4)).add(
                                scale(0x01, X5)).add(
                                scale(0xb5, X6)).add(
                                scale(0x8f, X6).multiply(X6)
                        ).add(X7)
                ))
                .build();

        Gate<Gf256.Element> g0, g1, g2, g3, g4;

        ArithmeticCircuit<Gf256.Element> aesSBoxCircuit = ArithmeticCircuit.builder(AES_FIELD)
                .addGate(g0 = new InputPort<>(AES_FIELD, 1))
                .addGate(g1 = new Gate<>(reciprocalOrZeroFn))
                .addGate(g2 = new Gate<>(affineTransformFn))
                .addGate(g3 = new OutputPort<>(AES_FIELD, 1))
                .addWire(g0, g1)
                .addWire(g1, g2)
                .addWire(g2, g3)
                .build();

        System.out.println(aesSBoxCircuit);
        System.out.println(aesSBoxCircuit.gatesInTopologicalOrder());

        AlgebraicFunction<Gf256.Element> aesSBox = aesSBoxCircuit.asFunction();

        System.out.println(aesSBox);
        System.out.println(aesSBox.apply(ImmutableList.of(zero())));
        System.out.println(aesSBox.apply(ImmutableList.of(one())));
        System.out.println(aesSBox.apply(ImmutableList.of(AES_FIELD.element(2))));

        checkAlgebraicFunction(reciprocalOrZeroFn);
        checkAlgebraicFunction(affineTransformFn);
        checkAlgebraicFunction(aesSBox);

        AlgebraicFunction<Gf256.Element> subBytesFn = AlgebraicFunction.vectorFn(aesSBox, 16);

        AlgebraicFunction<Gf256.Element> mixColumnFn = AlgebraicFunction.builder(AES_FIELD)
                .degree(1)
                .inputLength(4)
                .parityCheckTerms(ImmutableList.of(
                        scale(2, X0).add(scale(3, X1)).add(scale(1, X2)).add(scale(1, X3)).add(X4).add(X5).add(X6).add(X7),
                        scale(1, X0).add(scale(2, X1)).add(scale(3, X2)).add(scale(1, X3)).add(X4).add(X5).add(X6).add(X7),
                        scale(1, X0).add(scale(1, X1)).add(scale(2, X2)).add(scale(3, X3)).add(X4).add(X5).add(X6).add(X7),
                        scale(3, X0).add(scale(1, X1)).add(scale(1, X2)).add(scale(2, X3)).add(X4).add(X5).add(X6).add(X7)
                ))
                .simpleBaseFn()
                .build();

        AlgebraicFunction<Gf256.Element> mixColumnsFn = AlgebraicFunction.vectorFn(mixColumnFn, 4);

        AlgebraicFunction<Gf256.Element> addRoundKeyFn = AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 16);

        // NOTE: This takes the state bytes to be in row-major order; standard AES places them in column-major order.
        ArithmeticCircuit<Gf256.Element> aesRoundCircuit = ArithmeticCircuit.builder(AES_FIELD)
                .maximumFanOut(1)
                .maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 32))
                .addGate(g1 = new Gate<>(subBytesFn, "SubBytes"))
                .addGate(g2 = new Gate<>(mixColumnsFn, "MixColumns"))
                .addGate(g3 = new Gate<>(addRoundKeyFn, "AddRoundKey"))
                .addGate(g4 = new OutputPort<>(AES_FIELD, 16))
                // wire up inputs to SubBytes gate:
                .addWires(g0, g1, 16)
                // wire up SubBytes gate to MixColumns gate, via an implicit ShiftRows step:
                .addWires(g1, g2, 4)
                .addWire(g1, 4, g2, 7).addWires(g1, g2, 3)
                .addWires(g1, 8, g2, 10, 2).addWires(g1, g2, 2)
                .addWire(g1, 15, g2, 12).addWires(g1, g2, 3)
                // wire up MixColumns gate to AddRoundKey gate:
                .addWires(g2, g3, 16)
                // wire up remaining inputs to AddRoundKey gate:
                .addWires(g0, g3, 16)
                // wire up AddRoundKey gate to outputs:
                .addWires(g3, g4, 16)
                .build();

        System.out.println(aesRoundCircuit);

        AlgebraicFunction<Gf256.Element> aesRoundFn = aesRoundCircuit.asFunction();

        System.out.println(mixColumnFn.apply(
                LongStream.of(0xdb, 0x13, 0x53, 0x45).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList())));
        System.out.println(aesRoundFn.apply(Collections.nCopies(32, zero())));
    }

    private Constant<Gf256.Element> c(long n) {
        return constant(AES_FIELD.element(n));
    }

    private BasePolynomialExpression<Gf256.Element> scale(long n, PolynomialExpression<Gf256.Element> p) {
        return c(n).multiply(p);
    }

    private Gf256.Element affineTransform(Gf256.Element x) {
        byte acc = AFFINE_TRANSFORM_OFFSET.getValue();
        for (int i = 0; i < 8; i++) {
            acc ^= (x.getValue() & 1 << i) != 0 ? AFFINE_TRANSFORM_MATRIX_COLUMNS.get(i).getValue() : 0;
        }
        return AES_FIELD.element(acc);
    }

    @Test
    void testFrobenius() {
        for (Gf256.Element c : FROBENIUS_MAP_MULTIPLIERS) {
            for (int i = 0; i < 8; i++) {
                Gf256.Element x = AES_FIELD.element(1 << i).multiply(c);
                Gf256.Element y = x.add(x.pow(2)).add(x.pow(4)).add(x.pow(8))
                        .add(x.pow(16)).add(x.pow(32)).add(x.pow(64)).add(x.pow(128));
                System.out.println(x + " -> " + y);
            }
            System.out.println();
        }
    }

    @Test
    void testLinearTransform() {
        List<Gf256.Element> coefficients = IntStream.range(0, 8)
                .mapToObj(i -> sum(
                        Streams.zip(AFFINE_TRANSFORM_MATRIX_COLUMNS.stream(), FROBENIUS_MAP_MULTIPLIERS.stream(),
                                (a, b) -> a.multiply(b.pow(1 << i)))))
                .collect(ImmutableList.toImmutableList());

        System.out.println(coefficients);

        for (int i = 0; i < 8; i++) {
            Gf256.Element x = AES_FIELD.element(1 << i);
            Gf256.Element y = zero();
            for (int j = 0; j < 8; j++) {
                y = y.add(coefficients.get(j).multiply(x.pow(1 << j)));
            }
            System.out.println(x + " -> " + y);
        }
    }

    // TODO: Consider adding a separate 'AlgebraicFunctionTest' class, containing this utility function:

    private void checkAlgebraicFunction(AlgebraicFunction<Gf256.Element> function) {
        List<List<Gf256.Element>> allTuples = Lists.cartesianProduct(
                Collections.nCopies(function.inputLength(), ALL_FIELD_ELEMENTS));

        for (List<Gf256.Element> inputVector : allTuples) {
            List<Gf256.Element> allSymbols = function.baseFn().apply(inputVector);
            List<Gf256.Element> zeroSyndrome = Collections.nCopies(function.parityCheckTerms().size(), zero());

            assertEquals(inputVector, allSymbols.subList(0, function.inputLength()));
            assertEquals(zeroSyndrome, function.parityCheck(allSymbols));
        }
    }
}
