package securecompute.circuit.cryptography;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import securecompute.algebra.FiniteField;
import securecompute.algebra.Gf256;
import securecompute.algebra.polynomial.PolynomialExpression;
import securecompute.circuit.AlgebraicFunction;
import securecompute.circuit.ArithmeticCircuit;
import securecompute.circuit.ArithmeticCircuit.Gate;
import securecompute.circuit.ArithmeticCircuit.InputPort;
import securecompute.circuit.ArithmeticCircuit.OutputPort;

import java.util.Arrays;
import java.util.List;

import static securecompute.algebra.polynomial.BasePolynomialExpression.*;
import static securecompute.circuit.cryptography.Aes.*;

public class Groestl {
    // TODO: Consider memoising c(n) for each n & then remove this constant:
    private static final Constant<Gf256.Element> _0xff = c(0xff);

    // TODO: Make sure the second term will ultimately be elided - it depends linearly on the 1st & last (forward ref.):
    private static final byte[][] INCREMENT_BASE_FN_OUTPUT_TABLE = {
            {0x00, 1, 9, 0, 0, 0, 0x01},
            {0x01, 2, 6, 0, 0, 0, 0x02},
            {0x02, 1, 9, 1, 0, 0, 0x03},
            {0x03, 4, 6, 0, 0, 0, 0x04},
            {0x04, 1, 9, 0, 1, 0, 0x05},
            {0x05, 2, 6, 1, 0, 0, 0x06},
            {0x06, 1, 9, 1, 1, 0, 0x07},
            {0x07, 8, 9, 0, 0, 0, 0x08},
            {0x08, 1, 9, 0, 0, 1, 0x09},
            {0x09, 2, 6, 0, 1, 0, 0x0a},
            {0x0a, 1, 9, 1, 0, 1, 0x0b},
            {0x0b, 4, 6, 1, 0, 0, 0x0c},
            {0x0c, 1, 9, 0, 1, 1, 0x0d},
            {0x0d, 2, 6, 1, 1, 0, 0x0e},
            {0x0e, 1, 9, 1, 1, 1, 0x0f},
    };

    private static final List<List<Gf256.Element>> INCREMENT_BASE_FN_OUTPUTS = Arrays.stream(INCREMENT_BASE_FN_OUTPUT_TABLE)
            .map(b -> ImmutableList.of(elt(b[0]), elt(b[1]), elt(b[2]), elt(b[3]), elt(b[4]), elt(b[5]), elt(b[6])))
            .collect(ImmutableList.toImmutableList());

    private Groestl() {
    }

    private static Gf256.Element elt(long n) {
        return AES_FIELD.element(n);
    }

    private static AlgebraicFunction<Gf256.Element> subBytesFn() {
        return AlgebraicFunction.vectorFn(aesSBox(), 64);
    }

    private static AlgebraicFunction<Gf256.Element> subBytesWideFn() {
        return AlgebraicFunction.vectorFn(aesSBox(), 128);
    }

    private static AlgebraicFunction<Gf256.Element> mixColumnFn() {
        return AlgebraicFunction.builder(AES_FIELD)
                .degree(1)
                .inputLength(8)
                .parityCheckTerms(ImmutableList.of(
                        mixColumnCheckTerm(2, 2, 3, 4, 5, 3, 5, 7),
                        mixColumnCheckTerm(7, 2, 2, 3, 4, 5, 3, 5),
                        mixColumnCheckTerm(5, 7, 2, 2, 3, 4, 5, 3),
                        mixColumnCheckTerm(3, 5, 7, 2, 2, 3, 4, 5),
                        mixColumnCheckTerm(5, 3, 5, 7, 2, 2, 3, 4),
                        mixColumnCheckTerm(4, 5, 3, 5, 7, 2, 2, 3),
                        mixColumnCheckTerm(3, 4, 5, 3, 5, 7, 2, 2),
                        mixColumnCheckTerm(2, 3, 4, 5, 3, 5, 7, 2)
                ))
                .simpleBaseFn()
                .build();
    }

    private static AlgebraicFunction<Gf256.Element> mixBytesFn() {
        return AlgebraicFunction.vectorFn(mixColumnFn(), 8);
    }

    private static AlgebraicFunction<Gf256.Element> mixBytesWideFn() {
        return AlgebraicFunction.vectorFn(mixColumnFn(), 16);
    }

    private static List<PolynomialExpression<Gf256.Element>> addRoundConstantParityCheckTerms(boolean isP, int n) {
        // TODO: Consider placing 'roundVar' variable at the front of the input vector.
        // FIXME: This should be in column-major order, along with the round circuits below:
        Variable<Gf256.Element> roundVar = variable(n * 8);
        ImmutableList.Builder<PolynomialExpression<Gf256.Element>> builder = ImmutableList.builderWithExpectedSize(n * 8);
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < n; i++) {
                Variable<Gf256.Element> inVar = variable(j * n + i), outVar = variable((j + 8) * n + i + 1);
                if (isP) {
                    builder.add(j == 0
                            ? inVar.add(outVar).add(c(0x10L * i)).add(roundVar)
                            : inVar.add(outVar)
                    );
                } else {
                    builder.add(j == 7
                            ? inVar.add(outVar).add(c(0xff ^ 0x10L * i)).add(roundVar)
                            : inVar.add(outVar).add(_0xff)
                    );
                }
            }
        }
        return builder.build();
    }

    private static AlgebraicFunction<Gf256.Element> addRoundConstantFn(boolean isP) {
        return AlgebraicFunction.builder(AES_FIELD).degree(1).inputLength(65)
                .parityCheckTerms(addRoundConstantParityCheckTerms(isP, 8))
                .simpleBaseFn()
                .build();
    }

    private static AlgebraicFunction<Gf256.Element> addRoundConstantWideFn(boolean isP) {
        return AlgebraicFunction.builder(AES_FIELD).degree(1).inputLength(129)
                .parityCheckTerms(addRoundConstantParityCheckTerms(isP, 16))
                .simpleBaseFn()
                .build();
    }

    private static ArithmeticCircuit<Gf256.Element> groestlRoundCircuit(boolean isP) {
        Gate<Gf256.Element> g0, g1, g2, g3, g4;

        // NOTE: This takes the state bytes to be in row-major order; standard Groestl places them in column-major order.
        ArithmeticCircuit.Builder<Gf256.Element> builder = ArithmeticCircuit.builder(AES_FIELD)
                .maximumFanOut(1)
                .maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 65))
                .addGate(g1 = new Gate<>(addRoundConstantFn(isP), "AddRoundConstant"))
                .addGate(g2 = new Gate<>(subBytesFn(), "SubBytes"))
                .addGate(g3 = new Gate<>(mixBytesFn(), "MixBytes"))
                .addGate(g4 = new OutputPort<>(AES_FIELD, 64))
                // wire up inputs to AddRoundConstant gate:
                .addWires(g0, g1, 65)
                // wire up AddRoundConstant gate to SubBytes gate:
                .addWires(g1, g2, 64);

        // Wire up SubBytes gate to MixBytes gate, via an implicit ShiftBytes step:
        if (isP) {
            builder
                    .addWires(g2, g3, 8)
                    .addWires(g2, 0x08, g3, 0x0f, 1).addWires(g2, g3, 7)
                    .addWires(g2, 0x10, g3, 0x16, 2).addWires(g2, g3, 6)
                    .addWires(g2, 0x18, g3, 0x1d, 3).addWires(g2, g3, 5)
                    .addWires(g2, 0x20, g3, 0x24, 4).addWires(g2, g3, 4)
                    .addWires(g2, 0x28, g3, 0x2b, 5).addWires(g2, g3, 3)
                    .addWires(g2, 0x30, g3, 0x32, 6).addWires(g2, g3, 2)
                    .addWires(g2, 0x38, g3, 0x39, 7).addWires(g2, g3, 1);
        } else {
            builder
                    .addWires(g2, 0x00, g3, 0x07, 1).addWires(g2, g3, 7)
                    .addWires(g2, 0x08, g3, 0x0d, 3).addWires(g2, g3, 5)
                    .addWires(g2, 0x10, g3, 0x13, 5).addWires(g2, g3, 3)
                    .addWires(g2, 0x18, g3, 0x19, 7).addWires(g2, g3, 1)
                    .addWires(g2, g3, 8)
                    .addWires(g2, 0x28, g3, 0x2e, 2).addWires(g2, g3, 6)
                    .addWires(g2, 0x30, g3, 0x34, 4).addWires(g2, g3, 4)
                    .addWires(g2, 0x38, g3, 0x3a, 6).addWires(g2, g3, 2);
        }

        return builder
                // wire up MixBytes gate to outputs:
                .addWires(g3, g4, 64)
                .build();
    }

    private static ArithmeticCircuit<Gf256.Element> groestlRoundWideCircuit(boolean isP) {
        Gate<Gf256.Element> g0, g1, g2, g3, g4;

        // NOTE: This takes the state bytes to be in row-major order; standard Groestl places them in column-major order.
        ArithmeticCircuit.Builder<Gf256.Element> builder = ArithmeticCircuit.builder(AES_FIELD)
                .maximumFanOut(1)
                .maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 129))
                .addGate(g1 = new Gate<>(addRoundConstantWideFn(isP), "AddRoundConstantWide"))
                .addGate(g2 = new Gate<>(subBytesWideFn(), "SubBytesWide"))
                .addGate(g3 = new Gate<>(mixBytesWideFn(), "MixBytesWide"))
                .addGate(g4 = new OutputPort<>(AES_FIELD, 128))
                // wire up inputs to AddRoundConstantWide gate:
                .addWires(g0, g1, 129)
                // wire up AddRoundConstantWide gate to SubBytes gate:
                .addWires(g1, g2, 128);

        // Wire up SubBytesWide gate to MixBytesWide gate, via an implicit ShiftBytesWide step:
        if (isP) {
            builder
                    .addWires(g2, g3, 16)
                    .addWires(g2, 0x10, g3, 0x1f, 1).addWires(g2, g3, 15)
                    .addWires(g2, 0x20, g3, 0x2e, 2).addWires(g2, g3, 14)
                    .addWires(g2, 0x30, g3, 0x3d, 3).addWires(g2, g3, 13)
                    .addWires(g2, 0x40, g3, 0x4c, 4).addWires(g2, g3, 12)
                    .addWires(g2, 0x50, g3, 0x5b, 5).addWires(g2, g3, 11)
                    .addWires(g2, 0x60, g3, 0x6a, 6).addWires(g2, g3, 10)
                    .addWires(g2, 0x70, g3, 0x75, 11).addWires(g2, g3, 5);
        } else {
            builder
                    .addWires(g2, 0x00, g3, 0x0f, 1).addWires(g2, g3, 15)
                    .addWires(g2, 0x10, g3, 0x1d, 3).addWires(g2, g3, 13)
                    .addWires(g2, 0x20, g3, 0x2b, 5).addWires(g2, g3, 11)
                    .addWires(g2, 0x30, g3, 0x35, 11).addWires(g2, g3, 5)
                    .addWires(g2, g3, 16)
                    .addWires(g2, 0x50, g3, 0x5e, 2).addWires(g2, g3, 14)
                    .addWires(g2, 0x60, g3, 0x6c, 4).addWires(g2, g3, 12)
                    .addWires(g2, 0x70, g3, 0x7a, 6).addWires(g2, g3, 10);
        }

        return builder
                // wire up MixBytesWide gate to outputs:
                .addWires(g3, g4, 128)
                .build();
    }

    // visible for tests
    static AlgebraicFunction<Gf256.Element> incrementFn() {
        return AlgebraicFunction.builder(AES_FIELD).degree(2).inputLength(1).auxiliaryLength(5).outputLength(1)
                .baseFn(v -> INCREMENT_BASE_FN_OUTPUTS.get(v.get(0).getValue()))
                .parityCheckTerms(ImmutableList.of(
                        X0.add(X6).multiply(c(0x8c)).add(c(0x8d)).add(X1), // <- constants 0x3/0x2 & 1/0x2 resp.
                        X2.add(X1).multiply(X1).add(c(8)),
                        X2.add(c(6)).multiply(X2.add(c(9))),
                        X3.add(c(1)).multiply(X3),
                        X4.add(c(1)).multiply(X4),
                        X5.add(c(1)).multiply(X5),
                        X3.multiply(c(2)).add(X4.multiply(c(4))).add(X5.multiply(c(8))).add(c(1)).multiply(X1).add(X6)
                ))
                .build();
    }

    public static ArithmeticCircuit<Gf256.Element> evenMansourEncryptionRoundCircuit() {
        AlgebraicFunction<Gf256.Element> pRoundFn = groestlRoundCircuit(true).asFunction();
        AlgebraicFunction<Gf256.Element> qRoundFn = groestlRoundCircuit(false).asFunction();
        AlgebraicFunction<Gf256.Element> addKeyFn = AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 64);
        Gate<Gf256.Element> g0, g1, g2, g3, g4, g5, g6;

        return ArithmeticCircuit.builder(AES_FIELD).maximumFanOut(3).maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 129))
                .addGate(g1 = new Gate<>(addKeyFn, "PreWhiten"))
                .addGate(g2 = new Gate<>(qRoundFn, "Q_Round"))
                .addGate(g3 = new Gate<>(pRoundFn, "P_Round"))
                .addGate(g4 = new Gate<>(addKeyFn, "PostWhiten"))
                .addGate(g5 = new Gate<>(incrementFn(), "IncrementRoundNumber"))
                .addGate(g6 = new OutputPort<>(AES_FIELD, 129))
                // fan out round number input to Q_Round, P_Round & IncrementRoundNumber gates:
                .addWire(g0, 64, g2, 64)
                .addWire(g0, 64, g3, 64)
                .addWire(g0, 64, g5, 0)
                // wire up key & text inputs to PreWhiten (XOR) gate:
                .addWires(g0, g1, 64)
                .addWires(g0, g1, 64)
                // fan out key inputs to Q_Round gate:
                .addWires(g0, 0, g2, 0, 64)
                // wire up PreWhiten gate to P_Round gate:
                .addWires(g1, g3, 64)
                // wire up Q_Round & P_Round gates to PostWhiten (XOR) gate:
                .addWires(g2, g4, 64)
                .addWires(g3, g4, 64)
                // fan out Q_Round gate to key outputs:
                .addWires(g2, 0, g6, 0, 64)
                // wire up IncrementRoundNumber gate to round number output:
                .addWire(g5, g6)
                // wire up PostWhiten gate to text outputs:
                .addWires(g4, g6, 64)
                .build();
    }

    public static ArithmeticCircuit<Gf256.Element> evenMansourEncryptionRoundWideCircuit() {
        AlgebraicFunction<Gf256.Element> pRoundFn = groestlRoundWideCircuit(true).asFunction();
        AlgebraicFunction<Gf256.Element> qRoundFn = groestlRoundWideCircuit(false).asFunction();
        AlgebraicFunction<Gf256.Element> addKeyFn = AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 128);
        Gate<Gf256.Element> g0, g1, g2, g3, g4, g5, g6;

        // TODO: Similar to above method - consider de-duplicating some of gate wiring logic here:
        return ArithmeticCircuit.builder(AES_FIELD).maximumFanOut(3).maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 257))
                .addGate(g1 = new Gate<>(addKeyFn, "PreWhitenWide"))
                .addGate(g2 = new Gate<>(qRoundFn, "Q_RoundWide"))
                .addGate(g3 = new Gate<>(pRoundFn, "P_RoundWide"))
                .addGate(g4 = new Gate<>(addKeyFn, "PostWhitenWide"))
                .addGate(g5 = new Gate<>(incrementFn(), "IncrementRoundNumber"))
                .addGate(g6 = new OutputPort<>(AES_FIELD, 257))
                // fan out round number input to Q_RoundWide, P_RoundWide & IncrementRoundNumber gates:
                .addWire(g0, 128, g2, 128)
                .addWire(g0, 128, g3, 128)
                .addWire(g0, 128, g5, 0)
                // wire up key & text inputs to PreWhitenWide (XOR) gate:
                .addWires(g0, g1, 128)
                .addWires(g0, g1, 128)
                // fan out key inputs to Q_RoundWide gate:
                .addWires(g0, 0, g2, 0, 128)
                // wire up PreWhitenWide gate to P_RoundWide gate:
                .addWires(g1, g3, 128)
                // wire up Q_RoundWide & P_RoundWide gates to PostWhitenWide (XOR) gate:
                .addWires(g2, g4, 128)
                .addWires(g3, g4, 128)
                // fan out Q_RoundWide gate to key outputs:
                .addWires(g2, 0, g6, 0, 128)
                // wire up IncrementRoundNumber gate to round number output:
                .addWire(g5, g6)
                // wire up PostWhitenWide gate to text outputs:
                .addWires(g4, g6, 128)
                .build();
    }

    private static AlgebraicFunction<Gf256.Element> equalsConstantFn(Gf256.Element c) {
        return AlgebraicFunction.builder(AES_FIELD).degree(2).inputLength(1).auxiliaryLength(3).outputLength(1)
                .baseFn(v -> {
                    Gf256.Element x = v.get(0), y = x.add(c), z = y.recipOrZero(), w = y.multiply(z);
                    return ImmutableList.of(x, y, z, w, w.add(AES_FIELD.one()));
                })
                .parityCheckTerms(ImmutableList.of(
                        X0.add(constant(c)),
                        X1.multiply(X2).add(X3), X3.add(X4).add(c(1)), X3.multiply(X4), X1.add(X2).multiply(X4)
                ))
                .build();
    }

    static <E> AlgebraicFunction<E> checkZeroFn(FiniteField<E> field) {
        List<E> expectedInput = ImmutableList.of(field.zero());
        return AlgebraicFunction.builder(field).degree(1).inputLength(1).auxiliaryLength(0).outputLength(0)
                .baseFn(v -> {
                    Preconditions.checkArgument(expectedInput.equals(v), "disable flag must be cleared");
                    return expectedInput;
                })
                .parityCheckTerms(ImmutableList.of(variable(0)))
                .build();
    }

    public static ArithmeticCircuit<Gf256.Element> rawGroestlStepCircuit() {
        AlgebraicFunction<Gf256.Element> roundFn = evenMansourEncryptionRoundCircuit().asFunction();
        AlgebraicFunction<Gf256.Element> isLastFn = equalsConstantFn(AES_FIELD.element(0x0a));
        AlgebraicFunction<Gf256.Element> productFn = AlgebraicFunction.vectorFn(AlgebraicFunction.productFn(AES_FIELD, 2), 129);
        AlgebraicFunction<Gf256.Element> sumFn = AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 129);
        AlgebraicFunction<Gf256.Element> checkZeroFn = checkZeroFn(AES_FIELD);
        Gate<Gf256.Element> g0, g1, g2, g3, g4, g5, g6;

        return ArithmeticCircuit.builder(AES_FIELD).maximumFanOut(130).maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 194))
                .addGate(g1 = new Gate<>(roundFn, "EncryptionRound"))
                .addGate(g2 = new Gate<>(isLastFn, "IsLastRound"))
                .addGate(g3 = new Gate<>(productFn, "MaskState"))
                .addGate(g4 = new Gate<>(sumFn, "AddMaskedState"))
                .addGate(g5 = new Gate<>(checkZeroFn, "CheckZero"))
                .addGate(g6 = new OutputPort<>(AES_FIELD, 194))
                // wire up disable/done flag input to CheckZero gate:
                .addWire(g0, g5)
                // wire up message, round number & current state inputs to EncryptionRound gate:
                .addWires(g0, g1, 129)
                // wire up IsLastRound gate to disable/done flag output:
                .addWire(g2, g6)
                // wire up EncryptionRound gate to message outputs:
                .addWires(g1, g6, 64)
                // fan out EncryptionRound gate (@ new round number) to IsLastRound, MaskState & AddMaskedState gates:
                .addWire(g1, g2)
                .addWire(g1, 64, g3, 129)
                .addWire(g1, 64, g4, 0)
                // wire up EncryptionRound gate (@ new state), saved state inputs & MaskState gate to AddMaskedState gate:
                .addWires(g1, g4, 64)
                .addWires(g0, g4, 64)
                .addWires(g3, g4, 129)
                // fan out IsLastRound gate to (first half of) MaskState gate:
                .addWires(g2, 0, 0, g3, 0, 1, 129)
                // fan out (flipped) saved state inputs & EncryptionRound gate (@ new state) to MaskState gate:
                .addWires(g0, 130, g3, 130, 64)
                .addWires(g1, 65, g3, 194, 64)
                // wire up AddMaskedState gate to new & saved state outputs:
                .addWires(g4, g6, 129)
                .build();
    }

    public static ArithmeticCircuit<Gf256.Element> rawGroestlStepCircuitWide() {
        AlgebraicFunction<Gf256.Element> roundFn = evenMansourEncryptionRoundWideCircuit().asFunction();
        AlgebraicFunction<Gf256.Element> isLastFn = equalsConstantFn(AES_FIELD.element(0x0e));
        AlgebraicFunction<Gf256.Element> productFn = AlgebraicFunction.vectorFn(AlgebraicFunction.productFn(AES_FIELD, 2), 257);
        AlgebraicFunction<Gf256.Element> sumFn = AlgebraicFunction.vectorFn(AlgebraicFunction.sumFn(AES_FIELD, 2), 257);
        AlgebraicFunction<Gf256.Element> checkZeroFn = checkZeroFn(AES_FIELD);
        Gate<Gf256.Element> g0, g1, g2, g3, g4, g5, g6;

        // TODO: Similar to above method - consider de-duplicating some of gate wiring logic here:
        return ArithmeticCircuit.builder(AES_FIELD).maximumFanOut(258).maximumFanIn(1)
                .addGate(g0 = new InputPort<>(AES_FIELD, 386))
                .addGate(g1 = new Gate<>(roundFn, "EncryptionRoundWide"))
                .addGate(g2 = new Gate<>(isLastFn, "IsLastRound"))
                .addGate(g3 = new Gate<>(productFn, "MaskStateWide"))
                .addGate(g4 = new Gate<>(sumFn, "AddMaskedStateWide"))
                .addGate(g5 = new Gate<>(checkZeroFn, "CheckZero"))
                .addGate(g6 = new OutputPort<>(AES_FIELD, 386))
                // wire up disable/done flag input to CheckZero gate:
                .addWire(g0, g5)
                // wire up message, round number & current state inputs to EncryptionRoundWide gate:
                .addWires(g0, g1, 257)
                // wire up IsLastRound gate to disable/done flag output:
                .addWire(g2, g6)
                // wire up EncryptionRoundWide gate to message outputs:
                .addWires(g1, g6, 128)
                // fan out EncryptionRoundWide gate (@ new round number) to IsLastRound, MaskStateWide & AddMaskedStateWide gates:
                .addWire(g1, g2)
                .addWire(g1, 128, g3, 257)
                .addWire(g1, 128, g4, 0)
                // wire up EncryptionRound gate (@ new state), saved state inputs & MaskState gate to AddMaskedState gate:
                .addWires(g1, g4, 128)
                .addWires(g0, g4, 128)
                .addWires(g3, g4, 257)
                // fan out IsLastRound gate to (first half of) MaskState gate:
                .addWires(g2, 0, 0, g3, 0, 1, 257)
                // fan out (flipped) saved state inputs & EncryptionRound gate (@ new state) to MaskState gate:
                .addWires(g0, 258, g3, 258, 128)
                .addWires(g1, 129, g3, 386, 128)
                // wire up AddMaskedState gate to new & saved state outputs:
                .addWires(g4, g6, 257)
                .build();
    }
}
