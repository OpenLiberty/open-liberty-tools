/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.gradle.ui.internal;

import org.eclipse.swt.graphics.Image;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.AbstractCustomServerConfig;
import com.ibm.ws.st.liberty.gradle.internal.Activator;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;
import com.ibm.ws.st.liberty.gradle.internal.Messages;

public class CustomServerConfig extends AbstractCustomServerConfig {

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyGradle.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected Image getCustomConfigurationNodeImage() {
        return Activator.getImage(Activator.IMG_GRADLE_FOLDER);
    }

    /** {@inheritDoc} */
    @Override
    protected String getCustomConfigurationNodeLabel() {
        return Messages.gradleTargetLabel;
    }

}
