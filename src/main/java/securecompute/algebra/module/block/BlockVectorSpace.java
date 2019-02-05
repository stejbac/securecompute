package securecompute.algebra.module.block;

import securecompute.algebra.Field;
import securecompute.algebra.module.VectorSpace;

import java.util.List;

public class BlockVectorSpace<V, E> extends BlockModule<V, E> implements VectorSpace<List<V>, E> {

    public BlockVectorSpace(VectorSpace<V, E> baseVectorSpace, int blockSize) {
        super(baseVectorSpace, blockSize);
    }

    @Override
    public Field<E> getBaseRing() {
        return (Field<E>) super.getBaseRing();
    }

    @Override
    public VectorSpace<V, E> getBaseModule() {
        return (VectorSpace<V, E>) super.getBaseModule();
    }
}
