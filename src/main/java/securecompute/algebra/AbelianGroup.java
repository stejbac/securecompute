package securecompute.algebra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public interface AbelianGroup<E> {

    E zero();

    E sum(E left, E right);

    default E sum(Iterable<E> elements) {
        return sum(Streams.stream(elements));
    }

    default E sum(Stream<E> elements) {
        return elements.reduce(zero(), this::sum);
    }

    E negative(E elt);

    default E difference(E left, E right) {
        return sum(left, negative(right));
    }

    default E product(E elt, long k) {
        return product(elt, BigInteger.valueOf(k));
    }

    default E product(E elt, BigInteger k) {
        if (k.signum() < 0) {
            return product(negative(elt), k.negate());
        }
        int bitLength = k.bitLength();
        if (bitLength <= 1) {
            return k.testBit(0) ? elt : zero();
        }
        E x2, x3, x4, x5, x6, x7, x8;
        List<E> table = bitLength < 4 ?
                ImmutableList.of(zero(), elt) : bitLength < 64 ?
                ImmutableList.of(zero(), elt, x2 = sum(elt, elt), sum(elt, x2)) :
                ImmutableList.of(
                        zero(), elt, x2 = sum(elt, elt), x3 = sum(elt, x2),
                        x4 = sum(x2, x2), x5 = sum(x2, x3), x6 = sum(x3, x3), x7 = sum(x3, x4),
                        x8 = sum(x4, x4), sum(x4, x5), sum(x5, x5), sum(x5, x6),
                        sum(x6, x6), sum(x6, x7), sum(x7, x7), sum(x7, x8)
                );
        return windowedProduct(this, table, k);
    }

    default PlusMinus<E> product(PlusMinus<E> plusMinus, long k) {
        return product(plusMinus, BigInteger.valueOf(k));
    }

    default PlusMinus<E> product(PlusMinus<E> plusMinus, BigInteger k) {
        E witness = plusMinus.getWitness();
        return witness != null ? plusMinus(product(witness, k)) : PlusMinus.ofMissing(this);
    }

    default PlusMinus<E> plusMinus(E elt) {
        return new WitnessedPlusMinus<>(checkNotNull(elt), this);
    }

    default E select(int index, List<E> elements) {
        return elements.get(index);
    }

    static <E> E windowedProduct(AbelianGroup<E> group, List<E> table, BigInteger k) {
        E zero = group.zero();
        int bitLength = k.bitLength();
        int logTableSize = 31 - Integer.numberOfLeadingZeros(table.size());
        byte[] bytes = k.toByteArray();
        E acc = zero;
        for (int i = bitLength - 1 & -logTableSize; i >= 0; i -= logTableSize) {
            int index = (bytes[bytes.length - i / 8 - 1] & 0x0ff) >> (i & 7) & table.size() - 1;
            E tableElt = group.select(index, table);
            for (int j = 0; j < logTableSize; j++) {
                acc = acc != zero ? group.sum(acc, acc) : zero;
            }
            acc = acc != zero ? group.sum(acc, tableElt) : tableElt;
        }
        return acc;
    }
}
