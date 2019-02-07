package securecompute.constraint.cyclic;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.Field;
import securecompute.algebra.polynomial.IFieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.constraint.LinearCode;

import java.util.Collections;
import java.util.List;

// TODO: Is there a natural way to generalise this to 'extended' cyclic codes, e.g. the extended RS, Golay & Hamming codes?

public class PuncturedPolynomialCode<E> implements LinearCode<E, E> {

    private final IFieldPolynomialRing<E> polynomialRing;
    private final SingletonVectorSpace<E> symbolSpace;
    private final int length;
    private final int dimension;
    private final int distance;
    private final int codistance;
    private final Polynomial<E> generatorPolynomial;
    private final Polynomial<E> checkPolynomial;
    private final Polynomial<E> truncatedCheckPolynomial;
    private final int punctureNumber;
    private final int shortenNumber;

    public PuncturedPolynomialCode(int n, int k, int d, int e, Polynomial<E> generatorPolynomial) {
        if (generatorPolynomial.getDegree() < 0) {
            throw new IllegalArgumentException("Generator polynomial is zero");
        }
        if (k < 0) {
            throw new IllegalArgumentException("Negative code dimension k");
        }
        if (n < k) {
            throw new IllegalArgumentException("Length n is less than the code dimension k");
        }
        if (d <= 0) {
            throw new IllegalArgumentException("Negative or zero distance d");
        }
        if (e <= 0) {
            throw new IllegalArgumentException("Negative or zero codistance e");
        }
        if (d > n - k + 1) {
            throw new IllegalArgumentException("Singleton bound violation: d > n - k + 1");
        }
        if (e > k + 1) {
            throw new IllegalArgumentException("Singleton bound violation: e > k + 1");
        }

        this.length = n;
        this.dimension = k; // n - generatorPolynomial.getDegree();
        this.distance = d;
        this.codistance = e;

        this.generatorPolynomial = generatorPolynomial = normalise(generatorPolynomial, n - 1);
        this.polynomialRing = generatorPolynomial.getEuclideanDomain();
        this.symbolSpace = new SingletonVectorSpace<>(polynomialRing.getBaseRing());

        int a = generatorPolynomial.getDegree() + n - 1;
        checkPolynomial = normalise(polynomialRing.one().shift(a).div(generatorPolynomial), n - 1);
        truncatedCheckPolynomial = checkPolynomial.shift(k - checkPolynomial.getDegree());

        punctureNumber = generatorPolynomial.getDegree() + k - n;
        shortenNumber = checkPolynomial.getDegree() - k;

        if (punctureNumber < 0) {
            throw new IllegalArgumentException("Negative puncture number: normalised generator polynomial too short");
        }
        if (shortenNumber < 0) {
            throw new IllegalArgumentException("Negative shorten number: normalised check polynomial too short");
        }

//        Polynomial<E> basePolynomial = polynomialRing.one().shift(n).subtract(polynomialRing.one()); // X**n - 1;
//
//        DivModResult<Polynomial<E>> divModResult = basePolynomial.divMod(generatorPolynomial);
//        if (!divModResult.getRemainder().equals(polynomialRing.zero())) {
//            throw new IllegalArgumentException("Not a cyclic code: generator must divide X**n - 1");
//        }
//        checkPolynomial = divModResult.getQuotient();
    }

    private static <E> Polynomial<E> normalise(Polynomial<E> p, int maxDegree) {
        p = p.shift(maxDegree - p.getDegree());
        p = p.shift(-p.getOrder());
        Field<E> baseField = p.getEuclideanDomain().getBaseRing();
        return p.scalarMultiply(baseField.reciprocal(p.getCoefficients().get(p.getDegree())));
    }

    public Polynomial<E> generatorPolynomial() {
        return generatorPolynomial;
    }

    public Polynomial<E> checkPolynomial() {
        return checkPolynomial;
    }

    public int punctureNumber() {
        return punctureNumber;
    }

    public int shortenNumber() {
        return shortenNumber;
    }

    @Override
    public boolean isSystematic() {
        return true;
    }

    @Override
    public SingletonVectorSpace<E> symbolSpace() {
        return symbolSpace;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public int codistance() {
        return codistance;
    }

    private static <E> List<E> paddedCoefficients(int size, Polynomial<E> polynomial) {
        List<E> coefficients = polynomial.getCoefficients();

        return coefficients.size() >= size ? coefficients.subList(0, size) : ImmutableList.<E>builderWithExpectedSize(size)
                .addAll(coefficients)
                .addAll(Collections.nCopies(size - coefficients.size(), polynomial.getRing().getBaseRing().zero()))
                .build();
    }

    @Override
    public List<E> encode(List<E> message) {
        return paddedCoefficients(length,
                polynomialRing.polynomial(message)
                        .multiply(truncatedCheckPolynomial)
                        .shift(-dimension)
                        .multiply(generatorPolynomial)
                        .shift(-punctureNumber)
        );
    }

    @Override
    public List<E> decode(List<E> codeword) {
        return codeword.subList(length - dimension, length);
    }

    @Override
    public List<E> parityCheck(List<E> vector) {
        return paddedCoefficients(length - dimension,
                polynomialRing.polynomial(vector)
                        .multiply(checkPolynomial)
                        .shift(-checkPolynomial.getDegree())
        );
    }
}
