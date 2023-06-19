package securecompute.algebra;

import com.google.auto.value.AutoValue;
import securecompute.StreamUtils;

import java.util.Objects;
import java.util.stream.Stream;

public interface EuclideanDomain<E> extends Ring<E> {

    int size(E elt);

    default E abs(E elt) {
        return product(elt, invSignum(elt));
    }

    default E signum(E elt) {
        return invSignum(invSignum(elt));
    }

    E invSignum(E elt);

    default E div(E dividend, E divisor) {
        return divMod(dividend, divisor).getQuotient();
    }

    default E mod(E dividend, E divisor) {
        return divMod(dividend, divisor).getRemainder();
    }

    default DivModResult<E> divMod(E dividend, E divisor) {
        return DivModResult.of(div(dividend, divisor), mod(dividend, divisor));
    }

    default E gcd(E left, E right) {
        return gcdExt(left, right).getGcd();
    }

    default E lcm(E left, E right) {
        return abs(product(gcdExt(left, right).getLeftDivGcd(), right));
    }

    default GcdExtResult<E> gcdExt(E left, E right) {
        PartialGcdExtResult<E> r = partialGcdExtResults(this, left, right)
                .reduce((a, b) -> b)
                .orElseThrow(RuntimeException::new);

        E u = invSignum(r.left()), v = invSignum(u);
        E s = product(r.oddStep() ? r.s() : negative(r.s()), v);
        E t = product(r.oddStep() ? negative(r.t()) : r.t(), v);
        E x = product(r.x(), u);
        E y = product(r.y(), u);
        E gcd = product(r.left(), u);

        if (size(s) > 0) {
            DivModResult<? extends E> divModResult = divMod(x, s);
            x = divModResult.getRemainder();
            y = sum(y, product(t, divModResult.getQuotient()));
        }
        return GcdExtResult.of(x, y, gcd, t, s);
    }

    static <E> Stream<PartialGcdExtResult<E>> partialGcdExtResults(EuclideanDomain<E> ring, E left, E right) {
        E zero = ring.zero(), one = ring.one();
        return StreamUtils.iterate(PartialGcdExtResult.of(one, zero, zero, one, left, right, false), Objects::nonNull, r -> {
            if (ring.size(r.right()) == 0) {
                return null;
            }
            DivModResult<? extends E> divModResult = ring.divMod(r.left(), r.right());
            E left0 = r.right();
            E right0 = divModResult.getRemainder();

            E x = r.s();
            E s = ring.difference(r.x(), ring.product(r.s(), divModResult.getQuotient()));

            E y = r.t();
            E t = ring.difference(r.y(), ring.product(r.t(), divModResult.getQuotient()));

            return PartialGcdExtResult.of(x, y, s, t, left0, right0, !r.oddStep());
        });
    }

    @AutoValue
    abstract class DivModResult<E> {
        public abstract E getQuotient();

        public abstract E getRemainder();

        public static <E> DivModResult<E> of(E quotient, E remainder) {
            return new AutoValue_EuclideanDomain_DivModResult<>(quotient, remainder);
        }
    }

    @AutoValue
    abstract class PartialGcdExtResult<E> {
        public abstract E x();

        public abstract E y();

        public abstract E s();

        public abstract E t();

        public abstract E left();

        public abstract E right();

        public abstract boolean oddStep();

        public static <E> PartialGcdExtResult<E> of(E x, E y, E s, E t, E left, E right, boolean oddStep) {
            return new AutoValue_EuclideanDomain_PartialGcdExtResult<>(x, y, s, t, left, right, oddStep);
        }
    }

    @AutoValue
    abstract class GcdExtResult<E> {
        public abstract E getX();

        public abstract E getY();

        public abstract E getGcd();

        public abstract E getLeftDivGcd();

        public abstract E getRightDivGcd();

        public static <E> GcdExtResult<E> of(E x, E y, E gcd, E leftDivGcd, E rightDivGcd) {
            return new AutoValue_EuclideanDomain_GcdExtResult<>(x, y, gcd, leftDivGcd, rightDivGcd);
        }
    }
}
