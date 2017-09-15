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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
 * Dialog for creating a library element.
 */
public class LibraryCreateDialog extends AbstractChainableDialog {

    protected boolean isOK = false;
    protected String id = null;
    protected String filesetRef = null;
    protected List<Element> filesetElements = null;
    protected Text idText = null;

    public LibraryCreateDialog(Shell parent, Document doc, URI docURI,
                               UserDirectory userDir, String[] tags, String[] labels) {
        super(parent, doc, docURI, userDir, tags, labels);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.libraryCreateTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        if (buttonId == IDialogConstants.OK_ID) {
            isOK = true;
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.libraryCreateLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(Messages.libraryCreateMessage);

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
                    if (filesetRef != null) {
                        enableOKButton(true);
                    }
                } else {
                    enableOKButton(false);
                }
            }
        });

        // Fileset ref
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.libraryCreateFilesetRef);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Combo filesetCombo = new Combo(composite, SWT.NONE);
        filesetCombo.setItems(getIds(Constants.FILESET));
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        filesetCombo.setLayoutData(data);
        filesetCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String filesetStr = filesetCombo.getText();
                if (filesetStr != null && !filesetStr.isEmpty()) {
                    filesetRef = filesetStr;
                    if (id != null) {
                        enableOKButton(true);
                    }
                } else {
                    filesetRef = null;
                    enableOKButton(false);
                }
            }
        });

        Button filesetCreateButton = new Button(composite, SWT.PUSH);
        filesetCreateButton.setText(Messages.newButtonAcc);
        data = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        filesetCreateButton.setLayoutData(data);
        filesetCreateButton.addListener(SWT.Selection, new Listener() {
            /** {@inheritDoc} */
            @Override
            public void handleEvent(Event arg0) {
                String[] newTags = arrayAppend(tags, Constants.FILESET);
                String[] newLabels = arrayAppend(labels, getLabel(doc, newTags, docURI, Constants.FILESET));
                FilesetCreateDialog filesetDialog = new FilesetCreateDialog(composite.getShell(), doc, docURI, userDir, newTags, newLabels);
                filesetDialog.setParentLocation(getShell().getBounds());
                filesetDialog.open();
                if (filesetDialog.isOK()) {
                    // TODO: Check if fileset already exists and open confirm dialog
                    // to create fileset with same id
                    filesetElements = filesetDialog.getElements();
                    filesetRef = getRefString(filesetDialog.getIds());
                    filesetCombo.setText(filesetRef);
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

    @Override
    public List<Element> getElements() {
        if (isOK) {
            ArrayList<Element> list = new ArrayList<Element>(1);
            Element elem = doc.createElement(Constants.SHARED_LIBRARY);
            elem.setAttribute(Constants.INSTANCE_ID, id);
            elem.setAttribute(Constants.FILESET_REF, filesetRef);
            list.add(elem);
            if (filesetElements != null) {
                list.addAll(filesetElements);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
