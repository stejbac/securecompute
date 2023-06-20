package securecompute.constraint.grid;

import com.google.common.base.Suppliers;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableList;
import securecompute.StreamUtils;
import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.constraint.LinearCode;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

class PuncturedLinearCode<V, E> implements LinearCode<V, E> {

    private final LinearCode<V, E> baseCode;
    private final int punctureNumber;
    private final Function<List<V>, List<V>> extensionFn;

    PuncturedLinearCode(LinearCode<V, E> baseCode, int punctureNumber) {
        this.baseCode = baseCode;
        this.punctureNumber = punctureNumber;
        if (punctureNumber < 0 || punctureNumber >= baseCode.distance()) {
            throw new IllegalArgumentException("Puncture number must be non-negative and less than the code distance");
        }

        // This assumes that interpolation is supported on 'baseCode' & the first k symbols are linearly independent.
        // We know there are _some_ k linearly independent symbols; if we've guessed the wrong ones, the 'decode' &
        // 'parityCheck' functions will be unsupported. TODO: Allow user-supplied linearly independent symbol choices.
        Supplier<Function<List<V>, List<V>>> interpolationFnSupplier = Suppliers.memoize(() ->
                baseCode.interpolationFn(ContiguousSet.closedOpen(0, dimension())));
        //noinspection ConstantConditions
        extensionFn = v -> interpolationFnSupplier.get().apply(v.subList(0, dimension()));
    }

    @Override
    public FiniteVectorSpace<V, E> symbolSpace() {
        return baseCode.symbolSpace();
    }

    @Override
    public int length() {
        return baseCode.length() - punctureNumber;
    }

    @Override
    public int dimension() {
        return baseCode.dimension();
    }

    @Override
    public int distance() {
        return baseCode.distance() - punctureNumber;
    }

    @Override
    public int codistance() {
        return baseCode.codistance();
    }

    @Override
    public List<V> encode(List<V> message) {
        return baseCode.encode(message).subList(0, length());
    }

    @Override
    public List<V> decode(List<V> codeword) {
        return baseCode.decode(extensionFn.apply(codeword));
    }

    @Override
    public List<V> parityCheck(List<V> vector) {
        List<V> prefixMatchingCodeword = extensionFn.apply(vector);
        // TODO: Skipping may have poor performance with our substitute zip method - consider optimising:
        return StreamUtils.zip(vector.stream(), prefixMatchingCodeword.stream(), symbolSpace()::difference)
                .skip(dimension())
                .collect(ImmutableList.toImmutableList());
    }
}
