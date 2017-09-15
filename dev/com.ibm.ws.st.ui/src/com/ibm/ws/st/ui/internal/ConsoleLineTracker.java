/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.common.core.ext.internal.servertype.ServerTypeExtensionFactory;
import com.ibm.ws.st.core.internal.ConsoleLineTrackerHelper;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class ConsoleLineTracker implements IConsoleLineTracker {
    // [ERROR   ] SRVE0777E: Exception thrown by application class 'com.test.MyServlet.doGet():17'
    private static final String SERVLET_ERROR = "[ERROR   ] SRVE0777E";

    // [WARNING ] SRVE0269W: No Extension Processor found for handling JSPs
    private static final String JSP_WARNING = "[WARNING ] SRVE0269W: ";

    // [ERROR   ] CWNEN0030E: The Resource factory encountered a problem getting the object instance Reference
    private static final String JNDI_INJECT_ERROR = "[ERROR   ] CWNEN0030E:";

    //javax.naming.NoInitialContextException: Need to specify class name in environment or system property
    private static final String JNDI_NO_INITIAL_CONTEXT_EXCEPTION = "javax.naming.NoInitialContextException:";

    // [ERROR   ] CWWKE0040E: Platform bundles could not be resolved against cache. Restart the server with a cold start.
    private static final String COLD_START_ERROR = "[ERROR   ] CWWKE0040E: ";

    // [AUDIT   ] CWWKT0016I: Web application available (default_host): http://ithil.pok.ibm.com:8010/C1
    private static final String WEB_APP_ADD = "[AUDIT   ] CWWKT0016I: ";
    private static final String WEB_APP_REMOVE = "[AUDIT   ] CWWKT0017I: ";

    // [AUDIT   ] CWWKN2000A: HTTP Whiteboard context root added: http://localhost:9080/osgi/http
    private static final String HTTP_WHITEBOARD_ADD = "[AUDIT   ] CWWKN2000A: ";

    // [AUDIT   ] CWWKN2050A: OSGi Application console added at: http://localhost:9080/osgi/http/shared/system/console
    private static final String OSGI_APPCONSOLE_ADD = "[AUDIT   ] CWWKN2050A: ";

    // [AUDIT   ] CWWKN2051A: OSGi Application console removed at: http://9.41.49.189:8010/osgi/http/HttpWhiteboardExample/system/console
    private static final String OSGI_APPCONSOLE_REMOVE = "[AUDIT   ] CWWKN2051A: ";

    // [AUDIT   ] CWWKN2001A: HTTP Whiteboard context root removed: http://9.41.49.189:8010/osgi/http/HttpWhiteboardExample
    private static final String HTTP_WHITEBOARD_REMOVE = "[AUDIT   ] CWWKN2001A: ";

    // [AUDIT   ] CWWKG0093A:: Processing configuration drop-ins resource
    private static final String CONFIG_DROPINS_ADD = "[AUDIT   ] CWWKG0093A: ";

    //[AUDIT   ] CWWKG0028A: Processing included configuration resource
    private static final String INCLUDES_ADD = "[AUDIT   ] CWWKG0028A: ";

    private static final String FEATURE_JSP = "jsp";
    private static final String FEATURE_JNDI = "jndi";
    private static final String FEATURE_JDBC = "jdbc";

    private static final String ELEMENT_DATASOURCE = "dataSource";

    private static final String DATASOURCE_CLASS = "javax.sql.DataSource";

    private AbstractServerExtension serverExtension = null;

    // Env.Var. to turn on tracking of any content to the console
    public static final String CONSOLE_LINE_TRACKER_TEST = "console.line.tracker.test";
    // Other content enabled by the above property should use a different log file because logs can be deleted and created during testing
    public static final String BROWSER_LINK_LOG = "consolelinetracker-browserlink.log"; // For tracking links in the console

    class AbstractHyperlink implements IHyperlink {
        @Override
        public void linkEntered() {
            // do nothing
        }

        @Override
        public void linkExited() {
            // do nothing
        }

        @Override
        public void linkActivated() {
            // do nothing
        }
    }

    private IConsole console;

    @Override
    public void init(IConsole console) {
        this.console = console;
    }

    @Override
    public void lineAppended(IRegion line) {
        try {
            int offset = line.getOffset();
            int length = line.getLength();
            String text = console.getDocument().get(offset, length);

            if (text == null || length == 0)
                return;

            if (text.equals("wasdev")) {
                IHyperlink link = new AbstractHyperlink() {
                    @Override
                    public void linkActivated() {
                        try {
                            IWorkbenchBrowserSupport bs = PlatformUI.getWorkbench().getBrowserSupport();
                            IWebBrowser b = bs.createBrowser(IWorkbenchBrowserSupport.LOCATION_BAR | IWorkbenchBrowserSupport.NAVIGATION_BAR, null, null, null);
                            b.openURL(new URL("http://wasdev.net"));
                        } catch (Exception e) {
                            Trace.logError("Error activating console link: http://wasdev.net", e);
                        }
                    }
                };
                console.addLink(link, offset, text.length());
            }
            if (text.startsWith(SERVLET_ERROR)) {
                int start = text.indexOf("'") + 1;
                int end = text.indexOf("'", start);
                if (start > 0 && end > 0 && start < end - 3) {
                    String content = text.substring(start, end);
                    final String content1;

                    //Defect: 113712, First link in console view for error does not work.
                    //append "()" into the content string before ":"
                    int index = content.indexOf(":");
                    if (index > -1) {
                        String contentBegin = content.substring(0, index);
                        String contentEnd = content.substring(index);
                        content1 = contentBegin + "()" + contentEnd;
                    } else {
                        content1 = content + "()";
                    }
                    IHyperlink link = new AbstractHyperlink() {
                        @Override
                        public void linkActivated() {
                            try {
                                openJavaEditor(content1);
                            } catch (Exception e) {
                                Trace.logError("Error activating console link: " + content1, e);
                            }
                        }
                    };
                    console.addLink(link, offset + start, end - start);
                }
            } else if (text.startsWith(JSP_WARNING)) {
                IHyperlink link = new AbstractHyperlink() {
                    @Override
                    public void linkActivated() {
                        addJSPSupport();
                    }
                };
                console.addLink(link, offset + JSP_WARNING.length(), length - JSP_WARNING.length());
            } else if (text.startsWith(COLD_START_ERROR)) {
                IHyperlink link = new AbstractHyperlink() {
                    @Override
                    public void linkActivated() {
                        promptForColdStart();
                    }
                };
                console.addLink(link, offset + COLD_START_ERROR.length(), length - COLD_START_ERROR.length());
            } else if (text.startsWith(WEB_APP_ADD) || text.startsWith(WEB_APP_REMOVE)) {
                addBrowserLinkToConsole(text, offset, length, WEB_APP_ADD);
            } else if (text.startsWith(HTTP_WHITEBOARD_ADD)) {
                addBrowserLinkToConsole(text, offset, length, HTTP_WHITEBOARD_ADD);
            } else if (text.startsWith(HTTP_WHITEBOARD_REMOVE)) {
                addBrowserLinkToConsole(text, offset, length, HTTP_WHITEBOARD_REMOVE);
            } else if (text.startsWith(OSGI_APPCONSOLE_ADD)) {
                addBrowserLinkToConsole(text, offset, length, OSGI_APPCONSOLE_ADD);
            } else if (text.startsWith(OSGI_APPCONSOLE_REMOVE)) {
                addBrowserLinkToConsole(text, offset, length, OSGI_APPCONSOLE_REMOVE);
            } else if (text.contains(JNDI_NO_INITIAL_CONTEXT_EXCEPTION)) {
                IHyperlink link = new AbstractHyperlink() {
                    @Override
                    public void linkActivated() {
                        addJNDISupport();
                    }
                };
                console.addLink(link, offset + JNDI_NO_INITIAL_CONTEXT_EXCEPTION.length(), length - JNDI_NO_INITIAL_CONTEXT_EXCEPTION.length());
            } else if (text.startsWith(JNDI_INJECT_ERROR) && text.contains(DATASOURCE_CLASS)) {
                final String output = text;
                IHyperlink link = new AbstractHyperlink() {
                    @Override
                    public void linkActivated() {
                        addJDBCSupport(output);
                    }
                };
                console.addLink(link, offset + JNDI_INJECT_ERROR.length(), length - JNDI_INJECT_ERROR.length());
            } else if (text.startsWith(CONFIG_DROPINS_ADD)) {
                addPathLinkToConsole(text, offset, length, CONFIG_DROPINS_ADD);
            } else if (text.startsWith(INCLUDES_ADD)) {
                addPathLinkToConsole(text, offset, length, INCLUDES_ADD);
            }
        } catch (BadLocationException e) {
            // ignore, not critical
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error trying to add links to console", e);
        }
    }

    protected static void openJavaEditor(String content2) {
        String content = content2;

        int index = content.indexOf(":");
        int lineNumber = -1;
        if (index > 0) {
            try {
                lineNumber = Integer.parseInt(content.substring(index + 1));
            } catch (Exception e) {
                // ignore - could not get line number
            }
            content = content.substring(0, index);
        }

        // ignore method name - it is helpful for the user, but we don't need it
        index = content.lastIndexOf(".");
        if (content.endsWith("()") && index > 0) {
            //methodName = content.substring(index + 1, content.length() - 2);
            content = content.substring(0, index);
        }

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject p : projects) {
            try {
                if (p.isOpen() && p.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
                    IJavaProject javaProject = JavaCore.create(p);
                    IType type = javaProject.findType(content);
                    if (type != null) {
                        ITextEditor editor = (ITextEditor) JavaUI.openInEditor(type);

                        IDocumentProvider provider = editor.getDocumentProvider();
                        IDocument document = provider.getDocument(editor.getEditorInput());
                        int start = 0;
                        try {
                            start = document.getLineOffset(lineNumber - 1);
                        } catch (BadLocationException ble) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.INFO, "Could not get offset for line " + lineNumber);
                        }
                        editor.selectAndReveal(start, 0);

                        IWorkbenchPage page = editor.getSite().getPage();
                        page.activate(editor);
                    }
                }
            } catch (Exception e) {
                Trace.logError("Error opening Java editor", e);
            }
        }

        // TODO future - should only scan Java projects that are running on this server
        /*
         * try {
         * ILaunchConfiguration config = console.getProcess().getLaunch().getLaunchConfiguration();
         * IServer server = ServerUtil.getServer(config);
         * IModule[] m = server.getModules();
         * server.getChildModules(module, monitor)
         * } catch (Exception e) {
         * e.printStackTrace();
         * }
         */
    }

    protected void addJSPSupport() {
        // TODO: should move to jee bundle in the future, since this is a jee 'quick-fix'
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Running JSP feature quick fix");

        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            if (MessageDialog.openConfirm(shell, Messages.title, Messages.taskFixJSP)) {
                WebSphereServer server = getServer();
                // sanity check
                if (server == null) {
                    Trace.logError("Failed to get server", null);
                    return;
                }

                // try to add the jsp feature to the server configuration
                IStatus status = ConsoleLineTrackerHelper.addFeatureSupport(server, FEATURE_JSP);
                if (status == ConsoleLineTrackerHelper.EXIST_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JSP support already exists on server");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureExists, FEATURE_JSP));
                } else if (status == ConsoleLineTrackerHelper.UNRESOLVED_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JSP feature is not part of the runtime install");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureUnresolved, FEATURE_JSP));
                }
            }
        } catch (Exception e) {
            Trace.logError("Error trying to add JSP support", e);
        }
    }

    protected void addJNDISupport() {
        // TODO: should move to jee bundle in the future, since this is a jee 'quick-fix'
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Running JNDI feature quick fix");

        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            if (MessageDialog.openConfirm(shell, Messages.title, Messages.taskFixJNDI)) {
                WebSphereServer server = getServer();
                // sanity check
                if (server == null) {
                    Trace.logError("Failed to get server", null);
                    return;
                }

                // try and add the latest jndi feature
                IStatus status = ConsoleLineTrackerHelper.addFeatureSupport(server, FEATURE_JNDI);
                if (status == ConsoleLineTrackerHelper.EXIST_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JNDI support already exists on server");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureExists, FEATURE_JNDI));
                } else if (status == ConsoleLineTrackerHelper.UNRESOLVED_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JNDI feature is not part of the runtime install");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureUnresolved, FEATURE_JNDI));
                }
            }
        } catch (Exception e) {
            Trace.logError("Error trying to add JNDI support", e);
        }
    }

    protected void addJDBCSupport(String text) {
        // TODO: should move to jee bundle in the future, since this is a jee 'quick-fix'
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Running JDBC feature quick fix");

        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            WebSphereServer server = getServer();
            // sanity check
            if (server == null) {
                Trace.logError("Failed to get server", null);
                return;
            }

            // make sure external config files are in sync
            ConfigurationFile configFile = server.getConfiguration();
            if (configFile.getIFile() == null) {
                server.refreshConfiguration();
            }

            if (ConsoleLineTrackerHelper.isFeatureExist(server, FEATURE_JDBC)) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "JDBC support already exists on server");
                }

                if (configFile.hasElement(ELEMENT_DATASOURCE)) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "dataSource element already exists on server");
                    }

                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorElementSet, ELEMENT_DATASOURCE));

                } else {
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorElementNotSet, ELEMENT_DATASOURCE));
                }

                return;
            }

            if (MessageDialog.openConfirm(shell, Messages.title, Messages.taskFixJDBC)) {
                // try and add the latest jdbc feature
                IStatus status = ConsoleLineTrackerHelper.addFeatureSupport(server, FEATURE_JDBC);
                if (status == ConsoleLineTrackerHelper.EXIST_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JDBC support already exists on server");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureExists, FEATURE_JDBC));
                } else if (status == ConsoleLineTrackerHelper.UNRESOLVED_STATUS) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "JDBC feature is not part of the runtime install");
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.errorFeatureUnresolved, FEATURE_JDBC));
                }
            }
        } catch (Exception e) {
            Trace.logError("Error trying to add JDBC support", e);
        }
    }

    private WebSphereServer getServer() throws Exception {
        ILaunchConfiguration launchConfig = console.getProcess().getLaunch().getLaunchConfiguration();
        IServer srv = ServerUtil.getServer(launchConfig);
        return (srv == null) ? null : (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
    }

    protected void promptForColdStart() {
        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            if (MessageDialog.openQuestion(shell, Messages.title, Messages.taskColdStart)) {
                ILaunchConfiguration launchConfig = console.getProcess().getLaunch().getLaunchConfiguration();
                final IServer server = ServerUtil.getServer(launchConfig);
                final String mode = server.getMode();

                if (server.getServerState() == IServer.STATE_STARTED) {
                    server.stop(false, new IOperationListener() {
                        @Override
                        public void done(IStatus result) {
                            if (result != null && result.isOK())
                                cleanAndStart(server, mode);
                        }
                    });
                } else
                    cleanAndStart(server, mode);
            }
        } catch (Exception e) {
            Trace.logError("Error trying to do a clean start", e);
        }
    }

    protected static void cleanAndStart(IServer server, String mode) {
        WebSphereServerBehaviour wsServer = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        wsServer.setCleanOnStartup(true);
        server.start(mode, (IOperationListener) null);
    }

    protected void openBrowser(String urlString) {
        try {
            IWorkbenchBrowserSupport bs = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser b = bs.createBrowser(IWorkbenchBrowserSupport.LOCATION_BAR | IWorkbenchBrowserSupport.NAVIGATION_BAR, null, null, null);
            b.openURL(new URL(urlString));
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "URL is not valid or could not open browser", e);
        }
    }

    /**
     * Parse the given console message text to determine the URL and add a browser link to the console.
     */
    private void addBrowserLinkToConsole(String text, int offset, int length, String prefix) {
        if (console == null)
            return;

        int start = text.indexOf(": ", prefix.length()) + 2;
        String tempUrl = null;
        if (text.endsWith("*"))
            tempUrl = text.substring(start, length - 1);
        else
            tempUrl = text.substring(start, length);

        // Allow server extensions to determine the link
        try {
            WebSphereServer server = getServer();
            // Should cache this extension.  The tracker is created once per connected server per console.
            // For local liberty, this will be null
            if (serverExtension == null) {
                serverExtension = ServerTypeExtensionFactory.getServerExtension(server.getServerType());
            }
            if (serverExtension != null) {
                URI uri = URI.create(tempUrl); // Should be a valid URI to begin with

                IServer iServer = server.getServer();
                String serverHost = iServer.getHost();

                int uriPort = uri.getPort();
                String serverPort = uriPort < 0 ? "" : Integer.toString(uriPort); //$NON-NLS-1$

                // Get the string http:// or https://  . The uri.getScheme() only returns the http part, so you end up
                // having to add the :// yourself.  This way, it will get the needed string that immediately precedes the hostname.
                String scheme = tempUrl.substring(0, tempUrl.indexOf(uri.getHost()));

                String mappedHost = serverExtension.getConnectionHost(iServer, serverHost, serverPort);
                String mappedPort = serverExtension.getConnectionPort(iServer, serverPort);
                //        http://  localhost     :    8008         /restOfPath
                tempUrl = scheme + mappedHost + ":" + mappedPort + uri.getPath(); //$NON-NLS-1$

                // For test purposes only, set env var to write links to a tracker file
                String trackerValue = System.getProperty(CONSOLE_LINE_TRACKER_TEST);
                if (trackerValue != null) { // accept any value to turn it on
                    // Just append to the existing browser link log file
                    File consoleLineTrackerLog = com.ibm.ws.st.core.internal.Activator.getInstance().getStateLocation().append(BROWSER_LINK_LOG).toFile();
                    FileWriter fw = null;
                    try {
                        fw = new FileWriter(consoleLineTrackerLog, true);
                        fw.append("Original URL:" + uri.toString() + "\n"); //$NON-NLS-1$  //$NON-NLS-2$
                        fw.append("Mapped URL:" + tempUrl + "\n"); //$NON-NLS-1$  //$NON-NLS-2$
                    } catch (Exception e) {
                        e.printStackTrace(); // Test purposes only.  So OK to print the trace
                    } finally {
                        if (fw != null) {
                            fw.close();
                        }
                    }
                }

            }
        } catch (Exception e) {
            // If there are any issues calculating the mapped URL, then just use the original URL value as the link
            Trace.logError("Cannot convert to a mapped URL", e); //$NON-NLS-1$
        }

        final String url = tempUrl;
        IHyperlink link = new AbstractHyperlink() {
            @Override
            public void linkActivated() {
                openBrowser(url);
            }
        };
        console.addLink(link, offset + start, length - start);
    }

    protected void openFileOnEditor(String path) {
        WebSphereServer server = null;
        try {
            server = getServer();
        } catch (Exception e) {
            Trace.logError("Failed to get server", e);
            return;
        }

        if (server.isLocalSetup()) {
            openLocalFile(path);
        }

        else {
            openRemoteFile(server, path);
        }
    }

    private void openLocalFile(String path) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath pathToFile = new Path(path);
        IFile fileToOpen = workspaceRoot.getFileForLocation(pathToFile);
        Activator.openConfigurationEditor(fileToOpen, pathToFile.toFile().toURI());
    }

    private void openRemoteFile(WebSphereServer server, String path) {
        JMXConnection jmx = null;
        String localFilePath = null;
        try {
            jmx = server.createJMXConnection();
            localFilePath = server.getWebSphereServerBehaviour().resolveLocalFilePath(path, jmx);
        } catch (Exception e) {
            Trace.logError("Failed to create JMX connection", e);
        } finally {
            if (jmx != null)
                jmx.disconnect();
        }

        if (localFilePath != null && !localFilePath.isEmpty()) {
            openLocalFile(localFilePath);
        }
    }

    private void addPathLinkToConsole(final String text, int offset, int length, String prefix) {
        final String output = text;
        int pathIndex = 0;

        if (output.contains("\\")) {
            //Windows Path
            pathIndex = output.indexOf("\\") - 2;
        }

        else {
            //Linux Path
            pathIndex = output.indexOf("/");
        }

        final String path = text.substring(pathIndex, length);

        IHyperlink link = new AbstractHyperlink() {
            @Override
            public void linkActivated() {
                try {
                    openFileOnEditor(path);
                } catch (Exception e) {
                    Trace.logError("Error activating console link: " + path, e);
                }
            }
        };
        console.addLink(link, offset + pathIndex, length - pathIndex);
    }

    @Override
    public void dispose() {
        // ignore
    }
}