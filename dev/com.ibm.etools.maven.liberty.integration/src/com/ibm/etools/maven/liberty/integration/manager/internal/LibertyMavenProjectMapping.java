/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.manager.internal;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.AbstractProjectMapXML;
import com.ibm.etools.maven.liberty.integration.internal.Activator;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.etools.maven.liberty.integration.xml.internal.ProjectMapXML;

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
