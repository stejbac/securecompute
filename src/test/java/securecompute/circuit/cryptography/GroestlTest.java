package securecompute.circuit.cryptography;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import securecompute.algebra.Gf256;
import securecompute.circuit.AlgebraicFunction;
import securecompute.circuit.ArithmeticCircuitTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static securecompute.circuit.cryptography.Aes.AES_FIELD;

class GroestlTest {
//    private static Gf256.Element _1 = AES_FIELD.one(), _2 = AES_FIELD.element(2), _3 = AES_FIELD.element(3);
//    private static Gf256.Element _6 = AES_FIELD.element(6), _8 = AES_FIELD.element(8), _9 = AES_FIELD.element(9);

    private static final String KEY_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String PLAINTEXT_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String Q_MAPPED_KEY_HEX = "" +
            "6952329f2c1abb88a52c45989714cc0d2984ff322684a9ed2f299217fe330e4f" +
            "ded60ffa509afd3e77d10bb8ea7b3803ed3048f7d118abf59be82761d0319ef3";

    private static final String CIPHERTEXT_HEX = "" +
            "978a773749a27307ce15f5d15aa4982716a79c1825753fc3d92a134b5fca7c9f" +
            "3f8966722a138c8ec6b95c6803386d52153d6dd0622ff79912780fac63ad4c1e";

    private static final String WIDE_KEY_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String WIDE_PLAINTEXT_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String WIDE_Q_MAPPED_KEY_HEX = "" +
            "b03ac1561284cbd786fbf4d3c70c3aa2b1c4a7cf75cd578f7a0270cebb6b4083" +
            "4b05dedbe867d74441f3e3283b31650a15578e195b7bdfcfa982f0ebe3f4fc36" +
            "224ba778c3f31d2126bde24f3ab297a29c5fcfe9154050c60535343ed538bec8" +
            "fc2c14ec2b0f8fcfd1551a2d22fe8b7a20dcf7fab3f395ee23e998c5257ed410";

    private static final String WIDE_CIPHERTEXT_HEX = "" +
            "31a1bffaad983d7b64cd9aead9c0f1af65995dfe48158f976f9de611ba18c817" +
            "b182fb2a31dfb94ff392119d08f3166bbe5c4950e4b3ec7d3e0831a2d4622cd1" +
            "5aba35b1836e4acc8ca041a15488c651daa68f41d384dfef19ce061007efd3ba" +
            "647ff0014382f0153a8757ab70189a0b68b383cdc20a843a11969c5540bc9023";

    private static final String[] PADDED_EMPTY_MESSAGE_BLOCKS = {"" +
            "8000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000001", "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000"
    };

    private static final String[] WIDE_PADDED_EMPTY_MESSAGE_BLOCKS = {"" +
            "8000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000001", "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000"
    };

    private static final String GROESTL_224_IV_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000e0";

    private static final String GROESTL_256_IV_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000010000000000000000";

    private static final String GROESTL_384_IV_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000100000000000000000000000000000080";

    private static final String GROESTL_512_IV_HEX = "" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000200000000000000000000000000000000";

    private static final Map<Integer, String> GROESTL_IV_MAP = ImmutableMap.of(
            224, GROESTL_224_IV_HEX,
            256, GROESTL_256_IV_HEX,
            384, GROESTL_384_IV_HEX,
            512, GROESTL_512_IV_HEX
    );

    private static final String EMPTY_STRING_GROESTL_224_DIGEST =
            "f2e180fb5947be964cd584e22e496242c6a329c577fc4ce8c36d34c3";

    private static final String EMPTY_STRING_GROESTL_256_DIGEST =
            "1a52d11d550039be16107f9c58db9ebcc417f16f736adb2502567119f0083467";

    private static final String EMPTY_STRING_GROESTL_384_DIGEST = "" +
            "ac353c1095ace21439251007862d6c62f829ddbe6de4f78e68d310a9205a736d" +
            "8b11d99bffe448f57a1cfa2934f044a5";

    private static final String EMPTY_STRING_GROESTL_512_DIGEST = "" +
            "6d3ad29d279110eef3adbd66de2a0345a77baede1557f5d099fce0c03d6dc2ba" +
            "8e6d4a6633dfbd66053c20faa87d1a11f39a7fbe4a6c2f009801370308fc4ad8";

    private static final Map<Integer, String> EMPTY_STRING_DIGEST_MAP = ImmutableMap.of(
            224, EMPTY_STRING_GROESTL_224_DIGEST,
            256, EMPTY_STRING_GROESTL_256_DIGEST,
            384, EMPTY_STRING_GROESTL_384_DIGEST,
            512, EMPTY_STRING_GROESTL_512_DIGEST
    );

    @Test
    void testIncrementFn() {
        ArithmeticCircuitTest.checkAlgebraicFunction(Groestl.incrementFn());
    }

    @Test
    void testEvenMansourEncryptionRound() {
        AlgebraicFunction<Gf256.Element> roundFn = Groestl.evenMansourEncryptionRoundCircuit().asFunction();

        List<Gf256.Element> input = asVector(KEY_HEX + "00" + PLAINTEXT_HEX);
        List<Gf256.Element> expectedOutput = asVector(Q_MAPPED_KEY_HEX + "0a" + CIPHERTEXT_HEX);

        Assertions.assertEquals(expectedOutput, Stream.iterate(input, roundFn::apply).skip(10).iterator().next());
    }

    @Test
    void testEvenMansourEncryptionRoundWide() {
        AlgebraicFunction<Gf256.Element> roundFn = Groestl.evenMansourEncryptionRoundWideCircuit().asFunction();

        List<Gf256.Element> input = asVector(WIDE_KEY_HEX + "00" + WIDE_PLAINTEXT_HEX);
        List<Gf256.Element> expectedOutput = asVector(WIDE_Q_MAPPED_KEY_HEX + "0e" + WIDE_CIPHERTEXT_HEX);

        Assertions.assertEquals(expectedOutput, Stream.iterate(input, roundFn::apply).skip(14).iterator().next());
    }

    @ParameterizedTest
    @ValueSource(ints = {224, 256})
    void testRawGroestlStep(int nBits) {
        AlgebraicFunction<Gf256.Element> stepFn = Groestl.rawGroestlStepCircuit().asFunction();

        List<Gf256.Element> state = asVector("00" + GROESTL_IV_MAP.get(nBits) + GROESTL_IV_MAP.get(nBits));

        for (String blockHex : PADDED_EMPTY_MESSAGE_BLOCKS) {
            List<Gf256.Element> fullState = Stream.concat(asVector("00" + blockHex).stream(), state.stream())
                    .collect(ImmutableList.toImmutableList());

            while (fullState.get(0).getValue() == 0) { // step until disable/done flag is set
                fullState = stepFn.apply(fullState);
            }

            List<Gf256.Element> finalState = fullState;
            Assertions.assertEquals(AES_FIELD.one(), finalState.get(0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> stepFn.apply(finalState));

            state = finalState.subList(65, 194);
            Assertions.assertEquals(AES_FIELD.zero(), state.get(0));
            Assertions.assertEquals(state.subList(1, 65), state.subList(65, 129));
        }

        Assertions.assertEquals(EMPTY_STRING_DIGEST_MAP.get(nBits),
                BaseEncoding.base16().lowerCase().encode(toDigest(state, nBits)));
    }

    @ParameterizedTest
    @ValueSource(ints = {384, 512})
    void testRawGroestlStepWide(int nBits) {
        // TODO: Very similar to above - consider de-duplicating.
        AlgebraicFunction<Gf256.Element> stepFn = Groestl.rawGroestlStepCircuitWide().asFunction();

        List<Gf256.Element> state = asVector("00" + GROESTL_IV_MAP.get(nBits) + GROESTL_IV_MAP.get(nBits));

        for (String blockHex : WIDE_PADDED_EMPTY_MESSAGE_BLOCKS) {
            List<Gf256.Element> fullState = Stream.concat(asVector("00" + blockHex).stream(), state.stream())
                    .collect(ImmutableList.toImmutableList());

            while (fullState.get(0).getValue() == 0) { // step until disable/done flag is set
                fullState = stepFn.apply(fullState);
            }

            List<Gf256.Element> finalState = fullState;
            Assertions.assertEquals(AES_FIELD.one(), finalState.get(0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> stepFn.apply(finalState));

            state = finalState.subList(129, 386);
            Assertions.assertEquals(AES_FIELD.zero(), state.get(0));
            Assertions.assertEquals(state.subList(1, 129), state.subList(129, 257));
        }

        Assertions.assertEquals(EMPTY_STRING_DIGEST_MAP.get(nBits),
                BaseEncoding.base16().lowerCase().encode(toDigest(state, nBits)));
    }

    private static List<Gf256.Element> asVector(String hex) {
        return Bytes.asList(BaseEncoding.base16().lowerCase().decode(hex)).stream()
                .map(AES_FIELD::element)
                .collect(ImmutableList.toImmutableList());
    }

    private static byte[] toDigest(List<Gf256.Element> state, int nBits) {
        int blockSize = nBits > 256 ? 128 : 64;

        List<Gf256.Element> fullHash = Streams.zip(
                state.subList(1, blockSize + 1).stream(),
                asVector(blockSize > 64 ? WIDE_Q_MAPPED_KEY_HEX : Q_MAPPED_KEY_HEX).stream(),
                AES_FIELD::sum
        ).collect(ImmutableList.toImmutableList());

        byte[] digest = new byte[nBits / 8];
        int offset = blockSize - digest.length;

        for (int i = 0; i < blockSize / 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i * 8 + j >= offset) {
                    digest[i * 8 + j - offset] = fullHash.get(j * blockSize / 8 + i).getValue();
                }
            }
        }
        return digest;
    }

//    @Test
//    void testCounterFunction() {
//        for (int i = 0; i < 256; i++) {
//            Gf256.Element x = AES_FIELD.element(i);
//            for (int j = 0; j < 256; j++) {
//                Gf256.Element y = AES_FIELD.element(j);
//                Gf256.Element z = x.add(y).multiply(_3).add(_1).divide(_2);
//                if (z.getValue() == 0) {
//                    continue;
//                }
//                Gf256.Element u = z.add(_8.divide(z));
//                Gf256.Element v = u.add(_6).multiply(u.add(_9));
//                Gf256.Element w = y.divide(z).add(_1).divide(_2);
//                if ((w.getValue() & 0xf8) == 0 && v.getValue() == 0) {
//                    System.out.println("x = " + x + ", y = " + y + ", z = " + z + ", u = " + u + ", w = " + w);
//                }
//            }
//            System.out.println();
//        }
//    }
}
