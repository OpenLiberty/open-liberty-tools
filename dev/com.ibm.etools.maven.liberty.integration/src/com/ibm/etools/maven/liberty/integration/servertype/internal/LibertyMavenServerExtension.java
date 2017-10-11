/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.servertype.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.ServerExtension;

@SuppressWarnings("restriction")
/**
 * Liberty Maven server extension implementation
 *
 * Note: currently not being used by anything, but required for the extension
 */
public class LibertyMavenServerExtension extends ServerExtension {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.st.core.internal.ServerExtension#getChildModules(org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IModule[] getChildModules(IModule[] module) {
        // Not currently used, so return the default null
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.st.core.internal.ServerExtension#getRootModules(org.eclipse.wst.server.core.IModule)
     */
    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        // Not currently used, so return the default null
        return null;
    }

}