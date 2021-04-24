package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import securecompute.algebra.Field;

import java.util.List;

@AutoValue
public abstract class P2PointCoordinates<E> {
    public abstract E x();

    public abstract E y();

    public abstract E z();

    @Override
    public String toString() {
        return "(" + x() + " : " + y() + " : " + z() + ")";
    }

    public P2PointCoordinates<E> normalForm(Field<E> field) {
        return normalForms(field, ImmutableList.of(this)).get(0);
    }

    public static <E> P2PointCoordinates<E> of(E x, E y, E z) {
        return new AutoValue_P2PointCoordinates<>(x, y, z);
    }

    public static <E> List<P2PointCoordinates<E>> normalForms(Field<E> field, List<P2PointCoordinates<E>> coordinatesList) {
        E zero = field.zero(), one = field.one();

        List<E> scales = coordinatesList.stream()
                .map(p -> zero.equals(p.z()) ? zero.equals(p.y()) ? zero.equals(p.x()) ? one : p.x() : p.y() : p.z())
                .collect(ImmutableList.toImmutableList());
        List<E> invScales = Field.reciprocals(field, scales);

        ImmutableList.Builder<P2PointCoordinates<E>> builder = ImmutableList.builderWithExpectedSize(invScales.size());
        for (int i = 0; i < invScales.size(); i++) {
            P2PointCoordinates<E> p = coordinatesList.get(i);
            E z = zero.equals(p.z()) ? zero : one;
            E y = z == zero ? zero.equals(p.y()) ? zero : one : field.product(p.y(), invScales.get(i));
            E x = z == zero && y == zero ? zero.equals(p.x()) ? zero : one : field.product(p.x(), invScales.get(i));
            builder.add(of(x, y, z));
        }
        return builder.build();
    }
}
