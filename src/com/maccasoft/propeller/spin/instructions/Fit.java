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

public class Fit extends Spin2PAsmInstructionFactory {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        if (arguments.size() == 0) {
            return new Fit_(context);
        }
        if (arguments.size() == 1) {
            return new Fit_(context, arguments.get(0));
        }
        throw new RuntimeException("Invalid arguments");
    }

    public class Fit_ extends Spin2InstructionObject {

        Spin2PAsmExpression arg0;

        public Fit_(Spin2Context context) {
            super(context);
        }

        public Fit_(Spin2Context context, Spin2PAsmExpression arg0) {
            super(context);
            this.arg0 = arg0;
        }

        @Override
        public int resolve(int address) {
            return address;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

    }
}
