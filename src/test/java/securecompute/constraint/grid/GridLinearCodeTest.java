package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import securecompute.algebra.BooleanField;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;
import securecompute.constraint.LocallyTestableCode.LocalTest.Evidence;
import securecompute.constraint.LocallyTestableCode.RepeatedLocalTest;
import securecompute.constraint.cyclic.PuncturedPolynomialCode;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GridLinearCodeTest {

    private static final FieldPolynomialRing<Boolean> POLYNOMIAL_RING = new FieldPolynomialRing<>(BooleanField.INSTANCE);

    private static final List<Polynomial<Boolean>> HAMMING_FACTORS = LongStream.of(0b1101, 0b1011, 0b11)
            .mapToObj(BooleanField::fromBinary)
            .collect(ImmutableList.toImmutableList());

    private static final List<Polynomial<Boolean>> GOLAY_FACTORS = LongStream.of(0b110001110101, 0b101011100011, 0b11)
            .mapToObj(BooleanField::fromBinary)
            .collect(ImmutableList.toImmutableList());

    private static final PuncturedPolynomialCode<Boolean> HAMMING_CODE = new PuncturedPolynomialCode<>(
            7, 4, 3, 3, HAMMING_FACTORS.get(0));

    private static final PuncturedPolynomialCode<Boolean> BINARY_GOLAY_CODE = new PuncturedPolynomialCode<>(
            23, 12, 7, 7, GOLAY_FACTORS.get(0));

//    private static final PuncturedPolynomialCode<Boolean> EXTENDED_BINARY_GOLAY_CODE = new PuncturedPolynomialCode<>(
//            24, 12, 8, GOLAY_FACTORS.get(0).multiply(GOLAY_FACTORS.get(2)));

    private static final GridLinearCode<Boolean, Boolean> GRID_CODE = new GridLinearCode<>(
            BINARY_GOLAY_CODE, HAMMING_CODE);

    private static final List<Boolean> MINIMAL_BAD_VECTOR_OF_ERASURES = IntStream.range(0, 23 * 7)
            .mapToObj(i -> i % 23 < 6 && i / 23 < 3 ? null : false)
            .collect(Collectors.toList());

    // FIXME: This isn't really a bad (i.e. uncorrectable) vector. For that, need to make row & column codeword symbols 'incompatible', so GRID_CODE is 0.
    private static final List<Boolean> MINIMAL_BAD_VECTOR_OF_ERRORS = IntStream.range(0, 23 * 7)
            .mapToObj(i -> (0b110001110101 >> i % 23 & 1) != 0 && i / 23 < 2)
            .collect(Collectors.toList());

    @Test
    void hammingCodeHasExpectedProperties() {
        assertEquals(BooleanField.fromBinary(0b1000_0001), POLYNOMIAL_RING.product(HAMMING_FACTORS));
        assertEquals(0, HAMMING_CODE.punctureNumber());
        assertEquals(0, HAMMING_CODE.shortenNumber());
    }

    @Test
    void binaryGolayCodeHasExpectedProperties() {
        assertEquals(BooleanField.fromBinary(0b1000_0000_0000_0000_0000_0001), POLYNOMIAL_RING.product(GOLAY_FACTORS));
        assertEquals(0, BINARY_GOLAY_CODE.punctureNumber());
        assertEquals(0, BINARY_GOLAY_CODE.shortenNumber());
//        assertEquals(0, EXTENDED_BINARY_GOLAY_CODE.punctureNumber());
//        assertEquals(11, EXTENDED_BINARY_GOLAY_CODE.shortenNumber());

//        for (int i = 0; i < 12; i++) {
//            List<Boolean> message = POLYNOMIAL_RING.one().shift(i).add(POLYNOMIAL_RING.one().shift(12))
//                    .getCoefficients().subList(0, 12);
//            List<Boolean> codeword = BINARY_GOLAY_CODE.encode(message);
//            System.out.println(codeword.stream().map(b -> b ? "1" : ".").collect(Collectors.joining()));
//        }
    }

    @Test
    void localTestHasCorrectDistance() {
        int distance = GRID_CODE.localTest().distance();
        assertEquals(6 * 2, distance);
        assertTrue(distance < MINIMAL_BAD_VECTOR_OF_ERASURES.stream().filter(Objects::isNull).count());
        assertTrue(distance < MINIMAL_BAD_VECTOR_OF_ERRORS.stream().filter(b -> b).count());
    }

    @Test
    void localTestHasCorrectFalsePositiveRate() {
        Random rnd = new Random(12345);

        long passCount = Stream.generate(() -> GRID_CODE.localTest().query(MINIMAL_BAD_VECTOR_OF_ERRORS, rnd))
                .limit(5000)
                .filter(Evidence::isValid)
                .count();
        double passRate = passCount / 5000.0;

        assertEquals(passRate, GRID_CODE.localTest().falsePositiveProbability(), 0.01);
    }

    @Test
    void localTestHasCorrectRowSelectionRate() {
        Random rnd = new Random(12345);

        long rowCount = Stream.generate(() -> GRID_CODE.localTest().query(MINIMAL_BAD_VECTOR_OF_ERRORS, rnd))
                .limit(5000)
                .filter(e -> e.y >= 0)
                .count();
        double rowSelectionRate = rowCount / 5000.0;

        assertEquals(rowSelectionRate, GRID_CODE.localTest().rowSelectionProbability(), 0.01);
    }

    @Test
    void localTestPreferentiallySamplesFromStrongestCode() {
        // Here, the 'strongest' code is that with the highest relative distance, i.e. the Hamming (column) code.
        assertTrue(GRID_CODE.localTest().rowSelectionProbability() < 0.5);
    }

    @Test
    void compoundLocalTestCanHaveCryptographicStrength() {
        Assumptions.assumeTrue(GRID_CODE.localTest().falsePositiveProbability() > 0.707); // ~ 0.5 ** 0.5
        Assumptions.assumeTrue(GRID_CODE.localTest().falsePositiveProbability() < 0.841); // ~ 0.5 ** 0.25

        RepeatedLocalTest<Boolean, ?> compoundTest = GRID_CODE.localTest(0x1.0p-256);

        assertTrue(compoundTest.singleTest().falsePositiveProbability() < 0.841);
        assertTrue(compoundTest.repetitionCount() > 512);
        assertTrue(compoundTest.repetitionCount() < 1024);
        assertTrue(compoundTest.falsePositiveProbability() <= 0x1.0p-256);

        Random rnd = new Random(23456);
        assertEquals(compoundTest.repetitionCount(), compoundTest.query(MINIMAL_BAD_VECTOR_OF_ERASURES, rnd).evidenceList().size());
        assertFalse(compoundTest.query(MINIMAL_BAD_VECTOR_OF_ERASURES, rnd).isValid());
        assertFalse(compoundTest.query(MINIMAL_BAD_VECTOR_OF_ERRORS, rnd).isValid());
    }
}
