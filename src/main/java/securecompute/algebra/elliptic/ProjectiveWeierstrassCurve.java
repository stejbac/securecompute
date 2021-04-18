package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import securecompute.algebra.AbelianGroup;
import securecompute.algebra.AbelianGroupElement;
import securecompute.algebra.Field;
import securecompute.Lazy;

public class ProjectiveWeierstrassCurve<E> implements AbelianGroup<ProjectiveWeierstrassCurve.Point<E>> {

    private final Field<E> field;
    private final E a, b;
    private final Point<E> infinity;

    public ProjectiveWeierstrassCurve(Field<E> field, E a, E b) {
        this.field = field;
        this.a = a;
        this.b = b;
        infinity = rawPoint(field.zero(), field.one(), field.zero());
    }

    public Field<E> getField() {
        return field;
    }

    @Override
    public Point<E> zero() {
        return infinity;
    }

    @Override
    public Point<E> sum(Point<E> left, Point<E> right) {
        checkCurve(left);
        checkCurve(right);
        E x1 = left.coordinates().x(), y1 = left.coordinates().y(), z1 = left.coordinates().z();
        E x2 = right.coordinates().x(), y2 = right.coordinates().y(), z2 = right.coordinates().z();
        if (z2.equals(field.zero())) {
            return left;
        }
        if (z1.equals(field.zero())) {
            return right;
        }
        E sA = d(p(y2, z1), p(y1, z2));
        E sB = d(p(x2, z1), p(x1, z2));
        if (sA.equals(field.zero()) && sB.equals(field.zero())) {
            sA = s(p(sq(x1), 3), p(sq(z1), a));
            sB = p(p(y1, z1), 2);
        }
        E sB2 = sq(sB), sB2_z2 = p(sB2, z2), sB3_z2 = p(sB, sB2_z2);
        E u = d(p(p(sq(sA), z1), z2), p(sB2, s(p(x1, z2), p(x2, z1))));
        E x3 = p(sB, u);
        E y3 = d(p(sA, d(p(x1, sB2_z2), u)), p(y1, sB3_z2));
        E z3 = p(sB3_z2, z1);
        return rawPoint(x3, y3, z3);
    }

    private E s(E left, E right) {
        return field.sum(left, right);
    }

    private E d(E left, E right) {
        return field.difference(left, right);
    }

    private E p(E left, E right) {
        return field.product(left, right);
    }

    private E p(E elt, int k) {
        return field.product(elt, k);
    }

    private E sq(E elt) {
        return field.power(elt, 2);
    }

    @Override
    public Point<E> negative(Point<E> elt) {
        checkCurve(elt);
        return rawPoint(elt.coordinates().x(), field.negative(elt.coordinates().y()), elt.coordinates().z());
    }

    private void checkCurve(ProjectiveWeierstrassCurve.Point<E> v) {
        if (!equals(v.getAbelianGroup())) {
            throw new IllegalArgumentException("Curve mismatch");
        }
    }

    public boolean isCurvePoint(E x, E y, E z) {
        E z2 = sq(z), z3 = p(z, z2);
        E cubic = s(p(x, s(sq(x), p(z2, a))), p(z3, b));
        return cubic.equals(p(sq(y), z));
    }

    public Point<E> point(E x, E y, E z) {
        if (!isCurvePoint(x, y, z)) {
            throw new IllegalArgumentException("Invalid curve point: (" + x + " : " + y + " : " + z + ")");
        }
        return rawPoint(x, y, z);
    }

    private Point<E> rawPoint(E x, E y, E z) {
        return new AutoValue_ProjectiveWeierstrassCurve_Point<>(this, new LazyCoordinates(
                new AutoValue_ProjectiveWeierstrassCurve_PointCoordinates<>(x, y, z)
        ));
    }

    @AutoValue
    public static abstract class Point<E> implements AbelianGroupElement<Point<E>> {
        private PointCoordinates<E> coordinates() {
            return ((ProjectiveWeierstrassCurve<E>.LazyCoordinates) normalCoordinates()).originalCoordinates;
        }

        @Override
        public Point<E> cast() {
            return this;
        }

        @Override
        public abstract ProjectiveWeierstrassCurve<E> getAbelianGroup();

        public abstract Lazy<PointCoordinates<E>> normalCoordinates();

        @Override
        public String toString() {
            return normalCoordinates().toString();
        }
    }

    @AutoValue
    public static abstract class PointCoordinates<E> {
        public abstract E x();

        public abstract E y();

        public abstract E z();

        @Override
        public String toString() {
            return "(" + x() + " : " + y() + " : " + z() + ")";
        }
    }

    private class LazyCoordinates extends Lazy<PointCoordinates<E>> {
        private final PointCoordinates<E> originalCoordinates;

        LazyCoordinates(PointCoordinates<E> originalCoordinates) {
            this.originalCoordinates = originalCoordinates;
        }

        @Override
        protected PointCoordinates<E> compute() {
            E x = originalCoordinates.x(), y = originalCoordinates.y(), z = originalCoordinates.z();
            E r = field.reciprocal(
                    z.equals(field.zero()) ? y.equals(field.zero()) ? x : y : z
            );
            return new AutoValue_ProjectiveWeierstrassCurve_PointCoordinates<>(
                    field.product(x, r), field.product(y, r), field.product(z, r)
            );
        }
    }
}
