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
package com.ibm.etools.maven.liberty.integration.ui.internal;

import org.eclipse.swt.graphics.Image;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.AbstractCustomServerConfig;
import com.ibm.etools.maven.liberty.integration.internal.Activator;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.internal.Messages;

public class CustomServerConfig extends AbstractCustomServerConfig {

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected Image getCustomConfigurationNodeImage() {
        return Activator.getImage(Activator.IMG_MAVEN_FOLDER);
    }

    /** {@inheritDoc} */
    @Override
    protected String getCustomConfigurationNodeLabel() {
        return Messages.mavenTargetLabel;
    }

}
