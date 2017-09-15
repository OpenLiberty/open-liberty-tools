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
package com.ibm.ws.st.ui.internal.config;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

public class IncludeLink implements IAdvancedCustomizationObject {
    @Override
    public String invoke(String value, Node node, Element closestAncestor, IEditorPart editorPart) {
        if (value == null || value.trim().length() == 0) {
            // TODO empty, offer to create a new file?
            return null;
        }

        IEditorInput input = editorPart.getEditorInput();
        URI uri = ConfigUIUtils.getURI(input);
        if (uri != null) {
            URI includeURI = null;
            WebSphereServerInfo serverInfo = ConfigUtils.getServerInfo(uri);
            if (serverInfo != null) {
                includeURI = serverInfo.resolve(uri, value);
            } else {
                UserDirectory userDir = ConfigUtils.getUserDirectory(uri);
                if (userDir != null) {
                    includeURI = userDir.resolve(uri, value);
                }
            }
            if (includeURI != null) {
                IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
                IFile iFile = workspaceRoot.getFileForLocation(new Path(includeURI.getPath()));
                if (iFile != null && iFile.exists()) {
                    Activator.openConfigurationEditor(iFile, includeURI);
                } else {
                    File file = new File(includeURI);
                    if (file.exists()) {
                        Activator.openConfigurationEditor(null, includeURI);
                    }
                }
            }
        }

        return null;
    }
}
