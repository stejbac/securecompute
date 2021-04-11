package securecompute.constraint.cyclic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import securecompute.algebra.FiniteField;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;
import securecompute.constraint.MultiplicativeLinearCode;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ReedSolomonCode<E> extends PuncturedPolynomialCode<E> implements MultiplicativeLinearCode<E, E> {

    // TODO: Consider caching all RS codes with a default global cache, using a static factory instead of a constructor. (Can do this for other codes, too.)
    private final ConcurrentMap<Integer, ReedSolomonCode<E>> cachedCodePowers = new ConcurrentHashMap<>();

    public ReedSolomonCode(int n, int k, FiniteField<E> field) {
        super(n, k, n - k + 1, k + 1, generatorPolynomial(n, k, field));
        cachedCodePowers.put(1, this);
    }

    // TODO: Add 'primitivePower' parameter:
    private static <E> Polynomial<E> generatorPolynomial(int n, int k, FiniteField<E> field) {
        if (n < k) {
            throw new IllegalArgumentException("Length n is less than the code dimension k");
        }
        if (n >= field.sizeAsLong()) {
            throw new IllegalArgumentException("Codeword length n >= field cardinality");
        }

        E primitiveElement = field.getPrimitiveElement();
        FieldPolynomialRing<E> polynomialRing = new FieldPolynomialRing<>(field);

        Stream<Polynomial<E>> linearFactors = IntStream.range(0, k)
                .mapToObj(i -> field.power(primitiveElement, i))
                .map(x -> polynomialRing.polynomial(field.negative(x), field.one()));

        Polynomial<E> checkPolynomial = polynomialRing.product(linearFactors);
        return polynomialRing.one().shift(n + k - 1).div(checkPolynomial);
    }

    @Override
    public MultiplicativeLinearCode<E, E> pow(int exponent) {
        return cachedCodePowers.computeIfAbsent(exponent, m ->
                new ReedSolomonCode<>(length(), Math.min(Math.max((dimension() - 1) * m + 1, 0), length()), field()));
    }

    @Override
    public Function<List<E>, List<E>> interpolationFn(SortedSet<Integer> knownSymbolIndices) {
        if (knownSymbolIndices.size() > dimension()) {
            throw new IllegalArgumentException("Overdetermined system of equations");
        }

        E generator = field().reciprocal(field().getPrimitiveElement());
        List<E> sinusoidal = Stream.iterate(field().one(), x -> field().product(x, generator))
                .limit(length())
                .collect(ImmutableList.toImmutableList());

        int[] knownSymbolIndexArray = knownSymbolIndices.stream().mapToInt(i -> i).toArray();

        Stream<List<E>> sinusoidalOffsets = IntStream.range(0, length())
                .mapToObj(i -> knownSymbolIndices.stream()
                        .map(j -> field().difference(sinusoidal.get(i), sinusoidal.get(j)))
                        .collect(ImmutableList.toImmutableList()));

        List<List<E>> scaledTransposedBasis = sinusoidalOffsets
                .map(v -> productsOfAllBarOne(v).collect(ImmutableList.toImmutableList()))
                .collect(ImmutableList.toImmutableList());

        List<E> scaleFactors = IntStream.range(0, knownSymbolIndexArray.length)
                .mapToObj(i -> scaledTransposedBasis.get(knownSymbolIndexArray[i]).get(i))
                .map(field()::reciprocal)
                .collect(ImmutableList.toImmutableList());

        List<List<E>> transposedBasis = scaledTransposedBasis.stream()
                .map(v -> Streams.zip(scaleFactors.stream(), v.stream(), field()::product)
                        .collect(ImmutableList.toImmutableList()))
                .collect(ImmutableList.toImmutableList());

        return u -> transposedBasis.stream()
                .map(v -> Streams.zip(u.stream(), v.stream(), field()::product)
                        .reduce(field().zero(), field()::sum))
                .collect(ImmutableList.toImmutableList());

//        ImmutableList.Builder<List<E>> basisBuilder = ImmutableList.builder();
//        List<E> nextBasisVector = Collections.nCopies(length(), field().one());
//
//        for (int i : knownSymbolIndices) {
//            basisBuilder.add(nextBasisVector);
//
//            E offset = sinusoidal.get(i);
//            nextBasisVector = Streams.zip(nextBasisVector.stream(), sinusoidal.stream(),
//                    (x, y) -> field().product(x, field().difference(y, offset)))
//                    .collect(ImmutableList.toImmutableList());
//        }
//        List<List<E>> basis = basisBuilder.build();
//        return super.interpolationFn(knownSymbolIndices);
    }

    private Stream<E> productsOfAllBarOne(List<E> elements) {
        int i = elements.indexOf(field().zero());
        if (i >= 0) {
            elements = new ArrayList<>(elements);
            elements.remove(i);
            E product = field().product(elements);
            return IntStream.range(0, elements.size() + 1).mapToObj(j -> i == j ? product : field().zero());
        } else {
            E product = field().product(elements);
            return elements.stream().map(x -> field().quotient(product, x));
        }
    }
}
