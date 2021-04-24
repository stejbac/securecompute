package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.Lazy;
import securecompute.algebra.AbelianGroup;
import securecompute.algebra.AbelianGroupElement;
import securecompute.algebra.Field;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
        if (left == right) {
            return double_(left);
        }
        checkCurve(left);
        checkCurve(right);
        if (right == infinity) {
            return left;
        }
        if (left == infinity) {
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

    private E s(E left, E right) {
        return field.sum(left, right);
    }

    private E d(E left, E right) {
        return field.difference(left, right);
    }

    private E p(E left, E right) {
        return field.product(left, right);
    }

    private E p(E elt, long k) {
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

    private P2PointCoordinates<E> n(P2PointCoordinates<E> p) {
        return P2PointCoordinates.of(p.x(), field.negative(p.y()), p.z());
    }

    @Override
    public Point<E> product(Point<E> elt, BigInteger k) {
        int bitLength = k.abs().bitLength();
        // Use a lower 4-bit window threshold than super does, since a balanced table costs half as much to build.
        if (bitLength < 32) {
            return AbelianGroup.super.product(elt, k);
        }
        Point<E> p2, p3, p4;
        List<P2PointCoordinates<E>> list = Stream.of(
                infinity, elt, p2 = elt.add(elt), p3 = elt.add(p2), p4 = p2.add(p2), p2.add(p3), p3.add(p3), p3.add(p4), p4.add(p4)
        ).map(Point::coordinates).collect(ImmutableList.toImmutableList());

        // Threshold is based on the assumption that one field inversion (I) costs 32 field multiplications (M).
        // Converting the list to normal (affine) form should cost 31 * M + I but saves (numWindows - 1) * 3 * M by
        // allowing mixed additions instead of full addition. So we require numWindows > 22 (thus bitLength > 87).
        if (bitLength > 87) {
            list = P2PointCoordinates.normalForms(field, list);
        }

        List<Point<E>> table = Stream.of(
                n(list.get(8)), n(list.get(7)), n(list.get(6)), n(list.get(5)),
                n(list.get(4)), n(list.get(3)), n(list.get(2)), n(list.get(1)),
                list.get(0), list.get(1), list.get(2), list.get(3), list.get(4), list.get(5), list.get(6), list.get(7)
        ).map(this::rawPoint).collect(ImmutableList.toImmutableList());

        return AbelianGroup.windowedProduct(this, table, k.add(offset(bitLength)));
    }

    private static BigInteger offset(int bitLength) {
        int numWindows = (bitLength + 5) / 4;
        byte[] bytes = new byte[numWindows / 2 + 1];
        bytes[0] = (numWindows & 1) != 0 ? (byte) 0x08 : (byte) 0x00;
        Arrays.fill(bytes, 1, bytes.length, (byte) 0x88);
        return new BigInteger(bytes);
    }

    @Override
    public Point<E> select(int index, List<Point<E>> elements) {
        //noinspection ConstantConditions
        E x = field.select(index, Lists.transform(elements, p -> p.coordinates().x()));
        //noinspection ConstantConditions
        E y = field.select(index, Lists.transform(elements, p -> p.coordinates().y()));
        //noinspection ConstantConditions
        E z = field.select(index, Lists.transform(elements, p -> p.coordinates().z()));
        return rawPoint(x, y, z);
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
        return rawPoint(P2PointCoordinates.of(x, y, z));
    }

    private Point<E> rawPoint(P2PointCoordinates<E> pointCoordinates) {
        return new AutoValue_ProjectiveWeierstrassCurve_Point<>(this, new LazyCoordinates(pointCoordinates));
    }

    @AutoValue
    public static abstract class Point<E> implements AbelianGroupElement<Point<E>> {
        private P2PointCoordinates<E> coordinates() {
            return ((ProjectiveWeierstrassCurve<E>.LazyCoordinates) normalCoordinates()).originalCoordinates;
        }

        @Override
        public Point<E> cast() {
            return this;
        }

        @Override
        public abstract ProjectiveWeierstrassCurve<E> getAbelianGroup();

        public abstract Lazy<P2PointCoordinates<E>> normalCoordinates();

        @Override
        public String toString() {
            return normalCoordinates().toString();
        }
    }

    private class LazyCoordinates extends Lazy<P2PointCoordinates<E>> {
        private final P2PointCoordinates<E> originalCoordinates;

        LazyCoordinates(P2PointCoordinates<E> originalCoordinates) {
            this.originalCoordinates = originalCoordinates;
        }

        @Override
        protected P2PointCoordinates<E> compute() {
            return originalCoordinates.normalForm(field);
        }
    }
}
