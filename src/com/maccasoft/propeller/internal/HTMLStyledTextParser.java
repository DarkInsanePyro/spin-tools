/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;

public class HTMLStyledTextParser extends ParserCallback {

    private final List<StyleRange> listOfStyles;
    private final StyledText styledText;
    private final StringBuilder outputString;
    private StyleRange currentStyleRange;
    private TagType currentTagType;
    private int currentPosition;
    private final HTMLEditorKit.Parser parser;

    private enum TagType {
        B, U, I
    };

    public HTMLStyledTextParser(final StyledText styledText) {
        super();
        this.parser = new ParserDelegator();
        this.styledText = styledText;
        this.listOfStyles = new ArrayList<StyleRange>();
        this.outputString = new StringBuilder();
    }

    @Override
    public void handleStartTag(final Tag t, final MutableAttributeSet a, final int pos) {
        if (t == Tag.B) {
            currentStyleRange = new StyleRange();
            currentTagType = TagType.B;
            currentPosition = outputString.length();
        }
        if (t == Tag.I) {
            currentStyleRange = new StyleRange();
            currentTagType = TagType.I;
            currentPosition = outputString.length();
        }
        if (t == Tag.U) {
            currentStyleRange = new StyleRange();
            currentTagType = TagType.U;
            currentPosition = outputString.length();
        }
        if (t == Tag.P) {
            outputString.append("\n\n");
        }
    }

    @Override
    public void handleEndTag(final Tag t, final int pos) {
        if (t != Tag.B && t != Tag.I && t != Tag.U && t != Tag.PRE) {
            return;
        }
        int style = SWT.NORMAL;
        boolean underline = false;
        Font font = null;
        if (t == Tag.B) {
            if (TagType.B != this.currentTagType) {
                throw new RuntimeException("Error parsing [" + this.styledText.getText() + "] : bad syntax");
            }
            style = SWT.BOLD;
        }
        else if (t == Tag.I) {
            if (TagType.I != this.currentTagType) {
                throw new RuntimeException("Error parsing [" + this.styledText.getText() + "] : bad syntax");
            }
            style = SWT.ITALIC;
        }
        else if (t == Tag.U) {
            if (TagType.U != this.currentTagType) {
                throw new RuntimeException("Error parsing [" + this.styledText.getText() + "] : bad syntax");
            }
            style = SWT.NORMAL;
        }
        if (currentStyleRange != null) {
            currentStyleRange.start = currentPosition;
            currentStyleRange.length = outputString.length() - currentPosition;
            currentStyleRange.fontStyle = style;
            currentStyleRange.underline = underline;
            currentStyleRange.font = font;
            listOfStyles.add(this.currentStyleRange);
            currentStyleRange = null;
            currentTagType = null;
        }
    }

    @Override
    public void handleError(final String errorMsg, final int pos) {

    }

    @Override
    public void handleText(final char[] data, final int pos) {
        this.outputString.append(data);
    }

    @Override
    public void handleSimpleTag(final Tag t, final MutableAttributeSet a, final int pos) {
        if (t == Tag.BR) {
            outputString.append("\n");
        }
    }

    public void setText(String text) {
        listOfStyles.clear();
        currentStyleRange = null;
        currentTagType = null;
        currentPosition = 0;
        outputString.setLength(0);
        try {
            parser.parse(new StringReader(text), this, true);
            styledText.setText(outputString.toString());
            styledText.setStyleRanges(listOfStyles.toArray(new StyleRange[listOfStyles.size()]));
        } catch (Exception e) {
            e.printStackTrace();
            styledText.setText(text);
        }
    }
}