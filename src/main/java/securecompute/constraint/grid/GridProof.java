package securecompute.constraint.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import securecompute.constraint.Constraint;
import securecompute.constraint.LinearCode;
import securecompute.constraint.LocallyTestableProof;
import securecompute.constraint.MultiplicativeLinearCode;
import securecompute.constraint.block.BlockConstraint;
import securecompute.constraint.block.BlockLinearCode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GridProof<V, E> extends GridConstraint<List<V>> implements LocallyTestableProof<List<V>> {

    final GridLinearCode<V, E> topLayerCode, topLayerOuterCode; // TODO: Consider making these private & adding package-private getter.
    private final GridLinearCode<List<V>, E> innerGridCode;
    final GridLinearCode<List<V>, E> outerGridCode; // TODO: Consider making this private & adding package-private getter.
    private final TripleLayerConstraint<V, E> messageConstraint;

    public GridProof(GridLinearCode<V, E> topLayerCode, TripleLayerConstraint<V, E> messageConstraint) {
        super(
                new LineConstraint<>(messageConstraint.rowConstraint(), topLayerCode.rowConstraint(),
                        messageConstraint.columnConstraint().topConstraint().degree()),
                new LineConstraint<>(messageConstraint.columnConstraint(), topLayerCode.columnConstraint(),
                        messageConstraint.rowConstraint().topConstraint().degree())
        );
        this.topLayerCode = topLayerCode;
        this.messageConstraint = messageConstraint;

        innerGridCode = new GridLinearCode<>(
                ((LineConstraint<V, E>) rowConstraint()).innerCode,
                ((LineConstraint<V, E>) columnConstraint()).innerCode
        );

        outerGridCode = new GridLinearCode<>(
                ((LineConstraint<V, E>) rowConstraint()).outerCode,
                ((LineConstraint<V, E>) columnConstraint()).outerCode
        );

        topLayerOuterCode = new GridLinearCode<>(
                ((LineConstraint<V, E>) rowConstraint()).topLayerOuterCode,
                ((LineConstraint<V, E>) columnConstraint()).topLayerOuterCode
        );
    }

    private static <V, E> LinearCode<V, E> power(LinearCode<V, E> code, int exponent) {
//        if (exponent == 1) {
//            return code;
//        }
        if ((code instanceof MultiplicativeLinearCode)) {
            return ((MultiplicativeLinearCode<V, E>) code).pow(exponent);
        }
        throw new ClassCastException("Row/column code is not multiplicative: cannot raise it to the power of the " +
                "degree of the message column/row constraint");
//        throw new ClassCastException((isRow ? "Row" : "Column") +
//                " code is not multiplicative: cannot raise it to the power of the degree of the message" +
//                (isRow ? "column" : "row") + " constraint");
    }

    @Override
    public TripleLayerConstraint<V, E> witnessConstraint() {
        return messageConstraint;
    }

    @Override
    public int distance() {
        return outerGridCode.distance();
    }

    @Override
    public List<List<V>> encode(List<List<V>> witness) {
        List<V> messageTopLayer = paddedTopLayer(witness);
        List<V> topLayer = topLayerCode.encode(messageTopLayer);

        // TODO: Try to make this more efficient...
        List<List<V>> topLayerRows = Lists.partition(topLayer, rowConstraint().length());
        List<List<V>> topLayerColumns = BlockConstraint.transpose(topLayerRows, rowConstraint().length());

        List<V> middleLayer = Streams.zip(rowPaddingBlock().stream(),
                topLayerRows.stream().map(topLayerCode.rowConstraint()::decode),
                messageConstraint.rowConstraint()::zeroExtendedParity
        )
                .map(topLayerCode.rowConstraint()::encode)
                .flatMap(List::stream)
                .collect(ImmutableList.toImmutableList());

        List<List<V>> bottomLayerColumns = Streams.zip(columnPaddingBlock().stream(),
                topLayerColumns.stream().map(topLayerCode.columnConstraint()::decode),
                messageConstraint.columnConstraint()::zeroExtendedParity
        )
                .map(topLayerCode.columnConstraint()::encode)
                .collect(ImmutableList.toImmutableList());

        List<V> bottomLayer = BlockConstraint.streamLayers(bottomLayerColumns, columnConstraint().length())
                .flatMap(List::stream)
                .collect(ImmutableList.toImmutableList());

        return BlockConstraint.transpose(ImmutableList.of(topLayer, middleLayer, bottomLayer), length());
    }

    List<V> paddedTopLayer(List<List<V>> witness) {
        if (!witnessConstraint().isValid(witness)) {
            throw new IllegalArgumentException("Cannot encode an invalid witness");
        }
        List<List<V>> messageLayers = BlockConstraint.transpose(witness, 3);
        return messageLayers.get(0);
    }

    List<List<V>> rowPaddingBlock() {
        return Collections.nCopies(columnConstraint().length(), Collections.<V>emptyList());
    }

    List<List<V>> columnPaddingBlock() {
        return Collections.nCopies(rowConstraint().length(), Collections.<V>emptyList());
    }

    @Override
    public List<List<V>> decode(List<List<V>> codeword) {
        return innerGridCode.decode(codeword);
    }

    @Override
    public LocalTest<List<V>> localTest() {
        // TODO: Consider naming these anonymous inner classes:
        return new GridLinearCode.SimpleLocalTest<List<V>, E>(outerGridCode) {
            @Override
            protected Evidence evidence(int x, int y, List<List<V>> column, List<List<V>> row) {
                return new Evidence(x, y, column, row) {
                    @Override
                    public boolean isFailure() {
                        return !GridProof.this.rowConstraint().isValid(row) ||
                                !GridProof.this.columnConstraint().isValid(column);
                    }
                };
            }
        };
    }

    private static class LineConstraint<V, E> implements Constraint<List<V>> {

        private final Constraint<List<V>> messageConstraint;
        private final LinearCode<V, E> topLayerCode, topLayerOuterCode;
        private final BlockLinearCode<V, E> innerCode, outerCode;
        private final int parityLayerIndex;

        LineConstraint(TripleLayerConstraint.LineConstraint<V, E> messageConstraint,
                       LinearCode<V, E> topLayerCode, int degree) {

            if (!messageConstraint.topConstraint().symbolSpace().equals(topLayerCode.symbolSpace())) {
                throw new IllegalArgumentException("Row/column message constraint and code symbol space mismatch");
            }
            if (messageConstraint.length() != topLayerCode.dimension()) {
                throw new IllegalArgumentException("Row/column message length and code dimension mismatch");
            }
            this.messageConstraint = messageConstraint;
            this.topLayerCode = topLayerCode;
            topLayerOuterCode = power(topLayerCode, degree);
            innerCode = new BlockLinearCode<>(topLayerCode, 3);
            outerCode = new BlockLinearCode<>(topLayerOuterCode, 3);
            parityLayerIndex = messageConstraint.parityLayerIndex();
        }

        @Override
        public int length() {
            return topLayerCode.length();
        }

        @Override
        public boolean isValid(List<List<V>> vector) {
            List<List<V>> layers = BlockConstraint.streamLayers(vector, 3).collect(Collectors.toList());
            return topLayerCode.isValid(layers.get(0)) &&
                    topLayerCode.isValid(layers.get(parityLayerIndex)) &&
                    topLayerOuterCode.isValid(layers.get(3 - parityLayerIndex)) &&
                    messageConstraint.isValid(innerCode.decode(vector));
        }
    }
}
