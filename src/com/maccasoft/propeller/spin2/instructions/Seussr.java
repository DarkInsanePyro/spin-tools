/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin2.instructions;

import java.util.List;

import com.maccasoft.propeller.CompilerMessage;
import com.maccasoft.propeller.spin2.Spin2Context;
import com.maccasoft.propeller.spin2.Spin2InstructionObject;
import com.maccasoft.propeller.spin2.Spin2PAsmExpression;
import com.maccasoft.propeller.spin2.Spin2PAsmInstructionFactory;
import com.maccasoft.propeller.spin2.Spin2PAsmSchema;

public class Seussr extends Spin2PAsmInstructionFactory {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        if (Spin2PAsmSchema.D.check(arguments, effect)) {
            return new Seussr_(context, condition, arguments.get(0));
        }
        throw new RuntimeException("Invalid arguments");
    }

    /*
     * SEUSSR  D
     */
    public class Seussr_ extends Spin2InstructionObject {

        String condition;
        Spin2PAsmExpression dst;

        public Seussr_(Spin2Context context, String condition, Spin2PAsmExpression dst) {
            super(context);
            this.condition = condition;
            this.dst = dst;
        }

        // EEEE 1101011 000 DDDDDDDDD 001100101

        @Override
        public byte[] getBytes() {
            int value = e.setValue(0, condition == null ? 0b1111 : conditions.get(condition));
            value = o.setValue(value, 0b1101011);
            value = czi.setValue(value, 0b000);
            if (dst.getInteger() > 0x1FF) {
                throw new CompilerMessage("Destination register cannot exceed $1FF", dst.getExpression().getData());
            }
            value = d.setValue(value, dst.getInteger());
            value = s.setValue(value, 0b001100101);
            return getBytes(value);
        }

    }
}
