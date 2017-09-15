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
package com.ibm.ws.st.ui.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;

public class NavigatorDropAdapter extends CommonDropAdapterAssistant {

    protected URI sourceURI;
    protected URI sourceSharedURI;
    protected ConfigurationFile targetFile;
    protected UserDirectory targetUserDirectory;
    protected ConfigurationFolder targetFolder;

    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
        targetFile = null;
        targetUserDirectory = null;
        targetFolder = null;
        sourceURI = null;
        sourceSharedURI = null;

        // validate target: can be server (indicates the root config file),
        // a configuration element, or a shared config folder
        if (target instanceof IServer) {
            IServer server = (IServer) target;
            WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            if (wsServer != null)
                targetFile = wsServer.getConfiguration();
        } else if (target instanceof Element) {
            Element element = (Element) target;
            String uri = (String) element.getOwnerDocument().getUserData(ConfigurationFile.USER_DATA_URI);
            if (uri != null)
                targetFile = ConfigUtils.getConfigFile(uri);
        } else if (target instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) target;
            if (RuntimeExplorer.NodeType.SHARED_CONFIGURATIONS.equals(node.getType())) {
                targetUserDirectory = node.getUserDirectory();
            }
        } else if (target instanceof ConfigurationFolder) {
            targetFolder = (ConfigurationFolder) target;
            targetUserDirectory = targetFolder.getUserDirectory();
        } else if (target instanceof ConfigurationFile) {
            targetFile = (ConfigurationFile) target;
        }

        if (targetFile == null && targetUserDirectory == null)
            return Status.CANCEL_STATUS;

        // validate source: can be a configuration element, a configuration file, or an IFile
        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
            if (!s.isEmpty() && s instanceof IStructuredSelection) {
                IStructuredSelection sel = (IStructuredSelection) s;
                Object obj = sel.getFirstElement();
                if (obj instanceof Element) {
                    Element element = (Element) obj;
                    ConfigurationFile configFile = null;
                    String uri = (String) element.getOwnerDocument().getUserData(ConfigurationFile.USER_DATA_URI);
                    if (uri != null)
                        configFile = ConfigUtils.getConfigFile(uri);
                    if (configFile != null) {
                        sourceURI = configFile.getURI();
                        if (ConfigurationFile.LOCATION_TYPE.SHARED == configFile.getLocationType())
                            sourceSharedURI = configFile.getUserDirectory().getSharedConfigURI();
                    } else
                        return Status.CANCEL_STATUS;
                } else if (obj instanceof ConfigurationFile) {
                    ConfigurationFile configFile = (ConfigurationFile) obj;
                    sourceURI = configFile.getURI();
                    if (ConfigurationFile.LOCATION_TYPE.SHARED == configFile.getLocationType())
                        sourceSharedURI = configFile.getUserDirectory().getSharedConfigURI();
                } else if (obj instanceof IFile) {
                    IFile file = (IFile) obj;
                    if (ConfigUtils.isServerConfigFile(file)) {
                        sourceURI = file.getLocation().toFile().toURI();
                        int index = sourceURI.toString().indexOf("shared/config");
                        if (index > 0)
                            sourceSharedURI = sourceURI; // TODO parent
                    }
                }
            }
        }
        if (sourceURI == null)
            return Status.CANCEL_STATUS;

        // make sure source and target aren't the same file
        if (targetFile != null && targetFile.getURI().equals(sourceURI))
            return Status.CANCEL_STATUS;

        return Status.OK_STATUS;
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent, Object target) {
        try {
            if (targetUserDirectory != null)
                return addSharedConfigFile();
            return addIncludeElement();
        } catch (Exception e) {
            Trace.logError("Error dropping configuration file: " + sourceURI, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error dropping configuration file", e);
        }
    }

    /**
     * Create an include element within the target file that references the source file.
     * 
     * @return a status, indicating success or failure
     * @throws IOException
     */
    private IStatus addIncludeElement() throws IOException {
        // check if file is already included
        ConfigurationFile[] included = targetFile.getAllIncludedFiles();
        for (ConfigurationFile icf : included) {
            if (icf.getURI().equals(sourceURI)) {
                MessageDialog.openError(getShell(), Messages.title, NLS.bind(Messages.errorAlreadyIncluded, sourceURI.toASCIIString()));
                return Status.CANCEL_STATUS;
            }
        }

        if (!MessageDialog.openConfirm(getShell(), Messages.title, Messages.taskAddInclude))
            return Status.CANCEL_STATUS;

        // if the target is not shared, but the source is shared in the target
        // runtime's shared configuration location, use the ${shared.config.dir} variable
        String includeString = null;
        if (ConfigurationFile.LOCATION_TYPE.SHARED != targetFile.getLocationType() &&
            sourceSharedURI != null &&
            targetFile.getUserDirectory().getSharedConfigURI().equals(sourceSharedURI)) {
            URI relative = URIUtil.canonicalRelativize(sourceSharedURI, sourceURI);
            includeString = "${shared.config.dir}/" + relative.toASCIIString();
        } else { // otherwise, check for relative path
            URI targetURI = targetFile.getURI();
            URI relative = URIUtil.canonicalRelativize(targetURI, sourceURI);
            if (!relative.isAbsolute())
                includeString = relative.toASCIIString();
            else {
                includeString = sourceURI.getPath();
                if (System.getProperty("os.name").toLowerCase().contains("windows") && includeString.startsWith("/"))
                    includeString = includeString.substring(1);
            }
        }

        // make sure external config file is in sync
        WebSphereServerInfo wsServer = null;
        WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
        for (WebSphereServerInfo server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(targetFile.getURI());
            if (configFile != null)
                wsServer = server;
        }
        if (wsServer != null)
            wsServer.updateCache();

        // add <include> into file
        targetFile.addInclude(false, includeString);
        targetFile.save(null);

        if (wsServer != null)
            wsServer.updateCache();
        return Status.OK_STATUS;
    }

    /**
     * Copy the source configuration file into the shared configuration folder of
     * the target runtime.
     * 
     * @return a status, indicating success or failure
     * @throws MalformedURLException
     */
    private IStatus addSharedConfigFile() throws MalformedURLException {
        // TODO should maybe prompt for move or copy. just copy for now
        if (MessageDialog.openConfirm(getShell(), Messages.title, Messages.taskAddSharedConfigFile))
            return targetUserDirectory.addSharedConfigFile(targetFolder, sourceURI.toURL());

        return Status.CANCEL_STATUS;
    }
}
