package securecompute.algebra.module.singleton;

import com.google.common.collect.ImmutableList;
import securecompute.algebra.Field;
import securecompute.algebra.module.FiniteVectorSpace;

import java.util.List;

// TODO: Replace constructors of this (& other) type hierarchies with a single static factory method.

public class SingletonVectorSpace<E> extends SingletonModule<E> implements FiniteVectorSpace<E, E> {

    public SingletonVectorSpace(Field<E> baseField) {
        super(baseField);
    }

    @Override
    public Field<E> getBaseRing() {
        return (Field<E>) super.getBaseRing();
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public E pack(Iterable<E> elements) {
        return elements.iterator().next();
    }

    @Override
    public List<E> unpack(E vector) {
        return ImmutableList.of(vector);
    }
}
