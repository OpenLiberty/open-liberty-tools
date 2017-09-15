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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Browse dialog for configuration file includes.
 */
public class IncludeDialog extends AbstractBrowseDialog {

    protected String relPath;

    public IncludeDialog(Shell parent, Document document, IEditorInput editorInput) {
        super(parent, document, editorInput);
        setBaseURI(documentURI);
    }

    public void setBaseURI(URI uri) {
        if (uri != null) {
            String path = uri.getPath();
            int index = path.lastIndexOf('/');
            relPath = path.substring(0, index);
            File file = new File(relPath);
            if (file.exists()) {
                try {
                    relPath = file.getCanonicalPath();
                } catch (IOException e) {
                    Trace.logError("Failed to get canonical path for: " + file.toString(), e);
                }
            }
        }
    }

    @Override
    protected String getDialogTitle() {
        return Messages.includeDialogTitle;
    }

    @Override
    protected String getDialogLabel() {
        return Messages.includeDialogLabel;
    }

    @Override
    protected String getDialogMessage() {
        return Messages.includeDialogMessage;
    }

    /** {@inheritDoc} */
    @Override
    protected VariableEntry[] initVariables() {
        ArrayList<VariableEntry> list = new ArrayList<VariableEntry>();
        if (relPath != null)
            list.add(new VariableEntry(Messages.relativePath, relPath, null, Activator.getImage(Activator.IMG_RELATIVE_PATH)));
        if (userDir != null)
            list.add(new VariableEntry(NLS.bind(Messages.sharedConfigPath, ConfigVars.SHARED_CONFIG_DIR), userDir.getSharedConfigPath().toOSString(), SHAREDCONFIGVAR, Activator.getImage(Activator.IMG_CONFIG_FOLDER)));

        addConfigVars(list, ConfigVars.SHARED_CONFIG_DIR, false);

        return list.toArray(new VariableEntry[list.size()]);
    }

    @Override
    protected String getEntryLabel() {
        return Messages.includeDialogEntryLabel;
    }
}
