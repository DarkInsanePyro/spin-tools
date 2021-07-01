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

import com.maccasoft.propeller.spin1.Spin1BytecodeInstruction;
import com.maccasoft.propeller.spin1.Spin1Context;

public class Bytecode extends Spin1BytecodeInstruction {

    public int value;
    public String text;

    public Bytecode(Spin1Context context, int value, String text) {
        super(context);
        this.value = value;
        this.text = text;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public byte[] getBytes() {
        return new byte[] {
            (byte) value
        };
    }

    @Override
    public String toString() {
        return text;
    }

}
