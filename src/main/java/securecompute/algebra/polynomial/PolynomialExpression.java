package securecompute.algebra.polynomial;

import securecompute.algebra.Ring;

import java.util.List;
import java.util.function.IntFunction;

public interface PolynomialExpression<E> {

    enum Type {
        CONSTANT, SUM, PRODUCT, VARIABLE
    }

    Type expressionType();

    E constantValue();

    int variableIndex();

    List<PolynomialExpression<E>> subTerms();

    default E evaluate(Ring<E> ring, IntFunction<E> symbolMapping) {
        switch (expressionType()) {
            case CONSTANT:
                return constantValue();
            case SUM:
                return ring.sum(subTerms().stream().map(t -> t.evaluate(ring, symbolMapping)));
            case PRODUCT:
                return ring.product(subTerms().stream().map(t -> t.evaluate(ring, symbolMapping)));
            case VARIABLE:
                return symbolMapping.apply(variableIndex());
        }
        throw new AssertionError(); // unreachable
    }

    // TODO: Add pretty printer (here or to BasePolynomialExpression).
}
