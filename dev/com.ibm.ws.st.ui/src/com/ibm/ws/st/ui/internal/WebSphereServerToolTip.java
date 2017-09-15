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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.provisional.IServerToolTip;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;

@SuppressWarnings("restriction")
public class WebSphereServerToolTip implements IServerToolTip {

    @Override
    public void createContent(Composite parent, IServer server) {
        Composite comp = parent;

        WebSphereServer serv = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        if (serv == null)
            return;

        WebSphereRuntime runtime = serv.getWebSphereRuntime();
        if (runtime == null)
            return;

        Label label = new Label(comp, SWT.NONE);
        label.setText(NLS.bind(Messages.serverTooltipName, serv.getServerName()));
        label.setBackground(parent.getBackground());

        label = new Label(comp, SWT.NONE);
        label.setText(NLS.bind(Messages.serverTooltipLocation, runtime.getRuntime().getLocation().toOSString()));
        label.setBackground(parent.getBackground());

        if (serv.getConfigurationRoot() != null) {
            label = new Label(comp, SWT.NONE);
            label.setText(NLS.bind(Messages.serverTooltipConfigRoot, serv.getConfigurationRoot().toOSString()));
            label.setBackground(parent.getBackground());
        }

        if (serv.isLocalSetup() && serv.getServerInfo() != null) {
            // Add any server errors to the tool tip
            String[] errors = serv.getServerInfo().getServerErrors();
            if (errors.length > 0) {
                label = new Label(comp, SWT.NONE);
                label.setText(Messages.serverTooltipErrors);
                label.setBackground(parent.getBackground());

                for (String error : errors) {
                    label = new Label(comp, SWT.NONE);
                    label.setText("\t" + error);
                    label.setBackground(parent.getBackground());
                }
            }
        }
    }
}
