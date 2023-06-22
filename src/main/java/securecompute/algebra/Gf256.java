package securecompute.algebra;

import com.google.common.base.Strings;
import securecompute.ShallowCopyable;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

public class Gf256 extends ShallowCopyable implements FiniteField<Gf256.Element> {
    private static final BigInteger SIZE = BigInteger.valueOf(256);

    private final int irreduciblePolynomial, generator;
    private final byte[] logTable;
    private final byte[] expTable;
    private final Element[] elements = new Element[256];

    public Gf256(int primitivePolynomial) {
        this(BooleanField.fromBinary(primitivePolynomial));
    }

    public Gf256(int irreduciblePolynomial, int generator) {
        this(BooleanField.fromBinary(irreduciblePolynomial), BooleanField.fromBinary(generator));
    }

    public Gf256(Polynomial<Boolean> primitivePolynomial) {
        this(primitivePolynomial, primitivePolynomial.getRing().one().shift(1));
    }

    public Gf256(Polynomial<Boolean> irreduciblePolynomial, Polynomial<Boolean> generator) {
        this.irreduciblePolynomial = (int) BooleanField.toBinary(irreduciblePolynomial);
        this.generator = (int) BooleanField.toBinary(generator);
        this.logTable = new byte[256];
        this.expTable = new byte[510];

        FieldPolynomialRing<Boolean> polynomialRing = new FieldPolynomialRing<>(BooleanField.INSTANCE);
        QuotientField<Polynomial<Boolean>> quotientField = new QuotientField<>(polynomialRing, irreduciblePolynomial);

        QuotientField<Polynomial<Boolean>>.Coset primitiveElement = quotientField.coset(generator);
        QuotientField<Polynomial<Boolean>>.Coset elt = quotientField.one();

        checkArgument(primitiveElement.pow(255).equals(elt), "Not an irreducible polynomial: %s",
                irreduciblePolynomial);

        boolean backToBeginning = false;
        for (int i = 0; i < 255; i++) {
            checkArgument(!backToBeginning || primitiveElement.getWitness().equals(polynomialRing.one().shift(1)),
                    "Not a primitive element: %s", primitiveElement);
            checkArgument(!backToBeginning,
                    "Not a primitive polynomial: %s", irreduciblePolynomial);
            long value = BooleanField.toBinary(elt.getWitness());
            expTable[i] = (byte) value;
            expTable[i + 255] = (byte) value;
            logTable[(int) value] = (byte) i;

            elt = elt.multiply(primitiveElement);
            backToBeginning = elt.equals(quotientField.one());
        }

        Arrays.setAll(elements, n -> new Element((byte) n));
    }

    private Gf256(int irreduciblePolynomial, int generator, byte[] logTable, byte[] expTable) {
        this.irreduciblePolynomial = irreduciblePolynomial;
        this.generator = generator;
        this.logTable = logTable;
        this.expTable = expTable;
        Arrays.setAll(elements, n -> new Element((byte) n));
    }

    @Override
    protected Gf256 shallowCopy() {
        return new Gf256(irreduciblePolynomial, generator, logTable, expTable);
    }

    @Override
    public BigInteger size() {
        return SIZE;
    }

    @Override
    public Element getPrimitiveElement() {
        return exp(1);
    }

    @Override
    public Element exp(int n) {
        return element(n >= 0 && n < 510 ? expTable[n] : expTable[Math.floorMod(n, 255)]);
    }

    @Override
    public int log(Element elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        if (elt.value == 0) {
            throw new ArithmeticException("Logarithm of zero");
        }
        return (int) logTable[(int) elt.value & 0xff] & 0xff;
    }

    @Override
    public Element sampleUniformly(Random random) {
        return element(random.nextInt(256));
    }

    @Override
    public Element reciprocalOrZero(Element elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        if (elt.value == 0) {
            return zero();
        }
        return exp(255 - log(elt));
    }

    @Override
    public Element fromBigInteger(BigInteger n) {
        return element(n.intValue() & 1);
    }

    @Override
    public Element sum(Element left, Element right) {
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(right.getField()), "RHS field mismatch");
        return element(left.value ^ right.value);
    }

    @Override
    public Element product(Element left, Element right) {
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(right.getField()), "RHS field mismatch");
        if (left.value == 0 || right.value == 0) {
            return zero();
        }
        return exp(log(left) + log(right));
    }

    @Override
    public Element negative(Element elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        return elt;
    }

    public Element element(long value) {
        return elements[(int) value & 0xff];
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Gf256 && (o.getClass().isAssignableFrom(getClass()) ?
                irreduciblePolynomial == ((Gf256) o).irreduciblePolynomial &&
                        generator == ((Gf256) o).generator : o.equals(this));
    }

    @Override
    public int hashCode() {
        return 31 * irreduciblePolynomial + generator;
    }

    public final class Element implements FieldElement<Element> {

        private final byte value;

        private Element(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        @Override
        public Element cast() {
            return this;
        }

        @Override
        public Gf256 getField() {
            return Gf256.this;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Element && getField().equals(((Element) obj).getField()) &&
                    value == ((Element) obj).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getField(), value);
        }

        @Override
        public String toString() {
            return "0b" + Strings.padStart(Integer.toBinaryString(value & 0xFF), 8, '0');
        }
    }
}
