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
package com.ibm.ws.st.ui.internal.actions;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.ui.internal.DDETreeContentProvider;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;
import com.ibm.ws.st.ui.internal.Trace;

public class ShowInFilesystemAction extends SelectionProviderAction {
    private File file;

    public ShowInFilesystemAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionOpenFolder);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        file = null;
        Object obj = sel.getFirstElement();
        if (obj instanceof IRuntime) {
            IRuntime runtime = (IRuntime) obj;
            file = runtime.getLocation().toFile();
        } else if (obj instanceof IServer) {
            IServer srv = (IServer) obj;
            WebSphereServer server = (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
            if (server != null && server.getServerPath() != null)
                file = server.getServerPath().toFile();
        } else if (obj instanceof ConfigurationFolder) {
            ConfigurationFolder folder = (ConfigurationFolder) obj;
            file = folder.getPath().toFile();
        } else if (obj instanceof ConfigurationFile) {
            ConfigurationFile configFile = (ConfigurationFile) obj;
            file = configFile.getPath().toFile().getParentFile();
        } else if (obj instanceof ExtendedConfigFile) {
            ExtendedConfigFile configFile = (ExtendedConfigFile) obj;
            file = configFile.getFile().getParentFile();
        } else if (obj instanceof Element) {
            Element element = (Element) obj;
            URI uri = DDETreeContentProvider.getURI(element);
            file = new File(uri).getParentFile();
        } else if (obj instanceof UserDirectory) {
            UserDirectory userDir = (UserDirectory) obj;
            file = userDir.getPath().toFile();
        } else if (obj instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) obj;
            UserDirectory userDir = node.getUserDirectory();
            if (RuntimeExplorer.NodeType.SHARED_APPLICATIONS == node.getType())
                file = userDir.getSharedAppsPath().toFile();
            else if (RuntimeExplorer.NodeType.SHARED_CONFIGURATIONS == node.getType())
                file = userDir.getSharedConfigPath().toFile();
            else if (RuntimeExplorer.NodeType.SERVERS == node.getType())
                file = userDir.getServersPath().toFile();
        } else if (obj instanceof WebSphereServerInfo) {
            WebSphereServerInfo server = (WebSphereServerInfo) obj;
            file = server.getServerPath().toFile();
        }
        setEnabled(file != null);
    }

    @Override
    public void run() {
        if (file == null) {
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            Trace.logError("Error opening folder " + file.toString(), e);
        }
    }
}
