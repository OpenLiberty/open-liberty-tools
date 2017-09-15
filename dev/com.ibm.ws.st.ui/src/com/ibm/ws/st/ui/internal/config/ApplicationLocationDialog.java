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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Browse dialog for application locations.
 */
public class ApplicationLocationDialog extends AbstractBrowseDialog {
    private static final String SERVERAPPVAR = ConfigVarsUtils.getVarRef(ConfigVars.SERVER_CONFIG_DIR) + "/apps";

    public ApplicationLocationDialog(Shell parent, Document document, IEditorInput editorInput) {
        super(parent, document, editorInput);
    }

    @Override
    protected String getDialogTitle() {
        return Messages.appLocationDialogTitle;
    }

    @Override
    protected String getDialogLabel() {
        return Messages.appLocationDialogLabel;
    }

    @Override
    protected String getDialogMessage() {
        return Messages.appLocationDialogMessage;
    }

    @Override
    protected VariableEntry[] initVariables() {
        ArrayList<VariableEntry> list = new ArrayList<VariableEntry>();
        if (server != null) {
            IPath serverApps = server.getServerAppsPath();
            if (serverApps != null) {
                list.add(new VariableEntry(Messages.relativePath, serverApps.toOSString(), SERVERAPPVAR, Activator.getImage(Activator.IMG_APP_FOLDER)));
            }

        }
        if (userDir != null) {
            IPath sharedApps = userDir.getSharedAppsPath();
            list.add(new VariableEntry(NLS.bind(Messages.sharedAppsPath, ConfigVars.SHARED_APP_DIR), sharedApps.toOSString(), SHAREDAPPVAR, Activator.getImage(Activator.IMG_APP_FOLDER)));
        }

        addConfigVars(list, ConfigVars.SHARED_APP_DIR, true);

        return list.toArray(new VariableEntry[list.size()]);
    }

    @Override
    protected String getEntryLabel() {
        return Messages.appLocationDialogEntryLabel;
    }
}
