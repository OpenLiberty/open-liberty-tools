/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.ui.internal.ActionConstants;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.utility.SSLCertificateWizardPage;
import com.ibm.ws.st.ui.internal.utility.UtilityWizard;

/**
 * Create SSL certificate action.
 */
public class SSLCertificateAction extends WebSphereServerAction {
    private static final String commandPath = "bin/securityUtility";

    public static String ID = ActionConstants.SSL_CERTIFICATE_ACTION_ID;

    public SSLCertificateAction(Shell shell, ISelectionProvider selProvider) {
        super(Messages.actionCreateSSL, shell, selProvider);
    }

    @Override
    public void run() {
        if (notifyUtilityDisabled(wsServer.getServerType(), ID))
            return;
        UtilityWizard.open(shell, new SSLCertificateWizardPage(server));
    }

    @Override
    protected String getActionValidationPath() {
        return commandPath;
    }

    @Override
    public String getId() {
        return ID;
    }
}