package securecompute.algebra;

import com.google.common.collect.Streams;

import java.math.BigInteger;
import java.util.stream.Stream;

public interface Ring<E> extends AbelianGroup<E> {

    default E fromLong(long n) {
        return fromBigInteger(BigInteger.valueOf(n));
    }

    E fromBigInteger(BigInteger n);

    @Override
    default E zero() {
        return fromLong(0);
    }

    default E one() {
        return fromLong(1);
    }

    E product(E left, E right);

    @Override
    default E product(E elt, long k) {
        return product(elt, fromLong(k));
    }

    @Override
    default E product(E elt, BigInteger k) {
        return product(elt, fromBigInteger(k));
    }

    default E product(Iterable<E> elements) {
        return product(Streams.stream(elements));
    }

    default E product(Stream<E> elements) {
        return elements.reduce(one(), this::product);
    }

    default PlusMinus<E> product(PlusMinus<E> left, E right) {
        E witness = left.getWitness();
        return witness != null ? plusMinus(product(witness, right)) : PlusMinus.ofMissing(this);
    }

    default PlusMinus<E> product(PlusMinus<E> left, PlusMinus<E> right) {
        E witness = right.getWitness();
        return witness != null ? product(left, witness) : PlusMinus.ofMissing(this);
    }

    default E power(E elt, long exponent) {
        return power(elt, BigInteger.valueOf(exponent));
    }

    default E power(E elt, BigInteger exponent) {
        if (exponent.signum() < 0) {
            throw new ArithmeticException("Negative exponent");
        }
        E acc = null;
        for (int i = 0, len = exponent.bitLength(); i < len; i++) {
            if (exponent.testBit(i)) {
                acc = acc != null ? product(acc, elt) : elt;
            }
            if (i < len - 1) {
                elt = product(elt, elt);
            }
        }
        return acc != null ? acc : one();
    }
}
