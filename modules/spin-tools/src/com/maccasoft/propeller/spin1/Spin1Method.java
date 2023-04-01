/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin1;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BitField;

import com.maccasoft.propeller.expressions.Expression;
import com.maccasoft.propeller.expressions.LocalVariable;
import com.maccasoft.propeller.expressions.NumberLiteral;

public class Spin1Method {

    public static final BitField address = new BitField(0b0000000000000000_1111111111111111);
    public static final BitField locals = new BitField(0b1111111111111111_0000000000000000);

    Spin1Context scope;

    String label;
    List<LocalVariable> parameters;
    List<LocalVariable> returns;
    List<LocalVariable> localVariables;

    List<Spin1MethodLine> lines = new ArrayList<>();

    String comment;
    Object data;

    int startAddress;
    int endAddress;
    boolean addressChanged;

    List<Spin1Method> calledBy = new ArrayList<>();
    List<Spin1Method> calls = new ArrayList<>();

    public Spin1Method(Spin1Context scope, String label) {
        this.scope = scope;
        this.label = label;
        this.parameters = new ArrayList<>();
        this.returns = new ArrayList<>();
        this.localVariables = new ArrayList<>();

        LocalVariable defaultReturn = new LocalVariable("LONG", "RESULT", new NumberLiteral(1), 0);
        scope.addBuiltinSymbol(defaultReturn.getName(), defaultReturn);
        this.returns.add(defaultReturn);
    }

    public Spin1Context getScope() {
        return scope;
    }

    public String getLabel() {
        return label;
    }

    public LocalVariable addParameter(String name, Expression size) {
        LocalVariable var = new LocalVariable("LONG", name, size, 0) {

            @Override
            public int getOffset() {
                return 4 + parameters.indexOf(this) * 4;
            }

        };
        scope.addSymbol(name, var);
        parameters.add(var);
        return var;
    }

    public LocalVariable addReturnVariable(String name) {
        LocalVariable var = new LocalVariable("LONG", name, new NumberLiteral(1), 0);
        scope.addSymbol(name, var);
        return var;
    }

    public LocalVariable addLocalVariable(String type, String name, Expression size) {
        LocalVariable var = new LocalVariable(type, name, size, 0) {

            @Override
            public int getOffset() {
                int offset = 4 + parameters.size() * 4;

                for (LocalVariable var : localVariables) {
                    if (var == this) {
                        break;
                    }
                    int count = 4;
                    int varSize = var.getSize() != null ? var.getSize().getNumber().intValue() : 1;
                    if ("WORD".equalsIgnoreCase(var.getType())) {
                        count = 2;
                    }
                    else if ("BYTE".equalsIgnoreCase(var.getType())) {
                        count = 1;
                    }
                    offset += ((count * varSize + 3) / 4) * 4;
                }

                return offset;
            }

        };
        scope.addSymbol(name, var);
        localVariables.add(var);
        return var;
    }

    public void register() {
        for (Spin1MethodLine line : lines) {
            line.register(scope);
        }
    }

    public int resolve(int address) {
        addressChanged = startAddress != address;
        startAddress = address;

        scope.setAddress(address);
        for (Spin1MethodLine line : lines) {
            address = line.resolve(address);
            addressChanged |= line.isAddressChanged();
        }

        addressChanged |= endAddress != address;
        endAddress = address;

        return address;
    }

    public boolean isAddressChanged() {
        return addressChanged;
    }

    public List<LocalVariable> getReturns() {
        return returns;
    }

    public int getReturnsCount() {
        return returns.size();
    }

    public List<LocalVariable> getParameters() {
        return parameters;
    }

    public int getParametersCount() {
        return parameters.size();
    }

    public List<LocalVariable> getLocalVariables() {
        return localVariables;
    }

    public int getLocalsSize() {
        int count = 0;

        for (LocalVariable var : localVariables) {
            int size = 4;
            if ("WORD".equalsIgnoreCase(var.getType())) {
                size = 2;
            }
            else if ("BYTE".equalsIgnoreCase(var.getType())) {
                size = 1;
            }
            if (var.getSize() != null) {
                size = size * var.getSize().getNumber().intValue();
            }
            count += size;
        }

        return count;
    }

    public int getStackSize() {
        return parameters.size() * 4 + getLocalsSize();
    }

    public void addSource(Spin1MethodLine line) {
        lines.add(line);
    }

    public List<Spin1MethodLine> getLines() {
        return lines;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void writeTo(Spin1Object obj) {
        if (comment != null) {
            obj.writeComment(comment);
        }

        for (Spin1MethodLine line : lines) {
            line.writeTo(obj);
        }
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setCalledBy(Spin1Method method) {
        if (method == this) {
            return;
        }
        if (!calledBy.contains(method)) {
            calledBy.add(method);
        }
        if (!method.calls.contains(this)) {
            method.calls.add(this);
        }
    }

    void removeCalledBy(Spin1Method method) {
        calledBy.remove(method);
        if (calledBy.size() == 0) {
            for (Spin1Method ref : calls) {
                ref.removeCalledBy(this);
            }
        }
    }

    public void remove() {
        for (Spin1Method ref : calls) {
            ref.removeCalledBy(this);
        }
    }

    public boolean isReferenced() {
        return calledBy.size() != 0;
    }

}
