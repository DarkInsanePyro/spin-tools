package com.maccasoft.propeller.expressions;

public class Xor extends BinaryOperator {

    public Xor(Expression term1, Expression term2) {
        super(term1, term2);
    }

    @Override
    public Number getNumber() {
        return term1.getNumber().longValue() ^ term2.getNumber().longValue();
    }

    @Override
    public String getLexeme() {
        return "^";
    }

}
