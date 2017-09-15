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

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class AppMonitorDialog extends AbstractBrowseDialog {

    public AppMonitorDialog(Shell parent, Document document, IEditorInput editorInput) {
        super(parent, document, editorInput);
        directoriesOnly = true;
    }

    @Override
    protected String getDialogTitle() {
        return Messages.appMonitorDialogTitle;
    }

    @Override
    protected String getDialogLabel() {
        return Messages.appMonitorDialogLabel;
    }

    @Override
    protected String getDialogMessage() {
        return Messages.appMonitorDialogMessage;
    }

    /** {@inheritDoc} */
    @Override
    protected VariableEntry[] initVariables() {
        ArrayList<VariableEntry> list = new ArrayList<VariableEntry>();
        if (server != null) {
            IPath relPath = server.getServerPath();
            if (relPath != null) {
                list.add(new VariableEntry(Messages.relativePath, relPath.toOSString(), null, Activator.getImage(Activator.IMG_RELATIVE_PATH)));
            }
        }

        addConfigVars(list, null, true);

        return list.toArray(new VariableEntry[list.size()]);
    }

    @Override
    protected String getEntryLabel() {
        return Messages.appMonitorDialogEntryLabel;
    }

}
