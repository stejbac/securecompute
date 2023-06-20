package securecompute.constraint.cyclic;

import com.google.common.collect.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import securecompute.algebra.FiniteField;
import securecompute.algebra.Gf256;
import securecompute.algebra.Gf65536;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("unchecked")
class ReedSolomonCodeTest<E> {

    private static final Gf256 AES_FIELD = new Gf256(0b100011011, 0b11);
    private static final Gf65536 QUADRATIC_FIELD = new Gf65536(AES_FIELD, 0b10, 0b10);
    private static final int n = 1000, k = 100;

    private static final ReedSolomonCode<?> CODE = new ReedSolomonCode<>(n, k, QUADRATIC_FIELD);
    @SuppressWarnings("ConstantConditions")
    private static final Map<Integer, ?> KNOWN_SYMBOL_TEST_MAP = Maps.transformValues(ImmutableSortedMap.of(
            0, 0b00000000,
            1, 0b00000001,
            10, 0b00000010,
            100, 0b00000100,
            999, 0b00001000
    ), x -> QUADRATIC_FIELD.element(AES_FIELD.element(x), AES_FIELD.zero()));

    private final ReedSolomonCode<E> code = (ReedSolomonCode<E>) CODE;
    private final Map<Integer, E> knownSymbolTestMap = (Map<Integer, E>) KNOWN_SYMBOL_TEST_MAP;

    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "Message {arguments}")
    @MethodSource("testMessages")
    @interface ParamTest {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "Messages {arguments}")
    @MethodSource("testMessages2")
    @interface Param2Test {
    }

    private static List<List<?>> testMessages() {
        Random rnd = new Random(1234);
        return ImmutableList.of(
                randomMessage(0, rnd, 0, 0),
                randomMessage(1, rnd, 0, 1),
                randomMessage(2, rnd, k - 1, k),
                randomMessage(3, rnd, 0, k),
                randomMessage(4, rnd, 0, k)
        );
    }

    @SuppressWarnings("unused") // referenced by ParamTest2
    private static List<Arguments> testMessages2() {
        //noinspection ConstantConditions
        return Lists.transform(
                Lists.cartesianProduct(testMessages(), testMessages()),
                args -> Arguments.of(args.toArray())
        );
    }

    private static List<?> randomMessage(int caseIndex, Random rnd, int start, int end) {
        FiniteField<?> field = CODE.field();
        return IntStream.range(0, k)
                .mapToObj(i -> i >= start && i < end ? field.sampleUniformly(rnd) : field.zero())
                .collect(Collectors.toCollection(() -> new ArrayList<Object>() {
                    @Override
                    public String toString() {
                        return "[" + caseIndex + "]";
                    }
                }));
    }

    @ParamTest
    void encodeMapsToValidCodeword(List<E> message) {
        List<E> codeword = code.encode(message);
        List<E> zeroVector = Collections.nCopies(n - k, code.field().zero());

        assertEquals(zeroVector, code.parityCheck(codeword));
        Assertions.assertTrue(code.isValid(codeword));
    }

    @ParamTest
    void decodeReversesEncode(List<E> message) {
        assertEquals(message, code.decode(code.encode(message)));
    }

    @ParamTest
    void codeIsSystematic(List<E> message) {
        assertTrue(code.isSystematic());
        assertEquals(message, code.encode(message).subList(n - k, n));
    }

    @Param2Test
    void squareOfUnshortenedCodeIsReedSolomon(List<E> message1, List<E> message2) {
        assumeTrue(code.shortenNumber() == 0);

        List<E> codeword1 = code.encode(message1);
        List<E> codeword2 = code.encode(message2);

        //noinspection UnstableApiUsage
        List<E> pairwiseProduct = Streams.zip(codeword1.stream(), codeword2.stream(), code.field()::product)
                .collect(ImmutableList.toImmutableList());

        Assertions.assertTrue(code.pow(2).isValid(pairwiseProduct));
    }

    @Test
    void codeIsUnshortened() {
        assertEquals(0, code.shortenNumber());
    }

    @Test
    void interpolationGivesCorrectCodeword() {
        List<E> codeword = code.interpolate(knownSymbolTestMap);
        Assertions.assertTrue(code.isValid(codeword));
        // noinspection ConstantConditions
        assertEquals(knownSymbolTestMap, Maps.transformEntries(knownSymbolTestMap, (i, x) -> codeword.get(i)));

        ReedSolomonCode<E> innerCode = new ReedSolomonCode<>(n, knownSymbolTestMap.size(), code.field());
        Assertions.assertTrue(innerCode.isValid(codeword));
    }

    // TODO: Consider adding test that X^(n+1) div (X - g^i) shift -1 is a codeword _only_ for 0 <= i < k.
}
