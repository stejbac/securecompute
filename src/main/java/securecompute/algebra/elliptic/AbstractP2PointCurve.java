package securecompute.algebra.elliptic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.Lazy;
import securecompute.algebra.AbelianGroup;
import securecompute.algebra.AbelianGroupElement;
import securecompute.algebra.Field;
import securecompute.algebra.PlusMinus;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractP2PointCurve<E, P extends AbstractP2PointCurve.Point<E, P>> implements AbelianGroup<P> {
    final Field<E> field;
    final P identity;

    AbstractP2PointCurve(Field<E> field, P2PointCoordinates<E> identityCoordinates) {
        this.field = field;
        this.identity = rawPoint(identityCoordinates);
    }

    public abstract boolean isCurvePoint(E x, E y, E z);

    abstract boolean isNegative(P elt);

    abstract P rawPoint(P2PointCoordinates<E> pointCoordinates);

    abstract PlusMinusPoint<E, P> plusMinusPoint(P1PointCoordinates<E> p1Coordinates);

    abstract P2PointCoordinates<E> negative(P2PointCoordinates<E> p);

    abstract int tableNormalizationThreshold();

    public Field<E> getField() {
        return field;
    }

    @Override
    public P zero() {
        return identity;
    }

    @Override
    public P negative(P elt) {
        checkCurve(elt);
        return rawPoint(negative(elt.coordinates()));
    }

    Lazy<P> lazyAbs(P elt) {
        return Lazy.of(() -> isNegative(elt) ? negative(elt) : elt);
    }

    E s(E left, E right) {
        return field.sum(left, right);
    }

    E d(E left, E right) {
        return field.difference(left, right);
    }

    E p(E left, E right) {
        return field.product(left, right);
    }

    E p(E elt, long k) {
        return field.product(elt, k);
    }

    E sq(E elt) {
        return field.power(elt, 2);
    }

    void checkCurve(P v) {
        if (!equals(v.getAbelianGroup())) {
            throw new IllegalArgumentException("Curve mismatch");
        }
    }

    @Override
    public P product(P elt, BigInteger k) {
        int bitLength = k.abs().bitLength();
        // Use a lower 4-bit window threshold than super does, since a balanced table costs half as much to build.
        if (bitLength < 32) {
            return AbelianGroup.super.product(elt, k);
        }
        P p2, p3, p4;
        List<P2PointCoordinates<E>> list = Stream.of(
                identity, elt, p2 = elt.add(elt), p3 = elt.add(p2), p4 = p2.add(p2), p2.add(p3), p3.add(p3), p3.add(p4), p4.add(p4)
        ).map(P::coordinates).collect(ImmutableList.toImmutableList());

        if (bitLength >= tableNormalizationThreshold()) {
            list = P2PointCoordinates.normalForms(field, list);
        }

        List<P> table = Stream.of(
                negative(list.get(8)), negative(list.get(7)), negative(list.get(6)), negative(list.get(5)),
                negative(list.get(4)), negative(list.get(3)), negative(list.get(2)), negative(list.get(1)),
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
    public P select(int index, List<P> elements) {
        //noinspection ConstantConditions
        E x = field.select(index, Lists.transform(elements, p -> p.coordinates().x()));
        //noinspection ConstantConditions
        E y = field.select(index, Lists.transform(elements, p -> p.coordinates().y()));
        //noinspection ConstantConditions
        E z = field.select(index, Lists.transform(elements, p -> p.coordinates().z()));
        return rawPoint(x, y, z);
    }

    P rawPoint(E x, E y, E z) {
        return rawPoint(P2PointCoordinates.of(x, y, z));
    }

    public PlusMinusPoint<E, P> plusMinusPoint(E x, E y) {
        return plusMinusPoint(P1PointCoordinates.of(x, y));
    }

    public P point(E x, E y, E z) {
        if (!isCurvePoint(x, y, z)) {
            throw new IllegalArgumentException("Invalid curve point: (" + x + " : " + y + " : " + z + ")");
        }
        return rawPoint(x, y, z);
    }

    public abstract static class Point<E, P extends Point<E, P>> implements AbelianGroupElement<P> {
        P2PointCoordinates<E> coordinates() {
            return ((AbstractP2PointCurve<E, ?>.LazyCoordinates2) normalCoordinates()).originalCoordinates;
        }

        public abstract Lazy<P2PointCoordinates<E>> normalCoordinates();

        @Override
        public String toString() {
            return normalCoordinates().toString();
        }
    }

    public abstract static class PlusMinusPoint<E, P extends Point<E, P>> implements PlusMinus<P> {
        P1PointCoordinates<E> coordinates() {
            return ((AbstractP2PointCurve<E, ?>.LazyCoordinates1) normalCoordinates()).originalCoordinates;
        }

        public abstract Lazy<P1PointCoordinates<E>> normalCoordinates();

        abstract Lazy<P> lazyWitness();

        @Override
        public P getWitness() {
            return lazyWitness().get();
        }

        @Override
        public String toString() {
            return normalCoordinates().toString();
        }
    }

    class LazyCoordinates1 extends Lazy<P1PointCoordinates<E>> {
        private final P1PointCoordinates<E> originalCoordinates;

        LazyCoordinates1(P1PointCoordinates<E> originalCoordinates) {
            this.originalCoordinates = originalCoordinates;
        }

        @Override
        protected P1PointCoordinates<E> compute() {
            return originalCoordinates.normalForm(field);
        }
    }

    class LazyCoordinates2 extends Lazy<P2PointCoordinates<E>> {
        private final P2PointCoordinates<E> originalCoordinates;

        LazyCoordinates2(P2PointCoordinates<E> originalCoordinates) {
            this.originalCoordinates = originalCoordinates;
        }

        @Override
        protected P2PointCoordinates<E> compute() {
            return originalCoordinates.normalForm(field);
        }
    }
}
