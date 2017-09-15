/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.ui.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

/**
 *
 */
public class PublishWithErrorDialog extends MessageDialogWithToggle {

    private final ArrayList<String> projectNames;
    protected Table projectTable;

    public PublishWithErrorDialog(Shell parentShell, ArrayList<String> projectNames) {
        super(parentShell, Messages.publishWithErrorsDialogTitle, null, Messages.publishWithErrorMessage, MessageDialog.WARNING, new String[] {}, 1, Messages.publishWithErrorToggle, false);
        this.projectNames = projectNames;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        final Button OK = createButton(parent, 0, Messages.publishWarningOKButton, true);
        final Button Cancel = createButton(parent, -1, Messages.publishWarningCancelButton, false);
        super.createButtonsForButtonBar(parent);
    }

    @Override
    public Control createCustomArea(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        comp.setLayout(layout);

        StyledText descriptionText = new StyledText(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 100;
        descriptionText.setTopMargin(3);
        descriptionText.setLeftMargin(5);
        descriptionText.setRightMargin(5);
        descriptionText.setAlwaysShowScrollBars(false);
        descriptionText.setLayoutData(data);
        descriptionText.setText(getProjectListAsString(projectNames));
        descriptionText.setEditable(false);
        Label warning = new Label(comp, SWT.NONE);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.verticalIndent = 8;
        warning.setLayoutData(data);
        warning.setText(Messages.publishWithErrorUserMessage);
        return parent;
    }

    private String getProjectListAsString(ArrayList<String> projectNames) {
        if (projectNames == null || projectNames.isEmpty())
            return "";
        StringBuilder result = new StringBuilder();
        for (Iterator<String> iter = projectNames.iterator(); iter.hasNext();) {
            result.append(iter.next());
            if (iter.hasNext())
                result.append("\n");
        }
        return result.toString();
    }
}
