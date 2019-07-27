package securecompute.circuit.cryptography;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import securecompute.algebra.Gf256;
import securecompute.algebra.polynomial.BasePolynomialExpression;
import securecompute.algebra.polynomial.PolynomialExpression;
import securecompute.circuit.AlgebraicFunction;
import securecompute.circuit.ArithmeticCircuit;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static securecompute.algebra.polynomial.BasePolynomialExpression.*;

public class Aes {

    private Aes() {
    }

    public static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);

    private static final BasePolynomialExpression.Variable<Gf256.Element>
            X0 = variable(0), X1 = variable(1), X2 = variable(2), X3 = variable(3),
            X4 = variable(4), X5 = variable(5), X6 = variable(6), X7 = variable(7);

    private static BasePolynomialExpression.Constant<Gf256.Element> c(long n) {
        return constant(AES_FIELD.element(n));
    }

    private static BasePolynomialExpression<Gf256.Element> scale(long n, PolynomialExpression<Gf256.Element> p) {
        return c(n).multiply(p);
    }

    private static Gf256.Element affineTransform(Gf256.Element elt) {
        int n = elt.getValue() & 0xff;
        return AES_FIELD.element(0x63 ^ n ^ ((n ^= n << 8) << 1 ^ n << 2 ^ n << 3 ^ n << 4) >> 8);
    }

    // FIXME: The functions below should be memoised:

    // visible for tests
    static AlgebraicFunction<Gf256.Element> reciprocalOrZeroFn() {
        return AlgebraicFunction.builder(AES_FIELD)
                .degree(2)
                .inputLength(1)
                .auxiliaryLength(1)
                .outputLength(1)
                .baseFn(v -> ImmutableList.of(
                        v.get(0),
                        v.get(0).equals(AES_FIELD.zero()) ? AES_FIELD.one() : AES_FIELD.zero(),
                        v.get(0).recipOrZero()
                ))
                .parityCheckTerms(ImmutableList.of(
                        X0.multiply(X2).add(X1).add(constant(AES_FIELD.one())),
                        X1.multiply(X1.add(constant(AES_FIELD.one()))),
                        X0.add(X2).multiply(X1)
                ))
                .build();
    }

    // visible for tests
    static AlgebraicFunction<Gf256.Element> affineTransformFn() {
        return AlgebraicFunction.builder(AES_FIELD)
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
    }

    // visible for tests
    static ArithmeticCircuit<Gf256.Element> aesSBoxCircuit() {
        ArithmeticCircuit.Gate<Gf256.Element> g0, g1, g2, g3;

        return ArithmeticCircuit.builder(AES_FIELD)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(AES_FIELD, 1))
                .addGate(g1 = new ArithmeticCircuit.Gate<>(reciprocalOrZeroFn()))
                .addGate(g2 = new ArithmeticCircuit.Gate<>(affineTransformFn()))
                .addGate(g3 = new ArithmeticCircuit.OutputPort<>(AES_FIELD, 1))
                .addWire(g0, g1)
                .addWire(g1, g2)
                .addWire(g2, g3)
                .build();
    }

    // visible for tests
    static AlgebraicFunction<Gf256.Element> aesSBox() {
        return aesSBoxCircuit().asFunction();
    }

    private static PolynomialExpression<Gf256.Element> mixColumnCheckTerm(long... coefficients) {
        Stream<PolynomialExpression<Gf256.Element>> inputTerms = Streams.mapWithIndex(Arrays.stream(coefficients),
                (n, i) -> scale(n, variable((int) i)));

        Stream<PolynomialExpression<Gf256.Element>> outputTerms = IntStream.range(coefficients.length, coefficients.length * 2)
                .mapToObj(BasePolynomialExpression::variable);

        return sum(Stream.concat(inputTerms, outputTerms).collect(ImmutableList.toImmutableList()));
    }

    // visible for tests
    static AlgebraicFunction<Gf256.Element> mixColumnFn() {
        return AlgebraicFunction.builder(AES_FIELD)
                .degree(1)
                .inputLength(4)
                .parityCheckTerms(ImmutableList.of(
                        mixColumnCheckTerm(2, 3, 1, 1),
                        mixColumnCheckTerm(1, 2, 3, 1),
                        mixColumnCheckTerm(1, 1, 2, 3),
                        mixColumnCheckTerm(3, 1, 1, 2)
                ))
                .simpleBaseFn()
                .build();
    }

    private static AlgebraicFunction<Gf256.Element> subBytesFn() {
        return AlgebraicFunction.vectorFn(aesSBox(), 16);
    }

    private static AlgebraicFunction<Gf256.Element> mixColumnsFn() {
        return AlgebraicFunction.vectorFn(mixColumnFn(), 4);
    }

    private static AlgebraicFunction<Gf256.Element> addRoundKeyFn() {
        return AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 16);
    }

    public static ArithmeticCircuit<Gf256.Element> aesRoundCircuit() {
        ArithmeticCircuit.Gate<Gf256.Element> g0, g1, g2, g3, g4;

        // NOTE: This takes the state bytes to be in row-major order; standard AES places them in column-major order.
        return ArithmeticCircuit.builder(AES_FIELD)
                .maximumFanOut(1)
                .maximumFanIn(1)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(AES_FIELD, 32))
                .addGate(g1 = new ArithmeticCircuit.Gate<>(subBytesFn(), "SubBytes"))
                .addGate(g2 = new ArithmeticCircuit.Gate<>(mixColumnsFn(), "MixColumns"))
                .addGate(g3 = new ArithmeticCircuit.Gate<>(addRoundKeyFn(), "AddRoundKey"))
                .addGate(g4 = new ArithmeticCircuit.OutputPort<>(AES_FIELD, 16))
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
    }

    public static AlgebraicFunction<Gf256.Element> aesRoundFn() {
        return aesRoundCircuit().asFunction();
    }
}
