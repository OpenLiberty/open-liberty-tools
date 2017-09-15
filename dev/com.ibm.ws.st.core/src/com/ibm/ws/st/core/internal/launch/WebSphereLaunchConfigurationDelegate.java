/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server.RestartJob;
import org.eclipse.wst.server.core.internal.Server.StartJob;
import org.eclipse.wst.server.core.internal.ServerPreferences;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.w3c.dom.Element;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractLaunchConfigurationExtension;
import com.ibm.ws.st.common.core.ext.internal.servertype.ServerTypeExtensionFactory;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.MissingKeystoreHandler;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.FeatureList;

/**
 * Launch configuration for WebSphere server.
 */
@SuppressWarnings("restriction")
public class WebSphereLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

    public static final String INTERNAL_LAUNCH_JOB_FAMILY = Activator.PLUGIN_ID + ".internalLaunchJobFamily";
    public static final QualifiedName INTERNAL_LAUNCH_SERVER_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "internalLaunchServerProperty");

    protected static final String VM_ERROR_PAGE = "-Dwas4d.error.page=localhost:";
    protected static AbstractLaunchConfigurationExtension defaultLaunchConfiguration = new BaseLibertyLaunchConfiguration();

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        final IServer server = ServerUtil.getServer(configuration);

        if (server == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find server");
            return;
        }

        WebSphereServer websphereServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        AbstractLaunchConfigurationExtension delegate = ServerTypeExtensionFactory.getServerLaunchOperation(websphereServer.getServerType());
        if (delegate == null) {
            delegate = defaultLaunchConfiguration;
        }
        delegate.launch(configuration, mode, launch, monitor);
    }

    @Override
    public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor progressMonitor) throws CoreException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 30);

        boolean b = super.finalLaunchCheck(configuration, mode, monitor.newChild(5));

        try {
            final IServer server = ServerUtil.getServer(configuration);
            if (server == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Launch configuration could not find server");
                // throw CoreException();
                return b;
            }

            if (ServerPreferences.getInstance().isAutoPublishing()) {
                IJobManager jobManager = Job.getJobManager();
                Job[] jobs = jobManager.find(ServerUtil.SERVER_JOB_FAMILY);

                boolean isInsideJob = false;
                // 174122, added check for the case when the job is coming from a ServerWorkingCopy
                // Note: it should not try to restart the server on a working copy
                for (Job job : jobs) {
                    if (job instanceof StartJob) {
                        IServer jserver = ((StartJob) job).getServer();
                        if (checkServer(server, jserver)) {
                            isInsideJob = true;
                            break;
                        }
                    } else if (job instanceof RestartJob) {
                        IServer jserver = ((RestartJob) job).getServer();
                        if (checkServer(server, jserver)) {
                            isInsideJob = true;
                            break;
                        }
                    }
                }

                if (!isInsideJob) {
                    jobs = jobManager.find(INTERNAL_LAUNCH_JOB_FAMILY);
                    for (Job job : jobs) {
                        Object jserver = job.getProperty(INTERNAL_LAUNCH_SERVER_PROPERTY);
                        if (jserver instanceof IServer && checkServer(server, (IServer) jserver)) {
                            isInsideJob = true;
                            break;
                        }
                    }
                }

                if (monitor.isCanceled())
                    return false;

                monitor.worked(5);

                final WebSphereServerBehaviour servB = server.getAdapter(WebSphereServerBehaviour.class);
                if (servB == null) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "finalLaunchCheck(.) behaviour is null");
                    return b;
                }
                if (isInsideJob) {
                    /*
                     * We do the prompt only if it is not done in the publishModules(). That is when publishModules() is not called
                     * due to nothing is changed etc.
                     */
                    if (servB.shouldShownFeaturePromptInLauncher()) {
                        final IProgressMonitor publishMonitor = monitor.newChild(10);
                        final Thread promptThread = new Thread() {
                            @Override
                            public void run() {
                                servB.checkPublishedModules(publishMonitor);
                            }
                        };
                        promptThread.setDaemon(true);
                        promptThread.start();

                        promptThread.join();
                        servB.setShownFeaturePromptInLauncher(true);
                    } else {
                        monitor.worked(10);
                    }

                    if (monitor.isCanceled())
                        return false;

                    /*
                     * Special check for appSecurity and ebjRemote combination which requires a keyStore
                     * element and a user registry to be configured or the server will not function properly.
                     */
                    final WebSphereServerInfo serverInfo = servB.getWebSphereServerInfo();
                    if (serverInfo == null)
                        return b;

                    final ConfigurationFile configFile = serverInfo.getConfigRoot();
                    List<String> allFeatures = configFile.getAllFeatures();
                    List<String> features = Arrays.asList(new String[] { "ssl-1.0", "appSecurity-1.0", "ejbRemote-3.2" });
                    if (FeatureList.featuresEnabled(features, allFeatures, serverInfo.getWebSphereRuntime())) {
                        // First check what keystore (if any) we need to look for.

                        // If the default ssl is overridden then it will reference an
                        // ssl element that has a keystore ref.  If the keystore is not
                        // defined then the reference checking will have picked it up.
                        //    <ssl id="sslConfig" keyStoreRef="myKeyStore"/>
                        //    <sslDefault sslRef="sslConfig"/>
                        boolean hasNonDefaultSSL = false;
                        List<Element> elements = ConfigUtils.getResolvedElements(configFile.getDocument(), configFile.getURI(), serverInfo, configFile.getUserDirectory(),
                                                                                 Constants.SSL_DEFAULT_ELEMENT, null);
                        for (Element defaultSSLElement : elements) {
                            String sslRef = defaultSSLElement.getAttribute(Constants.SSL_REF_ATTR);
                            if (sslRef != null && !sslRef.isEmpty() && !sslRef.equals(Constants.DEFAULT_SSL_CONFIG_ID)) {
                                hasNonDefaultSSL = true;
                                break;
                            }
                        }
                        if (!hasNonDefaultSSL) {
                            // Check all ssl elements with the default id to see if the keystore ref
                            // is set to something other than the default.  If it is and the keystore
                            // is not defined then the reference checking will have picked it
                            // up already.
                            //    <ssl id="defaultSSLConfig" keyStoreRef="myKeyStore" />
                            boolean hasNonDefaultKeystore = false;
                            elements = ConfigUtils.getResolvedElements(configFile.getDocument(), configFile.getURI(), serverInfo, configFile.getUserDirectory(),
                                                                       Constants.SSL_ELEMENT, Constants.FACTORY_ID);
                            for (Element sslElement : elements) {
                                String id = sslElement.getAttribute(Constants.FACTORY_ID);
                                if (Constants.DEFAULT_SSL_CONFIG_ID.equals(id)) {
                                    String keystoreRef = sslElement.getAttribute(Constants.KEYSTORE_REF_ATTR);
                                    if (keystoreRef != null && !keystoreRef.isEmpty() && !keystoreRef.equals(Constants.DEFAULT_KEY_STORE)) {
                                        hasNonDefaultKeystore = true;
                                        break;
                                    }
                                }
                            }
                            if (!hasNonDefaultKeystore) {
                                // Check for a keystore element
                                boolean foundValidKeystore = false;
                                elements = ConfigUtils.getResolvedElements(configFile.getDocument(), configFile.getURI(), serverInfo, configFile.getUserDirectory(),
                                                                           Constants.KEY_STORE, Constants.FACTORY_ID);
                                for (Element elem : elements) {
                                    String id = elem.getAttribute(Constants.INSTANCE_ID);
                                    String password = elem.getAttribute(Constants.PASSWORD_ATTRIBUTE);
                                    // The id can be unset or set to the default keystore id, the password must be set
                                    if ((id == null || id.isEmpty() || id.equals(Constants.DEFAULT_KEY_STORE)) && password != null && !password.isEmpty()) {
                                        foundValidKeystore = true;
                                        break;
                                    }
                                }

                                if (!foundValidKeystore) {
                                    // Call the missing keystore handler
                                    final IProgressMonitor configSaveMonitor = monitor.newChild(9);
                                    final Thread dialogThread = new Thread() {
                                        @Override
                                        public void run() {
                                            MissingKeystoreHandler missingKeystoreHandler = Activator.getMissingKeystoreHandler();
                                            if (missingKeystoreHandler != null && missingKeystoreHandler.handleMissingKeystore(serverInfo, true)) {
                                                try {
                                                    configFile.save(configSaveMonitor);
                                                } catch (IOException e) {
                                                    Trace.logError("Failed to save the configuration file changes to add missing security elements.", e);
                                                }
                                            }
                                        }
                                    };
                                    dialogThread.setDaemon(true);
                                    dialogThread.start();

                                    dialogThread.join();

                                }
                            }
                        }
                    }
                    monitor.setWorkRemaining(1);
                    monitor.worked(1);
                    return b;
                }

                /*
                 * Code below is to handle launch from the debug view launcher
                 */

                if (monitor.isCanceled())
                    return false;

                ISchedulingRule rule = getProjectPublishRule(server, monitor.newChild(5));

                if (monitor.isCanceled() || rule == null)
                    return false;

                jobManager.beginRule(rule, monitor.newChild(5));

                IStatus status = null;
                try {
                    status = server.publish(IServer.PUBLISH_INCREMENTAL, monitor.newChild(10));
                } finally {
                    jobManager.endRule(rule);
                }
                if (monitor.isCanceled() || Status.CANCEL_STATUS.equals(status) ||
                    (status != null && status.getSeverity() == IStatus.ERROR))
                    return false;
            }
        } catch (Exception e) {
            Trace.logError("Error launching server", e);
        } finally {
            monitor.done();
        }
        return b;
    }

    // Check that the 2 servers are the same
    private boolean checkServer(IServer server, IServer jserver) {
        if (jserver instanceof ServerWorkingCopy) {
            if (server == ((ServerWorkingCopy) jserver).getOriginal()) {
                return true;
            }
        } else {
            if (server == jserver) {
                return true;
            }
        }
        return false;
    }

    private ISchedulingRule getProjectPublishRule(IServer server, IProgressMonitor monitor) {
        IModule[] modules = server.getModules();
        final List<IProject> projectList = new ArrayList<IProject>();

        if (modules.length > 0) {
            for (IModule curModule : modules) {
                if (monitor.isCanceled())
                    return null;
                collectModuleProjects(server, new IModule[] { curModule }, projectList, monitor);
            }
        }

        ISchedulingRule[] publishScheduleRules = new ISchedulingRule[projectList.size() + 1];
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        publishScheduleRules[0] = server;
        int i = 1;
        for (IProject curProj : projectList) {
            publishScheduleRules[i++] = ruleFactory.modifyRule(curProj);
        }

        ISchedulingRule publishRule = MultiRule.combine(publishScheduleRules);

        return publishRule;
    }

    private void collectModuleProjects(IServer server, IModule[] module, List<IProject> projectList, IProgressMonitor monitor) {
        if (module == null || module.length == 0 || monitor.isCanceled())
            return;
        IProject project = module[module.length - 1].getProject();
        if (project != null) {
            if (!projectList.contains(project)) {
                projectList.add(project);
            }
        }
        IModule[] children = server.getChildModules(module, monitor);
        for (IModule m : children) {
            IModule[] module2 = new IModule[module.length + 1];
            System.arraycopy(module, 0, module2, 0, module.length);
            module2[module.length] = m;
            collectModuleProjects(server, module2, projectList, monitor);
        }
    }

    /**
     * Sets up a server that is already started prior to calling this method.
     *
     * @param mode
     * @param launch
     * @param websphereServerBehaviour
     * @throws CoreException
     */
    public static void launchIt(String launchMode, WebSphereServerBehaviour websphereServerBehaviour) throws CoreException {
        if (websphereServerBehaviour != null) {
            WebSphereServer websphereServer = websphereServerBehaviour.getWebSphereServer();
            AbstractLaunchConfigurationExtension delegate = ServerTypeExtensionFactory.getServerLaunchOperation(websphereServer.getServerType());

            if (delegate == null) {
                delegate = defaultLaunchConfiguration;
            }

            delegate.launchStartedServer(launchMode, websphereServerBehaviour);
        }
    }
}
