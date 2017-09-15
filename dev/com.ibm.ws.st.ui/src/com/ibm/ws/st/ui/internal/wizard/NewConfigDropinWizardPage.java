/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.actions.NewConfigDropinAction;

public class NewConfigDropinWizardPage extends WizardPage {
    protected IPath dropinsPath;
    protected NewConfigDropinAction.DropinType type;
    protected String fileName;

    public NewConfigDropinWizardPage(IPath dropinsPath, NewConfigDropinAction.DropinType type) {
        super("configDropins", type.getTitle(), null);
        this.dropinsPath = dropinsPath;
        this.type = type;
        setDescription(type.getDesc());
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        comp.setLayout(layout);

        final Label label = new Label(comp, SWT.NONE);
        label.setText(Messages.newConfigDropinWizardFileNameLabel);
        GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
        label.setLayoutData(data);

        final Text text = new Text(comp, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        text.setLayoutData(data);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                fileName = text.getText();
                setPageComplete(validate());
            }
        });

        text.setFocus();
        setControl(comp);
        setPageComplete(validate());
    }

    protected boolean validate() {
        if (fileName == null || fileName.isEmpty()) {
            setMessage(null, IMessageProvider.NONE);
            return false;
        }

        if (!fileName.endsWith(".xml")) {
            setMessage(Messages.newConfigDropinWizardXMLExtError, IMessageProvider.ERROR);
            return false;
        }

        IPath path = dropinsPath.append(fileName);
        if (path.toFile().exists()) {
            setMessage(Messages.newConfigDropinWizardExistsError, IMessageProvider.ERROR);
            return false;
        }

        setMessage(null, IMessageProvider.NONE);
        return true;
    }

    public String getFileName() {
        return fileName;
    }
}
