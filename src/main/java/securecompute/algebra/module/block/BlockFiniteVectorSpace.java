package securecompute.algebra.module.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import securecompute.algebra.module.FiniteVectorSpace;

import java.util.List;

public class BlockFiniteVectorSpace<V, E> extends BlockVectorSpace<V, E> implements FiniteVectorSpace<List<V>, E> {

    private final int dimension;

    public BlockFiniteVectorSpace(FiniteVectorSpace<V, E> baseVectorSpace, int blockSize) {
        super(baseVectorSpace, blockSize);
        this.dimension = baseVectorSpace.getDimension() * blockSize;
    }

    @Override
    public FiniteVectorSpace<V, E> getBaseModule() {
        return (FiniteVectorSpace<V, E>) super.getBaseModule();
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public List<V> pack(Iterable<E> elements) {
        return Streams.stream(Iterables.partition(elements, getBaseModule().getDimension()))
                .map(getBaseModule()::pack)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<E> unpack(List<V> vector) {
        return vector.stream()
                .map(getBaseModule()::unpack)
                .flatMap(List::stream)
                .collect(ImmutableList.toImmutableList());
    }
}
