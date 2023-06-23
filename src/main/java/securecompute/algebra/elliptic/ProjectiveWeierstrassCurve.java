package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import securecompute.Lazy;
import securecompute.algebra.Field;
import securecompute.algebra.FiniteField;

public class ProjectiveWeierstrassCurve<E> extends AbstractP2PointCurve<E, ProjectiveWeierstrassCurve.Point<E>> {
    private final E a, b;

    public ProjectiveWeierstrassCurve(Field<E> field, E a, E b) {
        super(field, P2PointCoordinates.of(field.zero(), field.one(), field.zero()));
        this.a = a;
        this.b = b;
    }

    public E getA() {
        return a;
    }

    public E getB() {
        return b;
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
        if (z2.equals(field.zero())) {
            return left;
        }
        if (z1.equals(field.zero())) {
            return right;
        }
        // Use the "add-1998-cmo-2" addition formulas from http://hyperelliptic.org/EFD/g1p/auto-shortw-projective.html:
        E y1z2 = p(y1, z2), x1z2 = p(x1, z2);
        E u = d(p(y2, z1), y1z2), v = d(p(x2, z1), x1z2);
        if (u.equals(field.zero()) && v.equals(field.zero())) {
            return double_(left);
        }
        E uu = sq(u), vv = sq(v), vvv = p(v, sq(v)), z1z2 = p(z1, z2);
        E r = p(vv, x1z2), a = d(p(uu, z1z2), s(vvv, p(r, 2)));
        E x3 = p(v, a), y3 = d(p(u, d(r, a)), p(vvv, y1z2)), z3 = p(vvv, z1z2);
        return rawPoint(x3, y3, z3);
    }

    private Point<E> double_(Point<E> elt) {
        checkCurve(elt);
        E x1 = elt.coordinates().x(), y1 = elt.coordinates().y(), z1 = elt.coordinates().z();
        if (z1.equals(field.zero())) {
            return identity;
        }
        // Use the "dbl-2007-bl" doubling formulas from http://hyperelliptic.org/EFD/g1p/auto-shortw-projective.html:
        E xx = sq(x1), zz = sq(z1);
        E w = s(p(zz, a), p(xx, 3));
        E s = p(p(y1, z1), 2), ss = sq(s), sss = p(s, ss);
        E r = p(y1, s), rr = sq(r);
        E b = d(sq(s(x1, r)), s(xx, rr));
        E h = d(sq(w), p(b, 2));
        E x3 = p(h, s), y3 = d(p(w, d(b, h)), p(rr, 2));
        return rawPoint(x3, y3, sss);
    }

    @Override
    P2PointCoordinates<E> negative(P2PointCoordinates<E> p) {
        return P2PointCoordinates.of(p.x(), field.negative(p.y()), p.z());
    }

    @Override
    int tableNormalizationThreshold() {
        // Threshold is based on the assumption that one field inversion (I) costs 32 field multiplications (M).
        // Converting the list to normal (affine) form should cost 31 * M + I but saves (numWindows - 1) * 3 * M by
        // allowing mixed additions instead of full addition. So we require numWindows > 22 (thus bitLength > 87).
        return 88;
    }

    private E cubic(E x, E z) {
        E z2 = sq(z), z3 = p(z, z2);
        return s(p(x, s(sq(x), p(z2, a))), p(z3, b));
    }

    @Override
    public boolean isCurvePoint(E x, E y, E z) {
        return (!z.equals(field.zero()) || !y.equals(field.zero())) && cubic(x, z).equals(p(sq(y), z));
    }

    @Override
    boolean isNegative(Point<E> elt) {
        E y = elt.normalCoordinates().get().y();
        return !field.plusMinus(y).getWitness().equals(y) && !elt.equals(zero());
    }

    @Override
    Point<E> rawPoint(P2PointCoordinates<E> pointCoordinates) {
        return new AutoValue_ProjectiveWeierstrassCurve_Point<>(new LazyCoordinates2(pointCoordinates), this);
    }

    @Override
    PlusMinusPoint<E> plusMinusPoint(P1PointCoordinates<E> p1Coordinates) {
        Lazy<Point<E>> lazyWitness = Lazy.of(() -> {
            E x = p1Coordinates.x(), z = p1Coordinates.y(), zero = field.zero();
            if (z.equals(zero)) {
                return x.equals(zero) ? null : zero();
            }
            E cubic = cubic(x, z);
            if (cubic.equals(zero)) {
                return rawPoint(x, zero, z);
            }
            if (!(field instanceof FiniteField)) {
                throw new UnsupportedOperationException("Cannot compute square root");
            }
            FiniteField<E> field = (FiniteField<E>) this.field;
            E y = field.product(field.invSqrt(p(z, cubic)), cubic).getWitness();
            Point<E> witness;
            return y != null ? isNegative(witness = rawPoint(x, y, z)) ? witness.negate() : witness : null;
        });
        return new AutoValue_ProjectiveWeierstrassCurve_PlusMinusPoint<>(new LazyCoordinates1(p1Coordinates), lazyWitness, this);
    }

    @Override
    public PlusMinusPoint<E> plusMinus(Point<E> elt) {
        P1PointCoordinates<E> p1Coordinates = elt.coordinates().z().equals(field.zero())
                ? P1PointCoordinates.of(field.one(), field.zero())
                : P1PointCoordinates.of(elt.coordinates().x(), elt.coordinates().z());
        return new AutoValue_ProjectiveWeierstrassCurve_PlusMinusPoint<>(new LazyCoordinates1(p1Coordinates), lazyAbs(elt), this);
    }

    @AutoValue
    public static abstract class Point<E> extends AbstractP2PointCurve.Point<E, Point<E>> {
        @Override
        public Point<E> cast() {
            return this;
        }

        @Override
        public abstract ProjectiveWeierstrassCurve<E> getAbelianGroup();
    }

    @AutoValue
    public static abstract class PlusMinusPoint<E> extends AbstractP2PointCurve.PlusMinusPoint<E, Point<E>> {
        @Override
        public boolean isHalfZero() {
            E x = coordinates().x(), z = coordinates().y();
            E quartic = getAbelianGroup().p(z, getAbelianGroup().cubic(x, z));
            return quartic.equals(getAbelianGroup().field.negative(quartic));
        }

        @Override
        public abstract ProjectiveWeierstrassCurve<E> getAbelianGroup();
    }
}
