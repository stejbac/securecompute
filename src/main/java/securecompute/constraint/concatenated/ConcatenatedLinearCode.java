package securecompute.constraint.concatenated;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.constraint.LinearCode;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class ConcatenatedLinearCode<V, E> extends ConcatenatedAlgebraicConstraint<V, E> implements LinearCode<V, E> {

    private final int dimension;
    private final int distance;
    private final int redundancy;
    private final int codistance;

    public ConcatenatedLinearCode(LinearCode<V, E> rowCode, LinearCode<List<V>, E> outerCode) {
        super(rowCode, outerCode);
        dimension = rowCode.dimension() * outerCode.dimension();
        distance = rowCode.distance() * outerCode.distance();
        redundancy = rowCode.length() * outerCode.redundancy() + rowCode.redundancy() * outerCode.dimension();
        // TODO: Check this bound is correct (& sharp):
        codistance = Math.min(rowCode.codistance(), outerCode.codistance());
    }

    @Override
    public LinearCode<V, E> rowConstraint() {
        return (LinearCode<V, E>) super.rowConstraint();
    }

    @Override
    public LinearCode<List<V>, E> outerConstraint() {
        return (LinearCode<List<V>, E>) super.outerConstraint();
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public int redundancy() {
        return redundancy;
    }

    @Override
    public int codistance() {
        return codistance;
    }

    @Override
    public List<V> encode(List<V> message) {
        List<List<V>> encodedRows = Lists.partition(message, rowConstraint().dimension()).stream()
                .map(rowConstraint()::encode)
                .collect(ImmutableList.toImmutableList());

        return outerConstraint().encode(encodedRows).stream()
                .flatMap(List::stream)
                .collect(ImmutableList.toImmutableList());
    }

    private Stream<V> genericDecode(List<V> vector,
                                    Function<List<List<V>>, List<List<V>>> outerDecoder,
                                    Function<List<V>, List<V>> rowDecoder) {

        // TODO: Why are we converting the partitioned list to a stream, then back to a list again? Performance?
        List<List<V>> encodedRows = outerDecoder.apply(
                Lists.partition(vector, rowConstraint().length()).stream()
                        .collect(ImmutableList.toImmutableList()));

        return encodedRows.stream()
                .map(rowDecoder)
                .flatMap(List::stream);
    }

    @Override
    public List<V> decode(List<V> codeword) {
        return genericDecode(codeword, outerConstraint()::decode, rowConstraint()::decode)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<V> parityCheck(List<V> vector) {
        Stream<V> outerSyndrome = genericDecode(vector, outerConstraint()::parityCheck, Function.identity());
        Stream<V> rowSyndromes = genericDecode(vector, outerConstraint()::decode, rowConstraint()::parityCheck);

        return Stream.concat(outerSyndrome, rowSyndromes).collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean isValid(List<V> vector) {
        // TODO: Is it really more efficient to use this instead of 'super.isValid()', in general?
        return LinearCode.super.isValid(vector);
    }
}
