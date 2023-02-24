/*
 * Copyright (c) 2021-23 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin2.instructions;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.maccasoft.propeller.CompilerException;
import com.maccasoft.propeller.spin2.Spin2Context;
import com.maccasoft.propeller.spin2.Spin2InstructionObject;
import com.maccasoft.propeller.spin2.Spin2PAsmExpression;

public class Wordfit extends Word {

    @Override
    public Spin2InstructionObject createObject(Spin2Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        return new Wordfit_(context, arguments);
    }

    public class Wordfit_ extends Spin2InstructionObject {

        List<Spin2PAsmExpression> arguments;

        public Wordfit_(Spin2Context context, List<Spin2PAsmExpression> arguments) {
            super(context);
            this.arguments = arguments;
        }

        @Override
        public int getSize() {
            int size = 0;
            for (Spin2PAsmExpression exp : arguments) {
                size += 2 * exp.getCount();
            }
            return size;
        }

        @Override
        public byte[] getBytes() {
            CompilerException msgs = new CompilerException();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            for (Spin2PAsmExpression exp : arguments) {
                try {
                    if (exp.getExpression().isString()) {
                        byte[] b = exp.getExpression().getString().getBytes();
                        for (int i = 0; i < b.length; i++) {
                            os.write(b[i]);
                            os.write(0);
                        }
                    }
                    else {
                        if (exp.getInteger() < -0x8000 || exp.getInteger() > 0xFFFF) {
                            throw new CompilerException("Word value must range from -$8000 to $FFFF", exp.getExpression().getData());
                        }
                        byte[] value = exp.getWord();
                        for (int i = 0; i < exp.getCount(); i++) {
                            os.write(value);
                        }
                    }
                } catch (CompilerException e) {
                    msgs.addMessage(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            if (msgs.hasChilds()) {
                throw msgs;
            }

            return os.toByteArray();
        }

    }

}