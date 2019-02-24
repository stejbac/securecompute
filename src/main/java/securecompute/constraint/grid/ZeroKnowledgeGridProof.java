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
    private final GridLinearCode<V, E> puncturedTopLayerOuterCode;
    private final int width, height;
    private final int witnessWidth, witnessHeight;
    private final int paddingWidth, paddingHeight;
    private final int maxIndependentColumnCount, maxIndependentRowCount;

    public ZeroKnowledgeGridProof(GridLinearCode<V, E> topLayerCode, TripleLayerConstraint<V, E> witnessConstraint, Random random) {
        this(topLayerCode, shorten(topLayerCode, witnessConstraint), witnessConstraint, random);
    }

    private ZeroKnowledgeGridProof(GridLinearCode<V, E> topLayerCode, GridLinearCode<V, E> shortenedTopLayerCode,
                                   TripleLayerConstraint<V, E> witnessConstraint, Random random) {
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
        maxIndependentColumnCount = shortenedTopLayerCode.rowConstraint().codistance() - 1;
        maxIndependentRowCount = shortenedTopLayerCode.columnConstraint().codistance() - 1;

        if (maxIndependentColumnCount <= 0) {
            throw new IllegalArgumentException("Shortened inner row code must have codistance at least 2");
        }
        if (maxIndependentRowCount <= 0) {
            throw new IllegalArgumentException("Shortened inner column code must have codistance at least 2");
        }
        if (topLayerOuterCode.rowConstraint().distance() <= witnessWidth) {
            throw new IllegalArgumentException("Outer row code must have greater distance than the witness width");
        }
        if (topLayerOuterCode.columnConstraint().distance() <= witnessHeight) {
            throw new IllegalArgumentException("Outer column code must have greater distance than the witness height");
        }

        puncturedTopLayerOuterCode = puncture(topLayerOuterCode, witnessConstraint);
    }

    private static <W, E> GridLinearCode<W, E> shorten(GridLinearCode<W, E> innerCode, GridConstraint<?> witnessConstraint) {
        if (!innerCode.rowConstraint().isSystematic() || !innerCode.columnConstraint().isSystematic()) {
            throw new IllegalArgumentException("Inner row & column codes must be systematic");
        }
        LinearCode<W, E> rowCode = new ShortenedLinearCode<>(innerCode.rowConstraint(), witnessConstraint.rowConstraint().length());
        LinearCode<W, E> colCode = new ShortenedLinearCode<>(innerCode.columnConstraint(), witnessConstraint.columnConstraint().length());
        return new GridLinearCode<>(rowCode, colCode);
    }

    private static <W, E> GridLinearCode<W, E> puncture(GridLinearCode<W, E> outerCode, GridConstraint<?> witnessConstraint) {
        LinearCode<W, E> rowCode = new PuncturedLinearCode<>(outerCode.rowConstraint(), witnessConstraint.rowConstraint().length());
        LinearCode<W, E> colCode = new PuncturedLinearCode<>(outerCode.columnConstraint(), witnessConstraint.columnConstraint().length());
        return new GridLinearCode<>(rowCode, colCode);
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
        // Take the bottom right rectangle of the _decoded_ grid, of size witnessWidth * witnessHeight.
        // (This doesn't assume that the top layer code is systematic.)
        return Lists.partition(innerGridCode.decode(codeword), witnessWidth + paddingWidth).stream()
                .map(v -> v.subList(paddingWidth, witnessWidth + paddingWidth).stream())
                .skip(paddingHeight)
                .flatMap(Function.identity())
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public double minimumAllowedFalsePositiveProbability() {
        int maxAllowedRepetitions = Math.min(maxIndependentRowCount, maxIndependentColumnCount);
        return ZeroKnowledgeRepeatedLocalTest.minimumAllowedFalsePositiveProbability(localTest(), maxAllowedRepetitions);
    }

    @Override
    public SimpleLocalTest localTest() {
        return new SimpleLocalTest();
    }

    @Override
    public RepeatedLocalTest localTest(double maxFalsePositiveProbability) {
        int maxAllowedRepetitions = Math.min(maxIndependentRowCount, maxIndependentColumnCount);
        return new RepeatedLocalTest(maxFalsePositiveProbability, maxAllowedRepetitions);
    }

    @Override
    public RepeatedLocalTest localTestOfMaximalConfidence() {
        return localTest(minimumAllowedFalsePositiveProbability());
    }

    public class SimpleLocalTest extends GridLinearCode.SimpleLocalTest<List<V>>
            implements ZeroKnowledgeLocalTest<List<V>, SimpleGridEvidence<List<V>>> {

        private SimpleLocalTest() {
            super(rowConstraint(), columnConstraint(), puncturedTopLayerOuterCode);
        }

        @Override
        public SimpleGridEvidence<List<V>> simulate(Random random) {
            return new RepeatedLocalTest().simulate(random).evidenceList().get(0);
        }
    }

    public class RepeatedLocalTest extends ZeroKnowledgeRepeatedLocalTest<List<V>, SimpleGridEvidence<List<V>>> {

        RepeatedLocalTest() {
            super(localTest(), 1);
        }

        RepeatedLocalTest(double maxFalsePositiveProbability, int maxAllowedRepetitions) {
            super(localTest(), maxFalsePositiveProbability, maxAllowedRepetitions);
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
                if (e.y >= 0) {
                    sampledRows.put(e.y, null);
                } else {
                    sampledColumns.put(e.x, null);
                }
            });

            fillInFakeRowAndColumnSamples(sampledRows, sampledColumns);

            return new RepeatedEvidence<>(evidence.evidenceList().stream()
                    .map(e -> singleTest().evidence(e.x, e.y, e.y >= 0 ? sampledRows.get(e.y) : sampledColumns.get(e.x)))
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
