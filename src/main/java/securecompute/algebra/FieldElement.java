package securecompute.algebra;

public interface FieldElement<E> extends RingElement<E> {

    @Override
    default Ring<E> getRing() {
        return getField();
    }

    Field<E> getField();

//    default Field<E> getField() {
//        Ring<E> ring = getRing();
//        if (!(ring instanceof Field)) {
//            throw new UnsupportedOperationException("Not a field element");
//        }
//        return (Field<E>) ring;
//    }

    default E divide(E other) {
        return getField().quotient(cast(), other);
    }

    default E recip() {
        return getField().reciprocal(cast());
    }

    default E recipOrZero() {
        return getField().reciprocalOrZero(cast());
    }
}
