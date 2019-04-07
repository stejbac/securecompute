package securecompute.algebra.polynomial;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import securecompute.algebra.Ring;

import java.util.List;

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
    }

    @AutoValue
    public static abstract class Variable<E> extends BasePolynomialExpression<E> {

        Variable() {
            super(Type.VARIABLE);
        }

        @Override
        public abstract int variableIndex();
    }
}
