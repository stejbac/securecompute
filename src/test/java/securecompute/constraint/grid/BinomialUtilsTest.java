package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Math.log;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static securecompute.constraint.grid.BinomialUtils.logBinomialCoefficientRatio;
import static securecompute.constraint.grid.BinomialUtils.sortedRandomChoice;

class BinomialUtilsTest {

    private static final int[][] TEST_BINOMIAL_COEFFICIENTS = {
            {1},
            {1, 1},
            {1, 2, 1},
            {1, 3, 3, 1},
            {1, 4, 6, 4, 1},
            {1, 5, 10, 10, 5, 1},
    };

    private static Stream<Arguments> testArgPairs() {
        List<Integer> choices = ImmutableList.of(0, 1, 2, 3, 4, 5);
        return Lists.cartesianProduct(choices, choices).stream()
                .filter(args -> args.get(1) <= args.get(0))
                .map(List::toArray)
                .map(Arguments::of);
    }

    private static Stream<Arguments> testArgTriples() {
        List<Integer> choices = ImmutableList.of(0, 1, 2, 3, 4, 5);
        return Lists.cartesianProduct(choices, choices, choices).stream()
                .filter(args -> args.get(2) <= args.get(0) && args.get(2) <= args.get(1))
                .map(List::toArray)
                .map(Arguments::of);
    }

    @Test
    void logGammaFunctionHasSmallUlpError() {
        // TODO: Prevent cumulative rounding errors in log summation - probably have much better actual accuracy than 11 ulps...
        double logGamma = 0;
        for (int i = 1; i < 1000; logGamma += log(i++)) {
            double x = BinomialUtils.logGamma(i);
            int ulps = 0;
            while (x < logGamma) {
                x = Math.nextUp(x);
                ulps++;
            }
            while (x > logGamma) {
                x = Math.nextDown(x);
                ulps--;
            }
            assertTrue(ulps >= -3, "logGamma(" + i + ") lower bound");
            assertTrue(ulps <= 11, "logGamma(" + i + ") upper bound");
        }
    }

    @ParameterizedTest
    @MethodSource("testArgTriples")
    void logBinomialCoefficientRatioHasCorrectSmallValues(int n1, int n2, int k) {
        assumeTrue(k <= n1);
        assumeTrue(k <= n2);

        double expected = log(TEST_BINOMIAL_COEFFICIENTS[n1][k]) - log(TEST_BINOMIAL_COEFFICIENTS[n2][k]);
        assertEquals(expected, logBinomialCoefficientRatio(n1, n2, k), 2e-15);
    }

    @Test
    void logBinomialCoefficientRatioIsNegativeInfinityForOverlyLargeK() {
        assertEquals(Double.NEGATIVE_INFINITY, logBinomialCoefficientRatio(0, 1, 1));
        assertEquals(Double.NEGATIVE_INFINITY, logBinomialCoefficientRatio(1, 2, 2));
        assertEquals(Double.NEGATIVE_INFINITY, logBinomialCoefficientRatio(1, 3, 2));
        assertEquals(Double.NEGATIVE_INFINITY, logBinomialCoefficientRatio(5, 15, 10));
    }

    @ParameterizedTest
    @MethodSource("testArgPairs")
    void sortedRandomChoiceGivesEveryPossibleValidChoice(int n, int k) {
        assumeTrue(k <= n);

        Random rnd = new Random(123456);
        Set<List<Integer>> choices = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            List<Integer> choice = Ints.asList(sortedRandomChoice(n, k, rnd));
            if (choices.contains(choice)) {
                continue;
            }

            List<Integer> validChoice = choice.stream()
                    .filter(j -> j >= 0 && j < n)
                    .sorted()
                    .distinct()
                    .collect(ImmutableList.toImmutableList());

            assertEquals(k, validChoice.size());
            assertEquals(validChoice, choice);

            choices.add(validChoice);
        }

        assertEquals(TEST_BINOMIAL_COEFFICIENTS[n][k], choices.size());
    }
}
