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
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.customization.ICustomSuggestedValuesObject;

public class JDBCDriverSuggestedValues implements ICustomSuggestedValuesObject {

    /** {@inheritDoc} */
    @Override
    public List<String> getSuggestedValues(String value, Node itemNode, Element closestAncestor, IResource resource) {
        ArrayList<String> list = new ArrayList<String>();

        Document doc = closestAncestor.getOwnerDocument();

        URI docURI = null;
        if (resource != null) {
            docURI = resource.getLocation().toFile().toURI();
        } else {
            DOMImplementation domImpl = doc.getImplementation();
            if (domImpl.hasFeature("Core", "3.0")) {
                String uriStr = doc.getDocumentURI();
                if (uriStr != null) {
                    try {
                        docURI = new URI(uriStr);
                    } catch (URISyntaxException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to creat URI from string: " + uriStr, e);
                    }
                }
            }
        }

        WebSphereServerInfo serverInfo = null;
        UserDirectory userDir = null;
        if (docURI != null) {
            WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
            for (WebSphereServerInfo server : servers) {
                ConfigurationFile configFile = server.getConfigurationFileFromURI(docURI);
                if (configFile != null) {
                    serverInfo = configFile.getWebSphereServer();
                    userDir = configFile.getUserDirectory();
                    break;
                }
            }
        }

        String[] ids = DOMUtils.getIds(doc, docURI, serverInfo, userDir, "jdbcDriver");
        for (String id : ids) {
            list.add(id);
        }

        return list;
    }

}
