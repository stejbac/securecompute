package securecompute.algebra.elliptic;

import com.google.auto.value.AutoValue;
import securecompute.algebra.Field;

@AutoValue
public abstract class P1PointCoordinates<E> {
    public abstract E x();

    public abstract E y();

    @Override
    public String toString() {
        return "(" + x() + " : " + y() + ")";
    }

    public P1PointCoordinates<E> normalForm(Field<E> field) {
        E zero = field.zero(), one = field.one(), x = x(), y = y();
        return y.equals(zero) ? of(x.equals(zero) ? zero : one, zero) : of(field.quotient(x(), y), one);
    }

    public static <E> P1PointCoordinates<E> of(E x, E y) {
        return new AutoValue_P1PointCoordinates<>(x, y);
    }
}
