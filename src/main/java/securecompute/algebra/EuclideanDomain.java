package securecompute.algebra;

import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.Objects;

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
        return new DivModResult<>(div(dividend, divisor), mod(dividend, divisor));
    }

    default E gcd(E left, E right) {
        return gcdExt(left, right).gcd;
    }

    default E lcm(E left, E right) {
        return abs(product(gcdExt(left, right).leftDivGcd, right));
    }

    default GcdExtResult<E> gcdExt(E left, E right) {
        E x = one(), y = zero(), s = y, t = x;
        boolean oddSteps = false;
        while (size(right) > 0) {
            oddSteps ^= true;

            DivModResult<? extends E> divModResult = divMod(left, right);
            left = right;
            right = divModResult.remainder;

            E oldX = x;
            x = s;
            s = difference(oldX, product(s, divModResult.quotient));

            E oldY = y;
            y = t;
            t = difference(oldY, product(t, divModResult.quotient));
        }
        E u = invSignum(left), v = invSignum(u);
        s = product(oddSteps ? s : negative(s), v);
        t = product(oddSteps ? negative(t) : t, v);
        x = product(x, u);
        y = product(y, u);
        left = product(left, u);

        if (size(s) > 0) {
            DivModResult<? extends E> divModResult = divMod(x, s);
            x = divModResult.remainder;
            y = sum(y, product(t, divModResult.quotient));
        }
        return new GcdExtResult<>(x, y, left, t, s);
    }

    final class DivModResult<E> {

        private final E quotient, remainder;

        public DivModResult(E quotient, E remainder) {
            this.quotient = quotient;
            this.remainder = remainder;
        }

        public E getQuotient() {
            return quotient;
        }

        public E getRemainder() {
            return remainder;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof DivModResult &&
                    Objects.equals(quotient, ((DivModResult<?>) obj).quotient) &&
                    Objects.equals(remainder, ((DivModResult<?>) obj).remainder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(quotient, remainder);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("quotient", quotient)
                    .add("remainder", remainder)
                    .toString();
        }
    }

    final class GcdExtResult<E> {

        private final E x, y, gcd, leftDivGcd, rightDivGcd;

        public GcdExtResult(E x, E y, E gcd, E leftDivGcd, E rightDivGcd) {
            this.x = x;
            this.y = y;
            this.gcd = gcd;
            this.leftDivGcd = leftDivGcd;
            this.rightDivGcd = rightDivGcd;
        }

        public E getX() {
            return x;
        }

        public E getY() {
            return y;
        }

        public E getGcd() {
            return gcd;
        }

        public E getLeftDivGcd() {
            return leftDivGcd;
        }

        public E getRightDivGcd() {
            return rightDivGcd;
        }

        private Object[] allFields() {
            return new Object[]{x, y, gcd, leftDivGcd, rightDivGcd};
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof GcdExtResult &&
                    Arrays.equals(allFields(), ((GcdExtResult<?>) obj).allFields());
        }

        @Override
        public int hashCode() {
            return Objects.hash(allFields());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("x", x)
                    .add("y", y)
                    .add("gcd", gcd)
                    .add("leftDivGcd", leftDivGcd)
                    .add("rightDivGcd", rightDivGcd)
                    .toString();
        }
    }
}
