package securecompute.constraint;

// TODO: Consider making this extend 'FiniteVectorSpace' as well - it adds almost the same interface methods as 'Code'.

public interface LinearCode<V, E> extends LinearConstraint<V, E>, Code<V> {

    @Override
    default int redundancy() {
        return length() - dimension();
    }
}
