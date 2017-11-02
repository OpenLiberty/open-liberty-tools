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

package com.ibm.ws.st.liberty.gradle.manager.internal;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyManager;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.UIHelper;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants;
import com.ibm.ws.st.liberty.gradle.internal.Messages;

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
        return LibertyGradle.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean handleGenerationPrompt(String projectName) {
        return UIHelper.handleGenerationPrompt(LibertyGradleConstants.PROMPT_PREFERENCE, NLS.bind(Messages.generationPromptMsg, projectName));
    }

}
