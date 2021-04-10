package securecompute.circuit.cryptography;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Booleans;
import securecompute.algebra.BooleanField;
import securecompute.algebra.polynomial.BasePolynomialExpression;
import securecompute.algebra.polynomial.PolynomialExpression;
import securecompute.circuit.ArithmeticCircuit;
import securecompute.circuit.BooleanFunction;

import java.util.List;
import java.util.stream.IntStream;

import static securecompute.algebra.polynomial.BasePolynomialExpression.*;
import static securecompute.circuit.cryptography.Groestl.checkZeroFn;

public class Sha2 {
    private static final BasePolynomialExpression.Variable<Boolean>
            X0 = variable(0), X1 = variable(1), X2 = variable(2), X3 = variable(3);

    private static final int[] SHA256_ROUND_CONSTS = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private static final long[] SHA512_ROUND_CONSTS = {
            0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL, 0x3956c25bf348b538L,
            0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L, 0xd807aa98a3030242L, 0x12835b0145706fbeL,
            0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L, 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L,
            0xc19bf174cf692694L, 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
            0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L, 0x983e5152ee66dfabL,
            0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L, 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L,
            0x06ca6351e003826fL, 0x142929670a0e6e70L, 0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL,
            0x53380d139d95b3dfL, 0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L, 0x92722c851482353bL,
            0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L, 0xd192e819d6ef5218L,
            0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L, 0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L,
            0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L, 0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L,
            0x682e6ff3d6b2b8a3L, 0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
            0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L, 0xc67178f2e372532bL, 0xca273eceea26619cL,
            0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L, 0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L,
            0x113f9804bef90daeL, 0x1b710b35131c471bL, 0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL,
            0x431d67c49c100d4cL, 0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L
    };

    private Sha2() {
    }

    // visible for tests
    static BooleanFunction sumFn(int n, int nGroup) {
        return BooleanFunction.builder().degree(2).inputLength(n * 2).auxiliaryLength(n).outputLength(n)
                .parityCheckTerms(sumParityCheckTerms(n, nGroup))
                .baseFn(v -> {
                    boolean[] result = new boolean[n * 4];
                    boolean a, b, c = false;
                    for (int i = 1; i <= n; i++) {
                        result[n - i] = a = v.get(n - i);
                        result[2 * n - i] = b = v.get(2 * n - i);
                        result[2 * n + i - 1] = c;
                        result[4 * n - i] = a ^ b ^ c;
                        c = i % nGroup != 0 && (a ^ c) & (b ^ c) ^ c;
                    }
                    return ImmutableList.copyOf(Booleans.asList(result));
                })
                .build();
    }

    private static List<PolynomialExpression<Boolean>> sumParityCheckTerms(int n, int nGroup) {
        ImmutableList.Builder<PolynomialExpression<Boolean>> builder = ImmutableList.builderWithExpectedSize(2 * n);
        BasePolynomialExpression<Boolean> c = constant(false);
        for (int i = 1; i <= n; i++) {
            BasePolynomialExpression<Boolean> a, b, d;
            a = variable(n - i);
            b = variable(2 * n - i);
            d = variable(2 * n + i - 1);
            builder.add(c.add(d));
            c = i % nGroup != 0 ? a.add(d).multiply(b.add(d)).add(d) : constant(false);
        }
        for (int i = 1; i <= n; i++) {
            BasePolynomialExpression<Boolean> a, b, d;
            a = variable(n - i);
            b = variable(2 * n - i);
            c = variable(2 * n + i - 1);
            d = variable(4 * n - i);
            builder.add(a.add(b).add(c).add(d));
        }
        return builder.build();
    }

    private static BooleanFunction choiceFn() {
        return BooleanFunction.builder().degree(2).inputLength(3).auxiliaryLength(0).outputLength(1)
                .parityCheckTerms(ImmutableList.of(X1.add(X2).multiply(X0).add(X2).add(X3)))
                .baseFn(v -> ImmutableList.of(v.get(0), v.get(1), v.get(2),
                        v.get(0) ? v.get(1) : v.get(2)))
                .build();
    }

    private static BooleanFunction majorityFn() {
        return BooleanFunction.builder().degree(2).inputLength(3).auxiliaryLength(0).outputLength(1)
                .parityCheckTerms(ImmutableList.of(X0.add(X1).multiply(X0.add(X2)).add(X0).add(X3)))
                .baseFn(v -> ImmutableList.of(v.get(0), v.get(1), v.get(2),
                        v.get(0) ? v.get(1) || v.get(2) : v.get(1) && v.get(2)))
                .build();
    }

    private static BooleanFunction sigmaDeltaFn(int n, int i, int j, int k) {
        return BooleanFunction.builder().degree(1).inputLength(n)
                .parityCheckTerms(sigmaDeltaParityCheckTerms(n, i, j, k))
                .simpleBaseFn()
                .build();
    }

    private static List<PolynomialExpression<Boolean>> sigmaDeltaParityCheckTerms(int n, int i, int j, int k) {
        ImmutableList.Builder<PolynomialExpression<Boolean>> builder = ImmutableList.builderWithExpectedSize(n);
        for (int l = n; l < 2 * n; l++) {
            BasePolynomialExpression<Boolean> x, y, z, sum;
            x = variable((l - i) % n);
            y = variable((l - j) % n);
            z = variable((l - k) % n);
            sum = l < k ? x.add(y) : x.add(y).add(z);
            builder.add(sum.add(variable(l)));
        }
        return builder.build();
    }

    private static BooleanFunction roundConstFn(int n, long[] roundConsts) {
        return BooleanFunction.builder().degree(1).inputLength(roundConsts.length)
                .parityCheckTerms(roundConstParityCheckTerms(n, roundConsts))
                .simpleBaseFn()
                .build();
    }

    private static List<PolynomialExpression<Boolean>> roundConstParityCheckTerms(int n, long[] roundConsts) {
        ImmutableList.Builder<PolynomialExpression<Boolean>> builder = ImmutableList.builderWithExpectedSize(n);
        for (int i = 0; i < n; i++) {
            long mask = 1L << (n - i - 1);
            BasePolynomialExpression<Boolean> sum = sum(IntStream.range(0, roundConsts.length)
                    .filter(j -> (roundConsts[j] & mask) != 0)
                    .mapToObj(BasePolynomialExpression::<Boolean>variable)
                    .collect(ImmutableList.toImmutableList()));
            builder.add(sum.add(variable(roundConsts.length + i)));
        }
        return builder.build();
    }

    // visible for tests
    static ArithmeticCircuit<Boolean> roundCircuit(boolean isWide) {
        int n = isWide ? 64 : 32;
        BooleanFunction sumFn = sumFn(n, n);
        BooleanFunction choiceFn = (BooleanFunction) BooleanFunction.vectorFn(choiceFn(), n);
        BooleanFunction majorityFn = (BooleanFunction) BooleanFunction.vectorFn(majorityFn(), n);
        BooleanFunction sigma0Fn = isWide ? sigmaDeltaFn(n, 28, 34, 39) : sigmaDeltaFn(n, 2, 13, 22);
        BooleanFunction sigma1Fn = isWide ? sigmaDeltaFn(n, 14, 18, 41) : sigmaDeltaFn(n, 6, 11, 25);
        ArithmeticCircuit.Gate<Boolean> g0, g1, g2, g3, g4, g5, g6, g7, g8, g9, g10, g11;

        return ArithmeticCircuit.builder(BooleanField.INSTANCE).maximumFanOut(3).maximumFanIn(1)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(BooleanField.INSTANCE, n * 9))
                .addGate(g1 = new ArithmeticCircuit.Gate<>(sumFn, "AddRoundKey"))
                .addGate(g2 = new ArithmeticCircuit.Gate<>(choiceFn, "Choice"))
                .addGate(g3 = new ArithmeticCircuit.Gate<>(sigma1Fn, "Sigma1"))
                .addGate(g4 = new ArithmeticCircuit.Gate<>(majorityFn, "Majority"))
                .addGate(g5 = new ArithmeticCircuit.Gate<>(sigma0Fn, "Sigma0"))
                .addGate(g6 = new ArithmeticCircuit.Gate<>(sumFn, "AddChoiceResult"))
                .addGate(g7 = new ArithmeticCircuit.Gate<>(sumFn, "AddSigma1Result"))
                .addGate(g8 = new ArithmeticCircuit.Gate<>(sumFn, "AddPartialSum"))
                .addGate(g9 = new ArithmeticCircuit.Gate<>(sumFn, "AddMajorityResult"))
                .addGate(g10 = new ArithmeticCircuit.Gate<>(sumFn, "AddSigma0Result"))
                .addGate(g11 = new ArithmeticCircuit.OutputPort<>(BooleanField.INSTANCE, n * 8))
                // wire up round key & 8th state word inputs to AddRoundKey gate:
                .addWires(g0, g1, n)
                .addWires(g0, n * 8, g1, n, n)
                // wire up 5th, 6th & 7th state word inputs to Choice gate:
                .addWires(g0, n * 5, g2, 0, n * 3)
                // fan out 5th state word input to Sigma1 gate:
                .addWires(g0, n * 5, g3, 0, n)
                // wire up 1st, 2nd & 3rd state word inputs to Majority gate:
                .addWires(g0, n, g4, 0, n * 3)
                // fan out 1st state word input to Sigma0 gate:
                .addWires(g0, n, g5, 0, n)
                // wire up AddRoundKey & Choice gates to AddChoiceResult gate:
                .addWires(g1, g6, n)
                .addWires(g2, g6, n)
                // wire up Sigma1 & AddChoiceResult gates to AddSigma1Result gate:
                .addWires(g3, g7, n)
                .addWires(g6, g7, n)
                // wire up 4th state word input & AddSigma1Result gates to AddPartialSum gate:
                .addWires(g0, g8, n)
                .addWires(g7, g8, n)
                // wire up Majority gate & fan out AddSigma1Result gate to AddMajorityResult gate:
                .addWires(g4, g9, n)
                .addWires(g7, 0, g9, n, n)
                // wire up Sigma0 & AddMajorityResult gates to AddSigma0Result gate:
                .addWires(g5, g10, n)
                .addWires(g9, g10, n)
                // wire up AddSigma0Result gate to 1st state word output:
                .addWires(g10, g11, n)
                // fan out 1st, 2nd & 3rd state word inputs to 2nd, 3rd & 4th state word outputs respectively:
                .addWires(g0, n, g11, n, n * 3)
                // wire up AddPartialSum gate to 5th state word output:
                .addWires(g8, g11, n)
                // fan out 5th, 6th & 7th state word inputs to 6th, 7th & 8th state word outputs respectively:
                .addWires(g0, n * 5, g11, n * 5, n * 3)
                .build();
    }

    // visible for tests
    static ArithmeticCircuit<Boolean> keyScheduleCircuit(boolean isWide) {
        int n = isWide ? 64 : 32;
        long[] roundConsts = isWide ? SHA512_ROUND_CONSTS : IntStream.of(SHA256_ROUND_CONSTS).asLongStream().toArray();
        BooleanFunction sumFn = sumFn(n, n);
        BooleanFunction delta0Fn = isWide ? sigmaDeltaFn(n, 1, 8, 7 + n) : sigmaDeltaFn(n, 7, 18, 3 + n);
        BooleanFunction delta1Fn = isWide ? sigmaDeltaFn(n, 19, 61, 6 + n) : sigmaDeltaFn(n, 17, 19, 10 + n);
        BooleanFunction roundConstFn = roundConstFn(n, roundConsts);
        ArithmeticCircuit.Gate<Boolean> g0, g1, g2, g3, g4, g5, g6, g7, g8;

        return ArithmeticCircuit.builder(BooleanField.INSTANCE).maximumFanOut(2).maximumFanIn(1)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(BooleanField.INSTANCE, n * 16 + roundConsts.length))
                .addGate(g1 = new ArithmeticCircuit.Gate<>(delta0Fn, "Delta0"))
                .addGate(g2 = new ArithmeticCircuit.Gate<>(delta1Fn, "Delta1"))
                .addGate(g3 = new ArithmeticCircuit.Gate<>(sumFn, "AddDelta0Result"))
                .addGate(g4 = new ArithmeticCircuit.Gate<>(sumFn, "AddMiddleWord"))
                .addGate(g5 = new ArithmeticCircuit.Gate<>(sumFn, "AddDelta1Result"))
                .addGate(g6 = new ArithmeticCircuit.Gate<>(roundConstFn, "RoundConst"))
                .addGate(g7 = new ArithmeticCircuit.Gate<>(sumFn, "AddRoundConst"))
                .addGate(g8 = new ArithmeticCircuit.OutputPort<>(BooleanField.INSTANCE, n * 17 + roundConsts.length))
                // wire up 1st key word input & Delta0 gate to AddDelta0Result gate:
                .addWires(g0, g3, n)
                .addWires(g1, g3, n)
                // wire up 2nd - 16th key word inputs & AddDelta1Result gate to 1st - 16th outputs, in respective order:
                .addWires(g0, g8, n * 15)
                .addWires(g5, g8, n)
                // fan out 2nd key word input to Delta0 gate:
                .addWires(g0, n, g1, 0, n)
                // fan out 15th key word input to Delta1 gate:
                .addWires(g0, n * 14, g2, 0, n)
                // fan out 10th key word input & wire up AddDelta0Result gate to AddMiddleWord gate:
                .addWires(g0, n * 9, g4, n, n)
                .addWires(g3, g4, n)
                // wire up Delta1 gate & AddMiddleWord gate to AddDelta1Result gate:
                .addWires(g2, g5, n)
                .addWires(g4, g5, n)
                // fan out 1st key word input & wire up RoundConst gate to AddRoundConst gate:
                .addWires(g0, 0, g7, 0, n)
                .addWires(g6, g7, n)
                // wire up round constant selector input to round constant selector output, rotated right one bit:
                .addWire(g0, n * 16 + roundConsts.length - 1, g8, n * 16)
                .addWires(g0, g8, roundConsts.length - 1)
                // fan out round constant selector input to RoundConst gate:
                .addWires(g0, n * 16, g6, 0, roundConsts.length)
                // wire up AddRoundConst gate to round key output:
                .addWires(g7, g8, n)
                .build();
    }

    public static ArithmeticCircuit<Boolean> rawSha2StepCircuit(boolean isWide) {
        int n = isWide ? 64 : 32;
        int numRounds = isWide ? 80 : 64;
        BooleanFunction sumFn = sumFn(n * 8, n);
        BooleanFunction choiceFn = (BooleanFunction) BooleanFunction.vectorFn(choiceFn(), n * 16);
        BooleanFunction roundFn = (BooleanFunction) roundCircuit(isWide).asFunction();
        BooleanFunction keyScheduleFn = (BooleanFunction) keyScheduleCircuit(isWide).asFunction();
        BooleanFunction checkFalseFn = (BooleanFunction) checkZeroFn(BooleanField.INSTANCE);
        ArithmeticCircuit.Gate<Boolean> g0, g1, g2, g3, g4, g5, g6;

        return ArithmeticCircuit.builder(BooleanField.INSTANCE).maximumFanOut(n * 16 + 2).maximumFanIn(1)
                .addGate(g0 = new ArithmeticCircuit.InputPort<>(BooleanField.INSTANCE, n * 32 + numRounds + 1))
                .addGate(g1 = new ArithmeticCircuit.Gate<>(keyScheduleFn, "KeySchedule"))
                .addGate(g2 = new ArithmeticCircuit.Gate<>(roundFn, "EncryptionRound"))
                .addGate(g3 = new ArithmeticCircuit.Gate<>(sumFn, "AddSavedState"))
                .addGate(g4 = new ArithmeticCircuit.Gate<>(choiceFn, "ChooseState"))
                .addGate(g5 = new ArithmeticCircuit.Gate<>(checkFalseFn, "CheckFalse"))
                .addGate(g6 = new ArithmeticCircuit.OutputPort<>(BooleanField.INSTANCE, n * 32 + numRounds + 1))
                // wire up disable/done flag input to CheckFalse gate:
                .addWire(g0, g5)
                // wire up message & round constant selector inputs to KeySchedule gate:
                .addWires(g0, g1, n * 16 + numRounds)
                // fan out KeySchedule gate (@ first selector bit) to disable/done flag output & (first third of) ChooseState gate:
                .addWire(g1, n * 16, g6, 0)
                .addWires(g1, n * 16, 0, g4, 0, 1, n * 16)
                // wire up KeySchedule gate to message & round constant selector outputs:
                .addWires(g1, g6, n * 16 + numRounds)
                // wire up KeySchedule gate (@ round key) to EncryptionRound gate:
                .addWires(g1, g2, n)
                // wire up current state inputs to EncryptionRound gate:
                .addWires(g0, g2, n * 8)
                // wire up EncryptionRound gate (@ new state) & saved state inputs to AddSavedState gate:
                .addWires(g2, g3, n * 8)
                .addWires(g0, g3, n * 8)
                // fan out (two copies of) AddSavedState gate to (middle third of) ChooseState gate:
                .addWires(g3, g4, n * 8)
                .addWires(g3, 0, g4, n * 24, n * 8)
                // fan out EncryptionRound gate (@ new state) & saved state inputs to (last third of) ChooseState gate:
                .addWires(g2, 0, g4, n * 32, n * 8)
                .addWires(g0, n * 24 + numRounds + 1, g4, n * 40, n * 8)
                // wire up ChooseState gate to new & saved state outputs:
                .addWires(g4, g6, n * 16)
                .build();
    }
}
