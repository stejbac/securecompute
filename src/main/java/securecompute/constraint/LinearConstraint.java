package securecompute.constraint;

public interface LinearConstraint<V, E> extends AlgebraicConstraint<V, E> {

    @Override
    default int degree() {
        return 1;
    }
}
