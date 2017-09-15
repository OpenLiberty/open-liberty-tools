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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.util.RuntimeLifecycleAdapter;
import org.eclipse.wst.server.core.util.ServerLifecycleAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.IWebSphereServerListener;
import com.ibm.ws.st.core.internal.ServerListenerUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.IConfigurationElement;
import com.ibm.ws.st.ui.internal.custom.CustomServerConfigManager;
import com.ibm.xwt.dde.internal.customization.CustomizationManager;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.viewers.TreeContentProvider;

@SuppressWarnings("restriction")
public class DDETreeContentProvider extends TreeContentProvider {
    private static final String DDE_CUSTOMIZATION = "com.ibm.ws.st.ui.configuration.editor";
    private static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";
    protected static final Object[] NO_CHILDREN = new Object[0];

    protected CommonViewer commonViewer;
    protected RuntimeLifecycleAdapter runtimeListener;
    protected ServerLifecycleAdapter serverListener;

    protected IWebSphereServerListener listener;

    public DDETreeContentProvider() {
        super(getCustomization());
    }

    public static Customization getCustomization() {
        return CustomizationManager.getInstance().getCustomization(DDE_CUSTOMIZATION);
    }

    protected static boolean checkType(IRuntime runtime) {
        return WebSphereUtil.isWebSphereRuntime(runtime);
    }

    protected static boolean checkType(IServer server) {
        IServerType type = server.getServerType();
        if (type != null && type.getId().startsWith(Constants.SERVER_ID_PREFIX))
            return true;
        return false;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (!(viewer instanceof CommonViewer))
            return;

        commonViewer = (CommonViewer) viewer;

        if (listener == null) {
            listener = new IWebSphereServerListener() {
                @Override
                public void serverChanged(WebSphereServerInfo server) {
                    refresh(server);
                }

                @Override
                public void runtimeChanged(IRuntime runtime) {
                    refresh(runtime);
                }
            };
            ServerListenerUtil.getInstance().addServerListener(listener);
        }

        if (runtimeListener == null) {
            runtimeListener = new RuntimeLifecycleAdapter() {
                @Override
                public void runtimeAdded(IRuntime runtime) {
                    if (checkType(runtime))
                        add(runtime);
                }

                @Override
                public void runtimeChanged(IRuntime runtime) {
                    if (checkType(runtime))
                        refresh(runtime);
                }

                @Override
                public void runtimeRemoved(IRuntime runtime) {
                    if (checkType(runtime))
                        remove(runtime);
                }
            };
            ServerCore.addRuntimeLifecycleListener(runtimeListener);
        }

        if (serverListener == null) {
            serverListener = new ServerLifecycleAdapter() {
                @Override
                public void serverAdded(IServer server) {
                    if (checkType(server)) {
                        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                        if (ws != null)
                            refresh(ws.getServerInfo());
                    }
                }

                @Override
                public void serverChanged(IServer server) {
                    if (checkType(server)) {
                        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                        if (ws != null)
                            refresh(ws.getServerInfo());
                    }
                }

                @Override
                public void serverRemoved(IServer server) {
                    if (checkType(server)) {
                        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                        if (ws != null)
                            refresh(ws.getServerInfo());
                    }
                }
            };
            ServerCore.addServerLifecycleListener(serverListener);
        }
    }

    protected void add(final IRuntime runtime) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (commonViewer != null)
                    commonViewer.refresh(commonViewer.getInput());
            }
        });
    }

    protected void refresh(final IRuntime runtime) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (commonViewer != null) {
                    // Refresh the whole servers view, because we could have
                    // changed the location of the runtime, and any related
                    // servers will be out of sync
                    if (SERVERS_VIEW_ID.equals(commonViewer.getCommonNavigator().getViewSite().getId())) {
                        commonViewer.refresh(true);
                    } else {
                        commonViewer.refresh(runtime, true);
                    }
                }
            }
        });
    }

    protected void remove(final IRuntime runtime) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (commonViewer != null)
                    commonViewer.remove(runtime);
            }
        });
    }

    protected void refresh(final IServer server) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (commonViewer != null) {
                    commonViewer.refresh(server, true);

                    WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                    commonViewer.refresh(ws.getServerInfo(), true);
                }
            }
        });
    }

    protected void refresh(final WebSphereServerInfo serverInfo) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                // Different views (servers view, runtime explorer view) are displaying different objects
                // so refresh both on the server info and the server.  The server must be the original and
                // not a working copy.
                if (commonViewer != null) {
                    commonViewer.refresh(serverInfo, true);

                    WebSphereServer ws = WebSphereUtil.getWebSphereServer(serverInfo);
                    if (ws != null) {
                        IServer server = ws.getServer();
                        if (server.isWorkingCopy()) {
                            IServerWorkingCopy wc = (IServerWorkingCopy) server;
                            server = wc.getOriginal();
                        }
                        commonViewer.refresh(server, true);
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        commonViewer = null;
        ServerListenerUtil.getInstance().removeServerListener(listener);
        ServerCore.removeRuntimeLifecycleListener(runtimeListener);
        ServerCore.removeServerLifecycleListener(serverListener);
        super.dispose();
    }

    protected static URI getURI(Document document) {
        try {
            String userDataURI = (String) document.getUserData(ConfigurationFile.USER_DATA_URI);
            if (userDataURI == null)
                return null;
            return new URI(userDataURI);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error getting URI from document", e);
        }
        return null;
    }

    public static URI getURI(Element element) {
        return getURI(element.getOwnerDocument());
    }

    private static UserDirectory getUserDirectory(Document document) {
        return (UserDirectory) document.getUserData(ConfigurationFile.USER_DATA_USER_DIRECTORY);
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof String)
            return new Object[] { inputElement };
        Object[] children = getChildren(inputElement);
        if (children != null && children.length == 0 && inputElement instanceof Element)
            return new Object[] { Messages.configEmpty };

        return children;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IServer) {
            IServer server = (IServer) parentElement;

            WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            try {
                List<Object> list = new ArrayList<Object>();

                CustomServerConfigManager customServerConfigManager = CustomServerConfigManager.getInstance();
                List<Object> customServerElements = customServerConfigManager.getCustomServerElements(server);
                if (customServerElements != null && customServerElements.size() > 0) {
                    list.addAll(customServerElements);
                } else {
                    ConfigurationFile configFile = ws.getConfiguration();
                    if (configFile != null) {

                        // Parse the contents of the config file
                        Document document;
                        document = DomXmlDocumentFileCache.getInstance().getDocument(configFile);

                        if (document != null) {
                            list.add(document.getDocumentElement());
                        }
                    }

                    WebSphereServerInfo wsServer = ws.getServerInfo();
                    if (wsServer != null) {
                        ExtendedConfigFile config = wsServer.getBootstrap();
                        if (config != null)
                            list.add(config);

                        // only the jvm.options under the server config folder should be shown here
                        ExtendedConfigFile jvmOptions = wsServer.getJVMOptions(wsServer.getServerPath());
                        if (jvmOptions != null)
                            list.add(jvmOptions);

                        config = wsServer.getServerEnv();
                        if (config != null)
                            list.add(config);

                        ConfigurationFolder dropinsFolder = wsServer.getConfigurationDropinsFolder();
                        if (dropinsFolder != null)
                            list.add(dropinsFolder);
                    }
                }

                return list.toArray();
            } catch (Exception e) {
                Trace.logError("Error while trying to load config tree for server: " + server.getName(), e);
            }
        }

        if (parentElement instanceof Element) {
            Element element = (Element) parentElement;
            if (Constants.INCLUDE_ELEMENT.equals(element.getNodeName())) {
                NamedNodeMap nnm = element.getAttributes();
                Node attrNode = nnm.getNamedItem(Constants.LOCATION_ATTRIBUTE);
                if (attrNode != null) {
                    String include = attrNode.getNodeValue();
                    Document document = element.getOwnerDocument();
                    URI baseUri = getURI(document);
                    UserDirectory userDir = getUserDirectory(document);
                    if (userDir != null) {
                        URI includeURI = ConfigUtils.resolve(baseUri, include, userDir);
                        if (includeURI == null)
                            return NO_CHILDREN;
                        try {
                            ConfigurationFile configFile = new ConfigurationFile(includeURI, userDir);
                            Document document2 = configFile.getDomDocument();
                            if (document2 != null)
                                return super.getChildren(document2.getDocumentElement());
                        } catch (Exception e) {
                            Trace.logError("Error while trying to load config tree for include file with uri: " + includeURI, e);
                        }
                    }
                }
            }
        }

        if (parentElement instanceof IRuntime) {
            IRuntime runtime = (IRuntime) parentElement;
            WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            if (wr == null)
                return NO_CHILDREN;

            List<UserDirectory> userDirs = wr.getUserDirectories();
            if (userDirs == null || userDirs.isEmpty())
                return NO_CHILDREN;
            if (userDirs.size() == 1) {
                UserDirectory userDir = userDirs.get(0);
                return new Object[] { new RuntimeExplorer.Node(userDir, RuntimeExplorer.NodeType.SERVERS),
                                      new RuntimeExplorer.Node(userDir, RuntimeExplorer.NodeType.SHARED_CONFIGURATIONS) };
            }
            return userDirs.toArray();
        }
        if (parentElement instanceof UserDirectory) {
            UserDirectory userDir = (UserDirectory) parentElement;
            return new Object[] { new RuntimeExplorer.Node(userDir, RuntimeExplorer.NodeType.SERVERS),
                                  new RuntimeExplorer.Node(userDir, RuntimeExplorer.NodeType.SHARED_CONFIGURATIONS) };
        }
        if (parentElement instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) parentElement;
            return node.getChildren();
        }
        if (parentElement instanceof WebSphereServerInfo) {
            WebSphereServerInfo server = (WebSphereServerInfo) parentElement;
            List<Object> list = new ArrayList<Object>();
            list.add(server.getConfigRoot());
            ExtendedConfigFile config = server.getBootstrap();
            if (config != null)
                list.add(config);

            // only the jvm.options under the server config folder should be shown here
            ExtendedConfigFile jvmOptions = server.getJVMOptions(server.getServerPath());
            if (jvmOptions != null)
                list.add(jvmOptions);

            config = server.getServerEnv();
            if (config != null)
                list.add(config);
            ConfigurationFolder dropinsFolder = server.getConfigurationDropinsFolder();
            if (dropinsFolder != null)
                list.add(dropinsFolder);
            return list.toArray();
        }
        if (parentElement instanceof ConfigurationFolder) {
            return getConfigurationFolderChildren(parentElement);
        }
        if (parentElement instanceof ConfigurationFile) {
            ConfigurationFile configFile = (ConfigurationFile) parentElement;
            Document document2 = configFile.getDomDocument();
            Node node = document2.getDocumentElement();
            return getChildren(node);
        }
        if (parentElement instanceof CustomServerConfigTreeNode) {
            List<Object> children = ((CustomServerConfigTreeNode) parentElement).getChildren();
            return children.toArray(new Object[children.size()]);
        }

        Object[] children = super.getChildren(parentElement);

        if (children != null && children.length == 0 && parentElement instanceof Element && Constants.SERVER_ELEMENT.equals(((Element) parentElement).getNodeName()))
            return new Object[] { Messages.configEmpty };

        return children;
    }

    // Load our own IDOMModel
    //
    // We load our own IDOMModel, instead on relying on the one provided by
    // the configuration file, because we could end up with orphaned nodes
    // in the server's view (if it was expanded and the configuration file
    // was open, and we add/remove application to/from the server). The DDE
    // editor registers a model state listener, so when an application is
    // added to the server, the model is changed and the editor will re-load
    // the model which will update the document tree (replacing child nodes
    // with new ones). As a result, the nodes in the server view are orphaned
    // and we end up with null pointer excpetion when selecting any config
    // item in the server's view
//    @SuppressWarnings("deprecation")
//    private Document getDomDocument(ConfigurationFile configFile) {
//        InputStream in = null;
//        IDOMModel dom = null;
//
//        try {
//            final URI uri = configFile.getURI();
//            IModelManager manager = StructuredModelManager.getModelManager();
//            IStructuredModel model = null;
//            try {
//                in = new BufferedInputStream(new FileInputStream(new File(uri)));
//                model = manager.getModelForRead(new File(uri).getAbsolutePath(), in, null);
//
//                if (model == null || !(model instanceof IDOMModel)) {
//                    Trace.logError("Unable to load IDOM Model from uri: " + uri, null);
//                    return null;
//                }
//            } catch (Exception e) {
//                Trace.logError("Error loading IDOM Model from uri: " + uri, e);
//                return null;
//            } finally {
//                if (in != null)
//                    try {
//                        in.close();
//                    } catch (IOException e) {
//                        // ignore
//                    }
//            }
//
//            dom = (IDOMModel) model;
//
//            // put the document uri and user directory into user data to help downstream viewers
//            IDOMDocument domDocument = dom.getDocument();
//            domDocument.setUserData(ConfigurationFile.USER_DATA_URI, uri.toString(), null);
//            domDocument.setUserData(ConfigurationFile.USER_DATA_USER_DIRECTORY, configFile.getUserDirectory(), null);
//
//            return domDocument;
//        } finally {
//            if (dom != null)
//                dom.releaseFromRead();
//        }
//    }
    @Override
    public Object getParent(Object element) {
        if (element instanceof RuntimeExplorer.Node)
            return ((RuntimeExplorer.Node) element).getParent();
        if (element instanceof WebSphereServerInfo)
            return ((WebSphereServerInfo) element).getUserDirectory();
        return super.getParent(element);
    }

    // Get the children of Config Dropins folders including jvm.options
    public Object[] getConfigurationFolderChildren(Object parent) {
        ConfigurationFolder folder = (ConfigurationFolder) parent;
        String name = folder.getPath().lastSegment();
        if (name.equals(Constants.CONFIG_DROPINS_FOLDER)) {
            return folder.getChildren();
        }

        List<Object> configFolderChildren = new ArrayList<Object>();
        for (IConfigurationElement configElement : folder.getChildren()) {
            configFolderChildren.add(configElement);
        }

        // add the other jvmfiles
        List<WebSphereServerInfo> servers = folder.getUserDirectory().getWebSphereRuntime().getWebSphereServerInfos();
        WebSphereServerInfo configServer = null;
        IPath currConfigDropinsSubFolderPath = null;

        // Find the server and get the current config dropins folder
        for (WebSphereServerInfo currServer : servers) {
            IPath currConfigOverrideDropinsPath = currServer.getConfigOverrideDropinsPath();
            IPath currConfigDefaultDropinsPath = currServer.getConfigDefaultDropinsPath();
            if (currConfigOverrideDropinsPath != null && currConfigOverrideDropinsPath.equals(folder.getPath())) {
                currConfigDropinsSubFolderPath = currConfigOverrideDropinsPath;
                configServer = currServer;
                break;
            } else if (currConfigDefaultDropinsPath != null && currConfigDefaultDropinsPath.equals(folder.getPath())) {
                currConfigDropinsSubFolderPath = currConfigDefaultDropinsPath;
                configServer = currServer;
                break;
            }
        }

        if (configServer != null && currConfigDropinsSubFolderPath != null) {
            ExtendedConfigFile jvmOptions = configServer.getJVMOptions(currConfigDropinsSubFolderPath);
            if (jvmOptions != null)
                configFolderChildren.add(jvmOptions);
        }

        return configFolderChildren.toArray();
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IServer)
            return true;
        if (element instanceof IRuntime)
            return true;
        if (element instanceof UserDirectory)
            return true;
        if (element instanceof RuntimeExplorer.Node) {
            Object[] obj = ((RuntimeExplorer.Node) element).getChildren();
            return obj != null && obj.length > 0;
        }
        if (element instanceof WebSphereServerInfo) {
            Object[] obj = getChildren(element);
            return obj != null && obj.length > 0;
        }
        if (element instanceof ConfigurationFolder) {
            return getConfigurationFolderChildren(element).length > 0;
        }
        if (element instanceof ConfigurationFile)
            return true;
        return super.hasChildren(element);
    }
}
