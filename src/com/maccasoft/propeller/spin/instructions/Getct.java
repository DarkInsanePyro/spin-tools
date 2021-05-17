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

public class Getct extends Spin2PAsmInstructionFactory {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, List<Spin2PAsmExpression> arguments, String effect) {
        if (arguments.size() == 1) {
            return new Getct_(context, arguments.get(0), effect);
        }
        throw new RuntimeException("Invalid arguments");
    }

    public static class Getct_ extends Spin2InstructionObject {

        Spin2PAsmExpression argument;
        String effect;

        public Getct_(Spin2Context context, Spin2PAsmExpression argument, String effect) {
            super(context);
            this.argument = argument;
            this.effect = effect;
        }

        // EEEE 1101011 C00 DDDDDDDDD 000011010

        @Override
        public byte[] getBytes() {
            return getBytes(encode(0b1101011, "wc".equals(effect), false, false, argument.getInteger(), 0b000011010));
        }

    }
}
