package securecompute.algebra;

import securecompute.ShallowCopyable;

import java.math.BigInteger;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class QuotientField<E> extends ShallowCopyable implements Field<QuotientField<E>.Coset> {

    private final EuclideanDomain<E> baseRing;
    private final E idealGenerator;
    private final QuotientField<E>.Coset zero, one;

    public QuotientField(EuclideanDomain<E> baseRing, E idealGenerator) {
        this.baseRing = checkNotNull(baseRing);
        this.idealGenerator = baseRing.abs(idealGenerator);
        zero = coset(baseRing.zero());
        one = coset(baseRing.one());
    }

    @Override
    protected QuotientField<E> shallowCopy() {
        return new QuotientField<>(baseRing, idealGenerator);
    }

    public EuclideanDomain<E> getBaseRing() {
        return baseRing;
    }

    public E getIdealGenerator() {
        return idealGenerator;
    }

    @Override
    public Coset zero() {
        return zero;
    }

    @Override
    public Coset one() {
        return one;
    }

    @Override
    public Coset fromBigInteger(BigInteger n) {
        return coset(baseRing.fromBigInteger(n));
    }

    @Override
    public Coset sum(Coset left, Coset right) {
        QuotientField<E> rightField;
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(rightField = right.getField()), "RHS field mismatch");
        return right == zero ? left : left == zero && this == rightField ? right : coset(baseRing.sum(left.witness, right.witness));
    }

    @Override
    public Coset product(Coset left, Coset right) {
        QuotientField<E> rightField;
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(rightField = right.getField()), "RHS field mismatch");
        if (left == zero || right == zero) {
            return zero;
        }
        return right == one ? left : left == one && this == rightField ? right : coset(baseRing.product(left.witness, right.witness));
    }

    @Override
    public Coset negative(Coset elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        return coset(baseRing.negative(elt.witness));
    }

    @Override
    public Coset reciprocalOrZero(Coset elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        EuclideanDomain.GcdExtResult<E> gcdExtResult = baseRing.gcdExt(elt.witness, idealGenerator);
        if (!gcdExtResult.getGcd().equals(baseRing.one()) && !gcdExtResult.getX().equals(baseRing.zero())) {
            throw new ReducibleGeneratorException("Ideal generator is reducible",
                    gcdExtResult.getGcd(), gcdExtResult.getRightDivGcd());
        }
        return coset(gcdExtResult.getX());
    }

    public final Coset coset(E offset) {
        return new Coset(baseRing.mod(offset, idealGenerator));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof QuotientField && (o.getClass().isAssignableFrom(getClass()) ?
                baseRing.equals(((QuotientField<?>) o).baseRing) &&
                        Objects.equals(idealGenerator, ((QuotientField<?>) o).idealGenerator) : o.equals(shallowCopy()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseRing, idealGenerator);
    }

    public class Coset implements FieldElement<Coset> {

        private final E witness;

        private Coset(E witness) {
            this.witness = witness;
        }

        public E getWitness() {
            return witness;
        }

        @Override
        public Coset cast() {
            return this;
        }

        @Override
        public QuotientField<E> getField() {
            return QuotientField.this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof QuotientField.Coset &&
                    getRing().equals(((QuotientField<?>.Coset) obj).getRing()) &&
                    Objects.equals(witness, ((QuotientField<?>.Coset) obj).witness);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRing(), witness);
        }

        @Override
        public String toString() {
            return witness + " (mod " + idealGenerator + ")";
        }
    }
}
