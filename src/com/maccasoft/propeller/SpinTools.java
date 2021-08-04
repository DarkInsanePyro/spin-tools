/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.maccasoft.propeller.internal.ImageRegistry;
import com.maccasoft.propeller.internal.TempDirectory;
import com.maccasoft.propeller.spin1.Spin1Object;
import com.maccasoft.propeller.spin1.Spin1ObjectCompiler;
import com.maccasoft.propeller.spin2.Spin2Object;

import jssc.SerialPort;
import jssc.SerialPortException;

public class SpinTools {

    public static final String APP_TITLE = "Spin Tools";
    public static final String APP_VERSION = "0.0.1";

    Shell shell;
    CTabFolder tabFolder;
    StatusLine statusLine;

    SerialPortList serialPortList;

    Preferences preferences;

    final CaretListener caretListener = new CaretListener() {

        @Override
        public void caretMoved(CaretEvent event) {
            updateCaretPosition();
        }

    };

    public SpinTools(Shell shell) {
        this.shell = shell;
        this.shell.setData(this);

        Menu menu = new Menu(shell, SWT.BAR);
        createFileMenu(menu);
        createEditMenu(menu);
        //createSketchMenu(menu);
        createToolsMenu(menu);
        createHelpMenu(menu);
        shell.setMenuBar(menu);

        Composite container = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = 0;
        container.setLayout(layout);

        tabFolder = new CTabFolder(container, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder.setMaximizeVisible(false);
        tabFolder.setMinimizeVisible(false);
        tabFolder.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item != null && e.item.getData() != null) {
                    ((CTabItem) e.item).getControl().setFocus();
                }
                updateCaretPosition();
            }
        });
        createTabFolderMenu();

        tabFolder.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.character != SWT.TAB || (e.stateMask & SWT.MODIFIER_MASK) == 0) {
                    return;
                }
                int index = tabFolder.getSelectionIndex();
                if ((e.stateMask & SWT.SHIFT) != 0) {
                    index--;
                    if (index < 0) {
                        index = tabFolder.getItemCount() - 1;
                    }
                }
                else {
                    index++;
                    if (index >= tabFolder.getItemCount()) {
                        index = 0;
                    }
                }
                tabFolder.setSelection(index);

                EditorTab tab = (EditorTab) tabFolder.getItem(index).getData();
                tab.setFocus();
                updateCaretPosition();

                e.doit = false;
            }
        });
        tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {

            @Override
            public void close(CTabFolderEvent event) {
                EditorTab tab = (EditorTab) event.item.getData();
                event.doit = canCloseEditorTab(tab);
            }
        });

        statusLine = new StatusLine(container);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        layoutData.heightHint = 24;
        statusLine.setLayoutData(layoutData);

        preferences = Preferences.getInstance();

        serialPortList = new SerialPortList();

        String port = preferences.getPort();
        if (port != null) {
            serialPortList.setSelection(port);
            statusLine.setPort(port);
        }

        serialPortList.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String port = (String) evt.getNewValue();
                SerialTerminal serialTerminal = getSerialTerminal();
                if (serialTerminal != null) {
                    if (!port.equals(serialTerminal.getSerialPort().getPortName())) {
                        SerialPort oldSerialPort = serialTerminal.getSerialPort();
                        try {
                            if (oldSerialPort.isOpened()) {
                                oldSerialPort.closePort();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        serialTerminal.setSerialPort(new SerialPort(port));
                    }
                }
                preferences.setPort(port);
                statusLine.setPort(port);
            }

        });

        shell.addListener(SWT.Close, new Listener() {

            @Override
            public void handleEvent(Event event) {
                event.doit = handleUnsavedContent();
            }
        });
        shell.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                try {
                    SerialTerminal serialTerminal = getSerialTerminal();
                    if (serialTerminal != null) {
                        serialTerminal.close();
                    }
                    preferences.save();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    void createFileMenu(Menu parent) {
        Menu menu = new Menu(parent.getParent(), SWT.DROP_DOWN);

        MenuItem item = new MenuItem(parent, SWT.CASCADE);
        item.setText("&File");
        item.setMenu(menu);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("New\tCtrl+N");
        item.setAccelerator(SWT.CTRL + 'N');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileNew();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("New (From P1 template)\tCtrl+Alt+1");
        item.setAccelerator(SWT.CTRL + SWT.ALT + '1');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileNewSpin1();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("New (From P2 template)\tCtrl+Alt+1");
        item.setAccelerator(SWT.CTRL + SWT.ALT + '2');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileNewSpin2();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Open...");
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileOpen();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        final Menu openFromMenu = new Menu(parent.getParent(), SWT.DROP_DOWN);
        openFromMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuShown(MenuEvent e) {
                MenuItem[] item = openFromMenu.getItems();
                for (int i = 0; i < item.length; i++) {
                    item[i].dispose();
                }
                populateOpenFromMenu(openFromMenu);
            }

            @Override
            public void menuHidden(MenuEvent e) {
            }
        });
        item = new MenuItem(menu, SWT.CASCADE);
        item.setText("Open From...");
        item.setMenu(openFromMenu);

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Save\tCtrl+S");
        item.setAccelerator(SWT.MOD1 + 'S');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileSave();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Save As...");
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleFileSaveAs();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Preferences");
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        final int lruIndex = menu.getItemCount();

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Exit");
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    shell.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        menu.addMenuListener(new MenuListener() {

            List<MenuItem> list = new ArrayList<MenuItem>();

            @Override
            public void menuShown(MenuEvent e) {
                for (MenuItem item : list) {
                    item.dispose();
                }
                list.clear();
                populateLruFiles(menu, lruIndex, list);
            }

            @Override
            public void menuHidden(MenuEvent e) {
            }
        });
    }

    void populateOpenFromMenu(Menu menu) {
        List<String> defaultList = Arrays.asList(new String[] {
            new File("examples/P1").getAbsolutePath(),
            new File("examples/P2").getAbsolutePath(),
            new File("library/spin1").getAbsolutePath(),
            new File("library/spin2").getAbsolutePath()
        });
        List<String> list = new ArrayList<String>();

        for (String folder : defaultList) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(folder);
            item.addListener(SWT.Selection, new Listener() {

                @Override
                public void handleEvent(Event event) {
                    try {
                        handleFileOpenFrom(folder);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        Iterator<String> iter = Preferences.getInstance().getLru().iterator();
        while (iter.hasNext()) {
            boolean addSeparator = true;

            String folder = new File(iter.next()).getParent();
            if (!list.contains(folder) && !defaultList.contains(folder)) {
                if (addSeparator) {
                    new MenuItem(menu, SWT.SEPARATOR);
                    addSeparator = false;
                }
                MenuItem item = new MenuItem(menu, SWT.PUSH);
                item.setText(folder);
                item.addListener(SWT.Selection, new Listener() {

                    @Override
                    public void handleEvent(Event event) {
                        try {
                            handleFileOpenFrom(folder);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                list.add(folder);
            }
        }
    }

    void populateLruFiles(Menu menu, int itemIndex, List<MenuItem> list) {
        int index = 0;

        Iterator<String> iter = Preferences.getInstance().getLru().iterator();
        while (iter.hasNext()) {
            final File fileToOpen = new File(iter.next());
            MenuItem item = new MenuItem(menu, SWT.PUSH, itemIndex++);
            item.setText(String.format("%d %s", index + 1, fileToOpen.getName()));
            item.setToolTipText(fileToOpen.getAbsolutePath());
            item.addListener(SWT.Selection, new Listener() {

                @Override
                public void handleEvent(Event event) {
                    try {
                        if (!fileToOpen.exists()) {
                            MessageDialog.openError(shell, APP_TITLE, "File " + fileToOpen + " not found");
                            Preferences.getInstance().getLru().remove(fileToOpen.toString());

                            File parentFIle = fileToOpen.getParentFile();
                            while (parentFIle != null) {
                                if (parentFIle.exists()) {
                                    break;
                                }
                                parentFIle = parentFIle.getParentFile();
                            }
                            handleFileOpenFrom(parentFIle != null ? parentFIle.getAbsolutePath() : "");
                            return;
                        }

                        EditorTab editorTab = new EditorTab(tabFolder, fileToOpen.getName());
                        tabFolder.setSelection(tabFolder.getItemCount() - 1);
                        editorTab.setFocus();
                        preferences.addToLru(fileToOpen);

                        editorTab.addCaretListener(caretListener);

                        tabFolder.getDisplay().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    editorTab.setEditorText(loadFromFile(fileToOpen));
                                    editorTab.setFile(fileToOpen);
                                    updateCaretPosition();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            list.add(item);
            index++;
        }

        if (index > 0) {
            list.add(new MenuItem(menu, SWT.SEPARATOR, itemIndex));
        }
    }

    private void handleFileNew() {
        String suffix = ".spin";

        if (tabFolder.getSelection() != null) {
            EditorTab currentTab = (EditorTab) tabFolder.getSelection().getData();
            String tabName = currentTab.getText();
            suffix = tabName.substring(tabName.lastIndexOf('.'));
        }

        String name = getUniqueName("Untitled", suffix);
        EditorTab editorTab = new EditorTab(tabFolder, name);
        tabFolder.setSelection(tabFolder.getItemCount() - 1);
        editorTab.setFocus();
        editorTab.addCaretListener(caretListener);
        updateCaretPosition();
    }

    private void handleFileNewSpin1() {
        String name = getUniqueName("Untitled", ".spin");
        EditorTab editorTab = new EditorTab(tabFolder, name);
        editorTab.setEditorText(getResourceAsString("template.spin"));
        tabFolder.setSelection(tabFolder.getItemCount() - 1);
        editorTab.setFocus();
        editorTab.addCaretListener(caretListener);
        updateCaretPosition();
    }

    private void handleFileNewSpin2() {
        String name = getUniqueName("Untitled", ".spin2");
        EditorTab editorTab = new EditorTab(tabFolder, name);
        editorTab.setEditorText(getResourceAsString("template.spin2"));
        tabFolder.setSelection(tabFolder.getItemCount() - 1);
        editorTab.setFocus();
        editorTab.addCaretListener(caretListener);
        updateCaretPosition();
    }

    String getUniqueName(String prefix, String suffix) {
        int count = 0;
        String name = prefix + suffix;

        int index = 0;
        while (index < tabFolder.getItemCount()) {
            CTabItem tabItem = tabFolder.getItem(index);
            EditorTab editorTab = (EditorTab) tabItem.getData();
            if (editorTab.getText().equalsIgnoreCase(name)) {
                name = prefix + String.valueOf(++count) + suffix;
                index = -1;
            }
            index++;
        }

        return name;
    }

    String getResourceAsString(String name) {
        InputStream is = getClass().getResourceAsStream(name);
        try {
            byte[] b = new byte[is.available()];
            is.read(b);
            return new String(b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    private void handleFileOpen() {
        FileDialog dlg = new FileDialog(shell, SWT.OPEN);
        dlg.setText("Open Spin File");
        String[] filterNames = new String[] {
            "Spin Files"
        };
        String[] filterExtensions = new String[] {
            "*.spin;*.spin2"
        };
        dlg.setFilterNames(filterNames);
        dlg.setFilterExtensions(filterExtensions);

        File filterPath = null;

        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem != null) {
            EditorTab editorTab = (EditorTab) tabItem.getData();
            filterPath = editorTab.getFile();
        }
        if (filterPath == null && preferences.getLru().size() != 0) {
            filterPath = new File(preferences.getLru().get(0));
        }

        if (filterPath != null) {
            dlg.setFilterPath(filterPath.getParent());
        }

        String fileName = dlg.open();
        if (fileName != null) {
            File fileToOpen = new File(fileName);

            EditorTab editorTab = new EditorTab(tabFolder, fileToOpen.getName());

            tabFolder.setSelection(tabFolder.getItemCount() - 1);
            editorTab.setFocus();
            preferences.addToLru(fileToOpen);

            editorTab.addCaretListener(caretListener);

            tabFolder.getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        editorTab.setEditorText(loadFromFile(fileToOpen));
                        editorTab.setFile(fileToOpen);
                        updateCaretPosition();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
        }
    }

    private void handleFileOpenFrom(String filterPath) {
        FileDialog dlg = new FileDialog(shell, SWT.OPEN);
        dlg.setText("Open Spin File");
        String[] filterNames = new String[] {
            "Spin Files"
        };
        String[] filterExtensions = new String[] {
            "*.spin;*.spin2"
        };
        dlg.setFilterNames(filterNames);
        dlg.setFilterExtensions(filterExtensions);

        if (filterPath != null) {
            dlg.setFilterPath(filterPath);
        }

        String fileName = dlg.open();
        if (fileName != null) {
            File fileToOpen = new File(fileName);

            EditorTab editorTab = new EditorTab(tabFolder, fileToOpen.getName());

            tabFolder.setSelection(tabFolder.getItemCount() - 1);
            editorTab.setFocus();
            preferences.addToLru(fileToOpen);

            editorTab.addCaretListener(caretListener);

            tabFolder.getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        editorTab.setEditorText(loadFromFile(fileToOpen));
                        editorTab.setFile(fileToOpen);
                        updateCaretPosition();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
        }
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

    private void handleFileSave() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem != null) {
            EditorTab editorTab = (EditorTab) tabItem.getData();
            doFileSave(editorTab);
        }
    }

    private void doFileSave(EditorTab editorTab) {
        File fileToSave = editorTab.getFile();
        if (fileToSave == null) {
            doFileSaveAs(editorTab);
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave));
            writer.write(editorTab.getEditorText());
            writer.close();
            editorTab.clearDirty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFileSaveAs() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem != null) {
            EditorTab editorTab = (EditorTab) tabItem.getData();
            doFileSaveAs(editorTab);
        }
    }

    private void doFileSaveAs(EditorTab editorTab) {
        FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        dlg.setText("Save Spin File");
        String[] filterNames = new String[] {
            "Spin Files"
        };
        String[] filterExtensions = new String[] {
            "*.spin;*.spin2"
        };
        dlg.setFilterNames(filterNames);
        dlg.setFilterExtensions(filterExtensions);

        dlg.setFileName(editorTab.getText());

        File filterPath = editorTab.getFile();
        if (filterPath == null && preferences.getLru().size() != 0) {
            filterPath = new File(preferences.getLru().get(0));
        }
        if (filterPath != null) {
            dlg.setFilterPath(filterPath.getParent());
        }

        String fileName = dlg.open();
        if (fileName != null) {
            File fileToSave = new File(fileName);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave));
                writer.write(editorTab.getEditorText());
                writer.close();
                editorTab.setFile(fileToSave);
                editorTab.setText(fileToSave.getName());
                editorTab.clearDirty();
                preferences.addToLru(fileToSave);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Menu createEditMenu(Menu parent) {
        Menu menu = new Menu(parent.getParent(), SWT.DROP_DOWN);

        MenuItem item = new MenuItem(parent, SWT.CASCADE);
        item.setText("&Edit");
        item.setMenu(menu);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Undo\tCtrl+Z");
        item.setAccelerator(SWT.MOD1 + 'Z');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.undo();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Redo\tCtrl+Shift+Z");
        item.setAccelerator(SWT.MOD1 + SWT.MOD2 + 'Z');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.redo();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Cut\tCtrl+X");
        item.setAccelerator(SWT.MOD1 + 'X');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.cut();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy\tCtrl+C");
        item.setAccelerator(SWT.MOD1 + 'C');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.copy();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Paste\tCtrl+V");
        item.setAccelerator(SWT.MOD1 + 'V');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.paste();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All\tCtrl+A");
        item.setAccelerator(SWT.MOD1 + 'A');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.selectAll();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Find / Replace...\tCtrl+F");
        item.setAccelerator(SWT.MOD1 + 'F');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Find Next\tCtrl+K");
        item.setAccelerator(SWT.MOD1 + 'K');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Previous Annotation\tCtrl+,");
        item.setAccelerator(SWT.MOD1 + ',');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    if (tabItem == null) {
                        return;
                    }
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.goToPreviousError();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Next Annotation\tCtrl+.");
        item.setAccelerator(SWT.MOD1 + '.');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    CTabItem tabItem = tabFolder.getSelection();
                    if (tabItem == null) {
                        return;
                    }
                    EditorTab editorTab = (EditorTab) tabItem.getData();
                    editorTab.goToNextError();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return menu;
    }

    Menu createToolsMenu(Menu parent) {
        Menu menu = new Menu(parent.getParent(), SWT.DROP_DOWN);

        MenuItem item = new MenuItem(parent, SWT.CASCADE);
        item.setText("&Tools");
        item.setMenu(menu);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Show Info\tF8");
        item.setAccelerator(SWT.F8);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleCompile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Upload to RAM\tF9");
        item.setAccelerator(SWT.F9);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    handleCompileAndUpload();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Upload to Flash\tF10");
        item.setAccelerator(SWT.F10);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Serial Terminal\tCtrl+T");
        item.setAccelerator(SWT.MOD1 + 'T');
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {
                    SerialTerminal serialTerminal = getSerialTerminal();
                    if (serialTerminal == null) {
                        serialTerminal = new SerialTerminal(new SerialPort(serialPortList.getSelection()));
                        serialTerminal.open();
                    }
                    serialTerminal.getControl().setFocus();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        createPortMenu(menu);

        return menu;
    }

    void createPortMenu(Menu parent) {
        final Menu menu = new Menu(parent.getParent(), SWT.DROP_DOWN);
        menu.addMenuListener(new MenuListener() {

            @Override
            public void menuShown(MenuEvent e) {
                serialPortList.fillMenu(menu);
            }

            @Override
            public void menuHidden(MenuEvent e) {

            }
        });

        MenuItem item = new MenuItem(parent, SWT.CASCADE);
        item.setText("&Port");
        item.setMenu(menu);
    }

    private void handleCompile() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        handleInternalCompile();
    }

    private void handleInternalCompile() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();

        if (editorTab.hasErrors()) {
            editorTab.goToFirstError();
            MessageDialog.open(MessageDialog.INFORMATION, shell, APP_TITLE, "Editor has errors, fix all errors before opening the information dialog.", SWT.NONE);
            return;
        }

        Object object = editorTab.getObject();
        if (object instanceof Spin1Object) {
            MemoryDialog dlg = new MemoryDialog(shell) {

                @Override
                protected void doSaveBinary() {
                    try {
                        handleBinaryExport(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void doSaveListing() {
                    try {
                        handleListingExport(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            dlg.setObject((Spin1Object) object);
            dlg.open();
        }
        else if (object instanceof Spin2Object) {
            MemoryDialog2 dlg = new MemoryDialog2(shell) {

                @Override
                protected void doSaveBinary() {
                    try {
                        handleBinaryExport(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void doSaveListing() {
                    try {
                        handleListingExport(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            dlg.setObject((Spin2Object) object);
            dlg.open();
        }
    }

    private void handleBinaryExport(SpinObject object) {
        FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        dlg.setOverwrite(true);
        dlg.setText("Save Binary File");
        String[] filterNames = new String[] {
            "Binary Files"
        };
        String[] filterExtensions = new String[] {
            "*.bin;*.binary"
        };
        dlg.setFilterNames(filterNames);
        dlg.setFilterExtensions(filterExtensions);

        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();

        String name = editorTab.getText();
        int i = name.lastIndexOf('.');
        dlg.setFileName(name.substring(0, i) + ".binary");

        File filterPath = editorTab.getFile();
        if (filterPath == null && preferences.getLru().size() != 0) {
            filterPath = new File(preferences.getLru().get(0));
        }
        if (filterPath != null) {
            dlg.setFilterPath(filterPath.getParent());
        }

        String fileName = dlg.open();
        if (fileName != null) {
            File fileToSave = new File(fileName);
            try {
                FileOutputStream os = new FileOutputStream(fileToSave);
                object.generateBinary(os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleListingExport(SpinObject object) {
        FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        dlg.setOverwrite(true);
        dlg.setText("Save Listing File");
        String[] filterNames = new String[] {
            "Listing Files"
        };
        String[] filterExtensions = new String[] {
            "*.lst"
        };
        dlg.setFilterNames(filterNames);
        dlg.setFilterExtensions(filterExtensions);

        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();

        String name = editorTab.getText();
        int i = name.lastIndexOf('.');
        dlg.setFileName(name.substring(0, i) + ".lst");

        File filterPath = editorTab.getFile();
        if (filterPath == null && preferences.getLru().size() != 0) {
            filterPath = new File(preferences.getLru().get(0));
        }
        if (filterPath != null) {
            dlg.setFilterPath(filterPath.getParent());
        }

        String fileName = dlg.open();
        if (fileName != null) {
            File fileToSave = new File(fileName);
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(fileToSave));
                object.generateListing(ps);
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCompileAndUpload() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();
        if (editorTab.getText().toLowerCase().endsWith(".spin2")) {
            handleInternalCompileAndUpload();
        }
        else {
            handleSpin1InternalCompileAndUpload();
        }
    }

    private void handleSpin1InternalCompileAndUpload() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();
        if (editorTab.hasErrors()) {
            editorTab.goToFirstError();
            MessageDialog.open(MessageDialog.INFORMATION, shell, APP_TITLE, "Editor has errors, fix all errors before upload.", SWT.NONE);
            return;
        }

        Spin1Object obj = (Spin1Object) editorTab.getObject();
        SerialTerminal serialTerminal = getSerialTerminal();

        IRunnableWithProgress thread = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Upload", IProgressMonitor.UNKNOWN);

                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    obj.generateBinary(os);

                    SerialPort serialPort = null;
                    boolean shared = false;

                    if (serialTerminal != null) {
                        SerialPort terminalPort = serialTerminal.getSerialPort();
                        if (terminalPort.getPortName().equals(serialPortList.getSelection())) {
                            Display.getDefault().syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    serialTerminal.setSerialPort(null);
                                }
                            });
                            serialPort = terminalPort;
                            shared = true;
                        }
                    }
                    if (serialPort == null) {
                        serialPort = new SerialPort(serialPortList.getSelection());
                    }

                    PropellerLoader loader = new PropellerLoader(serialPort, shared) {

                        @Override
                        protected void bufferUpload(int type, byte[] binaryImage, String text) throws SerialPortException, IOException {
                            monitor.setTaskName("Loading " + text + " to RAM");
                            super.bufferUpload(type, binaryImage, text);
                        }

                        @Override
                        protected void notifyProgress(int sent, int total) {
                            if (sent == total) {
                                monitor.subTask(String.format("%d bytes sent", total));
                            }
                            else {
                                monitor.subTask(String.format("%d bytes remaining", total - sent));
                            }
                        }

                        @Override
                        protected void verifyRam() throws SerialPortException, IOException {
                            monitor.setTaskName("Verifying RAM ... ");
                            super.verifyRam();
                        }

                        @Override
                        protected void eepromWrite() throws SerialPortException, IOException {
                            monitor.setTaskName("Writing EEPROM ... ");
                            super.eepromWrite();
                        }

                        @Override
                        protected void eepromVerify() throws SerialPortException, IOException {
                            monitor.setTaskName("Verifying EEPROM ... ");
                            super.eepromVerify();
                        }

                    };

                    byte[] image = os.toByteArray();

                    byte sum = 0;
                    for (int i = 0; i < image.length; i++) {
                        sum += image[i];
                    }
                    image[5] = (byte) (0x14 - sum);

                    loader.upload(image, PropellerLoader.DOWNLOAD_RUN_BINARY);

                    if (shared) {
                        SerialPort terminalPort = serialPort;
                        Display.getDefault().syncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (serialTerminal != null) {
                                    serialTerminal.setSerialPort(terminalPort);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                monitor.done();
            }

        };

        ProgressMonitorDialog dlg = new ProgressMonitorDialog(shell);
        try {
            dlg.run(true, true, thread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleInternalCompileAndUpload() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();
        if (editorTab.hasErrors()) {
            editorTab.goToFirstError();
            MessageDialog.open(MessageDialog.INFORMATION, shell, APP_TITLE, "Editor has errors, fix all errors before upload.", SWT.NONE);
            return;
        }

        Spin2Object obj = (Spin2Object) editorTab.getObject();
        SerialTerminal serialTerminal = getSerialTerminal();

        IRunnableWithProgress thread = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Upload", IProgressMonitor.UNKNOWN);

                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    obj.generateBinary(os);

                    SerialPort serialPort = null;
                    boolean shared = false;

                    if (serialTerminal != null) {
                        SerialPort terminalPort = serialTerminal.getSerialPort();
                        if (terminalPort.getPortName().equals(serialPortList.getSelection())) {
                            Display.getDefault().syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    serialTerminal.setSerialPort(null);
                                }
                            });
                            serialPort = terminalPort;
                            shared = true;
                        }
                    }
                    if (serialPort == null) {
                        serialPort = new SerialPort(serialPortList.getSelection());
                    }

                    Propeller2Loader loader = new Propeller2Loader(serialPort, shared) {

                        @Override
                        protected void bufferUpload(int type, byte[] binaryImage, String text) throws SerialPortException, IOException {
                            monitor.setTaskName("Loading " + text + " to RAM");
                            super.bufferUpload(type, binaryImage, text);
                        }

                        @Override
                        protected void notifyProgress(int sent, int total) {
                            if (sent == total) {
                                monitor.subTask(String.format("%d bytes sent", total));
                            }
                            else {
                                monitor.subTask(String.format("%d bytes remaining", total - sent));
                            }
                        }

                        @Override
                        protected void verifyRam() throws SerialPortException, IOException {
                            monitor.setTaskName("Verifying RAM ... ");
                            super.verifyRam();
                        }

                    };
                    loader.upload(os.toByteArray(), 0);

                    if (shared) {
                        SerialPort terminalPort = serialPort;
                        Display.getDefault().syncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (serialTerminal != null) {
                                    serialTerminal.setSerialPort(terminalPort);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                monitor.done();
            }

        };

        ProgressMonitorDialog dlg = new ProgressMonitorDialog(shell);
        try {
            dlg.run(true, true, thread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createHelpMenu(Menu parent) {
        Menu menu = new Menu(parent.getParent(), SWT.DROP_DOWN);

        MenuItem item = new MenuItem(parent, SWT.CASCADE);
        item.setText("&Help");
        item.setMenu(menu);

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("About " + APP_TITLE);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                AboutDialog dlg = new AboutDialog(shell);
                dlg.open();
            }
        });
    }

    void createTabFolderMenu() {
        final ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT);

        final Menu menu = new Menu(toolBar);

        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Next Tab\tCtrl+Tab");
        item.setAccelerator(SWT.MOD1 + SWT.TAB);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Previous Tab\tCtrl+Shift+Tab");
        item.setAccelerator(SWT.MOD1 + SWT.MOD2 + SWT.TAB);
        item.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event e) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        final int entriesTokeep = menu.getItemCount();

        final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);
        toolItem.setImage(ImageRegistry.getImageFromResources("vertical-dots.png"));
        toolItem.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                while (menu.getItemCount() > entriesTokeep) {
                    menu.getItem(menu.getItemCount() - 1).dispose();
                }
                if (tabFolder.getItemCount() != 0) {
                    new MenuItem(menu, SWT.SEPARATOR);
                }
                for (CTabItem tabItem : tabFolder.getItems()) {
                    MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
                    menuItem.setText(((EditorTab) tabItem.getData()).getText());
                    menuItem.setSelection(tabFolder.getSelection() == tabItem);
                    menuItem.addListener(SWT.Selection, new Listener() {

                        @Override
                        public void handleEvent(Event e) {
                            tabFolder.setSelection(tabItem);
                            tabItem.getControl().setFocus();
                        }
                    });
                }

                Rectangle rect = toolItem.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = toolBar.toDisplay(pt);
                menu.setLocation(pt.x, pt.y);
                menu.setVisible(true);
            }
        });

        tabFolder.setTopRight(toolBar);
    }

    boolean canCloseEditorTab(EditorTab editorTab) {
        if (editorTab.isDirty()) {
            int style = SWT.APPLICATION_MODAL | SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL;
            MessageBox messageBox = new MessageBox(shell, style);
            messageBox.setText(APP_TITLE);
            messageBox.setMessage("Editor contains unsaved changes.  Save before close?");
            switch (messageBox.open()) {
                case SWT.CANCEL:
                    return false;
                case SWT.YES:
                    try {
                        doFileSave(editorTab);
                        if (editorTab.isDirty()) {
                            return false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    boolean handleUnsavedContent() {
        boolean dirty = false;

        for (CTabItem tabItem : tabFolder.getItems()) {
            EditorTab editorTab = (EditorTab) tabItem.getData();
            if (editorTab.isDirty()) {
                dirty = true;
                break;
            }
        }

        if (dirty) {
            int style = SWT.APPLICATION_MODAL | SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL;
            MessageBox messageBox = new MessageBox(shell, style);
            messageBox.setText(APP_TITLE);
            messageBox.setMessage("Editor contains unsaved changes.  Save before exit?");
            switch (messageBox.open()) {
                case SWT.CANCEL:
                    return false;
                case SWT.YES:
                    try {
                        for (CTabItem tabItem : tabFolder.getItems()) {
                            EditorTab editorTab = (EditorTab) tabItem.getData();
                            if (editorTab.isDirty()) {
                                doFileSave(editorTab);
                                if (editorTab.isDirty()) {
                                    return false;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
            }
        }

        return true;
    }

    SerialTerminal getSerialTerminal() {
        Shell[] shells = Display.getDefault().getShells();
        for (int i = 0; i < shells.length; i++) {
            if (shells[i].getData() instanceof SerialTerminal) {
                return (SerialTerminal) shells[i].getData();
            }
        }
        return null;
    }

    void updateCaretPosition() {
        CTabItem tabItem = tabFolder.getSelection();
        if (tabItem == null) {
            return;
        }
        EditorTab editorTab = (EditorTab) tabItem.getData();

        StyledText styledText = editorTab.getEditor().getStyledText();
        if (styledText != null) {
            int offset = styledText.getCaretOffset();
            int y = styledText.getLineAtOffset(offset);
            int x = offset - styledText.getOffsetAtLine(y);
            statusLine.setCaretPositionText(String.format("%d : %d : %d", y + 1, x + 1, offset));
        }
        else {
            statusLine.setCaretPositionText("");
        }
    }

    static {
        Display.setAppName(APP_TITLE);
        Display.setAppVersion(APP_VERSION);
        Spin1ObjectCompiler.OPENSPIN_COMPATIBILITY = true;
    }

    public static void main(String[] args) {
        final Display display = new Display();

        display.setErrorHandler(new Consumer<Error>() {

            @Override
            public void accept(Error t) {
                t.printStackTrace();
            }

        });
        display.setRuntimeExceptionHandler(new Consumer<RuntimeException>() {

            @Override
            public void accept(RuntimeException t) {
                t.printStackTrace();
            }

        });

        Realm.runWithDefault(DisplayRealm.getRealm(display), new Runnable() {

            @Override
            public void run() {
                try {
                    Shell shell = new Shell(display);
                    shell.setText("Spin Tools");

                    Rectangle screen = display.getClientArea();

                    Rectangle rect = new Rectangle(0, 0, 800, 800);
                    rect.x = (screen.width - rect.width) / 2;
                    rect.y = (screen.height - rect.height) / 2;
                    if (rect.y < 0) {
                        rect.height += rect.y * 2;
                        rect.y = 0;
                    }

                    shell.setLocation(rect.x, rect.y);
                    shell.setSize(rect.width, rect.height);

                    FillLayout layout = new FillLayout();
                    layout.marginWidth = layout.marginHeight = 5;
                    shell.setLayout(layout);

                    new SpinTools(shell);

                    shell.open();

                    while (display.getShells().length != 0) {
                        if (!display.readAndDispatch()) {
                            display.sleep();
                        }
                    }

                    TempDirectory.clean();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        display.dispose();
    }

}
