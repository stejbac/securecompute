package securecompute.circuit;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import securecompute.algebra.BooleanField;
import securecompute.algebra.module.singleton.SingletonVectorSpace;
import securecompute.algebra.polynomial.BasePolynomialExpression.Constant;
import securecompute.algebra.polynomial.BasePolynomialExpression.Variable;

import static securecompute.algebra.polynomial.BasePolynomialExpression.constant;
import static securecompute.algebra.polynomial.BasePolynomialExpression.variable;

@AutoValue
public abstract class BooleanFunction extends AlgebraicFunction<Boolean> {

    private static final SingletonVectorSpace<Boolean> SYMBOL_SPACE = new SingletonVectorSpace<>(BooleanField.INSTANCE);
    private static final Constant<Boolean> _1 = constant(true);
    private static final Variable<Boolean> X = variable(0), Y = variable(1), Z = variable(2);

    public static final BooleanFunction AND = builder()
            .degree(2)
            .simpleBaseFn(Boolean::logicalAnd)
            .parityCheckTerms(ImmutableList.of(X.multiply(Y).add(Z)))
            .build();

    @SuppressWarnings("ConstantConditions") // IDEA thinks that 'logicalOr' always returns true (a bug?)
    public static final BooleanFunction OR = builder()
            .degree(2)
            .simpleBaseFn(Boolean::logicalOr)
            .parityCheckTerms(ImmutableList.of(X.add(_1).multiply(Y.add(_1)).add(Z).add(_1)))
            .build();

    @SuppressWarnings("ConstantConditions") // IDEA thinks that 'logicalXor' always returns false (a bug?)
    public static final BooleanFunction XOR = builder()
            .degree(1)
            .simpleBaseFn(Boolean::logicalXor)
            .parityCheckTerms(ImmutableList.of(X.add(Y).add(Z)))
            .build();

    public static final BooleanFunction NOT = builder()
            .degree(1)
            .simpleBaseFn(b -> !b)
            .parityCheckTerms(ImmutableList.of(X.add(Y).add(_1)))
            .build();

    @Override
    public BooleanField field() {
        return BooleanField.INSTANCE;
    }

    @Override
    public SingletonVectorSpace<Boolean> symbolSpace() {
        return SYMBOL_SPACE;
    }

    public static Builder builder() {
        return new AutoValue_BooleanFunction.Builder();
    }

    @AutoValue.Builder
    public interface Builder extends AlgebraicFunction.Builder<Boolean, Builder, BooleanFunction> {

        @Override
        default BooleanField field() {
            return BooleanField.INSTANCE;
        }
    }
}
