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

package com.ibm.ws.st.liberty.gradle.ui.rtexplorer.internal;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.AbstractLibertyBuildPluginRuntimeContentProvider;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants;

public class GradleRuntimeContentProvider extends AbstractLibertyBuildPluginRuntimeContentProvider {
	
	@Override
	public String getRuntimeContentId() {
		return LibertyGradleConstants.LIBERTY_GRADLE_RUNTIME_CONTENT_ID;
	}
	
    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyGradle.getInstance();
    }

}
