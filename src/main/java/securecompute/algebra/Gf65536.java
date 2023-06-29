package securecompute.algebra;

import securecompute.ShallowCopyable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

public class Gf65536 extends ShallowCopyable implements FiniteField<Gf65536.Element> {
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
        checkArgument(baseField.equals(b.getField()), "Coefficients come from different fields");
        this.a = a;
        this.b = b;
        int order = order(primitiveElement);
        checkArgument(order > 0, "Invalid quadratic extension: generator is reducible (a == 0 or b == 0)");
        checkArgument(order > 255, "Generator is reducible: got order %s", order);
        checkArgument(order == 65535, "Generator is not primitive: got order %s", order);
    }

    private int order(Element elt) {
        return one.equals(elt.pow(65535)) ? IntStream.of(3, 5, 17, 257)
                .map(n -> 65535 / n)
                .filter(n -> one.equals(elt.pow(n)))
                .reduce(65535, IntegerRing.INSTANCE::gcd) : 0;
    }

    @Override
    protected Gf65536 shallowCopy() {
        return new Gf65536(a, b);
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
        checkArgument(equals(elt.getField()), "Field mismatch");
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
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(right.getField()), "RHS field mismatch");

        Gf256.Element x1 = left.getLsb(), x2 = left.getMsb();
        Gf256.Element y1 = right.getLsb(), y2 = right.getMsb();

        return element(x1.add(y1), x2.add(y2));
    }

    @Override
    public Element product(Element left, Element right) {
        checkArgument(equals(left.getField()), "LHS field mismatch");
        checkArgument(equals(right.getField()), "RHS field mismatch");

        Gf256.Element x1 = left.getLsb(), x2 = left.getMsb();
        Gf256.Element y1 = right.getLsb(), y2 = right.getMsb();
        Gf256.Element z1 = x1.multiply(y1), z2 = x1.multiply(y2).add(x2.multiply(y1)), z3 = x2.multiply(y2);

        return element(z1.subtract(z3.multiply(a)), z2.subtract(z3.multiply(b)));
    }

    @Override
    public Element negative(Element elt) {
        checkArgument(equals(elt.getField()), "Field mismatch");
        return elt;
    }

    public Element element(Gf256.Element lsb, Gf256.Element msb) {
        checkArgument(baseField.equals(lsb.getField()), "LSB coefficients come from the wrong field");
        checkArgument(baseField.equals(msb.getField()), "MSB coefficients come from the wrong field");
        return new Element(lsb.getValue(), msb.getValue());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Gf65536 && (o.getClass().isAssignableFrom(getClass()) ?
                a.equals(((Gf65536) o).a) && b.equals(((Gf65536) o).b) : o.equals(shallowCopy()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
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
