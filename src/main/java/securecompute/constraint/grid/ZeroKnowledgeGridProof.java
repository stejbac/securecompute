package securecompute.constraint.grid;

import com.google.common.collect.*;
import securecompute.algebra.FiniteField;
import securecompute.algebra.module.FiniteVectorSpace;
import securecompute.algebra.module.block.BlockFiniteVectorSpace;
import securecompute.constraint.LinearCode;
import securecompute.constraint.ZeroKnowledgeLocallyTestableProof;
import securecompute.constraint.block.BlockConstraint;
import securecompute.constraint.block.BlockLinearCode;
import securecompute.constraint.grid.GridLinearCode.SimpleGridEvidence;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ZeroKnowledgeGridProof<V, E> extends GridProof<V, E> implements ZeroKnowledgeLocallyTestableProof<List<V>> {

    private final Random random;
    private final FiniteField<E> field;
    private final TripleLayerConstraint<V, E> witnessConstraint;
    private final int width, height;
    private final int witnessWidth, witnessHeight;
    private final int paddingWidth, paddingHeight;
    private final int maxIndependentColumnCount, maxIndependentRowCount;

    public ZeroKnowledgeGridProof(GridLinearCode<V, E> topLayerCode, TripleLayerConstraint<V, E> witnessConstraint, Random random) {
        super(
                topLayerCode,
                TripleLayerConstraint.extendToSize(witnessConstraint,
                        topLayerCode.rowConstraint().dimension(), topLayerCode.columnConstraint().dimension())
        );

        this.random = random;
        this.field = topLayerCode.field();
        this.witnessConstraint = witnessConstraint;

        width = topLayerCode.rowConstraint().length();
        height = topLayerCode.columnConstraint().length();
        witnessWidth = witnessConstraint.rowConstraint().length();
        witnessHeight = witnessConstraint.columnConstraint().length();

        paddingWidth = topLayerCode.rowConstraint().dimension() - witnessWidth;
        paddingHeight = topLayerCode.columnConstraint().dimension() - witnessHeight;
        maxIndependentColumnCount = topLayerCode.rowConstraint().codistance() - witnessWidth - 1;
        maxIndependentRowCount = topLayerCode.columnConstraint().codistance() - witnessHeight - 1;

        if (maxIndependentColumnCount <= 0) {
            throw new IllegalArgumentException("Inner row code must have codistance at least 2 greater than the witness width");
        }
        if (maxIndependentRowCount <= 0) {
            throw new IllegalArgumentException("Inner column code must have codistance at least 2 greater than the witness height");
        }
        if (!topLayerCode.rowConstraint().isSystematic() || !topLayerCode.columnConstraint().isSystematic()) {
            throw new IllegalArgumentException("Inner row & column codes must be systematic");
        }
        if (topLayerOuterCode.rowConstraint().distance() <= witnessWidth) {
            throw new IllegalArgumentException("Outer row code must have greater distance than the witness width");
        }
        if (topLayerOuterCode.columnConstraint().distance() <= witnessHeight) {
            throw new IllegalArgumentException("Outer column code must have greater distance than the witness height");
        }
    }

    @Override
    public Random getRandom() {
        return random;
    }

    private <W> W randomElement(FiniteVectorSpace<W, E> space) {
        return space.pack(Stream.generate(() -> field.sampleUniformly(random))
                .limit(space.getDimension())
                .collect(ImmutableList.toImmutableList()));
    }

    private <W> Stream<W> randomElements(FiniteVectorSpace<W, E> space) {
        return Stream.generate(() -> randomElement(space));
    }

    @Override
    public TripleLayerConstraint<V, E> witnessConstraint() {
        return witnessConstraint;
    }

    @Override
    List<V> paddedTopLayer(List<List<V>> witness) {
        int paddedWidth = paddingWidth + witnessWidth;
        FiniteVectorSpace<V, E> symbolSpace = topLayerCode.symbolSpace();

        Stream<Stream<V>> topPaddingBlock = Stream.generate(() -> randomElements(symbolSpace).limit(paddedWidth))
                .limit(paddingHeight);

        List<V> topLayer = super.paddedTopLayer(witness);

        Stream<Stream<V>> paddedRows = Streams.zip(
                Stream.generate(() -> randomElements(symbolSpace).limit(paddingWidth)),
                Lists.partition(topLayer, witnessWidth).stream().map(List::stream),
                Stream::concat);

        return Stream.concat(topPaddingBlock, paddedRows)
                .flatMap(Function.identity())
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    List<List<V>> rowPaddingBlock() {
        LinearCode<V, E> outerColumnCode = topLayerOuterCode.columnConstraint();
        LinearCode<List<V>, E> outerColumnBlockCode = new BlockLinearCode<>(outerColumnCode, paddingWidth);

        List<List<V>> randomMessage = Stream.concat(
                randomElements(outerColumnBlockCode.symbolSpace())
                        .limit(outerColumnBlockCode.dimension() - witnessHeight),
                Stream.generate(outerColumnBlockCode.symbolSpace()::zero)
                        .limit(witnessHeight)
        )
                .collect(ImmutableList.toImmutableList());

        return outerColumnBlockCode.encode(randomMessage);
    }

    @Override
    List<List<V>> columnPaddingBlock() {
        // TODO: Very similar to code above - DEDUPLICATE:
        LinearCode<V, E> outerRowCode = topLayerOuterCode.rowConstraint();
        LinearCode<List<V>, E> outerRowBlockCode = new BlockLinearCode<>(outerRowCode, paddingHeight);

        List<List<V>> randomMessage = Stream.concat(
                randomElements(outerRowBlockCode.symbolSpace())
                        .limit(outerRowBlockCode.dimension() - witnessWidth),
                Stream.generate(outerRowBlockCode.symbolSpace()::zero)
                        .limit(witnessWidth)
        )
                .collect(ImmutableList.toImmutableList());

        return outerRowBlockCode.encode(randomMessage);
    }

    @Override
    public List<List<V>> decode(List<List<V>> codeword) {
        // Take the bottom right rectangle of the grid, of size witnessWidth * witnessHeight.
        return Lists.partition(codeword, width).stream()
                .map(v -> v.subList(width - witnessWidth, width).stream())
                .skip(height - witnessHeight)
                .flatMap(Function.identity())
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public double minimumAllowedFalseNegativeProbability() {
        int maxAllowedRepetitions = Math.min(maxIndependentRowCount, maxIndependentColumnCount);
        return ZeroKnowledgeRepeatedLocalTest.minimumAllowedFalseNegativeProbability(localTest(), maxAllowedRepetitions);
    }

    @Override
    public SimpleLocalTest localTest() {
        return new SimpleLocalTest();
    }

    @Override
    public RepeatedLocalTest localTest(double maxFalseNegativeProbability) {
        int maxAllowedRepetitions = Math.min(maxIndependentRowCount, maxIndependentColumnCount);
        return new RepeatedLocalTest(maxFalseNegativeProbability, maxAllowedRepetitions);
    }

    @Override
    public RepeatedLocalTest localTestOfMaximalPower() {
        return localTest(minimumAllowedFalseNegativeProbability());
    }

    public class SimpleLocalTest extends GridProof<V, E>.SimpleLocalTest
            implements ZeroKnowledgeLocalTest<List<V>, SimpleGridEvidence<List<V>>> {

        SimpleLocalTest() {
            super(witnessWidth, witnessHeight);
        }

        @Override
        protected SimpleGridEvidence<List<V>> evidence(int x, int y, List<List<V>> column, List<List<V>> row) {
            return new SimpleGridEvidence<List<V>>(x, y, column, row) {
                @Override
                public boolean isFailure() {
                    return !ZeroKnowledgeGridProof.this.rowConstraint().isValid(row) ||
                            !ZeroKnowledgeGridProof.this.columnConstraint().isValid(column);
                }
            };
        }

        @Override
        public SimpleGridEvidence<List<V>> query(List<List<V>> vector, Random random) {
            // The excluded rows & columns (those that pass through the witness) are at the bottom & right of the grid.
            return query(vector,
                    random.nextInt(width - witnessWidth),
                    random.nextInt(height - witnessHeight));
        }

        @Override
        public SimpleGridEvidence<List<V>> simulate(Random random) {
            return new RepeatedLocalTest().simulate(random).evidenceList().get(1);
        }
    }

    public class RepeatedLocalTest extends ZeroKnowledgeRepeatedLocalTest<List<V>, SimpleGridEvidence<List<V>>> {

        RepeatedLocalTest() {
            super(localTest(), 1);
        }

        RepeatedLocalTest(double maxFalseNegativeProbability, int maxAllowedRepetitions) {
            super(localTest(), maxFalseNegativeProbability, maxAllowedRepetitions);
        }

        @Override
        @SuppressWarnings("unchecked")
        public SimpleLocalTest singleTest() {
            return (SimpleLocalTest) super.singleTest();
        }

        @Override
        public RepeatedEvidence<SimpleGridEvidence<List<V>>> simulate(Random random) {

            List<List<V>> fullyErasedTestVector = Collections.nCopies(length(), null);
            RepeatedEvidence<SimpleGridEvidence<List<V>>> evidence = query(fullyErasedTestVector, random);

            SortedMap<Integer, List<List<V>>> sampledRows = new TreeMap<>();
            SortedMap<Integer, List<List<V>>> sampledColumns = new TreeMap<>();

            evidence.evidenceList().forEach(e -> {
                sampledRows.put(e.y, null);
                sampledColumns.put(e.x, null);
            });

            fillInFakeRowAndColumnSamples(sampledRows, sampledColumns);

            return new RepeatedEvidence<>(evidence.evidenceList().stream()
                    .map(e -> singleTest().evidence(e.x, e.y, sampledColumns.get(e.x), sampledRows.get(e.y)))
                    .collect(ImmutableList.toImmutableList()));
        }
    }

    private void fillInFakeRowAndColumnSamples(SortedMap<Integer, List<List<V>>> rows, SortedMap<Integer, List<List<V>>> columns) {
        FiniteVectorSpace<V, E> symbolSpace = topLayerCode.symbolSpace();

        Function<List<V>, List<V>> rowTopLayerFn, rowMiddleLayerFn, rowBottomLayerFn, rowParityFn;
        rowTopLayerFn = randomInterpolationFn(topLayerCode.rowConstraint(), ImmutableSortedSet.of());
        SortedSet<Integer> xIndices = ContiguousSet.closedOpen(width - witnessWidth, width);
        rowMiddleLayerFn = randomInterpolationFn(topLayerCode.rowConstraint(), xIndices);
        rowBottomLayerFn = randomInterpolationFn(topLayerOuterCode.rowConstraint(), xIndices);
        rowParityFn = witnessConstraint.rowConstraint()::zeroExtendedParity;

        for (int y : rows.keySet()) {
            List<V> rowTopLayer = rowTopLayerFn.apply(ImmutableList.of());
            List<V> rowMiddleLayer = rowMiddleLayerFn.apply(rowParityFn.apply(rowTopLayer.subList(width - witnessWidth, width)));
            List<V> rowBottomLayer = rowBottomLayerFn.apply(Collections.nCopies(witnessWidth, symbolSpace.zero()));

            List<List<V>> row = BlockConstraint.transpose(ImmutableList.of(rowTopLayer, rowMiddleLayer, rowBottomLayer), width);
            rows.put(y, row);
        }

        Function<List<V>, List<V>> colTopLayerFn, colMiddleLayerFn, colBottomLayerFn, colParityFn;
        colTopLayerFn = randomInterpolationFn(topLayerCode.columnConstraint(), ImmutableSortedSet.copyOf(rows.keySet()));
        SortedSet<Integer> yIndices = ImmutableSortedSet.copyOf(Sets.union(rows.keySet(), ContiguousSet.closedOpen(height - witnessHeight, height)));
        colMiddleLayerFn = randomInterpolationFn(topLayerOuterCode.columnConstraint(), yIndices);
        colBottomLayerFn = randomInterpolationFn(topLayerCode.columnConstraint(), yIndices);
        colParityFn = witnessConstraint.columnConstraint()::zeroExtendedParity;

        for (int x : columns.keySet()) {
            List<List<V>> crossovers = rows.values().stream()
                    .map(row -> row.get(x))
                    .collect(ImmutableList.toImmutableList());

            List<List<V>> crossoverLayers = BlockConstraint.transpose(crossovers, 3);

            List<V> colTopLayer = colTopLayerFn.apply(crossoverLayers.get(0));
            List<V> colMiddleLayer = colMiddleLayerFn.apply(concat(crossoverLayers.get(1),
                    Collections.nCopies(witnessHeight, symbolSpace.zero())));
            List<V> colBottomLayer = colBottomLayerFn.apply(concat(crossoverLayers.get(2),
                    colParityFn.apply(colTopLayer.subList(height - witnessHeight, height))));

            List<List<V>> column = BlockConstraint.transpose(ImmutableList.of(colTopLayer, colMiddleLayer, colBottomLayer), height);
            columns.put(x, column);
        }
    }

    private static <W> List<W> concat(List<W> left, List<W> right) {
        return Stream.concat(left.stream(), right.stream()).collect(ImmutableList.toImmutableList());
    }

    private <W> Function<List<W>, List<W>> randomInterpolationFn(LinearCode<W, E> code, SortedSet<Integer> knownSampleIndices) {
        FiniteVectorSpace<W, E> symbolSpace = code.symbolSpace();
        FiniteVectorSpace<List<W>, E> messageSpace = new BlockFiniteVectorSpace<>(symbolSpace, code.dimension());
        FiniteVectorSpace<List<W>, E> codewordSpace = new BlockFiniteVectorSpace<>(symbolSpace, code.length());

        Function<List<W>, List<W>> linearInterpolationFn = code.interpolationFn(knownSampleIndices);

        return samples -> {
            // TODO: This can be simplified slightly if we make linear codes vector spaces:
            List<W> randomCodeword = code.encode(randomElement(messageSpace));

            List<W> offsetSamples = Streams.zip(samples.stream(), knownSampleIndices.stream(),
                    (x, i) -> symbolSpace.difference(x, randomCodeword.get(i)))
                    .collect(ImmutableList.toImmutableList());

            List<W> offsetCodeword = linearInterpolationFn.apply(offsetSamples);
            return codewordSpace.sum(offsetCodeword, randomCodeword);
        };
    }
}
