package securecompute.circuit.cryptography;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import securecompute.circuit.BooleanFunction;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class Sha2Test {
    private static final String SHA224_IV_HEX = "befa4fa4 64f98fa7 68581511 ffc00b31 f70e5939 3070dd17 367cd507 c1059ed8";
    private static final String SHA256_IV_HEX = "5be0cd19 1f83d9ab 9b05688c 510e527f a54ff53a 3c6ef372 bb67ae85 6a09e667";
    private static final String SHA384_IV_HEX = "" +
            "47b5481dbefa4fa4 db0c2e0d64f98fa7 8eb44a8768581511 67332667ffc00b31 " +
            "152fecd8f70e5939 9159015a3070dd17 629a292a367cd507 cbbb9d5dc1059ed8";
    private static final String SHA512_IV_HEX = "" +
            "5be0cd19137e2179 1f83d9abfb41bd6b 9b05688c2b3e6c1f 510e527fade682d1 " +
            "a54ff53a5f1d36f1 3c6ef372fe94f82b bb67ae8584caa73b 6a09e667f3bcc908";
    private static final String SHA512_224_IV_HEX = "" +
            "1112e6ad91d692a1 3f9d85a86a1d36c8 77e36f7304c48942 0f6d2b697bd44da8 " +
            "679dd514582f9fcf 1dfab7ae32ff9c82 73e1996689dcd4d6 8c3d37c819544da2";
    private static final String SHA512_256_IV_HEX = "" +
            "0eb72ddc81c52ca2 2b0199fc2c85b8aa be5e1e2553863992 96283ee2a88effe3 " +
            "963877195940eabd 2393b86b6f53b151 9f555fa3c84c64c2 22312194fc2bf72c";

    private static final String PADDED_EMPTY_MESSAGE_BLOCK = "" +
            "00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000 " +
            "00000000 00000000 00000000 00000000 00000000 00000000 00000000 80000000";
    private static final String WIDE_PADDED_EMPTY_MESSAGE_BLOCK = "" +
            "0000000000000000 0000000000000000 0000000000000000 0000000000000000 " +
            "0000000000000000 0000000000000000 0000000000000000 0000000000000000 " +
            "0000000000000000 0000000000000000 0000000000000000 0000000000000000 " +
            "0000000000000000 0000000000000000 0000000000000000 8000000000000000";

    private static final Map<Integer, String> SHA2_IV_MAP = ImmutableMap.<Integer, String>builder()
            .put(224, SHA224_IV_HEX)
            .put(256, SHA256_IV_HEX)
            .put(384, SHA384_IV_HEX)
            .put(512, SHA512_IV_HEX)
            .put(65536 + 224, SHA512_224_IV_HEX)
            .put(65536 + 256, SHA512_256_IV_HEX)
            .build();

    @Test
    void testSumFn() {
//        System.out.println(asHexString(asVector("0123456789abcdef")));

        List<Boolean> input = asVector("0000789a 0000a987").reverse();
        List<Boolean> output = Sha2.sumFn(32, 8).apply(input);
        List<Boolean> inputPlusOutput = ImmutableList.<Boolean>builder().addAll(input).addAll(output).build();

        assertEquals("00002121", asHexString(Lists.reverse(output)));
        assertTrue(Sha2.sumFn(32, 8).parityCheck(inputPlusOutput).stream().noneMatch(b -> b));
    }

    @Test
    void testRound() {
        BooleanFunction roundFn = (BooleanFunction) Sha2.roundCircuit(false).asFunction();

        List<Boolean> input = asVector("00000008 00000007 00000006 00000005 00000004 00000003 00000002 00000001 80000000").reverse();
        List<Boolean> output = roundFn.apply(input);

        String expectedHex = "00000007 00000006 00000005 94a00292 00000003 00000002 00000001 d4a80691";
        assertEquals(expectedHex.replace(" ", ""), asHexString(Lists.reverse(output)));
    }

    @Test
    void testKeySchedule() {
        BooleanFunction keyScheduleFn = (BooleanFunction) Sha2.keyScheduleCircuit(false).asFunction();

        List<Boolean> input = asVector("0000000000000001 " +
                "00001000 00000f00 00000e00 00000d00 00000c00 00000b00 00000a00 00000900 " +
                "00000800 00000700 00000600 00000500 00000400 00000300 00000200 00000100"
        ).reverse();
        List<Boolean> output = keyScheduleFn.apply(input);

        String expectedHex = "428a3098 " + "0000000000000002 " +
                "06e00b47 00001000 00000f00 00000e00 00000d00 00000c00 00000b00 00000a00 " +
                "00000900 00000800 00000700 00000600 00000500 00000400 00000300 00000200";
        assertEquals(expectedHex.replace(" ", ""), asHexString(Lists.reverse(output)));
    }

    @ParameterizedTest
    @ValueSource(ints = {224, 256, 384, 512, 65536 + 224, 65536 + 256})
    void testRawSha2Step(int nBits) throws Exception {
        boolean isWide = nBits > 256;
        int n = isWide ? 64 : 32;
        int numRounds = isWide ? 80 : 64;
        BooleanFunction stepFn = (BooleanFunction) Sha2.rawSha2StepCircuit(isWide).asFunction();
        String ivHex = SHA2_IV_MAP.get(nBits);
        String selectorHex = Strings.repeat("0", isWide ? 19 : 15) + "1";

        String blockHex = isWide ? WIDE_PADDED_EMPTY_MESSAGE_BLOCK : PADDED_EMPTY_MESSAGE_BLOCK;
        List<Boolean> fullState = asVector(ivHex + ivHex + selectorHex + blockHex + "00")
                .reverse().subList(7, stepFn.inputLength() + 7);

        while (!fullState.get(0)) {
            fullState = stepFn.apply(fullState);
        }

        List<Boolean> finalState = fullState;
        assertThrows(IllegalArgumentException.class, () -> stepFn.apply(finalState));

        List<Boolean> state = finalState.subList(n * 16 + 1, finalState.size());
        assertEquals(selectorHex, asHexString(Lists.reverse(state.subList(0, numRounds))));
        assertEquals(state.subList(numRounds, numRounds + n * 8), state.subList(numRounds + n * 8, state.size()));

        List<Boolean> hashBits = Lists.partition(state.subList(numRounds, state.size()), n).stream()
                .flatMap(list -> Lists.reverse(list).stream())
                .limit(nBits & 65535)
                .collect(ImmutableList.toImmutableList());

        MessageDigest digest = MessageDigest.getInstance((nBits < 65536 ? "SHA-" : "SHA-512/") + (nBits & 65535));
        String expectedHex = BaseEncoding.base16().lowerCase().encode(digest.digest(new byte[0]));

        assertEquals(expectedHex, asHexString(hashBits));
    }

//    @Test
//    void testRoundFn() {
//        SHA2.SHA256 sha256 = new SHA2.SHA256();
//        byte[] buf = new byte[64];
//        buf[0] = -0x80;
//        sha256.implCompress(buf, 0);
//        Arrays.stream(sha256.state).mapToObj(Integer::toHexString).forEach(System.out::println);
//        System.out.println();
//        System.out.println(BaseEncoding.base16().lowerCase().encode(Hashing.sha256().hashBytes(new byte[0]).asBytes()));
//    }

    private static ImmutableList<Boolean> asVector(String hex) {
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(hex.replace(" ", ""));
        return IntStream.range(0, bytes.length * 8)
                .mapToObj(i -> (bytes[i / 8] & (128 >> (i & 7))) != 0)
                .collect(ImmutableList.toImmutableList());
    }

    private static String asHexString(List<Boolean> bits) {
        List<Byte> bytes = Lists.partition(bits, 8).stream()
                .map(list -> (byte) IntStream.range(0, 8).map(i -> i < list.size() && list.get(i) ? 128 >> i : 0).sum())
                .collect(ImmutableList.toImmutableList());
        return BaseEncoding.base16().lowerCase().encode(Bytes.toArray(bytes));
    }
}
