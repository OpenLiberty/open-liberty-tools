/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class PublishHelper {
    private static final String PROMPT_RESPONSES_FILENAME = "promptResponses.properties";

    private final WebSphereServerBehaviour wsBehaviour;
    private final Properties promptResponses;
    private final PromptHandler promptHandler = Activator.getPromptHandler();
    private boolean configChanged;

    public PublishHelper(WebSphereServerBehaviour wsBehaviour) {
        this.wsBehaviour = wsBehaviour;
        promptResponses = new Properties();
        FileUtil.loadProperties(promptResponses, wsBehaviour.getTempDirectory().append(PROMPT_RESPONSES_FILENAME));
    }

    public void checkPublishedModules(IProgressMonitor monitor) {
        if (promptHandler != null && !PromptUtil.isSuppressDialog()) {
            final List<PromptHandler.AbstractPrompt> registeredPrompts = getRegisteredPrompts();
            final List<IModule[]> modules = wsBehaviour.getPublishedModules();

            configChanged = false;
            for (PromptHandler.AbstractPrompt prompt : registeredPrompts) {
                prompt.getActionHandler().prePromptAction(modules, this, monitor);
            }

            final PromptHandler.AbstractPrompt[] activePrompts = getActivePrompts(registeredPrompts);
            if (activePrompts.length > 0) {
                IPromptResponse resp = promptHandler.getResponse(Messages.publishPromptMessage, activePrompts, PromptHandler.STYLE_CANCEL | PromptHandler.STYLE_HELP
                                                                                                                          | PromptHandler.STYLE_QUESTION);
                if (resp == null) {
                    monitor.setCanceled(true);
                    return;
                }

                for (PromptHandler.AbstractPrompt prompt : activePrompts) {
                    prompt.getActionHandler().postPromptAction(resp, this);
                }
            }

            if (configChanged) {
                final WebSphereServer ws = wsBehaviour.getWebSphereServer();
                final ConfigurationFile configFile = ws.getConfiguration();
                try {
                    configFile.save(monitor);
                    // we need to inform server of this change. Otherwise, 
                    // server will not pick up any app changes because of the AUCD order
                    if (ws.getServer().getServerState() == IServer.STATE_STARTED) {
                        JMXConnection jmx = null;
                        try {
                            jmx = ws.createJMXConnection();
                            if (jmx != null) {
                                ArrayList<String> file = new ArrayList<String>();
                                file.add(configFile.getIFile().getLocation().toOSString());
                                jmx.notifyFileChanges(null, file, null);
                            }
                        } catch (Exception e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Failed to use JMX to update the configuration file. ", e);
                        } finally {
                            if (jmx != null)
                                jmx.disconnect();
                        }
                    }
                } catch (IOException e) {
                    Trace.logError("Error saving configuration " + configFile.getURI(), e);
                }
                if (configFile.getIFile() == null)
                    ws.refreshConfiguration();
            }
        }
    }

    protected void addPromptResponse(String key, String value) {
        promptResponses.put(key, value);
    }

    protected String getPromptResponse(String key) {
        return promptResponses.getProperty(key);
    }

    protected void savePromptResponses() {
        FileUtil.saveCachedProperties(promptResponses, wsBehaviour.getTempDirectory().append(PROMPT_RESPONSES_FILENAME));
    }

    protected void setConfigChanged(boolean value) {
        configChanged = value;
    }

    protected WebSphereServer getWebSphereServer() {
        return wsBehaviour.getWebSphereServer();
    }

    protected WebSphereServerBehaviour getWebSphereServerBehaviour() {
        return wsBehaviour;
    }

    private List<PromptHandler.AbstractPrompt> getRegisteredPrompts() {
        List<PromptHandler.AbstractPrompt> list = new ArrayList<PromptHandler.AbstractPrompt>(3);
        list.add(new RequiredFeaturePrompt());
        list.add(new OutOfSyncApplicationPrompt());
        list.add(new AppMonitorMBeanPrompt());
        return list;
    }

    private PromptHandler.AbstractPrompt[] getActivePrompts(List<PromptHandler.AbstractPrompt> registeredPrompts) {
        List<PromptHandler.AbstractPrompt> list = new ArrayList<PromptHandler.AbstractPrompt>(registeredPrompts.size());
        for (PromptHandler.AbstractPrompt prompt : registeredPrompts) {
            if (prompt.isActive()) {
                list.add(prompt);
            }
        }

        return list.toArray(new PromptHandler.AbstractPrompt[list.size()]);
    }
}
