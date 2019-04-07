package securecompute.algebra.polynomial;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import securecompute.algebra.Ring;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

// TODO: Consider using @AutoOneOf instead of @AutoValue for each subclass here, to simplify.

public abstract class BasePolynomialExpression<E> implements PolynomialExpression<E> {

    private final Type expressionType;

    private BasePolynomialExpression(Type expressionType) {
        this.expressionType = expressionType;
    }

    @Override
    public Type expressionType() {
        return expressionType;
    }

    @Override
    public E constantValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int variableIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PolynomialExpression<E>> subTerms() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolynomialExpression<E> mapIndices(IntUnaryOperator indexMapping) {
        return mapVariablesAndConstants(i -> variable(indexMapping.applyAsInt(i)), BasePolynomialExpression::constant);
    }

    public static <E> Constant<E> constant(E value) {
        return new AutoValue_BasePolynomialExpression_Constant<>(value);
    }

    public static <E> Sum<E> sum(List<PolynomialExpression<E>> terms) {
        return new AutoValue_BasePolynomialExpression_Sum<>(ImmutableList.copyOf(terms));
    }

    public static <E> Product<E> product(List<PolynomialExpression<E>> terms) {
        return new AutoValue_BasePolynomialExpression_Product<>(ImmutableList.copyOf(terms));
    }

    public static <E> Variable<E> variable(int index) {
        return new AutoValue_BasePolynomialExpression_Variable<>(index);
    }

    public Sum<E> add(PolynomialExpression<E> other) {
        return sum(ImmutableList.of(this, other));
    }

    public Sum<E> subtract(PolynomialExpression<E> other, Ring<E> ring) {
        return add(constant(ring.fromInt(-1)).multiply(other));
    }

    public Product<E> multiply(PolynomialExpression<E> other) {
        return product(ImmutableList.of(this, other));
    }

    @AutoValue
    public static abstract class Constant<E> extends BasePolynomialExpression<E> {

        Constant() {
            super(Type.CONSTANT);
        }

        @Override
        public abstract E constantValue();

        @Override
        @SuppressWarnings("unchecked")
        public <F> PolynomialExpression<F> mapVariablesAndConstants(IntFunction<PolynomialExpression<F>> variableMapping,
                                                                    Function<E, PolynomialExpression<F>> constantMapping) {
            return constantMapping.apply(constantValue());
        }
    }

    @AutoValue
    public static abstract class Sum<E> extends BasePolynomialExpression<E> {

        Sum() {
            super(Type.SUM);
        }

        @Override
        public abstract List<PolynomialExpression<E>> subTerms();

        @Override
        public Sum<E> add(PolynomialExpression<E> other) {
            return sum(ImmutableList.<PolynomialExpression<E>>builderWithExpectedSize(subTerms().size() + 1)
                    .addAll(subTerms()).add(other).build());
        }

        @Override
        public <F> PolynomialExpression<F> mapVariablesAndConstants(IntFunction<PolynomialExpression<F>> variableMapping,
                                                                    Function<E, PolynomialExpression<F>> constantMapping) {
            return sum(subTerms().stream()
                    .map(p -> p.mapVariablesAndConstants(variableMapping, constantMapping))
                    .collect(ImmutableList.toImmutableList()));
        }
    }

    @AutoValue
    public static abstract class Product<E> extends BasePolynomialExpression<E> {

        Product() {
            super(Type.PRODUCT);
        }

        @Override
        public abstract List<PolynomialExpression<E>> subTerms();

        @Override
        public Product<E> multiply(PolynomialExpression<E> other) {
            return product(ImmutableList.<PolynomialExpression<E>>builderWithExpectedSize(subTerms().size() + 1)
                    .addAll(subTerms()).add(other).build());
        }

        @Override
        public <F> PolynomialExpression<F> mapVariablesAndConstants(IntFunction<PolynomialExpression<F>> variableMapping,
                                                                    Function<E, PolynomialExpression<F>> constantMapping) {
            return product(subTerms().stream()
                    .map(p -> p.mapVariablesAndConstants(variableMapping, constantMapping))
                    .collect(ImmutableList.toImmutableList()));
        }
    }

    @AutoValue
    public static abstract class Variable<E> extends BasePolynomialExpression<E> {

        Variable() {
            super(Type.VARIABLE);
        }

        @Override
        public abstract int variableIndex();

        @Override
        @SuppressWarnings("unchecked")
        public <F> PolynomialExpression<F> mapVariablesAndConstants(IntFunction<PolynomialExpression<F>> variableMapping,
                                                                    Function<E, PolynomialExpression<F>> constantMapping) {
            return variableMapping.apply(variableIndex());
        }
    }
}
