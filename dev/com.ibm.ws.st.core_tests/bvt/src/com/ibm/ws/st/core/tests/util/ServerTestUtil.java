/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.security.LibertySecurityHelper;
import com.ibm.ws.st.core.tests.StandaloneLibertyTestExt;
import com.ibm.ws.st.core.tests.TestBaseExtensionFactory;
import com.ibm.ws.st.core.tests.TestBaseInterface;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.ui.internal.ConsoleLineTracker;

/**
 *
 */
public class ServerTestUtil {

    public static final String SERVER_ID = "com.ibm.ws.st.server.wlp";
    public static final String RUNTIME_ID = "com.ibm.ws.st.runtime.wlp";

    public static final String SERVER_TYPE_PROP = "liberty.servertype";
    public static final String LOOSE_CFG_MODE_PROP = "liberty.loosecfg";

    private static TestBaseInterface testBaseExtension = null;

    public static IServer createServer(IRuntime rt, Map<String, String> serverInfo) throws Exception {
        TestBaseInterface testExt = getTestBaseExtension();
        return testExt.createServer(rt, serverInfo);
    }

    public static void setServerStartTimeout(IServer server, int timeout) throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.setAttribute(Server.PROP_START_TIMEOUT, timeout);
        wc.save(true, null);
    }

    public static void setServerStopTimeout(IServer server, int timeout) throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.setAttribute(Server.PROP_STOP_TIMEOUT, timeout);
        wc.save(true, null);
    }

    public static void disableAutoPublish(IServer server) throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.setAttribute(Server.PROP_AUTO_PUBLISH_SETTING, Server.AUTO_PUBLISH_DISABLE);
        wc.save(true, null);
    }

    public static void startServer(IServer server) throws Exception {
        startServer(server, ILaunchManager.RUN_MODE);
    }

    public static void debugServer(IServer server) throws Exception {
        startServer(server, ILaunchManager.DEBUG_MODE);
    }

    @SuppressWarnings("deprecation")
    private static void startServer(IServer server, String mode) throws Exception {
        if (server.getServerState() == IServer.STATE_STARTED) {
            print("Server already started.");
            return;
        }

        print("Starting server in mode: " + mode + ".");
        print("Initial server state: " + server.getServerState());

        server.synchronousStart(mode, null);

        print("Server start operation completed.");
        // give it 10 second for the server state to be updated (for CCB build)
        int i = 20;
        while (server.getServerState() != IServer.STATE_STARTED && --i > 0) {
            print("Server state: " + server.getServerState());
            Thread.sleep(500);
        }

        print("Final server state: " + server.getServerState());
        print("Exit startServer()");
    }

    public static void stopServer(IServer server) throws Exception {
        if (server.getServerState() == IServer.STATE_STOPPED) {
            print("Server already stopped.");
            return;
        }

        print("Stopping server");
        print("Initial server state: " + server.getServerState());
        final IStatus[] opStatus = new IStatus[1];
        opStatus[0] = null;
        server.stop(false, new IOperationListener() {
            @Override
            public void done(IStatus result) {
                opStatus[0] = result;
            }
        });
        int count = 0;
        boolean jobDone = TestUtil.jobWait(ServerUtil.SERVER_JOB_FAMILY);
        assertTrue(jobDone);
        while (opStatus[0] == null) { // wait to stop or failed
            try {
                Thread.sleep(300);
                print("Sleep for server stop operation.");
            } catch (InterruptedException e) {
                //do nothing
            }
            if (++count >= 5) {
                count = 0;
                print("Waiting for server to stop.");
            }
        }

        print("Server stop operation status: " + opStatus[0]);

        print("Server stop operation completed.");
        // give it 30 second for the server state to be updated (for CCB build)
        int i = 60;
        while (server.getServerState() != IServer.STATE_STOPPED && --i > 0) {
            print("Server state: " + server.getServerState());
            Thread.sleep(500);
        }
        print("Final server state: " + server.getServerState());

        assertEquals("The server did not stop", IServer.STATE_STOPPED, server.getServerState());
        print("Exit stopServer().");
    }

    public static void addApp(IServer server, String appProjectName) throws Exception {
        addApp(server, appProjectName, true, 30);
    }

    public static void addApp(IServer server, String appProjectName, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        addApps(server, new String[] { appProjectName }, checkConsoleForAppStart, timeOut);
    }

    public static void addApps(IServer server, String[] projectNames, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        addApps(server, projectNames, true, checkConsoleForAppStart, timeOut);
    }

    public static void addApps(IServer server, String[] projectNames, boolean publish, boolean checkConsoleForAppStart, int timeOut) throws Exception {
        List<IModule> moduleList = new ArrayList<IModule>(projectNames.length);
        for (String projectName : projectNames) {
            IModule module = ModuleHelper.getModule(projectName);
            if (module == null)
                print("Could not find module for project " + projectName + ".");
            else
                moduleList.add(module);
        }
        IModule[] modules = moduleList.toArray(new IModule[moduleList.size()]);

        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(modules, null, null);
        IServer curServer = wc.save(true, null);

        if (publish) {
            safePublishIncremental(curServer);

            // Wait for the applications to start
            int timeRun = 0;
            boolean allStarted = false;
            while (timeRun < timeOut) {
                allStarted = true;
                for (IModule module : moduleList) {
                    if (curServer.getModuleState(new IModule[] { module }) != IServer.STATE_STARTED)
                        allStarted = false;
                }
                if (allStarted)
                    break;
                try {
                    print("waiting for application(s) to start: " + Arrays.toString(projectNames) + " " + timeRun);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                timeRun++;
            }
            if (checkConsoleForAppStart) {
                assertTrue("The application(s) weren't started before a 30s timeout expired", allStarted);
            }
        }
    }

    public static void removeApp(IServer server, String appProjectName, long sleepMS) throws Exception {
        removeApps(server, new String[] { appProjectName }, sleepMS);
    }

    public static void removeApps(IServer server, String[] projectNames, long sleepMS) throws Exception {
        List<IModule> moduleList = new ArrayList<IModule>(projectNames.length);
        for (String projectName : projectNames) {
            IModule module = ModuleHelper.getModule(projectName);
            if (module == null)
                print("Could not find module for project " + projectName + ".");
            else
                moduleList.add(module);
        }
        IModule[] modules = moduleList.toArray(new IModule[moduleList.size()]);

        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(null, modules, null);
        IServer curServer = wc.save(true, null);
        safePublishIncremental(curServer);
        if (sleepMS > 0)
            Thread.sleep(sleepMS);
    }

    public static void updateFile(IServer server, IPath sourceFile, IProject desProject, String desRelativeFile, long waitTime) throws Exception {
        copyFile(sourceFile, desProject.getLocation().append(desRelativeFile));
        refreshAndPublish(server, desProject, waitTime);
    }

    public static void updateFileNoPublish(IServer server, IPath sourceFile, IProject desProject, String desRelativeFile) throws Exception {
        copyFile(sourceFile, desProject.getLocation().append(desRelativeFile));
        refreshProject(desProject);
    }

    public static void deleteFile(IServer server, IProject project, String relativeFile, long waitTime) throws Exception {
        IFile file = project.getFile(new Path(relativeFile));
        file.delete(true, null);
        refreshAndPublish(server, project, waitTime);
    }

    public static void copyFile(IPath sourceFile, IPath destinationFile) throws Exception {
        FileUtil.copyFile(sourceFile.toOSString(), destinationFile.toOSString());
    }

    public static void refreshProject(IProject project) throws CoreException {
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    }

    public static void refreshAndPublish(IServer server, IProject desProject, long waitTime) throws CoreException {
        //Refresh the resource
        refreshProject(desProject);

        // Do a publish.  This will ensure that the update is complete
        safePublishIncremental(server);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void safePublishIncremental(IServer server) {
        safePublish(server, IServer.PUBLISH_INCREMENTAL);
    }

    public static void safePublishFull(IServer server) {
        safePublish(server, IServer.PUBLISH_FULL);
    }

    public static void safePublish(IServer server, int publishType) {
        final IStatus[] status = new IStatus[1];
        server.publish(publishType, null, null, new IServer.IOperationListener() {
            @Override
            public void done(IStatus result) {
                status[0] = result;
            }
        });

        // Wait for publish to finish
        int timeRun = 0;
        String safePublishTimeout = System.getProperty("safe.publish.timeout");
        int timeout = 60;
        if (safePublishTimeout != null) {
            timeout = Integer.parseInt(safePublishTimeout);
        }
        while (status[0] == null && timeRun < timeout) {
            try {
                print("Waiting for publish to finish: " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }

        assertTrue("Safe publish did not finish before a 60s timeout expired", status[0] != null);

        if (status[0] != null) {
            if (!status[0].isOK()) {
                print(status[0].toString());
                printExceptions(status[0]);
            }
            assertTrue("Safe publish failed: " + status[0].getCode() + ", " + status[0].getMessage(), status[0].isOK());
        }
    }

    public static void testPingWebPage(IServer server, String partialURL, String verifyString) throws Exception {
        testPingWebPage(server, partialURL, verifyString, null);
    }

    public static void testPingWebPage(IServer server, String partialURL, String verifyString, String basicAuth) throws Exception {
        URL url = getURL(server, partialURL);
        pingWebPage(url, verifyString, basicAuth);
    }

    public static void testPingSecureWebPage(IServer server, String PartialURL, String verifyString, String auth) throws Exception {
        boolean found = false;
        HttpURLConnection connection = null;
        BufferedReader in = null;
        URL url = null;
        Throwable exp = null;
        String reponseMessage = null;
        int responsecode = -1;
        url = getURL(server, PartialURL);
        String encoding = DatatypeConverter.printBase64Binary(auth.getBytes("UTF-8"));
        for (int i = 0; i < 20; i++) {
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                //authorization property required for login.
                connection.setRequestProperty("Authorization", "Basic " + encoding);
                responsecode = connection.getResponseCode();
                reponseMessage = connection.getResponseMessage();
                if (responsecode == HttpURLConnection.HTTP_OK) {
                    break;
                }

            } catch (Throwable e) {
                exp = e;
            }
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
            Thread.sleep(500);
        }
        if (connection == null) {
            throw new Exception("Could not connect to: " + url + ", code: " + responsecode + ", message: " + reponseMessage, exp);
        }

        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (Throwable e) {
            connection.disconnect();
            throw new Exception("Could not open input stream for: " + url, e);
        }

        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                print(inputLine);
                if (inputLine.indexOf(verifyString) != -1) {
                    found = true;
                    break;
                }
            }
        } finally {
            in.close();
            connection.disconnect();
        }

        assertTrue("testPingSecureWebPage did not find expected text: " + verifyString, found);
    }

    public static void testWebPageWithURLParam(IServer server, String partialURL, String verifyString, String param) throws Exception {
        byte[] postData = param.getBytes(Charset.forName("UTF-8"));
        URL url = getURL(server, partialURL);
        HttpURLConnection conn = null;
        int code = -1;
        String message = null;
        Throwable exp = null;
        for (int i = 0; i < 15; i++) {
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postData);
                code = conn.getResponseCode();
                message = conn.getResponseMessage();
                if (code == HttpURLConnection.HTTP_OK) {
                    break;
                }
            } catch (Throwable e) {
                exp = e;
            }
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }

            Thread.sleep(1000 * (i + 1));
        }
        if (conn == null) {
            throw new Exception("Could not connect to: " + url + ", code: " + code + ", message: " + message, exp);
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (Throwable e) {
            conn.disconnect();
            throw new Exception("Could not open input stream for: " + url, e);
        }

        StringBuilder stringBuilder = new StringBuilder();
        boolean found = false;
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                print(inputLine);
                stringBuilder.append(inputLine);
                if (inputLine.indexOf(verifyString) != -1) {
                    found = true;
                    break;
                }
            }
        } finally {
            in.close();
            conn.disconnect();
        }

        assertTrue("testWebPageWithURLParam did not find expected text: " + verifyString + ", instead got: " + stringBuilder.toString(), found);
    }

    public static void pingWebPage(URL url, String verifyString, String basicAuth) throws Exception {
        print("Pinging web page at URL: " + url);
        HttpURLConnection conn = null;
        int code = -1;
        String message = null;
        Throwable exp = null;
        for (int i = 0; i < 20; i++) {
            try {
                conn = getConnection(url, basicAuth);
                code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    message = conn.getResponseMessage();
                } else {
                    break;
                }
            } catch (Throwable e) {
                exp = e;
            }
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
            Thread.sleep(500);
        }
        if (conn == null) {
            throw new Exception("Could not connect to: " + url + ", code: " + code + ", message: " + message, exp);
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (Throwable e) {
            conn.disconnect();
            throw new Exception("Could not open input stream for: " + url, e);
        }

        boolean found = false;
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                print(inputLine);
                if (inputLine.indexOf(verifyString) != -1) {
                    found = true;
                    break;
                }
            }
        } finally {
            in.close();
            conn.disconnect();
        }

        assertTrue("PingWebPage did not find expected text: " + verifyString, found);
    }

    public static void testWebPageNotFound(IServer server, String partialURL, String verifyString) throws Exception {
        testWebPageNotFound(server, partialURL, verifyString, null);
    }

    public static void testWebPageNotFound(IServer server, String partialURL, String verifyString, String basicAuth) throws Exception {
        try {
            URL url = getURL(server, partialURL);
            HttpURLConnection conn = null;
            for (int i = 0; i < 20; i++) {
                try {
                    conn = getConnection(url, basicAuth);
                    int code = conn.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK) {
                        return;
                    }
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                        conn = null;
                    }
                }
                Thread.sleep(500);
            }
            assertTrue("Web page found when it should not be, partial URL: " + partialURL, false);
        } catch (Exception e) {
            // this is expected
        }
    }

    public static boolean supportsLooseCfg(IServer server) throws Exception {
        TestBaseInterface testExt = getTestBaseExtension();
        return testExt.supportsLooseCfg(server);
    }

    public static void cleanUp() {
        TestBaseInterface testExt = getTestBaseExtension();
        testExt.cleanUp();
    }

    public static void cleanUp(IServer server) {
        TestBaseInterface testExt = getTestBaseExtension();
        testExt.cleanUp(server);
    }

    public static IProject getProject(String projectName) {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    }

    public static void importProjects(IPath importRoot, String[] projectNames) throws CoreException {
        importProjects(importRoot, projectNames, true);
    }

    /**
     * Import existing projects into the workspace.
     *
     * @param importRoot The root folder where the projects to import are located.
     *            All projects must be located in the same folder.
     * @param projectNames The names of the projects to be imported
     * @param copyProjects Whether to copy the project contents into the workspace or not
     * @throws CoreException
     */
    public static void importProjects(IPath importRoot, String[] projectNames, boolean copyProjects) throws CoreException {
        if (copyProjects) {
            for (String projectName : projectNames) {
                ImportUtil.importExistingProjectIntoWorkspace(projectName, importRoot);
            }
        } else {
            for (String projectName : projectNames) {
                importProject(importRoot, projectName);
            }
        }

        // Force a build to be on the safe side
        forceBuild();
    }

    public static IProject importProject(IPath importRoot, String projectName) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project.exists()) {
            print("The " + projectName + " project already exists");
            if (!project.isOpen())
                project.open(new NullProgressMonitor());
            return project;
        }

        IProjectDescription pd = ResourcesPlugin.getWorkspace().loadProjectDescription(importRoot.append(projectName).append(IProjectDescription.DESCRIPTION_FILE_NAME));
        project.create(pd, new NullProgressMonitor());
        if (!project.isOpen())
            project.open(new NullProgressMonitor());

        return project;
    }

    /**
     * Copy the specified projects to the target directory
     *
     * @param sourceRoot The root folder where the projects to copy are located.
     *            All projects must be located in the same folder.
     * @param projectNames The names of the projects to copy
     * @param targetRoot The target folder for the projects
     * @throws IOException
     */
    public static void copyProjects(IPath sourceRoot, String[] projectNames, IPath targetRoot) throws IOException {
        for (String projectName : projectNames) {
            IPath srcDir = sourceRoot.append(projectName);
            IPath targetDir = targetRoot.append(projectName);
            FileUtil.copyFiles(srcDir.toOSString(), targetDir.toOSString());
        }
    }

    public static void deleteProject(String projectName) {
        IProject project = getProject(projectName);
        if (project != null) {
            deleteProject(getProject(projectName));
        } else {
            print("Could not find a project with the name: " + projectName);
        }
    }

    public static void deleteProject(IProject project) {
        try {
            project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
        } catch (Exception e) {
            print("Failed to clean up project: " + project.getName(), e);
        }
    }

    public static boolean isLooseCfgEnabled() {
        String value = System.getProperty(LOOSE_CFG_MODE_PROP);
        if (value != null && value.equalsIgnoreCase("true")) {
            print("Loose configuration mode is enabled");
            return true;
        }
        print("Loose configuration mode is disabled");
        return false;
    }

    public static void checkTracker(String[] projectNames, IServer server) throws Exception {

        String trackerValue = System.getProperty(ConsoleLineTracker.CONSOLE_LINE_TRACKER_TEST);
        // The test cases will be adding EARs or WAR, etc (projectNames).  The EAR project name
        // will not be part of the URL.   So, retrieve the list of modules deployed to the server
        // and use that to check the console.  Otherwise, for WARs, we can just use the projectName.
        // check the console to see that the application is available AND the mapped URL is correct.
        if (trackerValue != null) {
            for (int i = 0; i < projectNames.length; i++) {
                IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectNames[i]);
                IModule[] modules = ServerUtil.getModules(project);
                if (modules.length > 0) {
                    IModule[] childModules = server.getChildModules(modules, null);
                    int len = childModules.length;
                    if (len > 0) { // EAR with one or more WARs
                        for (int j = 0; j < len; j++) {
                            String id = childModules[j].getModuleType().getId();
                            // Only valid applications will appear.  eg. not jst.utility
                            if (id.equals("jst.web") || id.equals("jst.ejb")) {
                                IProject mProject = childModules[j].getProject();
                                checkConsoleTrackerContentsForUrl(mProject.getName(), server);
                            }
                        }
                    } else { // WAR
                        checkConsoleTrackerContentsForUrl(project.getName(), server);
                    }
                } else {
                    checkConsoleTrackerContentsForUrl(projectNames[i], server);
                }
            }
        }
    }

    public static void removeConsoleLineTrackerLogForBrowserLinks() {
        String trackerValue = System.getProperty(ConsoleLineTracker.CONSOLE_LINE_TRACKER_TEST);
        if (trackerValue != null) {
            File consoleLineTrackerLog = Activator.getInstance().getStateLocation().append(ConsoleLineTracker.BROWSER_LINK_LOG).toFile();
            if (consoleLineTrackerLog.exists()) {
                consoleLineTrackerLog.delete();
            }
        }
    }

    /**
     * A convenience utility that will make the current thread wait for a condition to be satisfied.
     *
     * @param msg message to be printed to system.out prior to waiting
     * @param interval the amount of time to sleep in between checking the condition
     * @param timeout the maximum amount of time to wait for the condition to be satisfied
     * @param condition a condition that can be defined with an implementation of {@link com.ibm.ws.st.core.tests.util.ICondition}
     */
    public static boolean waitOnCondition(String msg, int interval, int timeout, ICondition condition) {
        print("Waiting: " + msg);
        int totalTime = 0;
        boolean result = false;
        try {
            do {
                totalTime += interval;
                Thread.sleep(interval);
                result = condition.isSatisfied();
            } while (!result && totalTime < timeout);
            print("Total time waited: " + totalTime);
            if (totalTime >= timeout)
                print("Timeout exceeded while waiting for condition to be satisfied.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void checkConsoleTrackerContentsForUrl(final String partialURL, IServer server) throws Exception {
        // Get base URL via WebSphereServer::getServerWebURL and compare it with console contents, which has the mapped link.
        // These should have the same authority: mappedHost:mappedPort   (The host and the port are mapped)
        final URL url = ServerTestUtil.getURL(server, partialURL);
        final File logFile = Activator.getInstance().getStateLocation().append(ConsoleLineTracker.BROWSER_LINK_LOG).toFile();
        waitOnCondition("Wait for consolelinetracker.log to appear", 1000, 30000, new ICondition() {

            @Override
            public boolean isSatisfied() {

                return logFile.exists();
            }
        });
        if (logFile.exists()) {
            // Mapped URL string is from ConsoleLineTracker
            boolean hasUrl = readerContains(logFile, "Mapped URL:", url.getAuthority() + "/" + partialURL);
            print("ConsoleLineTracker: The expected Mapped URL:" + url.getAuthority() + "/" + partialURL + " was " + (hasUrl ? "found." : "not found."));
            assertTrue("The expected Mapped URL:" + url.getAuthority() + "/" + partialURL + " was not found in the tracker file", hasUrl);
        } else {
            assertTrue("No tracker log file exists", logFile.exists());
        }
    }

    private static boolean readerContains(File inputFile, String startsWith, String text) {

        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(inputFile);
            br = new BufferedReader(fileReader);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(text) && line.startsWith(startsWith)) {
                    return true;
                }
            }
        } catch (Exception e) {
            print("Error obtaining or reading the file " + inputFile.getPath(), e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                print("Error closing the file streams.", e);
            }
        }
        return false;
    }

    private static void forceBuild() throws CoreException {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();
    }

    private static HttpURLConnection getConnection(URL url, String basicAuth) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setRequestMethod("GET");
        conn.setReadTimeout(15000);

        if (basicAuth != null) {
            conn.setRequestProperty("Authorization", basicAuth);
            setTrustAllCerts();
        }

        conn.connect();
        return conn;
    }

    public static URL getURL(IServer server, String partialUrl) throws Exception {
        // Should really use IURLProvider.getModuleRootURL() but that would require
        // a lot of test case changes so doing it this way for now.
        String baseUrl = getTestBaseExtension().getBaseURL(server);
        if (!partialUrl.startsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return new URL(baseUrl + partialUrl);
    }

    protected static void print(String str) {
        WLPCommonUtil.print(str);
    }

    protected static void print(String str, Throwable t) {
        WLPCommonUtil.print(str, t);
    }

    protected static void printExceptions(IStatus status) {
        if (status.getException() != null) {
            print("Status Exception", status.getException());
        }
        for (IStatus childStatus : status.getChildren()) {
            printExceptions(childStatus);
        }
    }

    private static String getTestServerType() {
        String serverType = System.getProperty(SERVER_TYPE_PROP);
        return serverType;
    }

    private static TestBaseInterface getTestBaseExtension() {
        if (testBaseExtension != null) {
            return testBaseExtension;
        }

        String testServerType = getTestServerType();
        if (testServerType != null) {
            testBaseExtension = TestBaseExtensionFactory.getTestBaseExtension(testServerType);
        }

        if (testBaseExtension == null) {
            testBaseExtension = new StandaloneLibertyTestExt();
        }

        return testBaseExtension;
    }

    private static void setTrustAllCerts() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = LibertySecurityHelper.getSSLContext();
        context.init(new KeyManager[0], new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                // TODO Auto-generated method stub

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                // TODO Auto-generated method stub

            }
        }
        }, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(
                                                      context.getSocketFactory());
    }

}
