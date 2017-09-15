/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;

public class ServerDecorator extends LabelProvider implements ILightweightLabelDecorator {
    @Override
    public void decorate(Object obj, IDecoration decoration) {
        if (obj instanceof IServer) {
            IServer server = (IServer) obj;
            if (server.getServerType() == null || !server.getServerType().getId().startsWith(Constants.SERVER_ID_PREFIX))
                return;

            WebSphereServer serverDelegate = (WebSphereServer) server.loadAdapter(WebSphereServer.class, new NullProgressMonitor());

            if (serverDelegate != null) {
                String name = serverDelegate.getServerDisplayName();
                if (name != null && !name.isEmpty())
                    decoration.addSuffix(" [" + name + "]");
                if (serverDelegate.isLocalSetup() && serverDelegate.getServerInfo() != null) {
                    // Check for any errors and add an image decorator and message on the server.  The message
                    // just indicates the number of errors and tells the user to hover for details.
                    // The server tool tip contains all of the error messages.
                    String[] errors = serverDelegate.getServerInfo().getServerErrors();
                    if (errors.length > 0) {
                        ImageDescriptor id = Activator.getImageDescriptor(Activator.IMG_ERROR_OVERLAY);
                        decoration.addOverlay(id, IDecoration.BOTTOM_LEFT);
                        decoration.addSuffix(" " + NLS.bind(Messages.serverDecoratorErrors, Integer.toString(errors.length)));
                    }
                }
            }
        } else if (obj instanceof IFile) {
            IFile file = (IFile) obj;
            ImageDescriptor id = Activator.getImageDescriptor(file.getName());
            if (id != null)
                decoration.addOverlay(id);
        }
    }
}
