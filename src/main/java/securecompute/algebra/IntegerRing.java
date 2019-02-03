package securecompute.algebra;

public enum IntegerRing implements EuclideanDomain<Integer> {

    INSTANCE;

    @Override
    public Integer fromInt(int n) {
        return n;
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
    public Integer div(Integer dividend, Integer divisor) {
        return Math.floorDiv(dividend, divisor);
    }

    @Override
    public Integer mod(Integer dividend, Integer divisor) {
        return Math.floorMod(dividend, divisor);
    }
}
