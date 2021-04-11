package securecompute.algebra;

import java.math.BigInteger;

public enum IntegerRing implements EuclideanDomain<Integer> {
    INSTANCE;

    @Override
    public Integer fromBigInteger(BigInteger n) {
        return n.intValueExact();
    }

    @Override
    public Integer sum(Integer left, Integer right) {
        return left + right;
    }

    @Override
    public Integer product(Integer left, Integer right) {
        return left * right;
    }

    @Override
    public Integer negative(Integer elt) {
        return -elt;
    }

    @Override
    public int size(Integer elt) {
        return Math.abs(elt);
    }

    @Override
    public Integer abs(Integer elt) {
        return Math.abs(elt);
    }

    @Override
    public Integer invSignum(Integer elt) {
        return elt < 0 ? -1 : elt == 0 ? 0 : 1;
    }

    @Override
    public Integer div(Integer dividend, Integer divisor) {
        return Math.floorDiv(dividend, divisor);
    }

    @Override
    public Integer mod(Integer dividend, Integer divisor) {
        return Math.floorMod(dividend, divisor);
    }
}
