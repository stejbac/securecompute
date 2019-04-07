package securecompute.algebra.polynomial;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import securecompute.algebra.IntegerRing;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static securecompute.algebra.polynomial.BasePolynomialExpression.*;

class BasePolynomialExpressionTest {

    private static final IntegerRing RING = IntegerRing.INSTANCE;
    private static final Constant<Integer> MINUS_1 = constant(-1);
    private static final Constant<Integer> _1 = constant(1), _2 = constant(2), _3 = constant(3);
    private static final Variable<Integer> X = variable(0), Y = variable(1);

    @Test
    void additionSubtractionChainProducesLowDepthTree() {
        PolynomialExpression<Integer> actual = _1.add(_2).subtract(_3, RING).subtract(X, RING).add(Y);
        List<PolynomialExpression<Integer>> expectedTerms = ImmutableList.of(_1, _2, MINUS_1.multiply(_3), MINUS_1.multiply(X), Y);

        assertEquals(Type.SUM, actual.expressionType());
        assertEquals(expectedTerms, actual.subTerms());
        assertEquals(sum(expectedTerms), actual);
    }

    @Test
    void multiplicationChainProducesLowDepthTree() {
        PolynomialExpression<Integer> actual = _1.multiply(_2).multiply(X).multiply(_3).multiply(Y);
        List<PolynomialExpression<Integer>> expectedTerms = ImmutableList.of(_1, _2, X, _3, Y);

        assertEquals(Type.PRODUCT, actual.expressionType());
        assertEquals(expectedTerms, actual.subTerms());
        assertEquals(product(expectedTerms), actual);
    }

    @Test
    void evaluateProducesCorrectResult() {
        PolynomialExpression<Integer> p = X.add(Y).multiply(X.subtract(Y, RING)).add(_1);

        assertEquals(1, p.evaluate(RING, i -> 0).intValue());
        assertEquals(1, p.evaluate(RING, i -> 1).intValue());
        assertEquals(0, p.evaluate(RING, i -> i).intValue());
        assertEquals(76, p.evaluate(RING, i -> i == 0 ? 10 : 5).intValue());
    }
}
