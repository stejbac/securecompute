package securecompute.algebra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import securecompute.ShallowCopyable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

class PowerTwoRootsOfUnity<E> {
    private static final Function<FiniteField<?>, PowerTwoRootsOfUnity<?>> CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .build(CacheLoader.from(PowerTwoRootsOfUnity::computePowerTwoRootsOfUnity));

    private final boolean hasCharacteristicTwo;
    private final BigInteger sqrtPower;
    private final BigInteger invSqrtPower;
    private final Map<E, E> invSqrtMap;

    private PowerTwoRootsOfUnity(boolean hasCharacteristicTwo, BigInteger invSqrtPower, Map<E, E> invSqrtMap) {
        this.hasCharacteristicTwo = hasCharacteristicTwo;
        this.invSqrtPower = invSqrtPower;
        this.invSqrtMap = invSqrtMap;
        sqrtPower = invSqrtPower.add(BigInteger.ONE);
    }

    public PlusMinus<E> invSqrt(Field<E> field, E x) {
        E y = field.power(x, invSqrtPower), yy = field.power(y, 2), xyy = field.product(x, yy);
        return invSqrtMap.containsKey(xyy) ? field.plusMinus(field.product(y, invSqrtMap.get(xyy))) : PlusMinus.ofMissing(field);
    }

    public PlusMinus<E> sqrt(Field<E> field, E x) {
        return hasCharacteristicTwo
                ? field.plusMinus(field.power(x, sqrtPower)) // inverse Frobenius automorphism
                : x.equals(field.zero()) ? field.plusMinus(field.zero()) : field.product(invSqrt(field, x), x);
    }

    @SuppressWarnings("unchecked")
    public static <E> PowerTwoRootsOfUnity<E> powerTwoRootsOfUnity(FiniteField<E> field) {
        return (PowerTwoRootsOfUnity<E>) CACHE.apply(field);
    }

    private static <E> PowerTwoRootsOfUnity<E> computePowerTwoRootsOfUnity(FiniteField<E> field) {
        BigInteger qMinus1 = field.size().subtract(BigInteger.ONE);
        int n = 0;
        while (!qMinus1.testBit(n)) {
            n++;
        }
        if (n > 20) {
            throw new UnsupportedOperationException("Too many power-two roots of unity");
        }
        BigInteger invSqrtPower = qMinus1.shiftRight(n + 1);

        // Attempt to replace the field with a clone at this point, to prevent strong references from
        // cache values to keys via the field elements held in the returned PowerTwoRootsOfUnity instance.
        FiniteField<E> clonedField = ShallowCopyable.tryClone(field);

        E primitiveRootOfUnity = clonedField.power(clonedField.getPrimitiveElement(), qMinus1.shiftRight(n));
        List<E> rootsOfUnity = Stream.iterate(clonedField.one(), x -> clonedField.product(primitiveRootOfUnity, x))
                .limit(1 << n)
                .collect(ImmutableList.toImmutableList());

        ImmutableMap.Builder<E, E> builder = ImmutableMap.builderWithExpectedSize((rootsOfUnity.size() + 1) / 2);
        for (int i = 0; i < rootsOfUnity.size(); i += 2) {
            builder.put(rootsOfUnity.get(i), rootsOfUnity.get(i > 1 ? rootsOfUnity.size() - i / 2 : 0));
        }
        Map<E, E> invSqrtMap = builder.build();

        return new PowerTwoRootsOfUnity<>(n == 0, invSqrtPower, invSqrtMap);
    }
}
