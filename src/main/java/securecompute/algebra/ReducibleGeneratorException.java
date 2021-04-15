package securecompute.algebra;

public class ReducibleGeneratorException extends ArithmeticException {
    private final Object firstFactor, secondFactor;

    ReducibleGeneratorException(String s, Object firstFactor, Object secondFactor) {
        super(s);
        this.firstFactor = firstFactor;
        this.secondFactor = secondFactor;
    }

    public Object getFirstFactor() {
        return firstFactor;
    }

    public Object getSecondFactor() {
        return secondFactor;
    }
}
