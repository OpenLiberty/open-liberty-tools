/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.etools.maven.liberty.integration.ui.rtexplorer.internal;

import org.eclipse.swt.graphics.Image;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.AbstractLibertyBuildPluginRuntimeLabelProvider;
import com.ibm.etools.maven.liberty.integration.internal.Activator;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;

public class MavenRuntimeLabelProvider extends AbstractLibertyBuildPluginRuntimeLabelProvider {

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected Image getRuntimeImage() {
        return Activator.getImage(Activator.IMG_MAVEN_RUNTIME);
    }
}
