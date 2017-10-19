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

package com.ibm.etools.maven.liberty.integration.manager.internal;

import org.eclipse.osgi.util.NLS;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.etools.maven.liberty.integration.internal.Messages;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyManager;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.UIHelper;

public class LibertyManager extends AbstractLibertyManager {

    private static final LibertyManager instance = new LibertyManager();

    private LibertyManager() {
        // singleton
    }

    public static final LibertyManager getInstance() {
        return instance;
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean handleGenerationPrompt(String projectName) {
        return UIHelper.handleGenerationPrompt(LibertyMavenConstants.PROMPT_PREFERENCE, NLS.bind(Messages.generationPromptMsg, projectName));
    }

}
