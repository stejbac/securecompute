package securecompute.algebra.polynomial;

import securecompute.algebra.Field;

public class FieldPolynomialRing<E> extends PolynomialRing<E> implements IFieldPolynomialRing<E> {

    public FieldPolynomialRing(Field<E> baseField) {
        super(baseField);
    }

    @Override
    public Field<E> getBaseRing() {
        return (Field<E>) super.getBaseRing();
    }
}
