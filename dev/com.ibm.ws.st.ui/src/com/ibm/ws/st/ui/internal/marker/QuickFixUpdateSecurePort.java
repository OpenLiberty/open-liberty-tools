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
package com.ibm.ws.st.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.validation.ValidationFramework;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for updating the secure port in the server configuration
 */
public class QuickFixUpdateSecurePort extends AbstractMarkerResolution {

    @Override
    public String getLabel() {
        return Messages.updateSecurePortQuickFix;
    }

    @Override
    public void run(IMarker marker) {
        IResource resource = getResource(marker);
        if (resource == null)
            return;

        final ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        WebSphereServerInfo wsInfo = configFile.getWebSphereServer();
        if (wsInfo == null)
            return;

        WebSphereServer webSphereServer = WebSphereUtil.getWebSphereServer(wsInfo);
        if (webSphereServer == null)
            return;

        IServer server = webSphereServer.getServer();
        IServerWorkingCopy workingCopy = server.createWorkingCopy();

        int httpsPort = configFile.getHTTPSPort();
        WebSphereServer adapter = (WebSphereServer) workingCopy.loadAdapter(WebSphereServer.class, null);
        adapter.setServerSecurePort(Integer.toString(httpsPort));

        try {
            workingCopy.save(true, null);
            ValidationFramework validationFramework = org.eclipse.wst.validation.ValidationFramework.getDefault();
            validationFramework.validate(configFile.getIFile(), new NullProgressMonitor());
        } catch (CoreException coreException) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Update secure port failed.", coreException);
            showErrorMessage();
        }
    }

    @Override
    protected String getErrorMessage() {
        return Messages.updateSecurePortFailedMsg;
    }

}
