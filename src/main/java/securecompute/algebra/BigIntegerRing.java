package securecompute.algebra;

import java.math.BigInteger;

public enum BigIntegerRing implements EuclideanDomain<BigInteger> {
    INSTANCE;

    @Override
    public BigInteger fromBigInteger(BigInteger n) {
        return n;
    }

    @Override
    public BigInteger sum(BigInteger left, BigInteger right) {
        return left.add(right);
    }

    @Override
    public BigInteger product(BigInteger left, BigInteger right) {
        return left.multiply(right);
    }

    @Override
    public BigInteger negative(BigInteger elt) {
        return elt.negate();
    }

    @Override
    public int size(BigInteger elt) {
        // TODO: Is this right?
        return elt.abs().bitLength();
    }

    @Override
    public BigInteger abs(BigInteger elt) {
        return elt.abs();
    }

    @Override
    public BigInteger signum(BigInteger elt) {
        return BigInteger.valueOf(elt.signum());
    }

    @Override
    public BigInteger invSignum(BigInteger elt) {
        return BigInteger.valueOf(elt.signum());
    }

    @Override
    public BigInteger div(BigInteger dividend, BigInteger divisor) {
        return dividend.signum() * divisor.signum() < 0
                ? dividend.add(signum(divisor)).divide(divisor).subtract(BigInteger.ONE)
                : dividend.divide(divisor);
    }

    @Override
    public BigInteger mod(BigInteger dividend, BigInteger divisor) {
        BigInteger result = dividend.remainder(divisor);
        return result.signum() * divisor.signum() < 0 ? result.add(divisor) : result;
    }

    @Override
    public DivModResult<BigInteger> divMod(BigInteger dividend, BigInteger divisor) {
        BigInteger[] result = dividend.divideAndRemainder(divisor);
        return result[1].signum() * divisor.signum() < 0
                ? new DivModResult<>(result[0].subtract(BigInteger.ONE), result[1].add(divisor))
                : new DivModResult<>(result[0], result[1]);
    }

    @Override
    public BigInteger gcd(BigInteger left, BigInteger right) {
        return left.gcd(right);
    }

    @Override
    public GcdExtResult<BigInteger> gcdExt(BigInteger left, BigInteger right) {
        BigInteger gcd = left.gcd(right);
        if (left.equals(zero()) || right.equals(zero())) {
            return new GcdExtResult<>(signum(left), signum(right), gcd, signum(left), signum(right));
        }
        BigInteger leftDivGcd = left.divide(gcd), absRightDivGcd = right.abs().divide(gcd);
        if (absRightDivGcd.equals(one())) {
            return new GcdExtResult<>(zero(), signum(right), gcd, leftDivGcd, signum(right));
        }
        BigInteger x = leftDivGcd.modInverse(absRightDivGcd);
        BigInteger y = one().subtract(x.multiply(leftDivGcd)).divide(absRightDivGcd);
        return right.signum() < 0
                ? new GcdExtResult<>(x.subtract(absRightDivGcd), y.add(leftDivGcd).negate(), gcd, leftDivGcd, absRightDivGcd.negate())
                : new GcdExtResult<>(x, y, gcd, leftDivGcd, absRightDivGcd);
    }
}
