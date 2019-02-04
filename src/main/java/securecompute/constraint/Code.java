package securecompute.constraint;

import java.util.List;

public interface Code<V> extends Constraint<V> {

    int dimension();

    int distance();

    List<V> encode(List<V> message);

    List<V> decode(List<V> codeword);
}
