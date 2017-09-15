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

import java.util.ArrayList;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Generic browse dialog for files.
 */
public class GenericFileDialog extends AbstractBrowseDialog {

    private String relPathVar = null;

    public GenericFileDialog(Shell parent, Document document, IEditorInput editorInput, String relPathVar) {
        super(parent, document, editorInput);
        this.relPathVar = relPathVar;
    }

    public GenericFileDialog(Shell parent, Document document, IEditorInput editorInput) {
        super(parent, document, editorInput);
    }

    @Override
    protected String getDialogTitle() {
        return Messages.genericFileDialogTitle;
    }

    @Override
    protected String getDialogLabel() {
        return Messages.genericFileDialogLabel;
    }

    @Override
    protected String getDialogMessage() {
        return Messages.genericFileDialogMessage;
    }

    /** {@inheritDoc} */
    @Override
    protected VariableEntry[] initVariables() {
        ArrayList<VariableEntry> list = new ArrayList<VariableEntry>();
        if (relPathVar != null) {
            ConfigVars vars = getConfigVars();
            String relPath = vars.getValue(relPathVar);
            if (relPath != null && !relPath.isEmpty()) {
                list.add(new VariableEntry(Messages.relativePath, relPath, null, Activator.getImage(Activator.IMG_RELATIVE_PATH)));
            }
        }
        addConfigVars(list, null, true);
        return list.toArray(new VariableEntry[list.size()]);
    }

    @Override
    protected String getEntryLabel() {
        return Messages.genericFileDialogEntryLabel;
    }
}
