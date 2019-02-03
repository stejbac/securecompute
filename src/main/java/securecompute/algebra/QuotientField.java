package securecompute.algebra;

import java.util.Objects;

public class QuotientField<E> implements Field<QuotientField<E>.Coset> {

    private final EuclideanDomain<E> baseRing;
    private final E idealGenerator;

    public QuotientField(EuclideanDomain<E> baseRing, E idealGenerator) {
        this.baseRing = baseRing;
        this.idealGenerator = idealGenerator;
    }

    @Override
    public Coset fromInt(int n) {
        return coset(baseRing.fromInt(n));
    }

    @Override
    public Coset sum(Coset left, Coset right) {
        return coset(baseRing.sum(left.witness, right.witness));
    }

    @Override
    public Coset product(Coset left, Coset right) {
        return coset(baseRing.product(left.witness, right.witness));
    }

    @Override
    public Coset negative(Coset elt) {
        return coset(baseRing.negative(elt.witness));
    }

    @Override
    public Coset reciprocalOrZero(Coset elt) {
        EuclideanDomain.GcdExtResult<E> gcdExtResult = baseRing.gcdExt(elt.witness, idealGenerator);
        // TODO: Add error handling for the case that 'idealGenerator' turns out to be reducible.
        return coset(gcdExtResult.getGcd().equals(baseRing.one())
                ? gcdExtResult.getX()
                : baseRing.zero());
    }

    public final Coset coset(E offset) {
        return new Coset(baseRing.mod(offset, idealGenerator));
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
        public Field<Coset> getField() {
            return QuotientField.this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof QuotientField.Coset &&
                    getRing().equals(((QuotientField<?>.Coset) obj).getRing()) &&
                    witness.equals(((QuotientField<?>.Coset) obj).witness);
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
