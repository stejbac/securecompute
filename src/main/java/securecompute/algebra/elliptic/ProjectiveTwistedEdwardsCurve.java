package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import securecompute.Lazy;
import securecompute.algebra.Field;
import securecompute.algebra.FiniteField;

public class ProjectiveTwistedEdwardsCurve<E> extends AbstractP2PointCurve<E, ProjectiveTwistedEdwardsCurve.Point<E>> {
    private final E a, d;

    public ProjectiveTwistedEdwardsCurve(Field<E> field, E a, E d) {
        super(field, P2PointCoordinates.of(field.zero(), field.one(), field.one()));
        this.a = a;
        this.d = d;
    }

    public E getA() {
        return a;
    }

    public E getD() {
        return d;
    }

    @Override
    public Point<E> sum(Point<E> left, Point<E> right) {
        if (left == right) {
            return double_(left);
        }
        checkCurve(left);
        checkCurve(right);
        if (right == identity) {
            return left;
        }
        if (left == identity) {
            return right;
        }
        E x1 = left.coordinates().x(), y1 = left.coordinates().y(), z1 = left.coordinates().z();
        E x2 = right.coordinates().x(), y2 = right.coordinates().y(), z2 = right.coordinates().z();
        // Use the "add-2008-bbjlp" addition formulas from http://hyperelliptic.org/EFD/g1p/auto-twisted-projective.html:
        E a = p(z1, z2), b = sq(a), c = p(x1, x2), d = p(y1, y2);
        E e = p(p(c, d), this.d), f = d(b, e), g = s(b, e);
        E x3 = p(p(a, f), d(p(s(x1, y1), s(x2, y2)), s(c, d))), y3 = p(p(a, g), d(d, p(c, this.a))), z3 = p(f, g);
        return rawPoint(x3, y3, z3);
    }

    private Point<E> double_(Point<E> elt) {
        checkCurve(elt);
        E x1 = elt.coordinates().x(), y1 = elt.coordinates().y(), z1 = elt.coordinates().z();
        // Use the "dbl-2008-bbjlp" doubling formulas from http://hyperelliptic.org/EFD/g1p/auto-twisted-projective.html:
        E b = sq(s(x1, y1)), c = sq(x1), d = sq(y1);
        E e = p(c, a), f = s(e, d), h = sq(z1), j = d(f, p(h, 2));
        E x3 = p(d(b, s(c, d)), j), y3 = p(f, d(e, d)), z3 = p(f, j);
        return rawPoint(x3, y3, z3);
    }

    @Override
    P2PointCoordinates<E> negative(P2PointCoordinates<E> p) {
        return P2PointCoordinates.of(field.negative(p.x()), p.y(), p.z());
    }

    @Override
    int tableNormalizationThreshold() {
        // Threshold is based on the assumption that one field inversion (I) costs 32 field multiplications (M).
        // Converting the list to normal (affine) form should cost 31 * M + I but saves (numWindows - 1) * M by
        // allowing mixed additions instead of full addition. So we require numWindows > 64 (thus bitLength > 255).
        return 256;
    }

    @Override
    public boolean isCurvePoint(E x, E y, E z) {
        if (z.equals(field.zero())) {
            return false;
        }
        E xx = sq(x), yy = sq(y), zz = sq(z);
        return p(d(s(p(xx, a), yy), zz), zz).equals(p(p(xx, yy), d));
    }

    @Override
    boolean isNegative(ProjectiveTwistedEdwardsCurve.Point<E> elt) {
        E x = elt.normalCoordinates().get().x();
        return !field.plusMinus(x).getWitness().equals(x);
    }

    @Override
    Point<E> rawPoint(P2PointCoordinates<E> pointCoordinates) {
        return new AutoValue_ProjectiveTwistedEdwardsCurve_Point<>(new LazyCoordinates2(pointCoordinates), this);
    }

    @Override
    PlusMinusPoint<E> plusMinusPoint(P1PointCoordinates<E> p1Coordinates) {
        // x2 = (1 - y2) / (a - d * y2). So x = invSqrt((a - d * y2) * (1 - y2)) * (1 - y2).
        // So X = invSqrt((a * Z2 - d * Y2) * (Z2 - Y2) * Z2) * (Z2 - Y2) * Z2.
        Lazy<Point<E>> lazyWitness = Lazy.of(() -> {
            E y = p1Coordinates.x(), z = p1Coordinates.y(), zero = field.zero();
            if (z.equals(zero)) {
                return null;
            }
            E yy = sq(y), zz = sq(z);
            E r1 = d(p(zz, a), p(d, yy)), r2 = p(d(zz, yy), zz);
            if (r1.equals(zero)) {
                throw new ArithmeticException("Bad curve: Found square root of a/d: " + field.quotient(y, z));
            }
            if (r2.equals(zero)) {
                return rawPoint(zero, y, z);
            }
            if (!(field instanceof FiniteField)) {
                throw new UnsupportedOperationException("Cannot compute square root");
            }
            FiniteField<E> field = (FiniteField<E>) this.field;
            E x = field.product(field.invSqrt(p(r1, r2)), r2).getWitness();
            Point<E> witness;
            return x != null ? isNegative(witness = rawPoint(x, y, z)) ? witness.negate() : witness : null;
        });
        return new AutoValue_ProjectiveTwistedEdwardsCurve_PlusMinusPoint<>(new LazyCoordinates1(p1Coordinates), lazyWitness, this);
    }

    @Override
    public PlusMinusPoint<E> plusMinus(Point<E> elt) {
        P1PointCoordinates<E> p1Coordinates = P1PointCoordinates.of(elt.coordinates().y(), elt.coordinates().z());
        return new AutoValue_ProjectiveTwistedEdwardsCurve_PlusMinusPoint<>(new LazyCoordinates1(p1Coordinates), lazyAbs(elt), this);
    }

    @AutoValue
    public static abstract class Point<E> extends AbstractP2PointCurve.Point<E, Point<E>> {
        @Override
        public Point<E> cast() {
            return this;
        }

        @Override
        public abstract ProjectiveTwistedEdwardsCurve<E> getAbelianGroup();
    }

    @AutoValue
    public static abstract class PlusMinusPoint<E> extends AbstractP2PointCurve.PlusMinusPoint<E, Point<E>> {
        @Override
        public boolean isHalfZero() {
            E y = coordinates().x(), z = coordinates().y(), minus_z = getAbelianGroup().field.negative(z);
            return z.equals(minus_z) || y.equals(z) || y.equals(minus_z);
        }

        @Override
        public abstract ProjectiveTwistedEdwardsCurve<E> getAbelianGroup();
    }
}
