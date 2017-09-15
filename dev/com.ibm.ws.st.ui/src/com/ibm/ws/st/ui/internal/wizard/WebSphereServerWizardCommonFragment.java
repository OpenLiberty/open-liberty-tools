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
package com.ibm.ws.st.ui.internal.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.common.ui.ext.internal.servertype.ServerTypeUIExtension;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

/**
 *
 */
public class WebSphereServerWizardCommonFragment extends WizardFragment {

    public static final String SERVER_TYPE_DATA = "com.ibm.ws.st.ui.serverTypeData";
    public static final String SERVICE_INFO = "com.ibm.ws.st.ui.serviceInfo";

    protected boolean runtimeHasServers() {
        // Default to true so that the user has the New button if anything goes wrong
        boolean hasServers = true;
        IRuntime rt = (IRuntime) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        if (rt != null) {
            WebSphereRuntime wrt = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);
            if (wrt != null) {
                hasServers = wrt.hasServers();
            }
        }
        return hasServers;
    }

    protected boolean directoryHasServer(IPath userDir) {
        File[] folders = userDir.toFile().listFiles();
        // if userDir does not exist, listFiles will
        // return null
        if (folders != null) {
            for (File f : folders) {
                if (f.isDirectory()) {
                    File serverFile = new File(f, Constants.SERVER_XML);
                    if (serverFile.exists())
                        return true;
                }
            }
        }
        return false;
    }

    protected boolean isLocalhost() {
        return isLocalhost(getTaskModel());
    }

    public static boolean isLocalhost(TaskModel taskModel) {
        if (taskModel == null) {
            return true;
        }
        IServerWorkingCopy swc = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
        String host = swc.getHost();

        if (host != null)
            return SocketUtil.isLocalhost(host);

        Trace.logError("The value for host in the server task model is null", new Exception("Host value is null"));
        return true;
    }

    protected List<ServerTypeUIExtension> getServerTypeExtensions() {
        List<ServerTypeUIExtension> exts = new ArrayList<ServerTypeUIExtension>();
        for (ServerTypeUIExtension ext : ServerTypeUIExtension.getServerTypeUIExtensions()) {
            exts.add(ext);
        }
        return exts;
    }

    protected List<ServerTypeUIExtension> getActiveServerTypeExtensions() {
        TaskModel taskModel = getTaskModel();
        List<ServerTypeUIExtension> exts = new ArrayList<ServerTypeUIExtension>();
        for (ServerTypeUIExtension ext : ServerTypeUIExtension.getServerTypeUIExtensions()) {
            if (ext.isActive(taskModel)) {
                exts.add(ext);
            }
        }
        return exts;
    }
}
