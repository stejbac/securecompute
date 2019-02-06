package securecompute.constraint.cyclic;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.FiniteField;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;
import securecompute.constraint.MultiplicativeLinearCode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

public class ReedSolomonCode<E> extends PuncturedPolynomialCode<E> implements MultiplicativeLinearCode<E, E> {

    // TODO: Consider caching all RS codes with a default global cache, using a static factory instead of a constructor. (Can do this for other codes, too.)
    private final ConcurrentMap<Integer, ReedSolomonCode<E>> cachedCodePowers = new ConcurrentHashMap<>();

    public ReedSolomonCode(int n, int k, FiniteField<E> field) {
        super(n, k, n - k + 1, generatorPolynomial(n, k, field));
        cachedCodePowers.put(1, this);
    }

    // TODO: Add 'primitivePower' parameter:
    private static <E> Polynomial<E> generatorPolynomial(int n, int k, FiniteField<E> field) {
        if (n < k) {
            throw new IllegalArgumentException("Length n is less than the code dimension k");
        }
        if (n >= field.size()) {
            throw new IllegalArgumentException("Codeword length n >= field cardinality");
        }

        E primitiveElement = field.getPrimitiveElement();
        FieldPolynomialRing<E> polynomialRing = new FieldPolynomialRing<>(field);

        // TODO: Consider changing Ring & AbelianGroup interface not to require an intermediate list like this:
        List<Polynomial<E>> linearFactors = IntStream.range(0, k)
                .mapToObj(i -> field.power(primitiveElement, i))
                .map(x -> polynomialRing.polynomial(field.negative(x), field.one()))
                .collect(ImmutableList.toImmutableList());

        Polynomial<E> checkPolynomial = polynomialRing.product(linearFactors);
        return polynomialRing.one().shift(n + k - 1).div(checkPolynomial);
    }

    @Override
    public MultiplicativeLinearCode<E, E> pow(int exponent) {
        return cachedCodePowers.computeIfAbsent(exponent, m ->
                new ReedSolomonCode<>(length(), Math.min(Math.max((dimension() - 1) * m + 1, 0), length()), field()));
    }
}
