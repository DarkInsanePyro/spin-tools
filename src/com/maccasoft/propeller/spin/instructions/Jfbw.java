/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin.instructions;

import java.util.List;

import com.maccasoft.propeller.spin.Spin2Context;
import com.maccasoft.propeller.spin.Spin2InstructionObject;
import com.maccasoft.propeller.spin.Spin2PAsmExpression;
import com.maccasoft.propeller.spin.Spin2PAsmInstructionFactory;
import com.maccasoft.propeller.spin.Spin2PAsmSchema;

public class Jfbw extends Spin2PAsmInstructionFactory {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        if (Spin2PAsmSchema.S.check(arguments, effect)) {
            return new Jfbw_(context, condition, arguments.get(0));
        }
        throw new RuntimeException("Invalid arguments");
    }

    /*
     * JFBW    {#}S
     */
    public class Jfbw_ extends Spin2InstructionObject {

        String condition;
        Spin2PAsmExpression src;

        public Jfbw_(Spin2Context context, String condition, Spin2PAsmExpression src) {
            super(context);
            this.condition = condition;
            this.src = src;
        }

        @Override
        public int getSize() {
            return src.isLongLiteral() ? 8 : 4;
        }

        // EEEE 1011110 01I 000001001 SSSSSSSSS

        @Override
        public byte[] getBytes() {
            int value = e.setValue(0, condition == null ? 0b1111 : conditions.get(condition));
            value = o.setValue(value, 0b1011110);
            value = cz.setValue(value, 0b01);
            value = i.setBoolean(value, src.isLiteral());
            value = d.setValue(value, 0b000001001);

            int offset = src.getInteger();
            if (src.isLiteral()) {
                offset -= context.getInteger("$");
                if (src.getInteger() >= 0x400) {
                    offset /= 4;
                }
                offset--;
            }
            value = s.setValue(value, offset);

            return src.isLongLiteral() ? getBytes(encodeAugs(condition, offset), value) : getBytes(value);
        }

    }
}
