package securecompute.algebra.module.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import securecompute.algebra.Ring;
import securecompute.algebra.module.Module;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

// TODO: Should these really be called BlockXXX, instead of RepeatedXXX (or similar)? Not sure this is consistent with the meaning of a block code/constraint.
public class BlockModule<V, E> implements Module<List<V>, E> {

    private final Module<V, E> baseModule;
    private final Ring<E> baseRing;
    private final int blockSize;

    public BlockModule(Module<V, E> baseModule, int blockSize) {
        this.baseModule = baseModule;
        this.blockSize = blockSize;
        baseRing = baseModule.getBaseRing();
    }

    public Module<V, E> getBaseModule() {
        return baseModule;
    }

    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public Ring<E> getBaseRing() {
        return baseRing;
    }

    @Override
    public List<V> scalarProduct(List<V> left, E right) {
        return left.stream().map(elt -> baseModule.scalarProduct(elt, right))
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<V> zero() {
        return Collections.nCopies(blockSize, baseModule.zero());
    }

    @Override
    public List<V> sum(List<V> left, List<V> right) {
        return Streams.zip(left.stream(), right.stream(), baseModule::sum)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<V> negative(List<V> elt) {
        return elt.stream().map(baseModule::negative)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj != null && getClass().equals(obj.getClass()) &&
                baseModule.equals(((BlockModule<?, ?>) obj).baseModule) &&
                blockSize == (((BlockModule<?, ?>) obj).blockSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), baseModule, blockSize);
    }
}
