/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SearchPattern;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Browse dialog for configuration file.
 */
public abstract class AbstractBrowseDialog extends TitleAreaDialog {

    protected static final String VAR_DATA = "varData";

    protected static final String SHAREDCONFIGVAR = ConfigVarsUtils.getVarRef(ConfigVars.SHARED_CONFIG_DIR);
    protected static final String SHAREDAPPVAR = ConfigVarsUtils.getVarRef(ConfigVars.SHARED_APP_DIR);
    protected static final String SHAREDRESVAR = ConfigVarsUtils.getVarRef(ConfigVars.SHARED_RESOURCE_DIR);

    protected static final int PATH_MIN_LEN = 10;

    protected Document document = null;
    protected URI documentURI = null;
    protected WebSphereServerInfo server = null;
    protected UserDirectory userDir = null;
    protected VariableEntry[] cachedVariables = null;
    protected VariableEntry selectedVar = null;
    protected Text entryText = null;
    protected Text locationText = null;
    protected Text pathText = null;
    protected String selectedPath = null;
    protected boolean isOK = false;
    protected boolean updating = false;
    protected String currentText = null;
    protected boolean directoriesOnly = false;
    protected ISharedImages sharedImages;
    protected FileComparator fileComparator = new FileComparator();
    protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
    protected ConfigVars configVars = null;

    //    protected TreeItem[] currentComboList = null;

    public AbstractBrowseDialog(Shell parent, Document document, IEditorInput editorInput) {
        super(parent);
        this.document = document;
        this.documentURI = ConfigUIUtils.getURI(editorInput);
        WebSphereServerInfo server = ConfigUtils.getServerInfo(documentURI);
        if (server != null) {
            this.server = server;
            this.userDir = server.getUserDirectory();
        } else {
            IFile inputFile = null;
            if (editorInput instanceof IFileEditorInput) {
                inputFile = ((IFileEditorInput) editorInput).getFile();
            }
            // Allow ghost runtime providers to contribute their user directory
            this.userDir = ConfigUtils.getUserDirectory(documentURI, inputFile);
        }
        this.sharedImages = PlatformUI.getWorkbench().getSharedImages();
    }

    public String getFullPath() {
        if (isOK) {
            return getCurrentPath();
        }
        return null;
    }

    public String getAbsolutePath() {
        if (isOK) {
            String path = getSelectedPath();
            StringBuilder builder = new StringBuilder();
            String basePath = selectedVar.getPath();
            if (basePath != null) {
                builder.append(basePath);
                if (path != null && !path.isEmpty()) {
                    builder.append('/');
                }
            }
            if (path != null) {
                builder.append(path);
            }
            return builder.toString();
        }
        return null;
    }

    @Override
    public int open() {
        if (getVariables().length > 0) {
            return super.open();
        }
        MessageDialog.openInformation(getShell(), Messages.title, Messages.locationDialogNoRelPathsInfo);
        return Window.CANCEL;
    }

    protected VariableEntry[] getVariables() {
        if (cachedVariables == null) {
            cachedVariables = initVariables();
        }
        return cachedVariables;
    }

    protected String getCurrentPath() {
        String path = getSelectedPath();
        StringBuilder builder = new StringBuilder();
        String varPath = selectedVar.getVarPath();
        if (varPath != null) {
            builder.append(varPath);
            if (path != null && !path.isEmpty()) {
                builder.append('/');
            }
        }
        if (path != null) {
            builder.append(path);
        }
        return builder.toString();
    }

    protected String getSelectedPath() {
        String path = selectedPath;
        if (path == null) {
            path = currentText;
        }
        return path;
    }

    protected abstract String getDialogTitle();

    protected abstract String getDialogLabel();

    protected abstract String getDialogMessage();

    protected abstract String getEntryLabel();

    protected abstract VariableEntry[] initVariables();

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(getDialogTitle());
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        if (buttonId == IDialogConstants.OK_ID) {
            isOK = true;
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(getDialogLabel());
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(getDialogMessage());

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());

        Composite leftComposite = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        leftComposite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, false, true);
        leftComposite.setLayoutData(data);

        Composite rightComposite = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        rightComposite.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightComposite.setLayoutData(data);

        // Filter text
        final Text varFilter = new Text(leftComposite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, false, false);
        varFilter.setLayoutData(data);
        varFilter.setMessage(Messages.filterMessage);

        // Variable table
        final Table varTable = new Table(leftComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL, false, true);
        varTable.setLayoutData(data);
        initVarTable(varTable, null);

        // Selected path
        Label label = new Label(rightComposite, SWT.WRAP);
        label.setText(Messages.selectedPath);
        data = new GridData(GridData.FILL, GridData.CENTER, false, false);
        label.setLayoutData(data);

        pathText = new Text(rightComposite, SWT.READ_ONLY);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        pathText.setLayoutData(data);

        // Selection tree
        final Tree selectionTree = new Tree(rightComposite, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 16 * 16; // 16 lines by 16 pixels
        data.widthHint = data.heightHint;
        data.horizontalSpan = 2;
        selectionTree.setLayoutData(data);

        // Text entry label
        final Label entryLabel = new Label(rightComposite, SWT.WRAP);
        entryLabel.setText(getEntryLabel());
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        entryLabel.setLayoutData(data);

        // Text entry box
        entryText = new Text(rightComposite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.horizontalSpan = 2;
        entryText.setLayoutData(data);

        // Selected location
        final Label locationLabel = new Label(leftComposite, SWT.WRAP);
        locationLabel.setText(Messages.selectedLocation);
        data = new GridData(GridData.END, GridData.CENTER, false, false);
        locationLabel.setLayoutData(data);

        locationText = new Text(rightComposite, SWT.READ_ONLY);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.horizontalSpan = 2;
        locationText.setLayoutData(data);

        // Listeners
        varFilter.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = varFilter.getText();
                String filter = null;
                if (text != null) {
                    filter = text;
                }
                initVarTable(varTable, filter);
                varTable.setSelection(0);
                selectedVar = (VariableEntry) varTable.getItem(0).getData(VAR_DATA);
                updatePathText();
                initRootItems(selectionTree, selectedVar.getPath());
                entryText.clearSelection();
                entryText.setText("");
                updateLocation();
                varFilter.setFocus();
            }
        });

        varTable.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                TableItem[] selections = varTable.getSelection();
                if (selections.length == 0) {
                    return;
                }
                TableItem item = selections[0];
                selectedVar = (VariableEntry) item.getData(VAR_DATA);
                updatePathText();
                initRootItems(selectionTree, selectedVar.getPath());
                updateLocation();
                entryText.clearSelection();
                entryText.setText("");
                varTable.setFocus();
            }
        });

        // Add selection tree listeners
        selectionTree.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (updating)
                    return;
                updating = true;
                TreeItem item = (TreeItem) event.item;
                selectedPath = getPath(item);
                entryText.setText(selectedPath);
                updateLocation();
                updating = false;
            }
        });

        selectionTree.addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                selectedPath = getPath(item);
                buttonPressed(IDialogConstants.OK_ID);
                close();
            }
        });

        selectionTree.addListener(SWT.Expand, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                initChildren(item);
            }
        });

        // Add text box listener
        entryText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (updating) {
                    return;
                }
                updating = true;
                String text = entryText.getText();
                text = text.replace('\\', '/');
                Path path = new Path(text);
                TreeItem item = getFirstMatch(selectionTree, path);
                if (item != null) {
                    File file = (File) item.getData();
                    if (file.isDirectory() && text.endsWith("/")) {
                        if (!item.getExpanded()) {
                            initChildren(item);
                            item.setExpanded(true);
                        }
                    }
                    selectionTree.setSelection(item);
                    selectedPath = getPath(item);
                } else {
                    selectionTree.deselectAll();
                    selectedPath = null;
                }
                currentText = entryText.getText();
                updateLocation();
                entryText.setFocus();
                updating = false;
            }
        });

        selectedVar = getVariables()[0];
        varTable.setSelection(new int[] { 0 });
        initRootItems(selectionTree, selectedVar.getPath());
        return composite;
    }

    @Override
    public void create() {
        super.create();
        updatePathText();
        updateLocation();
        entryText.setFocus();
    }

    protected void updatePathText() {
        String pathString = selectedVar.getPath();
        if (pathString == null) {
            pathString = "";
        }
        pathText.setText(shortenText(pathString, pathText));
        pathText.setToolTipText(pathString);
    }

    protected void updateLocation() {
        String currentPath = getCurrentPath();
        if (currentPath.isEmpty()) {
            currentPath = Messages.emptyLocation;
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
        locationText.setText(currentPath);
        locationText.setToolTipText(currentPath);
    }

    protected void initVarTable(Table varTable, String filter) {
        varTable.removeAll();
        VariableEntry[] variables = getVariables();
        if (filter == null) {
            for (VariableEntry variable : variables) {
                addEntry(varTable, variable);
            }
        } else {
            pattern.setPattern("*" + filter + "*");
            for (VariableEntry variable : variables) {
                if (variable.match(pattern)) {
                    addEntry(varTable, variable);
                }
            }
        }
    }

    protected void addEntry(Table varTable, VariableEntry variable) {
        TableItem item = new TableItem(varTable, SWT.NONE);
        item.setText(variable.getName());
        item.setImage(variable.getImage());
        item.setData(VAR_DATA, variable);
    }

    protected void initRootItems(Tree tree, String path) {
        tree.removeAll();
        File[] files = null;
        boolean isRoot = false;
        if (path != null) {
            File file = new File(path);
            files = file.listFiles();
        } else {
            files = File.listRoots();
            if (files.length == 1) {
                files = files[0].listFiles();
            } else {
                isRoot = true;
            }
        }
        if (files == null)
            return;
        Arrays.sort(files, fileComparator);
        for (File child : files) {
            if (directoriesOnly && !child.isDirectory()) {
                continue;
            }
            TreeItem item = new TreeItem(tree, 0);
            item.setText(isRoot ? child.getPath() : child.getName());
            item.setData(child);
            if (child.isDirectory()) {
                item.setItemCount(1);
                item.setImage(getDirImage());
            } else {
                item.setImage(getFileImage());
            }
        }
    }

    protected void initChildren(TreeItem parent) {
        parent.removeAll();
        File file = (File) parent.getData();
        if (file == null)
            return;
        File[] files = file.listFiles();
        if (files == null)
            return;
        Arrays.sort(files, fileComparator);
        for (File child : files) {
            if (directoriesOnly && !child.isDirectory()) {
                // Directories are sorted first so just quit
                // when hit a file.
                break;
            }
            TreeItem item = new TreeItem(parent, 0);
            item.setText(child.getName());
            item.setData(child);
            if (child.isDirectory()) {
                item.setItemCount(1);
                item.setImage(getDirImage());
            } else {
                item.setImage(getFileImage());
            }
        }
    }

    protected String getPath(TreeItem item) {
        if (selectedVar.getAbsolute()) {
            File file = (File) item.getData();
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                Trace.logError("Failed to get canonical path for: " + file.toString(), e);
                return file.getPath();
            }
        }
        TreeItem parent = item.getParentItem();
        if (parent != null) {
            return getPath(parent) + "/" + item.getText();
        }
        return item.getText();
    }

    protected TreeItem getFirstMatch(Tree tree, Path path) {
        TreeItem result = null;
        String[] segments = path.segments();
        if (segments.length > 0) {
            result = getFirstMatch(tree.getItems(), segments[0]);
            for (int i = 1; i < segments.length && result != null; i++) {
                result = getFirstMatch(result.getItems(), segments[i]);
            }
        }
        return result;
    }

    protected TreeItem getFirstMatch(TreeItem[] items, String filter) {
        for (TreeItem item : items) {
            String name = item.getText();
            if (name.startsWith(filter)) {
                return item;
            }
        }
        return null;
    }

    protected TreeItem[] getComboList(Tree tree, String filter) {
        ArrayList<TreeItem> list = new ArrayList<TreeItem>(20);
        TreeItem[] items = tree.getItems();
        for (TreeItem item : items) {
            String name = item.getText();
            if (filter == null || name.startsWith(filter)) {
                list.add(item);
            }
        }
        return list.toArray(new TreeItem[list.size()]);
    }

    protected TreeItem[] getComboList(TreeItem parent, String filter) {
        ArrayList<TreeItem> list = new ArrayList<TreeItem>(20);
        TreeItem[] items = parent.getItems();
        for (TreeItem item : items) {
            String name = item.getText();
            if (filter == null || name.startsWith(filter)) {
                list.add(item);
            }
        }
        return list.toArray(new TreeItem[list.size()]);
    }

    protected String[] getTextList(TreeItem[] items) {
        String[] textList = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            textList[i] = items[i].getText();
        }
        return textList;
    }

    protected String getFilter(String path) {
        int index = Math.max(path.indexOf('/'), path.indexOf('\\'));
        if (index < 1) {
            if (path.length() > 0) {
                return path;
            }
            return null;
        }
        String filter = path.substring(index);
        if (filter.length() == 0) {
            return null;
        }
        return filter;
    }

    protected TreeItem getCurrentSelection(Tree tree) {
        TreeItem[] selections = tree.getSelection();
        if (selections.length > 0) {
            return selections[0];
        }
        return null;
    }

    protected Image getFileImage() {
        return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
    }

    protected Image getDirImage() {
        return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
    }

    protected void addConfigVars(ArrayList<VariableEntry> list, String skip, boolean includeDocVars) {
        ConfigVars vars = getConfigVars();
        if (document != null && includeDocVars) {
            ConfigurationFile configFile = null;
            if (server != null && documentURI != null) {
                configFile = server.getConfigurationFileFromURI(documentURI);
            }
            ConfigUtils.getVariables(configFile, document, documentURI, server, userDir, vars);
        }
        List<String> names = vars.getSortedVars(ConfigVars.STRING_TYPES, false);
        for (String name : names) {
            if (skip != null && name.equals(skip)) {
                continue;
            }
            String value = vars.getValue(name);
            File file = new File(value);
            if (file.exists() && (!directoriesOnly || file.isDirectory())) {
                String path;
                try {
                    path = file.getCanonicalPath();
                } catch (IOException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Could not get canonical path for: " + value, e);
                    }
                    path = value;
                }
                list.add(new VariableEntry(name, path, ConfigVarsUtils.getVarRef(name), Activator.getImage(Activator.IMG_ABSOLUTE_PATH)));
            }
        }
    }

    protected ConfigVars getConfigVars() {
        if (configVars == null) {
            ConfigVars vars = new ConfigVars();
            if (server != null) {
                server.getVariables(vars, true);
            } else if (userDir != null) {
                userDir.getVariables(vars, true);
            }
            configVars = vars;
        }
        return configVars;
    }

    protected static class FileComparator implements Comparator<File>, Serializable {
        @Override
        public int compare(File file1, File file2) {
            if (file1.isDirectory() && !file2.isDirectory()) {
                return -1;
            } else if (!file1.isDirectory() && file2.isDirectory()) {
                return 1;
            } else {
                String name1 = file1.getName();
                String name2 = file2.getName();
                return name1.compareToIgnoreCase(name2);
            }
        }
    }

    protected static class VariableEntry {
        protected final String varName;
        protected final String path;
        protected final String varPath;
        protected final Image image;
        protected boolean isAbsolute;

        public VariableEntry(String varName, String path, String varPath, Image image) {
            this.varName = varName;
            this.path = path;
            this.varPath = varPath;
            this.image = image;
        }

        public String getName() {
            return varName;
        }

        public String getPath() {
            return path;
        }

        public String getVarPath() {
            return varPath;
        }

        public Image getImage() {
            return image;
        }

        public void setAbsolute(boolean value) {
            isAbsolute = value;
        }

        public boolean getAbsolute() {
            return isAbsolute;
        }

        public boolean match(SearchPattern pattern) {
            return pattern.matches(varName);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Name: " + varName + ", Path: " + path;
        }
    }
}
