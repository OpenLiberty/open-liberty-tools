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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.IConfigurationElement;

public class WebSphereViewerSorter extends ViewerSorter {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        // sort feature manager to the top, all else stays as-is
        if (e1 instanceof Element && e2 instanceof Element) {
            Element el1 = (Element) e1;
            Element el2 = (Element) e2;
            if (Constants.FEATURE_MANAGER.equals(el1.getNodeName()) && !Constants.FEATURE_MANAGER.equals(el2.getNodeName())) {
                return -1;
            } else if (!Constants.FEATURE_MANAGER.equals(el1.getNodeName()) && Constants.FEATURE_MANAGER.equals(el2.getNodeName())) {
                return 1;
            }
            return 0;
        }

        if (e1 instanceof WebSphereServerInfo && e2 instanceof WebSphereServerInfo)
            return super.compare(viewer, ((WebSphereServerInfo) e1).getServerName(), ((WebSphereServerInfo) e2).getServerName());

        if (e1 instanceof UserDirectory && e2 instanceof UserDirectory) {
            UserDirectory u1 = (UserDirectory) e1;
            UserDirectory u2 = (UserDirectory) e2;
            if (u1.getProject() != null && u2.getProject() == null)
                return -1;
            if (u1.getProject() == null && u2.getProject() != null)
                return 1;
            if (u1.getProject() != null && u2.getProject() != null)
                return u1.getProject().getName().compareTo(u2.getProject().getName());
            return u1.getPath().toOSString().compareTo(u2.getPath().toOSString());
        }

        if (e1 instanceof IConfigurationElement && e2 instanceof IConfigurationElement) {
            // always sort folders before files, then compare names
            if (e1 instanceof ConfigurationFolder && e2 instanceof ConfigurationFile)
                return -1;
            if (e1 instanceof ConfigurationFile && e2 instanceof ConfigurationFolder)
                return 1;
            return super.compare(viewer, e1, e2);
        }

        if (e1 instanceof ExtendedConfigFile && e2 instanceof Element)
            return 1;

        if (e1 instanceof Element && e2 instanceof ExtendedConfigFile)
            return -1;

        return 0;
    }
}
