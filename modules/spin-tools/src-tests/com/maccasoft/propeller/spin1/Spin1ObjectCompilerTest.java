/*
 * Copyright (c) 2021-23 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.maccasoft.propeller.CompilerException;
import com.maccasoft.propeller.model.Node;

class Spin1ObjectCompilerTest {

    @Test
    void testEmptyMethod() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' PUB main\n"
            + "00008 00008       32             RETURN\n"
            + "00009 00009       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testParameterVarAssignment() throws Exception {
        String text = ""
            + "PUB main(a)\n"
            + "\n"
            + "    a := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' PUB main(a)\n"
            + "'     a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testLocalVarAssignment() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testGlobalVarAssignment() throws Exception {
        String text = ""
            + "VAR a\n"
            + "\n"
            + "PUB main\n"
            + "\n"
            + "    a := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 4)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' PUB main\n"
            + "'     a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testExpressionAssignment() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := 1 + b * 3\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := 1 + b * 3\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000A 0000A       38 03          CONSTANT (3)\n"
            + "0000C 0000C       F4             MULTIPLY\n"
            + "0000D 0000D       EC             ADD\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testIfConditional() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    if a == 1\n"
            + "        a := 2\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     if a == 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 03          JZ $00010 (3)\n"
            + "'         a := 2\n"
            + "0000D 0000D       38 02          CONSTANT (2)\n"
            + "0000F 0000F       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testIfConditionExpression() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    if (a := b) == 0\n"
            + "        a := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     if (a := b) == 0\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       80             WRITE\n"
            + "0000B 0000B       35             CONSTANT (0)\n"
            + "0000C 0000C       FC             TEST_EQUAL\n"
            + "0000D 0000D       0A 02          JZ $00011 (2)\n"
            + "'         a := 1\n"
            + "0000F 0000F       36             CONSTANT (1)\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeat() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       04 7C          JMP $00008 (-4)\n"
            + "0000C 0000C       32             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatCount() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat 10\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat 10\n"
            + "00008 00008       38 0A          CONSTANT (10)\n"
            + "0000A 0000A       08 04          TJZ $00010 (4)\n"
            + "'         a := 1\n"
            + "0000C 0000C       36             CONSTANT (1)\n"
            + "0000D 0000D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000E 0000E       09 7C          DJNZ $0000C (-4)\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatVarCounter() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat a\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat a\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       08 04          TJZ $0000F (4)\n"
            + "'         a := 1\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       09 7C          DJNZ $0000B (-4)\n"
            + "0000F 0000F       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatQuit() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        if a == 1\n"
            + "            quit\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         if a == 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 02          JZ $0000F (2)\n"
            + "'             quit\n"
            + "0000D 0000D       04 04          JMP $00013 (4)\n"
            + "'         a := 1\n"
            + "0000F 0000F       36             CONSTANT (1)\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       04 75          JMP $00008 (-11)\n"
            + "00013 00013       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatCaseQuit() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        case a\n"
            + "            1: quit\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         case a\n"
            + "00008 00008       38 15          ADDRESS ($0015)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 01          CASE-JMP $0000F (1)\n"
            + "0000E 0000E       0C             CASE_DONE\n"
            + "'             1: quit\n"
            + "0000F 0000F       38 08 14       POP 8\n"
            + "00012 00012       04 05          JMP $00019 (5)\n"
            + "00014 00014       0C             CASE_DONE\n"
            + "'         a := 1\n"
            + "00015 00015       36             CONSTANT (1)\n"
            + "00016 00016       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00017 00017       04 6F          JMP $00008 (-17)\n"
            + "00019 00019       32             RETURN\n"
            + "0001A 0001A       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatNext() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        if a == 1\n"
            + "            next\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         if a == 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 02          JZ $0000F (2)\n"
            + "'             next\n"
            + "0000D 0000D       04 79          JMP $00008 (-7)\n"
            + "'         a := 1\n"
            + "0000F 0000F       36             CONSTANT (1)\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       04 75          JMP $00008 (-11)\n"
            + "00013 00013       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatWhile() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat while a < 1\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat while a < 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       F9             TEST_BELOW\n"
            + "0000B 0000B       0A 04          JZ $00011 (4)\n"
            + "'         a := 1\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       04 77          JMP $00008 (-9)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatUntil() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat until a < 1\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat until a < 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       F9             TEST_BELOW\n"
            + "0000B 0000B       0B 04          JNZ $00011 (4)\n"
            + "'         a := 1\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       04 77          JMP $00008 (-9)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatWhileQuit() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat while a < 1\n"
            + "        if a == 1\n"
            + "            quit\n"
            + "        a := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat while a < 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       F9             TEST_BELOW\n"
            + "0000B 0000B       0A 0B          JZ $00018 (11)\n"
            + "'         if a == 1\n"
            + "0000D 0000D       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000E 0000E       36             CONSTANT (1)\n"
            + "0000F 0000F       FC             TEST_EQUAL\n"
            + "00010 00010       0A 02          JZ $00014 (2)\n"
            + "'             quit\n"
            + "00012 00012       04 04          JMP $00018 (4)\n"
            + "'         a := 1\n"
            + "00014 00014       36             CONSTANT (1)\n"
            + "00015 00015       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00016 00016       04 70          JMP $00008 (-16)\n"
            + "00018 00018       32             RETURN\n"
            + "00019 00019       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatPostWhile() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        a := 1\n"
            + "    while a < 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     while a < 1\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       F9             TEST_BELOW\n"
            + "0000D 0000D       0B 79          JNZ $00008 (-7)\n"
            + "0000F 0000F       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatPostUntil() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        a := 1\n"
            + "    until a < 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     until a < 1\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       F9             TEST_BELOW\n"
            + "0000D 0000D       0A 79          JZ $00008 (-7)\n"
            + "0000F 0000F       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatPostConditionQuit() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        if a == 1\n"
            + "            quit\n"
            + "        a := 1\n"
            + "    while a < 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         if a == 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 02          JZ $0000F (2)\n"
            + "'             quit\n"
            + "0000D 0000D       04 07          JMP $00016 (7)\n"
            + "'         a := 1\n"
            + "0000F 0000F       36             CONSTANT (1)\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     while a < 1\n"
            + "00011 00011       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00012 00012       36             CONSTANT (1)\n"
            + "00013 00013       F9             TEST_BELOW\n"
            + "00014 00014       0B 72          JNZ $00008 (-14)\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatPostConditionNext() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    repeat\n"
            + "        if a == 1\n"
            + "            next\n"
            + "        a := 1\n"
            + "    until a < 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     repeat\n"
            + "'         if a == 1\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 02          JZ $0000F (2)\n"
            + "'             next\n"
            + "0000D 0000D       04 02          JMP $00011 (2)\n"
            + "'         a := 1\n"
            + "0000F 0000F       36             CONSTANT (1)\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     until a < 1\n"
            + "00011 00011       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00012 00012       36             CONSTANT (1)\n"
            + "00013 00013       F9             TEST_BELOW\n"
            + "00014 00014       0A 72          JZ $00008 (-14)\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatRange() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    repeat a from 1 to 10\n"
            + "        b := a + 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     repeat a from 1 to 10\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'         b := a + 1\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       EC             ADD\n"
            + "0000D 0000D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000E 0000E       36             CONSTANT (1)\n"
            + "0000F 0000F       38 0A          CONSTANT (10)\n"
            + "00011 00011       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00012 00012       02 76          REPEAT-JMP $0000A (-10)\n"
            + "00014 00014       32             RETURN\n"
            + "00015 00015       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatRangeVariables() throws Exception {
        String text = ""
            + "VAR\n"
            + "    long a, b\n"
            + "\n"
            + "PUB main | c, d\n"
            + "\n"
            + "    repeat a from b to c\n"
            + "        d := a + 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 8)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | c, d\n"
            + "'     repeat a from b to c\n"
            + "00008 00008       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "'         d := a + 1\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       EC             ADD\n"
            + "0000D 0000D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000E 0000E       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "0000F 0000F       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00010 00010       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00011 00011       02 77          REPEAT-JMP $0000A (-9)\n"
            + "00013 00013       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRepeatRangeStepVariables() throws Exception {
        String text = ""
            + "VAR\n"
            + "    long a, b\n"
            + "\n"
            + "PUB main | c, d\n"
            + "\n"
            + "    repeat a from b to c step 5\n"
            + "        d := a + 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 8)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | c, d\n"
            + "'     repeat a from b to c step 5\n"
            + "00008 00008       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "'         d := a + 1\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       EC             ADD\n"
            + "0000D 0000D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000E 0000E       38 05          CONSTANT (5)\n"
            + "00010 00010       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "00011 00011       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00012 00012       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00013 00013       06 75          REPEAT-JMP $0000A (-11)\n"
            + "00015 00015       32             RETURN\n"
            + "00016 00016       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatRangeStepNext() throws Exception {
        String text = ""
            + "VAR\n"
            + "    long a, b\n"
            + "\n"
            + "PUB main | c, d\n"
            + "\n"
            + "    repeat a from b to c step 5\n"
            + "        if c == 2\n"
            + "            next\n"
            + "        d := a + 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 8)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | c, d\n"
            + "'     repeat a from b to c step 5\n"
            + "00008 00008       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "'         if c == 2\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       38 02          CONSTANT (2)\n"
            + "0000D 0000D       FC             TEST_EQUAL\n"
            + "0000E 0000E       0A 02          JZ $00012 (2)\n"
            + "'             next\n"
            + "00010 00010       04 04          JMP $00016 (4)\n"
            + "'         d := a + 1\n"
            + "00012 00012       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00013 00013       36             CONSTANT (1)\n"
            + "00014 00014       EC             ADD\n"
            + "00015 00015       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "00016 00016       38 05          CONSTANT (5)\n"
            + "00018 00018       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "00019 00019       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0001A 0001A       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "0001B 0001B       06 6D          REPEAT-JMP $0000A (-19)\n"
            + "0001D 0001D       32             RETURN\n"
            + "0001E 0001E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testModifyExpressions() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a += 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     a += 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       4C             ADD\n"
            + "0000B 0000B       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testCase() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    case a\n"
            + "        1: a := 4\n"
            + "        2:\n"
            + "           a := 5\n"
            + "        3: a := 6\n"
            + "        other: a := 7\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       28 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     case a\n"
            + "00008 00008       38 26          ADDRESS ($0026)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 0C          CASE-JMP $0001A (12)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 0C          CASE-JMP $0001E (12)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       0D 0C          CASE-JMP $00022 (12)\n"
            + "'         other: a := 7\n"
            + "00016 00016       38 07          CONSTANT (7)\n"
            + "00018 00018       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00019 00019       0C             CASE_DONE\n"
            + "'         1: a := 4\n"
            + "0001A 0001A       38 04          CONSTANT (4)\n"
            + "0001C 0001C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001D 0001D       0C             CASE_DONE\n"
            + "'            a := 5\n"
            + "0001E 0001E       38 05          CONSTANT (5)\n"
            + "00020 00020       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00021 00021       0C             CASE_DONE\n"
            + "'         3: a := 6\n"
            + "00022 00022       38 06          CONSTANT (6)\n"
            + "00024 00024       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00025 00025       0C             CASE_DONE\n"
            + "00026 00026       32             RETURN\n"
            + "00027 00027       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testCaseRange() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    case a\n"
            + "        1..5: a := 6\n"
            + "        other: a := 7\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     case a\n"
            + "00008 00008       38 18          ADDRESS ($0018)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       38 05          CONSTANT (5)\n"
            + "0000E 0000E       0E 04          CASE-RANGE-JMP $00014 (4)\n"
            + "'         other: a := 7\n"
            + "00010 00010       38 07          CONSTANT (7)\n"
            + "00012 00012       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00013 00013       0C             CASE_DONE\n"
            + "'         1..5: a := 6\n"
            + "00014 00014       38 06          CONSTANT (6)\n"
            + "00016 00016       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00017 00017       0C             CASE_DONE\n"
            + "00018 00018       32             RETURN\n"
            + "00019 00019       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testCaseList() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    case a\n"
            + "        1, 2      : a := 14\n"
            + "        3, 4, 5   : a := 15\n"
            + "        6, 7, 8, 9: a := 16\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       3C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     case a\n"
            + "00008 00008       38 3B          ADDRESS ($003B)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 21          CASE-JMP $0002F (33)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 1D          CASE-JMP $0002F (29)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       0D 1D          CASE-JMP $00033 (29)\n"
            + "00016 00016       38 04          CONSTANT (4)\n"
            + "00018 00018       0D 19          CASE-JMP $00033 (25)\n"
            + "0001A 0001A       38 05          CONSTANT (5)\n"
            + "0001C 0001C       0D 15          CASE-JMP $00033 (21)\n"
            + "0001E 0001E       38 06          CONSTANT (6)\n"
            + "00020 00020       0D 15          CASE-JMP $00037 (21)\n"
            + "00022 00022       38 07          CONSTANT (7)\n"
            + "00024 00024       0D 11          CASE-JMP $00037 (17)\n"
            + "00026 00026       38 08          CONSTANT (8)\n"
            + "00028 00028       0D 0D          CASE-JMP $00037 (13)\n"
            + "0002A 0002A       38 09          CONSTANT (9)\n"
            + "0002C 0002C       0D 09          CASE-JMP $00037 (9)\n"
            + "0002E 0002E       0C             CASE_DONE\n"
            + "'         1, 2      : a := 14\n"
            + "0002F 0002F       38 0E          CONSTANT (14)\n"
            + "00031 00031       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00032 00032       0C             CASE_DONE\n"
            + "'         3, 4, 5   : a := 15\n"
            + "00033 00033       38 0F          CONSTANT (15)\n"
            + "00035 00035       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00036 00036       0C             CASE_DONE\n"
            + "'         6, 7, 8, 9: a := 16\n"
            + "00037 00037       38 10          CONSTANT (16)\n"
            + "00039 00039       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0003A 0003A       0C             CASE_DONE\n"
            + "0003B 0003B       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testTernaryExpression() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := (b == 1) ? 2 : 3\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := (b == 1) ? 2 : 3\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 04          JZ $00011 (4)\n"
            + "0000D 0000D       38 02          CONSTANT (2)\n"
            + "0000F 0000F       04 02          JMP $00013 (2)\n"
            + "00011 00011       38 03          CONSTANT (3)\n"
            + "00013 00013       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00014 00014       32             RETURN\n"
            + "00015 00015       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testMethodCall() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    function1(1, 2, 3)\n"
            + "    \\function2\n"
            + "\n"
            + "PUB function1(a, b, c)\n"
            + "\n"
            + "PUB function2\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       04             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008       1C 00 00 00    Function function1 @ $001C (local size 0)\n"
            + "0000C 0000C       1D 00 00 00    Function function2 @ $001D (local size 0)\n"
            + "' PUB main\n"
            + "'     function1(1, 2, 3)\n"
            + "00010 00010       01             ANCHOR\n"
            + "00011 00011       36             CONSTANT (1)\n"
            + "00012 00012       38 02          CONSTANT (2)\n"
            + "00014 00014       38 03          CONSTANT (3)\n"
            + "00016 00016       05 02          CALL_SUB\n"
            + "'     \\function2\n"
            + "00018 00018       03             ANCHOR (TRY)\n"
            + "00019 00019       05 03          CALL_SUB\n"
            + "0001B 0001B       32             RETURN\n"
            + "' PUB function1(a, b, c)\n"
            + "0001C 0001C       32             RETURN\n"
            + "' PUB function2\n"
            + "0001D 0001D       32             RETURN\n"
            + "0001E 0001E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testMethodReturn() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    return 1\n"
            + "\n"
            + "PUB function1 | a\n"
            + "\n"
            + "    return a\n"
            + "\n"
            + "PUB function2 : b\n"
            + "\n"
            + "    b := 1\n"
            + "    return\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       04             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008       13 00 04 00    Function function1 @ $0013 (local size 4)\n"
            + "0000C 0000C       16 00 00 00    Function function2 @ $0016 (local size 0)\n"
            + "' PUB main\n"
            + "'     return 1\n"
            + "00010 00010       36             CONSTANT (1)\n"
            + "00011 00011       33             RETURN\n"
            + "00012 00012       32             RETURN\n"
            + "' PUB function1 | a\n"
            + "'     return a\n"
            + "00013 00013       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00014 00014       33             RETURN\n"
            + "00015 00015       32             RETURN\n"
            + "' PUB function2 : b\n"
            + "'     b := 1\n"
            + "00016 00016       36             CONSTANT (1)\n"
            + "00017 00017       61             VAR_WRITE LONG DBASE+$0000 (short)\n"
            + "'     return\n"
            + "00018 00018       32             RETURN\n"
            + "00019 00019       32             RETURN\n"
            + "0001A 0001A       00 00          Padding\n"
            + "", compile(text, true));
    }

    @Test
    void testOptimizedMethodReturn() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    return 1\n"
            + "\n"
            + "PUB function1 | a\n"
            + "\n"
            + "    return a\n"
            + "\n"
            + "PUB function2 : b\n"
            + "\n"
            + "    b := 1\n"
            + "    return\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       04             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008       12 00 04 00    Function function1 @ $0012 (local size 4)\n"
            + "0000C 0000C       14 00 00 00    Function function2 @ $0014 (local size 0)\n"
            + "' PUB main\n"
            + "'     return 1\n"
            + "00010 00010       36             CONSTANT (1)\n"
            + "00011 00011       33             RETURN\n"
            + "' PUB function1 | a\n"
            + "'     return a\n"
            + "00012 00012       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00013 00013       33             RETURN\n"
            + "' PUB function2 : b\n"
            + "'     b := 1\n"
            + "00014 00014       36             CONSTANT (1)\n"
            + "00015 00015       61             VAR_WRITE LONG DBASE+$0000 (short)\n"
            + "'     return\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
            + "", compile(text, false));
    }

    @Test
    void testCompilePAsm() throws Exception {
        String text = ""
            + "DAT             org     $000\n"
            + "\n"
            + "start\n"
            + "                cogid   a\n"
            + "                cogstop a\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000                start               \n"
            + "00004 00004   000 01 04 FC 0C                        cogid   a\n"
            + "00008 00008   001 03 04 7C 0C                        cogstop a\n"
            + "0000C 0000C   002                a                   res     1\n"
            + "", compile(text));
    }

    @Test
    void testCompilePAsmReference() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    coginit(cogid, @start, 0)\n"
            + "\n"
            + "DAT             org     $000\n"
            + "\n"
            + "start           cogid   a\n"
            + "                cogstop a\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008   000                                    org     $000\n"
            + "00008 00008   000 01 04 FC 0C    start               cogid   a\n"
            + "0000C 0000C   001 03 04 7C 0C                        cogstop a\n"
            + "00010 00010   002                a                   res     1\n"
            + "' PUB main\n"
            + "'     coginit(cogid, @start, 0)\n"
            + "00010 00010       3F 89          REG_READ $1E9\n"
            + "00012 00012       C7 08          MEM_ADDRESS LONG PBASE+$0008\n"
            + "00014 00014       35             CONSTANT (0)\n"
            + "00015 00015       2C             COGINIT\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testLookdown() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := lookdown(b : 10, 20, 30, 40)\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := lookdown(b : 10, 20, 30, 40)\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       38 19          ADDRESS ($0019)\n"
            + "0000B 0000B       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000C 0000C       38 0A          CONSTANT (10)\n"
            + "0000E 0000E       11             LOOKDOWN\n"
            + "0000F 0000F       38 14          CONSTANT (20)\n"
            + "00011 00011       11             LOOKDOWN\n"
            + "00012 00012       38 1E          CONSTANT (30)\n"
            + "00014 00014       11             LOOKDOWN\n"
            + "00015 00015       38 28          CONSTANT (40)\n"
            + "00017 00017       11             LOOKDOWN\n"
            + "00018 00018       0F             LOOKDONE\n"
            + "00019 00019       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001A 0001A       32             RETURN\n"
            + "0001B 0001B       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testLookupRange() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := lookup(b : 10, 20..30, 40)\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := lookup(b : 10, 20..30, 40)\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       38 18          ADDRESS ($0018)\n"
            + "0000B 0000B       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000C 0000C       38 0A          CONSTANT (10)\n"
            + "0000E 0000E       10             LOOKUP\n"
            + "0000F 0000F       38 14          CONSTANT (20)\n"
            + "00011 00011       38 1E          CONSTANT (30)\n"
            + "00013 00013       12             LOOKUP\n"
            + "00014 00014       38 28          CONSTANT (40)\n"
            + "00016 00016       10             LOOKUP\n"
            + "00017 00017       0F             LOOKDONE\n"
            + "00018 00018       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00019 00019       32             RETURN\n"
            + "0001A 0001A       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testLookdownString() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := lookdown(b : \"abcdefgh\")\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       28 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := lookdown(b : \"abcdefgh\")\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       38 25          ADDRESS ($0025)\n"
            + "0000B 0000B       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000C 0000C       38 61          CONSTANT (\"a\")\n"
            + "0000E 0000E       11             LOOKDOWN\n"
            + "0000F 0000F       38 62          CONSTANT (\"b\")\n"
            + "00011 00011       11             LOOKDOWN\n"
            + "00012 00012       38 63          CONSTANT (\"c\")\n"
            + "00014 00014       11             LOOKDOWN\n"
            + "00015 00015       38 64          CONSTANT (\"d\")\n"
            + "00017 00017       11             LOOKDOWN\n"
            + "00018 00018       38 65          CONSTANT (\"e\")\n"
            + "0001A 0001A       11             LOOKDOWN\n"
            + "0001B 0001B       38 66          CONSTANT (\"f\")\n"
            + "0001D 0001D       11             LOOKDOWN\n"
            + "0001E 0001E       38 67          CONSTANT (\"g\")\n"
            + "00020 00020       11             LOOKDOWN\n"
            + "00021 00021       38 68          CONSTANT (\"h\")\n"
            + "00023 00023       11             LOOKDOWN\n"
            + "00024 00024       0F             LOOKDONE\n"
            + "00025 00025       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00026 00026       32             RETURN\n"
            + "00027 00027       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testPreEffectsOperators() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    ++a\n"
            + "    --a\n"
            + "    ~a\n"
            + "    ~~a\n"
            + "    -a\n"
            + "    ++word[a]\n"
            + "    --byte[a]\n"
            + "    ~long[a]\n"
            + "    ~~word[a]\n"
            + "    ++a[1]\n"
            + "    --a[2]\n"
            + "    ~a[3]\n"
            + "    ~~a[4]\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       34 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     ++a\n"
            + "00008 00008       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00009 00009       26             PRE_INC\n"
            + "'     --a\n"
            + "0000A 0000A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             PRE_DEC\n"
            + "'     ~a\n"
            + "0000C 0000C       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       10             SIGN_EXTEND_BYTE\n"
            + "'     ~~a\n"
            + "0000E 0000E       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       14             SIGN_EXTEND_WORD\n"
            + "'     -a\n"
            + "00010 00010       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00011 00011       46             NEGATE\n"
            + "'     ++word[a]\n"
            + "00012 00012       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00013 00013       A2             MEM_ASSIGN WORD POP\n"
            + "00014 00014       24             PRE_INC\n"
            + "'     --byte[a]\n"
            + "00015 00015       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00016 00016       82             MEM_ASSIGN BYTE POP\n"
            + "00017 00017       32             PRE_DEC\n"
            + "'     ~long[a]\n"
            + "00018 00018       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00019 00019       C2             MEM_ASSIGN LONG POP\n"
            + "0001A 0001A       10             SIGN_EXTEND_BYTE\n"
            + "'     ~~word[a]\n"
            + "0001B 0001B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0001C 0001C       A2             MEM_ASSIGN WORD POP\n"
            + "0001D 0001D       14             SIGN_EXTEND_WORD\n"
            + "'     ++a[1]\n"
            + "0001E 0001E       36             CONSTANT (1)\n"
            + "0001F 0001F       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "00021 00021       26             PRE_INC\n"
            + "'     --a[2]\n"
            + "00022 00022       38 02          CONSTANT (2)\n"
            + "00024 00024       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "00026 00026       36             PRE_DEC\n"
            + "'     ~a[3]\n"
            + "00027 00027       38 03          CONSTANT (3)\n"
            + "00029 00029       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "0002B 0002B       10             SIGN_EXTEND_BYTE\n"
            + "'     ~~a[4]\n"
            + "0002C 0002C       38 04          CONSTANT (4)\n"
            + "0002E 0002E       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "00030 00030       14             SIGN_EXTEND_WORD\n"
            + "00031 00031       32             RETURN\n"
            + "00032 00032       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testPostEffectsOperators() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a++\n"
            + "    a--\n"
            + "    a~\n"
            + "    a~~\n"
            + "    word[a]++\n"
            + "    byte[a]--\n"
            + "    long[a]~\n"
            + "    long[a]~~\n"
            + "    a[1]++\n"
            + "    a[2]--\n"
            + "    a[3]~\n"
            + "    a[4]~~\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       30 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     a++\n"
            + "00008 00008       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00009 00009       2E             POST_INC\n"
            + "'     a--\n"
            + "0000A 0000A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       3E             POST_DEC\n"
            + "'     a~\n"
            + "0000C 0000C       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       18             POST_CLEAR\n"
            + "'     a~~\n"
            + "0000E 0000E       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       1C             POST_SET\n"
            + "'     word[a]++\n"
            + "00010 00010       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00011 00011       A2             MEM_ASSIGN WORD POP\n"
            + "00012 00012       2E             POST_INC\n"
            + "'     byte[a]--\n"
            + "00013 00013       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00014 00014       82             MEM_ASSIGN BYTE POP\n"
            + "00015 00015       3E             POST_DEC\n"
            + "'     long[a]~\n"
            + "00016 00016       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00017 00017       C2             MEM_ASSIGN LONG POP\n"
            + "00018 00018       18             POST_CLEAR\n"
            + "'     long[a]~~\n"
            + "00019 00019       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0001A 0001A       C2             MEM_ASSIGN LONG POP\n"
            + "0001B 0001B       1C             POST_SET\n"
            + "'     a[1]++\n"
            + "0001C 0001C       36             CONSTANT (1)\n"
            + "0001D 0001D       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "0001F 0001F       2E             POST_INC\n"
            + "'     a[2]--\n"
            + "00020 00020       38 02          CONSTANT (2)\n"
            + "00022 00022       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "00024 00024       3E             POST_DEC\n"
            + "'     a[3]~\n"
            + "00025 00025       38 03          CONSTANT (3)\n"
            + "00027 00027       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "00029 00029       18             POST_CLEAR\n"
            + "'     a[4]~~\n"
            + "0002A 0002A       38 04          CONSTANT (4)\n"
            + "0002C 0002C       DE 04          VAR_MODIFY_INDEXED LONG DBASE+$0004 (short)\n"
            + "0002E 0002E       1C             POST_SET\n"
            + "0002F 0002F       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testDatUnaryOperators() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    a++\n"
            + "    a--\n"
            + "    a~\n"
            + "    a~~\n"
            + "    ++a\n"
            + "    --a\n"
            + "    ~a\n"
            + "    ~~a\n"
            + "    -a\n"
            + "DAT\n"
            + "a   long 0\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       28 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 00 00    Function main @ $000C (local size 0)\n"
            + "00008 00008   000 00 00 00 00    a                   long    0\n"
            + "' PUB main\n"
            + "'     a++\n"
            + "0000C 0000C       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "0000E 0000E       2E             POST_INC\n"
            + "'     a--\n"
            + "0000F 0000F       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00011 00011       3E             POST_DEC\n"
            + "'     a~\n"
            + "00012 00012       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00014 00014       18             POST_CLEAR\n"
            + "'     a~~\n"
            + "00015 00015       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00017 00017       1C             POST_SET\n"
            + "'     ++a\n"
            + "00018 00018       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "0001A 0001A       26             PRE_INC\n"
            + "'     --a\n"
            + "0001B 0001B       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "0001D 0001D       36             PRE_DEC\n"
            + "'     ~a\n"
            + "0001E 0001E       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00020 00020       10             SIGN_EXTEND_BYTE\n"
            + "'     ~~a\n"
            + "00021 00021       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00023 00023       14             SIGN_EXTEND_WORD\n"
            + "'     -a\n"
            + "00024 00024       C4 08          MEM_READ LONG PBASE+$0008\n"
            + "00026 00026       46             NEGATE\n"
            + "00027 00027       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testUnaryOperatorsAssignment() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    b := a++\n"
            + "    b := a--\n"
            + "    b := a~\n"
            + "    b := a~~\n"
            + "    b := ++a\n"
            + "    b := --a\n"
            + "    b := ~a\n"
            + "    b := ~~a\n"
            + "    b := -a\n"
            + "DAT\n"
            + "a   long 0\n"
            + "b   long 0\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       40 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008   000 00 00 00 00    a                   long    0\n"
            + "0000C 0000C   001 00 00 00 00    b                   long    0\n"
            + "' PUB main\n"
            + "'     b := a++\n"
            + "00010 00010       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00012 00012       AE             POST_INC\n"
            + "00013 00013       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := a--\n"
            + "00015 00015       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00017 00017       BE             POST_DEC\n"
            + "00018 00018       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := a~\n"
            + "0001A 0001A       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "0001C 0001C       98             POST_CLEAR\n"
            + "0001D 0001D       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := a~~\n"
            + "0001F 0001F       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00021 00021       9C             POST_SET\n"
            + "00022 00022       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := ++a\n"
            + "00024 00024       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00026 00026       A6             PRE_INC\n"
            + "00027 00027       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := --a\n"
            + "00029 00029       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "0002B 0002B       B6             PRE_DEC\n"
            + "0002C 0002C       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := ~a\n"
            + "0002E 0002E       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00030 00030       90             SIGN_EXTEND_BYTE\n"
            + "00031 00031       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := ~~a\n"
            + "00033 00033       C6 08          MEM_ASSIGN LONG PBASE+$0008\n"
            + "00035 00035       94             SIGN_EXTEND_WORD\n"
            + "00036 00036       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "'     b := -a\n"
            + "00038 00038       C4 08          MEM_READ LONG PBASE+$0008\n"
            + "0003A 0003A       E6             NEGATE\n"
            + "0003B 0003B       C5 0C          MEM_WRITE LONG PBASE+$000C\n"
            + "0003D 0003D       32             RETURN\n"
            + "0003E 0003E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testDatUnaryOperatorsAssignment() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    b := a++\n"
            + "    b := a--\n"
            + "    b := a~\n"
            + "    b := a~~\n"
            + "    b := ++a\n"
            + "    b := --a\n"
            + "    b := ~a\n"
            + "    b := ~~a\n"
            + "    b := -a\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       24 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     b := a++\n"
            + "00008 00008       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00009 00009       AE             POST_INC\n"
            + "0000A 0000A       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := a--\n"
            + "0000B 0000B       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       BE             POST_DEC\n"
            + "0000D 0000D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := a~\n"
            + "0000E 0000E       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       98             POST_CLEAR\n"
            + "00010 00010       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := a~~\n"
            + "00011 00011       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00012 00012       9C             POST_SET\n"
            + "00013 00013       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := ++a\n"
            + "00014 00014       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00015 00015       A6             PRE_INC\n"
            + "00016 00016       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := --a\n"
            + "00017 00017       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00018 00018       B6             PRE_DEC\n"
            + "00019 00019       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := ~a\n"
            + "0001A 0001A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0001B 0001B       90             SIGN_EXTEND_BYTE\n"
            + "0001C 0001C       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := ~~a\n"
            + "0001D 0001D       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0001E 0001E       94             SIGN_EXTEND_WORD\n"
            + "0001F 0001F       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     b := -a\n"
            + "00020 00020       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00021 00021       E6             NEGATE\n"
            + "00022 00022       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "00023 00023       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testRegisterUnaryOperators() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    DIRA[1]~\n"
            + "    DIRA[2]~~\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     DIRA[1]~\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       3D D6          REGBIT_MODIFY $1F6\n"
            + "0000B 0000B       18             POST_CLEAR\n"
            + "'     DIRA[2]~~\n"
            + "0000C 0000C       38 02          CONSTANT (2)\n"
            + "0000E 0000E       3D D6          REGBIT_MODIFY $1F6\n"
            + "00010 00010       1C             POST_SET\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRegisterBit() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    DIRA[1] := 1\n"
            + "    DIRA[2..5] := 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     DIRA[1] := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       3D B6          REGBIT_WRITE $1F6\n"
            + "'     DIRA[2..5] := 1\n"
            + "0000C 0000C       36             CONSTANT (1)\n"
            + "0000D 0000D       38 02          CONSTANT (2)\n"
            + "0000F 0000F       38 05          CONSTANT (5)\n"
            + "00011 00011       3E B6          REGBIT_RANGE_WRITE $1F6\n"
            + "00013 00013       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testVarSizeAssignment() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    BYTE[a] := 1\n"
            + "    b := BYTE[a]\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     BYTE[a] := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       81             MEM_WRITE BYTE POP\n"
            + "'     b := BYTE[a]\n"
            + "0000B 0000B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       80             MEM_READ BYTE POP\n"
            + "0000D 0000D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testVarOrdering() throws Exception {
        String text = ""
            + "VAR\n"
            + "\n"
            + "    byte a\n"
            + "    word b\n"
            + "    long c\n"
            + "\n"
            + "PUB main\n"
            + "\n"
            + "    a := 1\n"
            + "    b := 1\n"
            + "    c := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 8)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' PUB main\n"
            + "'     a := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       89 06          VAR_WRITE BYTE VBASE+$0006\n"
            + "'     b := 1\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       A9 04          VAR_WRITE WORD VBASE+$0004\n"
            + "'     c := 1\n"
            + "0000E 0000E       36             CONSTANT (1)\n"
            + "0000F 0000F       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testIfCaseElse() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    if a >= 1\n"
            + "        case a\n"
            + "            1: a := 4\n"
            + "            2: a := 5\n"
            + "            3: a := 6\n"
            + "    else\n"
            + "        a := 8\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       30 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     if a >= 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000A 0000A       DA             TEST_ABOVE\n"
            + "0000B 0000B       0A 1D          JZ $0002A (29)\n"
            + "'         case a\n"
            + "0000D 0000D       38 28          ADDRESS ($0028)\n"
            + "0000F 0000F       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00010 00010       36             CONSTANT (1)\n"
            + "00011 00011       0D 09          CASE-JMP $0001C (9)\n"
            + "00013 00013       37 00          CONSTANT (2)\n"
            + "00015 00015       0D 09          CASE-JMP $00020 (9)\n"
            + "00017 00017       37 21          CONSTANT (3)\n"
            + "00019 00019       0D 09          CASE-JMP $00024 (9)\n"
            + "0001B 0001B       0C             CASE_DONE\n"
            + "'             1: a := 4\n"
            + "0001C 0001C       37 01          CONSTANT (4)\n"
            + "0001E 0001E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001F 0001F       0C             CASE_DONE\n"
            + "'             2: a := 5\n"
            + "00020 00020       38 05          CONSTANT (5)\n"
            + "00022 00022       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00023 00023       0C             CASE_DONE\n"
            + "'             3: a := 6\n"
            + "00024 00024       38 06          CONSTANT (6)\n"
            + "00026 00026       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00027 00027       0C             CASE_DONE\n"
            + "00028 00028       04 03          JMP $0002D (3)\n"
            + "'     else\n"
            + "'         a := 8\n"
            + "0002A 0002A       37 02          CONSTANT (8)\n"
            + "0002C 0002C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0002D 0002D       32             RETURN\n"
            + "0002E 0002E       00 00          Padding\n"
            + "", compile(text, true));
    }

    @Test
    void testTypeIndex() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := WORD[b]\n"
            + "    WORD[b] := a\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := WORD[b]\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       A0             MEM_READ WORD POP\n"
            + "0000A 0000A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     WORD[b] := a\n"
            + "0000B 0000B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000D 0000D       A1             MEM_WRITE WORD POP\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testArray() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := WORD[b][0]\n"
            + "    WORD[b][0] := a\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := WORD[b][0]\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       35             CONSTANT (0)\n"
            + "0000A 0000A       B0             MEM_READ_INDEXED WORD POP\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     WORD[b][0] := a\n"
            + "0000C 0000C       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000E 0000E       35             CONSTANT (0)\n"
            + "0000F 0000F       B1             MEM_WRITE_INDEXED WORD POP\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testVariableIndex() throws Exception {
        String text = ""
            + "PUB main | a, b[5]\n"
            + "\n"
            + "    a := b[1]\n"
            + "    b[1] := a\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 18 00    Function main @ $0008 (local size 24)\n"
            + "' PUB main | a, b[5]\n"
            + "'     a := b[1]\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       DC 08          VAR_READ_INDEXED LONG DBASE+$0008 (short)\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b[1] := a\n"
            + "0000C 0000C       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       DD 08          VAR_WRITE_INDEXED LONG DBASE+$0008 (short)\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testIndexedMemPostEffect() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    BYTE[a + 1][0]~\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     BYTE[a + 1][0]~\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       EC             ADD\n"
            + "0000B 0000B       35             CONSTANT (0)\n"
            + "0000C 0000C       92             MEM_ASSIGN_INDEXED BYTE POP\n"
            + "0000D 0000D       18             POST_CLEAR\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testTypedMemPostEffect() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    LONG[a + 1]~\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     LONG[a + 1]~\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       36             CONSTANT (1)\n"
            + "0000A 0000A       EC             ADD\n"
            + "0000B 0000B       C2             MEM_ASSIGN LONG POP\n"
            + "0000C 0000C       18             POST_CLEAR\n"
            + "0000D 0000D       32             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testRepeatComplexExpression() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    repeat until ((a := byte[b][c++]) == 0)\n"
            + "        a := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 0C 00    Function main @ $0008 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     repeat until ((a := byte[b][c++]) == 0)\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       6E             VAR_MODIFY LONG DBASE+$000C (short)\n"
            + "0000A 0000A       AE             POST_INC\n"
            + "0000B 0000B       90             MEM_READ_INDEXED BYTE POP\n"
            + "0000C 0000C       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       80             WRITE\n"
            + "0000E 0000E       35             CONSTANT (0)\n"
            + "0000F 0000F       FC             TEST_EQUAL\n"
            + "00010 00010       0B 04          JNZ $00016 (4)\n"
            + "'         a := 1\n"
            + "00012 00012       36             CONSTANT (1)\n"
            + "00013 00013       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00014 00014       04 72          JMP $00008 (-14)\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testUnaryExpressions() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := -b\n"
            + "    a := ?b\n"
            + "    a := b?\n"
            + "    a++\n"
            + "    --a\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := -b\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       E6             NEGATE\n"
            + "0000A 0000A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := ?b\n"
            + "0000B 0000B       6A             VAR_MODIFY LONG DBASE+$0008 (short)\n"
            + "0000C 0000C       88             RANDOM_FORWARD\n"
            + "0000D 0000D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := b?\n"
            + "0000E 0000E       6A             VAR_MODIFY LONG DBASE+$0008 (short)\n"
            + "0000F 0000F       8C             RANDOM_REVERSE\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++\n"
            + "00011 00011       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00012 00012       2E             POST_INC\n"
            + "'     --a\n"
            + "00013 00013       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00014 00014       36             PRE_DEC\n"
            + "00015 00015       32             RETURN\n"
            + "00016 00016       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testNegativeConstants() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a := -1\n"
            + "    a := -10\n"
            + "    a := -100\n"
            + "    a := -1_000\n"
            + "    a := -32_767\n"
            + "    a := -32_768\n"
            + "    a := -32_769\n"
            + "    a := -100_000\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       2C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'     a := -1\n"
            + "00008 00008       34             CONSTANT (-1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -10\n"
            + "0000A 0000A       38 09 E7       CONSTANT (-10)\n"
            + "0000D 0000D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -100\n"
            + "0000E 0000E       38 63 E7       CONSTANT (-100)\n"
            + "00011 00011       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -1_000\n"
            + "00012 00012       39 03 E7       CONSTANT (1_000 - 1)\n"
            + "00015 00015       E7             COMPLEMENT\n"
            + "00016 00016       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -32_767\n"
            + "00017 00017       39 7F FE       CONSTANT (32_767 - 1)\n"
            + "0001A 0001A       E7             COMPLEMENT\n"
            + "0001B 0001B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -32_768\n"
            + "0001C 0001C       37 6E          CONSTANT (-32_768)\n"
            + "0001E 0001E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -32_769\n"
            + "0001F 0001F       37 4E          CONSTANT (-32_769)\n"
            + "00021 00021       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := -100_000\n"
            + "00022 00022       3B FF FE 79 60 CONSTANT (-100_000)\n"
            + "00027 00027       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00028 00028       32             RETURN\n"
            + "00029 00029       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testString() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    a := string(\"1234\", 13, 10)\n"
            + "    b := \"1234\"\n"
            + "    c := @\"1234\"\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       24 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 0C 00    Function main @ $0008 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     a := string(\"1234\", 13, 10)\n"
            + "00008 00008       87 80 13       MEM_ADDRESS BYTE PBASE+$0013\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"1234\"\n"
            + "0000C 0000C       87 1A          MEM_ADDRESS BYTE PBASE+$001A\n"
            + "0000E 0000E       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     c := @\"1234\"\n"
            + "0000F 0000F       87 1F          MEM_ADDRESS BYTE PBASE+$001F\n"
            + "00011 00011       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00012 00012       32             RETURN\n"
            + "' (string data)\n"
            + "00013 00013       31 32 33 34 0D STRING\n"
            + "00018 00018       0A 00\n"
            + "0001A 0001A       31 32 33 34 00 STRING\n"
            + "0001F 0001F       31 32 33 34 00 STRING\n"
            + "", compile(text, true));
    }

    @Test
    void testOptimizeStrings() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    a := string(\"1234\", 13, 10)\n"
            + "    b := \"1234\"\n"
            + "    c := @\"1234\"\n"
            + "\n"
            + "PUB setup | a, b, c\n"
            + "\n"
            + "    a := string(\"1234\", 13, 10)\n"
            + "    b := \"1234\"\n"
            + "    c := @\"1234\"\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       30 00          Object size\n"
            + "00002 00002       03             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 0C 00    Function main @ $000C (local size 12)\n"
            + "00008 00008       17 00 0C 00    Function setup @ $0017 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     a := string(\"1234\", 13, 10)\n"
            + "0000C 0000C       87 80 22       MEM_ADDRESS BYTE PBASE+$0022\n"
            + "0000F 0000F       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"1234\"\n"
            + "00010 00010       87 29          MEM_ADDRESS BYTE PBASE+$0029\n"
            + "00012 00012       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     c := @\"1234\"\n"
            + "00013 00013       87 29          MEM_ADDRESS BYTE PBASE+$0029\n"
            + "00015 00015       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00016 00016       32             RETURN\n"
            + "' PUB setup | a, b, c\n"
            + "'     a := string(\"1234\", 13, 10)\n"
            + "00017 00017       87 80 22       MEM_ADDRESS BYTE PBASE+$0022\n"
            + "0001A 0001A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"1234\"\n"
            + "0001B 0001B       87 29          MEM_ADDRESS BYTE PBASE+$0029\n"
            + "0001D 0001D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     c := @\"1234\"\n"
            + "0001E 0001E       87 29          MEM_ADDRESS BYTE PBASE+$0029\n"
            + "00020 00020       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00021 00021       32             RETURN\n"
            + "' (string data)\n"
            + "00022 00022       31 32 33 34 0D STRING\n"
            + "00027 00027       0A 00\n"
            + "00029 00029       31 32 33 34 00 STRING\n"
            + "0002E 0002E       00 00          Padding\n"
            + "", compile(text, false));
    }

    @Test
    void testOptimizePartialStrings() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    a := string(\"12345678\", 13, 10)\n"
            + "    b := \"12345678\"\n"
            + "    c := @\"5678\"\n"
            + "\n"
            + "PUB setup | a, b, c\n"
            + "\n"
            + "    a := string(\"12345678\", 13, 10)\n"
            + "    b := \"12345678\"\n"
            + "    c := @\"5678\"\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       38 00          Object size\n"
            + "00002 00002       03             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 0C 00    Function main @ $000C (local size 12)\n"
            + "00008 00008       17 00 0C 00    Function setup @ $0017 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     a := string(\"12345678\", 13, 10)\n"
            + "0000C 0000C       87 80 22       MEM_ADDRESS BYTE PBASE+$0022\n"
            + "0000F 0000F       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"12345678\"\n"
            + "00010 00010       87 2D          MEM_ADDRESS BYTE PBASE+$002D\n"
            + "00012 00012       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     c := @\"5678\"\n"
            + "00013 00013       87 31          MEM_ADDRESS BYTE PBASE+$0031\n"
            + "00015 00015       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00016 00016       32             RETURN\n"
            + "' PUB setup | a, b, c\n"
            + "'     a := string(\"12345678\", 13, 10)\n"
            + "00017 00017       87 80 22       MEM_ADDRESS BYTE PBASE+$0022\n"
            + "0001A 0001A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"12345678\"\n"
            + "0001B 0001B       87 2D          MEM_ADDRESS BYTE PBASE+$002D\n"
            + "0001D 0001D       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "'     c := @\"5678\"\n"
            + "0001E 0001E       87 31          MEM_ADDRESS BYTE PBASE+$0031\n"
            + "00020 00020       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00021 00021       32             RETURN\n"
            + "' (string data)\n"
            + "00022 00022       31 32 33 34 35 STRING\n"
            + "00027 00027       36 37 38 0D 0A\n"
            + "0002C 0002C       00\n"
            + "0002D 0002D       31 32 33 34 35 STRING\n"
            + "00032 00032       36 37 38 00\n"
            + "00036 00036       00 00          Padding\n"
            + "", compile(text, false));
    }

    @Test
    void testCharacterLiteral() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := string(\"1\")\n"
            + "    b := \"2\"\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := string(\"1\")\n"
            + "00008 00008       87 80 10       MEM_ADDRESS BYTE PBASE+$0010\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b := \"2\"\n"
            + "0000C 0000C       38 32          CONSTANT (\"2\")\n"
            + "0000E 0000E       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000F 0000F       32             RETURN\n"
            + "' (string data)\n"
            + "00010 00010       31 00          STRING\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testMultipleAssignment() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := b := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := b := 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       6A             VAR_MODIFY LONG DBASE+$0008 (short)\n"
            + "0000A 0000A       80             WRITE\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       32             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testCognewInterpreter() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    cognew(thread(1,2), @b)\n"
            + "\n"
            + "PUB thread(p1,p2)\n"
            + "\n"
            + "    p1 := 1\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       03             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 08 00    Function main @ $000C (local size 8)\n"
            + "00008 00008       16 00 00 00    Function thread @ $0016 (local size 0)\n"
            + "' PUB main | a, b\n"
            + "'     cognew(thread(1,2), @b)\n"
            + "0000C 0000C       36             CONSTANT (1)\n"
            + "0000D 0000D       38 02          CONSTANT (2)\n"
            + "0000F 0000F       39 02 02       CONSTANT ($202)\n"
            + "00012 00012       6B             VAR_ADDRESS LONG DBASE+$0008 (short)\n"
            + "00013 00013       15             MARK_INTERPRETED\n"
            + "00014 00014       2C             COGNEW\n"
            + "00015 00015       32             RETURN\n"
            + "' PUB thread(p1,p2)\n"
            + "'     p1 := 1\n"
            + "00016 00016       36             CONSTANT (1)\n"
            + "00017 00017       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00018 00018       32             RETURN\n"
            + "00019 00019       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testCognewPAsm() throws Exception {
        String text = ""
            + "PUB main\n"
            + "\n"
            + "    cognew(@start, 0)\n"
            + "\n"
            + "DAT             org     $000\n"
            + "\n"
            + "start           cogid   a\n"
            + "                cogstop a\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       10 00 00 00    Function main @ $0010 (local size 0)\n"
            + "00008 00008   000                                    org     $000\n"
            + "00008 00008   000 01 04 FC 0C    start               cogid   a\n"
            + "0000C 0000C   001 03 04 7C 0C                        cogstop a\n"
            + "00010 00010   002                a                   res     1\n"
            + "' PUB main\n"
            + "'     cognew(@start, 0)\n"
            + "00010 00010       34             CONSTANT (-1)\n"
            + "00011 00011       C7 08          MEM_ADDRESS LONG PBASE+$0008\n"
            + "00013 00013       35             CONSTANT (0)\n"
            + "00014 00014       2C             COGNEW\n"
            + "00015 00015       32             RETURN\n"
            + "00016 00016       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testDatVariableType() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a := b.byte\n"
            + "    b.byte := a\n"
            + "DAT\n"
            + "b   long 0\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 04 00    Function main @ $000C (local size 4)\n"
            + "00008 00008   000 00 00 00 00    b                   long    0\n"
            + "' PUB main | a\n"
            + "'     a := b.byte\n"
            + "0000C 0000C       84 08          MEM_READ BYTE PBASE+$0008\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b.byte := a\n"
            + "0000F 0000F       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00010 00010       85 08          MEM_WRITE BYTE PBASE+$0008\n"
            + "00012 00012       32             RETURN\n"
            + "00013 00013       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testVariableTypeIndex() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := b.byte[1]\n"
            + "    b.byte[1] := a\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := b.byte[1]\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       9C 08          MEM_READ_INDEXED BYTE DBASE+$0008\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     b.byte[1] := a\n"
            + "0000C 0000C       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       9D 08          MEM_WRITE_INDEXED BYTE DBASE+$0008\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testAddress() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := @b\n"
            + "    a := @@b\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := @b\n"
            + "00008 00008       6B             VAR_ADDRESS LONG DBASE+$0008 (short)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := @@b\n"
            + "0000A 0000A       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000B 0000B       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0000D 0000D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testVarAddress() throws Exception {
        String text = ""
            + "VAR b[20], c\n"
            + "\n"
            + "PUB main | a\n"
            + "\n"
            + "        a := @b\n"
            + "        a := @c\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 84)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'         a := @b\n"
            + "00008 00008       43             VAR_ADDRESS LONG VBASE+$0000 (short)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'         a := @c\n"
            + "0000A 0000A       CB 50          VAR_ADDRESS LONG VBASE+$0050\n"
            + "0000C 0000C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       32             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testVarAbsoluteAddress() throws Exception {
        String text = ""
            + "VAR b[20], c\n"
            + "\n"
            + "PUB main | a\n"
            + "\n"
            + "        a := @@b\n"
            + "        a := @@c\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 84)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' PUB main | a\n"
            + "'         a := @@b\n"
            + "00008 00008       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00009 00009       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'         a := @@c\n"
            + "0000C 0000C       C8 50          VAR_READ LONG VBASE+$0050\n"
            + "0000E 0000E       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testLocalVarAddress() throws Exception {
        String text = ""
            + "PUB main | a, b[20], c\n"
            + "\n"
            + "        a := @b\n"
            + "        a := @c\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 58 00    Function main @ $0008 (local size 88)\n"
            + "' PUB main | a, b[20], c\n"
            + "'         a := @b\n"
            + "00008 00008       6B             VAR_ADDRESS LONG DBASE+$0008 (short)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'         a := @c\n"
            + "0000A 0000A       CF 58          VAR_ADDRESS LONG DBASE+$0058\n"
            + "0000C 0000C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000D 0000D       32             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testLocalVarAbsoluteAddress() throws Exception {
        String text = ""
            + "PUB main | a, b[20], c\n"
            + "\n"
            + "        a := @@b\n"
            + "        a := @@c\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 58 00    Function main @ $0008 (local size 88)\n"
            + "' PUB main | a, b[20], c\n"
            + "'         a := @@b\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'         a := @@c\n"
            + "0000C 0000C       CC 58          VAR_READ LONG DBASE+$0058\n"
            + "0000E 0000E       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testTypedAddress() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := @word[b]\n"
            + "    a := @@word[b]\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     a := @word[b]\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       A3             MEM_ADDRESS WORD POP\n"
            + "0000A 0000A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := @@word[b]\n"
            + "0000B 0000B       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000C 0000C       A0             MEM_READ WORD POP\n"
            + "0000D 0000D       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0000F 0000F       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testTypedAddressIndex() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    a := @word[b][c]\n"
            + "    a := @@word[b][c]\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 0C 00    Function main @ $0008 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     a := @word[b][c]\n"
            + "00008 00008       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00009 00009       6C             VAR_READ LONG DBASE+$000C (short)\n"
            + "0000A 0000A       B3             MEM_ADDRESS_INDEXED WORD POP\n"
            + "0000B 0000B       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := @@word[b][c]\n"
            + "0000C 0000C       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0000D 0000D       6C             VAR_READ LONG DBASE+$000C (short)\n"
            + "0000E 0000E       B0             MEM_READ_INDEXED WORD POP\n"
            + "0000F 0000F       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00011 00011       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00012 00012       32             RETURN\n"
            + "00013 00013       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testOrg() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                long    0\n"
            + "                long    1\n"
            + "                org   $100\n"
            + "                long    2\n"
            + "                long    3\n"
            + "                long    4\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 00 00 00 00                        long    0\n"
            + "00008 00008   001 01 00 00 00                        long    1\n"
            + "0000C 0000C   002                                    org     $100\n"
            + "0000C 0000C   100 02 00 00 00                        long    2\n"
            + "00010 00010   101 03 00 00 00                        long    3\n"
            + "00014 00014   102 04 00 00 00                        long    4\n"
            + "", compile(text));
    }

    @Test
    void testRes() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                long    0\n"
            + "                res    1\n"
            + "                res    2\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       08 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 00 00 00 00                        long    0\n"
            + "00008 00008   001                                    res     1\n"
            + "00008 00008   002                                    res     2\n"
            + "", compile(text));
    }

    @Test
    void testFit() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                long    0[$10]\n"
            + "                fit   $10\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       44 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 00 00 00 00                        long    0[$10]\n"
            + "00008 00008   001 00 00 00 00   \n"
            + "0000C 0000C   002 00 00 00 00   \n"
            + "00010 00010   003 00 00 00 00   \n"
            + "00014 00014   004 00 00 00 00   \n"
            + "00018 00018   005 00 00 00 00   \n"
            + "0001C 0001C   006 00 00 00 00   \n"
            + "00020 00020   007 00 00 00 00   \n"
            + "00024 00024   008 00 00 00 00   \n"
            + "00028 00028   009 00 00 00 00   \n"
            + "0002C 0002C   00A 00 00 00 00   \n"
            + "00030 00030   00B 00 00 00 00   \n"
            + "00034 00034   00C 00 00 00 00   \n"
            + "00038 00038   00D 00 00 00 00   \n"
            + "0003C 0003C   00E 00 00 00 00   \n"
            + "00040 00040   00F 00 00 00 00\n"
            + "00044 00044   010                                    fit     $10\n"
            + "", compile(text));
    }

    @Test
    void testFitLimit() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                long    0[$10]\n"
            + "                long    1\n"
            + "                fit   $10\n"
            + "";

        Assertions.assertThrows(CompilerException.class, new Executable() {

            @Override
            public void execute() throws Throwable {
                compile(text);
            }
        });
    }

    @Test
    void testByte() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                byte    0\n"
            + "                byte    1\n"
            + "                byte    2\n"
            + "                byte    3\n"
            + "                byte    4\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       09 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 00                                 byte    0\n"
            + "00005 00005   000 01                                 byte    1\n"
            + "00006 00006   000 02                                 byte    2\n"
            + "00007 00007   000 03                                 byte    3\n"
            + "00008 00008   001 04                                 byte    4\n"
            + "", compile(text));
    }

    @Test
    void testWord() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                word    0\n"
            + "                word    1\n"
            + "                word    2\n"
            + "                word    3\n"
            + "                word    4\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0E 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 00 00                              word    0\n"
            + "00006 00006   000 01 00                              word    1\n"
            + "00008 00008   001 02 00                              word    2\n"
            + "0000A 0000A   001 03 00                              word    3\n"
            + "0000C 0000C   002 04 00                              word    4\n"
            + "", compile(text));
    }

    @Test
    void testAbsoluteAddress() throws Exception {
        String text = ""
            + "DAT             org   $000\n"
            + "                mov   a, #@@a\n"
            + "                ret\n"
            + "a               res   1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000 0C 04 FC A0                        mov     a, #@@a\n"
            + "00008 00008   001 00 00 7C 5C                        ret\n"
            + "0000C 0000C   002                a                   res     1\n"
            + "", compile(text));
    }

    @Test
    void testSpinAbsoluteAddress() throws Exception {
        String text = ""
            + "PUB main | a\n"
            + "\n"
            + "    a := @@driver\n"
            + "\n"
            + "DAT\n"
            + "                org   $000\n"
            + "driver          jmp   #$\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 04 00    Function main @ $000C (local size 4)\n"
            + "00008 00008   000                                    org     $000\n"
            + "00008 00008   000 00 00 7C 5C    driver              jmp     #$\n"
            + "' PUB main | a\n"
            + "'     a := @@driver\n"
            + "0000C 0000C       C4 08          MEM_READ LONG PBASE+$0008\n"
            + "0000E 0000E       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00010 00010       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00011 00011       32             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testSpinAbsoluteAddressExpression() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    a := @@driver + b\n"
            + "\n"
            + "DAT\n"
            + "                org   $000\n"
            + "driver          jmp   #$\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 08 00    Function main @ $000C (local size 8)\n"
            + "00008 00008   000                                    org     $000\n"
            + "00008 00008   000 00 00 7C 5C    driver              jmp     #$\n"
            + "' PUB main | a, b\n"
            + "'     a := @@driver + b\n"
            + "0000C 0000C       C4 08          MEM_READ LONG PBASE+$0008\n"
            + "0000E 0000E       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00010 00010       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00011 00011       EC             ADD\n"
            + "00012 00012       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00013 00013       32             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testSpinAbsoluteAddressIndex() throws Exception {
        String text = ""
            + "PUB main | a, c\n"
            + "\n"
            + "    a := BYTE[@@b[c]]\n"
            + "    a := BYTE[@@w[c]]\n"
            + "    a := BYTE[@@l[c]]\n"
            + "\n"
            + "DAT\n"
            + "\n"
            + "b   byte    1, 2, 3, 4\n"
            + "w   word    1, 2, 3, 4\n"
            + "l   long    1, 2, 3, 4\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       3C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       24 00 08 00    Function main @ $0024 (local size 8)\n"
            + "00008 00008   000 01 02 03 04    b                   byte    1, 2, 3, 4\n"
            + "0000C 0000C   001 01 00 02 00    w                   word    1, 2, 3, 4\n"
            + "00010 00010   002 03 00 04 00\n"
            + "00014 00014   003 01 00 00 00    l                   long    1, 2, 3, 4\n"
            + "00018 00018   004 02 00 00 00   \n"
            + "0001C 0001C   005 03 00 00 00   \n"
            + "00020 00020   006 04 00 00 00\n"
            + "' PUB main | a, c\n"
            + "'     a := BYTE[@@b[c]]\n"
            + "00024 00024       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00025 00025       94 08          MEM_READ_INDEXED BYTE PBASE+$0008\n"
            + "00027 00027       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00029 00029       80             MEM_READ BYTE POP\n"
            + "0002A 0002A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := BYTE[@@w[c]]\n"
            + "0002B 0002B       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "0002C 0002C       B4 0C          MEM_READ_INDEXED WORD PBASE+$000C\n"
            + "0002E 0002E       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00030 00030       80             MEM_READ BYTE POP\n"
            + "00031 00031       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := BYTE[@@l[c]]\n"
            + "00032 00032       68             VAR_READ LONG DBASE+$0008 (short)\n"
            + "00033 00033       D4 14          MEM_READ_INDEXED LONG PBASE+$0014\n"
            + "00035 00035       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00037 00037       80             MEM_READ BYTE POP\n"
            + "00038 00038       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00039 00039       32             RETURN\n"
            + "0003A 0003A       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testSpinAbsoluteVarAddressIndex() throws Exception {
        String text = ""
            + "PUB main | a, b, c\n"
            + "\n"
            + "    a := BYTE[@@b[c]]\n"
            + "    a := WORD[@@b[c]]\n"
            + "    a := LONG[@@b[c]]\n"
            + "\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 0C 00    Function main @ $0008 (local size 12)\n"
            + "' PUB main | a, b, c\n"
            + "'     a := BYTE[@@b[c]]\n"
            + "00008 00008       6C             VAR_READ LONG DBASE+$000C (short)\n"
            + "00009 00009       DC 08          VAR_READ_INDEXED LONG DBASE+$0008 (short)\n"
            + "0000B 0000B       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0000D 0000D       80             MEM_READ BYTE POP\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := WORD[@@b[c]]\n"
            + "0000F 0000F       6C             VAR_READ LONG DBASE+$000C (short)\n"
            + "00010 00010       DC 08          VAR_READ_INDEXED LONG DBASE+$0008 (short)\n"
            + "00012 00012       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "00014 00014       A0             MEM_READ WORD POP\n"
            + "00015 00015       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a := LONG[@@b[c]]\n"
            + "00016 00016       6C             VAR_READ LONG DBASE+$000C (short)\n"
            + "00017 00017       DC 08          VAR_READ_INDEXED LONG DBASE+$0008 (short)\n"
            + "00019 00019       97 00          MEM_ADDRESS_INDEXED BYTE PBASE+$0000\n"
            + "0001B 0001B       C0             MEM_READ LONG POP\n"
            + "0001C 0001C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001D 0001D       32             RETURN\n"
            + "0001E 0001E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    public void testRotateAssign() throws Exception {
        String text = ""
            + "PUB main | a, b\n"
            + "\n"
            + "    dira[a] := b <-= 1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 08 00    Function main @ $0008 (local size 8)\n"
            + "' PUB main | a, b\n"
            + "'     dira[a] := b <-= 1\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       6A             VAR_MODIFY LONG DBASE+$0008 (short)\n"
            + "0000A 0000A       C1             ROTATE_LEFT\n"
            + "0000B 0000B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       3D B6          REGBIT_WRITE $1F6\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
            + "", compile(text));
    }

    String compile(String text) throws Exception {
        return compile(text, false);
    }

    String compile(String text, boolean openspinCompatible) throws Exception {
        Spin1TokenStream stream = new Spin1TokenStream(text);
        Spin1Parser subject = new Spin1Parser(stream);
        Node root = subject.parse();

        Spin1ObjectCompiler compiler = new Spin1ObjectCompiler(new Spin1Compiler());
        compiler.setOpenspinCompatibile(openspinCompatible);
        Spin1Object obj = compiler.compileObject(root);

        for (CompilerException msg : compiler.getMessages()) {
            if (msg.type == CompilerException.ERROR) {
                throw msg;
            }
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        obj.generateListing(new PrintStream(os));

        return os.toString().replaceAll("\\r\\n", "\n");
    }

}
