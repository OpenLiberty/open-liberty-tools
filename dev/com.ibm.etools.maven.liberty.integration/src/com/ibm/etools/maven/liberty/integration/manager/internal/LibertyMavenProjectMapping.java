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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.etools.maven.liberty.integration.internal.Activator;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.etools.maven.liberty.integration.xml.internal.ProjectMapXML;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.AbstractProjectMapXML;

public class LibertyMavenProjectMapping extends AbstractLibertyProjectMapping {

    private static final LibertyMavenProjectMapping instance = new LibertyMavenProjectMapping();

    private LibertyMavenProjectMapping() {
        super();
        // singleton
    }

    public static AbstractLibertyProjectMapping getInstance() {
        return instance;
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractProjectMapXML getProjectMapXML() {
        return ProjectMapXML.instance();
    }

    /** {@inheritDoc} */
    @Override
    protected IPath getPluginConfigPath() {
        return new Path(LibertyMavenConstants.LIBERTY_PLUGIN_CONFIG_PATH);
    }

    /** {@inheritDoc} */
    @Override
    public IPath getLibertyBuildProjectCachePath(String projectName) {
        return getLibertyMavenCachePath().append(projectName);
    }

    private IPath getLibertyMavenCachePath() {
        IPath cachePath = Activator.getLibertyMavenMetadataPath().append("cache");
        File cacheFile = cachePath.toFile();
        if (cacheFile != null && !cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        return cachePath;
    }

}
