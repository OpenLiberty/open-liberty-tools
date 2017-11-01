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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.AbstractProjectMapXML;
import com.ibm.ws.st.liberty.gradle.internal.Activator;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants;
import com.ibm.ws.st.liberty.gradle.xml.internal.ProjectMapXML;

public class LibertyGradleProjectMapping extends AbstractLibertyProjectMapping {

    private static final LibertyGradleProjectMapping instance = new LibertyGradleProjectMapping();

    private LibertyGradleProjectMapping() {
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
        return new Path(LibertyGradleConstants.LIBERTY_PLUGIN_CONFIG_PATH);
    }

    /** {@inheritDoc} */
    @Override
    public IPath getLibertyBuildProjectCachePath(String projectName) {
        return getLibertyGradleCachePath().append(projectName);
    }

    private IPath getLibertyGradleCachePath() {
        IPath cachePath = Activator.getLibertyGradleMetadataPath().append("cache");
        File cacheFile = cachePath.toFile();
        if (cacheFile != null && !cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        return cachePath;
    }

}
