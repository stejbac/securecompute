package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import securecompute.Nullable;
import securecompute.algebra.AbelianGroup;
import securecompute.algebra.AbelianGroupElement;
import securecompute.algebra.Field;

public class WeierstrassCurve<E> implements AbelianGroup<WeierstrassCurve.Point<E>> {

    private final Field<E> field;
    private final E a, b;
    private final Point<E> infinity;

    public WeierstrassCurve(Field<E> field, E a, E b) {
        this.field = field;
        this.a = a;
        this.b = b;
        infinity = rawPoint(null, null);
    }

    public Field<E> getField() {
        return field;
    }

    public E getA() {
        return a;
    }

    public E getB() {
        return b;
    }

    @Override
    public Point<E> zero() {
        return infinity;
    }

    @Override
    public Point<E> sum(Point<E> u, Point<E> v) {
        checkCurve(u);
        checkCurve(v);
        if (v.equals(zero())) {
            return u;
        }
        if (u.equals(zero())) {
            return v;
        }
        if (u.equals(negative(v))) {
            return zero();
        }

        E slope;
        if (u.equals(v)) {
            slope = field.quotient(
                    field.sum(field.product(field.power(u.x(), 2), 3), a),
                    field.product(u.y(), 2)
            );
        } else {
            slope = field.quotient(field.difference(v.y(), u.y()), field.difference(v.x(), u.x()));
        }
        E x3 = field.difference(field.difference(field.power(slope, 2), u.x()), v.x());
        E y3 = field.difference(field.product(slope, field.difference(u.x(), x3)), u.y());
        return rawPoint(x3, y3);

//        E slope = using(field, u.x(), u.y(), v.x(), v.y(), a, (x1, y1, x2, y2, a) ->
//                u.equals(v)
//                        ? x1.pow(2).multiply(3).add(a).divide(y1.multiply(2))
//                        : y2.subtract(y1).divide(x2.subtract(x1))
//        );
//        E x = using(field, u.x(), v.x(), slope, (x1, x2, s) ->
//                s.pow(2).subtract(x1).subtract(x2)
//        );
//        E y = using(field, u.x(), x, u.y(), slope, (x1, x3, y1, s) ->
//                s.multiply(x1.subtract(x3)).subtract(y1)
//        );
//        return rawPoint(x, y);
    }

    @Override
    public Point<E> negative(Point<E> v) {
        checkCurve(v);
        return v.equals(zero()) ? zero() : rawPoint(v.x(), field.negative(v.y()));
    }

    private void checkCurve(Point<E> v) {
        if (!equals(v.getAbelianGroup())) {
            throw new IllegalArgumentException("Curve mismatch");
        }
    }

    public boolean isCurvePoint(E x, E y) {
        if ((x == null) != (y == null)) {
            return false;
        }
        if (y == null) {
            return true;
        }
        E cubic = field.sum(field.product(x, field.sum(field.power(x, 2), a)), b);
        return cubic.equals(field.power(y, 2));
    }

    public Point<E> point(E x, E y) {
        if (!isCurvePoint(x, y)) {
            throw new IllegalArgumentException("Invalid curve point: (" + x + ", " + y + ")");
        }
        return y != null ? rawPoint(x, y) : infinity;
    }

    private Point<E> rawPoint(E x, E y) {
        return new AutoValue_WeierstrassCurve_Point<>(this, x, y);
    }

    @AutoValue
    public static abstract class Point<E> implements AbelianGroupElement<Point<E>> {
        @Override
        public abstract WeierstrassCurve<E> getAbelianGroup();

        @Nullable
        public abstract E x();

        @Nullable
        public abstract E y();

        @Override
        public Point<E> cast() {
            return this;
        }

        @Override
        public String toString() {
            return y() != null ? "(" + x() + ", " + y() + ")" : "Infinity";
        }
    }

//    private static <E> Wrap<E> wrap(Field<E> field, E elt) {
//        return new Wrap<E>() {
//            @Override
//            E unwrap() {
//                return elt;
//            }
//
//            @Override
//            public Field<Wrap<E>> getField() {
//                return new Field<Wrap<E>>() {
//                    @Override
//                    public Wrap<E> reciprocalOrZero(Wrap<E> elt) {
//                        return wrap(field, field.reciprocalOrZero(elt.unwrap()));
//                    }
//
//                    @Override
//                    public Wrap<E> fromBigInteger(BigInteger n) {
//                        return wrap(field, field.fromBigInteger(n));
//                    }
//
//                    @Override
//                    public Wrap<E> product(Wrap<E> left, Wrap<E> right) {
//                        return wrap(field, field.product(left.unwrap(), right.unwrap()));
//                    }
//
//                    @Override
//                    public Wrap<E> sum(Wrap<E> left, Wrap<E> right) {
//                        return wrap(field, field.sum(left.unwrap(), right.unwrap()));
//                    }
//
//                    @Override
//                    public Wrap<E> negative(Wrap<E> elt) {
//                        return wrap(field, field.negative(elt.unwrap()));
//                    }
//                };
//            }
//        };
//    }
//
//    private static abstract class Wrap<E> implements FieldElement<Wrap<E>> {
//        @Override
//        public Wrap<E> cast() {
//            return this;
//        }
//
//        abstract E unwrap();
//    }
//
//    private interface TernaryOperator<T> {
//        T apply(T x1, T x2, T x3);
//    }
//
//    private interface QuaternaryOperator<T> {
//        T apply(T x1, T x2, T x3, T x4);
//    }
//
//    private interface QuinternaryOperator<T> {
//        T apply(T x1, T x2, T x3, T x4, T x5);
//    }
//
//    private static <E> E using(Field<E> field, E x, UnaryOperator<Wrap<E>> op) {
//        return op.apply(wrap(field, x)).unwrap();
//    }
//
//    private static <E> E using(Field<E> field, E x1, E x2, BinaryOperator<Wrap<E>> op) {
//        return op.apply(wrap(field, x1), wrap(field, x2)).unwrap();
//    }
//
//    private static <E> E using(Field<E> field, E x1, E x2, E x3, TernaryOperator<Wrap<E>> op) {
//        return op.apply(wrap(field, x1), wrap(field, x2), wrap(field, x3)).unwrap();
//    }
//
//    private static <E> E using(Field<E> field, E x1, E x2, E x3, E x4, QuaternaryOperator<Wrap<E>> op) {
//        return op.apply(wrap(field, x1), wrap(field, x2), wrap(field, x3), wrap(field, x4)).unwrap();
//    }
//
//    private static <E> E using(Field<E> field, E x1, E x2, E x3, E x4, E x5, QuinternaryOperator<Wrap<E>> op) {
//        return op.apply(wrap(field, x1), wrap(field, x2), wrap(field, x3), wrap(field, x4), wrap(field, x5)).unwrap();
//    }
}
