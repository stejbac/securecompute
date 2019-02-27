package securecompute.constraint.grid;

import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class BinomialUtils {

    private static final double LOG_PI = 1.14472988584940017414;
    private static final double HALF_LOG_2_PI = 0.918938533204672741780;
    private static final double LANCZOS_G = 607.0 / 128.0;

    // The same coefficients used in Apache Commons Math, taken from http://my.fit.edu/~gabdo/gamma.txt
    // (A note on the computation of the convergent Lanczos complex Gamma approximation, Paul Godfrey, 2001).
    private static final double[] LANCZOS_COEFFICIENTS = {
            0.99999999999999709182,
            57.156235665862923517,
            -59.597960355475491248,
            14.136097974741747174,
            -0.49191381609762019978,
            .33994649984811888699e-4,
            .46523628927048575665e-4,
            -.98374475304879564677e-4,
            .15808870322491248884e-3,
            -.21026444172410488319e-3,
            .21743961811521264320e-3,
            -.16431810653676389022e-3,
            .84418223983852743293e-4,
            -.26190838401581408670e-4,
            .36899182659531622704e-5,
    };

    private BinomialUtils() {
    }

    private static double sinPiTimes(double x) {
        return Math.sin(Math.PI * (x - (long) x));
    }

    // visible for tests
    static double logGamma(double x) {
        if (x < 0.5) {
            return LOG_PI - Math.log(sinPiTimes(x)) - logGamma(1 - x);
        }
        if (x == 1.0 || x == 2.0) {
            return 0.0;
        }
        double lanczos = 0.0;
        for (int i = LANCZOS_COEFFICIENTS.length; --i > 0; ) {
            lanczos += LANCZOS_COEFFICIENTS[i] / (i - 1 + x);
        }
        lanczos += LANCZOS_COEFFICIENTS[0];
        double t = x - 0.5 + LANCZOS_G;
        return Math.log(lanczos) + HALF_LOG_2_PI - t + (x - 0.5) * Math.log(t);
    }

    static double logBinomialCoefficientRatio(int n1, int n2, int k) {
        return logGamma(n1 + 1) - logGamma(n2 + 1) + (logGamma(n2 + 1 - k) - logGamma(n1 + 1 - k));
    }

    static int[] sortedRandomChoice(int n, int k, Random random) {
        int[] result;
        if (n - k < k) {
            int[] complement = sortedRandomChoice(n, n - k, random);
            result = new int[k];
            for (int i = 0, j = 0; i < n; i++) {
                if (i - j >= complement.length || complement[i - j] != i) {
                    result[j++] = i;
                }
            }
        } else {
            Set<Integer> choice = new HashSet<>(k);
            while (choice.size() < k) {
                choice.add(random.nextInt(n));
            }
            result = Ints.toArray(choice);
            Arrays.sort(result);
        }
        return result;
    }
}
