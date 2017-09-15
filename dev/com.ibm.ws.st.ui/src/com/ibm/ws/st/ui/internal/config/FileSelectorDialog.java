/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.dialogs.SearchPattern;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class FileSelectorDialog extends TitleAreaDialog {

    private static final String SEPARATOR = ", ";

    protected String dialogTitle;
    protected String dialogLabel;
    protected String dialogMessage;
    protected String path;
    protected String filter = "";

    protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);

    protected Table table;
    protected String fileList;
    protected boolean isOK = false;

    public FileSelectorDialog(Shell parent, String path, String title, String label, String message) {
        super(parent);
        this.path = path;
        this.dialogTitle = title;
        this.dialogLabel = label;
        this.dialogMessage = message;
    }

    public void setInitialFilter(String filter) {
        this.filter = filter;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(dialogTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(dialogLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(dialogMessage);

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

        // File table
        table = new Table(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.horizontalSpan = 2;
        data.heightHint = 16 * 16; // 16 lines by 16 pixels
        data.widthHint = data.heightHint;
        table.setLayoutData(data);
        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                updateOKButton();
            }
        });

        table.addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                okPressed();
                close();
            }
        });

        // Text entry label
        final Label entryLabel = new Label(composite, SWT.NONE);
        entryLabel.setText(Messages.fileSelectorFilter);
        data = new GridData(GridData.FILL, GridData.CENTER, false, false);
        entryLabel.setLayoutData(data);

        // Text entry box
        final Text entryText = new Text(composite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        entryText.setText(filter);
        entryText.setLayoutData(data);
        entryText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = entryText.getText();
                if (text == null) {
                    filter = "";
                } else {
                    filter = text;
                }
                initTableItems();
                updateOKButton();
            }
        });

        initTableItems();
        return composite;
    }

    protected void initTableItems() {
        table.removeAll();
        File[] files = null;
        if (path != null) {
            File file = new File(path);
            pattern.setPattern(filter);
            files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return (!file.isDirectory() && pattern.matches(file.getName()));
                }
            });
        }
        if (files == null)
            return;

        Arrays.sort(files);
        for (File file : files) {
            TableItem item = new TableItem(table, 0);
            item.setText(file.getName());
            item.setData(file);
        }

    }

    protected void saveFileList() {
        StringBuilder builder = new StringBuilder();
        boolean start = true;
        for (TableItem item : table.getSelection()) {
            if (!start) {
                builder.append(SEPARATOR);
            } else {
                start = false;
            }
            builder.append(item.getText());
        }
        if (start) {
            fileList = null;
        } else {
            fileList = builder.toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        updateOKButton();
        return control;
    }

    protected void updateOKButton() {
        getButton(IDialogConstants.OK_ID).setEnabled(table.getSelectionCount() > 0);
    }

    /** {@inheritDoc} */
    @Override
    protected void okPressed() {
        saveFileList();
        isOK = true;
        super.okPressed();
    }

    public boolean isOK() {
        return isOK;
    }

    public String getFileList() {
        return fileList;
    }

}
