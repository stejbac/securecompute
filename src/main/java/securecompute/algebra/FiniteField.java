package securecompute.algebra;

import java.math.BigInteger;
import java.util.Random;
import java.util.stream.Stream;

public interface FiniteField<E> extends Field<E> {

    BigInteger size();

    default long sizeAsLong() {
        return size().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    E getPrimitiveElement();

    default E exp(int n) {
        return power(getPrimitiveElement(), n);
    }

    default int log(E elt) {
        if (elt.equals(zero())) {
            throw new ArithmeticException("Logarithm of zero");
        }
        E primitiveElement = getPrimitiveElement();
        E primitivePower = one();
        for (int n = 0; ; n++) {
            if (elt.equals(primitivePower)) {
                return n;
            }
            primitivePower = product(primitivePower, primitiveElement);
        }
    }

    default PlusMinus<E> invSqrt(E elt) {
        return PowerTwoRootsOfUnity.powerTwoRootsOfUnity(this).invSqrt(this, elt);
    }

    default PlusMinus<E> sqrt(E elt) {
        return PowerTwoRootsOfUnity.powerTwoRootsOfUnity(this).sqrt(this, elt);
    }

    E sampleUniformly(Random random);

    default Stream<E> getElements() {
        E a = getPrimitiveElement();
        return Stream.concat(Stream.of(zero()), Stream.iterate(one(), x -> product(a, x))).limit(sizeAsLong());
    }
}
