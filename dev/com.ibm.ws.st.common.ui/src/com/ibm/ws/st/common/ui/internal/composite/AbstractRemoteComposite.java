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
package com.ibm.ws.st.common.ui.internal.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 *
 */
public abstract class AbstractRemoteComposite extends Composite {

    protected static final int VALIDATION_INDEX_PLATFORM = 0;
    protected static final int VALIDATION_INDEX_PATHS = 1;
    protected static final int VALIDATION_INDEX_LOG_ON_METHOD = 2;
    protected static final int VALIDATION_INDEX_OS_LOG_ON = 3;
    protected static final int VALIDATION_INDEX_SSH_LOG_ON = 4;
    protected static final int VALIDATION_STARTUP_ENABLED = 5;
    protected static final int VALIDATION_INDEX_PORT = 6;

    public AbstractRemoteComposite(Composite parent) {
        super(parent, SWT.NO_RADIO_GROUP);
    }

    /**
     * Creates a button instance and sets the default layout data.
     */
    protected Button createButton(Composite parent, String text, int style) {
        Button button = new Button(parent, style);
        button.setText(text);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        button.setLayoutData(data);
        return button;
    }

    /**
     * Creates a label instance and sets the default layout data.
     */
    protected Label createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.LEFT);
        label.setText(text);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        label.setLayoutData(data);
        return label;
    }

    /**
     * Create a text field specific for this application
     */
    protected Text createTextField(Composite parent, int layoutStyle) {
        Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData data = new GridData(layoutStyle);
        text.setLayoutData(data);
        return text;
    }

}
