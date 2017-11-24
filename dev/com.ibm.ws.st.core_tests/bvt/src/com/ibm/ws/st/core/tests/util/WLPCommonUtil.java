/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.tests.TestsPlugin;
import com.ibm.ws.st.core.tests.ToolsTestBase;

/**
 * Common utilities that can be used by external teams
 */
public class WLPCommonUtil {
    private static String wlpLocation = null;
    public static final String SERVER_ID = "com.ibm.ws.st.server.wlp";
    public static final String RUNTIME_ID = "com.ibm.ws.st.runtime.wlp";
    public static final String SERVER_V85_ID = "com.ibm.ws.st.server.v85.was";
    public static final String RUNTIME_V85_ID = "com.ibm.ws.st.runtime.v85.was";
    public static final String RUNTIME_LOCATION_PROPERTY = "was.runtime.liberty";
    public static final String SHIPPED_RUNTIME_LOCATION_PROPERTY = "was.runtime.liberty.shipped";
    public static final String ENABLE_JAVA2SECURITY_PROPERTY = "was.runtime.liberty.enableJava2Security";
    public static final String AUTO_EXPAND_APPS_PROPERTY = "was.runtime.liberty.autoExpandApps";
    public static final String HTTP_PORT_START_PROPERTY = "was.runtime.liberty.httpPortStart";
    // Sets console log format.  Valid settings are json or basic (default).
    public static final String CONSOLE_LOG_FORMAT_PROPERTY = "was.runtime.liberty.consoleLogFormat";
    // Sets additional messages to be sent to the console log.  Valid settings are message,trace,accessLog,ffdc.
    public static final String CONSOLE_LOG_SOURCE_PROPERTY = "was.runtime.liberty.consoleLogSource";
    public static boolean isDebugEnabled = true;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy k:mm:ss");
    private static final String DEFAULT_RUNTIME_NAME = "Liberty_Tools_Runtime";
    private static final boolean DEBUG_JOBS_ENABLED = true;//Boolean.TRUE.equals(System.getProperty("debugJobs"));
    private static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
    private static final String TEST_OVERRIDE_XML = "testOverride.xml";

    public enum RuntimeLocationType {
        LATEST(RUNTIME_LOCATION_PROPERTY),
        SHIPPED(SHIPPED_RUNTIME_LOCATION_PROPERTY);

        private final String key;

        private RuntimeLocationType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     */
    public static IServer createServer(IRuntime rt, String serverName, IPath serverConfigResourceLocation, MultiStatus status) {
        IServer[] servers = ServerCore.getServers();
        for (IServer sr : servers) {
            if (sr.getId().equals(serverName)) {
                status.add(Status.OK_STATUS);
                return sr;
            }
        }
        IServer server = null;
        try {
            IPath serverPath = (new Path(getWLPInstallDir())).append("/usr/servers/" + serverName);
            if (serverConfigResourceLocation != null) {
                FileUtil.copyFiles(serverConfigResourceLocation.toPortableString(), serverPath.toPortableString());
            }

            IServerType st = ServerCore.findServerType(SERVER_ID);
            IServerWorkingCopy wc = st.createServer(serverName, null, rt, null);
            wc.setAttribute(WebSphereServer.PROP_SERVER_NAME, serverName);
            server = wc.save(true, null);
            WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            WLPCommonUtil.basicServerSetup(serverPath);
            if (serverConfigResourceLocation != null) // we should refresh the runtime if the we create the server profile
                wsServer.getWebSphereRuntime().updateServerCache(true);
            WLPCommonUtil.jobWait(Constants.JOB_FAMILY);
        } catch (CoreException ce) {
            status.add(createStatus(IStatus.ERROR, "Failed to create server", ce));
        } catch (IOException ie) {
            status.add(createStatus(IStatus.ERROR, "Failed to create server", ie));
        }
        return server;
    }

    public static void waitForJobsToCompleted(String[] jobNames) {
        if (jobNames == null || jobNames.length == 0)
            return;

        IJobManager jobManager = Job.getJobManager();
        boolean completed = false;
        int printCounter = 0;
        while (!completed) {
            boolean stillRunning = false;
            String waitingJobName = "";
            for (Job job : jobManager.find(null)) {
                String name = job.getName();
                for (String jobName : jobNames) {
                    if (name.indexOf(jobName) >= 0) {
                        stillRunning = true;
                        waitingJobName = name;
                        break;
                    }
                }
                if (stillRunning)
                    break;
            }
            if (!stillRunning) {
                completed = true;
            } else {
                if (printCounter == 8) { // print each 2 seconds
                    ToolsTestBase.print("wait for " + waitingJobName + " to complete.");
                    printCounter = 0;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // do nothing
                }
                printCounter++;
            }
        }

    }

    private static IStatus createStatus(int sev, String msg, Throwable e) {
        return new Status(sev, TestsPlugin.PLUGIN_ID, msg, e);
    }

    public static void wait(String msg, int ms) {
        try {
            if (msg != null) {
                print(msg);
            }
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void print(String msg) {
        print(msg, null);
    }

    public static void print(String msg, Throwable t) {
        if (isDebugEnabled) {
            System.out.println(WLPCommonUtil.sdf.format(new Date()) + "  " + msg);
            if (t != null) {
                t.printStackTrace(System.out);
            }
        }
    }

    /**
     * If the test is running in Ian's LA framework, the runtime location is passed in via the -D parameter in the CIF file;
     * if it is running the CCB build, the -D parameter is not subsitituted. We need to point to the directory to where we download.
     *
     * @return the location of the WLP runtime
     */
    public static String getWLPInstallDir() {
        return getWLPInstallDir(RuntimeLocationType.LATEST);
    }

    public static String getWLPInstallDir(RuntimeLocationType locationType) {
        if (WLPCommonUtil.wlpLocation == null) {
            WLPCommonUtil.wlpLocation = getRuntimeLocation(locationType);
            if (WLPCommonUtil.wlpLocation != null && WLPCommonUtil.wlpLocation.indexOf('$') >= 0) {
                WLPCommonUtil.wlpLocation = TestsPlugin.getInstance().getStateLocation().append("wlp").toOSString();
            }
            print("WLP location = " + WLPCommonUtil.wlpLocation);
        }
        return WLPCommonUtil.wlpLocation;
    }

    public static String getRuntimeLocation() {
        return getRuntimeLocation(RuntimeLocationType.LATEST);
    }

    public static String getRuntimeLocation(RuntimeLocationType locationType) {
        String location = System.getProperty(locationType.getKey());
        return location;
    }

    // Wait on workspace jobs for the given family.
    public static boolean jobWait(Object family) {
        // noticed that some test cases hang while waiting for a joined build
        // job to finish, so use thread sleep instead.
        if (family == ResourcesPlugin.FAMILY_MANUAL_BUILD || family == ResourcesPlugin.FAMILY_AUTO_BUILD)
            return jobWaitSleep(family, 4, 180000);

        return jobWaitJoin(family, 1, 200);
    }

    public static boolean jobWaitBuildandResource() {
        return jobWaitBuildandResource("Waiting for build and resource change jobs");
    }

    public static boolean jobWaitBuildandResource(String message) {
        if (message != null)
            print(message);

        if (!jobWait(Constants.JOB_FAMILY))
            return false;

        if (!jobWait(ResourcesPlugin.FAMILY_AUTO_BUILD))
            return false;

        return jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
    }

    public static boolean jobWaitBuild() {
        if (!jobWait(ResourcesPlugin.FAMILY_AUTO_BUILD))
            return false;

        return jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
    }

    private static boolean jobWaitJoin(Object family, int failureThreshold, int waitInterval) {
        IJobManager manager = Job.getJobManager();

        int threshold = 0;
        boolean firstAttempt = true;
        while (threshold < failureThreshold) {
            Job[] jobs = manager.find(family);
            while (jobs.length > 0) {
                if (!joinJob(jobs[0])) {
                    return false;
                }

                threshold = 0;
                jobs = manager.find(family);
            }

            if (firstAttempt)
                firstAttempt = false;
            else
                ++threshold;

            try {
                Thread.sleep(waitInterval);
            } catch (Exception e) {
                // ignore
            }
        }

        return true;
    }

    private static boolean jobWaitSleep(Object family, int failureThreshold, long maxWaitTime) {
        IJobManager jobManager = Job.getJobManager();
        int printCounter = 0;
        int threshold = 0;
        long endTime = -1;
        String lastJobName = null;
        while (threshold < failureThreshold && (endTime == -1 || System.currentTimeMillis() < endTime)) {
            String waitingJobName = "";
            Job[] jobs = jobManager.find(family);

            if (jobs == null || jobs.length == 0) {
                ++threshold;
                printCounter = 0;
                lastJobName = null;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // do nothing
                }
            } else {
                String name = jobs[0].getName();
                if (endTime == -1)
                    endTime = System.currentTimeMillis() + maxWaitTime;

                waitingJobName = name;
                lastJobName = name;
                threshold = 0;

                if (printCounter == 8) { // print each 2 seconds
                    ToolsTestBase.print("Wait for " + waitingJobName + " to complete.");
                    printCounter = 0;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // do nothing
                }
                printCounter++;
            }
        }

        if (lastJobName != null && threshold < failureThreshold && System.currentTimeMillis() > endTime)
            print("Job " + lastJobName + "did not finish in time");

        return true;
    }

    public static IRuntime createRuntime(String runtimeName, IPath user_dir, MultiStatus status) {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        for (IRuntime r : runtimes) {
            if (r.getName().equals(runtimeName)) {
                print("Runtime exists.  Reuse.");
                status.add(Status.OK_STATUS);
                return r;
            }
        }
        IServerType st = ServerCore.findServerType(SERVER_ID);
        String wlpInstallDir = getWLPInstallDir();
        IPath runtimePath = new Path(wlpInstallDir);
        IRuntimeWorkingCopy wc;
        try {
            wc = st.getRuntimeType().createRuntime(null, null);
            wc.setLocation(runtimePath);
            if (runtimeName != null)
                wc.setName(runtimeName);
            if (user_dir != null) {
                WebSphereRuntime wr = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
                wr.addUserDirectory(user_dir);
            }
            waitForJobsToCompleted(new String[] { runtimeName }); // runtime validator job

            return wc.save(true, null);
        } catch (CoreException e) {
            status.add(createStatus(IStatus.ERROR, "Failed to create runtime:" + runtimeName, e));
        }
        return null;
    }

    public static IRuntime createRuntime(MultiStatus status) {
        return createRuntime(DEFAULT_RUNTIME_NAME, null, status);
    }

    @SuppressWarnings("deprecation")
    public static boolean startServer(IServer server, MultiStatus status) {
        if (server.getServerState() == IServer.STATE_STARTED) {
            print("Server already started.");
            status.add(createStatus(IStatus.OK, "Server already started", null));
            return true;
        }

        print("Starting server in run mode.");
        print("Initial server state: " + server.getServerState());

        try {
            server.synchronousStart(ILaunchManager.RUN_MODE, null);
        } catch (CoreException e1) {
            status.add(createStatus(IStatus.ERROR, "CoreException trying to start server: " + e1.getMessage(), null));
            status.add(e1.getStatus());
            return false;
        }

        print("Server start operation completed.");

        // give it 20 second for the server state to be updated (for CCB build)
        int i = 80;
        while (server.getServerState() != IServer.STATE_STARTED && --i > 0) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                //nothing
            }
        }

        boolean b;
        if (server.getServerState() != IServer.STATE_STARTED) {
            status.add(createStatus(IStatus.ERROR, "Failed to start server", null));
            b = false;
        } else {
            status.add(createStatus(IStatus.OK, "Server started", null));
            b = true;
        }
        print("Final server state: " + server.getServerState());
        print("Exit startServer()");
        return b;
    }

    public static boolean stopServer(IServer server, MultiStatus status) {
        if (server.getServerState() == IServer.STATE_STOPPED) {
            print("Server already stopped.");
            status.add(createStatus(IStatus.OK, "Server already stopped", null));
            return true;
        }

        print("Stopping server");
//        server.synchronousStop(false);
        final IStatus[] opStatus = new IStatus[1];
        opStatus[0] = null;
        server.stop(false, new IOperationListener() {
            @Override
            public void done(IStatus result) {
                opStatus[0] = result;
            }
        });
        int count = 0;
        while (opStatus[0] == null) { // wait to stop or failed
            try {
                Thread.sleep(300);
                print("Sleep for server stop operation.");
            } catch (InterruptedException e) {
                //do nothing
            }
            if (++count >= 5) {
                count = 0;
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Waiting for server to start.");
            }
        }

        print("Server stop operation completed.");

//        if (opStatus[0].getSeverity() == IStatus.ERROR) {
//            status.add(opStatus[0]);
//            return false;
//        }
        // give it 20 second for the server state to be updated (for CCB build)
        int i = 80;
        while (server.getServerState() != IServer.STATE_STOPPED && --i > 0) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // Nothing
            }
        }

        boolean b;
        if (server.getServerState() != IServer.STATE_STOPPED) {
            status.add(createStatus(IStatus.ERROR, "Failed to stop server", null));
            status.add(opStatus[0]);
            b = false;
        } else {
            status.add(createStatus(IStatus.OK, "Server stopped", null));
            b = true;
        }
        print("Exit stopServer().");
        return b;
    }

    private static boolean joinJob(Job job) {
        try {
            print("Before join job: " + job.getName());
            job.join();
            print("After join job: " + job.getName());
        } catch (InterruptedException e) {
            print("Interrupted join job: " + job.getName());
            return false;
        }
        return true;
    }

    public static void checkJobs(String message) {
        if (!DEBUG_JOBS_ENABLED)
            return;

        IJobManager jm = Job.getJobManager();
        Object[] families = new Object[] { ResourcesPlugin.FAMILY_AUTO_BUILD,
                                           ResourcesPlugin.FAMILY_MANUAL_BUILD,
                                           ResourcesPlugin.FAMILY_AUTO_REFRESH,
                                           ResourcesPlugin.FAMILY_MANUAL_REFRESH,
                                           Constants.JOB_FAMILY };
        String[] subMessages = new String[] { "  - auto build jobs are running",
                                              " - manual build jobs are running",
                                              " - auto refresh jobs are running",
                                              " - manual refresh jobs are running",
                                              " - " + Constants.JOB_FAMILY + " jobs are running" };

        try {
            Thread.sleep(50);
        } catch (Exception e) {
            // ignore
        }

        for (int i = 0; i < families.length; ++i) {
            Job[] jobs = jm.find(families[i]);
            if (jobs.length > 0) {
                System.out.println(message + subMessages[i]);
                for (Job job : jobs) {
                    System.out.println("  ==> " + job.getName() + " - " + job.getClass().getName() + "@" + job.hashCode());
                }
                System.out.flush();
            }
        }
    }

    public static void cleanUp() {
        // delete runtime can result in coreException. Exit cleanup if there is any exception.
        // When deleteRuntime fails delete project can end up deleting runtime project (wlp/usr directory)
        try {
            // Delete eclipse servers
            deleteAllServers();

            // Delete runtimes and liberty servers
            deleteAllRuntimes();
            // Delete any other projects
            deleteAllProjects();
        } catch (Exception e) {
            print("Error running clean up " + e.getMessage());
        }
        // wait for a few seconds to settle all jobs.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void deleteAllServers() {
        IServer[] servers = ServerCore.getServers();
        for (IServer server : servers) {
            WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            if (ws != null) {
                if (ws.isLocalSetup()) {
                    try {
                        // Make sure that local servers are stopped otherwise when the next test case
                        // tries to start a server the ports will be taken.
                        ServerTestUtil.stopServer(server);
                    } catch (Exception e1) {
                        print("Failed to stop server: " + ws.getServerName());
                    }
                }
                try {
                    server.delete();
                } catch (Exception e) {
                    print("Failed to clean up the server: " + ws.getServerName());
                    print(e.toString());
                }
            }
        }
    }

    public static void deleteAllRuntimes() throws Exception {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        if (runtimes == null || runtimes.length == 0)
            return;

        for (IRuntime runtime : runtimes) {
            WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            if (wr != null) {

                for (WebSphereServerInfo serverInfo : wr.getWebSphereServerInfos()) {
                    // Deletes the runtime server definition
                    wr.deleteServer(serverInfo, new NullProgressMonitor());
                    jobWait(Constants.JOB_FAMILY);
                }

                // Delete the eclipse runtime
                runtime.delete();
            }
        }
    }

    public static void deleteAllProjects() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        if (projects == null || projects.length == 0)
            return;

        for (IProject project : projects) {
            try {
                project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
            } catch (Exception e) {
                print("Failed to clean up project: " + project.getName());
                print(e.toString());
            }
        }
    }

    public static IPath getResourceFolder() {
        return TestsPlugin.getInstallLocation().append("resources");
    }

    public static void basicServerSetup(IPath serverPath) {
        WLPCommonUtil.ensureBootstrapProperties(serverPath);
        WLPCommonUtil.ensureDropinsOverrideXML(serverPath);
    }

    /**
     * Ensure that the bootstrap.properties exists for the server. If the
     * system property for Java2 security is set to true then add to the
     * bootstrap.properties.
     */
    public static void ensureBootstrapProperties(IPath serverLocation) {
        IPath path = serverLocation.append(BOOTSTRAP_PROPERTIES);
        File file = path.toFile();
        if (!file.exists()) {
            IPath fromPath = getResourceFolder().append(BOOTSTRAP_PROPERTIES);
            try {
                FileUtil.copyFile(fromPath.toOSString(), path.toOSString());
            } catch (IOException e) {
                print("Could not copy " + fromPath + " to " + path + ": " + e.getMessage());
            }
        }

        String prop = System.getProperty(ENABLE_JAVA2SECURITY_PROPERTY);
        if ("true".equals(prop)) {
            appendStringToFile(file, "\r\nwebsphere.java.security=true");
        }

        prop = System.getProperty(CONSOLE_LOG_FORMAT_PROPERTY);
        if (prop != null && !prop.isEmpty()) {
            appendStringToFile(file, "\r\ncom.ibm.ws.logging.console.format=" + prop);
        }

        prop = System.getProperty(CONSOLE_LOG_SOURCE_PROPERTY);
        if (prop != null && !prop.isEmpty()) {
            appendStringToFile(file, "\r\ncom.ibm.ws.logging.console.source=" + prop);
        }
    }

    /**
     * Ensure that the dropins override xml exists for the server. If the
     * system property for auto expand apps is set to true or not
     * set (it is the default) then add it to the include.xml.
     */
    public static void ensureDropinsOverrideXML(IPath serverLocation) {
        IPath path = serverLocation.append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);
        File file = path.toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        path = path.append(TEST_OVERRIDE_XML);
        file = path.toFile();
        if (!file.exists()) {
            StringBuilder builder = new StringBuilder();
            builder.append("<server>\n");
            String prop = System.getProperty(HTTP_PORT_START_PROPERTY);
            if (prop != null && !prop.isEmpty()) {
                try {
                    int httpPort = Integer.parseInt(prop);
                    if (httpPort > 0 && httpPort < 65535) {
                        int httpsPort = httpPort + 1;
                        builder.append("  <httpEndpoint id='defaultHttpEndpoint' httpPort='" + httpPort + "' httpsPort='" + httpsPort + "'/>\n");
                    } else {
                        print("Invalid value for " + HTTP_PORT_START_PROPERTY + " property: " + prop);
                    }
                } catch (NumberFormatException e) {
                    print("Invalid value for " + HTTP_PORT_START_PROPERTY + " property: " + prop, e);
                }
            }
            prop = System.getProperty(AUTO_EXPAND_APPS_PROPERTY);
            if (prop == null || prop.equals("true")) {
                builder.append("  <applicationManager autoExpand='true'/>\n");
            }
            builder.append("</server>");
            appendStringToFile(file, builder.toString());
        }
    }

    private static void appendStringToFile(File file, String str) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.write(str);
        } catch (Exception e) {
            print("Failed to append: '" + str + "' to file: " + file, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

}
