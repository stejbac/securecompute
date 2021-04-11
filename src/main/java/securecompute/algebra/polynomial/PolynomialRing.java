package securecompute.algebra.polynomial;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import securecompute.algebra.Ring;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PolynomialRing<E> implements IPolynomialRing<E> {

    // FIXME: Implement 'equals()' & 'hashCode()' for this ring structure.

    private final Ring<E> baseRing;
    private final Element zero, one;

    public PolynomialRing(Ring<E> baseRing) {
        this.baseRing = baseRing;
        this.zero = new Element(ImmutableList.of());
        this.one = new Element(ImmutableList.of(baseRing.one()));
    }

    @Override
    public Ring<E> getBaseRing() {
        return baseRing;
    }

    @Override
    public Element fromBigInteger(BigInteger n) {
        return polynomial(baseRing.fromBigInteger(n));
    }

    @Override
    public Element zero() {
        return zero;
    }

    @Override
    public Element one() {
        return one;
    }

    @Override
    public Element sum(Polynomial<E> left, Polynomial<E> right) {
        return sumShifted(left, right, 0, 0);
    }

    private Element sumShifted(Polynomial<E> left, Polynomial<E> right, int leftShift, int rightShift) {
        List<E> leftCoefficients = left.getCoefficients();
        List<E> rightCoefficients = right.getCoefficients();

        int finalSize = Math.max(leftShift + leftCoefficients.size(), rightShift + rightCoefficients.size());
        ImmutableList.Builder<E> builder = ImmutableList.builderWithExpectedSize(finalSize);

        for (int i = 0; i < finalSize; i++) {
            boolean inLeftRange = i >= leftShift && i < leftShift + leftCoefficients.size();
            boolean inRightRange = i >= rightShift && i < rightShift + rightCoefficients.size();
            if (inLeftRange) {
                builder.add(inRightRange
                        ? baseRing.sum(leftCoefficients.get(i - leftShift), rightCoefficients.get(i - rightShift))
                        : leftCoefficients.get(i - leftShift));
            } else {
                builder.add(inRightRange
                        ? rightCoefficients.get(i - rightShift)
                        : baseRing.zero());
            }
        }

        return polynomial(builder.build());
    }

    @Override
    public Element product(Polynomial<E> left, Polynomial<E> right) {
        // TODO: Use Karatsuba multiplication rather than this naive O(n^2) divide & conquer method ...
        if (left.getDegree() < 0 || right.getDegree() < 0) {
            return zero;
        }
        if (left.getDegree() == 0) {
            E leftElt = left.getCoefficients().get(0);
            return polynomial(right.getCoefficients().stream()
                    .map(elt -> baseRing.product(leftElt, elt))
                    .collect(ImmutableList.toImmutableList()));
        }
        if (right.getDegree() == 0) {
            E rightElt = right.getCoefficients().get(0);
            return polynomial(left.getCoefficients().stream()
                    .map(elt -> baseRing.product(elt, rightElt))
                    .collect(ImmutableList.toImmutableList()));
        }

        List<E> leftCoefficients = left.getCoefficients();
        int leftShift = leftCoefficients.size() / 2;

        Element leftHalfA = polynomial(leftCoefficients.subList(0, leftShift));
        Element leftHalfB = polynomial(leftCoefficients.subList(leftShift, leftCoefficients.size()));

        return sumShifted(leftHalfA.multiply(right), leftHalfB.multiply(right), 0, leftShift);
    }

    @Override
    public Polynomial<E> scalarProduct(Polynomial<E> left, E right) {
        return left.multiply(polynomial(right));
    }

    @Override
    public Element negative(Polynomial<E> elt) {
        return polynomial(elt.getCoefficients().stream()
                .map(baseRing::negative)
                .collect(ImmutableList.toImmutableList()));
    }

    @Override
    public Element polynomial(List<E> coefficients) {
        coefficients = ImmutableList.copyOf(coefficients);
        int degree = Lists.transform(coefficients, baseRing.zero()::equals).lastIndexOf(false);
        return degree < 0
                ? zero : degree == 0 && baseRing.one().equals(coefficients.get(0))
                ? one : new Element(coefficients.subList(0, degree + 1));
    }

    @Override
    @SafeVarargs
    public final Element polynomial(E... coefficients) {
        return polynomial(ImmutableList.copyOf(coefficients));
    }

    @Override
    public Polynomial<E> shift(Polynomial<E> elt, int n) {
        if (n == 0) {
            return elt;
        }
        List<E> coefficients = elt.getCoefficients();
        int size = coefficients.size();
        if (size == 0 || n <= -size) {
            return zero;
        }
        if (n < 0) {
            coefficients = coefficients.subList(-n, size);
        } else {
            coefficients = ImmutableList.<E>builderWithExpectedSize(n + size)
                    .addAll(Collections.nCopies(n, baseRing.zero()))
                    .addAll(coefficients)
                    .build();
        }
        return polynomial(coefficients);
    }

    public class Element implements Polynomial<E> {

        private final List<E> coefficients;

        // TODO: Should we make the coefficient list big-endian instead?

        private Element(List<E> coefficients) {
            this.coefficients = coefficients;
        }

        @Override
        public List<E> getCoefficients() {
            return coefficients;
        }

        @Override
        public int getDegree() {
            return getCoefficients().size() - 1;
        }

        @Override
        public PolynomialRing<E> getModule() {
            return PolynomialRing.this;
        }

        @Override
        public Element cast() {
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof PolynomialRing.Element &&
                    baseRing.equals(((PolynomialRing<?>.Element) obj).getRing().getBaseRing()) &&
                    coefficients.equals(((PolynomialRing<?>.Element) obj).coefficients);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRing(), coefficients);
        }

        @Override
        public String toString() {
            if (coefficients.isEmpty()) {
                return "0";
            }
            if (coefficients.size() == 1) {
                return coefficients.get(0).toString();
            }
            return Streams.mapWithIndex(coefficients.stream(),
                    (elt, i) -> asTerm(elt) + (i == 0 ? "" : i == 1 ? "*X" : "*X^" + i))
                    .collect(Collectors.joining(" + "));
            // "0 + 1*X + 2*X^2 + 10*X^3 + (-3)*X^4"
        }
    }

    private static <E> String asTerm(E elt) {
        String eltStr = elt.toString();
        // FIXME: This test for requiring brackets is imperfect:
        return eltStr.contains("+") || eltStr.contains("-") ? "(" + eltStr + ")" : eltStr;
    }
}
