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

public class Augd extends Spin2PAsmInstructionFactory {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        if (arguments.size() == 1 && "#".equals(arguments.get(0).getPrefix()) && effect == null) {
            return new Augd_(context, condition, arguments.get(0));
        }
        throw new RuntimeException("Invalid arguments");
    }

    /*
     * AUGD    #n
     */
    public static class Augd_ extends Spin2InstructionObject {

        String condition;
        Spin2PAsmExpression argument;

        public Augd_(Spin2Context context, String condition, Spin2PAsmExpression argument) {
            super(context);
            this.condition = condition;
            this.argument = argument;
        }

        // EEEE 11111nn nnn nnnnnnnnn nnnnnnnnn

        @Override
        public byte[] getBytes() {
            return getBytes(encodeAugd(condition, argument.getInteger()));
        }

    }
}
