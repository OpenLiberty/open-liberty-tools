/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.junit.Test;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformUtil;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.ConsoleStreamsProxy;
import com.ibm.ws.st.core.internal.launch.WebSphereLaunchConfigurationDelegate;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.remote.RemoteServerTest;
import com.ibm.ws.st.core.tests.remote.RemoteTestUtil;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.ICondition;
import com.ibm.ws.st.core.tests.util.LibertyTestUtil;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

import junit.framework.TestCase;

/**
 * <p>
 * This class can act as a base for tests that want to import projects into the Eclipse workspace and use them in deployments onto a server. It is designed to work by copying a
 * server definition to the runtime location and then add applications from the workspace into it. When extending this class it should use <code>@BeforeClass</code> and
 * <code>@AfterClass</code> methods to set up the test fixture to run the tests within. For example an extension to this class might do the following to set it up and tear it down:
 * </p>
 * <code>
 * public class TestClass extends ToolsTestBase {<br/>
 * <br/>
 * &nbsp;&nbsp;@BeforeClass<br/>
 * &nbsp;&nbsp;public static void setUpClass {<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;init(); // Initialize utilities<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;createRuntime(); // Create a runtime, there is also a variant that takes the name of the runtime<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;createServer("MyServer", "Resources/MyTest/MyServer"); // Create and start a server to use in the tests<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;createVM("testJdk"); // Create a VM that can be used by the tests, the VM name should match the one set in the
 * .classpath file for the projects being imported<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;importProjects(new Path("MyTest"), new String[] {"Project1", "Project2"}); // Imports projects into the workspace<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;addApp("Project1"); // Adds the imported projects into the server<br/>
 * &nbsp;&nbsp;}<br/>
 * <br/>
 * &nbsp;&nbsp;@AfterClass<br/>
 * &nbsp;&nbsp;public static void tearDownClass {<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;removeApp("Project1"); // Removes the imported projects from the server<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;stopServer(); // Stops the server
 * &nbsp;&nbsp;}<br/>
 * <br/>
 * }<br>
 * </code>
 */
public abstract class ToolsTestBase extends TestCase {
    protected static final String RUNTIME_NAME = "Liberty_Tools_Runtime";
    protected static final String NEW_RUNTIME_NAME = RemoteServerTest.class.getCanonicalName() + ".newRuntime";
    protected static final String JDK_NAME = "Liberty_Tools_JDK";
    protected static final String libertyCertFile = "libertycerts";
    protected static final String remoteResourceFolder = "RemoteServer";
    protected static int LAUNCH_TIMEOUT = 35; // timeout in seconds
    protected static String runtimeLocation;
    protected static IPath resourceFolder;

    protected static IRuntime runtime;
    protected static IServer server;
    protected static String serverName;
    protected static WebSphereServer wsServer;
    protected static JMXConnection jmxConnection;
    protected static List<IPath> downloadedRemoteFiles = null;

    protected void doRemoteSetup(String runtimeName) throws Exception {
        init();
        RemoteTestUtil.setRemotePreferences();
        createRuntime(runtimeName);
        createVM(JDK_NAME);
    }

    /**
     * This method will initialize the test class ready to run test cases. This should be called from a method annotated with <code>@BeforeClass</code>.
     */
    public void init() {
        // Get the resource folder and runtime location so that we can use them running the tests
        resourceFolder = getResourceFolder();
        runtimeLocation = WLPCommonUtil.getWLPInstallDir(getRuntimeLocationType());
        assertNotNull("Runtime location is not set", runtimeLocation);

        // Set the publish timeout so that it does not hang
        Activator.setPreference("publish.exit.timeout", "60000");

        // At the start of each test, delete the consoleLineTrackerLog file if the property is set and if it exists.
        ServerTestUtil.removeConsoleLineTrackerLogForBrowserLinks();
    }

    public WLPCommonUtil.RuntimeLocationType getRuntimeLocationType() {
        return WLPCommonUtil.RuntimeLocationType.LATEST;
    }

    public IPath getResourceFolder() {
        return getPluginResourceFolder();
    }

    public static IPath getPluginResourceFolder() {
        return TestsPlugin.getInstallLocation().append("resources");
    }

    public IPath getInstallLocation() {
        return TestsPlugin.getInstallLocation();
    }

    /**
     * Creates a runtime named {@link #RUNTIME_NAME}.
     */
    protected void createRuntime() throws Exception {
        createRuntime(getRuntimeName());
    }

    protected static String getRuntimeName() {
        return RUNTIME_NAME;
    }

    /**
     * Creates a liberty runtime with the supplied name
     *
     * @param runtimeName
     * @throws Exception
     */
    protected void createRuntime(String runtimeName) throws Exception {
        createRuntime(runtimeName, null);
    }

    protected void createRuntime(String runtimeName, IPath userDir) throws Exception {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        for (IRuntime r : runtimes) {
            if (r.getName().equals(runtimeName)) {
                runtime = r;
                print("Runtime exists.  Reuse.");
                return;
            }
        }
        runtime = TestUtil.createRuntime(getServerTypeId(), runtimeLocation, runtimeName, userDir);

        //set environment variables
        Map<String, String> wlpEnvVars = new HashMap<String, String>() {
            {
                put("WLP_OUTPUT_DIR", runtime.getLocation().append("usr").append(Constants.SERVERS_FOLDER).toOSString());
                put("WLP_DEFAULT_OUTPUT_DIR", runtime.getLocation().append("usr").append(Constants.SERVERS_FOLDER).toOSString());
                put("WLP_USER_DIR", runtime.getLocation().append("usr").toOSString());
                put("WLP_DEFAULT_USER_DIR", runtime.getLocation().append("usr").toOSString());
                if (PlatformUtil.getOS(System.getProperty("os.name")) == PlatformUtil.OperatingSystem.MAC) {
                    //227127: OSX requires the HOME environment variable to be set in order to run docker-machine and containers
                    put("HOME", System.getenv("HOME"));
                }
            }
        };

        setEnvironmentVariables(wlpEnvVars);

        WLPCommonUtil.jobWaitBuildandResource("Wait for runtime create to complete: " + runtimeName);
    }

    protected static String getRuntimeVersion() {
        WebSphereRuntime wsRuntime = getWebSphereRuntime();
        return wsRuntime.getRuntimeVersion();
    }

    protected boolean runtimeSupportsFeature(String featureName) {
        WebSphereRuntime wsRuntime = getWebSphereRuntime();
        FeatureSet features = wsRuntime.getInstalledFeatures();
        String feature = features.resolve(featureName);
        return feature != null && !feature.isEmpty();
    }

    /*
     * Resolve a feature. Ensures the runtime supports the feature
     * and if the feature name passed in has no version then it returns
     * the latest version of that feature.
     *
     * If the feature cannot be resolved it returns null.
     */
    protected String resolveFeature(String featureName) {
        WebSphereRuntime wsRuntime = getWebSphereRuntime();
        FeatureSet features = wsRuntime.getInstalledFeatures();
        String feature = features.resolve(featureName);
        if (feature != null && feature.isEmpty()) {
            feature = null;
        }
        return feature;
    }

    protected static WebSphereRuntime getWebSphereRuntime() {
        if (runtime == null)
            throw new NullPointerException("Runtime should not be null.");
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (wsRuntime == null)
            throw new NullPointerException("Runtime should be an instance of WebSphereRuntime.  Actual runtime type is: " + runtime.getClass().getName());
        return wsRuntime;
    }

    protected void addUserDir(IProject userDirProject) throws Exception {
        LibertyTestUtil.addUserDir(runtime, userDirProject);
    }

    protected void addUserDir(IPath userDirPath) throws Exception {
        LibertyTestUtil.addUserDir(runtime, userDirPath);
    }

    public static WebSphereServer getWebSphereServer() {
        if (server != null)
            return (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        return null;
    }

    protected void createServer(IRuntime rt, String serverName, String serverConfigResourceLocation) throws Exception {
        createServer(rt, null, serverName, serverConfigResourceLocation);
    }

    protected void createServer(IRuntime rt, IPath userDir, String serverName, String serverConfigResourceLocation) throws Exception {
        Map<String, String> serverInfo = new HashMap<String, String>(3);
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_NAME, serverName);
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LOOSE_CFG, Boolean.toString(isLooseCfg()));
        if (userDir != null) {
            serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_USER_DIR, userDir.toPortableString());
        }
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_CONFIG_SOURCE, resolveResourceLocation(serverConfigResourceLocation));
        server = ServerTestUtil.createServer(rt, serverInfo);
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        try {
            jmxConnection = wsServer.createJMXConnection();
        } catch (Exception e) {
            // not all test cases require a working JMX Connection
        }
    }

    protected String resolveResourceLocation(String location) {
        if (location == null) {
            return null;
        }
        return getInstallLocation().append(location).toOSString();
    }

    public static boolean reuseExistingRemoteServer() throws Exception {
        serverName = getRemoteServerName();
        assertNotNull("Couldn't get remote server name via JMX", serverName);

        // only non-loose config supported for remote servers
        boolean isLC = false;

        IServer[] servers = ServerCore.getServers();
        for (IServer sr : servers) {
            if (sr.getId().equals(serverName)) {
                server = sr;
                wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                print("Server exists...reusing it.");
                if (wsServer.isLooseConfigEnabled() != isLC) {
                    IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
                    wc.setAttribute(WebSphereServer.PROP_SERVER_NAME, serverName);
                    wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, isLC);
                    server = wc.save(true, null);
                    wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                }
                print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
                return true;
            }
        }
        return false;
    }

    public static void setupRemoteServer(IRuntime rt, String hostname, String port, String user, String password) throws Exception {
        if (reuseExistingRemoteServer())
            return;
        server = RemoteTestUtil.setRemoteServerAttributes(rt, serverName, hostname, port, user, password);
        downloadedRemoteFiles = RemoteTestUtil.downloadRemoteServerFiles(rt, server, jmxConnection);
    }

    public static void createRemoteServer(IRuntime rt) throws Exception {
        String hostname = getRemoteHostName();
        String user = getRemoteUserName();
        String password = getRemotePassword();
        String port = getRemotePort();

        if (rt == null)
            throw new Exception("runtime cannot be null");

        // TODO: must validate that the runtime is a stub
        // if (!rt.isStub())
        //  throw new Exception("Remote servers only work with stubs");

        jmxConnection = RemoteTestUtil.setJMXConnection(getRemoteHostName(), getRemotePort(), getRemoteUserName(), getRemotePassword());
        assertTrue("Couldn't create connection to the remote server", jmxConnection.isConnected());

        setupRemoteServer(rt, hostname, port, user, password);
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
    }

    public static String getRemoteHostName() {
        return System.getProperty("liberty.remote.hostname");
    }

    public static String getRemoteUserName() {
        return System.getProperty("liberty.remote.username");
    }

    public static String getRemotePassword() {
        return System.getProperty("liberty.remote.password");
    }

    public static String getRemotePort() {
        return System.getProperty("liberty.remote.https.port");
    }

    public static String getRemoteCollectiveHostName() {
        return System.getProperty("liberty.remote.collective.hostname");
    }

    public static String getRemoteCollectiveUserName() {
        return System.getProperty("liberty.remote.collective.username");
    }

    public static String getRemoteCollectivePassword() {
        return System.getProperty("liberty.remote.collective.password");
    }

    public String getRemoteCollectivePort() {
        return System.getProperty("liberty.remote.collective.https.port");
    }

    public String getRemoteCollectiveKeyStorePassword() {
        return System.getProperty("liberty.remote.collective.keystorePassword");
    }

    protected void setServerStartTimeout(int timeout) throws Exception {
        ServerTestUtil.setServerStartTimeout(server, timeout);
    }

    protected void setServerStopTimeout(int timeout) throws Exception {
        ServerTestUtil.setServerStopTimeout(server, timeout);
    }

    protected void disableAutoPublish() throws Exception {
        ServerTestUtil.disableAutoPublish(server);
    }

    protected boolean isLooseCfg() {
        return ServerTestUtil.isLooseCfgEnabled();
    }

    /**
     * <p>
     * Creates a new VM within Eclipse with the specified name. This is useful for importing projects as it means that the VM is known before importing them so the following can be
     * added to the .classpath file:
     * </p>
     * <code>
     * &lt;classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/<b>&lt;jdkName&gt;</b>"&gt;<br/>
     * &nbsp;&nbsp;&lt;attributes&gt;<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;attribute name="owner.project.facets" value="java"/&gt;<br/>
     * &nbsp;&nbsp;&lt;/attributes&gt;<br/>
     * &lt;/classpathentry&gt;<br/>
     * </code>
     *
     * @param jdkName
     * @throws Exception
     */
    protected static void createVM(String jdkName) throws Exception {
        TestUtil.createVM(jdkName);
    }

    public static void importProjects(IPath srcWS, String[] projectNames) throws CoreException {
        importProjects(srcWS, projectNames, true);
    }

    /**
     * Import existing projects into the workspace
     *
     * @param srcWS        The source location of the project (all projects must be located in the same folder)
     * @param projectNames The names of the projects to import
     * @param copyProjects Whether project contents should be copied into the workspace or not
     * @throws CoreException
     */
    public static void importProjects(IPath srcWS, String[] projectNames, boolean copyProjects) throws CoreException {
        IPath srcPath = resourceFolder.append(srcWS);
        if (!copyProjects) {
            // Copy projects to a temp directory so the originals do not get modified
            IPath tmpPath = new Path(System.getProperty("java.io.tmpdir"));
            tmpPath = tmpPath.append("LibertyTestProjects");
            if (tmpPath.toFile().exists()) {
                try {
                    FileUtil.deleteDirectory(tmpPath.toOSString(), true);
                } catch (IOException e) {
                    print("Failed to delete the tmp directory: " + tmpPath.toOSString());
                }
            }
            try {
                ServerTestUtil.copyProjects(srcPath, projectNames, tmpPath);
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
            }
            srcPath = tmpPath;
        }
        ServerTestUtil.importProjects(srcPath, projectNames, copyProjects);
    }

    protected static void forceBuild() throws CoreException {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();
    }

    protected static void waitForBuildToFinish() {
        boolean interrupted = true;
        while (interrupted) {
            try {
                Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD,
                                         new NullProgressMonitor());
                interrupted = false;
            } catch (InterruptedException e) {
                //
                print("waiting for build.");
            }
        }
    }

    public static void startServer() throws Exception {
        ServerTestUtil.startServer(server);
    }

    public static void debugServer() throws Exception {
        ServerTestUtil.debugServer(server);
    }

    public static void externallyStartServer(String mode) throws Exception {
        // start the server from a process
        ProcessBuilder pb = wsServer.getWebSphereRuntime().createProcessBuilder(mode, wsServer.getServerInfo());
        print("Externally start server cmd: ");
        for (String cmd : pb.command()) {
            System.out.print(cmd + " ");
        }
        pb.start();
        // give it 10 second for the server state to be updated
        int i = 20;
        while (server.getServerState() != IServer.STATE_STARTED && --i > 0) {
            print("Server state: " + server.getServerState());
            Thread.sleep(500);
        }

        // give externally started servers 10 seconds for the server mode to be updated
        i = 20;
        while (!server.getMode().equals(mode) && --i > 0) {
            print("Server state: " + server.getServerState());
            Thread.sleep(500);
        }

        print("Final server state: " + server.getServerState());
    }

    public static void wait(String msg, int ms) {
        WLPCommonUtil.wait(msg, ms);
    }

    /**
     * A convenience utility that will make the current thread wait for a condition to be satisfied.
     *
     * @param msg       message to be printed to system.out prior to waiting
     * @param interval  the amount of time to sleep in between checking the condition
     * @param timeout   the maximum amount of time to wait for the condition to be satisfied
     * @param condition a condition that can be defined with an implementation of {@link com.ibm.ws.st.core.tests.util.ICondition}
     */
    protected static boolean waitOnCondition(String msg, int interval, int timeout, ICondition condition) {
        return ServerTestUtil.waitOnCondition(msg, interval, timeout, condition);
    }

    public static void print(String msg) {
        WLPCommonUtil.print(msg);
    }

    public static void print(String msg, Throwable t) {
        WLPCommonUtil.print(msg, t);
    }

    public static void stopServer() throws Exception {
        ServerTestUtil.stopServer(server);
    }

    public static void removeApp(String appProjectName, long sleepMS) throws Exception {
        ServerTestUtil.removeApps(server, new String[] { appProjectName }, sleepMS);
        // Delete the tracker file after a removeApp call.  When an addApp occurs, a new entry will be added so we want to verify the 'new' content
        // instead of possibly the old one.  If verifying for a specific message, do it before calling removeApp
        // eg. 1. addApp(s)
        //     2. Verify tracker log to see if the message to indicate the application is ready with the provided link is available
        //     3. removeApp
        //     4. Delete log  <-- Essentially, we're removing the above message
        //     5. addApp(s)
        //     6. Verify tracker log for the new message and not the old one if the log was not deleted
        ServerTestUtil.removeConsoleLineTrackerLogForBrowserLinks();
    }

    public static void removeApps(String... projectNames) throws Exception {
        ServerTestUtil.removeApps(server, projectNames, 1000);
        // Delete the tracker file after a removeApps call.  When an addApp occurs, a new entry will be added so we want to verify the 'new' content
        // instead of possibly the old one.  If verifying for a specific message, do it before calling removeApps.
        // eg. 1. addApp(s)
        //     2. Verify tracker log to see if the message to indicate the application is ready with the provided link is available
        //     3. removeApp
        //     4. Delete log  <-- Essentially, we're removing the above message
        //     5. addApp(s)
        //     6. Verify tracker log for the new message and not the old one if the log was not deleted
        ServerTestUtil.removeConsoleLineTrackerLogForBrowserLinks();
    }

    public static void addApp(String appProjectName) throws Exception {
        addApp(appProjectName, true, 30);
    }

    /**
     * This will add the specified project as an IModule to the {@link #server}. It will then wait for a success message () to appear in the console.
     *
     * @param appProjectName
     * @throws Exception
     */
    public static void addApp(String appProjectName, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        addApps(new String[] { appProjectName }, checkConsoleForAppStart, timeOut);
    }

    public static void addApps(String... projectNames) throws Exception {
        addApps(projectNames, true, 100);
    }

    public static void addApps(String[] projectNames, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        addApps(projectNames, true, checkConsoleForAppStart, timeOut);
    }

    public static void addApps(String[] projectNames, boolean publish, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        String l = (wsServer.isLooseConfigEnabled()) ? "looseConfig" : "non_looseConfig";
        print("Adding " + Arrays.toString(projectNames) + " to the server using " + l);
        ServerTestUtil.addApps(server, projectNames, publish, checkConsoleForAppStart, timeOut);
        if (publish) {
            ServerTestUtil.checkTracker(projectNames, server);
        }
    }

    public static void startApp(String appProjectName, int timeOut) throws Exception {
        IModule module = ModuleHelper.getModule(appProjectName);
        IModule[] modules = new IModule[] { module };
        moduleStart(modules);

        // Wait for the app to start
        int timeRun = 0;

        while (server.getModuleState(modules) != IServer.STATE_STARTED && timeRun < timeOut) {
            try {
                print("waiting for application to start: " + appProjectName + " " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }
        assertTrue("The app wasn't started before a 30s timeout expired", server.getModuleState(modules) == IServer.STATE_STARTED);
    }

    public static void stopApp(String appProjectName, int timeOut) throws Exception {
        IModule module = ModuleHelper.getModule(appProjectName);
        IModule[] modules = new IModule[] { module };
        moduleStop(modules);

        // Wait for the app to start
        int timeRun = 0;

        while (server.getModuleState(modules) != IServer.STATE_STOPPED && timeRun < timeOut) {
            try {
                print("waiting for application to stop: " + appProjectName + " " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }
        assertTrue("The app wasn't stopped before a 30s timeout expired", server.getModuleState(modules) == IServer.STATE_STOPPED);
    }

    public void testWebPageWithURLParam(String partialURL, String verifyString, String param) throws Exception {
        ServerTestUtil.testWebPageWithURLParam(server, partialURL, verifyString, param);
    }

    public void testPingSecureWebPage(String partialURL, String verifyString, String auth) throws Exception {
        ServerTestUtil.testPingSecureWebPage(server, partialURL, verifyString, auth);
    }

    public void testPingWebPage(String partialURL, String verifyString) throws Exception {
        testPingWebPage(partialURL, verifyString, null);
    }

    public void testPingWebPage(String partialURL, String verifyString, String basicAuth) throws Exception {
        ServerTestUtil.testPingWebPage(server, partialURL, verifyString, basicAuth);
    }

    public static void pingWebPage(URL url, String verifyString) throws Exception {
        ServerTestUtil.pingWebPage(url, verifyString, null);
    }

    public void testWebPageNotFound(String partialURL, String verifyString) throws Exception {
        ServerTestUtil.testWebPageNotFound(server, partialURL, verifyString, null);
    }

    public static IProject getProject(String projectName) {
        return ServerTestUtil.getProject(projectName);
    }

    public static void updateFile(IPath sourceFile, IProject desProject, String desRelativeFile, long waitTime) throws Exception {
        ServerTestUtil.updateFile(server, sourceFile, desProject, desRelativeFile, waitTime);
    }

    public static void copyFile(IPath sourceFile, IPath destinationFile) throws Exception {
        ServerTestUtil.copyFile(sourceFile, destinationFile);
    }

    public static void deleteFile(IProject project, String relativeFile, long waitTime) throws Exception {
        ServerTestUtil.deleteFile(server, project, relativeFile, waitTime);
    }

    protected void cleanUp() {
        ServerTestUtil.cleanUp(server);
    }

//    protected static void safePublishIncremental(IServer server) throws CoreException {
//        safePublish(server, IServer.PUBLISH_INCREMENTAL);
//    }
//
//    protected static void safePublishFull(IServer server) throws CoreException {
//        safePublish(server, IServer.PUBLISH_FULL);
//    }
//
//    public static void safePublish(IServer server, int publishType) throws CoreException {
//        IJobManager jobManager = Job.getJobManager();
//
//        ISchedulingRule rule = server;
//        jobManager.beginRule(rule, new NullProgressMonitor());
//
//        IStatus status = null;
//        try {
//            status = server.publish(publishType, new NullProgressMonitor());
//        } finally {
//            jobManager.endRule(rule);
//        }
//        if (status == null) {
//            status = new Status(IStatus.ERROR, "Safe publish failed for server: " + server.getName(), null);
//        }
//        if (!Status.OK_STATUS.equals(status)) {
//            print("Safe publish did not return ok status, status code: " + status.getCode() + ", status message: " + status.getMessage());
//            throw new CoreException(status);
//        }
//    }

    public static void safePublishIncremental(IServer server) {
        ServerTestUtil.safePublishIncremental(server);
    }

    public static void safePublishFull(IServer server) {
        ServerTestUtil.safePublishFull(server);
    }

    public static void moduleStart(IModule[] module) {
        final IStatus[] status = new IStatus[1];
        server.startModule(module, new IServer.IOperationListener() {
            @Override
            public void done(IStatus result) {
                status[0] = result;
            }
        });

        // Wait for publish to finish
        int timeRun = 0;
        while (status[0] == null && timeRun < 60) {
            try {
                print("Waiting for module start to finish: " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }

        assertTrue("Module start did not finish before a 60s timeout expired", status[0] != null);

        if (status[0] != null) {
            assertTrue("Module start failed: " + status[0].getCode() + ", " + status[0].getMessage(), status[0].isOK());
        }

    }

    public static void moduleStop(IModule[] module) {
        final IStatus[] status = new IStatus[1];
        server.stopModule(module, new IServer.IOperationListener() {
            @Override
            public void done(IStatus result) {
                status[0] = result;
            }
        });

        // Wait for publish to finish
        int timeRun = 0;
        while (status[0] == null && timeRun < 60) {
            try {
                print("Waiting for module stop to finish: " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }

        assertTrue("Module stop did not finish before a 60s timeout expired", status[0] != null);

        if (status[0] != null) {
            assertTrue("Module stop failed: " + status[0].getCode() + ", " + status[0].getMessage(), status[0].isOK());
        }

    }

    protected String getServerTypeId() {
        return WLPCommonUtil.SERVER_ID;
    }

    protected static int waitForLaunch(ILaunch launch) throws DebugException, TimeoutException {
        try {
            // Add stream listeners.  Inside a try/catch block since this is just for
            // debugging purposes and should not cause the test case to fail.
            launch.getProcesses()[0].getStreamsProxy().getOutputStreamMonitor().addListener(new IStreamListener() {
                @Override
                public void streamAppended(String text, IStreamMonitor monitor) {
                    print("Launch output stream: " + text);
                }
            });
            launch.getProcesses()[0].getStreamsProxy().getErrorStreamMonitor().addListener(new IStreamListener() {
                @Override
                public void streamAppended(String text, IStreamMonitor monitor) {
                    print("Launch error stream: " + text);
                }
            });
        } catch (Exception e) {
            print("Failed to add stream listeners to the launch process.", e);
        }

        // Wait for the launch to finish
        for (int i = 0; i < LAUNCH_TIMEOUT * 10; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            if (launch.isTerminated())
                return launch.getProcesses()[0].getExitValue();
        }

        launch.terminate();
        throw new TimeoutException("Process did not complete and had to be terminated");
    }

    protected String getJMXMBeanAttributeValue(String objectName, String attribute, String key) throws Exception {
        String value = null;
        TabularData data = (TabularData) jmxConnection.getMBeanAttribute("java.lang:type=Runtime", "SystemProperties");
        Collection<?> rows = data.values();
        for (Object o : rows) {
            CompositeData cData = (CompositeData) o;
            if (cData.get("key").equals(key)) {
                value = (String) cData.get("value");
            }
        }
        return value;
    }

    @Test
    public boolean testServerState(final int state) {

        final IServer serverf = server;
        assertNotNull("server is null. cannot run the test", serverf);

        String msg = "";
        switch (state) {
            case IServer.STATE_STARTED:
                msg = "Started";
                break;
            case IServer.STATE_STOPPED:
                msg = "Stopped";
        }

        return waitOnCondition("Waiting for server status to go to " + msg, 1000, 80000, new ICondition() {

            @Override
            public boolean isSatisfied() {
                print("server state: " + serverf.getServerState());
                return serverf.getServerState() == state;
            }
        });
    }

    public boolean configFileHasInclude(String includeFileName) {
        ConfigurationFile serverConfig = wsServer.getServerInfo().getConfigRoot();
        for (ConfigurationFile config : serverConfig.getAllIncludedFiles()) {
            if (config.getPath().lastSegment().equals(includeFileName)) {
                return true;
            }
        }
        return false;
    }

    public void switchconfig(boolean looseconfig) throws CoreException {
        print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
        if (wsServer.isLooseConfigEnabled() != looseconfig) {
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
            wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, looseconfig);
            wc.save(true, null);
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            WebSphereServerBehaviour webSphereServerBehaviour = wsServer.getWebSphereServerBehaviour();
            AbstractServerBehaviourExtension ext = (AbstractServerBehaviourExtension) webSphereServerBehaviour.getAdapter(AbstractServerBehaviourExtension.class);
            if (ext != null) {
                IStatus status = ext.preSaveLooseConfigChange(webSphereServerBehaviour, looseconfig);
                if (status != Status.OK_STATUS) {
                    print("Failed to change loose config mode on the " + server.getName() + " server to " + (looseconfig ? "enabled" : "disabled"));
                    throw new CoreException(status);
                }
                TestUtil.jobWait(WebSphereLaunchConfigurationDelegate.INTERNAL_LAUNCH_JOB_FAMILY);
            }
        }
        safePublishIncremental(server);
        print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
    }

    public static String getStateMsg(int expected, int actual) {
        return "Expected server state is " + expected + " but actual state is " + actual;
    }

    public void copySharedResources(String resourceloc, String runtimeLoc) throws IOException {
        if (resourceloc != null) {
            IPath resourcePath = getInstallLocation().append(resourceloc);
            IPath runtimePath = (new Path(runtimeLocation + runtimeLoc));
            FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());
        }
    }

    public static String getParamString(Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0)
                postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        return postData.toString();
    }

    public static String readConsole() throws Exception {
        IPath consolePath = wsServer.getServerInfo().getMessagesFile();
        ConsoleStreamsProxy streamsProxy = new ConsoleStreamsProxy(consolePath.toFile(), false, null);
        Thread.sleep(5000);
        String result = streamsProxy.getOutputStreamMonitor().getContents();
        streamsProxy.close();
        return result;
    }

    public static String getRemoteServerName() throws Exception {
        return RemoteUtils.getServerName(jmxConnection);
    }

    //Method from stackoverflow to reset the environment variables for tests
    //http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java#comment12192172_7201825
    @SuppressWarnings("unchecked")
    public static void setEnvironmentVariables(Map<String, String> newenv) {
        try {
            //Windows
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                //Linux
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e1) {
                Trace.logError("Failed to reset environment variables on Linux.", e1);
            }
        } catch (Exception e2) {
            Trace.logError("Failed to reset environment variables on Windows.", e2);
        }
    }

    protected boolean hasJakartaFeatures() {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        FeatureSet set = wr.getInstalledFeatures();
        return set.isFeatureSupported("servlet-5.0");
    }
}
