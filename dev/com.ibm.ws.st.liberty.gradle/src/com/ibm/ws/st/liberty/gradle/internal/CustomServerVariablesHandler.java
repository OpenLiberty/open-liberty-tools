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

package com.ibm.ws.st.liberty.gradle.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.AbstractCustomServerVariablesHandler;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;

@SuppressWarnings("restriction")
public class CustomServerVariablesHandler extends AbstractCustomServerVariablesHandler {

    @Override
    protected LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project) {
    		NullProgressMonitor monitor = new NullProgressMonitor();
    		// We only care about projects with the Gradle nature
    	    if (LibertyGradle.isGradleProject(project, monitor)) {
    	    		return LibertyGradle.getLibertyGradleProjectConfiguration(project, monitor);
    	    }
    	    return null;
    }

    /** {@inheritDoc} */
    @Override
    protected IProject getMappedProject(IServer server) {
        return LibertyGradle.getMappedProject(server);
    }

    @Override
    protected void addInlineVars(IProject project, ConfigVars configVars, LibertyBuildPluginConfiguration libertyBuildProjectConfiguration) {

        URI buildScriptURI = obtainBuildScriptURI(project);
        if (libertyBuildProjectConfiguration != null && buildScriptURI != null) {
            // Load in-line bootstrap variables (when applicable)
            // Overlapping variables from server.env file will be overridden (in-line bootstrap variables have higher priority)
            // Overlapping variables from bootstrap.properties file will be overridden (in-line bootstrap variables have higher priority)
            // Note that in practice overlapping between bootstrap.properties file and in-line bootstrap variables should not happen
            DocumentLocation documentLocation = DocumentLocation.createDocumentLocation(buildScriptURI, DocumentLocation.Type.BOOTSTRAP);
            Map<String, String> bootstrapProperties = libertyBuildProjectConfiguration.getBootstrapProperties();
            Iterator<String> iterator = bootstrapProperties.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = bootstrapProperties.get(key);
                configVars.add(key, value, documentLocation);
            }
        }
    }

	private URI obtainBuildScriptURI(IProject project) {
        if (project == null)
            return null;
        try {
            return new URI(project.getLocationURI().toString() + "/" + LibertyGradleConstants.GRADLE_BUILD_SCRIPT);
        } catch (URISyntaxException exception) {
            Trace.logError("Could not obtain URI to " + LibertyGradleConstants.GRADLE_BUILD_SCRIPT + " in project " + project.getName(), exception);
        }
        return null;
    }

}
