package securecompute.algebra;

public interface EuclideanDomainElement<E> extends RingElement<E> {

    @Override
    default Ring<E> getRing() {
        return getEuclideanDomain();
    }

    EuclideanDomain<E> getEuclideanDomain();

//    default EuclideanDomain<E> getEuclideanDomain() {
//        Ring<E> ring = getRing();
//        if (!(ring instanceof EuclideanDomain)) {
//            throw new UnsupportedOperationException("Not a euclidean domain element");
//        }
//        return (EuclideanDomain<E>) ring;
//    }

    default int size() {
        return getEuclideanDomain().size(cast());
    }

    default E abs() {
        return getEuclideanDomain().abs(cast());
    }

    default E signum() {
        return getEuclideanDomain().signum(cast());
    }

    default E div(E other) {
        return getEuclideanDomain().div(cast(), other);
    }

    default E mod(E other) {
        return getEuclideanDomain().mod(cast(), other);
    }

    default EuclideanDomain.DivModResult<E> divMod(E other) {
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
}
