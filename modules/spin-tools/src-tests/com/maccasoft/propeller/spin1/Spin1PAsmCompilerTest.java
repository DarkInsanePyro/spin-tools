/*
 * Copyright (c) 2023 Marco Maccaferri and others.
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

class Spin1PAsmCompilerTest {

    @Test
    void testCompile() throws Exception {
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
    void testLocalLabels() throws Exception {
        String text = ""
            + "DAT             org     $000\n"
            + "\n"
            + "start\n"
            + "                mov     a, #1\n"
            + "                jmp     #:l2\n"
            + ":l1             add     a, #1\n"
            + "                add     a, #1\n"
            + ":l2             add     a, #1\n"
            + "                jmp     #:l1\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000                start               \n"
            + "00004 00004   000 01 0C FC A0                        mov     a, #1\n"
            + "00008 00008   001 04 00 7C 5C                        jmp     #:l2\n"
            + "0000C 0000C   002 01 0C FC 80    :l1                 add     a, #1\n"
            + "00010 00010   003 01 0C FC 80                        add     a, #1\n"
            + "00014 00014   004 01 0C FC 80    :l2                 add     a, #1\n"
            + "00018 00018   005 02 00 7C 5C                        jmp     #:l1\n"
            + "0001C 0001C   006                a                   res     1\n"
            + "", compile(text));
    }

    @Test
    void testAliasedLabels() throws Exception {
        String text = ""
            + "DAT             org     $000\n"
            + "\n"
            + "start\n"
            + "                mov     a, #1\n"
            + "                jmp     #:l2\n"
            + ":l1\n"
            + "                add     a, #1\n"
            + "                add     a, #1\n"
            + ":l2\n"
            + "                add     a, #1\n"
            + "                jmp     #:l1\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       1C 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000                start               \n"
            + "00004 00004   000 01 0C FC A0                        mov     a, #1\n"
            + "00008 00008   001 04 00 7C 5C                        jmp     #:l2\n"
            + "0000C 0000C   002                :l1                 \n"
            + "0000C 0000C   002 01 0C FC 80                        add     a, #1\n"
            + "00010 00010   003 01 0C FC 80                        add     a, #1\n"
            + "00014 00014   004                :l2                 \n"
            + "00014 00014   004 01 0C FC 80                        add     a, #1\n"
            + "00018 00018   005 02 00 7C 5C                        jmp     #:l1\n"
            + "0001C 0001C   006                a                   res     1\n"
            + "", compile(text));
    }

    @Test
    void testDuplicatedLocalLabels() throws Exception {
        String text = ""
            + "DAT             org     $000\n"
            + "\n"
            + "start\n"
            + "                mov     a, #1\n"
            + "                jmp     #:l2\n"
            + ":l1             add     a, #1\n"
            + "                add     a, #1\n"
            + ":l2             add     a, #1\n"
            + "                jmp     #:l1\n"
            + "\n"
            + "setup\n"
            + "                mov     a, #1\n"
            + "                jmp     #:l2\n"
            + ":l1             add     a, #1\n"
            + "                add     a, #1\n"
            + ":l2             add     a, #1\n"
            + "                jmp     #:l1\n"
            + "\n"
            + "a               res     1\n"
            + "";

        Assertions.assertEquals(""
            + "' Object header (var size 0)\n"
            + "00000 00000       34 00          Object size\n"
            + "00002 00002       01             Method count + 1\n"
            + "00003 00003       00             Object count\n"
            + "00004 00004   000                                    org     $000\n"
            + "00004 00004   000                start               \n"
            + "00004 00004   000 01 18 FC A0                        mov     a, #1\n"
            + "00008 00008   001 04 00 7C 5C                        jmp     #:l2\n"
            + "0000C 0000C   002 01 18 FC 80    :l1                 add     a, #1\n"
            + "00010 00010   003 01 18 FC 80                        add     a, #1\n"
            + "00014 00014   004 01 18 FC 80    :l2                 add     a, #1\n"
            + "00018 00018   005 02 00 7C 5C                        jmp     #:l1\n"
            + "0001C 0001C   006                setup               \n"
            + "0001C 0001C   006 01 18 FC A0                        mov     a, #1\n"
            + "00020 00020   007 0A 00 7C 5C                        jmp     #:l2\n"
            + "00024 00024   008 01 18 FC 80    :l1                 add     a, #1\n"
            + "00028 00028   009 01 18 FC 80                        add     a, #1\n"
            + "0002C 0002C   00A 01 18 FC 80    :l2                 add     a, #1\n"
            + "00030 00030   00B 08 00 7C 5C                        jmp     #:l1\n"
            + "00034 00034   00C                a                   res     1\n"
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

    String compile(String text) throws Exception {
        return compile(text, false);
    }

    String compile(String text, boolean openspinCompatible) throws Exception {
        Spin1TokenStream stream = new Spin1TokenStream(text);
        Spin1Parser subject = new Spin1Parser(stream);
        Node root = subject.parse();

        Spin1ObjectCompiler compiler = new Spin1ObjectCompiler(new Spin1Compiler());
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