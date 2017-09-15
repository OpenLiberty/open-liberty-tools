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

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for adding secure port to server.xml
 */
public class QuickFixAddSecurePortElement extends AbstractMarkerResolution {

    @Override
    public String getLabel() {
        return Messages.addSecurePortQuickFix;
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

        Element httpEndpointElement = configFile.getDocument().createElement(Constants.HTTP_ENDPOINT);
        httpEndpointElement.setAttribute(Constants.HTTPS_PORT, webSphereServer.getServerSecurePort());
        configFile.getDocument().getDocumentElement().appendChild(httpEndpointElement);

        Text textNode = configFile.getDocument().createTextNode("\n");
        configFile.getDocument().getDocumentElement().appendChild(textNode);

        try {
            configFile.save(null);
        } catch (IOException ioException) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Add secure port quick fix failed. Error trying to update configuration: " + configFile.getURI(), ioException);
            showErrorMessage();
        }

    }

    @Override
    protected String getErrorMessage() {
        return Messages.addSecurePortFailedMsg;
    }

}
