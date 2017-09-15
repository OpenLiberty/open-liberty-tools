/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Dialog for creating a fileset element.
 */
public class FilesetCreateDialog extends AbstractChainableDialog {

    private static final String DEFAULT_INCLUDES = "*";
    private static final String INITIAL_FILTER = "*.jar";

    protected boolean isOK = false;
    protected String id = null;
    protected String filesetDir = null;
    protected String filesetIncludes = null;
    protected String filesetExcludes = null;
    protected Text idText = null;

    public FilesetCreateDialog(Shell parent, Document doc, URI docURI, UserDirectory userDir,
                               String[] tags, String[] labels) {
        super(parent, doc, docURI, userDir, tags, labels);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.filesetCreateTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        isOK = true;
        super.okPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.filesetCreateLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(Messages.filesetCreateMessage);

        final Composite composite = createTopLevelComposite(parent);
        createBreadcrumb(composite);

        // Id
        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.chainableConfigId);
        GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        idText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        data.horizontalSpan = 2;
        idText.setLayoutData(data);
        idText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String idStr = idText.getText();
                if (idStr != null && !idStr.isEmpty()) {
                    id = idStr;
                    if (filesetDir != null) {
                        enableOKButton(true);
                    }
                } else {
                    enableOKButton(false);
                }
            }
        });

        // Directory
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.filesetCreateBaseDir);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text dirText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        dirText.setLayoutData(data);

        Button dirBrowseButton = new Button(composite, SWT.PUSH);
        dirBrowseButton.setText(Messages.browseButtonAcc);
        data = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        dirBrowseButton.setLayoutData(data);
        dirBrowseButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                DirectoryDialog dirDialog = new DirectoryDialog(getShell());
                String dirStr = dirDialog.open();
                if (dirStr != null && !dirStr.isEmpty()) {
                    filesetDir = dirStr;
                    dirText.setText(dirStr);
                    dirText.setToolTipText(dirStr);
                    if (id != null) {
                        enableOKButton(true);
                    }
                }
            }
        });

        // Includes
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.filesetCreateIncludes);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text includesText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        includesText.setLayoutData(data);
        includesText.setText(DEFAULT_INCLUDES);
        includesText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String includesStr = includesText.getText();
                if (includesStr != null && !includesStr.isEmpty()) {
                    filesetIncludes = includesStr;
                    includesText.setToolTipText(includesStr);
                } else {
                    filesetIncludes = null;
                    includesText.setToolTipText("");
                }
            }
        });

        final Button includeBrowseButton = new Button(composite, SWT.PUSH);
        includeBrowseButton.setText(Messages.browseButtonAcc2);
        data = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        includeBrowseButton.setLayoutData(data);
        includeBrowseButton.setEnabled(false);
        includeBrowseButton.addListener(SWT.Selection, new Listener() {
            /** {@inheritDoc} */
            @Override
            public void handleEvent(Event arg0) {
                FileSelectorDialog fileDialog = new FileSelectorDialog(getShell(), filesetDir, Messages.filesetIncludesTitle, Messages.filesetIncludesLabel, Messages.filesetIncludesMessage);
                fileDialog.setInitialFilter(INITIAL_FILTER);
                fileDialog.open();
                if (fileDialog.isOK()) {
                    filesetIncludes = fileDialog.getFileList();
                    includesText.setText(filesetIncludes);
                    includesText.setToolTipText(filesetIncludes);
                }
            }
        });

        // Excludes
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.filesetCreateExcludes);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text excludesText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        excludesText.setLayoutData(data);
        excludesText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String excludesStr = excludesText.getText();
                if (excludesStr != null && !excludesStr.isEmpty()) {
                    filesetExcludes = excludesStr;
                    excludesText.setToolTipText(excludesStr);
                } else {
                    filesetExcludes = null;
                    excludesText.setToolTipText("");
                }
            }
        });

        final Button excludeBrowseButton = new Button(composite, SWT.PUSH);
        excludeBrowseButton.setText(Messages.browseButtonAcc3);
        data = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        excludeBrowseButton.setLayoutData(data);
        excludeBrowseButton.setEnabled(false);
        excludeBrowseButton.addListener(SWT.Selection, new Listener() {
            /** {@inheritDoc} */
            @Override
            public void handleEvent(Event arg0) {
                FileSelectorDialog fileDialog = new FileSelectorDialog(getShell(), filesetDir, Messages.filesetExcludesTitle, Messages.filesetExcludesLabel, Messages.filesetExcludesMessage);
                fileDialog.setInitialFilter(INITIAL_FILTER);
                fileDialog.open();
                if (fileDialog.isOK()) {
                    filesetExcludes = fileDialog.getFileList();
                    excludesText.setText(filesetExcludes);
                    excludesText.setToolTipText(filesetExcludes);
                }
            }
        });

        // Directory must be specified before includes and excludes
        // can be browsed.
        dirText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String dirStr = dirText.getText();
                if (dirStr != null && !dirStr.isEmpty()) {
                    filesetDir = dirStr;
                    dirText.setToolTipText(dirStr);
                    includeBrowseButton.setEnabled(true);
                    excludeBrowseButton.setEnabled(true);
                    if (id != null) {
                        enableOKButton(true);
                    }
                } else {
                    filesetDir = null;
                    dirText.setToolTipText("");
                    includeBrowseButton.setEnabled(false);
                    excludeBrowseButton.setEnabled(false);
                    enableOKButton(false);
                }
            }
        });

        return composite;
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton(false);
        return control;
    }

    @Override
    public void create() {
        super.create();
        idText.setFocus();
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    public boolean isOK() {
        return isOK;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getIds() {
        if (isOK) {
            return new String[] { id };
        }
        return new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public List<Element> getElements() {
        if (isOK) {
            ArrayList<Element> list = new ArrayList<Element>(1);
            Element elem = doc.createElement(Constants.FILESET);
            elem.setAttribute(Constants.INSTANCE_ID, id);
            elem.setAttribute(Constants.FILESET_DIR, filesetDir);
            if (filesetIncludes != null) {
                elem.setAttribute(Constants.FILESET_INCLUDES, filesetIncludes);
            }
            if (filesetExcludes != null) {
                elem.setAttribute(Constants.FILESET_EXCLUDES, filesetExcludes);
            }
            list.add(elem);
            return list;
        }
        return Collections.emptyList();
    }
}
