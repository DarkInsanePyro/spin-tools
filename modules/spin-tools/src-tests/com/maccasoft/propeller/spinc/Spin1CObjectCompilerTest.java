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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.maccasoft.propeller.CompilerException;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.spin1.Spin1Compiler;
import com.maccasoft.propeller.spin1.Spin1Object;

class Spin1CObjectCompilerTest {

    @Test
    void testEmptyFunction() throws Exception {
        String text = ""
            + "void main()\n"
            + "{\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "' }\n"
            + "00008 00008       32             RETURN\n"
            + "00009 00009       00 00 00       Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "' }\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "' }\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 10 00    Function main @ $0008 (local size 16)\n"
            + "' void main() {\n"
            + "'     int a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     int b = 2, c = 3, d = 4;\n"
            + "0000A 0000A       38 02          CONSTANT (2)\n"
            + "0000C 0000C       69             VAR_WRITE LONG DBASE+$0008 (short)\n"
            + "0000D 0000D       38 03          CONSTANT (3)\n"
            + "0000F 0000F       6D             VAR_WRITE LONG DBASE+$000C (short)\n"
            + "00010 00010       38 04          CONSTANT (4)\n"
            + "00012 00012       71             VAR_WRITE LONG DBASE+$0010 (short)\n"
            + "' }\n"
            + "00013 00013       32             RETURN\n"
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
            + "' Object header (var size 12)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "0000A 0000A       38 02          CONSTANT (2)\n"
            + "0000C 0000C       89 04          VAR_WRITE BYTE VBASE+$0004\n"
            + "0000E 0000E       35             CONSTANT (0)\n"
            + "0000F 0000F       A9 05          VAR_WRITE WORD VBASE+$0005\n"
            + "00011 00011       35             CONSTANT (0)\n"
            + "00012 00012       A9 07          VAR_WRITE WORD VBASE+$0007\n"
            + "'     c = 3;\n"
            + "00014 00014       38 03          CONSTANT (3)\n"
            + "00016 00016       A9 05          VAR_WRITE WORD VBASE+$0005\n"
            + "'     d = 4;\n"
            + "00018 00018       38 04          CONSTANT (4)\n"
            + "0001A 0001A       A9 07          VAR_WRITE WORD VBASE+$0007\n"
            + "' }\n"
            + "0001C 0001C       32             RETURN\n"
            + "0001D 0001D       00 00 00       Padding\n"
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
            + "' Object header (var size 8)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     a = 1 + b * 3;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       44             VAR_READ LONG VBASE+$0004 (short)\n"
            + "0000A 0000A       38 03          CONSTANT (3)\n"
            + "0000C 0000C       F4             MULTIPLY\n"
            + "0000D 0000D       EC             ADD\n"
            + "0000E 0000E       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "' }\n"
            + "0000F 0000F       32             RETURN\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       03             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       0C 00 00 00    Function main @ $000C (local size 0)\n"
            + "00008 00008       10 00 00 00    Function setup @ $0010 (local size 0)\n"
            + "' void main() {\n"
            + "'     setup();\n"
            + "0000C 0000C       01             ANCHOR\n"
            + "0000D 0000D       05 02          CALL_SUB\n"
            + "' }\n"
            + "0000F 0000F       32             RETURN\n"
            + "' void setup() {\n"
            + "'     a = 1;\n"
            + "00010 00010       36             CONSTANT (1)\n"
            + "00011 00011       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "' }\n"
            + "00012 00012       32             RETURN\n"
            + "00013 00013       00             Padding\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     for(;;) {\n"
            + "00008 00008       04 7E          JMP $00008 (-2)\n"
            + "'     }\n"
            + "' }\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
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
            + "' Object header (var size 8)\n"
            + "00000 00000       24 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     for(;;) {\n"
            + "'         if (a == 10) {\n"
            + "00008 00008       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00009 00009       38 0A          CONSTANT (10)\n"
            + "0000B 0000B       FC             TEST_EQUAL\n"
            + "0000C 0000C       0A 04          JZ $00012 (4)\n"
            + "'             b++;\n"
            + "0000E 0000E       46             VAR_MODIFY LONG VBASE+$0004 (short)\n"
            + "0000F 0000F       2E             POST_INC\n"
            + "'             continue;\n"
            + "00010 00010       04 76          JMP $00008 (-10)\n"
            + "'         }\n"
            + "'         if (a == 20) {\n"
            + "00012 00012       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00013 00013       38 14          CONSTANT (20)\n"
            + "00015 00015       FC             TEST_EQUAL\n"
            + "00016 00016       0A 04          JZ $0001C (4)\n"
            + "'             b = 1;\n"
            + "00018 00018       36             CONSTANT (1)\n"
            + "00019 00019       45             VAR_WRITE LONG VBASE+$0004 (short)\n"
            + "'             break;\n"
            + "0001A 0001A       04 06          JMP $00022 (6)\n"
            + "'         }\n"
            + "'         b += 2;\n"
            + "0001C 0001C       38 02          CONSTANT (2)\n"
            + "0001E 0001E       46             VAR_MODIFY LONG VBASE+$0004 (short)\n"
            + "0001F 0001F       4C             ADD\n"
            + "00020 00020       04 66          JMP $00008 (-26)\n"
            + "'     }\n"
            + "' }\n"
            + "00022 00022       32             RETURN\n"
            + "00023 00023       00             Padding\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     for(a = 0; a < 100;) {\n"
            + "00008 00008       35             CONSTANT (0)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       38 64          CONSTANT (100)\n"
            + "0000D 0000D       F9             TEST_BELOW\n"
            + "0000E 0000E       0A 04          JZ $00014 (4)\n"
            + "'         a++;\n"
            + "00010 00010       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00011 00011       2E             POST_INC\n"
            + "00012 00012       04 76          JMP $0000A (-10)\n"
            + "'     }\n"
            + "' }\n"
            + "00014 00014       32             RETURN\n"
            + "00015 00015       00 00 00       Padding\n"
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
            + "' Object header (var size 8)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     for(a = 0; a < 100; a++) {\n"
            + "00008 00008       35             CONSTANT (0)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       38 64          CONSTANT (100)\n"
            + "0000D 0000D       F9             TEST_BELOW\n"
            + "0000E 0000E       0A 06          JZ $00016 (6)\n"
            + "'         b++;\n"
            + "00010 00010       46             VAR_MODIFY LONG VBASE+$0004 (short)\n"
            + "00011 00011       2E             POST_INC\n"
            + "00012 00012       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00013 00013       2E             POST_INC\n"
            + "00014 00014       04 74          JMP $0000A (-12)\n"
            + "'     }\n"
            + "' }\n"
            + "00016 00016       32             RETURN\n"
            + "00017 00017       00             Padding\n"
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
            + "' Object header (var size 8)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     for(a = 0, b = 1; a < 100; a++) {\n"
            + "00008 00008       35             CONSTANT (0)\n"
            + "00009 00009       41             VAR_WRITE LONG VBASE+$0000 (short)\n"
            + "0000A 0000A       36             CONSTANT (1)\n"
            + "0000B 0000B       45             VAR_WRITE LONG VBASE+$0004 (short)\n"
            + "0000C 0000C       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000D 0000D       38 64          CONSTANT (100)\n"
            + "0000F 0000F       F9             TEST_BELOW\n"
            + "00010 00010       0A 06          JZ $00018 (6)\n"
            + "'         b++;\n"
            + "00012 00012       46             VAR_MODIFY LONG VBASE+$0004 (short)\n"
            + "00013 00013       2E             POST_INC\n"
            + "00014 00014       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00015 00015       2E             POST_INC\n"
            + "00016 00016       04 74          JMP $0000C (-12)\n"
            + "'     }\n"
            + "' }\n"
            + "00018 00018       32             RETURN\n"
            + "00019 00019       00 00 00       Padding\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     while(a < 100) {\n"
            + "00008 00008       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00009 00009       38 64          CONSTANT (100)\n"
            + "0000B 0000B       F9             TEST_BELOW\n"
            + "0000C 0000C       0A 04          JZ $00012 (4)\n"
            + "'         a++;\n"
            + "0000E 0000E       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "0000F 0000F       2E             POST_INC\n"
            + "00010 00010       04 76          JMP $00008 (-10)\n"
            + "'     }\n"
            + "' }\n"
            + "00012 00012       32             RETURN\n"
            + "00013 00013       00             Padding\n"
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
            + "' Object header (var size 8)\n"
            + "00000 00000       28 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     while(a < 100) {\n"
            + "00008 00008       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00009 00009       38 64          CONSTANT (100)\n"
            + "0000B 0000B       F9             TEST_BELOW\n"
            + "0000C 0000C       0A 18          JZ $00026 (24)\n"
            + "'         if (a == 10) {\n"
            + "0000E 0000E       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000F 0000F       38 0A          CONSTANT (10)\n"
            + "00011 00011       FC             TEST_EQUAL\n"
            + "00012 00012       0A 04          JZ $00018 (4)\n"
            + "'             b++;\n"
            + "00014 00014       46             VAR_MODIFY LONG VBASE+$0004 (short)\n"
            + "00015 00015       2E             POST_INC\n"
            + "'             continue;\n"
            + "00016 00016       04 70          JMP $00008 (-16)\n"
            + "'         }\n"
            + "'         if (a == 20) {\n"
            + "00018 00018       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "00019 00019       38 14          CONSTANT (20)\n"
            + "0001B 0001B       FC             TEST_EQUAL\n"
            + "0001C 0001C       0A 04          JZ $00022 (4)\n"
            + "'             b = 1;\n"
            + "0001E 0001E       36             CONSTANT (1)\n"
            + "0001F 0001F       45             VAR_WRITE LONG VBASE+$0004 (short)\n"
            + "'             break;\n"
            + "00020 00020       04 04          JMP $00026 (4)\n"
            + "'         }\n"
            + "'         a++;\n"
            + "00022 00022       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00023 00023       2E             POST_INC\n"
            + "00024 00024       04 62          JMP $00008 (-30)\n"
            + "'     }\n"
            + "' }\n"
            + "00026 00026       32             RETURN\n"
            + "00027 00027       00             Padding\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     do {\n"
            + "'         a++;\n"
            + "00008 00008       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00009 00009       2E             POST_INC\n"
            + "'     } while(a < 100);\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       38 64          CONSTANT (100)\n"
            + "0000D 0000D       F9             TEST_BELOW\n"
            + "0000E 0000E       0B 78          JNZ $00008 (-8)\n"
            + "' }\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
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
            + "' Object header (var size 4)\n"
            + "00000 00000       14 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main() {\n"
            + "'     do {\n"
            + "'         a++;\n"
            + "00008 00008       42             VAR_MODIFY LONG VBASE+$0000 (short)\n"
            + "00009 00009       2E             POST_INC\n"
            + "'     } until(a > 100);\n"
            + "0000A 0000A       40             VAR_READ LONG VBASE+$0000 (short)\n"
            + "0000B 0000B       38 64          CONSTANT (100)\n"
            + "0000D 0000D       FA             TEST_ABOVE\n"
            + "0000E 0000E       0A 78          JZ $00008 (-8)\n"
            + "' }\n"
            + "00010 00010       32             RETURN\n"
            + "00011 00011       00 00 00       Padding\n"
            + "", compile(text));
    }

    @Test
    void testConstants() throws Exception {
        String text = ""
            + "#define FREQ 160_000_000\n"
            + "\n"
            + "void main() {\n"
            + "    int a;\n"
            + "\n"
            + "    a = FREQ;\n"
            + "}\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     a = FREQ;\n"
            + "00008 00008       3B 09 89 68 00 CONSTANT (160_000_000)\n"
            + "0000D 0000D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "' }\n"
            + "0000E 0000E       32             RETURN\n"
            + "0000F 0000F       00             Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     if (a == 0) {\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       35             CONSTANT (0)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 02          JZ $0000F (2)\n"
            + "'         a = 1;\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "0000F 0000F       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       18 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     if (a == 0) {\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       35             CONSTANT (0)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 04          JZ $00011 (4)\n"
            + "'         a = 1;\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       04 03          JMP $00014 (3)\n"
            + "'     }\n"
            + "'     else {\n"
            + "'         a = 2;\n"
            + "00011 00011       38 02          CONSTANT (2)\n"
            + "00013 00013       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00014 00014       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       28 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     if (a == 0) {\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       35             CONSTANT (0)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 04          JZ $00011 (4)\n"
            + "'         a = 1;\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       04 13          JMP $00024 (19)\n"
            + "'     }\n"
            + "'     else if (a == 1) {\n"
            + "00011 00011       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00012 00012       36             CONSTANT (1)\n"
            + "00013 00013       FC             TEST_EQUAL\n"
            + "00014 00014       0A 05          JZ $0001B (5)\n"
            + "'         a = 2;\n"
            + "00016 00016       38 02          CONSTANT (2)\n"
            + "00018 00018       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00019 00019       04 09          JMP $00024 (9)\n"
            + "'     }\n"
            + "'     else if (a == 2) {\n"
            + "0001B 0001B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0001C 0001C       38 02          CONSTANT (2)\n"
            + "0001E 0001E       FC             TEST_EQUAL\n"
            + "0001F 0001F       0A 03          JZ $00024 (3)\n"
            + "'         a = 3;\n"
            + "00021 00021       38 03          CONSTANT (3)\n"
            + "00023 00023       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00024 00024       32             RETURN\n"
            + "00025 00025       00 00 00       Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       2C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     if (a == 0) {\n"
            + "00008 00008       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00009 00009       35             CONSTANT (0)\n"
            + "0000A 0000A       FC             TEST_EQUAL\n"
            + "0000B 0000B       0A 04          JZ $00011 (4)\n"
            + "'         a = 1;\n"
            + "0000D 0000D       36             CONSTANT (1)\n"
            + "0000E 0000E       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0000F 0000F       04 18          JMP $00029 (24)\n"
            + "'     }\n"
            + "'     else if (a == 1) {\n"
            + "00011 00011       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "00012 00012       36             CONSTANT (1)\n"
            + "00013 00013       FC             TEST_EQUAL\n"
            + "00014 00014       0A 05          JZ $0001B (5)\n"
            + "'         a = 2;\n"
            + "00016 00016       38 02          CONSTANT (2)\n"
            + "00018 00018       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00019 00019       04 0E          JMP $00029 (14)\n"
            + "'     }\n"
            + "'     else if (a == 2) {\n"
            + "0001B 0001B       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0001C 0001C       38 02          CONSTANT (2)\n"
            + "0001E 0001E       FC             TEST_EQUAL\n"
            + "0001F 0001F       0A 05          JZ $00026 (5)\n"
            + "'         a = 3;\n"
            + "00021 00021       38 03          CONSTANT (3)\n"
            + "00023 00023       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00024 00024       04 03          JMP $00029 (3)\n"
            + "'     }\n"
            + "'     else {\n"
            + "'         a = 4;\n"
            + "00026 00026       38 04          CONSTANT (4)\n"
            + "00028 00028       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     }\n"
            + "' }\n"
            + "00029 00029       32             RETURN\n"
            + "0002A 0002A       00 00          Padding\n"
            + "", compile(text));
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
            + "' Object header (var size 0)\n"
            + "00000 00000       24 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     switch(a) {\n"
            + "00008 00008       38 23          ADDRESS ($0023)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 09          CASE-JMP $00017 (9)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 09          CASE-JMP $0001B (9)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       0D 09          CASE-JMP $0001F (9)\n"
            + "00016 00016       0C             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00017 00017       38 04          CONSTANT (4)\n"
            + "00019 00019       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001A 0001A       0C             CASE_DONE\n"
            + "'             a = 5;\n"
            + "0001B 0001B       38 05          CONSTANT (5)\n"
            + "0001D 0001D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001E 0001E       0C             CASE_DONE\n"
            + "'             a = 6;\n"
            + "0001F 0001F       38 06          CONSTANT (6)\n"
            + "00021 00021       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00022 00022       0C             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "00023 00023       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     switch(a) {\n"
            + "00008 00008       38 1E          ADDRESS ($001E)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 08          CASE-JMP $00016 (8)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 08          CASE-JMP $0001A (8)\n"
            + "'             a = 6;\n"
            + "00012 00012       38 06          CONSTANT (6)\n"
            + "00014 00014       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00015 00015       0C             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00016 00016       38 04          CONSTANT (4)\n"
            + "00018 00018       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00019 00019       0C             CASE_DONE\n"
            + "'             a = 5;\n"
            + "0001A 0001A       38 05          CONSTANT (5)\n"
            + "0001C 0001C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001D 0001D       0C             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001E 0001E       32             RETURN\n"
            + "0001F 0001F       00             Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       24 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     switch(a) {\n"
            + "00008 00008       38 21          ADDRESS ($0021)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 09          CASE-JMP $00017 (9)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 08          CASE-JMP $0001A (8)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       0D 07          CASE-JMP $0001D (7)\n"
            + "00016 00016       0C             CASE_DONE\n"
            + "'             a = 4;\n"
            + "00017 00017       38 04          CONSTANT (4)\n"
            + "00019 00019       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'             a = 5;\n"
            + "0001A 0001A       38 05          CONSTANT (5)\n"
            + "0001C 0001C       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'             a = 6;\n"
            + "0001D 0001D       38 06          CONSTANT (6)\n"
            + "0001F 0001F       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00020 00020       0C             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "00021 00021       32             RETURN\n"
            + "00022 00022       00 00          Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       20 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     switch(a) {\n"
            + "00008 00008       38 1F          ADDRESS ($001F)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 09          CASE-JMP $00017 (9)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 05          CASE-JMP $00017 (5)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       0D 05          CASE-JMP $0001B (5)\n"
            + "00016 00016       0C             CASE_DONE\n"
            + "'             a = 5;\n"
            + "00017 00017       38 05          CONSTANT (5)\n"
            + "00019 00019       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001A 0001A       0C             CASE_DONE\n"
            + "'             a = 6;\n"
            + "0001B 0001B       38 06          CONSTANT (6)\n"
            + "0001D 0001D       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "0001E 0001E       0C             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "0001F 0001F       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       2C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 04 00    Function main @ $0008 (local size 4)\n"
            + "' void main() {\n"
            + "'     switch(a) {\n"
            + "00008 00008       38 29          ADDRESS ($0029)\n"
            + "0000A 0000A       64             VAR_READ LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       36             CONSTANT (1)\n"
            + "0000C 0000C       0D 13          CASE-JMP $00021 (19)\n"
            + "0000E 0000E       38 02          CONSTANT (2)\n"
            + "00010 00010       0D 0F          CASE-JMP $00021 (15)\n"
            + "00012 00012       38 03          CONSTANT (3)\n"
            + "00014 00014       38 05          CONSTANT (5)\n"
            + "00016 00016       0E 0D          CASE-RANGE-JMP $00025 (13)\n"
            + "00018 00018       38 07          CONSTANT (7)\n"
            + "0001A 0001A       0D 09          CASE-JMP $00025 (9)\n"
            + "0001C 0001C       38 08          CONSTANT (8)\n"
            + "0001E 0001E       0D 05          CASE-JMP $00025 (5)\n"
            + "00020 00020       0C             CASE_DONE\n"
            + "'             a = 5;\n"
            + "00021 00021       38 05          CONSTANT (5)\n"
            + "00023 00023       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00024 00024       0C             CASE_DONE\n"
            + "'             a = 6;\n"
            + "00025 00025       38 06          CONSTANT (6)\n"
            + "00027 00027       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "00028 00028       0C             CASE_DONE\n"
            + "'     }\n"
            + "' }\n"
            + "00029 00029       32             RETURN\n"
            + "0002A 0002A       00 00          Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       0C 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a++;\n"
            + "00008 00008       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "00009 00009       2E             POST_INC\n"
            + "' }\n"
            + "0000A 0000A       32             RETURN\n"
            + "0000B 0000B       00             Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++;\n"
            + "0000A 0000A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       2E             POST_INC\n"
            + "' }\n"
            + "0000C 0000C       32             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 2;\n"
            + "00008 00008       38 02          CONSTANT (2)\n"
            + "0000A 0000A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       2E             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 2;\n"
            + "00008 00008       38 02          CONSTANT (2)\n"
            + "0000A 0000A       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++;\n"
            + "0000B 0000B       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000C 0000C       2E             POST_INC\n"
            + "' }\n"
            + "0000D 0000D       32             RETURN\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++;\n"
            + "0000A 0000A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       2E             POST_INC\n"
            + "' }\n"
            + "0000C 0000C       32             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
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
            + "' Object header (var size 0)\n"
            + "00000 00000       10 00          Object size\n"
            + "00002 00002       02             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004       08 00 00 00    Function main @ $0008 (local size 0)\n"
            + "' void main(int a) {\n"
            + "'     a = 1;\n"
            + "00008 00008       36             CONSTANT (1)\n"
            + "00009 00009       65             VAR_WRITE LONG DBASE+$0004 (short)\n"
            + "'     a++;\n"
            + "0000A 0000A       66             VAR_MODIFY LONG DBASE+$0004 (short)\n"
            + "0000B 0000B       2E             POST_INC\n"
            + "' }\n"
            + "0000C 0000C       32             RETURN\n"
            + "0000D 0000D       00 00 00       Padding\n"
            + "", compile(text));
    }

    String compile(String text) throws Exception {
        CTokenStream stream = new CTokenStream(text);
        CParser subject = new CParser(stream);
        Node root = subject.parse();

        Spin1CObjectCompiler compiler = new Spin1CObjectCompiler(new Spin1Compiler());
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
