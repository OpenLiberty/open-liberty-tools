/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.validation.ICustomServerVariablesHandler;
import com.ibm.ws.st.ui.internal.utility.PathUtil;

@SuppressWarnings("restriction")
public abstract class AbstractCustomServerVariablesHandler implements ICustomServerVariablesHandler {

    protected abstract IProject getMappedProject(IServer server);

    protected abstract LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project);

    protected abstract void addInlineVars(IProject project, ConfigVars configVars, LibertyBuildPluginConfiguration libertyBuildProjectConfiguration);

    @Override
    public void addCustomServerVariables(ConfigVars configVars, IProject project) {

        LibertyBuildPluginConfiguration libertyBuildProjectConfiguration = getLibertyBuildPluginConfiguration(project);

        if (libertyBuildProjectConfiguration != null) {

            // Load variables from server.env file (when applicable)
            String serverEnvFile = libertyBuildProjectConfiguration.getConfigValue(ConfigurationType.serverEnv);
            if (serverEnvFile != null) {
                URI serverEnvFileURI;
                serverEnvFileURI = PathUtil.getURIForFilePath(serverEnvFile);
                if (serverEnvFileURI != null) {
                    DocumentLocation serverEnvFileLocation = DocumentLocation.createDocumentLocation(serverEnvFileURI, DocumentLocation.Type.SERVER_ENV);
                    loadPropertiesFile(configVars, serverEnvFile, serverEnvFileLocation);
                }
            }

            // Load variables from bootstrap.properties file (when applicable)
            // Overlapping variables from server.env file will be overridden (bootstrap.properties variables have higher priority)
            String bootstrapFile = libertyBuildProjectConfiguration.getConfigValue(ConfigurationType.bootstrapPropertiesFile);
            if (bootstrapFile != null) {
                URI bootstrapFileURI;
                bootstrapFileURI = PathUtil.getURIForFilePath(bootstrapFile);
                if (bootstrapFileURI != null) {
                    DocumentLocation bootstrapFileLocation = DocumentLocation.createDocumentLocation(bootstrapFileURI, DocumentLocation.Type.BOOTSTRAP);
                    loadPropertiesFile(configVars, bootstrapFile, bootstrapFileLocation);
                }
            }

            addInlineVars(project, configVars, libertyBuildProjectConfiguration);
        }
    }

    @Override
    public void addCustomServerVariables(ConfigVars configVars, WebSphereServerInfo serverInfo) {
        if (serverInfo == null)
            return;
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        if (wsServer == null)
            return;
        IProject project = getMappedProject(wsServer.getServer());
        if (project == null)
            return;

        addCustomServerVariables(configVars, project);
    }

    private void loadPropertiesFile(ConfigVars configVars, String fileName, DocumentLocation documentLocation) {
        try {
            File file = new File(fileName);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();

            Iterator<Object> iterator = properties.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String value = properties.getProperty(key);
                configVars.add(key, value, documentLocation);
            }

        } catch (Exception exception) {
            Trace.logError("Could not read bootstrap properties file " + fileName, exception);
        }
    }

}
