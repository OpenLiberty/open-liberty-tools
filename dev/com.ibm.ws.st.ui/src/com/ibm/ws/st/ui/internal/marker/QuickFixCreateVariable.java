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
package com.ibm.ws.st.ui.internal.marker;

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.config.VariableDialog;

/**
 * Quick fix for create variable.
 */
public class QuickFixCreateVariable extends AbstractMarkerResolution {
    private final String variableName;

    public QuickFixCreateVariable(String name) {
        variableName = name;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.createVariableQuickFix, new String[] { variableName });
    }

    @Override
    public void run(IMarker marker) {
        final IResource resource = getResource(marker);
        if (resource == null)
            return;

        final String value = getVariableValue();
        if (value == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix for create variable failed. The value for the variable '" + variableName + "' is null.", null);
            showErrorMessage();
            return;
        }

        final ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        if (configFile.addVariable(variableName, value)) {
            try {
                configFile.save(null);
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.ERROR, "Quick fix for create variable '" + variableName + "'failed. Error trying to save configuration file: " + configFile.getURI(), e);
                showErrorMessage();
            }
        } else {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix for create variable failed. Error adding variable '" + variableName + "' to configuration: " + configFile.getURI(), null);
            showErrorMessage();
        }
    }

    private String getVariableValue() {
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final VariableDialog dialog = new VariableDialog(shell, variableName);

        dialog.open();
        return dialog.getValue();
    }

    @Override
    protected String getErrorMessage() {
        return Messages.createVariableFailedMessage;
    }
}
