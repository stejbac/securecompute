package securecompute.algebra;

import java.util.Random;

public interface FiniteField<E> extends Field<E> {

    int size();

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

    E sampleUniformly(Random random);
}
