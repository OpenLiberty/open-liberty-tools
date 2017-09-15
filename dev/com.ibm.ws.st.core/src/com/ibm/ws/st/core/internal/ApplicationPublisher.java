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
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;
import org.w3c.dom.Document;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour.ApplicationStateTracker;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.looseconfig.LooseconfigXMLGenerator;

/**
 * Extension for downstream adopters to add support for publishing new types of applications.
 * Subclasses should be stateless and not hold onto server or configuration objects.
 */
public abstract class ApplicationPublisher {
    private ServerExtensionWrapper wrapper;

    private static final String CLASS_EXTENSION = ".class";

    @SuppressWarnings("boxing")
    private static final int RETRY_LIMIT_IN_SECS = Integer.getInteger("com.ibm.ws.st.applicationPublishTimeoutInSeconds", 60);

    private boolean isLooseConfig = false;

    protected List<String> addedResourceList;
    protected List<String> removedResourceList;
    protected List<String> changedResourceList;

    protected List<String> jmxDeleteList = new ArrayList<String>();

    /**
     * Init method, called once to pass in the context.
     *
     * @param wrapper
     */
    protected final void init(ServerExtensionWrapper wrapper) {
        this.wrapper = wrapper;
    }

    protected final IServer getServer() {
        return wrapper.getWebSphereServer().getServer();
    }

    protected final WebSphereServerInfo getWebSphereServerInfo() {
        return wrapper.getWebSphereServerInfo();
    }

    protected final WebSphereServer getWebSphereServer() {
        return wrapper.getWebSphereServer();
    }

    protected final WebSphereServerBehaviour getWebSphereServerBehaviour() {
        return wrapper.getWebSphereServerBehaviour();
    }

    protected final ConfigurationFile getConfiguration() {
        return wrapper.getWebSphereServer().getConfiguration();
    }

    protected final JMXConnection getJMXConnection() {
        return wrapper.getJmxConnection();
    }

    /**
     * Returns whether loose config is enabled.
     */
    protected final boolean isLooseConfig() {
        return isLooseConfig;
    }

    /**
     * Set the loose config flag if the server extension supports it.
     * The value is always {@code false} if the runtime does not support it.
     */
    protected final void setIsLooseConfig(boolean value) {
        isLooseConfig = value && isLooseConfigSupported();
    }

    @Override
    public String toString() {
        return "ApplicationPublisher [" + getClass().toString() + "]";
    }

    public void prePublishApplication(int kind, PublishUnit app, MultiStatus status, IProgressMonitor monitor) {
        // do nothing
    }

    /**
     * Publish a module and its child modules, given the publish kind.
     */
    public void publishModuleAndChildren(int kind, PublishUnit unit, MultiStatus mStatus, IProgressMonitor monitor) {
        IModule[] module = unit.getModule();
        String projectName = module[module.length - 1].getName();
        int kind2 = kind;

        try {
            List<PublishUnit> children = unit.getChildren();

            int ticks = 2;
            if (children != null) {
                ticks += children.size();
            }
            monitor.beginTask("", ticks);

            IStatus status = null;
            int deltaKind = unit.getDeltaKind();
            // remove the children first in the remove case; else publish the parent first
            if (deltaKind != ServerBehaviourDelegate.REMOVED) {
                status = publishModule(kind2, unit, new SubProgressMonitor(monitor, 1));

                monitor.worked(1);
            }
            if (children != null) {
                for (PublishUnit child : children) {
                    publishModuleAndChildren(kind, child, mStatus, new SubProgressMonitor(monitor, 1));
                    monitor.worked(1);
                }
            }
            if (deltaKind == ServerBehaviourDelegate.REMOVED) {
                status = publishModule(kind2, unit, new SubProgressMonitor(monitor, 1));
            }

            monitor.worked(1);

            // RTC 163295 - we should only add module status if there was a failure, otherwise if there is an error there will be too many "OK" messages
            // which isn't helpful.
            if (status != null && !status.isOK())
                mStatus.add(status);
            monitor.worked(ticks);
        } finally {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "--<-- Done publishing module " + projectName);
            monitor.done();
        }
    }

    /**
     * Publish a single module
     */
    protected abstract IStatus publishModule(int kind, PublishUnit module, IProgressMonitor monitor);

    protected void setModulePublishState(IModule[] module, int state) {
        getWebSphereServerBehaviour().setModulePublishState(state, module);
    }

    protected abstract String getModuleDeployName(IModule module);

    /**
     * @param app
     */
    protected void handleAppLooseConfigXML(PublishUnit app) throws Exception {
        String xmlFileName = getModuleDeployName(app.getModule()[0]) + ".xml";
        IPath serverPath = getWebSphereServerBehaviour().getRootPublishFolder(false);
        serverPath = serverPath.append(xmlFileName);

        File serverFile = serverPath.toFile();

        IPath appsPathOverride = getAppsPathOverride();
        if (appsPathOverride != null) {
            appsPathOverride = appsPathOverride.append(xmlFileName);
        }

        if (app.getDeltaKind() == ServerBehaviourDelegate.REMOVED) {
            // Override apps path to point to proper mount path instead of runtime/server project path
            if (appsPathOverride != null) {
                serverFile = appsPathOverride.toFile();
            }
            if (serverFile.exists()) {
                if (!serverFile.delete()) {
                    Trace.logError("Can't delete loose config xml " + serverPath.toString(), null);
                }
            }
        } else {
            IPath tempPath = getWebSphereServerBehaviour().getTempDirectory().append(xmlFileName);
            long time = System.currentTimeMillis();
            LooseconfigXMLGenerator gen = new LooseconfigXMLGenerator(getWebSphereServerBehaviour());
            if (appsPathOverride == null) {
                doHandleGen(app, serverPath, serverFile, tempPath, gen);
            }
            // For extensions (docker) that have an overridden path
            if (appsPathOverride != null) {
                File looseConfigXmlFile = appsPathOverride.toFile();
                doHandleGen(app, appsPathOverride, looseConfigXmlFile, tempPath, gen);
                getChangedResourceList().add(serverPath.toOSString());
            }
            if (Trace.ENABLED) {
                Trace.tracePerf("Loose config xml gen", time);
            }
        }
    }

    /**
     * @param app
     * @param serverPath
     * @param serverFile
     * @param tempPath
     * @param gen
     * @throws ParserConfigurationException
     * @throws IOException
     */
    private void doHandleGen(PublishUnit app, IPath serverPath, File serverFile, IPath tempPath, LooseconfigXMLGenerator gen) throws ParserConfigurationException, IOException {
        if (serverFile.exists()) {
            try {
                gen.generateRepository(tempPath, app);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document tempDoc = builder.parse(tempPath.toFile());
                Document serverDoc = builder.parse(serverFile);
                tempDoc.normalizeDocument();
                serverDoc.normalizeDocument();
                if (!tempDoc.isEqualNode(serverDoc)) {
                    FileUtil.move(tempPath, serverPath);
                    getChangedResourceList().add(serverPath.toOSString());
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Exception when update loose config file", e);
                gen.generateRepository(serverPath, app);
                getChangedResourceList().add(serverPath.toOSString());
            }
        } else {
            gen.generateRepository(serverPath, app);
            if (app.getDeltaKind() != ServerBehaviourDelegate.ADDED) { // don't need to do JMX when a new app is added
                                                                       // this is to cover the case of the xml is deleted
                                                                       // for some reasons
                getChangedResourceList().add(serverPath.toOSString());
            }
        }
    }

    protected void notifyUpdatedApplicationResourcesViaJMX() throws Exception {
        // we will not do a JMX call if the server is not started or JMX is not enabled
        JMXConnection jmx = getJMXConnection();
        if (jmx == null || getServer().getServerState() != IServer.STATE_STARTED)
            return;

        if ((addedResourceList == null || addedResourceList.isEmpty()) &&
            (removedResourceList == null || removedResourceList.isEmpty()) &&
            (changedResourceList == null || changedResourceList.isEmpty()))
            return;

        if (!getWebSphereServer().isLocalSetup()) {

            WebSphereServerBehaviour wsb = getWebSphereServerBehaviour();

            // Try to get the value for ${server.config.dir} to display to the user
            String remoteServerConfigPath = wsb.resolveConfigVar("${server.config.dir}", jmx);

            List<String> remoteAddedResourceList = null;
            List<String> remoteChangedResourceList = null;
            List<String> remoteRemovedResourceList = null;

            if (addedResourceList != null) {
                remoteAddedResourceList = new ArrayList<String>(addedResourceList.size());
                for (String f : addedResourceList) {
                    try {
                        String dest = f.replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(), remoteServerConfigPath).replace("\\", "/");
                        remoteAddedResourceList.add(dest);
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to upload file: " + f, e);
                    }
                }
            }
            if (changedResourceList != null) {
                remoteChangedResourceList = new ArrayList<String>(changedResourceList.size());
                for (String f : changedResourceList) {
                    try {
                        String dest = f.replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(), remoteServerConfigPath).replace("\\", "/");
                        remoteChangedResourceList.add(dest);
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to upload file: " + f, e);
                    }
                }
            }
            if (removedResourceList != null) {
                long deleteStartTime = System.currentTimeMillis();
                remoteRemovedResourceList = new ArrayList<String>(removedResourceList.size());
                for (String f : removedResourceList) {
                    try {
                        String dest = f.replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(), remoteServerConfigPath).replace("\\", "/");
                        remoteRemovedResourceList.add(dest);
                        jmx.deleteFile(dest);
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to delete remote file: " + f, e);
                    }
                }
                if (Trace.ENABLED && removedResourceList.size() > 0) {
                    Trace.tracePerf("Finished JMX delete operations", deleteStartTime);
                }
            }
            jmx.notifyFileChanges(remoteAddedResourceList, remoteChangedResourceList, remoteRemovedResourceList);
        } else {
            jmx.notifyFileChanges(addedResourceList, changedResourceList, removedResourceList);
        }
    }

    public void postPublishApplication(int kind, PublishUnit app, MultiStatus status, IProgressMonitor monitor) {
        try {
            if (isLooseConfig)
                handleAppLooseConfigXML(app);

            updateRemoteApplicationFilesViaJMX(app, status);

            /*
             * We need to wait for application to be stopped before runtime is notified about deleted resources else it results in CWWKZ0059E error. defect #176917
             *
             * However, if the publish state is unknown (eg. app contains errors and was not started) we won't get a notification back when removing the
             * app from the server so the app won't go to STOPPED and there's no point waiting.
             */
            int trycount = 0;
            if (app.getDeltaKind() == ServerBehaviourDelegate.REMOVED && getServer().getModulePublishState(app.getModule()) != IServer.PUBLISH_STATE_UNKNOWN) {
                try {
                    while ((!getWebSphereServerBehaviour().appStateTracker.hasApplicationState(app.getModuleName(), ApplicationStateTracker.STOPPED) && trycount < 50)
                           && !monitor.isCanceled()) {
                        Thread.sleep(200);
                        trycount++;
                    }
                    if (trycount >= 50)
                        Trace.logError("Timed out waiting for Application Stopped message(CWWKZ0009I) for app " + app.getModuleName(), null);
                } catch (Exception e) {
                    Trace.logError("Exception occured for app " + app.getModuleName(), e);
                }
            }
            notifyUpdatedApplicationResourcesViaJMX();

            JMXConnection jmx = getJMXConnection();
            WebSphereServerBehaviour wsb = getWebSphereServerBehaviour();

            // attempt to sync config after publishing the first app so that it doesn't need to wait
            // for other apps to get processed before the user can run on server
            wsb.handleAutoConfigSyncJob(-1);
            status.add(wsb.syncConfig(jmx));

            if (status.getSeverity() != IStatus.ERROR)
                setApplicationModuleStates(app, IServer.PUBLISH_STATE_NONE);

            boolean appPreExistInServerXML = wsb.getOverriddenAppsInServerXML().contains(app.getModuleName());
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "status=" + status.getSeverity());
                Trace.trace(Trace.INFO, "isJMXNull=" + (jmx == null));
                Trace.trace(Trace.INFO, "appDeltaKind=" + app.getDeltaKind());
                Trace.trace(Trace.INFO, "serverState=" + getServer().getServerState());
                Trace.trace(Trace.INFO, "moduleState=" + getServer().getModuleState(app.getModule()));
                Trace.trace(Trace.INFO, "appStateTrackerHasRestartState=" + wsb.isAppStateTrackerHasState(app.getModuleName(), ApplicationStateTracker.NEED_RESTART_APP));
                Trace.trace(Trace.INFO, "appPreExistInServerXML=" + appPreExistInServerXML);
                Trace.trace(Trace.INFO, "isLocalHost=" + wsb.getWebSphereServer().isLocalSetup());
            }
            // if the publish status has no errors so far we can try to start the application
            if (status.getSeverity() != IStatus.ERROR && jmx != null && app.getDeltaKind() == ServerBehaviourDelegate.ADDED &&
                getServer().getServerState() == IServer.STATE_STARTED &&
                getServer().getModuleState(app.getModule()) != IServer.STATE_STARTED &&
                (wsb.isAppStateTrackerHasState(app.getModuleName(), ApplicationStateTracker.NEED_RESTART_APP) ||
                 appPreExistInServerXML || !wsb.getWebSphereServer().isLocalSetup())) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Getting deployment path and pushing notification to the server.");
                }
                String appDeployName = getModuleDeployName(app.getModule()[0]);
                if (isLooseConfig)
                    appDeployName += ".xml";
                IPath deployPath = wsb.getRootPublishFolder(false);
                if (deployPath != null) {
                    deployPath = deployPath.append(appDeployName);
                    IPath usrMount = getAppsPathOverride();
                    if (usrMount != null) {
                        deployPath = usrMount.append(appDeployName);
                    }

                    ArrayList<String> a = new ArrayList<String>(1);
                    a.add(getDestinationPath(deployPath, jmx));
                    // TODO use the round-trip version of this call so that it returns after the server has processed the changes
                    long startTime = System.currentTimeMillis();

                    jmx.notifyFileChanges(a, null, null);
                    if (Trace.ENABLED)
                        Trace.tracePerf("Notify file changes completed", startTime);

                    /*
                     * Defect 230701: Docker: Publish hangs when deploying OSGi app to Liberty in a Docker container
                     *
                     * At this point in the method the file transfer should be complete. It shouldn't take too long for the app
                     * MBean to be created. We don't know how long it should take but should not wait endlessly in case there's an
                     * error that prevents the app MBean from being created. After a specified wait time we should prompt the user to
                     * inform them it is taking an unusual amount of time and to ask them whether to continue to wait or cancel the
                     * publish. The user can also check the server logs for hints as to why the publish is not completing.
                     *
                     * One such scenario that hit the above condition is when the tools targeted a local runtime that had a different feature set than the runtime within the Docker
                     * container. If the local runtime features matched the Docker runtime features the add application
                     * in the Add/Remove dialog would have blocked the publish but since the feature was available in the local
                     * runtime the application add was allowed. In this case the app MBean never comes up because the required features
                     * are not available. The feature resolution should be done against the features available in the runtime of the
                     * server that the application will be deployed.
                     */
                    int retries = 0;
                    while (!jmx.appMBeanExists(app.getModuleName()) && !monitor.isCanceled()) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.INFO, "Waiting for application MBean to be created before attempting to start");
                        }
                        try {
                            Thread.sleep(250);
                            if (retries++ > RETRY_LIMIT_IN_SECS * 4) {
                                retries = 0;
                                boolean continueWaiting = waitForApplicationPrompt(app.getModuleName());
                                if (!continueWaiting)
                                    monitor.setCanceled(true);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    jmx.startApplication(app.getModuleName());
                }
                if (appPreExistInServerXML)
                    wsb.getOverriddenAppsInServerXML().remove(app.getModuleName());
            }
        } catch (Exception e) {
            Trace.logError("Exception occured in postPublishApplication()", e);
            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPublishModule, app.getModule()[0].getName()), e));
        } finally {
            cleanupRemoteTempPublishFiles();
        }

        if (status.getSeverity() == IStatus.OK) {
            status.add(new Status(IStatus.OK, Activator.PLUGIN_ID, NLS.bind(Messages.deployAppSuccessfully, app.getModule()[0].getName())));
        } else {
            status.add(new Status(status.getSeverity(), Activator.PLUGIN_ID, NLS.bind(Messages.deployAppFailed, app.getModule()[0].getName())));
        }
    }

    private boolean waitForApplicationPrompt(String appName) {
        PromptHandler handler = Activator.getPromptHandler();
        if (handler == null || PromptUtil.isSuppressDialog())
            return false;
        return handler.handleConfirmPrompt(NLS.bind(Messages.publishModule, appName), Messages.waitForApplicationDialogMsg, true);
    }

    /**
     * Utility method to combine a list of status
     *
     * @param status a List of IStatus
     */
    protected static IStatus combineModulePublishStatus(List<IStatus> status, String moduleName) {
        if (status == null || status.size() == 0)
            return Status.OK_STATUS;

        if (status.size() == 1) {
            return status.get(0);
        }
        IStatus[] children = new IStatus[status.size()];
        status.toArray(children);
        return new MultiStatus(Activator.PLUGIN_ID, 0, children, NLS.bind(Messages.publishingModule, moduleName), null);
    }

    protected void setApplicationModuleStates(PublishUnit app, int state) {
        setModulePublishState(app.getModule(), state);
        List<PublishUnit> children = app.getChildren();
        if (children != null) {
            for (PublishUnit child : children)
                setApplicationModuleStates(child, state);
        }
    }

    protected int getApplicationChangeKind(PublishUnit pu) {
        if (pu.getParent() == null)
            return pu.getDeltaKind();
        return getApplicationChangeKind(pu.getParent());
    }

    /**
     * Returns the location that is used by the (local) server for the resource.
     *
     * @param deployPath - null to indicate it is loose config.
     * @param resource
     * @return
     */
    protected String getPublishLocation(IPath deployPath, IModuleResource resource) {
        String path = null;
        try {
            if (deployPath == null) {
                File file = resource.getAdapter(File.class);
                if (file != null) {
                    path = file.getAbsolutePath();
                } else {
                    IFile ifile = resource.getAdapter(IFile.class);
                    if (ifile != null) {
                        path = ifile.getLocation().toOSString();
                    }
                }
                if (path == null) {
                    Trace.logError("The File and IFile for the " + resource.getName() + " resource were both null.", null);
                    return null;
                }
            } else {
                path = deployPath.append(resource.getModuleRelativePath()).append(resource.getName()).toFile().getAbsolutePath();
            }
        } catch (Exception e) {
            Trace.logError("getPublishLocation(..) " + resource.getName(), e);
        }
        if (isLooseConfig) {
            IPath mappedPath = getLooseConfigVolumeMapping(new Path(path));
            if (mappedPath != null) {
                path = mappedPath.toOSString();
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.RESOURCE, "PublishLocation: isLooseConfig = " + isLooseConfig + " : path is " + path);
        }
        return path;
    }

    protected void computeDeltaResources(IPath deployPath, final IModuleResourceDelta[] deltaArray) {
        for (IModuleResourceDelta delta : deltaArray) {
            String s = getPublishLocation(deployPath, delta.getModuleResource());
            if (s != null) {
                switch (delta.getKind()) {
                    case IModuleResourceDelta.ADDED:
                        getAddedResourceList().add(s);
                        if (Trace.ENABLED)
                            Trace.trace(Trace.RESOURCE, "Resource added: " + s);
                        break;
                    case IModuleResourceDelta.CHANGED:
                        getChangedResourceList().add(s);
                        if (Trace.ENABLED)
                            Trace.trace(Trace.RESOURCE, "Resource changed: " + s);
                        break;
                    case IModuleResourceDelta.REMOVED:
                        getRemovedResourceList().add(s);
                        if (Trace.ENABLED)
                            Trace.trace(Trace.RESOURCE, "Resource removed: " + s);
                        break;
                    default:
                        break;
                }
            }
            // check if the delta has affected children
            IModuleResourceDelta[] childDelta = delta.getAffectedChildren();
            // don't remove the null check.  The doc says return an empty array if there is no child,
            // but null is returned.
            if (childDelta != null && childDelta.length != 0) {
                computeDeltaResources(deployPath, childDelta);
            }
        }
    }

    public void handleLooseConfigModeChange(int kind, PublishUnit pu, MultiStatus multi, IProgressMonitor monitor) {
        MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, NLS.bind(Messages.publishModule, pu.getModule()[0].getName()), null);

        if (pu.getDeltaKind() != ServerBehaviourDelegate.REMOVED && pu.getDeltaKind() != ServerBehaviourDelegate.ADDED) {
            String appName = pu.getModule()[0].getName();

            int appState = getServer().getModuleState(pu.getModule());

            JMXConnection jmx = getJMXConnection();
            if (jmx != null && appState == IServer.STATE_STARTED) {
                try {
                    jmx.stopApplication(appName);
                    // Wait for the application to stop using the server stop timeout (in seconds) as the application stop timeout
                    int timeout = getServer().getStopTimeout() * 20;
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Waiting for the " + appName + " application to stop. Wait " + timeout + " times 0.05 seconds.");
                    for (int i = 0; i < timeout && !monitor.isCanceled() && getServer().getModuleState(pu.getModule()) != IServer.STATE_STOPPED; i++) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                    if (Trace.ENABLED && getServer().getModuleState(pu.getModule()) != IServer.STATE_STOPPED) {
                        Trace.trace(Trace.WARNING, "The " + appName + " module did not stop before the timeout.");
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Exception occured in handleLooseConfigModeChange() when stopping the application", e);
                }
            }

            removePublishedAppFiles(pu, !isLooseConfig, status, monitor);

            SubProgressMonitor childMonitor = new SubProgressMonitor(monitor, 15);
            PublishUnit currentPU = copyPublishUnitWithADD(pu);
            prePublishApplication(IServer.PUBLISH_FULL, currentPU, status, childMonitor);
            childMonitor.done();
            childMonitor = new SubProgressMonitor(childMonitor, 70);
            publishModuleAndChildren(IServer.PUBLISH_FULL, currentPU, status, childMonitor);
            if (isLooseConfig) {
                try {
                    handleAppLooseConfigXML(pu);
                } catch (Exception e) {
                    Trace.logError("Exception occured in handlePublishModeChange() when generate loose config file", e);
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPublishModule, appName), e));
                }
            }
            childMonitor.done();
            if (jmx != null && appState == IServer.STATE_STARTED) {
                try {
                    childMonitor = new SubProgressMonitor(childMonitor, 15);
                    updateServerOnPublishModeChangeViaJMX(kind, pu, jmx, status, childMonitor);

                    // Allow extensions to decide whether or not to update the remote files
                    // (In particular, when switching to non-loose, the app should also be uploaded to the remote server
                    // where in the standalone case, this is not needed).
                    if (postLooseConfigChange()) {
                        updateRemoteApplicationFilesViaJMX(pu, status);
                    }
                    jmx.startApplication(appName);
                } catch (Exception e) {
                    Trace.logError("Exception occured in handlePublishModeChange() when start the application", e);
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPublishModule, appName), e));
                }
            }

            multi.addAll(status);
            monitor.done();
        }
    }

    private PublishUnit copyPublishUnitWithADD(final PublishUnit origPU) {
        PublishUnit pu = new PublishUnit(origPU.getModule(), ServerBehaviourDelegate.ADDED);
        List<PublishUnit> children = origPU.getChildren();
        if (children != null) {
            for (PublishUnit child : children) {
                pu.addChild(copyPublishUnitWithADD(child));
            }
        }
        return pu;
    }

    private void updateServerOnPublishModeChangeViaJMX(int kind, PublishUnit app, JMXConnection jmx, MultiStatus multi, IProgressMonitor monitor) {
        IPath path = getWebSphereServerBehaviour().getRootPublishFolder(false);
        IPath loosePath = path.append(getModuleDeployName(app.getModule()[0]) + ".xml");
        IPath non_loosePath = path.append(getModuleDeployName(app.getModule()[0]));

        if (isLooseConfig) {
            IPath symLinkPath = getAppsPathOverride();
            if (symLinkPath != null) {
                loosePath = symLinkPath.append(getModuleDeployName(app.getModule()[0]) + ".xml");
                non_loosePath = symLinkPath.append(getModuleDeployName(app.getModule()[0]));
            }
        }

        // Map paths to the remote host if necessary
        String looseStr = getDestinationPath(loosePath, jmx);
        String non_looseStr = getDestinationPath(non_loosePath, jmx);

        ArrayList<String> add = null;
        ArrayList<String> remove = new ArrayList<String>();

        if (isLooseConfig) {
            remove.add(non_looseStr);
        } else {
            remove.add(looseStr);
        }
        if (app.getDeltaKind() != ServerBehaviourDelegate.REMOVED) {
            add = new ArrayList<String>();
            if (isLooseConfig) {
                add.add(looseStr);
            } else {
                add.add(non_looseStr);
            }
        }

        try {
            if (Trace.ENABLED) {
                String s;
                if (add != null)
                    s = add.get(0);
                else
                    s = "none";
                Trace.trace(Trace.INFO, "JMX for switch publish mode. Add: " + s + " Remove: " + remove.get(0));
            }
            jmx.notifyFileChanges(add, null, remove);
        } catch (Exception e) {
            Trace.logError("Exception occured in updateServerOnPublishModeChange ", e);
            multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPublishModule, app.getModule()[0].getName()), e));
        }
    }

    public String getDestinationPath(IPath sourcePath, JMXConnection jmx) {
        IPath destPath = sourcePath;
        if (isLooseConfig) {
            AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) getWebSphereServerBehaviour().getAdapter(AbstractServerBehaviourExtension.class);
            if (ext != null) {
                IPath mappedPath = ext.getMappedPath(destPath, getWebSphereServerBehaviour());
                if (mappedPath != null) {
                    destPath = mappedPath;
                }
            }
        }

        String destStr = destPath.toOSString();

        if (!isLooseConfig && !getWebSphereServer().isLocalSetup()) {
            try {
                String remotePath = getWebSphereServerBehaviour().resolveConfigVar("${server.config.dir}", jmx);
                destStr = destStr.replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(), remotePath);
            } catch (Exception e) {
                Trace.logError("An exception occured trying to get the value for ${server.config.dir} for the " + getServer().getName() + " server.", e);
            }
        }

        // Use canonical paths
        destStr = destStr.replace("\\", "/");
        return destStr;
    }

    protected static void addArrayStatusToList(List<IStatus> list, IStatus[] a) {
        if (list == null || a == null || a.length == 0)
            return;

        int size = a.length;
        for (int i = 0; i < size; i++)
            list.add(a[i]);
    }

    public boolean needToActOnLooseConfigModeChange(PublishUnit pu) {
        return true;
    }

    protected void removePublishedAppFiles(PublishUnit app, boolean looseCfg, MultiStatus multi, IProgressMonitor monitor) {
        IPath path = getWebSphereServerBehaviour().getRootPublishFolder(false);
        IPath symLinkPath = getAppsPathOverride();

        if (looseCfg) {
            path = path.append(getModuleDeployName(app.getModule()[0]) + ".xml");
            if (symLinkPath != null) {
                path = symLinkPath.append(getModuleDeployName(app.getModule()[0]) + ".xml");
            }

            removeRemoteFiles(path, looseCfg, monitor);
        } else {
            path = path.append(getModuleDeployName(app.getModule()[0]));
            if (symLinkPath != null) {
                path = symLinkPath.append(getModuleDeployName(app.getModule()[0]));
            }
        }
        File f = path.toFile();
        List<IStatus> status = new ArrayList<IStatus>();
        if (f.exists()) {
            if (f.isDirectory()) {
                IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
                addArrayStatusToList(status, stat);
            } else {
                if (!f.delete())
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorPublishModule, app.getModule()[0].getName())));
            }
        }
        multi.add(combineModulePublishStatus(status, app.getModule()[0].getName()));

    }

    public boolean requireConsoleOutputBeforePublishComplete(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        return false;
    }

    /**
     * @param module the internal module that overrides the external module
     * @param taskName
     * @param monitor
     */
    protected void removeExternalAppFiles(IModule module, String taskName, IProgressMonitor monitor) {
        if (module == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "application module is null");
            return;
        }
        monitor.beginTask(taskName, 30);
        getWebSphereServerBehaviour().setSyncExternalModulesAfterPublish();
        String appName = module.getName();
        if (getServer().getServerState() == IServer.STATE_STARTED) {
            IModule externalModule = getWebSphereServer().getExternalApp(appName); // internal module has the same name as the external module but it is a different module
            if (externalModule != null) {
                IModule[] externalApp = new IModule[] { externalModule };
                JMXConnection jmx = getJMXConnection();
                if (jmx != null && getServer().getModuleState(externalApp) != IServer.STATE_STOPPED) {
                    try {
                        jmx.stopApplication(appName);
                        int itr = getServer().getStopTimeout() * 20; // we use the server stop timeout as the application stop timeout
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Start to wait for application " + appName + ", to stop before remove it. wait " + itr + " times of 0.05 seconds.");
                        while (--itr > 0 && !monitor.isCanceled() && getServer().getModuleState(externalApp) != IServer.STATE_STOPPED) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                // Nothing to do;
                            }
                        }
                        if (Trace.ENABLED) {
                            if (itr <= 0)
                                Trace.trace(Trace.WARNING, "Application " + appName + " is not stopped before timeout.");
                            else
                                Trace.trace(Trace.INFO, "Application " + appName + " is stopped successfully. Loop remain: " + itr);
                        }
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Cannot stop the application, " + appName + ", before override it.");
                    }
                }
            }
        }
        monitor.worked(5);
        //remove the app from dropins first if it is there
        String deployName = getModuleDeployName(module);
        IPath rootPublishPath = getWebSphereServerBehaviour().getRootPublishFolder(false);
        File dropinsFile = getWebSphereServer().getServerPath().append("dropins").append(deployName).toFile();
        File nonLooseFile = null;
        File looseFile = null;

        if (rootPublishPath == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "The root publish path is null when remove module files for external application: " + appName);
        } else {
            nonLooseFile = rootPublishPath.append(deployName).toFile();
            looseFile = rootPublishPath.append(deployName + ".xml").toFile();
        }

        try {
            if (dropinsFile.exists()) {
                getWebSphereServerBehaviour().addAppRequireCallStartAfterPublish(appName);

                if (dropinsFile.isDirectory()) {
                    IStatus[] ss = PublishHelper.deleteDirectory(dropinsFile, monitor);
                    if (Trace.ENABLED) {
                        for (IStatus s : ss) {
                            if (s.getSeverity() != IStatus.OK)
                                Trace.trace(Trace.WARNING, "There is a problme to delete application file. " + s.getMessage());
                        }
                    }
                } else {
                    if (!dropinsFile.delete()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Cannot delete application file. " + dropinsFile.getAbsolutePath());
                    }
                }
            } else if (nonLooseFile != null && nonLooseFile.exists()) {
                getWebSphereServerBehaviour().addAppRequireCallStartAfterPublish(appName); // this is needed because the start in postPublish() will fail.
                if (nonLooseFile.isDirectory()) {
                    IStatus[] ss = PublishHelper.deleteDirectory(nonLooseFile, monitor);
                    if (Trace.ENABLED) {
                        for (IStatus s : ss) {
                            if (s.getSeverity() != IStatus.OK)
                                Trace.trace(Trace.WARNING, "There is a problme to delete application file. " + s.getMessage());
                        }
                    }
                } else {
                    if (!nonLooseFile.delete()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Cannot delete application file. " + nonLooseFile.getAbsolutePath());
                    }
                }
            } else if (looseFile != null && looseFile.exists()) {
                getWebSphereServerBehaviour().addAppRequireCallStartAfterPublish(appName); // this is needed because the start in postPublish() will fail.
                if (!looseFile.delete()) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Cannot delete application file. " + looseFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Cannot delete application file for " + appName, e);
        }
    }

    protected void publishDir(int kind, int deltaKind, IModule[] module, IPath destPath, PublishHelper helper, List<IStatus> status, IProgressMonitor monitor) {
        try {
            // remove if requested or if previously published and are now serving without publishing
            WebSphereServer serv = getWebSphereServer();
            if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED
                || !serv.isPublishToRuntime()) {
                File f = destPath.toFile();
                jmxDeleteList.add(destPath.toOSString());
                if (f.exists()) {
                    if (f.isDirectory()) {
                        IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
                        addArrayStatusToList(status, stat);
                    }
                }

                if (deltaKind == ServerBehaviourDelegate.REMOVED || !serv.isPublishToRuntime())
                    return;
            }

            File f = destPath.toFile();
            if (f.exists() && !f.isDirectory() && !f.delete()) {
                status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDeleteFile, f.getName())));
                return;
            }

            WebSphereServerBehaviour servBehaviour = getWebSphereServerBehaviour();
            if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
                IModuleResource[] mr = servBehaviour.getResources(module);
                IStatus[] stat = helper.publishSmart(mr, destPath, monitor);
                addArrayStatusToList(status, stat);
                return;
            }

            IModuleResourceDelta[] delta = servBehaviour.getPublishedResourceDelta(module);
            IStatus[] stat = helper.publishDelta(delta, destPath, monitor);
            addArrayStatusToList(status, stat);
        } catch (Throwable t) {
            Trace.logError("Failed to copy module resource.", t);
            if (status != null)
                status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t));
        }
    }

    /**
     * @return the addedResourceList. A new list is created if it is null.
     */
    protected List<String> getAddedResourceList() {
        if (addedResourceList == null)
            addedResourceList = new ArrayList<String>();
        return addedResourceList;
    }

    /**
     * Sets the addedResourceList to null.
     */
    protected void clearAddedResourceList() {
        addedResourceList = null;
    }

    /**
     * @return the removedResourceList. A new list is created if it is null.
     */
    protected List<String> getRemovedResourceList() {
        if (removedResourceList == null)
            removedResourceList = new ArrayList<String>();
        return removedResourceList;
    }

    /**
     * Set removedResourceList to null.
     */
    protected void clearRemovedResourceList() {
        removedResourceList = null;
    }

    /**
     * @return the changedResourceList. A new list is created if it is null.
     */
    protected List<String> getChangedResourceList() {
        if (changedResourceList == null)
            changedResourceList = new ArrayList<String>();
        return changedResourceList;
    }

    /**
     * Sets the changedResourceList to null
     */
    protected void clearChangedResourceList() {
        changedResourceList = null;
    }

    protected boolean checkFileExtension(List<String> list) {
        for (String s : list) {
            String ext = getFileExtension(s);
            if (ext != null) {
                if (ext.equalsIgnoreCase(CLASS_EXTENSION))
                    return true;
            }
        }
        return false;
    }

    private String getFileExtension(String fullName) {
        if (fullName == null)
            return null;
        int i = fullName.lastIndexOf('.');
        if (i != -1)
            return fullName.substring(i);
        return null;
    }

    /**
     * The default is to have loose config support enabled. If a publisher
     * does not support loose config it must override this method.
     */
    public boolean isLooseConfigSupported() {
        return getWebSphereServer().isLocalSetup() || getWebSphereServer().isLooseConfigEnabled();
    }

    /**
     * Handles uploading added application files and deleting removed application files for remote servers.
     *
     * Ordered steps:
     * 1. Use JMX to delete files marked for deletion
     * 2. Package applications present in the temporary publish locations
     * 3. Upload application packages
     *
     * @throws Exception
     */
    private void updateRemoteApplicationFilesViaJMX(PublishUnit app, MultiStatus status) throws Exception {
        JMXConnection jmx = getJMXConnection();
        if (jmx == null || jmx.isLocalConnection() || getServer().getServerState() != IServer.STATE_STARTED) {
            jmxDeleteList.clear();
            return;
        }

        WebSphereServerBehaviour wsb = getWebSphereServerBehaviour();

        if (!jmxDeleteList.isEmpty()) {
            long deleteStartTime = System.currentTimeMillis();
            for (String file : jmxDeleteList) {
                // TODO We can only delete a single file at a time using the JMX deleteFile operation at this time.
                // Therefore, this is workaround code since we cannot just specify a directory or list of files
                // to be deleted. We will revisit this code to make it more efficient in the future once the runtime
                // has such an operation.
                try {
                    String remotePath = wsb.resolveConfigVar("${server.config.dir}", jmx);
                    String dest = file.replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(), remotePath).replace("\\",
                                                                                                                                  "/");
                    CompositeData[] data = jmx.getDirectoryEntries(dest, true, "");

                    if (data == null)
                        continue;

                    ArrayDeque<String> dirs = new ArrayDeque<String>(data.length);
                    dirs.push(dest); // the parent directory is last to be deleted
                    // the directories are listed in a depth-first manner so each file
                    // should be pushed to the top of the stack so that we are deleting the
                    // deepest files first and work up to the root directories & files
                    for (CompositeData c : data) {
                        dirs.push((String) c.get("fileName"));
                    }

                    while (!dirs.isEmpty()) {
                        String f = dirs.pop();
                        try {
                            jmx.deleteFile(f);
                        } catch (Exception e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Could not delete remote file: " + f, e);
                        }
                    }
                } catch (Exception e) {
                    Trace.logError("Failed to delete file: " + file, e);
                }
            }
            if (Trace.ENABLED) {
                Trace.tracePerf("Finished JMX delete operations", deleteStartTime);
            }

            jmxDeleteList.clear();
        } // jmx delete end

        IPath path = wsb.getRootPublishFolder(false);
        IPath tempPath = wsb.getTempDirectory();
        Map<File, File> filesToUpload = new HashMap<File, File>();
        try {
            if (path != null) {
                try {
                    // At the time of writing this appsDir can only be apps in the server output directory
                    // The tools don't currently support publishing to the shared apps location
                    File appsDir = path.toFile();
                    if (appsDir.exists() && appsDir.isDirectory()) {
                        File[] files = appsDir.listFiles();
                        if (files != null) {
                            for (File f : files) {
                                // Currently we only handle applications that are expanded in directory structure (ie. the apps are directories not files)
                                if (f.isDirectory()) {
                                    IPath zipPath = tempPath.append(f.getName() + ".zip");
                                    FileUtil.zipDirectory(f, zipPath.toOSString());
                                    File zipFile = zipPath.toFile();
                                    if (zipFile.exists()) {
                                        filesToUpload.put(zipFile, f);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorRemotePublishPackage, app.getModuleName()), e));
                }

                // don't upload if the packaging step failed
                if (status.getSeverity() != IStatus.OK)
                    return;

                long uploadStartTime = System.currentTimeMillis();
                try {
                    for (Map.Entry<File, File> entry : filesToUpload.entrySet()) {
                        File zipFile = entry.getKey();
                        File origFile = entry.getValue();
                        String remoteServerConfigPath = wsb.resolveConfigVar("${server.config.dir}", jmx);
                        String remoteDestination = origFile.getAbsolutePath().replace(wrapper.getWebSphereServerInfo().getServerPath().toOSString(),
                                                                                      remoteServerConfigPath).replace("\\", "/");
                        jmx.uploadFile(zipFile, remoteDestination, true);
                    }
                } catch (Exception e) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorRemotePublishTransfer, app.getModuleName()), e));
                }
                if (Trace.ENABLED && !filesToUpload.isEmpty()) {
                    Trace.tracePerf("Finished JMX upload operations", uploadStartTime);
                }
            }
        } finally {
            for (File file : filesToUpload.keySet()) {
                if (!file.delete()) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Failed to delete temporary file: " + file);
                    }
                }
            }
        }
    }

    private void cleanupRemoteTempPublishFiles() {
        if (getWebSphereServerBehaviour().isLocalUserDir())
            return;

        WebSphereServerBehaviour wsb = getWebSphereServerBehaviour();
        IPath path = wsb.getRootPublishFolder(false);
        if (path == null)
            return;
        File appDir = path.toFile();
        if (appDir.exists() && appDir.isDirectory()) {
            File[] files = appDir.listFiles();
            if (files == null)
                return;
            for (File f : files) {
                try {
                    if (f.isDirectory()) {
                        FileUtil.deleteDirectory(f.getAbsolutePath(), true);
                    } else {
                        if (!f.delete()) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Unable to delete temporary remote publish file: " + f.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to delete temporary remote publish file: " + f.getAbsolutePath());
                }
            }
        }
    }

    /*
     * Method available for publishers to check if extensions mount any host local path into the runtime (container) path specifically at /opt/ibm/wlp/usr.
     * For example, if the extensions define a Destination mount value of "/opt/ibm/wlp/usr", then the extension will return the Source mount value, which is
     * the physical, local file system path to the usr folder. Typically, as designed, it will be in a temp location in the workspace's .metadata/.plugins folder.
     *
     * A practical use case for publishers is that they can write directly to the apps folder. They just need to append the Source mount value with
     * "/servers/<serverName>/apps".
     */
    protected IPath getAppsPathOverride() {
        IPath appsOverridePath = null;
        WebSphereServerBehaviour webSphereServerBehaviour = getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            appsOverridePath = ext.getAppsOverride(webSphereServerBehaviour);
        }
        return appsOverridePath;
    }

    /*
     * Method available for publishers to check extensions if they mount the workspace path into a different (container) path
     *
     * @return Map where the key is the workspace path and the value is the destination mount path
     */
    protected IPath getLooseConfigVolumeMapping(IPath localPath) {
        WebSphereServerBehaviour webSphereServerBehaviour = getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            return ext.getMappedPath(localPath, webSphereServerBehaviour);
        }
        return null;
    }

    /*
     * Method available for publishers to check extensions if they want to update files to the remote server during the handling of the loose config
     * change and prior to starting the app. Extensions will return true to update the files, false, if no update is necessary.
     *
     * @return boolean
     */
    protected boolean postLooseConfigChange() throws Exception {
        WebSphereServerBehaviour webSphereServerBehaviour = getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            return ext.doUpdateRemoteAppFiles(webSphereServerBehaviour, isLooseConfig);
        }
        return false;
    }

    /*
     * Method available for publishers to check extensions if they want to remove files from the remote server during a loose config change, prior to publishing
     *
     * @param path
     *
     * @param looseCfg
     *
     * @param monitor
     */
    protected void removeRemoteFiles(IPath path, boolean looseCfg, IProgressMonitor monitor) {
        WebSphereServerBehaviour webSphereServerBehaviour = getWebSphereServerBehaviour();
        AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
        if (ext != null) {
            ext.removeRemoteAppFiles(webSphereServerBehaviour, path, looseCfg, monitor);
        }
    }
}