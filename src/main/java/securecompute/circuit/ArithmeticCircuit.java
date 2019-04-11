package securecompute.circuit;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.graph.*;
import securecompute.algebra.FiniteField;
import securecompute.algebra.polynomial.BasePolynomialExpression;
import securecompute.algebra.polynomial.PolynomialExpression;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static securecompute.algebra.polynomial.BasePolynomialExpression.variable;

@AutoValue
public abstract class ArithmeticCircuit<E> {

    public abstract int degree();

    public abstract int maximumFanIn();

    public abstract int maximumFanOut();

    public abstract FiniteField<E> field();

    public abstract Network<Gate<E>, Wire<E>> network();

    public abstract Optional<InputPort<E>> inputPort();

    public abstract Optional<OutputPort<E>> outputPort();

    @Memoized
    public List<Gate<E>> gatesInTopologicalOrder() {
        MutableGraph<Gate<E>> graph = Graphs.copyOf(network().asGraph());
        inputPort().ifPresent(graph::removeNode);
        outputPort().ifPresent(graph::removeNode);

        ImmutableList.Builder<Gate<E>> sortedGates = ImmutableList.builderWithExpectedSize(graph.nodes().size() + 2);
        sortedGates.add(inputPort().orElseGet(() -> new InputPort<>(field(), 0)));

        Set<Gate<E>> startGates = graph.nodes().stream()
                .filter(g -> graph.inDegree(g) == 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        while (!startGates.isEmpty()) {
            Gate<E> gate = startGates.iterator().next();

            startGates.remove(gate);
            sortedGates.add(gate);

            for (Gate<E> successor : graph.successors(gate)) {
                graph.removeEdge(gate, successor);
                if (graph.inDegree(successor) == 0) {
                    startGates.add(successor);
                }
            }
            graph.removeNode(gate);
        }

        sortedGates.add(outputPort().orElseGet(() -> new OutputPort<>(field(), 0)));
        return sortedGates.build();
    }

    private void evaluate(List<E> inputs, Consumer<List<E>> gateStateSink) {
        Map<Wire<E>, E> front = new HashMap<>();

        for (Gate<E> gate : gatesInTopologicalOrder()) {
            AlgebraicFunction<E> gateFn = gate.function();
            List<E> gateState;
            if (gate instanceof ArithmeticCircuit.InputPort) {
                gateStateSink.accept(gateState = inputs);
            } else {
                List<E> gateInputs = ImmutableList.copyOf(network().inEdges(gate).stream()
                        .collect(Collectors.toMap(Wire::toIndex, front::remove, gate::matchInputs))
                        .values());
                gateStateSink.accept(gateState = gateFn.baseFn().apply(gateInputs));
                if (gate instanceof ArithmeticCircuit.OutputPort) {
                    return;
                }
            }
            List<E> gateOutputs = gateState.subList(gateFn.inputLength() + gateFn.auxiliaryLength(), gateFn.length());
            Set<Wire<E>> outWires = gate instanceof ArithmeticCircuit.InputPort && !inputPort().isPresent()
                    ? ImmutableSet.of()
                    : network().outEdges(gate);

            // noinspection ConstantConditions
            front.putAll(Maps.asMap(outWires, w -> gateOutputs.get(w.fromIndex())));
        }
        throw new AssertionError(); // unreachable
    }

    private List<E> evaluateCircuitState(List<E> input, int expectedLength) {
        ImmutableList.Builder<E> builder = ImmutableList.builderWithExpectedSize(expectedLength);
        evaluate(input, builder::addAll);
        return builder.build();
    }

    private List<PolynomialExpression<E>> parityCheckTerms() {
        ImmutableList.Builder<PolynomialExpression<E>> terms = ImmutableList.builder();
        int offset = 0;
        for (Gate<E> gate : gatesInTopologicalOrder()) {
            int finalOffset = offset;
            // noinspection ConstantConditions
            terms.addAll(Lists.transform(gate.function().parityCheckTerms(), p -> p.mapIndices(i -> i + finalOffset)));
            offset += gate.function().length();
        }
        for (Wire<E> wire : network().edges()) {
            BasePolynomialExpression.Variable<E> fromVar = variable(wire.fromIndex()), toVar = variable(wire.toIndex());
            terms.add(fromVar.subtract(toVar, field()));
        }
        return terms.build();
    }

    @Memoized
    public AlgebraicFunction<E> asFunction() {
        int inputLength = inputPort().isPresent() ? inputPort().get().function().length() : 0;
        int outputLength = outputPort().isPresent() ? outputPort().get().function().length() : 0;
        int length = gatesInTopologicalOrder().stream().mapToInt(g -> g.function().length()).sum();

        return AlgebraicFunction.builder(field())
                .degree(degree())
                .inputLength(inputLength)
                .outputLength(outputLength)
                .auxiliaryLength(length - inputLength - outputLength)
                .parityCheckTerms(parityCheckTerms())
                .baseFn(v -> evaluateCircuitState(v, length))
                .build();
    }

    public static class Gate<E> {

        private final AlgebraicFunction<E> function;
        private final String name;

        public Gate(AlgebraicFunction<E> function, String name) {
            this.function = Objects.requireNonNull(function);
            this.name = name;
        }

        public Gate(AlgebraicFunction<E> function) {
            this(function, null);
        }

        public AlgebraicFunction<E> function() {
            return function;
        }

        @Override
        public String toString() {
            return name != null ? name : getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
        }

        private E matchInputs(E x, E y) {
            if (!x.equals(y)) {
                throw new IllegalArgumentException("Mismatched fan-in inputs " + x + " and " + y + " at gate: " + this);
            }
            return x;
        }
    }

    public static class InputPort<E> extends Gate<E> {

        public InputPort(FiniteField<E> field, int length) {
            super(AlgebraicFunction.inputPortFn(field, length));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{length=" + function().length() + "}";
        }
    }

    public static class OutputPort<E> extends Gate<E> {

        public OutputPort(FiniteField<E> field, int length) {
            super(AlgebraicFunction.outputPortFn(field, length));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{length=" + function().length() + "}";
        }
    }

    @AutoValue
    public static abstract class Wire<E> {

        public abstract Gate<E> fromGate();

        public abstract int fromIndex();

        public abstract Gate<E> toGate();

        public abstract int toIndex();
    }

    public static <E> Builder<E> builder(FiniteField<E> field) {
        Builder<E> builder = new AutoValue_ArithmeticCircuit.Builder<E>();
        return builder
                .field(field)
                .network(NetworkBuilder.directed().allowsParallelEdges(true).build());
    }

    @AutoValue.Builder
    public static abstract class Builder<E> {

        public abstract Builder<E> degree(int degree);

        public abstract Builder<E> maximumFanIn(int maximumFanIn);

        public abstract Builder<E> maximumFanOut(int maximumFanOut);

        abstract Builder<E> field(FiniteField<E> field);

        abstract Builder<E> network(Network<Gate<E>, Wire<E>> network);

        @SuppressWarnings("UnusedReturnValue")
        abstract Builder<E> inputPort(InputPort<E> inputPort);

        @SuppressWarnings("UnusedReturnValue")
        abstract Builder<E> outputPort(OutputPort<E> outputPort);

        abstract Optional<Integer> degree();

        abstract Optional<Integer> maximumFanIn();

        abstract Optional<Integer> maximumFanOut();

        public abstract FiniteField<E> field();

        abstract Network<Gate<E>, Wire<E>> network();

        public abstract Optional<InputPort<E>> inputPort();

        public abstract Optional<OutputPort<E>> outputPort();

        public Builder<E> addGate(Gate<E> gate) {
            if (!gate.function().field().equals(field())) {
                throw new IllegalArgumentException("Cannot add gate over a mismatched field");
            }
            if (gate instanceof ArithmeticCircuit.InputPort) {
                if (inputPort().isPresent()) {
                    throw new IllegalStateException("Already added an input port");
                }
                inputPort((InputPort<E>) gate);
            }
            if (gate instanceof ArithmeticCircuit.OutputPort) {
                if (outputPort().isPresent()) {
                    throw new IllegalStateException("Already added an output port");
                }
                outputPort((OutputPort<E>) gate);
            }
            ((MutableNetwork<Gate<E>, Wire<E>>) network()).addNode(gate);
            return this;
        }

        public Builder<E> addWire(Gate<E> fromGate, int fromIndex, Gate<E> toGate, int toIndex) {
            Preconditions.checkPositionIndex(fromIndex, fromGate.function().outputLength(), "fromIndex");
            Preconditions.checkPositionIndex(toIndex, toGate.function().inputLength(), "toIndex");

            ((MutableNetwork<Gate<E>, Wire<E>>) network()).addEdge(fromGate, toGate,
                    new AutoValue_ArithmeticCircuit_Wire<>(fromGate, fromIndex, toGate, toIndex));
            return this;
        }

        public Builder<E> addWire(Gate<E> fromGate, Gate<E> toGate) {
            return addWire(
                    fromGate, firstUnusedOutputIndexOrLast(fromGate),
                    toGate, firstUnusedInputIndexOrLast(toGate)
            );
        }

        public Builder<E> addWires(Gate<E> fromGate, int fromIndexStart, int fromIndexStep,
                                   Gate<E> toGate, int toIndexStart, int toIndexStep, int n) {

            for (int i = 0; i < n; i++, fromIndexStart += fromIndexStep, toIndexStart += toIndexStep) {
                addWire(fromGate, fromIndexStart, toGate, toIndexStart);
            }
            return this;
        }

        public Builder<E> addWires(Gate<E> fromGate, int fromIndexStart,
                                   Gate<E> toGate, int toIndexStart, int n) {

            return addWires(fromGate, fromIndexStart, 1, toGate, toIndexStart, 1, n);
        }

        public Builder<E> addWires(Gate<E> fromGate, Gate<E> toGate, int n) {
            return addWires(
                    fromGate, firstUnusedOutputIndexOrLast(fromGate),
                    toGate, firstUnusedInputIndexOrLast(toGate), n
            );
        }

        private int firstUnusedIndexOrLast(Set<Integer> usedIndices, int length) {
            return IntStream.range(0, length)
                    .filter(i -> !usedIndices.contains(i))
                    .findFirst().orElse(length - 1);
        }

        private int firstUnusedInputIndexOrLast(Gate<E> gate) {
            return firstUnusedIndexOrLast(network().inEdges(gate).stream()
                            .map(Wire::toIndex)
                            .collect(Collectors.toSet()),
                    gate.function().inputLength());
        }

        private int firstUnusedOutputIndexOrLast(Gate<E> gate) {
            return firstUnusedIndexOrLast(network().outEdges(gate).stream()
                            .map(Wire::fromIndex)
                            .collect(Collectors.toSet()),
                    gate.function().outputLength());
        }

        abstract ArithmeticCircuit<E> rawBuild();

        public ArithmeticCircuit<E> build() {
            int degree = -1, fanIn = 0, fanOut = 0;

            for (Gate<E> gate : network().nodes()) {
                Set<Wire<E>> inputs = network().inEdges(gate);
                Set<Wire<E>> outputs = network().outEdges(gate);

                long inputIndexCount = inputs.stream().mapToInt(Wire::toIndex).distinct().count();
                long outputIndexCount = outputs.stream().mapToInt(Wire::fromIndex).distinct().count();

                fanIn = Math.max(fanIn, inputs.stream().collect(Collectors.groupingBy(Wire::toIndex, Collectors.counting()))
                        .values().stream().mapToInt(Long::intValue).max().orElse(0)
                );
                fanOut = Math.max(fanOut, outputs.stream().collect(Collectors.groupingBy(Wire::fromIndex, Collectors.counting()))
                        .values().stream().mapToInt(Long::intValue).max().orElse(0)
                );
                degree = Math.max(degree, gate.function().degree());

                if (inputIndexCount < gate.function().inputLength()) {
                    throw new IllegalStateException("Floating inputs at gate: " + gate);
                }
                if (outputIndexCount < gate.function().outputLength()) {
                    throw new IllegalStateException("Floating outputs at gate: " + gate);
                }
                if (degree > degree().orElse(Integer.MAX_VALUE)) {
                    throw new IllegalStateException("Specified degree exceeded at gate: " + gate);
                }
                if (fanIn > maximumFanIn().orElse(Integer.MAX_VALUE)) {
                    throw new IllegalStateException("Maximum allowed fan-in exceeded at gate: " + gate);
                }
                if (fanOut > maximumFanOut().orElse(Integer.MAX_VALUE)) {
                    throw new IllegalStateException("Maximum allowed fan-out exceeded at gate: " + gate);
                }
            }

            if (Graphs.hasCycle(network())) {
                throw new IllegalStateException("Gate network must be acyclic");
            }
            return maximumFanIn(fanIn).maximumFanOut(fanOut).degree(degree)
                    .network(ImmutableNetwork.copyOf(network()))
                    .rawBuild();
        }
    }
}
