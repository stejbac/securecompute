package securecompute.constraint;

import java.util.List;

public interface Constraint<V> {

    boolean isValid(List<V> vector);

    int length();
}
