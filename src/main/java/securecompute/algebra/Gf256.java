package securecompute.algebra;

import com.google.common.base.Strings;
import securecompute.algebra.polynomial.FieldPolynomialRing;
import securecompute.algebra.polynomial.Polynomial;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class Gf256 implements FiniteField<Gf256.Element> {

    private final byte[] logTable = new byte[256];
    private final byte[] expTable = new byte[510];
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
        FieldPolynomialRing<Boolean> polynomialRing = new FieldPolynomialRing<>(BooleanField.INSTANCE);
        QuotientField<Polynomial<Boolean>> quotientField = new QuotientField<>(polynomialRing, irreduciblePolynomial);

        QuotientField<Polynomial<Boolean>>.Coset primitiveElement = quotientField.coset(generator);
        QuotientField<Polynomial<Boolean>>.Coset elt = quotientField.one();

        if (!primitiveElement.pow(255).equals(elt)) {
            throw new IllegalArgumentException("Not an irreducible polynomial: " + irreduciblePolynomial);
        }

        boolean backToBeginning = false;
        for (int i = 0; i < 255; i++) {
            if (backToBeginning) {
                throw new IllegalArgumentException(primitiveElement.getWitness().equals(polynomialRing.one().shift(1))
                        ? "Not a primitive polynomial: " + irreduciblePolynomial
                        : "Not a primitive element: " + primitiveElement);
            }
            long value = BooleanField.toBinary(elt.getWitness());
            expTable[i] = (byte) value;
            expTable[i + 255] = (byte) value;
            logTable[(int) value] = (byte) i;

            elt = elt.multiply(primitiveElement);
            backToBeginning = elt.equals(quotientField.one());
        }

        Arrays.setAll(elements, n -> new Element((byte) n));
    }

    @Override
    public int size() {
        return 256;
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
        if (elt.value == 0) {
            return zero();
        }
        return exp(255 - log(elt));
    }

    @Override
    public Element fromInt(int n) {
        return element(n & 1);
    }

    @Override
    public Element sum(Element left, Element right) {
        return element(left.value ^ right.value);
    }

    @Override
    public Element product(Element left, Element right) {
        if (left.value == 0 || right.value == 0) {
            return zero();
        }
        return exp(log(left) + log(right));
    }

    @Override
    public Element negative(Element elt) {
        return elt;
    }

    public Element element(long value) {
        return elements[(int) value & 0xff];
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
