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
 * Dialog for creating a JDBC driver element.
 */
public class JDBCDriverCreateDialog extends AbstractChainableDialog {

    protected boolean isOK = false;
    protected String id = null;
    protected String libraryRef = null;
    protected List<Element> libraryElements = null;
    protected Text idText = null;

    public JDBCDriverCreateDialog(Shell parent, Document doc, URI docURI, UserDirectory userDir,
                                  String[] tags, String[] labels) {
        super(parent, doc, docURI, userDir, tags, labels);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.jdbcDriverCreateTitle);
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
        setTitle(Messages.jdbcDriverCreateLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(Messages.jdbcDriverCreateMessage);

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
                    if (libraryRef != null) {
                        enableOKButton(true);
                    }
                } else {
                    enableOKButton(false);
                }
            }
        });

        // Library ref
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.jdbcDriverLibRef);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Combo libraryCombo = new Combo(composite, SWT.NONE);
        libraryCombo.setItems(getIds(Constants.LIBRARY));
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        libraryCombo.setLayoutData(data);
        libraryCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String libraryStr = libraryCombo.getText();
                if (libraryStr != null && !libraryStr.isEmpty()) {
                    libraryRef = libraryStr;
                    if (id != null) {
                        enableOKButton(true);
                    }
                } else {
                    libraryRef = null;
                    enableOKButton(false);
                }
            }
        });

        Button libraryCreateButton = new Button(composite, SWT.PUSH);
        libraryCreateButton.setText(Messages.newButtonAcc);
        data = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        libraryCreateButton.setLayoutData(data);
        libraryCreateButton.addListener(SWT.Selection, new Listener() {
            /** {@inheritDoc} */
            @Override
            public void handleEvent(Event arg0) {
                String[] newTags = arrayAppend(tags, Constants.SHARED_LIBRARY);
                String[] newLabels = arrayAppend(labels, getLabel(doc, newTags, docURI, Constants.SHARED_LIBRARY));
                LibraryCreateDialog libraryDialog = new LibraryCreateDialog(composite.getShell(), doc, docURI, userDir, newTags, newLabels);
                libraryDialog.setParentLocation(getShell().getBounds());
                libraryDialog.open();
                if (libraryDialog.isOK()) {
                    // TODO: Check if library already exists and confirm with
                    // user if want to create a new one. Or should do this on
                    // okPressed of LibraryCreateDialog?
                    libraryElements = libraryDialog.getElements();
                    libraryRef = getRefString(libraryDialog.getIds());
                    libraryCombo.setText(libraryRef);
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
            Element elem = doc.createElement(Constants.JDBC_DRIVER);
            elem.setAttribute(Constants.INSTANCE_ID, id);
            elem.setAttribute(Constants.SHARED_LIBRARY_REF, libraryRef);
            list.add(elem);
            if (libraryElements != null) {
                list.addAll(libraryElements);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
