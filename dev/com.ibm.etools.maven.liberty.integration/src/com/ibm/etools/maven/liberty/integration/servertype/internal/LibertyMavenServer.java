/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.etools.maven.liberty.integration.servertype.internal;

import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 * Liberty Maven Server Implementation
 */

@SuppressWarnings("restriction")
public class LibertyMavenServer extends AbstractServerExtension {

    // In order to use the existing loose config publishing mechanism, the isLocalSetup needs to be
    // true, otherwise it attempts to do remote publishing
    @Override
    public Boolean isLocalSetup(IServer server) {
        if (server != null) {
            WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
            return new Boolean(wsServer.isLocalHost());
        }
        return null;
    }
}