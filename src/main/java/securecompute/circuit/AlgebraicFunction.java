package securecompute.circuit;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import securecompute.algebra.BooleanField;
import securecompute.algebra.FiniteField;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.algebra.polynomial.BasePolynomialExpression;
import securecompute.algebra.polynomial.PolynomialExpression;
import securecompute.constraint.AlgebraicConstraint;
import securecompute.constraint.block.BlockConstraint;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static securecompute.algebra.polynomial.BasePolynomialExpression.*;

public abstract class AlgebraicFunction<E> implements Function<List<E>, List<E>>, AlgebraicConstraint<E, E> {

    public abstract int inputLength();

    public abstract int outputLength();

    public abstract int auxiliaryLength();

    public abstract Function<List<E>, List<E>> baseFn();

    public abstract List<PolynomialExpression<E>> parityCheckTerms();

    @Override
    public abstract FiniteField<E> field();

    @Override
    public int length() {
        return inputLength() + auxiliaryLength() + outputLength();
    }

    @Override
    public int redundancy() {
        return parityCheckTerms().size();
    }

    @Override
    public List<E> parityCheck(List<E> vector) {
        return parityCheck(field(), parityCheckTerms(), vector).collect(ImmutableList.toImmutableList());
    }

    private static <E> Stream<E> parityCheck(FiniteField<E> field, List<PolynomialExpression<E>> terms, List<E> vector) {
        return terms.stream().map(p -> p.evaluate(field, vector::get));
    }

    private static <E> List<E> zeroExtend(FiniteField<E> field, List<E> vector, int rightPadding) {
        // TODO: This is very similar to PuncturedPolynomialCode.paddedCoefficients - DEDUPLICATE:
        return ImmutableList.<E>builderWithExpectedSize(vector.size() + rightPadding)
                .addAll(vector)
                .addAll(Collections.nCopies(rightPadding, field.zero()))
                .build();
    }

    @Override
    public List<E> apply(List<E> inputVector) {
        return baseFn().apply(inputVector).subList(inputLength() + auxiliaryLength(), length());
    }

    @SuppressWarnings("unchecked")
    public static <E> Builder<E, ?, ?> builder(FiniteField<E> field) {
        if (field == BooleanField.INSTANCE) {
            return (Builder<E, ?, ?>) BooleanFunction.builder();
        }
        return new AutoValue_AlgebraicFunction_Impl.Builder<E>().field(field);
    }

    // TODO: Add validation to this builder & replace lists with immutable copies:

    public interface Builder<E, B extends Builder<E, B, R>, R extends AlgebraicFunction<E>> {

        B inputLength(int inputLength);

        B outputLength(int outputLength);

        B auxiliaryLength(int auxiliaryLength);

        B degree(int degree);

        B baseFn(Function<List<E>, List<E>> baseFn);

        B parityCheckTerms(List<PolynomialExpression<E>> parityCheckTerms);

        FiniteField<E> field();

        List<PolynomialExpression<E>> parityCheckTerms();

        default B simpleBaseFn(Function<E, E> unaryFn) {
            return baseFn(v -> ImmutableList.of(v.get(0), unaryFn.apply(v.get(0))))
                    .inputLength(1).auxiliaryLength(0).outputLength(1);
        }

        default B simpleBaseFn(BiFunction<E, E, E> binaryFn) {
            return baseFn(v -> ImmutableList.of(v.get(0), v.get(1), binaryFn.apply(v.get(0), v.get(1))))
                    .inputLength(2).auxiliaryLength(0).outputLength(1);
        }

        default B simpleBaseFn() {
            int outputLength = parityCheckTerms().size();
            return baseFn(v -> Stream.concat(v.stream(), parityCheck(
                    field(), parityCheckTerms(), zeroExtend(field(), v, outputLength))
            )
                    .collect(ImmutableList.toImmutableList()))
                    .auxiliaryLength(0).outputLength(outputLength);
        }

        R build();
    }

    static <E> AlgebraicFunction<E> inputPortFn(FiniteField<E> field, int length) {
        return builder(field).degree(-1).inputLength(0).outputLength(length).auxiliaryLength(0)
                .baseFn(v -> {
                    throw new UnsupportedOperationException("indeterminate");
                })
                .parityCheckTerms(ImmutableList.of())
                .build();
    }

    static <E> AlgebraicFunction<E> outputPortFn(FiniteField<E> field, int length) {
        return builder(field).degree(-1).inputLength(length).outputLength(0).auxiliaryLength(0)
                .baseFn(v -> v)
                .parityCheckTerms(ImmutableList.of())
                .build();
    }

    public static <E> AlgebraicFunction<E> vectorFn(AlgebraicFunction<E> function, int n) {
        return builder(function.field()).degree(function.degree())
                .inputLength(n * function.inputLength())
                .outputLength(n * function.outputLength())
                .auxiliaryLength(n * function.auxiliaryLength())
                .parityCheckTerms(function.parityCheckTerms().stream()
                        .flatMap(p -> IntStream.range(0, n).mapToObj(i -> p.mapIndices(j -> i + n * j)))
                        .collect(ImmutableList.toImmutableList())
                )
                .baseFn(v -> BlockConstraint.streamLayers(
                        BlockConstraint.streamLayers(Lists.partition(v, n), n)
                                .map(function.baseFn())
                                .collect(ImmutableList.toImmutableList()), function.length()
                        ).flatMap(List::stream).collect(ImmutableList.toImmutableList())
                )
                .build();
    }

    public static <E> AlgebraicFunction<E> sumFn(FiniteField<E> field, int n) {
        return builder(field).degree(1).inputLength(n)
                .parityCheckTerms(ImmutableList.of(sum(IntStream.range(0, n)
                        .mapToObj(BasePolynomialExpression::<E>variable)
                        .collect(ImmutableList.toImmutableList())
                ).subtract(variable(n), field)))
                .simpleBaseFn()
                .build();
    }

    public static <E> AlgebraicFunction<E> productFn(FiniteField<E> field, int n) {
        return builder(field).degree(Math.max(n, 1)).inputLength(n)
                .parityCheckTerms(ImmutableList.of(product(IntStream.range(0, n)
                        .mapToObj(BasePolynomialExpression::<E>variable)
                        .collect(ImmutableList.toImmutableList())
                ).subtract(variable(n), field)))
                .simpleBaseFn()
                .build();
    }

    // FIXME: This class is named 'Impl' in its auto-generated 'toString()' output. Make it use a more meaningful name:
    @AutoValue
    static abstract class Impl<E> extends AlgebraicFunction<E> {

        @Override
        @Memoized
        public SingletonVectorSpace<E> symbolSpace() {
            return new SingletonVectorSpace<>(field());
        }

        @AutoValue.Builder
        interface ImplBuilder<E> extends AlgebraicFunction.Builder<E, ImplBuilder<E>, Impl<E>> {

            ImplBuilder<E> field(FiniteField<E> field);
        }
    }
}
