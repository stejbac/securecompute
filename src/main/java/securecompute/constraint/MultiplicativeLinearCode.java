package securecompute.constraint;

// NOTE: Code C is expected to contain the constant code 1, so that 1 <= C <= C*C <= C*C*C <= ...
public interface MultiplicativeLinearCode<V, E> extends LinearCode<V, E> {

    MultiplicativeLinearCode<V, E> pow(int exponent);
}
