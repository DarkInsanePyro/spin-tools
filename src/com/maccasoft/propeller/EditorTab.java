/*
 * Copyright (c) 2021-22 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package com.maccasoft.propeller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.maccasoft.propeller.SourceTokenMarker.TokenMarker;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.spin1.Spin1Compiler;
import com.maccasoft.propeller.spin1.Spin1Parser;
import com.maccasoft.propeller.spin1.Spin1TokenMarker;
import com.maccasoft.propeller.spin1.Spin1TokenStream;
import com.maccasoft.propeller.spin2.Spin2Compiler;
import com.maccasoft.propeller.spin2.Spin2Parser;
import com.maccasoft.propeller.spin2.Spin2TokenMarker;
import com.maccasoft.propeller.spin2.Spin2TokenStream;

public class EditorTab implements FindReplaceTarget {

    SourcePool sourcePool;

    File file;
    SourceEditor editor;
    CTabItem tabItem;

    Font busyFont;

    SourceTokenMarker tokenMarker;

    String tabItemText;
    boolean dirty;

    AtomicBoolean threadRunning = new AtomicBoolean(false);
    AtomicBoolean pendingCompile = new AtomicBoolean(false);

    Set<String> dependencies = new HashSet<String>();

    boolean errors;
    List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
    Object object;

    final PropertyChangeListener settingsChangeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {

        }
    };

    final PropertyChangeListener sourcePoolChangeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SourcePool.PROP_DEBUG_ENABLED)) {
                scheduleCompile();
                return;
            }
            if (evt.getPropertyName().equals(tabItemText)) {
                return;
            }
            if (!dependencies.contains(evt.getPropertyName())) {
                return;
            }
            if (tabItemText.toLowerCase().endsWith(".spin2")) {
                Display.getDefault().timerExec(500, spin2CompilerRunnable);
            }
            else {
                Display.getDefault().timerExec(500, spin1CompilerRunnable);
            }
            tabItem.setFont(busyFont);
        }

    };

    class Spin1TokenMarkerAdatper extends Spin1TokenMarker {

        @Override
        protected Node getObjectTree(String fileName) {
            File localFile = new File(file != null ? file.getParentFile() : new File(""), fileName + ".spin");
            File libraryFile = new File(Preferences.getInstance().getSpin1LibraryPath(), fileName + ".spin");

            Node node = sourcePool.getParsedSource(localFile.getAbsolutePath());
            if (node == null) {
                node = sourcePool.getParsedSource(libraryFile.getAbsolutePath());
            }
            if (node == null) {
                File file = localFile;
                if (!file.exists()) {
                    file = libraryFile;
                }
                if (file.exists()) {
                    try {
                        Spin1TokenStream stream = new Spin1TokenStream(loadFromFile(file));
                        Spin1Parser subject = new Spin1Parser(stream);
                        node = subject.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return node;
        }

    }

    class Spin1CompilerAdapter extends Spin1Compiler {

        @Override
        protected Node getParsedObject(String fileName) {
            File localFile = new File(file != null ? file.getParentFile() : new File(""), fileName);
            File libraryFile = new File(Preferences.getInstance().getSpin1LibraryPath(), fileName);

            File file = localFile;
            Node node = sourcePool.getParsedSource(file.getAbsolutePath());
            if (node == null) {
                file = libraryFile;
                node = sourcePool.getParsedSource(file.getAbsolutePath());
            }
            if (node == null) {
                file = localFile;
                if (!file.exists()) {
                    file = libraryFile;
                }
                if (file.exists()) {
                    try {
                        Spin1TokenStream stream = new Spin1TokenStream(loadFromFile(file));
                        Spin1Parser subject = new Spin1Parser(stream);
                        node = subject.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            dependencies.add(node != null ? file.getAbsolutePath() : fileName);

            return node;
        }

        @Override
        protected byte[] getBinaryFile(String fileName) {
            File fileParent = file != null ? file.getParentFile() : null;

            try {
                File fileToLoad = new File(fileParent, fileName);
                if (!fileToLoad.exists()) {
                    fileToLoad = new File(Preferences.getInstance().getSpin2LibraryPath(), fileName);
                }
                InputStream is = new FileInputStream(fileToLoad);
                try {
                    byte[] b = new byte[is.available()];
                    is.read(b);
                    return b;
                } finally {
                    try {
                        is.close();
                    } catch (Exception e) {

                    }
                }
            } catch (Exception e) {
                // Do nothing
            }

            return null;
        }

    }

    final Runnable spin1CompilerRunnable = new Runnable() {

        @Override
        public void run() {
            if (editor.getControl().isDisposed()) {
                return;
            }
            if (!threadRunning.getAndSet(true)) {
                pendingCompile.set(false);

                Spin1TokenStream stream = new Spin1TokenStream(editor.getStyledText().getText());
                Spin1Parser parser = new Spin1Parser(stream);
                Node root = parser.parse();

                File localFile = file != null ? file : new File(tabItemText);
                sourcePool.setParsedSource(localFile.getAbsolutePath(), root);

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        dependencies.clear();

                        Spin1Compiler compiler = new Spin1CompilerAdapter();
                        compiler.setRemoveUnusedMethods(true);
                        try {
                            object = compiler.compile(tabItemText, root);
                            errors = compiler.hasErrors();
                        } catch (Exception e) {
                            errors = true;
                            e.printStackTrace();
                        }

                        if (!pendingCompile.get()) {
                            messages.clear();
                            messages.addAll(compiler.getMessages());

                            List<CompilerMessage> list = new ArrayList<CompilerMessage>();
                            for (CompilerMessage msg : messages) {
                                if (tabItemText.equals(msg.fileName)) {
                                    list.add(msg);
                                }
                            }

                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    if (editor == null || editor.getStyledText().isDisposed()) {
                                        return;
                                    }
                                    editor.setCompilerMessages(list);
                                    editor.redraw();
                                    tabItem.setFont(null);
                                    updateTabItemText();
                                }
                            });
                        }
                        threadRunning.set(false);
                    }

                });
                thread.start();
            }
            else {
                pendingCompile.set(true);
                Display.getDefault().timerExec(250, spin1CompilerRunnable);
            }
        }

    };

    class Spin2TokenMarkerAdatper extends Spin2TokenMarker {

        @Override
        protected Node getObjectTree(String fileName) {
            File localFile = new File(file != null ? file.getParentFile() : new File(""), fileName + ".spin2");
            File libraryFile = new File(Preferences.getInstance().getSpin2LibraryPath(), fileName + ".spin2");

            Node node = sourcePool.getParsedSource(localFile.getAbsolutePath());
            if (node == null) {
                node = sourcePool.getParsedSource(libraryFile.getAbsolutePath());
            }
            if (node == null) {
                File file = localFile;
                if (!file.exists()) {
                    file = libraryFile;
                }
                if (file.exists()) {
                    try {
                        Spin2TokenStream stream = new Spin2TokenStream(loadFromFile(file));
                        Spin2Parser subject = new Spin2Parser(stream);
                        node = subject.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return node;
        }

    }

    class Spin2CompilerAdapter extends Spin2Compiler {

        @Override
        protected Node getParsedObject(String fileName) {
            File localFile = new File(file != null ? file.getParentFile() : new File(""), fileName);
            File libraryFile = new File(Preferences.getInstance().getSpin2LibraryPath(), fileName);

            File file = localFile;
            Node node = sourcePool.getParsedSource(file.getAbsolutePath());
            if (node == null) {
                file = libraryFile;
                node = sourcePool.getParsedSource(file.getAbsolutePath());
            }
            if (node == null) {
                file = localFile;
                if (!file.exists()) {
                    file = libraryFile;
                }
                if (file.exists()) {
                    try {
                        Spin2TokenStream stream = new Spin2TokenStream(loadFromFile(file));
                        Spin2Parser subject = new Spin2Parser(stream);
                        node = subject.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            dependencies.add(node != null ? file.getAbsolutePath() : fileName);

            return node;
        }

        @Override
        protected byte[] getBinaryFile(String fileName) {
            File fileParent = file != null ? file.getParentFile() : null;

            try {
                File fileToLoad = new File(fileParent, fileName);
                if (!fileToLoad.exists()) {
                    fileToLoad = new File(Preferences.getInstance().getSpin2LibraryPath(), fileName);
                }
                InputStream is = new FileInputStream(fileToLoad);
                try {
                    byte[] b = new byte[is.available()];
                    is.read(b);
                    return b;
                } finally {
                    try {
                        is.close();
                    } catch (Exception e) {

                    }
                }
            } catch (Exception e) {
                // Do nothing
            }

            return null;
        }

    }

    final Runnable spin2CompilerRunnable = new Runnable() {

        @Override
        public void run() {
            if (editor.getControl().isDisposed()) {
                return;
            }
            if (!threadRunning.getAndSet(true)) {
                pendingCompile.set(false);

                Spin2TokenStream stream = new Spin2TokenStream(editor.getStyledText().getText());
                Spin2Parser parser = new Spin2Parser(stream);
                Node root = parser.parse();

                File localFile = file != null ? file : new File(tabItemText);
                sourcePool.setParsedSource(localFile.getAbsolutePath(), root);

                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        dependencies.clear();

                        Spin2Compiler compiler = new Spin2CompilerAdapter();
                        compiler.setRemoveUnusedMethods(true);
                        compiler.setDebugEnabled(sourcePool.isDebugEnabled());
                        try {
                            object = compiler.compile(tabItemText, root);
                            errors = compiler.hasErrors();
                        } catch (Exception e) {
                            errors = true;
                            e.printStackTrace();
                        }

                        if (!pendingCompile.get()) {
                            messages.clear();
                            messages.addAll(compiler.getMessages());

                            List<CompilerMessage> list = new ArrayList<CompilerMessage>();
                            for (CompilerMessage msg : messages) {
                                if (tabItemText.equals(msg.fileName)) {
                                    list.add(msg);
                                }
                            }

                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    if (editor == null || editor.getStyledText().isDisposed()) {
                                        return;
                                    }
                                    editor.setCompilerMessages(list);
                                    editor.redraw();
                                    tabItem.setFont(null);
                                    updateTabItemText();
                                }
                            });
                        }
                        threadRunning.set(false);
                    }

                });
                thread.start();
            }
            else {
                pendingCompile.set(true);
                Display.getDefault().timerExec(250, spin2CompilerRunnable);
            }
        }

    };

    public EditorTab(CTabFolder folder, String name, SourcePool sourcePool) {
        this.tabItemText = name;
        this.sourcePool = sourcePool;

        tabItem = new CTabItem(folder, SWT.NONE);
        tabItem.setShowClose(true);
        tabItem.setText(tabItemText);
        tabItem.setData(this);

        FontData[] fontData = tabItem.getFont().getFontData();
        busyFont = new Font(tabItem.getDisplay(), fontData[0].getName(), fontData[0].getHeight(), SWT.ITALIC);

        editor = new SourceEditor(folder);

        if (tabItemText.toLowerCase().endsWith(".spin2")) {
            editor.setTokenMarker(tokenMarker = new Spin2TokenMarkerAdatper());
            editor.setHelpProvider(new EditorHelp("Spin2Instructions.xml", new File(""), ".spin2"));
        }
        else {
            editor.setTokenMarker(tokenMarker = new Spin1TokenMarkerAdatper());
            editor.setHelpProvider(new EditorHelp("Spin1Instructions.xml", new File(""), ".spin"));
        }

        editor.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                if (!dirty) {
                    dirty = true;
                    updateTabItemText();
                }
                scheduleCompile();
            }
        });

        sourcePool.addPropertyChangeListener(sourcePoolChangeListener);

        tabItem.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                sourcePool.removePropertyChangeListener(sourcePoolChangeListener);
                File localFile = file != null ? file : new File(tabItemText);
                sourcePool.removeParsedSource(localFile.getAbsolutePath(), dirty);
                busyFont.dispose();
            }

        });

        tabItem.setControl(editor.getControl());
    }

    public void addCaretListener(CaretListener listener) {
        editor.getStyledText().addCaretListener(listener);
    }

    public void removeCaretListener(CaretListener listener) {
        editor.getStyledText().removeCaretListener(listener);
    }

    public void addDisposeListener(DisposeListener listener) {
        editor.getControl().addDisposeListener(listener);
    }

    public void removeDisposeListener(DisposeListener listener) {
        editor.getControl().removeDisposeListener(listener);
    }

    void updateTabItemText() {
        if (dirty) {
            tabItem.setText("*" + tabItemText);
        }
        else {
            tabItem.setText(tabItemText);
        }
        tabItem.setSelectionForeground(errors ? Display.getDefault().getSystemColor(SWT.COLOR_RED) : null);
        tabItem.setForeground(errors ? Display.getDefault().getSystemColor(SWT.COLOR_RED) : null);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        File localFile = file != null ? file : new File(tabItemText);
        sourcePool.removeParsedSource(localFile.getAbsolutePath(), dirty);

        this.file = file;
        if (file.getName().toLowerCase().endsWith(".spin2")) {
            editor.setTokenMarker(tokenMarker = new Spin2TokenMarkerAdatper());
            editor.setHelpProvider(new EditorHelp("Spin2Instructions.xml", file.getParentFile(), ".spin2"));
        }
        else {
            editor.setTokenMarker(tokenMarker = new Spin1TokenMarkerAdatper());
            editor.setHelpProvider(new EditorHelp("Spin1Instructions.xml", file.getParentFile(), ".spin"));
        }

        tabItem.setToolTipText(file != null ? file.getAbsolutePath() : "");
    }

    public void setFocus() {
        editor.getControl().setFocus();
    }

    public void setText(String text) {
        tabItemText = text;
        updateTabItemText();
    }

    public String getText() {
        return tabItemText;
    }

    public void dispose() {
        editor.getControl().dispose();
        tabItem.dispose();
    }

    public CTabItem getTabItem() {
        return tabItem;
    }

    public void cut() {
        editor.cut();
    }

    public void copy() {
        editor.copy();
    }

    public void paste() {
        editor.paste();
    }

    public void selectAll() {
        editor.selectAll();
    }

    public void undo() {
        editor.undo();
    }

    public void redo() {
        editor.redo();
    }

    public void setEditorText(String text) {
        editor.setText(text);
        if (tabItemText.toLowerCase().endsWith(".spin2")) {
            Display.getDefault().timerExec(1000, spin2CompilerRunnable);
        }
        else {
            Display.getDefault().timerExec(1000, spin1CompilerRunnable);
        }
        tabItem.setFont(busyFont);
    }

    public String getEditorText() {
        return editor.getText();
    }

    public void clearDirty() {
        if (dirty) {
            dirty = false;
            updateTabItemText();
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean hasErrors() {
        return errors;
    }

    public List<CompilerMessage> getMessages() {
        return messages;
    }

    public Object getObject() {
        return object;
    }

    public void goToFirstError() {
        Iterator<TokenMarker> iter = tokenMarker.getCompilerTokens().iterator();
        if (iter.hasNext()) {
            TokenMarker marker = iter.next();
            int markerLine = editor.getStyledText().getLineAtOffset(marker.start);
            editor.goToLineColumn(markerLine, marker.start - editor.getStyledText().getOffsetAtLine(markerLine));
        }
    }

    public void goToNextError() {
        int offset = editor.getStyledText().getCaretOffset();

        Iterator<TokenMarker> iter = tokenMarker.getCompilerTokens().iterator();
        while (iter.hasNext()) {
            TokenMarker marker = iter.next();
            if (marker.start > offset) {
                int markerLine = editor.getStyledText().getLineAtOffset(marker.start);
                editor.goToLineColumn(markerLine, marker.start - editor.getStyledText().getOffsetAtLine(markerLine));
                return;
            }
        }

        iter = tokenMarker.getCompilerTokens().iterator();
        if (iter.hasNext()) {
            TokenMarker marker = iter.next();
            int markerLine = editor.getStyledText().getLineAtOffset(marker.start);
            editor.goToLineColumn(markerLine, marker.start - editor.getStyledText().getOffsetAtLine(markerLine));
        }

        Display.getDefault().beep();
    }

    public void goToPreviousError() {
        int offset = editor.getStyledText().getCaretOffset();

        Iterator<TokenMarker> iter = tokenMarker.getCompilerTokens().descendingIterator();
        while (iter.hasNext()) {
            TokenMarker marker = iter.next();
            if (marker.start < offset) {
                int markerLine = editor.getStyledText().getLineAtOffset(marker.start);
                editor.goToLineColumn(markerLine, marker.start - editor.getStyledText().getOffsetAtLine(markerLine));
                return;
            }
        }

        iter = tokenMarker.getCompilerTokens().descendingIterator();
        if (iter.hasNext()) {
            TokenMarker marker = iter.next();
            int markerLine = editor.getStyledText().getLineAtOffset(marker.start);
            editor.goToLineColumn(markerLine, marker.start - editor.getStyledText().getOffsetAtLine(markerLine));
        }

        Display.getDefault().beep();
    }

    String loadFromFile(File file) throws Exception {
        String line;
        StringBuilder sb = new StringBuilder();

        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
        }

        return sb.toString();
    }

    public SourceEditor getEditor() {
        return editor;
    }

    public void addOpenListener(IOpenListener listener) {
        editor.addOpenListener(listener);
        editor.getOutline().addOpenListener(listener);
    }

    public void removeOpenListener(IOpenListener listener) {
        editor.removeOpenListener(listener);
        editor.getOutline().removeOpenListener(listener);
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {
        int patternFlags = 0;
        StyledText styledText = editor.getStyledText();
        String text = styledText.getText();

        if (!regExSearch) {
            findString = asRegPattern(findString);
            if (wholeWord) {
                findString = "\\b" + findString + "\\b";
            }
        }

        if (!caseSensitive) {
            patternFlags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }

        Pattern pattern = Pattern.compile(findString, patternFlags);
        Matcher matcher = pattern.matcher(text);

        if (searchForward) {
            if (widgetOffset == -1) {
                widgetOffset = 0;
            }
            if (matcher.find(widgetOffset)) {
                styledText.setSelectionRange(matcher.start(), matcher.group().length());
                revealCaret(styledText);
                return matcher.start();
            }
        }
        else {
            if (widgetOffset == -1) {
                widgetOffset = text.length();
            }
            boolean found = matcher.find(0);
            int index = -1;
            while (found && matcher.start() + matcher.group().length() <= widgetOffset) {
                index = matcher.start();
                found = matcher.find(index + 1);
            }
            if (index > -1) {
                matcher.find(index);
                styledText.setSelectionRange(matcher.start(), matcher.group().length());
                revealCaret(styledText);
                return index;
            }

        }

        return -1;
    }

    @Override
    public Point getSelection() {
        StyledText styledText = editor.getStyledText();
        return styledText.getSelectionRange();
    }

    @Override
    public String getSelectionText() {
        StyledText styledText = editor.getStyledText();
        return styledText.getSelectionText();
    }

    @Override
    public void replaceSelection(String text) {
        StyledText styledText = editor.getStyledText();
        Point selection = styledText.getSelectionRange();
        if (selection.y != 0) {
            styledText.replaceTextRange(selection.x, selection.y, text);
        }
    }

    void revealCaret(StyledText styledText) {
        Rectangle rect = styledText.getClientArea();
        int offset = styledText.getCaretOffset();
        int topLine = styledText.getLineIndex(0);
        int bottomLine = styledText.getLineIndex(rect.height);
        int pageSize = bottomLine - topLine;
        while (offset < styledText.getOffsetAtLine(topLine)) {
            topLine -= pageSize;
            bottomLine -= pageSize;
        }
        while (offset > styledText.getOffsetAtLine(bottomLine)) {
            topLine += pageSize;
            bottomLine += pageSize;
        }

        if (styledText.getLineIndex(0) != topLine) {
            styledText.setTopIndex(topLine);
        }
    }

    private String asRegPattern(String string) {
        StringBuilder out = new StringBuilder(string.length());
        boolean quoting = false;

        for (int i = 0, length = string.length(); i < length; i++) {
            char ch = string.charAt(i);
            if (ch == '\\') {
                if (quoting) {
                    out.append("\\E");
                    quoting = false;
                }
                out.append("\\\\");
                continue;
            }
            if (!quoting) {
                out.append("\\Q");
                quoting = true;
            }
            out.append(ch);
        }
        if (quoting) {
            out.append("\\E");
        }

        return out.toString();
    }

    void searchNext(String findString, boolean caseSensitiveSearch, boolean wrapSearch, boolean wholeWordSearch, boolean regexSearch) {
        Point r = getSelection();
        int findReplacePosition = r.x + r.y;

        int index = findAndSelect(findReplacePosition, findString, true, caseSensitiveSearch, wholeWordSearch, regexSearch);
        if (index == -1) {
            editor.getControl().getDisplay().beep();
            if (wrapSearch) {
                index = findAndSelect(-1, findString, true, caseSensitiveSearch, wholeWordSearch, regexSearch);
            }
        }
    }

    void searchPrevious(String findString, boolean caseSensitiveSearch, boolean wrapSearch, boolean wholeWordSearch, boolean regexSearch) {
        Point r = getSelection();
        int findReplacePosition = r.x + r.y;

        int index = findReplacePosition == 0 ? -1 : findAndSelect(findReplacePosition - 1, findString, false, caseSensitiveSearch, wholeWordSearch, regexSearch);
        if (index == -1) {
            editor.getControl().getDisplay().beep();
            if (wrapSearch) {
                index = findAndSelect(-1, findString, false, caseSensitiveSearch, wholeWordSearch, regexSearch);
            }
        }
    }

    void scheduleCompile() {
        if (tabItemText.toLowerCase().endsWith(".spin2")) {
            Display.getDefault().timerExec(500, spin2CompilerRunnable);
        }
        else {
            Display.getDefault().timerExec(500, spin1CompilerRunnable);
        }
        tabItem.setFont(busyFont);
    }

}
