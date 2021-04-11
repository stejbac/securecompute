package securecompute.algebra;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;

public class Gf65536 implements FiniteField<Gf65536.Element> {
    private static final BigInteger SIZE = BigInteger.valueOf(65536);

    private final Gf256 baseField;
    private final Gf256.Element a, b;

    private final Gf65536.Element zero = new Element((byte) 0, (byte) 0);
    private final Gf65536.Element one = new Element((byte) 1, (byte) 0);
    private final Gf65536.Element primitiveElement = new Element((byte) 0, (byte) 1);

    public Gf65536(Gf256 baseField, long aValue, long bValue) {
        this(baseField.element(aValue), baseField.element(bValue));
    }

    public Gf65536(Gf256.Element a, Gf256.Element b) {
        baseField = a.getField();
        if (!baseField.equals(b.getField())) {
            throw new IllegalArgumentException("Coefficients come from different fields");
        }
        this.a = a;
        this.b = b;
    }

    @Override
    public BigInteger size() {
        return SIZE;
    }

    @Override
    public Element getPrimitiveElement() {
        return primitiveElement;
    }

    @Override
    public Element sampleUniformly(Random random) {
        int value = random.nextInt(65536);
        return element(baseField.element(value & 255), baseField.element(value >> 8));
    }

    @Override
    public Element reciprocalOrZero(Element elt) {
        Gf256.Element x1 = elt.getLsb(), x2 = elt.getMsb(), y = x1.subtract(x2.multiply(b));
        Gf256.Element invNorm = x1.multiply(y).add(x2.multiply(x2).multiply(a)).recipOrZero();
        return element(y.multiply(invNorm), x2.negate().multiply(invNorm));
    }

    @Override
    public Element fromBigInteger(BigInteger n) {
        return element(baseField.fromBigInteger(n), baseField.zero());
    }

    @Override
    public Element zero() {
        return zero;
    }

    @Override
    public Element one() {
        return one;
    }

    @Override
    public Element sum(Element left, Element right) {
        Gf256.Element x1 = left.getLsb(), x2 = left.getMsb();
        Gf256.Element y1 = right.getLsb(), y2 = right.getMsb();

        return element(x1.add(y1), x2.add(y2));
    }

    @Override
    public Element product(Element left, Element right) {
        Gf256.Element x1 = left.getLsb(), x2 = left.getMsb();
        Gf256.Element y1 = right.getLsb(), y2 = right.getMsb();
        Gf256.Element z1 = x1.multiply(y1), z2 = x1.multiply(y2).add(x2.multiply(y1)), z3 = x2.multiply(y2);

        return element(z1.subtract(z3.multiply(a)), z2.subtract(z3.multiply(b)));
    }

    @Override
    public Element negative(Element elt) {
        return elt;
    }

    public Element element(Gf256.Element lsb, Gf256.Element msb) {
        if (!baseField.equals(lsb.getField()) || !baseField.equals(msb.getField())) {
            throw new IllegalArgumentException("Coefficients come from the wrong field(s)");
        }
        return new Element(lsb.getValue(), msb.getValue());
    }

    public final class Element implements FieldElement<Element> {

        private final byte lsb, msb;

        private Element(byte lsb, byte msb) {
            this.lsb = lsb;
            this.msb = msb;
        }

        public Gf256.Element getLsb() {
            return baseField.element(lsb);
        }

        public Gf256.Element getMsb() {
            return baseField.element(msb);
        }

        @Override
        public Element cast() {
            return this;
        }

        @Override
        public Field<Element> getField() {
            return Gf65536.this;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Element && getField().equals(((Element) obj).getField()) &&
                    lsb == ((Element) obj).lsb && msb == ((Element) obj).msb;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getField(), lsb, msb);
        }

        @Override
        public String toString() {
            return "(" + getLsb() + ", " + getMsb() + ")";
        }
    }
}
