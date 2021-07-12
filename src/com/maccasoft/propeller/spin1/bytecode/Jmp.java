/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin1.bytecode;

import com.maccasoft.propeller.expressions.ContextLiteral;
import com.maccasoft.propeller.expressions.Expression;
import com.maccasoft.propeller.expressions.Identifier;
import com.maccasoft.propeller.spin1.Spin1Bytecode;
import com.maccasoft.propeller.spin1.Spin1Context;

public class Jmp extends Spin1Bytecode {

    int code;
    public Expression expression;

    public Jmp(Spin1Context context, Expression expression) {
        super(context);
        this.code = 0b00000100;
        this.expression = expression;
    }

    public Jmp(Spin1Context context, int code, Expression expression) {
        super(context);
        this.code = code;
        this.expression = expression;
    }

    @Override
    public int getSize() {
        if (expression instanceof ContextLiteral) {
            if (!((ContextLiteral) expression).getContext().isAddressSet()) {
                return 3;
            }
        }
        else if (expression instanceof Identifier) {
            if (!((Identifier) expression).getContext().isAddressSet()) {
                return 3;
            }
        }
        int address = expression.getNumber().intValue();
        int value = address - context.getAddress() - 2;
        if (Math.abs(value) >= 0x40) {
            value--;
        }
        return Math.abs(value) < 0x40 ? 2 : 3;
    }

    @Override
    public byte[] getBytes() {
        int address = expression.getNumber().intValue();
        int value = address - context.getAddress() - 2;
        if (Math.abs(value) < 0x40) {
            return new byte[] {
                (byte) code,
                (byte) (value & 0x7F)
            };
        }
        else {
            value--;
            return new byte[] {
                (byte) code,
                (byte) ((value & 0x7F) | 0x80),
                (byte) ((value >> 7) & 0x7F)
            };
        }
    }

    public String toString(String prefix) {
        int address = expression.getNumber().intValue();
        int value = address - (context.getAddress() + getSize());
        return prefix + String.format(" $%05X (%d)", address, value);
    }

    @Override
    public String toString() {
        return toString("JMP");
    }

}