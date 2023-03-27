/*
 * Copyright (c) 2023 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spinc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.maccasoft.propeller.CompilerException;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.spin2.Spin2Debugger;
import com.maccasoft.propeller.spin2.Spin2Object;

class Spin2CObjectCompilerTest {

    @Test
    void testEmptyFunction() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       0A 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "' }\n"
            + "00009 00009       04             RETURN\n"
            + "0000A 0000A       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testParameterVarAssignment() throws Exception {
        String text = ""
            + "void main(int a)\n"
            + "{\n"
            + "    a = 1;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0C 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "' }\n"
            + "0000B 0000B       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testLocalVarAssignment() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "\n"
            + "    a = 1;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       0C 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "' }\n"
            + "0000B 0000B       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testLocalVarDeclarationAndAssignment() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a = 1;\n"
            + "    int b = 2, c = 3, d = 4;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       12 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       04             (stack size)\n"
            + "'     int a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     int b = 2, c = 3, d = 4;\n"
            + "0000B 0000B       A3             CONSTANT (2)\n"
            + "0000C 0000C       F1             VAR_WRITE LONG DBASE+$00001 (short)\n"
            + "0000D 0000D       A4             CONSTANT (3)\n"
            + "0000E 0000E       F2             VAR_WRITE LONG DBASE+$00002 (short)\n"
            + "0000F 0000F       A5             CONSTANT (4)\n"
            + "00010 00010       F3             VAR_WRITE LONG DBASE+$00003 (short)\n"
            + "' }\n"
            + "00011 00011       04             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testGlobalVarAssignment() throws Exception {
        String text = ""
            + "long a = 1;\n"
            + "byte b = 2;\n"
            + "word c = 0, d = 0;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    c = 3;\n"
            + "    d = 4;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       21 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "0000C 0000C       A3             CONSTANT (2)\n"
            + "0000D 0000D       52 08 81       VAR_WRITE BYTE VBASE+$00008\n"
            + "00010 00010       A1             CONSTANT (0)\n"
            + "00011 00011       58 09 81       VAR_WRITE WORD VBASE+$00009\n"
            + "00014 00014       A1             CONSTANT (0)\n"
            + "00015 00015       58 0B 81       VAR_WRITE WORD VBASE+$0000B\n"
            + "'     c = 3;\n"
            + "00018 00018       A4             CONSTANT (3)\n"
            + "00019 00019       58 09 81       VAR_WRITE WORD VBASE+$00009\n"
            + "'     d = 4;\n"
            + "0001C 0001C       A5             CONSTANT (4)\n"
            + "0001D 0001D       58 0B 81       VAR_WRITE WORD VBASE+$0000B\n"
            + "' }\n"
            + "00020 00020       04             RETURN\n"
            + "00021 00021       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testExpressionAssignment() throws Exception {
        String text = ""
            + "int a, b;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    a = 1 + b * 3;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       12 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 1 + b * 3;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       C2 80          VAR_READ LONG VBASE+$00002 (short)\n"
            + "0000C 0000C       A4             CONSTANT (3)\n"
            + "0000D 0000D       96             MULTIPLY\n"
            + "0000E 0000E       8A             ADD\n"
            + "0000F 0000F       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "' }\n"
            + "00011 00011       04             RETURN\n"
            + "00012 00012       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testMethodCall() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    setup();\n"
            + "}\n"
            + "\n"
            + "void setup() {\n"
            + "    a = 1;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       0C 00 00 80    Method main @ $0000C (0 parameters, 0 returns)\n"
            + "00004 00004       11 00 00 80    Method setup @ $00011 (0 parameters, 0 returns)\n"
            + "00008 00008       16 00 00 00    End\n"
            + "' void main() {\n"
            + "0000C 0000C       00             (stack size)\n"
            + "'     setup();\n"
            + "0000D 0000D       00             ANCHOR\n"
            + "0000E 0000E       0A 01          CALL_SUB (1)\n"
            + "' }\n"
            + "00010 00010       04             RETURN\n"
            + "' void setup() {\n"
            + "00011 00011       00             (stack size)\n"
            + "'     a = 1;\n"
            + "00012 00012       A2             CONSTANT (1)\n"
            + "00013 00013       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "' }\n"
            + "00015 00015       04             RETURN\n"
            + "00016 00016       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testForLoop() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    for(;;) {\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       0C 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     for(;;) {\n"
            + "00009 00009       12 7F          JMP $00009 (-1)\n"
            + "'     }\n"
            + "' }\n"
            + "0000B 0000B       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testForLoopBreakAndContinue() throws Exception {
        String text = ""
            + "int a, b;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    for(;;) {\n"
            + "        if (a == 10) {\n"
            + "            b++;\n"
            + "            continue;\n"
            + "        }\n"
            + "        if (a == 20) {\n"
            + "            b = 1;\n"
            + "            break;\n"
            + "        }\n"
            + "        b += 2;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       25 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     for(;;) {\n"
            + "'         if (a == 10) {\n"
            + "00009 00009       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000B 0000B       AB             CONSTANT (10)\n"
            + "0000C 0000C       70             EQUAL\n"
            + "0000D 0000D       13 05          JZ $00013 (5)\n"
            + "'             b++;\n"
            + "0000F 0000F       C2             VAR_SETUP LONG VBASE+$00002 (short)\n"
            + "00010 00010       83             POST_INC\n"
            + "'             continue;\n"
            + "00011 00011       12 77          JMP $00009 (-9)\n"
            + "'         }\n"
            + "'         if (a == 20) {\n"
            + "00013 00013       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "00015 00015       44 14          CONSTANT (20)\n"
            + "00017 00017       70             EQUAL\n"
            + "00018 00018       13 06          JZ $0001F (6)\n"
            + "'             b = 1;\n"
            + "0001A 0001A       A2             CONSTANT (1)\n"
            + "0001B 0001B       C2 81          VAR_WRITE LONG VBASE+$00002 (short)\n"
            + "'             break;\n"
            + "0001D 0001D       12 06          JMP $00024 (6)\n"
            + "'         }\n"
            + "'         b += 2;\n"
            + "0001F 0001F       A3             CONSTANT (2)\n"
            + "00020 00020       C2             VAR_SETUP LONG VBASE+$00002 (short)\n"
            + "00021 00021       A3             ADD_ASSIGN\n"
            + "00022 00022       12 66          JMP $00009 (-26)\n"
            + "'     }\n"
            + "' }\n"
            + "00024 00024       04             RETURN\n"
            + "00025 00025       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testForLoopTwoArguments() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    for(a = 0; a < 100;) {\n"
            + "        a++;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       18 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     for(a = 0; a < 100;) {\n"
            + "00009 00009       A1             CONSTANT (0)\n"
            + "0000A 0000A       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "0000C 0000C       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000E 0000E       44 64          CONSTANT (100)\n"
            + "00010 00010       6C             LESS_THAN\n"
            + "00011 00011       13 05          JZ $00017 (5)\n"
            + "'         a++;\n"
            + "00013 00013       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "00014 00014       83             POST_INC\n"
            + "00015 00015       12 76          JMP $0000C (-10)\n"
            + "'     }\n"
            + "' }\n"
            + "00017 00017       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testForLoopThreeArguments() throws Exception {
        String text = ""
            + "int a, b;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    for(a = 0; a < 100; a++) {\n"
            + "        b++;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       1A 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     for(a = 0; a < 100; a++) {\n"
            + "00009 00009       A1             CONSTANT (0)\n"
            + "0000A 0000A       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "0000C 0000C       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000E 0000E       44 64          CONSTANT (100)\n"
            + "00010 00010       6C             LESS_THAN\n"
            + "00011 00011       13 07          JZ $00019 (7)\n"
            + "'         b++;\n"
            + "00013 00013       C2             VAR_SETUP LONG VBASE+$00002 (short)\n"
            + "00014 00014       83             POST_INC\n"
            + "00015 00015       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "00016 00016       83             POST_INC\n"
            + "00017 00017       12 74          JMP $0000C (-12)\n"
            + "'     }\n"
            + "' }\n"
            + "00019 00019       04             RETURN\n"
            + "0001A 0001A       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testForLoopMultipleInits() throws Exception {
        String text = ""
            + "int a, b;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    for(a = 0, b = 1; a < 100; a++) {\n"
            + "        b++;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       1D 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     for(a = 0, b = 1; a < 100; a++) {\n"
            + "00009 00009       A1             CONSTANT (0)\n"
            + "0000A 0000A       C1 81          VAR_WRITE LONG VBASE+$00001 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       C2 81          VAR_WRITE LONG VBASE+$00002 (short)\n"
            + "0000F 0000F       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "00011 00011       44 64          CONSTANT (100)\n"
            + "00013 00013       6C             LESS_THAN\n"
            + "00014 00014       13 07          JZ $0001C (7)\n"
            + "'         b++;\n"
            + "00016 00016       C2             VAR_SETUP LONG VBASE+$00002 (short)\n"
            + "00017 00017       83             POST_INC\n"
            + "00018 00018       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "00019 00019       83             POST_INC\n"
            + "0001A 0001A       12 74          JMP $0000F (-12)\n"
            + "'     }\n"
            + "' }\n"
            + "0001C 0001C       04             RETURN\n"
            + "0001D 0001D       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testWhileLoop() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    while(a < 100) {\n"
            + "        a++;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       15 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     while(a < 100) {\n"
            + "00009 00009       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000B 0000B       44 64          CONSTANT (100)\n"
            + "0000D 0000D       6C             LESS_THAN\n"
            + "0000E 0000E       13 05          JZ $00014 (5)\n"
            + "'         a++;\n"
            + "00010 00010       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "00011 00011       83             POST_INC\n"
            + "00012 00012       12 76          JMP $00009 (-10)\n"
            + "'     }\n"
            + "' }\n"
            + "00014 00014       04             RETURN\n"
            + "00015 00015       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testWhileLoopBreakAndContinue() throws Exception {
        String text = ""
            + "int a, b;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    while(a < 100) {\n"
            + "        if (a == 10) {\n"
            + "            b++;\n"
            + "            continue;\n"
            + "        }\n"
            + "        if (a == 20) {\n"
            + "            b = 1;\n"
            + "            break;\n"
            + "        }\n"
            + "        a++;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       2B 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     while(a < 100) {\n"
            + "00009 00009       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000B 0000B       44 64          CONSTANT (100)\n"
            + "0000D 0000D       6C             LESS_THAN\n"
            + "0000E 0000E       13 1B          JZ $0002A (27)\n"
            + "'         if (a == 10) {\n"
            + "00010 00010       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "00012 00012       AB             CONSTANT (10)\n"
            + "00013 00013       70             EQUAL\n"
            + "00014 00014       13 05          JZ $0001A (5)\n"
            + "'             b++;\n"
            + "00016 00016       C2             VAR_SETUP LONG VBASE+$00002 (short)\n"
            + "00017 00017       83             POST_INC\n"
            + "'             continue;\n"
            + "00018 00018       12 70          JMP $00009 (-16)\n"
            + "'         }\n"
            + "'         if (a == 20) {\n"
            + "0001A 0001A       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0001C 0001C       44 14          CONSTANT (20)\n"
            + "0001E 0001E       70             EQUAL\n"
            + "0001F 0001F       13 06          JZ $00026 (6)\n"
            + "'             b = 1;\n"
            + "00021 00021       A2             CONSTANT (1)\n"
            + "00022 00022       C2 81          VAR_WRITE LONG VBASE+$00002 (short)\n"
            + "'             break;\n"
            + "00024 00024       12 05          JMP $0002A (5)\n"
            + "'         }\n"
            + "'         a++;\n"
            + "00026 00026       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "00027 00027       83             POST_INC\n"
            + "00028 00028       12 60          JMP $00009 (-32)\n"
            + "'     }\n"
            + "' }\n"
            + "0002A 0002A       04             RETURN\n"
            + "0002B 0002B       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testDoWhileLoop() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    do {\n"
            + "        a++;\n"
            + "    } while(a < 100);\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       13 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     do {\n"
            + "'         a++;\n"
            + "00009 00009       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "0000A 0000A       83             POST_INC\n"
            + "'     } while(a < 100);\n"
            + "0000B 0000B       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000D 0000D       44 64          CONSTANT (100)\n"
            + "0000F 0000F       6C             LESS_THAN\n"
            + "00010 00010       14 78          JNZ $00009 (-8)\n"
            + "' }\n"
            + "00012 00012       04             RETURN\n"
            + "00013 00013       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testDoUntilLoop() throws Exception {
        String text = ""
            + "int a;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "    do {\n"
            + "        a++;\n"
            + "    } until(a > 100);\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       13 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     do {\n"
            + "'         a++;\n"
            + "00009 00009       C1             VAR_SETUP LONG VBASE+$00001 (short)\n"
            + "0000A 0000A       83             POST_INC\n"
            + "'     } until(a > 100);\n"
            + "0000B 0000B       C1 80          VAR_READ LONG VBASE+$00001 (short)\n"
            + "0000D 0000D       44 64          CONSTANT (100)\n"
            + "0000F 0000F       74             GREATER_THAN\n"
            + "00010 00010       13 78          JZ $00009 (-8)\n"
            + "' }\n"
            + "00012 00012       04             RETURN\n"
            + "00013 00013       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testConstants() throws Exception {
        String text = ""
            + "#define _CLKFREQ 160_000_000\n"
            + "\n"
            + "void main() {\n"
            + "    int a;\n"
            + "\n"
            + "    a = _CLKFREQ;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       10 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     a = _CLKFREQ;\n"
            + "00009 00009       48 00 68 89 09 CONSTANT (160_000_000)\n"
            + "0000E 0000E       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "' }\n"
            + "0000F 0000F       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testIf() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "\n"
            + "    if (a == 0) {\n"
            + "        a = 1;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       11 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     if (a == 0) {\n"
            + "00009 00009       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000A 0000A       A1             CONSTANT (0)\n"
            + "0000B 0000B       70             EQUAL\n"
            + "0000C 0000C       13 03          JZ $00010 (3)\n"
            + "'         a = 1;\n"
            + "0000E 0000E       A2             CONSTANT (1)\n"
            + "0000F 0000F       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00010 00010       04             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testIfElse() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "\n"
            + "    if (a == 0) {\n"
            + "        a = 1;\n"
            + "    }\n"
            + "    else {\n"
            + "        a = 2;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       15 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     if (a == 0) {\n"
            + "00009 00009       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000A 0000A       A1             CONSTANT (0)\n"
            + "0000B 0000B       70             EQUAL\n"
            + "0000C 0000C       13 05          JZ $00012 (5)\n"
            + "'         a = 1;\n"
            + "0000E 0000E       A2             CONSTANT (1)\n"
            + "0000F 0000F       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00010 00010       12 03          JMP $00014 (3)\n"
            + "'     }\n"
            + "'     else {\n"
            + "'         a = 2;\n"
            + "00012 00012       A3             CONSTANT (2)\n"
            + "00013 00013       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00014 00014       04             RETURN\n"
            + "00015 00015       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testIfElseIf() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "\n"
            + "    if (a == 0) {\n"
            + "        a = 1;\n"
            + "    }\n"
            + "    else if (a == 1) {\n"
            + "        a = 2;\n"
            + "    }\n"
            + "    else if (a == 2) {\n"
            + "        a = 3;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       23 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     if (a == 0) {\n"
            + "00009 00009       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000A 0000A       A1             CONSTANT (0)\n"
            + "0000B 0000B       70             EQUAL\n"
            + "0000C 0000C       13 05          JZ $00012 (5)\n"
            + "'         a = 1;\n"
            + "0000E 0000E       A2             CONSTANT (1)\n"
            + "0000F 0000F       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00010 00010       12 11          JMP $00022 (17)\n"
            + "'     }\n"
            + "'     else if (a == 1) {\n"
            + "00012 00012       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "00013 00013       A2             CONSTANT (1)\n"
            + "00014 00014       70             EQUAL\n"
            + "00015 00015       13 05          JZ $0001B (5)\n"
            + "'         a = 2;\n"
            + "00017 00017       A3             CONSTANT (2)\n"
            + "00018 00018       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00019 00019       12 08          JMP $00022 (8)\n"
            + "'     }\n"
            + "'     else if (a == 2) {\n"
            + "0001B 0001B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0001C 0001C       A3             CONSTANT (2)\n"
            + "0001D 0001D       70             EQUAL\n"
            + "0001E 0001E       13 03          JZ $00022 (3)\n"
            + "'         a = 3;\n"
            + "00020 00020       A4             CONSTANT (3)\n"
            + "00021 00021       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00022 00022       04             RETURN\n"
            + "00023 00023       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testIfElseIfElse() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "\n"
            + "    if (a == 0) {\n"
            + "        a = 1;\n"
            + "    }\n"
            + "    else if (a == 1) {\n"
            + "        a = 2;\n"
            + "    }\n"
            + "    else if (a == 2) {\n"
            + "        a = 3;\n"
            + "    }\n"
            + "    else {\n"
            + "        a = 4;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       27 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     if (a == 0) {\n"
            + "00009 00009       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000A 0000A       A1             CONSTANT (0)\n"
            + "0000B 0000B       70             EQUAL\n"
            + "0000C 0000C       13 05          JZ $00012 (5)\n"
            + "'         a = 1;\n"
            + "0000E 0000E       A2             CONSTANT (1)\n"
            + "0000F 0000F       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00010 00010       12 15          JMP $00026 (21)\n"
            + "'     }\n"
            + "'     else if (a == 1) {\n"
            + "00012 00012       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "00013 00013       A2             CONSTANT (1)\n"
            + "00014 00014       70             EQUAL\n"
            + "00015 00015       13 05          JZ $0001B (5)\n"
            + "'         a = 2;\n"
            + "00017 00017       A3             CONSTANT (2)\n"
            + "00018 00018       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00019 00019       12 0C          JMP $00026 (12)\n"
            + "'     }\n"
            + "'     else if (a == 2) {\n"
            + "0001B 0001B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0001C 0001C       A3             CONSTANT (2)\n"
            + "0001D 0001D       70             EQUAL\n"
            + "0001E 0001E       13 05          JZ $00024 (5)\n"
            + "'         a = 3;\n"
            + "00020 00020       A4             CONSTANT (3)\n"
            + "00021 00021       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00022 00022       12 03          JMP $00026 (3)\n"
            + "'     }\n"
            + "'     else {\n"
            + "'         a = 4;\n"
            + "00024 00024       A5             CONSTANT (4)\n"
            + "00025 00025       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00026 00026       04             RETURN\n"
            + "00027 00027       00             Padding\n"
            + "", compile(text));
    }

    @Test
    void testInlinePAsmBlock() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    asm {\n"
            + "        nop\n"
            + "        ret\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       18 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     asm {\n"
            + "00009 00009       19 5E          INLINE-EXEC\n"
            + "0000B 0000B       00 00 01 00    ORG=$000, 2\n"
            + "0000F 0000F   000 00 00 00 00                        nop\n"
            + "00013 00013   001 2D 00 64 FD                        ret\n"
            + "'     }\n"
            + "' }\n"
            + "00017 00017       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testDebug() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    debug();\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       0D 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     debug();\n"
            + "00009 00009       43 00 01       DEBUG #1\n"
            + "' }\n"
            + "0000C 0000C       04             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
            + "' Debug data\n"
            + "00B24 00000       06 00         \n"
            + "00B26 00002       04 00         \n"
            + "00B28 00004       04 00         \n"
            + "", compile(text, true));
    }

    @Test
    void testSwitch() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "    switch(a) {\n"
            + "        case 1:\n"
            + "            a = 4;\n"
            + "            break;\n"
            + "        case 2:\n"
            + "            a = 5;\n"
            + "            break;\n"
            + "        case 3:\n"
            + "            a = 6;\n"
            + "            break;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       20 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     switch(a) {\n"
            + "00009 00009       44 1F          ADDRESS ($0001F)\n"
            + "0000B 0000B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       1C 08          CASE_JMP $00016 (8)\n"
            + "0000F 0000F       A3             CONSTANT (2)\n"
            + "00010 00010       1C 08          CASE_JMP $00019 (8)\n"
            + "00012 00012       A4             CONSTANT (3)\n"
            + "00013 00013       1C 08          CASE_JMP $0001C (8)\n"
            + "00015 00015       1E             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00016 00016       A5             CONSTANT (4)\n"
            + "00017 00017       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00018 00018       1E             CASE_DONE\n"
            + "'             a = 5;\n"
            + "00019 00019       A6             CONSTANT (5)\n"
            + "0001A 0001A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001B 0001B       1E             CASE_DONE\n"
            + "'             a = 6;\n"
            + "0001C 0001C       A7             CONSTANT (6)\n"
            + "0001D 0001D       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001E 0001E       1E             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001F 0001F       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testSwitchDefault() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "    switch(a) {\n"
            + "        case 1:\n"
            + "            a = 4;\n"
            + "            break;\n"
            + "        case 2:\n"
            + "            a = 5;\n"
            + "            break;\n"
            + "        default:\n"
            + "            a = 6;\n"
            + "            break;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       1C 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     switch(a) {\n"
            + "00009 00009       44 1B          ADDRESS ($0001B)\n"
            + "0000B 0000B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       1C 07          CASE_JMP $00015 (7)\n"
            + "0000F 0000F       A3             CONSTANT (2)\n"
            + "00010 00010       1C 07          CASE_JMP $00018 (7)\n"
            + "'             a = 6;\n"
            + "00012 00012       A7             CONSTANT (6)\n"
            + "00013 00013       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00014 00014       1E             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00015 00015       A5             CONSTANT (4)\n"
            + "00016 00016       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00017 00017       1E             CASE_DONE\n"
            + "'             a = 5;\n"
            + "00018 00018       A6             CONSTANT (5)\n"
            + "00019 00019       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001A 0001A       1E             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001B 0001B       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testSwitchCaseWithoutBreak() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "    switch(a) {\n"
            + "        case 1:\n"
            + "            a = 4;\n"
            + "        case 2:\n"
            + "            a = 5;\n"
            + "        case 3:\n"
            + "            a = 6;\n"
            + "            break;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       1E 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     switch(a) {\n"
            + "00009 00009       44 1D          ADDRESS ($0001D)\n"
            + "0000B 0000B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       1C 08          CASE_JMP $00016 (8)\n"
            + "0000F 0000F       A3             CONSTANT (2)\n"
            + "00010 00010       1C 07          CASE_JMP $00018 (7)\n"
            + "00012 00012       A4             CONSTANT (3)\n"
            + "00013 00013       1C 06          CASE_JMP $0001A (6)\n"
            + "00015 00015       1E             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00016 00016       A5             CONSTANT (4)\n"
            + "00017 00017       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'             a = 5;\n"
            + "00018 00018       A6             CONSTANT (5)\n"
            + "00019 00019       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'             a = 6;\n"
            + "0001A 0001A       A7             CONSTANT (6)\n"
            + "0001B 0001B       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001C 0001C       1E             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001D 0001D       04             RETURN\n"
            + "0001E 0001E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testSwitchCaseWithoutStatements() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "    switch(a) {\n"
            + "        case 1:\n"
            + "        case 2:\n"
            + "            a = 5;\n"
            + "            break;\n"
            + "        case 3:\n"
            + "            a = 6;\n"
            + "            break;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       1D 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     switch(a) {\n"
            + "00009 00009       44 1C          ADDRESS ($0001C)\n"
            + "0000B 0000B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       1C 08          CASE_JMP $00016 (8)\n"
            + "0000F 0000F       A3             CONSTANT (2)\n"
            + "00010 00010       1C 05          CASE_JMP $00016 (5)\n"
            + "00012 00012       A4             CONSTANT (3)\n"
            + "00013 00013       1C 05          CASE_JMP $00019 (5)\n"
            + "00015 00015       1E             CASE_DONE\n"
            + "'             a = 5;\n"
            + "00016 00016       A6             CONSTANT (5)\n"
            + "00017 00017       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00018 00018       1E             CASE_DONE\n"
            + "'             a = 6;\n"
            + "00019 00019       A7             CONSTANT (6)\n"
            + "0001A 0001A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001B 0001B       1E             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001C 0001C       04             RETURN\n"
            + "0001D 0001D       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testSwitchCaseWithSpinSyntax() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "    int a;\n"
            + "    switch(a) {\n"
            + "        case 1, 2:\n"
            + "            a = 5;\n"
            + "            break;\n"
            + "        case 3..5,7,8:\n"
            + "            a = 6;\n"
            + "            break;\n"
            + "    }\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 80    Method main @ $00008 (0 parameters, 0 returns)\n"
            + "00004 00004       24 00 00 00    End\n"
            + "' void main() {\n"
            + "00008 00008       01             (stack size)\n"
            + "'     switch(a) {\n"
            + "00009 00009       44 23          ADDRESS ($00023)\n"
            + "0000B 0000B       E0             VAR_READ LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       A2             CONSTANT (1)\n"
            + "0000D 0000D       1C 0F          CASE_JMP $0001D (15)\n"
            + "0000F 0000F       A3             CONSTANT (2)\n"
            + "00010 00010       1C 0C          CASE_JMP $0001D (12)\n"
            + "00012 00012       A4             CONSTANT (3)\n"
            + "00013 00013       A6             CONSTANT (5)\n"
            + "00014 00014       1D 0B          CASE_RANGE_JMP $00020 (11)\n"
            + "00016 00016       A8             CONSTANT (7)\n"
            + "00017 00017       1C 08          CASE_JMP $00020 (8)\n"
            + "00019 00019       A9             CONSTANT (8)\n"
            + "0001A 0001A       1C 05          CASE_JMP $00020 (5)\n"
            + "0001C 0001C       1E             CASE_DONE\n"
            + "'             a = 5;\n"
            + "0001D 0001D       A6             CONSTANT (5)\n"
            + "0001E 0001E       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "0001F 0001F       1E             CASE_DONE\n"
            + "'             a = 6;\n"
            + "00020 00020       A7             CONSTANT (6)\n"
            + "00021 00021       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "00022 00022       1E             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "00023 00023       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testPreprocessorNotDefined() throws Exception {
        String text = ""
            + "void main(int a)\n"
            + "{\n"
            + "#ifdef TEST\n"
            + "    a = 1;\n"
            + "#endif\n"
            + "    a++;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0C 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a++;\n"
            + "00009 00009       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000A 0000A       83             POST_INC\n"
            + "' }\n"
            + "0000B 0000B       04             RETURN\n"
            + "", compile(text));
    }

    @Test
    void testPreprocessorDefined() throws Exception {
        String text = ""
            + "#define TEST\n"
            + "\n"
            + "void main(int a)\n"
            + "{\n"
            + "#ifdef TEST\n"
            + "    a = 1;\n"
            + "#endif\n"
            + "    a++;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0E 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       83             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       04             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testPreprocessorNotDefinedElse() throws Exception {
        String text = ""
            + "void main(int a)\n"
            + "{\n"
            + "#ifdef TEST\n"
            + "    a = 1;\n"
            + "#else\n"
            + "    a = 2;\n"
            + "#endif\n"
            + "    a++;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0E 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 2;\n"
            + "00009 00009       A3             CONSTANT (2)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       83             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       04             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testNestedPreprocessorNotDefinedElse1() throws Exception {
        String text = ""
            + "void main(int a)\n"
            + "{\n"
            + "#ifndef P2\n"
            + "  #ifdef TEST\n"
            + "    a = 1;\n"
            + "  #else\n"
            + "    a = 2;\n"
            + "  #endif\n"
            + "    a++;\n"
            + "#endif\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0E 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 2;\n"
            + "00009 00009       A3             CONSTANT (2)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       83             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       04             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testNestedPreprocessorNotDefinedElse2() throws Exception {
        String text = ""
            + "#define TEST\n"
            + "\n"
            + "void main(int a)\n"
            + "{\n"
            + "#ifndef P2\n"
            + "  #ifdef TEST\n"
            + "    a = 1;\n"
            + "  #else\n"
            + "    a = 2;\n"
            + "  #endif\n"
            + "    a++;\n"
            + "#endif\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0E 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       83             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       04             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    @Test
    void testPreprocessorIf() throws Exception {
        String text = ""
            + "#define TEST 1\n"
            + "\n"
            + "void main(int a)\n"
            + "{\n"
            + "#if defined(__P2__)\n"
            + "  #if TEST\n"
            + "    a = 1;\n"
            + "  #else\n"
            + "    a = 2;\n"
            + "  #endif\n"
            + "    a++;\n"
            + "#endif\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header\n"
            + "00000 00000       08 00 00 81    Method main @ $00008 (1 parameters, 0 returns)\n"
            + "00004 00004       0E 00 00 00    End\n"
            + "' void main(int a) {\n"
            + "00008 00008       00             (stack size)\n"
            + "'     a = 1;\n"
            + "00009 00009       A2             CONSTANT (1)\n"
            + "0000A 0000A       F0             VAR_WRITE LONG DBASE+$00000 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       D0             VAR_SETUP LONG DBASE+$00000 (short)\n"
            + "0000C 0000C       83             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       04             RETURN\n"
            + "0000E 0000E       00 00          Padding\n"
            + "", compile(text));
    }

    String compile(String text) throws Exception {
        return compile(text, false);
    }

    String compile(String text, boolean debugEnabled) throws Exception {
        CTokenStream stream = new CTokenStream(text);
        CParser subject = new CParser(stream);
        Node root = subject.parse();

        Spin2CObjectCompiler compiler = new Spin2CObjectCompiler(new Spin2CCompiler(), new ArrayList<>());
        compiler.debugEnabled = debugEnabled;
        Spin2Object obj = compiler.compileObject(root);
        if (debugEnabled) {
            obj.setDebugData(compiler.generateDebugData());
            obj.setDebugger(new Spin2Debugger());
        }

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