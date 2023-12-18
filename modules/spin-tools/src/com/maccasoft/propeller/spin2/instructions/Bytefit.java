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
import com.maccasoft.propeller.expressions.Context;
import com.maccasoft.propeller.expressions.Type;
import com.maccasoft.propeller.spin2.Spin2InstructionObject;
import com.maccasoft.propeller.spin2.Spin2PAsmExpression;

public class Bytefit extends Byte {

    @Override
    public Spin2InstructionObject createObject(Context context, String condition, List<Spin2PAsmExpression> arguments, String effect) {
        return new Bytefit_(context, arguments);
    }

    public class Bytefit_ extends Spin2InstructionObject {

        List<Spin2PAsmExpression> arguments;

        public Bytefit_(Context context, List<Spin2PAsmExpression> arguments) {
            super(context);
            this.arguments = arguments;
        }

        @Override
        public int getSize() {
            try {
                return getBytes().length;
            } catch (Exception e) {
                // Do nothing
            }
            return 0;
        }

        @Override
        public byte[] getBytes() {
            CompilerException msgs = new CompilerException();
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            for (Spin2PAsmExpression exp : arguments) {
                try {
                    if (exp.getExpression() instanceof Type) {
                        switch (((Type) exp.getExpression()).getType().toUpperCase()) {
                            case "WORD":
                                if (exp.getInteger() < -0x8000 || exp.getInteger() > 0xFFFF) {
                                    throw new CompilerException("Word value must range from -$8000 to $FFFF", exp.getExpression().getData());
                                }
                                break;
                            case "LONG":
                                break;
                            default:
                                if (exp.getInteger() < -0x80 || exp.getInteger() > 0xFF) {
                                    throw new CompilerException("Byte value must range from -$80 to $FF", exp.getExpression().getData());
                                }
                                break;
                        }
                    }
                    else if (exp.getInteger() < -0x80 || exp.getInteger() > 0xFF) {
                        throw new CompilerException("Byte value must range from -$80 to $FF", exp.getExpression().getData());
                    }
                    byte[] value = exp.getByte();
                    for (int i = 0; i < exp.getCount(); i++) {
                        os.write(value);
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
