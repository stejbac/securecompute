package securecompute.constraint;

public interface LocallyTestableProof<V> extends LocallyTestableCode<V> {

//    LocallyTestableLinearCode<V, E> asLinearCode();

    Constraint<V> witnessConstraint();

    @Override
    default int dimension() {
        return witnessConstraint().length();
    }
//
//    @Override
//    default int distance() {
//        return asLinearCode().distance();
//    }
//
//    @Override
//    default List<V> encode(List<V> message) {
//        return asLinearCode().encode(message);
//    }
//
//    @Override
//    default List<V> decode(List<V> codeword) {
//        return asLinearCode().decode(codeword);
//    }
//
//    @Override
//    default boolean isValid(List<V> vector) {
//        return asLinearCode().isValid(vector) && witnessConstraint().isValid(decode(vector));
//    }
//
//    @Override
//    default int length() {
//        return asLinearCode().length();
//    }
}
