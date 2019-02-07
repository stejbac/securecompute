package securecompute.constraint;

import com.google.common.collect.ImmutableSortedMap;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Function;

public interface Code<V> extends Constraint<V> {

    int dimension();

    int distance();

    List<V> encode(List<V> message);

    List<V> decode(List<V> codeword);

    default boolean isSystematic() {
        return false;
    }

    default Function<List<V>, List<V>> interpolationFn(SortedSet<Integer> knownSymbolIndices) {
        throw new UnsupportedOperationException("interpolationFn");
    }

    default List<V> interpolate(Map<Integer, V> knownSymbols) {
        ImmutableSortedMap<Integer, V> sortedKnownSymbols = ImmutableSortedMap.copyOf(knownSymbols);
        List<V> knownSymbolVector = sortedKnownSymbols.values().asList();
        return interpolationFn(sortedKnownSymbols.keySet()).apply(knownSymbolVector);
    }
}
