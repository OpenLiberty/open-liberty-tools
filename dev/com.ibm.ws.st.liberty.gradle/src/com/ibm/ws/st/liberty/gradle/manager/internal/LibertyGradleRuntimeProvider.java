/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.liberty.gradle.manager.internal;

import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.AbstractLibertyRuntimeProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;

public class LibertyGradleRuntimeProvider extends AbstractLibertyRuntimeProvider {

    public LibertyGradleRuntimeProvider() {
    		// Empty
    }

	// Gradle specific
	
	@Override
	protected ILibertyBuildPluginImpl getBuildPlugin() {
		return LibertyManager.getInstance().getBuildPluginImpl();
	}

	@Override
	protected IServer getMappedServer(String projectName) {
		return LibertyGradle.getMappedServer(projectName);
	}
}
