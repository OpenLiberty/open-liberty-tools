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
package com.ibm.ws.st.ui.internal.actions;

import java.io.IOException;
import java.net.ConnectException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.ui.internal.Messages;

public class OpenMessagesAction extends OpenLogAction {

    public OpenMessagesAction(ISelectionProvider sp, Shell shell) {
        super(Messages.actionOpenLogMessages, sp, shell);
    }

    /** {@inheritDoc} */
    @Override
    public IPath getServerInfoLogFile() {
        if (wsServerInfo == null)
            return null;
        return wsServerInfo.getMessagesFile();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @throws UnsupportedServiceException
     * @throws ConnectException
     */
    @Override
    public IPath getServerLogFile() throws ConnectException, UnsupportedServiceException, IOException {

        return wsServer.getMessagesFile();
    }
}
