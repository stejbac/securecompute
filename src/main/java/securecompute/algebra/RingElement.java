package securecompute.algebra;

import java.math.BigInteger;

public interface RingElement<E> extends AbelianGroupElement<E> {

    @Override
    default AbelianGroup<E> getAbelianGroup() {
        return getRing();
    }

    Ring<E> getRing();

    /*
    // TO DO: Should be (package) private:
    static <E> Ring<E> getRing(RingElement<E> elt, Class<?> clazz) {
        try {
            @SuppressWarnings("unchecked")
            Ring<E> structure = (Ring<E>) clazz.cast(elt.getRing());
            return structure;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Not a " + clazz.getSimpleName() + " element.");
        }
    }

    default Field<E> getField() {
        return (Field<E>) getRing(this, Field.class);
//        Ring<E> structure = getRing();
//        if (!(structure instanceof Field)) {
//            throw new UnsupportedOperationException("Not a field element.");
//        }
//        return (Field<E>) structure;
    }

    default EuclideanDomain<E> getEuclideanDomain() {
        return (EuclideanDomain<E>) getRing(this, EuclideanDomain.class);
    }
    */

    default E multiply(E other) {
        return getRing().product(cast(), other);
    }

    default E pow(long exponent) {
        return getRing().power(cast(), exponent);
    }

    default E pow(BigInteger exponent) {
        return getRing().power(cast(), exponent);
    }

    /*
    default E divide(E other) {
        return getField().quotient(cast(), other);
    }

    default E recip() {
        return getField().reciprocal(cast());
    }

    default E recipOrZero() {
        return getField().reciprocalOrZero(cast());
    }

    default int size() {
        return getEuclideanDomain().size(cast());
    }

    default E div(E other) {
        return getEuclideanDomain().div(cast(), other);
    }

    default E mod(E other) {
        return getEuclideanDomain().mod(cast(), other);
    }

    default EuclideanDomain.DivModResult<? extends E> divMod(E other) {
        return getEuclideanDomain().divMod(cast(), other);
    }

    default E gcd(E other) {
        return getEuclideanDomain().gcd(cast(), other);
    }

    default E lcm(E other) {
        return getEuclideanDomain().lcm(cast(), other);
    }

    default EuclideanDomain.GcdExtResult<? extends E> gcdExt(E other) {
        return getEuclideanDomain().gcdExt(cast(), other);
    }
    */
}
