package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Math.log;
import static java.math.MathContext.DECIMAL128;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static securecompute.constraint.grid.BinomialUtils.logBinomialCoefficientRatio;
import static securecompute.constraint.grid.BinomialUtils.sortedRandomChoice;

class BinomialUtilsTest {

    private static final BigDecimal _0 = BigDecimal.ZERO;
    private static final BigDecimal _1 = BigDecimal.ONE;
    private static final BigDecimal _2 = BigDecimal.valueOf(2);
    private static final BigDecimal E = new BigDecimal("2.7182818284590452353602874713526624977572", DECIMAL128);
    private static final BigDecimal LOG_2 = new BigDecimal("0.6931471805599453094172321214581765680755", DECIMAL128);
    private static final BigDecimal HALF_PI = new BigDecimal("3.1415926535897932384626433832795028841972").divide(_2, DECIMAL128);

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
    void logGammaHasSmallUlpError() {
        // A sanity check of 'logApprox'; if it's accurate at 1, e and e**20, then it's probably accurate everywhere in between:
        assumeTrue(isCloseToZero(logApprox(_1)));
        assumeTrue(isCloseToZero(logApprox(E).subtract(_1)));
        assumeTrue(isCloseToZero(logApprox(E.pow(20)).scaleByPowerOfTen(-1).subtract(_2)));

//        double maxAbsUlpError = 0.0;
        BigDecimal logGamma = _0;
        for (int i = 1; i < 65536; logGamma = logGamma.add(logApprox(i++))) {
            double x = BinomialUtils.logGamma(i);
            double ulp = Math.ulp(x);

            double ulpError = BigDecimal.valueOf((long) (x / ulp))
                    .subtract(logGamma.multiply(_2.pow(-Math.getExponent(ulp)), DECIMAL128))
                    .doubleValue();

//            if (Math.abs(ulpError) > maxAbsUlpError) {
//                System.out.println(ulpError + " @ " + i);
//                maxAbsUlpError = Math.abs(ulpError);
//            }
            assertEquals(0.0, ulpError, 2.4, "logGamma(" + i + ") error in ulps");
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

    private static BigDecimal sqrtApprox(BigDecimal x) {
        BigDecimal y0 = BigDecimal.valueOf(Math.sqrt(x.doubleValue()));
        return x.divide(y0, DECIMAL128).add(y0, DECIMAL128).divide(_2, DECIMAL128);
    }

    private static BigDecimal agmApprox(BigDecimal x, BigDecimal y) {
        for (int i = 0; i < 10; i++) {
            BigDecimal gm = sqrtApprox(x.multiply(y, DECIMAL128));
            x = x.add(y, DECIMAL128).divide(_2, DECIMAL128);
            y = gm;
        }
        return x;
    }

    private static BigDecimal logApprox(BigDecimal x) {
        int m = 64 - Math.getExponent(x.doubleValue());
        BigDecimal y = _2.pow(Math.max(2 - m, 0)).divide(_2.pow(Math.max(m - 2, 0)).multiply(x, DECIMAL128), DECIMAL128);
        return HALF_PI.divide(agmApprox(_1, y), DECIMAL128).subtract(LOG_2.multiply(BigDecimal.valueOf(m)), DECIMAL128);
    }

    private static BigDecimal logApprox(long n) {
        return n == 1 ? _0 : logApprox(BigDecimal.valueOf(n));
    }

    private static boolean isCloseToZero(BigDecimal x) {
        return x.compareTo(_0) == 0 || x.scale() - x.precision() > 29;
    }
}
