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
package com.ibm.ws.st.ui.internal.download;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class ScrollableMessageDialog extends MessageDialog {

    private String message = null;

    /**
     * @param parentShell
     * @param dialogTitle
     * @param dialogTitleImage
     * @param dialogMessage
     * @param dialogImageType
     * @param dialogButtonLabels
     * @param defaultIndex
     */
    public ScrollableMessageDialog(Shell shell, String message, int dialogType, String messageDecription) {
        super(shell, Messages.title, null, messageDecription, dialogType,
              new String[] { IDialogConstants.OK_LABEL }, 0);
        this.message = message;

    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public Control createCustomArea(Composite parent) {
        if (this.message == null | this.message.length() == 0)
            return null;
        StyledText descriptionText = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = 600;
        data.heightHint = 200;
        data.minimumHeight = 80;
        descriptionText.setTopMargin(3);
        descriptionText.setLeftMargin(5);
        descriptionText.setRightMargin(5);
        descriptionText.setAlwaysShowScrollBars(false);
        descriptionText.setLayoutData(data);
        descriptionText.setText(message);
        descriptionText.setEditable(false);
        return parent;
    }

}
