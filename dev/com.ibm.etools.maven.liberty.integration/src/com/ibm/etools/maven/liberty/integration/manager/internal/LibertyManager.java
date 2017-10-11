/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.manager.internal;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyManager;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.UIHelper;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.etools.maven.liberty.integration.internal.Messages;

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
