package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import securecompute.constraint.block.BlockConstraint;
import securecompute.constraint.AlgebraicConstraint;
import securecompute.constraint.Constraint;

import java.util.Collections;
import java.util.List;

public class TripleLayerConstraint<V, E> extends GridConstraint<List<V>> {

    public TripleLayerConstraint(AlgebraicConstraint<V, E> topRowConstraint, AlgebraicConstraint<V, E> topColumnConstraint) {
        this(topRowConstraint, topColumnConstraint, 0, 0);
    }

    private TripleLayerConstraint(AlgebraicConstraint<V, E> topRowConstraint, AlgebraicConstraint<V, E> topColumnConstraint,
                                  int rowPadding, int columnPadding) {
        super(
                new LineConstraint<>(topRowConstraint, 1, rowPadding),
                new LineConstraint<>(topColumnConstraint, 2, columnPadding)
        );
    }

    static <V, E> TripleLayerConstraint<V, E> extendToSize(TripleLayerConstraint<V, E> constraint, int newWidth, int newHeight) {
        if (constraint.rowConstraint().padding > 0 || constraint.columnConstraint().padding > 0) {
            throw new AssertionError("Constraint already padded");
        }
        int rowPadding = newWidth - constraint.rowConstraint().length();
        int columnPadding = newHeight - constraint.columnConstraint().length();

        return new TripleLayerConstraint<>(
                constraint.rowConstraint().topConstraint, constraint.columnConstraint().topConstraint,
                rowPadding, columnPadding);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LineConstraint<V, E> rowConstraint() {
        return (LineConstraint<V, E>) super.rowConstraint();
    }

    @Override
    @SuppressWarnings("unchecked")
    public LineConstraint<V, E> columnConstraint() {
        return (LineConstraint<V, E>) super.columnConstraint();
    }

    public static class LineConstraint<V, E> implements Constraint<List<V>> {

        private final AlgebraicConstraint<V, E> topConstraint;
        private final int parityLayerIndex;
        private final int padding; // TODO: Should we rename this 'paddingLength' for consistency (here & elsewhere)?
//        private final BlockFiniteVectorSpace<V, E> symbolSpace, layerSpace;

        private LineConstraint(AlgebraicConstraint<V, E> topConstraint, int parityLayerIndex, int padding) {
            this.topConstraint = topConstraint;
            this.parityLayerIndex = parityLayerIndex;
            this.padding = padding;
            if (topConstraint.length() < topConstraint.redundancy()) {
                throw new IllegalArgumentException("Row/column constraint must have redundancy no larger than its length");
            }
//            symbolSpace = new BlockFiniteVectorSpace<>(topConstraint.symbolSpace(), 3);
//            layerSpace = new BlockFiniteVectorSpace<>(topConstraint.symbolSpace(), topConstraint.length());
        }

        AlgebraicConstraint<V, E> topConstraint() {
            return topConstraint;
        }

        int parityLayerIndex() {
            return parityLayerIndex;
        }

//        @Override
//        public BlockFiniteVectorSpace<V, E> symbolSpace() {
//            return symbolSpace;
//        }

        @Override
        public int length() {
            return topConstraint.length() + padding;
        }

        @Override
        public boolean isValid(List<List<V>> vector) {
            List<List<V>> layers = BlockConstraint.streamLayers(vector.subList(padding, length()), 3)
                    .collect(ImmutableList.toImmutableList());

            List<V> topLayer = layers.get(0);
            List<V> parityLayer = layers.get(parityLayerIndex);
            List<V> zeroLayer = layers.get(3 - parityLayerIndex);

            // TODO: For consistency, should we pad out a short parity layer with zeros at the beginning, instead of the end?
            return isZero(zeroLayer) && isZero(parityLayer.subList(topConstraint.redundancy(), length() - padding)) &&
                    topConstraint.parityCheck(topLayer).equals(parityLayer.subList(0, topConstraint.redundancy()));
        }

        private boolean isZero(List<V> layer) {
            return layer.stream().allMatch(topConstraint.symbolSpace().zero()::equals);
        }

//        @Override
//        public int degree() {
//            return Integer.max(topConstraint.degree(), 1);
//        }

//        @Override
//        public int redundancy() {
//            return topConstraint.length();
//        }

//        @Override
//        public List<List<V>> parityCheck(List<List<V>> vector) {
//            List<List<V>> layers = BlockConstraint.streamLayers(vector.subList(padding, length()), 3)
//                    .collect(Collectors.toList());
//
//            List<V> topLayerParity = zeroExtendedParity(layers.get(0));
//            layers.set(parityLayerIndex, layerSpace.difference(layers.get(parityLayerIndex), topLayerParity));
//            layers.set(0, layerSpace.zero());
//
//            return BlockConstraint.transpose(layers, length());
//        }

        List<V> zeroExtendedParity(List<V> topLayer) {
            return zeroExtendedParity(ImmutableList.of(), topLayer);
        }

        List<V> zeroExtendedParity(List<V> prefix, List<V> topLayer) {
            List<V> syndrome = topConstraint.parityCheck(topLayer.subList(padding, length()));
            // TODO: This is very similar to PuncturedPolynomialCode.paddedCoefficients - DEDUPLICATE.
            return ImmutableList.<V>builderWithExpectedSize(length())
                    .addAll(prefix)
                    .addAll(syndrome)
                    .addAll(Collections.nCopies(length() - prefix.size() - syndrome.size(), topConstraint.symbolSpace().zero()))
                    .build();
        }
    }
}
