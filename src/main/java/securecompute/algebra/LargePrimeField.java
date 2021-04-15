package securecompute.algebra;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSortedSet;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class LargePrimeField extends QuotientField<BigInteger> implements FiniteField<QuotientField<BigInteger>.Coset> {

    private final Supplier<Coset> primitiveElement = Suppliers.memoize(this::findPrimitiveElement);
    private final Supplier<SortedSet<BigInteger>> totientCofactors;

    public LargePrimeField(BigInteger p) {
        this(p, true);
    }

    public LargePrimeField(BigInteger p, boolean checkPrime) {
        super(BigIntegerRing.INSTANCE, checkPrime ? checkPrime(p) : p);
        totientCofactors = Suppliers.memoize(() -> findPrimeCofactors(p.subtract(ONE)));
    }

    public LargePrimeField(BigInteger p, List<BigInteger> totientPrimeFactors) {
        super(BigIntegerRing.INSTANCE, checkPrime(p));
        this.totientCofactors = Suppliers.ofInstance(getPrimeCofactors(p.subtract(ONE), totientPrimeFactors));
    }

    private static BigInteger checkPrime(BigInteger p) {
        checkArgument(p.isProbablePrime(20), "Not a prime: %s", p);
        return p;
    }

    private Coset findPrimitiveElement() {
        return Stream.iterate(one(), x -> x.add(one()))
                .filter(this::isPrimitive)
                .findFirst()
                .orElseThrow(RuntimeException::new);
    }

    private static SortedSet<BigInteger> getPrimeCofactors(BigInteger n, List<BigInteger> primeFactors) {
        BigInteger product = primeFactors.stream().reduce(ONE, BigInteger::multiply);
        checkArgument(product.equals(n), "Unexpected prime factor product: %s", product);

        return primeFactors.stream()
                .distinct()
                .map(p -> n.divide(checkPrime(p)))
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.reverseOrder()));
    }

    // only works if n has at most 1 large prime factor
    private static SortedSet<BigInteger> findPrimeCofactors(BigInteger n) {
        ImmutableSortedSet.Builder<BigInteger> cofactors = ImmutableSortedSet.reverseOrder();

        BigInteger d = n;
        BigInteger p = BigInteger.valueOf(2);
        if (d.compareTo(ONE) > 0) {
            while (!d.isProbablePrime(100)) {
                while (!d.mod(p).equals(ZERO)) {
                    p = p.nextProbablePrime();
                }
                d = d.divide(p);
                cofactors.add(n.divide(p));
            }
            cofactors.add(n.divide(d));
        }
        return cofactors.build();
    }

    private boolean isPrimitive(Coset unit) {
        return totientCofactors.get().stream().noneMatch(n -> unit.pow(n).equals(one()));
    }

    @Override
    public BigInteger size() {
        return getIdealGenerator();
    }

    @Override
    public Coset getPrimitiveElement() {
        return primitiveElement.get();
    }

    @Override
    public Coset sampleUniformly(Random random) {
        BigInteger p = getIdealGenerator();
        while (true) {
            BigInteger n = new BigInteger(p.bitLength(), random);
            if (n.compareTo(p) < 0) {
                return coset(n);
            }
        }
    }
}
